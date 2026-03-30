"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const vitest_1 = require("vitest");
const env_1 = require("../src/config/env");
const priceAccessService_1 = require("../src/services/priceAccessService");
class FakeClient {
    constructor(data) {
        this.data = data;
    }
    async getById(resource, id) {
        const key = `${resource}:${id}`;
        return this.data[key];
    }
}
const originalCountryGroupMap = { ...env_1.config.countryGroupMap };
(0, vitest_1.afterEach)(() => {
    env_1.config.countryGroupMap = { ...originalCountryGroupMap };
});
(0, vitest_1.describe)("getPriceAccess", () => {
    (0, vitest_1.it)("denies access for inactive customer", async () => {
        const client = new FakeClient({
            "customers:10": { customers: { customer: { id: 10, active: 0, id_default_group: 1 } } },
            "groups:1": { groups: { group: { id: 1, show_prices: 1 } } }
        });
        const result = await (0, priceAccessService_1.getPriceAccess)(client, 10);
        (0, vitest_1.expect)(result.allowed).toBe(false);
        (0, vitest_1.expect)(result.active).toBe(false);
    });
    (0, vitest_1.it)("allows access for active customer with show_prices group", async () => {
        const client = new FakeClient({
            "customers:20": { customers: { customer: { id: 20, active: 1, id_default_group: 2 } } },
            "groups:2": { groups: { group: { id: 2, show_prices: 1 } } }
        });
        const result = await (0, priceAccessService_1.getPriceAccess)(client, 20);
        (0, vitest_1.expect)(result.allowed).toBe(true);
        (0, vitest_1.expect)(result.groupShowPrices).toBe(true);
    });
    (0, vitest_1.it)("denies access when group shows prices but is not a configured wholesale group", async () => {
        env_1.config.countryGroupMap = { GR: 7 };
        const client = new FakeClient({
            "customers:30": { customers: { customer: { id: 30, active: 1, id_default_group: 2 } } },
            "groups:2": { groups: { group: { id: 2, show_prices: 1 } } }
        });
        const result = await (0, priceAccessService_1.getPriceAccess)(client, 30);
        (0, vitest_1.expect)(result.groupShowPrices).toBe(true);
        (0, vitest_1.expect)(result.matchesWholesaleGroupConfig).toBe(false);
        (0, vitest_1.expect)(result.allowed).toBe(false);
    });
    (0, vitest_1.it)("allows access when customer belongs to a configured wholesale group", async () => {
        env_1.config.countryGroupMap = { GR: 7 };
        const client = new FakeClient({
            "customers:40": { customers: { customer: { id: 40, active: 1, id_default_group: 7 } } },
            "groups:7": { groups: { group: { id: 7, show_prices: 1 } } }
        });
        const result = await (0, priceAccessService_1.getPriceAccess)(client, 40);
        (0, vitest_1.expect)(result.matchesWholesaleGroupConfig).toBe(true);
        (0, vitest_1.expect)(result.allowed).toBe(true);
    });
});
