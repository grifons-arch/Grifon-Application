import { describe, expect, it } from "vitest";
import { parseXmlToJson } from "../src/utils/xml";

describe("parseXmlToJson", () => {
  it("parses prestashop xml payload", () => {
    const xml = `<?xml version="1.0" encoding="UTF-8"?>
      <prestashop>
        <categories>
          <category>
            <id>1</id>
            <name>Root</name>
          </category>
        </categories>
      </prestashop>`;

    const result = parseXmlToJson(xml) as any;
    expect(result.prestashop.categories.category.id).toBe("1");
    expect(result.prestashop.categories.category.name).toBe("Root");
  });
});
