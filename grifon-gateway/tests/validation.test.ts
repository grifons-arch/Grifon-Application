import { describe, expect, it } from "vitest";
import { shopQuerySchema } from "../src/routes/schemas";

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
