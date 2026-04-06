"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.customerActivityClearBodySchema = exports.customerActivityQuerySchema = exports.productActivityBodySchema = exports.wholesaleApplicationBodySchema = exports.registerBodySchema = exports.loginBodySchema = exports.etsWholesaleFormFieldsQuerySchema = exports.moduleFileQuerySchema = exports.moduleSearchQuerySchema = exports.tableNameSchema = exports.moduleNameSchema = exports.productIdSchema = exports.categoryIdSchema = exports.customerIdSchema = exports.productFilterQuerySchema = exports.productPaginationSchema = exports.paginationSchema = exports.shopQuerySchema = void 0;
const zod_1 = require("zod");
const toNumber = (value) => {
    if (value === undefined || value === null || value === "")
        return undefined;
    const parsed = Number(value);
    return Number.isNaN(parsed) ? value : parsed;
};
const toOptionalString = (value) => {
    if (value === undefined || value === null)
        return undefined;
    if (typeof value !== "string")
        return value;
    const trimmed = value.trim();
    return trimmed === "" ? undefined : trimmed;
};
const toOptionalSocialTitle = (value) => {
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
exports.shopQuerySchema = zod_1.z.object({
    shopId: zod_1.z.preprocess(toNumber, zod_1.z.union([zod_1.z.literal(1), zod_1.z.literal(4)])).default(4),
    lang: zod_1.z.preprocess(toNumber, zod_1.z.number().int().positive().optional())
});
exports.paginationSchema = zod_1.z.object({
    page: zod_1.z.preprocess(toNumber, zod_1.z.number().int().min(1).max(1000)).default(1),
    pageSize: zod_1.z.preprocess(toNumber, zod_1.z.number().int().min(1).max(1000)).default(100)
});
exports.productPaginationSchema = zod_1.z.object({
    page: zod_1.z.preprocess(toNumber, zod_1.z.number().int().min(1).max(1000)).default(1),
    pageSize: zod_1.z.preprocess(toNumber, zod_1.z.number().int().min(1).max(1000)).default(100),
    sort: zod_1.z.string().optional().default("[id_DESC]")
});
exports.productFilterQuerySchema = zod_1.z.object({
    search: zod_1.z.preprocess(toOptionalString, zod_1.z.string().optional()),
    priceMin: zod_1.z.preprocess(toNumber, zod_1.z.number().nonnegative().optional()),
    priceMax: zod_1.z.preprocess(toNumber, zod_1.z.number().nonnegative().optional()),
    colors: zod_1.z.preprocess(toOptionalString, zod_1.z.string().optional()),
    attributes: zod_1.z.preprocess(toOptionalString, zod_1.z.string().optional())
});
exports.customerIdSchema = zod_1.z.object({
    customerId: zod_1.z.preprocess(toNumber, zod_1.z.number().int().positive())
});
exports.categoryIdSchema = zod_1.z.object({
    categoryId: zod_1.z.preprocess(toNumber, zod_1.z.number().int().positive())
});
exports.productIdSchema = zod_1.z.object({
    productId: zod_1.z.preprocess(toNumber, zod_1.z.number().int().positive())
});
exports.moduleNameSchema = zod_1.z.object({
    moduleName: zod_1.z.string().trim().regex(/^[A-Za-z0-9_-]+$/)
});
exports.tableNameSchema = zod_1.z.object({
    tableName: zod_1.z.string().trim().regex(/^[A-Za-z0-9_]+$/)
});
exports.moduleSearchQuerySchema = zod_1.z.object({
    pattern: zod_1.z.string().trim().min(1).max(120)
});
exports.moduleFileQuerySchema = zod_1.z.object({
    path: zod_1.z.string().trim().min(1).max(300),
    start: zod_1.z.preprocess(toNumber, zod_1.z.number().int().min(1).max(10000)).default(1),
    lines: zod_1.z.preprocess(toNumber, zod_1.z.number().int().min(1).max(200)).default(80)
});
exports.etsWholesaleFormFieldsQuerySchema = zod_1.z.object({
    formType: zod_1.z.preprocess(toOptionalString, zod_1.z.enum(["registration", "add_information"]).optional())
});
exports.loginBodySchema = zod_1.z.object({
    email: zod_1.z.string().trim().email(),
    password: zod_1.z.string().min(1),
    countryIso: zod_1.z.string().trim().length(2).optional()
});
exports.registerBodySchema = zod_1.z
    .object({
    email: zod_1.z.string().trim().email(),
    password: zod_1.z.string().min(8).optional(),
    passwd: zod_1.z.string().min(8).optional(),
    socialTitle: zod_1.z.preprocess(toOptionalSocialTitle, zod_1.z.enum(["mr", "mrs"]).optional()),
    firstName: zod_1.z.string().trim().min(1),
    lastName: zod_1.z.string().trim().min(1),
    countryIso: zod_1.z.string().trim().length(2),
    street: zod_1.z.string().trim().min(1),
    city: zod_1.z.string().trim().min(1),
    postalCode: zod_1.z.string().trim().min(1),
    phone: zod_1.z.string().trim().min(1).optional(),
    company: zod_1.z.string().trim().min(1).optional(),
    vatNumber: zod_1.z.string().trim().min(1).optional(),
    dni: zod_1.z.string().trim().min(1).optional(), // ΠΡΟΣΘΗΚΗ DNI
    iban: zod_1.z.string().trim().min(1).optional(),
    customerDataPrivacyAccepted: zod_1.z.boolean().optional().default(false),
    newsletter: zod_1.z.boolean().optional().default(false),
    termsAndPrivacyAccepted: zod_1.z.boolean().optional().default(false),
    wholesaleRequested: zod_1.z.boolean().optional().default(false),
    partnerOffers: zod_1.z.boolean().optional()
})
    .superRefine((data, context) => {
    if (!data.password && !data.passwd) {
        context.addIssue({
            code: zod_1.z.ZodIssueCode.custom,
            path: ["password"],
            message: "Required"
        });
    }
})
    .transform((data) => ({
    ...data,
    password: (data.password ?? data.passwd)
}));
exports.wholesaleApplicationBodySchema = zod_1.z.object({
    customerId: zod_1.z.preprocess(toNumber, zod_1.z.number().int().positive().optional()),
    email: zod_1.z.string().trim().email(),
    firstName: zod_1.z.string().trim().min(1),
    lastName: zod_1.z.string().trim().min(1),
    contactPersonFullName: zod_1.z.preprocess(toOptionalString, zod_1.z.string().min(1).optional()),
    country: zod_1.z.preprocess(toOptionalString, zod_1.z.string().optional()),
    countryIso: zod_1.z.string().trim().length(2),
    street: zod_1.z.string().trim().min(1),
    city: zod_1.z.string().trim().min(1),
    postalCode: zod_1.z.string().trim().min(1),
    phone: zod_1.z.string().trim().min(1),
    company: zod_1.z.string().trim().min(1),
    vatNumber: zod_1.z.string().trim().min(1),
    addressCoordinates: zod_1.z.preprocess(toOptionalString, zod_1.z.string().optional()),
    companyRegistrationFileName: zod_1.z.preprocess(toOptionalString, zod_1.z.string().optional()),
    invoiceFileName: zod_1.z.preprocess(toOptionalString, zod_1.z.string().optional()),
    customerDataPrivacyAccepted: zod_1.z.literal(true),
    termsAndPrivacyAccepted: zod_1.z.literal(true),
    newsletter: zod_1.z.boolean().optional().default(false),
    partnerOffers: zod_1.z.boolean().optional()
});
exports.productActivityBodySchema = zod_1.z.object({
    customerId: zod_1.z.preprocess(toNumber, zod_1.z.number().int().positive()),
    shopId: zod_1.z.preprocess(toNumber, zod_1.z.union([zod_1.z.literal(1), zod_1.z.literal(4)])),
    productId: zod_1.z.preprocess(toNumber, zod_1.z.number().int().positive()),
    isFavorite: zod_1.z.boolean().optional(),
    product: zod_1.z.object({
        title: zod_1.z.preprocess(toOptionalString, zod_1.z.string().optional()),
        price: zod_1.z.preprocess(toNumber, zod_1.z.number().nonnegative().optional()),
        currency: zod_1.z.preprocess(toOptionalString, zod_1.z.string().optional()),
        imageUrl: zod_1.z.preprocess(toOptionalString, zod_1.z.string().optional()),
        brand: zod_1.z.preprocess(toOptionalString, zod_1.z.string().optional())
    }).optional()
});
exports.customerActivityQuerySchema = zod_1.z.object({
    customerId: zod_1.z.preprocess(toNumber, zod_1.z.number().int().positive()),
    shopId: zod_1.z.preprocess(toNumber, zod_1.z.union([zod_1.z.literal(1), zod_1.z.literal(4)])),
    limit: zod_1.z.preprocess(toNumber, zod_1.z.number().int().min(1).max(100).optional())
});
exports.customerActivityClearBodySchema = zod_1.z.object({
    shopId: zod_1.z.preprocess(toNumber, zod_1.z.union([zod_1.z.literal(1), zod_1.z.literal(4)]))
});
