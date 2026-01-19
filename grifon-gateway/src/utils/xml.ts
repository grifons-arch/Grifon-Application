import { XMLParser } from "fast-xml-parser";

const parser = new XMLParser({
  ignoreAttributes: false,
  attributeNamePrefix: "",
  textNodeName: "text",
  allowBooleanAttributes: true
});

export const parseXmlToJson = (xml: string): unknown => {
  return parser.parse(xml);
};
