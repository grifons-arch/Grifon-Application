import { AxiosError } from "axios";

export const normalizeNetworkErrorMessage = (error: unknown): string | undefined => {
  const axiosError = error as AxiosError & {
    code?: string;
    hostname?: string;
    cause?: { code?: string; hostname?: string; message?: string };
  };

  const code = axiosError.code ?? axiosError.cause?.code;
  const hostname = axiosError.hostname ?? axiosError.cause?.hostname;

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
