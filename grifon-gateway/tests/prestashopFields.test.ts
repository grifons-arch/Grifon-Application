import { describe, expect, it } from "vitest";
import { toNumber } from "../src/utils/prestashopFields";

describe("toNumber", () => {
  it("parses plain decimal strings", () => {
    expect(toNumber("123.450000")).toBe(123.45);
  });

  it("parses comma decimal strings", () => {
    expect(toNumber("123,45")).toBe(123.45);
  });

  it("parses formatted prices with thousand separators", () => {
    expect(toNumber("1.234,56")).toBe(1234.56);
    expect(toNumber("1,234.56")).toBe(1234.56);
  });

  it("returns null for invalid numeric values", () => {
    expect(toNumber("")).toBeNull();
    expect(toNumber("N/A")).toBeNull();
  });
});
