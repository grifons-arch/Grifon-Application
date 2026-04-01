import { z } from "zod";

const toNumber = (value: unknown) => {
  if (value === undefined || value === null || value === "") return undefined;
  const parsed = Number(value);
  return Number.isNaN(parsed) ? value : parsed;
};

const toOptionalString = (value: unknown) => {
  if (value === undefined || value === null) return undefined;
  if (typeof value !== "string") return value;
  const trimmed = value.trim();
  return trimmed === "" ? undefined : trimmed;
};

const toOptionalSocialTitle = (value: unknown) => {
  const normalizedValue = toOptionalString(value);
  if (normalizedValue === undefined || typeof normalizedValue !== "string") {
    return normalizedValue;
  }

  switch (normalizedValue.toLowerCase()) {
    case "mr":
    case "m":
    case "κος":
    case "κος.":
      return "mr";
    case "mrs":
    case "ms":
    case "f":
    case "κα":
    case "κα.":
      return "mrs";
    default:
      return normalizedValue;
  }
};

export const shopQuerySchema = z.object({
  shopId: z.preprocess(toNumber, z.union([z.literal(1), z.literal(4)])).default(4),
  lang: z.preprocess(toNumber, z.number().int().positive().optional())
});

export const paginationSchema = z.object({
  page: z.preprocess(toNumber, z.number().int().min(1).max(1000)).default(1),
  pageSize: z.preprocess(toNumber, z.number().int().min(1).max(1000)).default(100)
});

export const productPaginationSchema = z.object({
  page: z.preprocess(toNumber, z.number().int().min(1).max(1000)).default(1),
  pageSize: z.preprocess(toNumber, z.number().int().min(1).max(1000)).default(100),
  sort: z.string().optional().default("[id_DESC]")
});

export const customerIdSchema = z.object({
  customerId: z.preprocess(toNumber, z.number().int().positive())
});

export const categoryIdSchema = z.object({
  categoryId: z.preprocess(toNumber, z.number().int().positive())
});

export const productIdSchema = z.object({
  productId: z.preprocess(toNumber, z.number().int().positive())
});

export const moduleNameSchema = z.object({
  moduleName: z.string().trim().regex(/^[A-Za-z0-9_-]+$/)
});

export const tableNameSchema = z.object({
  tableName: z.string().trim().regex(/^[A-Za-z0-9_]+$/)
});

export const moduleSearchQuerySchema = z.object({
  pattern: z.string().trim().min(1).max(120)
});

export const loginBodySchema = z.object({
  email: z.string().trim().email(),
  password: z.string().min(1),
  countryIso: z.string().trim().length(2).optional()
});

export const registerBodySchema = z
  .object({
    email: z.string().trim().email(),
    password: z.string().min(8).optional(),
    passwd: z.string().min(8).optional(),
    socialTitle: z.preprocess(toOptionalSocialTitle, z.enum(["mr", "mrs"]).optional()),
    firstName: z.string().trim().min(1),
    lastName: z.string().trim().min(1),
    countryIso: z.string().trim().length(2),
    street: z.string().trim().min(1),
    city: z.string().trim().min(1),
    postalCode: z.string().trim().min(1),
    phone: z.string().trim().min(1).optional(),
    company: z.string().trim().min(1).optional(),
    vatNumber: z.string().trim().min(1).optional(),
    dni: z.string().trim().min(1).optional(), // ΠΡΟΣΘΗΚΗ DNI
    iban: z.string().trim().min(1).optional(),
    customerDataPrivacyAccepted: z.boolean().optional().default(false),
    newsletter: z.boolean().optional().default(false),
    termsAndPrivacyAccepted: z.boolean().optional().default(false),
    wholesaleRequested: z.boolean().optional().default(false),
    partnerOffers: z.boolean().optional()
  })
  .superRefine((data, context) => {
    if (!data.password && !data.passwd) {
      context.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["password"],
        message: "Required"
      });
    }
  })
  .transform((data) => ({
    ...data,
    password: (data.password ?? data.passwd) as string
  }));

export const productActivityBodySchema = z.object({
  customerId: z.preprocess(toNumber, z.number().int().positive()),
  shopId: z.preprocess(toNumber, z.union([z.literal(1), z.literal(4)])),
  productId: z.preprocess(toNumber, z.number().int().positive()),
  isFavorite: z.boolean().optional(),
  product: z.object({
    title: z.preprocess(toOptionalString, z.string().optional()),
    price: z.preprocess(toNumber, z.number().nonnegative().optional()),
    currency: z.preprocess(toOptionalString, z.string().optional()),
    imageUrl: z.preprocess(toOptionalString, z.string().optional()),
    brand: z.preprocess(toOptionalString, z.string().optional())
  }).optional()
});

export const customerActivityQuerySchema = z.object({
  customerId: z.preprocess(toNumber, z.number().int().positive()),
  shopId: z.preprocess(toNumber, z.union([z.literal(1), z.literal(4)])),
  limit: z.preprocess(toNumber, z.number().int().min(1).max(100).optional())
});

export const customerActivityClearBodySchema = z.object({
  shopId: z.preprocess(toNumber, z.union([z.literal(1), z.literal(4)]))
});
