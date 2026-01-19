import { env } from '../config/env.js';
import { parseXmlToJson } from '../utils/prestashop.js';

export class PrestaShopError extends Error {
  status: number;
  constructor(message: string, status: number) {
    super(message);
    this.status = status;
  }
}

export class PrestaShopClient {
  private readonly baseUrl: string;
  private readonly apiKey: string;
  private readonly timeoutMs: number;

  constructor(baseUrl = env.PRESTASHOP_BASE_URL, apiKey = env.PRESTASHOP_API_KEY, timeoutMs = 10000) {
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.timeoutMs = timeoutMs;
  }

  private buildAuthHeader() {
    const token = Buffer.from(`${this.apiKey}:`).toString('base64');
    return `Basic ${token}`;
  }

  async get<T>(path: string, params: Record<string, string | number | undefined> = {}): Promise<T> {
    const url = new URL(path, this.baseUrl.endsWith('/') ? this.baseUrl : `${this.baseUrl}/`);
    const searchParams = new URLSearchParams();
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        searchParams.set(key, String(value));
      }
    });
    if (!searchParams.has('output_format')) {
      searchParams.set('output_format', 'JSON');
    }
    url.search = searchParams.toString();

    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), this.timeoutMs);

    try {
      const response = await fetch(url, {
        headers: {
          Authorization: this.buildAuthHeader()
        },
        signal: controller.signal
      });

      if (!response.ok) {
        throw new PrestaShopError(`PrestaShop responded with status ${response.status}`, response.status);
      }

      const contentType = response.headers.get('content-type') ?? '';
      if (contentType.includes('application/json')) {
        return (await response.json()) as T;
      }

      const text = await response.text();
      return parseXmlToJson(text) as T;
    } catch (error) {
      if (error instanceof PrestaShopError) {
        throw error;
      }
      if (error instanceof DOMException && error.name === 'AbortError') {
        throw new PrestaShopError('PrestaShop request timed out', 504);
      }
      throw new PrestaShopError('PrestaShop request failed', 502);
    } finally {
      clearTimeout(timeout);
    }
  }
}
