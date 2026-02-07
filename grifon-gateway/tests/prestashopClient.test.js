"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const vitest_1 = require("vitest");
const networkErrors_1 = require("../src/utils/networkErrors");
(0, vitest_1.describe)("normalizeNetworkErrorMessage", () => {
    (0, vitest_1.it)("formats ENOTFOUND with hostname", () => {
        const message = (0, networkErrors_1.normalizeNetworkErrorMessage)({
            code: "ENOTFOUND",
            hostname: "replica"
        });
        (0, vitest_1.expect)(message).toBe("Unable to resolve upstream hostname: replica");
    });
    (0, vitest_1.it)("formats timeout and connection refused errors", () => {
        (0, vitest_1.expect)((0, networkErrors_1.normalizeNetworkErrorMessage)({ code: "ETIMEDOUT" })).toBe("Upstream service timed out");
        (0, vitest_1.expect)((0, networkErrors_1.normalizeNetworkErrorMessage)({ code: "ECONNREFUSED" })).toBe("Upstream service refused the connection");
    });
    (0, vitest_1.it)("falls back to the original message when code is unknown", () => {
        const message = (0, networkErrors_1.normalizeNetworkErrorMessage)({ message: "socket hang up" });
        (0, vitest_1.expect)(message).toBe("socket hang up");
    });
});
