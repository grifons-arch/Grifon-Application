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
  const container = root?.[resource];
  if (!container) return [];
  const itemKey = resourceMap[resource];
  if (itemKey && container[itemKey]) {
    return asArray(container[itemKey]);
  }
  if (Array.isArray(container)) return container as T[];
  return asArray(container as T);
};

export const extractResourceItem = <T = Record<string, unknown>>(
  resource: string,
  payload: any
): T | null => {
  const list = extractResourceList<T>(resource, payload);
  if (list.length > 0) return list[0];
  const root = payload?.prestashop ?? payload;
  const direct = root?.[resource];
  if (!direct) return null;
  return direct as T;
};
