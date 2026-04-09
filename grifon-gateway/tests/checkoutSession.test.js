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
(0, vitest_1.describe)("checkoutService", () => {
    (0, vitest_1.it)("builds storefront urls from shop id", () => {
        (0, vitest_1.expect)((0, checkoutService_1.buildStorefrontBaseUrl)(4)).toBe("https://grifon.gr/");
        (0, vitest_1.expect)((0, checkoutService_1.getCheckoutHandoffUrl)(1, "checkout")).toBe("https://grifon.se/index.php?controller=order");
    });
    (0, vitest_1.it)("returns login-first checkout session for guests", async () => {
        const client = new FakeClient({});
        const session = await (0, checkoutService_1.getCheckoutSession)(client, 4);
        (0, vitest_1.expect)(session.isLoggedIn).toBe(false);
        (0, vitest_1.expect)(session.canCheckout).toBe(false);
        (0, vitest_1.expect)(session.loginUrl).toContain("controller=order&login=1");
    });
    (0, vitest_1.it)("returns active checkout session for logged-in wholesale customers", async () => {
        const client = new FakeClient({
            "customers:20": { customers: { customer: { id: 20, active: 1, id_default_group: 2 } } },
            "groups:2": { groups: { group: { id: 2, name: "Wholesale Greece", show_prices: 0 } } }
        });
        const session = await (0, checkoutService_1.getCheckoutSession)(client, 4, 20);
        (0, vitest_1.expect)(session.isLoggedIn).toBe(true);
        (0, vitest_1.expect)(session.canViewPrices).toBe(true);
        (0, vitest_1.expect)(session.canCheckout).toBe(true);
        (0, vitest_1.expect)(session.checkoutUrl).toContain("controller=order");
    });
});
