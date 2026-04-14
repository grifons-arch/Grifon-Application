"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.filterWholesaleGroupNames = exports.hasWholesaleKeyword = void 0;
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
const hasWholesaleKeyword = (name) => {
    const normalized = name?.trim().toLowerCase() ?? "";
    if (!normalized) {
        return false;
    }
    return WHOLESALE_KEYWORDS.some((keyword) => normalized.includes(keyword));
};
exports.hasWholesaleKeyword = hasWholesaleKeyword;
const filterWholesaleGroupNames = (names) => names.filter((name) => typeof name === "string" && (0, exports.hasWholesaleKeyword)(name));
exports.filterWholesaleGroupNames = filterWholesaleGroupNames;
