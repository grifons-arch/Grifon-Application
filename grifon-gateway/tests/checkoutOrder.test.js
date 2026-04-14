"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const vitest_1 = require("vitest");
const checkoutService_1 = require("../src/services/checkoutService");
class FakeClient {
    constructor(data) {
        this.data = data;
    }
    async getById(resource, id) {
        const key = `${resource}:${id}`;
        return this.data[key];
    }
}
(0, vitest_1.describe)("createCheckoutOrder", () => {
    (0, vitest_1.it)("creates a draft order intent for wholesale customers", async () => {
        const client = new FakeClient({
            "customers:20": { customers: { customer: { id: 20, active: 1, id_default_group: 2 } } },
            "groups:2": { groups: { group: { id: 2, name: "Wholesale Greece", show_prices: 0 } } }
        });
        const order = await (0, checkoutService_1.createCheckoutOrder)(client, {
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
        (0, vitest_1.expect)(order.orderReference).toMatch(/^ORD-/);
        (0, vitest_1.expect)(order.totalAmount).toBe(31);
        (0, vitest_1.expect)(order.paymentStatus).toBe("requires_payment");
        (0, vitest_1.expect)(order.paymentProvider).toBe("stripe_cards");
    });
    (0, vitest_1.it)("returns access details when checkout is denied", async () => {
        const client = new FakeClient({
            "customers:21": { customers: { customer: { id: 21, active: 1, id_default_group: 3 } } },
            "groups:3": { groups: { group: { id: 3, name: "Customer", show_prices: 1 } } }
        });
        await (0, vitest_1.expect)((0, checkoutService_1.createCheckoutOrder)(client, {
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
        })).rejects.toMatchObject({
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
