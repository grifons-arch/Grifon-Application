export const getLocalizedValue = (field: any, lang?: number): string | null => {
  if (!field) return null;
  if (typeof field === "string") return field;
  const language = field.language;
  if (!language) return null;
  const list = Array.isArray(language) ? language : [language];
  if (lang !== undefined) {
    const match = list.find((item) => Number(item.id) === lang);
    return match?.value ?? match?.text ?? null;
  }
  const first = list[0];
  return first?.value ?? first?.text ?? null;
};

export const toNumber = (value: any): number | null => {
  if (value === undefined || value === null || value === "") return null;
  const num = Number(value);
  return Number.isNaN(num) ? null : num;
};

export const toBooleanFlag = (value: any): boolean => {
  if (value === true || value === 1 || value === "1") return true;
  return false;
};
