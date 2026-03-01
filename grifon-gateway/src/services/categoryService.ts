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
  try {
    const data = await client.get("categories", {
      "filter[active]": 1,
      display: "full",
      sort: "[id_ASC]",
      limit: toLimitParam(page, pageSize)
    });

    const categories = extractResourceList<any>("categories", data);
    if (!categories || categories.length === 0) {
        return { items: [], tree: [] };
    }

    const items: CategoryItem[] = categories.map((category) => ({
      id: Number(category.id),
      parentId: category.id_parent ? toNumber(category.id_parent) : null,
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
      // Root categories in PS usually have parent ID 0, 1 (Root) or 2 (Home)
      if (node.parentId && node.parentId > 2 && nodeMap.has(node.parentId)) {
        nodeMap.get(node.parentId)?.children.push(node);
      } else if (node.id > 2) {
        tree.push(node);
      }
    });

    return { items, tree };
  } catch (error: any) {
    console.error("Category fetch failed:", {
        status: error.status,
        message: error.message,
        details: error.details
    });
    return { items: [], tree: [] };
  }
};
