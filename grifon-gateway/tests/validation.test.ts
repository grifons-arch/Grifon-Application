import { describe, expect, it } from "vitest";
import { registerBodySchema, shopQuerySchema } from "../src/routes/schemas";

describe("shopId validation", () => {
  it("accepts valid shopId", () => {
    const result = shopQuerySchema.safeParse({ shopId: "4" });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.shopId).toBe(4);
    }
  });

  it("rejects invalid shopId", () => {
    const result = shopQuerySchema.safeParse({ shopId: "3" });
    expect(result.success).toBe(false);
  });
});

describe("registerBodySchema social title normalization", () => {
  const baseBody = {
    email: "test@example.com",
    password: "secret123",
    firstName: "Test",
    lastName: "User",
    countryIso: "GR",
    street: "Odos 1",
    city: "Athens",
    postalCode: "10435"
  };

  it("normalizes greek male social title to mr", () => {
    const result = registerBodySchema.safeParse({
      ...baseBody,
      socialTitle: "Κος"
    });

    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.socialTitle).toBe("mr");
    }
  });

  it("normalizes greek female social title to mrs", () => {
    const result = registerBodySchema.safeParse({
      ...baseBody,
      socialTitle: "Κα"
    });

    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.socialTitle).toBe("mrs");
    }
  });
});
