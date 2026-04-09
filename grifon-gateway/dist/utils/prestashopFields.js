"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.toBooleanFlag = exports.toNumber = exports.getLocalizedValue = void 0;
const getLocalizedValue = (field, lang) => {
    if (!field)
        return null;
    if (typeof field === "string")
        return field;
    const language = field.language;
    if (!language)
        return null;
    const list = Array.isArray(language) ? language : [language];
    if (lang !== undefined) {
        const match = list.find((item) => Number(item.id) === lang);
        return match?.value ?? match?.text ?? null;
    }
    const first = list[0];
    return first?.value ?? first?.text ?? null;
};
exports.getLocalizedValue = getLocalizedValue;
const normalizeNumericString = (value) => {
    const trimmed = value.trim();
    if (!trimmed) {
        return null;
    }
    const sanitized = trimmed
        .replace(/\s+/g, "")
        .replace(/[^\d,.\-]/g, "");
    if (!sanitized || sanitized === "-" || sanitized === "," || sanitized === ".") {
        return null;
    }
    const lastComma = sanitized.lastIndexOf(",");
    const lastDot = sanitized.lastIndexOf(".");
    if (lastComma >= 0 && lastDot >= 0) {
        const decimalIndex = Math.max(lastComma, lastDot);
        const integerPart = sanitized.slice(0, decimalIndex).replace(/[.,]/g, "");
        const fractionalPart = sanitized.slice(decimalIndex + 1).replace(/[.,]/g, "");
        return fractionalPart ? `${integerPart}.${fractionalPart}` : integerPart;
    }
    if (lastComma >= 0) {
        const fractionalDigits = sanitized.length - lastComma - 1;
        if (fractionalDigits >= 1 && fractionalDigits <= 6) {
            return sanitized.replace(/\./g, "").replace(",", ".");
        }
        return sanitized.replace(/,/g, "");
    }
    if (lastDot >= 0) {
        const fractionalDigits = sanitized.length - lastDot - 1;
        const dotCount = (sanitized.match(/\./g) ?? []).length;
        if (dotCount > 1) {
            const integerPart = sanitized.slice(0, lastDot).replace(/\./g, "");
            const fractionalPart = sanitized.slice(lastDot + 1).replace(/\./g, "");
            return fractionalPart ? `${integerPart}.${fractionalPart}` : integerPart;
        }
        if (fractionalDigits >= 1 && fractionalDigits <= 6) {
            return sanitized;
        }
        return sanitized.replace(/\./g, "");
    }
    return sanitized;
};
const toNumber = (value) => {
    if (value === undefined || value === null || value === "")
        return null;
    if (typeof value === "number") {
        return Number.isFinite(value) ? value : null;
    }
    const normalized = normalizeNumericString(String(value));
    if (!normalized) {
        return null;
    }
    const num = Number(normalized);
    return Number.isNaN(num) ? null : num;
};
exports.toNumber = toNumber;
const toBooleanFlag = (value) => {
    if (value === true || value === 1 || value === "1")
        return true;
    return false;
};
exports.toBooleanFlag = toBooleanFlag;
