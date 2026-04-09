import { afterEach, describe, expect, it } from "vitest";
import { getPriceAccess } from "../src/services/priceAccessService";

class FakeClient {
  constructor(private data: Record<string, any>) {}

  async getById(resource: string, id: number) {
    const key = `${resource}:${id}`;
    return this.data[key];
  }
}

afterEach(() => {});

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

  it("allows access for active customer with wholesale keyword in default group", async () => {
    const client = new FakeClient({
      "customers:20": { customers: { customer: { id: 20, active: 1, id_default_group: 2 } } },
      "groups:2": { groups: { group: { id: 2, name: "Wholesale Greece", show_prices: 0 } } }
    }) as any;

    const result = await getPriceAccess(client, 20);
    expect(result.allowed).toBe(true);
    expect(result.hasWholesaleGroup).toBe(true);
  });

  it("denies access when no customer group contains wholesale keyword", async () => {
    const client = new FakeClient({
      "customers:30": { customers: { customer: { id: 30, active: 1, id_default_group: 2 } } },
      "groups:2": { groups: { group: { id: 2, name: "Customer", show_prices: 1 } } }
    }) as any;

    const result = await getPriceAccess(client, 30);
    expect(result.hasWholesaleGroup).toBe(false);
    expect(result.allowed).toBe(false);
  });

  it("allows access when customer belongs to a wholesale secondary group", async () => {
    const client = new FakeClient({
      "customers:40": {
        customers: {
          customer: {
            id: 40,
            active: 1,
            id_default_group: 3,
            associations: { groups: [{ id: 3 }, { id: 7 }] }
          }
        }
      },
      "groups:3": { groups: { group: { id: 3, name: "Customer", show_prices: 0 } } },
      "groups:7": { groups: { group: { id: 7, name: "Wholesale Scandinavia", show_prices: 0 } } }
    }) as any;

    const result = await getPriceAccess(client, 40);
    expect(result.hasWholesaleGroup).toBe(true);
    expect(result.allowed).toBe(true);
  });

  it("allows access when wholesale keyword exists in a localized group name", async () => {
    const client = new FakeClient({
      "customers:50": {
        customers: {
          customer: {
            id: 50,
            active: 1,
            id_default_group: 8,
          }
        }
      },
      "groups:8": {
        groups: {
          group: {
            id: 8,
            name: {
              language: [
                { id: 2, value: "Χονδρική Ελλάδα" },
                { id: 1, value: "Wholesale Greece" }
              ]
            },
            show_prices: 0
          }
        }
      }
    }) as any;

    const result = await getPriceAccess(client, 50);
    expect(result.hasWholesaleGroup).toBe(true);
    expect(result.allowed).toBe(true);
  });

  it("allows access when localized group name contains greek wholesale keyword only", async () => {
    const client = new FakeClient({
      "customers:60": {
        customers: {
          customer: {
            id: 60,
            active: 1,
            id_default_group: 9,
          }
        }
      },
      "groups:9": {
        groups: {
          group: {
            id: 9,
            name: {
              language: [
                { id: 2, value: "Χονδρική Ελλάδα" }
              ]
            },
            show_prices: 0
          }
        }
      }
    }) as any;

    const result = await getPriceAccess(client, 60);
    expect(result.hasWholesaleGroup).toBe(true);
    expect(result.allowed).toBe(true);
  });
});
