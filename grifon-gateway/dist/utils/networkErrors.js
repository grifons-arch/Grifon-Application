"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.normalizeNetworkErrorMessage = void 0;
const normalizeNetworkErrorMessage = (error, options = {}) => {
    const axiosError = error;
    const code = axiosError.code ?? axiosError.cause?.code;
    const hostname = axiosError.hostname ?? axiosError.cause?.hostname ?? options.fallbackHostname;
    if (code === "ENOTFOUND") {
        return hostname
            ? `Unable to resolve upstream hostname: ${hostname}`
            : "Unable to resolve upstream hostname";
    }
    if (code === "ECONNREFUSED") {
        return "Upstream service refused the connection";
    }
    if (code === "ETIMEDOUT" || code === "ECONNABORTED") {
        return "Upstream service timed out";
    }
    if (axiosError.message) {
        return axiosError.message;
    }
    if (typeof error === "string") {
        return error;
    }
    return undefined;
};
exports.normalizeNetworkErrorMessage = normalizeNetworkErrorMessage;
