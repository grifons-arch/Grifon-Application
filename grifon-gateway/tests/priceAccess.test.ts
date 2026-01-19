import { describe, expect, it } from "vitest";
import { getPriceAccess } from "../src/services/priceAccessService";

class FakeClient {
  constructor(private data: Record<string, any>) {}

  async getById(resource: string, id: number) {
    const key = `${resource}:${id}`;
    return this.data[key];
  }
}

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
});
