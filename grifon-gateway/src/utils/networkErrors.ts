import { AxiosError } from "axios";

interface NormalizeNetworkErrorOptions {
  fallbackHostname?: string;
}

export const normalizeNetworkErrorMessage = (
  error: unknown,
  options: NormalizeNetworkErrorOptions = {}
): string | undefined => {
  const axiosError = error as AxiosError & {
    code?: string;
    hostname?: string;
    cause?: { code?: string; hostname?: string; message?: string };
  };

  const code = axiosError.code ?? axiosError.cause?.code;
  const hostname = axiosError.hostname ?? axiosError.cause?.hostname ?? options.fallbackHostname;

  if (code === "ENOTFOUND") {
    return hostname
      ? `Unable to resolve upstream: ${hostname}`
      : "Unable to resolve upstream";
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
