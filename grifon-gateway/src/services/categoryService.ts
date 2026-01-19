import { PrestaShopClient } from "../clients/PrestaShopClient";
import { extractResourceList } from "./prestashopParser";
import { getLocalizedValue, toNumber } from "../utils/prestashopFields";
import { toLimitParam } from "../utils/pagination";

export interface CategoryItem {
  id: number;
  parentId: number | null;
  name: string | null;
  position: number | null;
  active: number | null;
  slug: string | null;
}

export interface CategoryTreeNode extends CategoryItem {
  children: CategoryTreeNode[];
}

export const listCategories = async (
  client: PrestaShopClient,
  page: number,
  pageSize: number,
  lang?: number
): Promise<{ items: CategoryItem[]; tree: CategoryTreeNode[] }> => {
  const data = await client.get("categories", {
    "filter[active]": 1,
    sort: "[position_ASC]",
    limit: toLimitParam(page, pageSize)
  });

  const categories = extractResourceList<any>("categories", data);
  const items: CategoryItem[] = categories.map((category) => ({
    id: Number(category.id),
    parentId: toNumber(category.id_parent),
    name: getLocalizedValue(category.name, lang),
    position: toNumber(category.position),
    active: toNumber(category.active),
    slug: getLocalizedValue(category.link_rewrite, lang)
  }));

  const nodeMap = new Map<number, CategoryTreeNode>();
  items.forEach((item) => {
    nodeMap.set(item.id, { ...item, children: [] });
  });

  const tree: CategoryTreeNode[] = [];
  nodeMap.forEach((node) => {
    if (node.parentId && nodeMap.has(node.parentId)) {
      nodeMap.get(node.parentId)?.children.push(node);
    } else {
      tree.push(node);
    }
  });

  return { items, tree };
};
