const normalizeGroupName = (value: string): string => {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim();
};

export const hasWholesaleKeyword = (groupName: string | null | undefined): boolean => {
  if (typeof groupName !== "string") {
    return false;
  }

  const normalized = normalizeGroupName(groupName);
  return normalized.includes("wholesale") || normalized.includes("χονδρ");
};
