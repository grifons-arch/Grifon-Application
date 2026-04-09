const WHOLESALE_KEYWORDS = [
  "wholesale",
  "wholesaler",
  "wholesales",
  "whalesale",
  "whalesales",
  "χονδρ",
  "grossist",
  "b2b"
];

export const hasWholesaleKeyword = (name: string | null | undefined): boolean => {
  const normalized = name?.trim().toLowerCase() ?? "";
  if (!normalized) {
    return false;
  }

  return WHOLESALE_KEYWORDS.some((keyword) => normalized.includes(keyword));
};

export const filterWholesaleGroupNames = (names: Array<string | null | undefined>): string[] =>
  names.filter((name): name is string => typeof name === "string" && hasWholesaleKeyword(name));
