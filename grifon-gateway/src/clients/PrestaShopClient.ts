import axios, { AxiosInstance, AxiosResponse, AxiosError } from "axios";
import { config, ShopId } from "../config/env";
import { parseXmlToJson } from "../utils/xml";

export interface PrestaShopClientOptions {
  shopId: ShopId;
  lang?: number;
}

export class PrestaShopClient {
  private client: AxiosInstance;
  private shopId: ShopId;
  private lang?: number;

  constructor(options: PrestaShopClientOptions) {
    this.shopId = options.shopId;
    this.lang = options.lang;
    const baseURL =
      options.shopId === config.defaultShopId && config.prestashopBaseUrl
        ? config.prestashopBaseUrl
        : config.shopBaseUrls[options.shopId];

    this.client = axios.create({
      baseURL,
      timeout: config.timeoutMs,
      auth: {
        username: config.prestashopApiKey,
        password: ""
      }
    });
  }

  private parsePayload(payload: unknown): unknown {
    if (typeof payload !== "string") {
      return payload;
    }
    const trimmed = payload.trim();
    if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
      try {
        return JSON.parse(payload);
      } catch {
        // Fall back to XML parsing below.
      }
    }
    return parseXmlToJson(payload);
  }

  async get(resource: string, params?: Record<string, string | number>): Promise<unknown> {
    const response = await this.request("get", `/${resource}`, params);
    return response;
  }

  async getById(resource: string, id: string | number, params?: Record<string, string | number>): Promise<unknown> {
    const response = await this.request("get", `/${resource}/${id}`, params);
    return response;
  }

  async postXml(
    resource: string,
    xmlBody: string,
    params?: Record<string, string | number>
  ): Promise<unknown> {
    const response = await this.request("post", `/${resource}`, params, xmlBody, {
      "Content-Type": "application/xml"
    });
    return response;
  }

  private async request(
    method: "get" | "post",
    url: string,
    params?: Record<string, string | number>,
    data?: unknown,
    headers?: Record<string, string>
  ): Promise<unknown> {
    const queryParams: Record<string, string | number> = {
      output_format: "JSON",
      id_shop: this.shopId,
      ...params
    };

    if (this.lang !== undefined) {
      queryParams["language"] = this.lang;
    }

    const maxAttempts = method === "get" ? 3 : 1;
    let lastError: unknown;

    for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
      try {
        const response: AxiosResponse = await this.client.request({
          method,
          url,
          params: queryParams,
          data,
          headers
        });
        return this.parsePayload(response.data);
      } catch (error) {
        lastError = error;
        const axiosError = error as AxiosError;
        if (axiosError.response) {
          const responseData = axiosError.response.data;
          const parsed = this.parsePayload(responseData);
          const message =
            (parsed as any)?.prestashop?.errors?.error?.message ??
            (parsed as any)?.prestashop?.errors?.error?.[0]?.message ??
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

    const lastErrorMessage = (lastError as Error)?.message;
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
