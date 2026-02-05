"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.PrestaShopClient = void 0;
const axios_1 = __importDefault(require("axios"));
const env_1 = require("../config/env");
const xml_1 = require("../utils/xml");
class PrestaShopClient {
    constructor(options) {
        this.shopId = options.shopId;
        this.lang = options.lang;
        const baseURL = options.shopId === env_1.config.defaultShopId && env_1.config.prestashopBaseUrl
            ? env_1.config.prestashopBaseUrl
            : env_1.config.shopBaseUrls[options.shopId];
        this.client = axios_1.default.create({
            baseURL,
            timeout: env_1.config.timeoutMs,
            auth: {
                username: env_1.config.prestashopApiKey,
                password: ""
            }
        });
    }
    parsePayload(payload) {
        if (typeof payload !== "string") {
            return payload;
        }
        const trimmed = payload.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                return JSON.parse(payload);
            }
            catch {
                // Fall back to XML parsing below.
            }
        }
        return (0, xml_1.parseXmlToJson)(payload);
    }
    async get(resource, params) {
        const response = await this.request("get", `/${resource}`, params);
        return response;
    }
    async getById(resource, id, params) {
        const response = await this.request("get", `/${resource}/${id}`, params);
        return response;
    }
    async postXml(resource, xmlBody, params) {
        const response = await this.request("post", `/${resource}`, params, xmlBody, {
            "Content-Type": "application/xml"
        });
        return response;
    }
    async request(method, url, params, data, headers) {
        const queryParams = {
            output_format: "JSON",
            id_shop: this.shopId,
            ...params
        };
        if (this.lang !== undefined) {
            queryParams["language"] = this.lang;
        }
        const maxAttempts = method === "get" ? 3 : 1;
        let lastError;
        for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
            try {
                const response = await this.client.request({
                    method,
                    url,
                    params: queryParams,
                    data,
                    headers
                });
                return this.parsePayload(response.data);
            }
            catch (error) {
                lastError = error;
                const axiosError = error;
                if (axiosError.response) {
                    const responseData = axiosError.response.data;
                    const parsed = this.parsePayload(responseData);
                    const message = parsed?.prestashop?.errors?.error?.message ??
                        parsed?.prestashop?.errors?.error?.[0]?.message ??
                        axiosError.message;
                    throw {
                        status: axiosError.response.status,
                        code: "PRESTASHOP_ERROR",
                        message,
                        details: parsed
                    };
                }
                if (attempt === maxAttempts) {
                    break;
                }
                if (method !== "get") {
                    break;
                }
                await new Promise((resolve) => setTimeout(resolve, 250 * attempt));
            }
        }
        // eslint-disable-next-line no-console
        console.error("PrestaShop upstream request failed", {
            method,
            url,
            shopId: this.shopId,
            attempts: maxAttempts,
            error: lastError?.message
        });
        throw {
            status: 502,
            code: "UPSTREAM_ERROR",
            message: lastErrorMessage
                ? `Failed to fetch upstream data: ${lastErrorMessage}`
                : "Failed to fetch upstream data",
            details: lastErrorMessage
        };
    }
}
exports.PrestaShopClient = PrestaShopClient;
