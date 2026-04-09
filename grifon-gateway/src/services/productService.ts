import { PrestaShopClient } from "../clients/PrestaShopClient";
import { extractResourceItem, extractResourceList } from "./prestashopParser";
import { toLimitParam } from "../utils/pagination";
import { getLocalizedValue, toNumber } from "../utils/prestashopFields";
import { ShopId } from "../config/env";

export interface ProductListItem {
  id: number;
  name: string | null;
  price: number | null;
  reference: string | null;
  attributes: Record<string, string[]>;
  defaultImage: { id: number; url: string } | null;
  active: number | null;
  categories: { id: number }[] | null;
}

export interface CatalogFacetOption {
  key: string;
  label: string;
  count: number;
}

export interface CatalogFacet {
  key: string;
  title: string;
  type: "color" | "brand" | "attribute" | "price" | "availability";
  options: CatalogFacetOption[];
  minValue?: number;
  maxValue?: number;
}

export interface BasicProductFilters {
  search?: string;
  priceMin?: number;
  priceMax?: number;
  colors?: string[];
  attributeFilters?: Record<string, string[]>;
}

type AttributeLookup = Map<number, Record<string, string[]>>;

const buildImageUrl = (shopId: ShopId, productId: number, imageId: number): string => {
  return `/v1/images/products/${productId}/${imageId}?shopId=${shopId}`;
};

const normalizeProduct = (
  product: any,
  shopId: ShopId,
  lang?: number,
  allowPrice = true,
  resolvedAttributes: Record<string, string[]> = {}
): ProductListItem => {
  const id = Number(product.id);
  let idImage = toNumber(product.id_default_image);
  if (!idImage && product.associations?.images) {
    const images = extractResourceList<any>("images", product.associations);
    if (images.length > 0) idImage = Number(images[0].id);
  }

  const categories = product.associations?.categories 
    ? extractResourceList<any>("categories", product.associations).map(c => ({ id: toNumber(c.id) }))
    : [];

  return {
    id,
    name: getLocalizedValue(product.name, lang),
    price: allowPrice ? toNumber(product.price) : null,
    reference: product.reference ?? null,
    attributes: resolvedAttributes,
    defaultImage: idImage ? { id: idImage, url: buildImageUrl(shopId, id, idImage) } : null,
    active: toNumber(product.active),
    categories
  };
};

const buildProductAttributeLookup = async (
  client: PrestaShopClient,
  products: any[],
  lang?: number
): Promise<AttributeLookup> => {
  const pushAttributeValue = (
    target: Record<string, string[]>,
    key: string,
    value: string
  ) => {
    const normalizedKey = key.trim();
    const normalizedValue = value.trim();
    if (!normalizedKey || !normalizedValue) {
      return;
    }

    const existing = target[normalizedKey] ?? [];
    if (!existing.includes(normalizedValue)) {
      target[normalizedKey] = [...existing, normalizedValue];
    }
  };

  const productFeaturePairs = products.map((product) => {
    const productId = toNumber(product.id) ?? 0;
    const features = product.associations?.product_features
      ? extractResourceList<any>("product_features", product.associations)
      : [];

    return {
      productId,
      features: features.map((feature) => ({
        featureId: toNumber(feature.id),
        featureValueId: toNumber(feature.id_feature_value),
      })).filter((feature) => feature.featureId && feature.featureValueId),
    };
  });

  const featureIds = Array.from(
    new Set(
      productFeaturePairs.flatMap((entry) => entry.features.map((feature) => feature.featureId as number))
    )
  );
  const featureValueIds = Array.from(
    new Set(
      productFeaturePairs.flatMap((entry) => entry.features.map((feature) => feature.featureValueId as number))
    )
  );

  const featureNames = new Map<number, string>();
  const featureValues = new Map<number, string>();

  if (featureIds.length > 0) {
    const payload = await client.get("product_features", {
      "filter[id]": `[${featureIds.join("|")}]`,
      display: "full",
      limit: featureIds.length.toString(),
    });
    const items = extractResourceList<any>("product_features", payload);
    items.forEach((item) => {
      const id = toNumber(item.id);
      const name = getLocalizedValue(item.name, lang);
      if (id && name) {
        featureNames.set(id, name);
      }
    });
  }

  if (featureValueIds.length > 0) {
    const payload = await client.get("product_feature_values", {
      "filter[id]": `[${featureValueIds.join("|")}]`,
      display: "full",
      limit: featureValueIds.length.toString(),
    });
    const items = extractResourceList<any>("product_feature_values", payload);
    items.forEach((item) => {
      const id = toNumber(item.id);
      const value = getLocalizedValue(item.value, lang);
      if (id && value) {
        featureValues.set(id, value);
      }
    });
  }

  const attributeLookup: AttributeLookup = new Map();

  productFeaturePairs.forEach((entry) => {
    const attributes: Record<string, string[]> = {};
    entry.features.forEach((feature) => {
      const name = feature.featureId ? featureNames.get(feature.featureId) : null;
      const value = feature.featureValueId ? featureValues.get(feature.featureValueId) : null;
      if (name && value) {
        pushAttributeValue(attributes, name, value);
      }
    });
    attributeLookup.set(entry.productId, attributes);
  });

  const productCombinationPairs = products.map((product) => ({
    productId: toNumber(product.id) ?? 0,
    combinationIds: product.associations?.combinations
      ? extractResourceList<any>("combinations", product.associations)
          .map((combination) => toNumber(combination.id))
          .filter((id): id is number => id !== null)
      : [],
  }));

  const combinationIds = Array.from(
    new Set(productCombinationPairs.flatMap((entry) => entry.combinationIds))
  );

  if (combinationIds.length === 0) {
    return attributeLookup;
  }

  const combinationsPayload = await client.get("combinations", {
    "filter[id]": `[${combinationIds.join("|")}]`,
    display: "full",
    limit: combinationIds.length.toString(),
  });
  const combinations = extractResourceList<any>("combinations", combinationsPayload);

  const optionValueIds = Array.from(
    new Set(
      combinations.flatMap((combination) =>
        combination.associations?.product_option_values
          ? extractResourceList<any>("product_option_values", combination.associations)
              .map((optionValue) => toNumber(optionValue.id))
              .filter((id): id is number => id !== null)
          : []
      )
    )
  );

  if (optionValueIds.length === 0) {
    return attributeLookup;
  }

  const optionValuesPayload = await client.get("product_option_values", {
    "filter[id]": `[${optionValueIds.join("|")}]`,
    display: "full",
    limit: optionValueIds.length.toString(),
  });
  const optionValues = extractResourceList<any>("product_option_values", optionValuesPayload);

  const optionGroupIds = Array.from(
    new Set(
      optionValues
        .map((optionValue) => toNumber(optionValue.id_attribute_group))
        .filter((id): id is number => id !== null)
    )
  );

  const optionGroupNames = new Map<number, string>();
  if (optionGroupIds.length > 0) {
    const optionGroupsPayload = await client.get("product_options", {
      "filter[id]": `[${optionGroupIds.join("|")}]`,
      display: "full",
      limit: optionGroupIds.length.toString(),
    });
    const optionGroups = extractResourceList<any>("product_options", optionGroupsPayload);
    optionGroups.forEach((optionGroup) => {
      const id = toNumber(optionGroup.id);
      const name =
        getLocalizedValue(optionGroup.public_name, lang) ??
        getLocalizedValue(optionGroup.name, lang);
      if (id && name) {
        optionGroupNames.set(id, name);
      }
    });
  }

  const optionValueLookup = new Map<number, { groupName: string; value: string }>();
  optionValues.forEach((optionValue) => {
    const id = toNumber(optionValue.id);
    const groupId = toNumber(optionValue.id_attribute_group);
    const value = getLocalizedValue(optionValue.name, lang);
    const groupName = groupId ? optionGroupNames.get(groupId) : null;
    if (id && groupName && value) {
      optionValueLookup.set(id, { groupName, value });
    }
  });

  const combinationOptionValues = new Map<number, number[]>();
  combinations.forEach((combination) => {
    const combinationId = toNumber(combination.id);
    if (!combinationId) {
      return;
    }
    const ids = combination.associations?.product_option_values
      ? extractResourceList<any>("product_option_values", combination.associations)
          .map((optionValue) => toNumber(optionValue.id))
          .filter((id): id is number => id !== null)
      : [];
    combinationOptionValues.set(combinationId, ids);
  });

  productCombinationPairs.forEach((entry) => {
    const attributes = attributeLookup.get(entry.productId) ?? {};
    entry.combinationIds.forEach((combinationId) => {
      const ids = combinationOptionValues.get(combinationId) ?? [];
      ids.forEach((optionValueId) => {
        const option = optionValueLookup.get(optionValueId);
        if (option) {
          pushAttributeValue(attributes, option.groupName, option.value);
        }
      });
    });
    attributeLookup.set(entry.productId, attributes);
  });

  return attributeLookup;
};

export const listAllProducts = async (
  client: PrestaShopClient,
  shopId: ShopId,
  page: number,
  pageSize: number,
  sort: string,
  lang?: number,
  allowPrice = true,
  filters: BasicProductFilters = {}
): Promise<ProductListItem[]> => {
  const requiresFullFiltering = hasBasicProductFilters(filters) || sort.includes("price");
  if (requiresFullFiltering) {
    const data = await client.get("products", {
      "filter[active]": 1,
      display: "full",
      limit: "5000"
    });

    const items = extractResourceList<any>("products", data);
    const attributeLookup = await buildProductAttributeLookup(client, items, lang);
    const normalizedItems = items.map((product) =>
      normalizeProduct(
        product,
        shopId,
        lang,
        allowPrice,
        attributeLookup.get(Number(product.id)) ?? {}
      )
    );

    return paginateProducts(
      sortProducts(applyBasicProductFilters(normalizedItems, filters), sort),
      page,
      pageSize
    );
  }

  const data = await client.get("products", {
    "filter[active]": 1,
    sort,
    limit: toLimitParam(page, pageSize),
    display: "full" 
  });

  const items = extractResourceList<any>("products", data);
  const attributeLookup = await buildProductAttributeLookup(client, items, lang);
  return items.map((product) =>
    normalizeProduct(
      product,
      shopId,
      lang,
      allowPrice,
      attributeLookup.get(Number(product.id)) ?? {}
    )
  );
};

async function getAllCategoryDescendants(client: PrestaShopClient, parentId: number): Promise<number[]> {
  const results: number[] = [parentId];
  
  const fetchChildren = async (id: number) => {
    try {
      const data = await client.get("categories", {
        "filter[id_parent]": id,
        "filter[active]": 1,
        display: "[id]",
        limit: "1000"
      });
      const children = extractResourceList<any>("categories", data);
      for (const child of children) {
        const childId = toNumber(child.id);
        if (!results.includes(childId)) {
          results.push(childId);
          await fetchChildren(childId);
        }
      }
    } catch (e) {}
  };

  await fetchChildren(parentId);
  return results;
}

export const listProductsByCategory = async (
  client: PrestaShopClient,
  shopId: ShopId,
  categoryId: number,
  page: number,
  pageSize: number,
  sort: string,
  lang?: number,
  allowPrice = true,
  filters: BasicProductFilters = {}
): Promise<ProductListItem[]> => {
  // 0. Λήψη συνολικού πλήθους προϊόντων καταστήματος (για log)
  try {
    const shopData = await client.get("products", {
      "filter[active]": 1,
      display: "[id]",
      limit: "1" // Quick check
    });
    // Σημείωση: Το PrestaShop API συνήθως δεν δίνει το total_results στο JSON body 
    // χωρίς ειδική ρύθμιση, αλλά μπορούμε να πάρουμε μια ιδέα από το "Όλα τα προϊόντα"
  } catch (e) {}

  console.log(`\n--- [CategoryFetch Start] ---`);
  console.log(`Target Category: ${categoryId} | Shop: ${shopId}`);

  if (categoryId === 2) {
    const items = await listAllProducts(client, shopId, page, pageSize, sort, lang, allowPrice, filters);
    console.log(`[CategoryFetch] Shop Root (2) Results: ${items.length} products`);
    console.log(`--- [CategoryFetch End] ---\n`);
    return items;
  }

  // 1. Get ALL categories in tree
  const categoryIds = await getAllCategoryDescendants(client, categoryId);
  console.log(`[CategoryFetch] Hierarchy: Found ${categoryIds.length} categories/subcategories`);
  
  // 2. Aggregate unique product IDs from ALL categories in the tree
  const productIdsSet = new Set<number>();

  // Use id_category_default filter (Source A)
  try {
    const filterVal = `[${categoryIds.join("|")}]`;
    const dataDefault = await client.get("products", {
      "filter[id_category_default]": filterVal,
      "filter[active]": 1,
      display: "[id]",
      limit: "2000"
    });
    const itemsA = extractResourceList<any>("products", dataDefault);
    itemsA.forEach(p => productIdsSet.add(toNumber(p.id)));
    console.log(`[CategoryFetch] Source A (Default Category): Found ${itemsA.length} IDs`);
  } catch (e) {}

  // Then use associations for each category (Source B)
  const associationPromises = categoryIds.map(async (id) => {
    try {
      const catData = await client.get(`categories/${id}`, { display: "full" });
      const category = extractResourceItem<any>("categories", catData);
      if (category?.associations?.products) {
        const pIds = extractResourceList<any>("products", category.associations).map(p => toNumber(p.id));
        pIds.forEach(pid => productIdsSet.add(pid));
        return pIds.length;
      }
    } catch (e) {}
    return 0;
  });
  const resultsB = await Promise.all(associationPromises);
  const totalRawB = resultsB.reduce((sum, val) => sum + val, 0);
  console.log(`[CategoryFetch] Source B (Associations): Found ${totalRawB} raw links`);

  const allIds = Array.from(productIdsSet);
  console.log(`[CategoryFetch] TOTAL UNIQUE PRODUCTS for this Category Tree: ${allIds.length}`);

  if (allIds.length === 0) {
    console.log(`[CategoryFetch] No products found.`);
    console.log(`--- [CategoryFetch End] ---\n`);
    return [];
  }

  const requiresFullFiltering = hasBasicProductFilters(filters) || sort.includes("price");
  if (requiresFullFiltering) {
    const normalizedItems = await fetchNormalizedProductsByIds(client, shopId, allIds, lang, allowPrice);
    const filteredAndSortedItems = sortProducts(
      applyBasicProductFilters(normalizedItems, filters),
      sort
    );
    console.log(`[CategoryFetch] Filtered Results: ${filteredAndSortedItems.length} products`);
    console.log(`--- [CategoryFetch End] ---\n`);
    return paginateProducts(filteredAndSortedItems, page, pageSize);
  }

  // 3. Simple ID-based pagination
  const start = (page - 1) * pageSize;
  const pageIds = allIds.slice(start, start + pageSize);
  console.log(`[CategoryFetch] Page ${page}: Requesting full data for ${pageIds.length} items`);

  // 4. Fetch the actual product objects for this page
  const filterIds = `[${pageIds.join("|")}]`;
  const productsData = await client.get("products", {
    "filter[id]": filterIds,
    "filter[active]": 1,
    display: "full",
    limit: pageSize.toString()
  });

  const finalItems = extractResourceList<any>("products", productsData);
  console.log(`[CategoryFetch] Successfully retrieved ${finalItems.length} products`);
  console.log(`--- [CategoryFetch End] ---\n`);

  // Manual sorting
  if (sort.includes("price")) {
    const desc = sort.includes("DESC");
    finalItems.sort((a, b) => {
      const vA = toNumber(a.price);
      const vB = toNumber(b.price);
      return desc ? vB - vA : vA - vB;
    });
  }

  const attributeLookup = await buildProductAttributeLookup(client, finalItems, lang);
  return finalItems.map((product) =>
    normalizeProduct(
      product,
      shopId,
      lang,
      allowPrice,
      attributeLookup.get(Number(product.id)) ?? {}
    )
  );
};

export const listFacetProductsByCategory = async (
  client: PrestaShopClient,
  shopId: ShopId,
  categoryId: number,
  lang?: number,
  allowPrice = true
): Promise<ProductListItem[]> => {
  return listProductsByCategory(client, shopId, categoryId, 1, 1000, "[id_DESC]", lang, allowPrice, {});
};

const countByValue = (values: string[]) => {
  const counts = new Map<string, number>();
  values.forEach((value) => {
    counts.set(value, (counts.get(value) ?? 0) + 1);
  });
  return counts;
};

const colorAliases = ["color", "colour", "χρώμα", "χρωματισμός", "χρωματισμοί", "färg"];

const isColorAttributeKey = (key: string) => {
  const normalized = key.trim().toLowerCase();
  return colorAliases.some((alias) => normalized.includes(alias));
};

const colorValueAliases: Record<string, string[]> = {
  white: ["white", "λευκ", "ασπρ", "vit"],
  gray: ["gray", "grey", "γκρι", "grå", "grafit"],
  black: ["black", "μαυρ", "svart"],
  red: ["red", "κόκ", "κοκκ", "röd", "bordo", "burgundy", "bordeaux"],
  pink: ["pink", "ροζ", "fuchsia", "φουξ", "rosa"],
  purple: ["purple", "μωβ", "λιλά", "lila", "violet"],
  beige: ["beige", "μπεζ", "sand", "εκρού", "ecru"],
  brown: ["brown", "καφέ", "brun", "camel", "tabac"],
  yellow: ["yellow", "κίτρ", "κιτρ", "gul", "mustard", "μουσταρδ"],
  orange: ["orange", "πορτοκαλ"],
  green: ["green", "πράσ", "πρασ", "grön", "khaki", "χακί", "olive"],
  blue: ["blue", "μπλε", "blå", "navy", "γαλάζ", "σιελ"],
  turquoise: ["turquoise", "τυρκ", "turkos", "petrol", "aqua"],
  silver: ["silver", "ασημ"],
  gold: ["gold", "χρυσ", "guld", "ochre", "ώχρα"],
  multicolor: ["multi", "multicolor", "πολύχρ", "flerfär"],
};

const normalizeText = (value: string) =>
  value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .trim()
    .toLowerCase();

const normalizeCodeText = (value: string) => normalizeText(value).replace(/[^\p{L}\p{N}]/gu, "");

const splitCodeFragments = (value: string) =>
  value
    .split(/[\s\-_/.,]+/g)
    .map(normalizeCodeText)
    .filter((token) => token.length > 0);

const matchesColorFilter = (product: ProductListItem, selectedColors: string[]) => {
  if (selectedColors.length === 0) {
    return true;
  }

  const colorValues = Object.entries(product.attributes)
    .filter(([key]) => isColorAttributeKey(key))
    .flatMap(([, values]) => values.map((value) => normalizeText(value)));

  if (colorValues.length === 0) {
    return false;
  }

  return selectedColors.some((selectedColor) => {
    const aliases = colorValueAliases[normalizeText(selectedColor)] ?? [normalizeText(selectedColor)];
    return colorValues.some((value) => aliases.some((alias) => value.includes(alias)));
  });
};

const matchesAttributeFilters = (
  product: ProductListItem,
  attributeFilters: Record<string, string[]>
) => {
  const activeFilters = Object.entries(attributeFilters).filter(([, values]) => values.length > 0);
  if (activeFilters.length === 0) {
    return true;
  }

  return activeFilters.every(([key, values]) => {
    const productValues =
      Object.entries(product.attributes).find(([productKey]) =>
        normalizeText(productKey) === normalizeText(key)
      )?.[1] ?? [];

    if (productValues.length === 0) {
      return false;
    }

    return values.some((value) =>
      productValues.some((productValue) => normalizeText(productValue) === normalizeText(value))
    );
  });
};

const matchesSearchFilter = (product: ProductListItem, search?: string) => {
  if (!search) {
    return true;
  }

  const textNeedle = normalizeText(search);
  const codeNeedle = normalizeCodeText(search);
  const searchableValues = [
    String(product.id),
    product.name ?? "",
    product.reference ?? "",
    ...Object.keys(product.attributes),
    ...Object.values(product.attributes).flat(),
  ];
  const textHaystack = searchableValues.map(normalizeText).join(" ");
  const codeHaystack = searchableValues.map(normalizeCodeText).join("");
  const textTokens = textNeedle.split(/\s+/g).filter((token) => token.length > 0);
  const codeTokens = splitCodeFragments(search);

  return (
    textHaystack.includes(textNeedle) ||
    textTokens.every((token) => textHaystack.includes(token)) ||
    (codeNeedle.length > 0 && codeHaystack.includes(codeNeedle)) ||
    (codeTokens.length > 0 && codeTokens.every((token) => codeHaystack.includes(token)))
  );
};

const applyBasicProductFilters = (
  products: ProductListItem[],
  filters: BasicProductFilters = {}
) => {
  const selectedColors = filters.colors?.filter((value) => value.trim() !== "") ?? [];
  const attributeFilters = Object.entries(filters.attributeFilters ?? {}).reduce<Record<string, string[]>>(
    (acc, [key, values]) => {
      const normalizedValues = values.filter((value) => value.trim() !== "");
      if (key.trim() !== "" && normalizedValues.length > 0) {
        acc[key] = normalizedValues;
      }
      return acc;
    },
    {}
  );

  return products.filter((product) => {
    const matchesPriceMin =
      filters.priceMin === undefined || product.price === null || product.price >= filters.priceMin;
    const matchesPriceMax =
      filters.priceMax === undefined || product.price === null || product.price <= filters.priceMax;

    return (
      matchesSearchFilter(product, filters.search) &&
      matchesPriceMin &&
      matchesPriceMax &&
      matchesColorFilter(product, selectedColors) &&
      matchesAttributeFilters(product, attributeFilters)
    );
  });
};

const hasBasicProductFilters = (filters: BasicProductFilters = {}) => {
  return Boolean(
    filters.search ||
      filters.priceMin !== undefined ||
      filters.priceMax !== undefined ||
      (filters.colors?.length ?? 0) > 0 ||
      Object.values(filters.attributeFilters ?? {}).some((values) => values.length > 0)
  );
};

const sortProducts = (products: ProductListItem[], sort: string) => {
  const items = [...products];
  if (sort.includes("price")) {
    const desc = sort.includes("DESC");
    items.sort((a, b) => {
      const priceA = a.price ?? (desc ? Number.NEGATIVE_INFINITY : Number.POSITIVE_INFINITY);
      const priceB = b.price ?? (desc ? Number.NEGATIVE_INFINITY : Number.POSITIVE_INFINITY);
      return desc ? priceB - priceA : priceA - priceB;
    });
    return items;
  }

  if (sort.includes("name")) {
    const desc = sort.includes("DESC");
    items.sort((a, b) => {
      const nameA = a.name ?? "";
      const nameB = b.name ?? "";
      return desc ? nameB.localeCompare(nameA) : nameA.localeCompare(nameB);
    });
    return items;
  }

  const desc = !sort.includes("ASC");
  items.sort((a, b) => (desc ? b.id - a.id : a.id - b.id));
  return items;
};

const paginateProducts = (products: ProductListItem[], page: number, pageSize: number) => {
  const start = (page - 1) * pageSize;
  return products.slice(start, start + pageSize);
};

const chunkArray = <T>(items: T[], size: number): T[][] => {
  const chunks: T[][] = [];
  for (let index = 0; index < items.length; index += size) {
    chunks.push(items.slice(index, index + size));
  }
  return chunks;
};

const fetchNormalizedProductsByIds = async (
  client: PrestaShopClient,
  shopId: ShopId,
  productIds: number[],
  lang?: number,
  allowPrice = true
) => {
  if (productIds.length === 0) {
    return [];
  }

  const rawProducts: any[] = [];
  for (const batch of chunkArray(productIds, 100)) {
    const batchData = await client.get("products", {
      "filter[id]": `[${batch.join("|")}]`,
      "filter[active]": 1,
      display: "full",
      limit: batch.length.toString(),
    });
    rawProducts.push(...extractResourceList<any>("products", batchData));
  }

  const attributeLookup = await buildProductAttributeLookup(client, rawProducts, lang);
  return rawProducts.map((product) =>
    normalizeProduct(
      product,
      shopId,
      lang,
      allowPrice,
      attributeLookup.get(Number(product.id)) ?? {}
    )
  );
};

export const buildCatalogFacets = (
  products: ProductListItem[],
  lang?: number
): CatalogFacet[] => {
  const facets: CatalogFacet[] = [];

  const colorValues = products.flatMap((product) =>
    Object.entries(product.attributes)
      .filter(([key]) => isColorAttributeKey(key))
      .flatMap(([, values]) => values)
  );
  const colorCounts = countByValue(colorValues);
  if (colorCounts.size > 0) {
    facets.push({
      key: "colors",
      title: lang === 2 ? "Χρωματισμοί" : "Colors",
      type: "color",
      options: Array.from(colorCounts.entries())
        .map(([value, count]) => ({
          key: value,
          label: value,
          count,
        }))
        .sort((a, b) => a.label.localeCompare(b.label)),
    });
  }

  const attributeMap = new Map<string, string[]>();
  products.forEach((product) => {
    Object.entries(product.attributes).forEach(([key, values]) => {
      if (key.toLowerCase() === "reference" || isColorAttributeKey(key)) {
        return;
      }
      attributeMap.set(key, [...(attributeMap.get(key) ?? []), ...values]);
    });
  });

  Array.from(attributeMap.entries())
    .sort((a, b) => a[0].localeCompare(b[0]))
    .forEach(([key, values]) => {
      const counts = countByValue(values);
      if (counts.size === 0) {
        return;
      }
      facets.push({
        key,
        title: key,
        type: "attribute",
        options: Array.from(counts.entries())
          .map(([value, count]) => ({ key: value, label: value, count }))
          .sort((a, b) => a.label.localeCompare(b.label)),
      });
    });

  const prices = products.map((product) => product.price).filter((value): value is number => value !== null);
  if (prices.length > 0) {
    facets.push({
      key: "price",
      title: lang === 2 ? "Τιμή" : "Price",
      type: "price",
      options: [],
      minValue: Math.min(...prices),
      maxValue: Math.max(...prices),
    });
  }

  return facets;
};

export const getProductDetail = async (
  client: PrestaShopClient,
  shopId: ShopId,
  productId: number,
  lang?: number,
  allowPrice = true
): Promise<any> => {
  const data = await client.getById("products", productId, { display: "full" });
  const product = extractResourceItem<any>("products", data);
  if (!product) return null;
  const attributeLookup = await buildProductAttributeLookup(client, [product], lang);
  return normalizeProduct(
    product,
    shopId,
    lang,
    allowPrice,
    attributeLookup.get(Number(product.id)) ?? {}
  );
};
