import { describe, expect, it } from "vitest";
import {
  buildStorefrontBaseUrl,
  getCheckoutHandoffUrl,
  getCheckoutSession
} from "../src/services/checkoutService";

class FakeClient {
  constructor(private data: Record<string, any>) {}

  async getById(resource: string, id: number) {
    const key = `${resource}:${id}`;
    return this.data[key];
  }
}

describe("checkoutService", () => {
  it("builds storefront urls from shop id", () => {
    expect(buildStorefrontBaseUrl(4)).toBe("https://grifon.gr/");
    expect(getCheckoutHandoffUrl(1, "checkout")).toBe(
      "https://grifon.se/index.php?controller=order"
    );
  });

  it("returns login-first checkout session for guests", async () => {
    const client = new FakeClient({}) as any;

    const session = await getCheckoutSession(client, 4);

    expect(session.isLoggedIn).toBe(false);
    expect(session.canCheckout).toBe(false);
    expect(session.loginUrl).toContain("controller=order&login=1");
  });

  it("returns active checkout session for logged-in wholesale customers", async () => {
    const client = new FakeClient({
      "customers:20": { customers: { customer: { id: 20, active: 1, id_default_group: 2 } } },
      "groups:2": { groups: { group: { id: 2, name: "Wholesale Greece", show_prices: 0 } } }
    }) as any;

    const session = await getCheckoutSession(client, 4, 20);

    expect(session.isLoggedIn).toBe(true);
    expect(session.canViewPrices).toBe(true);
    expect(session.canCheckout).toBe(true);
    expect(session.checkoutUrl).toContain("controller=order");
  });
});
