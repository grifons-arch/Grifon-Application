import { describe, expect, it, vi } from "vitest";
import { createCheckoutOrder } from "../src/services/checkoutService";

vi.mock("../src/services/stripeService", () => ({
  createStripeCheckoutSession: vi.fn(async () => null),
  verifyStripeCheckoutSession: vi.fn(),
}));

class FakeClient {
  constructor(private data: Record<string, any>) {}

  async getById(resource: string, id: number) {
    const key = `${resource}:${id}`;
    return this.data[key];
  }
}

describe("createCheckoutOrder", () => {
  it("creates a draft order intent for wholesale customers", async () => {
    const client = new FakeClient({
      "customers:20": { customers: { customer: { id: 20, active: 1, id_default_group: 2 } } },
      "groups:2": { groups: { group: { id: 2, name: "Wholesale Greece", show_prices: 0 } } }
    }) as any;

    const order = await createCheckoutOrder(client, {
      shopId: 4,
      customerId: 20,
      paymentMethodCode: "stripe_cards",
      shippingMethodCode: "prestashippingquote",
      address: {
        recipient: "Test Buyer",
        email: "buyer@example.com",
        phone: "6999999999",
        company: "Test Co",
        street: "Demo 1",
        city: "Heraklion",
        postalCode: "71201",
        country: "Greece"
      },
      items: [
        {
          productId: 101,
          title: "Demo Product",
          qty: 2,
          unitPrice: 15.5,
          currency: "EUR"
        }
      ]
    });

    expect(order.orderReference).toMatch(/^ORD-/);
    expect(order.totalAmount).toBe(31);
    expect(order.items).toHaveLength(1);
    expect(order.items[0]).toMatchObject({ productId: 101, qty: 2, unitPrice: 15.5 });
    expect(order.paymentStatus).toBe("requires_payment");
    expect(order.paymentProvider).toBe("stripe_cards");
  });

  it("returns access details when checkout is denied", async () => {
    const client = new FakeClient({
      "customers:21": { customers: { customer: { id: 21, active: 1, id_default_group: 3 } } },
      "groups:3": { groups: { group: { id: 3, name: "Customer", show_prices: 1 } } }
    }) as any;

    await expect(
      createCheckoutOrder(client, {
        shopId: 4,
        customerId: 21,
        paymentMethodCode: "stripe_cards",
        shippingMethodCode: "prestashippingquote",
        address: {
          recipient: "Test Buyer",
          email: "buyer@example.com",
          phone: "6999999999",
          company: "Test Co",
          street: "Demo 1",
          city: "Heraklion",
          postalCode: "71201",
          country: "Greece"
        },
        items: [
          {
            productId: 101,
            title: "Demo Product",
            qty: 1,
            unitPrice: 15.5,
            currency: "EUR"
          }
        ]
      })
    ).rejects.toMatchObject({
      status: 403,
      code: "CHECKOUT_NOT_ALLOWED",
      details: {
        active: true,
        hasWholesaleGroup: false,
        groupNames: ["Customer"]
      }
    });
  });
});
