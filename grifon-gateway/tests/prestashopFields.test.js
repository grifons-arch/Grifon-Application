"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const vitest_1 = require("vitest");
const prestashopFields_1 = require("../src/utils/prestashopFields");
(0, vitest_1.describe)("toNumber", () => {
    (0, vitest_1.it)("parses plain decimal strings", () => {
        (0, vitest_1.expect)((0, prestashopFields_1.toNumber)("123.450000")).toBe(123.45);
    });
    (0, vitest_1.it)("parses comma decimal strings", () => {
        (0, vitest_1.expect)((0, prestashopFields_1.toNumber)("123,45")).toBe(123.45);
    });
    (0, vitest_1.it)("parses formatted prices with thousand separators", () => {
        (0, vitest_1.expect)((0, prestashopFields_1.toNumber)("1.234,56")).toBe(1234.56);
        (0, vitest_1.expect)((0, prestashopFields_1.toNumber)("1,234.56")).toBe(1234.56);
    });
    (0, vitest_1.it)("returns null for invalid numeric values", () => {
        (0, vitest_1.expect)((0, prestashopFields_1.toNumber)("")).toBeNull();
        (0, vitest_1.expect)((0, prestashopFields_1.toNumber)("N/A")).toBeNull();
    });
});
