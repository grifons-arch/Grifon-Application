export const toLimitParam = (page: number, pageSize: number): string => {
  const offset = (page - 1) * pageSize;
  return `${offset},${pageSize}`;
};

export const chunkArray = <T>(items: T[], size: number): T[][] => {
  const chunks: T[][] = [];
  for (let i = 0; i < items.length; i += size) {
    chunks.push(items.slice(i, i + size));
  }
  return chunks;
};
