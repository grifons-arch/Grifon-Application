import { describe, expect, it } from 'vitest';
import { buildPaginationLimit, buildSortParam, parseXmlToJson } from '../utils/prestashop.js';

describe('prestashop utils', () => {
  it('parses xml to json', () => {
    const xml = `
      <prestashop>
        <categories>
          <category>
            <id>1</id>
            <name>Root</name>
          </category>
        </categories>
      </prestashop>
    `;

    const result = parseXmlToJson(xml) as {
      prestashop?: { categories?: { category?: { id?: number; name?: string } } };
    };

    expect(result.prestashop?.categories?.category?.id).toBe(1);
    expect(result.prestashop?.categories?.category?.name).toBe('Root');
  });

  it('maps pagination to prestashop limit', () => {
    expect(buildPaginationLimit(1, 20)).toBe('0,20');
    expect(buildPaginationLimit(2, 10)).toBe('10,10');
  });

  it('maps sorting to prestashop format', () => {
    expect(buildSortParam('id_desc')).toBe('[id_DESC]');
    expect(buildSortParam('price_asc')).toBe('[price_ASC]');
    expect(buildSortParam('invalid')).toBeUndefined();
  });
});
