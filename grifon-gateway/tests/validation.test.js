"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const vitest_1 = require("vitest");
const schemas_1 = require("../src/routes/schemas");
(0, vitest_1.describe)("shopId validation", () => {
    (0, vitest_1.it)("accepts valid shopId", () => {
        const result = schemas_1.shopQuerySchema.safeParse({ shopId: "4" });
        (0, vitest_1.expect)(result.success).toBe(true);
        if (result.success) {
            (0, vitest_1.expect)(result.data.shopId).toBe(4);
        }
    });
    (0, vitest_1.it)("rejects invalid shopId", () => {
        const result = schemas_1.shopQuerySchema.safeParse({ shopId: "3" });
        (0, vitest_1.expect)(result.success).toBe(false);
    });
});
(0, vitest_1.describe)("registerBodySchema social title normalization", () => {
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
    (0, vitest_1.it)("normalizes greek male social title to mr", () => {
        const result = schemas_1.registerBodySchema.safeParse({
            ...baseBody,
            socialTitle: "Κος"
        });
        (0, vitest_1.expect)(result.success).toBe(true);
        if (result.success) {
            (0, vitest_1.expect)(result.data.socialTitle).toBe("mr");
        }
    });
    (0, vitest_1.it)("normalizes greek female social title to mrs", () => {
        const result = schemas_1.registerBodySchema.safeParse({
            ...baseBody,
            socialTitle: "Κα"
        });
        (0, vitest_1.expect)(result.success).toBe(true);
        if (result.success) {
            (0, vitest_1.expect)(result.data.socialTitle).toBe("mrs");
        }
    });
});
