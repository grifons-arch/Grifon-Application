import { PrestaShopClient } from "../clients/PrestaShopClient";
import { extractResourceList } from "./prestashopParser";
import { getLocalizedValue, toNumber } from "../utils/prestashopFields";

export interface CmsPageItem {
  id: number;
  title: string | null;
  metaTitle: string | null;
  content: string | null;
  active: number | null;
}

export const listPages = async (
  client: PrestaShopClient,
  lang?: number
): Promise<CmsPageItem[]> => {
  const data = await client.get("content_management_system", {
    "filter[active]": 1,
    display: "[id,meta_title,content,active,meta_title,link_rewrite,meta_description,title]"
  });

  const pages = extractResourceList<any>("content_management_system", data);

  return pages.map((page) => ({
    id: Number(page.id),
    title: getLocalizedValue(page.title, lang) ?? getLocalizedValue(page.meta_title, lang),
    metaTitle: getLocalizedValue(page.meta_title, lang),
    content: getLocalizedValue(page.content, lang),
    active: toNumber(page.active)
  }));
};
