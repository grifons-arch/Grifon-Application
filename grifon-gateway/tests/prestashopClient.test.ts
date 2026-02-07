import { describe, expect, it } from "vitest";
import { normalizeNetworkErrorMessage } from "../src/utils/networkErrors";

describe("normalizeNetworkErrorMessage", () => {
  it("formats ENOTFOUND with hostname", () => {
    const message = normalizeNetworkErrorMessage({
      code: "ENOTFOUND",
      hostname: "replica"
    });

    expect(message).toBe("Unable to resolve upstream hostname: replica");
  });

  it("formats timeout and connection refused errors", () => {
    expect(normalizeNetworkErrorMessage({ code: "ETIMEDOUT" })).toBe(
      "Upstream service timed out"
    );
    expect(normalizeNetworkErrorMessage({ code: "ECONNREFUSED" })).toBe(
      "Upstream service refused the connection"
    );
  });

  it("falls back to the original message when code is unknown", () => {
    const message = normalizeNetworkErrorMessage({ message: "socket hang up" });
    expect(message).toBe("socket hang up");
  });
});
