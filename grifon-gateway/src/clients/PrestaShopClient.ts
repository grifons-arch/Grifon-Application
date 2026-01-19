import axios, { AxiosInstance, AxiosResponse } from "axios";
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
    const baseURL = config.shopBaseUrls[options.shopId];

    this.client = axios.create({
      baseURL,
      timeout: config.timeoutMs,
      auth: {
        username: config.prestashopApiKey,
        password: ""
      }
    });
  }

  async get(resource: string, params?: Record<string, string | number>): Promise<unknown> {
    const response = await this.request("get", `/${resource}`, params);
    return response;
  }

  async getById(resource: string, id: string | number, params?: Record<string, string | number>): Promise<unknown> {
    const response = await this.request("get", `/${resource}/${id}`, params);
    return response;
  }

  private async request(
    method: "get",
    url: string,
    params?: Record<string, string | number>
  ): Promise<unknown> {
    const queryParams: Record<string, string | number> = {
      output_format: "JSON",
      id_shop: this.shopId,
      ...params
    };

    if (this.lang !== undefined) {
      queryParams["language"] = this.lang;
    }

    const maxAttempts = 3;
    let lastError: unknown;

    for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
      try {
        const response: AxiosResponse = await this.client.request({
          method,
          url,
          params: queryParams
        });
        if (typeof response.data === "string") {
          return parseXmlToJson(response.data);
        }
        return response.data;
      } catch (error) {
        lastError = error;
        if (attempt === maxAttempts) {
          break;
        }
        await new Promise((resolve) => setTimeout(resolve, 250 * attempt));
      }
    }

    throw {
      status: 502,
      code: "UPSTREAM_ERROR",
      message: "Failed to fetch upstream data",
      details: (lastError as Error)?.message
    };
  }
}
