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
    // Φέρνουμε όλες τις κατηγορίες (χωρίς active filter για το sync) 
    // με display=full για να έχουμε το id_parent
    const data = await client.get("categories", {
      display: "full",
      limit: toLimitParam(page, pageSize)
    });

    const categories = extractResourceList<any>("categories", data);
    console.log(`[Gateway] PrestaShop returned ${categories.length} raw categories`);

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

    // Build tree
    const nodeMap = new Map<number, CategoryTreeNode>();
    items.forEach((item) => {
      nodeMap.set(item.id, { ...item, children: [] });
    });

    const tree: CategoryTreeNode[] = [];
    nodeMap.forEach((node) => {
      if (node.parentId && nodeMap.has(node.parentId) && node.parentId !== node.id) {
        nodeMap.get(node.parentId)?.children.push(node);
      } else {
        tree.push(node);
      }
    });

    return { items, tree };
  } catch (error) {
    console.error("[Gateway] Category fetch failed:", error);
    return { items: [], tree: [] };
  }
};
