import { afterEach, describe, expect, it } from "vitest";
import { config } from "../src/config/env";
import { getPriceAccess } from "../src/services/priceAccessService";

class FakeClient {
  constructor(private data: Record<string, any>) {}

  async getById(resource: string, id: number) {
    const key = `${resource}:${id}`;
    return this.data[key];
  }
}

const originalCountryGroupMap = { ...config.countryGroupMap };

afterEach(() => {
  config.countryGroupMap = { ...originalCountryGroupMap };
});

describe("getPriceAccess", () => {
  it("denies access for inactive customer", async () => {
    const client = new FakeClient({
      "customers:10": { customers: { customer: { id: 10, active: 0, id_default_group: 1 } } },
      "groups:1": { groups: { group: { id: 1, show_prices: 1 } } }
    }) as any;

    const result = await getPriceAccess(client, 10);
    expect(result.allowed).toBe(false);
    expect(result.active).toBe(false);
  });

  it("allows access for active customer with show_prices group", async () => {
    const client = new FakeClient({
      "customers:20": { customers: { customer: { id: 20, active: 1, id_default_group: 2 } } },
      "groups:2": { groups: { group: { id: 2, show_prices: 1 } } }
    }) as any;

    const result = await getPriceAccess(client, 20);
    expect(result.allowed).toBe(true);
    expect(result.groupShowPrices).toBe(true);
  });

  it("denies access when group shows prices but is not a configured wholesale group", async () => {
    config.countryGroupMap = { GR: 7 };

    const client = new FakeClient({
      "customers:30": { customers: { customer: { id: 30, active: 1, id_default_group: 2 } } },
      "groups:2": { groups: { group: { id: 2, show_prices: 1 } } }
    }) as any;

    const result = await getPriceAccess(client, 30);
    expect(result.groupShowPrices).toBe(true);
    expect(result.matchesWholesaleGroupConfig).toBe(false);
    expect(result.allowed).toBe(false);
  });

  it("allows access when customer belongs to a configured wholesale group", async () => {
    config.countryGroupMap = { GR: 7 };

    const client = new FakeClient({
      "customers:40": { customers: { customer: { id: 40, active: 1, id_default_group: 7 } } },
      "groups:7": { groups: { group: { id: 7, show_prices: 1 } } }
    }) as any;

    const result = await getPriceAccess(client, 40);
    expect(result.matchesWholesaleGroupConfig).toBe(true);
    expect(result.allowed).toBe(true);
  });
});
