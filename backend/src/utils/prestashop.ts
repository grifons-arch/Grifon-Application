import { XMLParser } from 'fast-xml-parser';

const xmlParser = new XMLParser({
  ignoreAttributes: false,
  attributeNamePrefix: '',
  parseAttributeValue: true,
  parseTagValue: true
});

export const parseXmlToJson = (xml: string) => {
  return xmlParser.parse(xml);
};

export const ensureArray = <T>(value: T | T[] | undefined | null): T[] => {
  if (!value) {
    return [];
  }
  return Array.isArray(value) ? value : [value];
};

export const buildPaginationLimit = (page: number, pageSize: number) => {
  const safePage = Math.max(1, page);
  const safePageSize = Math.max(1, pageSize);
  const offset = (safePage - 1) * safePageSize;
  return `${offset},${safePageSize}`;
};

export const buildSortParam = (sort?: string) => {
  if (!sort) {
    return undefined;
  }
  const [field, direction] = sort.split('_');
  if (!field || !direction) {
    return undefined;
  }
  const upperDirection = direction.toUpperCase();
  if (upperDirection !== 'ASC' && upperDirection !== 'DESC') {
    return undefined;
  }
  return `[${field}_${upperDirection}]`;
};
