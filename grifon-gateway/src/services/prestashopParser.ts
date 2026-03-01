const resourceMap: Record<string, string> = {
  categories: "category",
  products: "product",
  customers: "customer",
  groups: "group",
  content_management_system: "content_management_system",
  images: "image",
  stock_availables: "stock_available"
};

const asArray = <T>(value: T | T[] | undefined): T[] => {
  if (!value) return [];
  return Array.isArray(value) ? value : [value];
};

export const extractResourceList = <T = Record<string, unknown>>(
  resource: string,
  payload: any
): T[] => {
  const root = payload?.prestashop ?? payload;

  // Προσπάθεια εύρεσης στον πληθυντικό (π.χ. payload.categories.category)
  const container = root?.[resource];
  if (container) {
    const itemKey = resourceMap[resource];
    if (itemKey && container[itemKey]) {
      return asArray(container[itemKey]);
    }
    if (Array.isArray(container)) return container as T[];
  }

  // Προσπάθεια εύρεσης στον ενικό (π.χ. payload.category) - για getById
  const singularKey = resourceMap[resource];
  if (singularKey && root?.[singularKey]) {
    return asArray(root[singularKey]);
  }

  return [];
};

export const extractResourceItem = <T = Record<string, unknown>>(
  resource: string,
  payload: any
): T | null => {
  const list = extractResourceList<T>(resource, payload);
  return list.length > 0 ? list[0] : null;
};
