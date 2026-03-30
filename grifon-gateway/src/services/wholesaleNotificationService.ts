import { spawn } from "child_process";
import { config } from "../config/env";

export interface WholesaleNotificationRequest {
  email: string;
  firstName: string;
  lastName: string;
  countryIso?: string;
  street?: string;
  city?: string;
  postalCode?: string;
  phone?: string;
  company?: string;
  vatNumber?: string;
  wholesaleRequested?: boolean;
}

export interface WholesaleNotificationResult {
  customerId?: string;
  countryIso?: string;
}

export const notifyWholesaleRequest = async (
  request: WholesaleNotificationRequest,
  result?: WholesaleNotificationResult
): Promise<void> => {
  if (!request.wholesaleRequested) {
    return;
  }

  const recipient = config.wholesaleNotificationTo?.trim();
  if (!recipient) {
    return;
  }

  const message = buildWholesaleNotificationMessage(recipient, request, result);
  await sendViaSendmail(message);
};

const buildWholesaleNotificationMessage = (
  recipient: string,
  request: WholesaleNotificationRequest,
  result?: WholesaleNotificationResult
): string => {
  const customerName = `${request.firstName} ${request.lastName}`.trim();
  const submittedAt = new Date().toISOString();
  const lines = [
    `To: ${recipient}`,
    `From: ${config.wholesaleNotificationFrom}`,
    `Subject: New wholesale account request: ${customerName || request.email}`,
    "Content-Type: text/plain; charset=utf-8",
    "",
    "A customer has submitted an application for a wholesale account.",
    "Please review the customer details below.",
    "",
    `Submitted at: ${submittedAt}`,
    `Customer ID: ${result?.customerId ?? "-"}`,
    `Name: ${customerName || "-"}`,
    `Email: ${request.email || "-"}`,
    `Phone: ${request.phone?.trim() || "-"}`,
    `Company: ${request.company?.trim() || "-"}`,
    `VAT Number: ${request.vatNumber?.trim() || "-"}`,
    `Country: ${(result?.countryIso || request.countryIso || "GR").trim().toUpperCase()}`,
    `City: ${request.city?.trim() || "-"}`,
    `Street: ${request.street?.trim() || "-"}`,
    `Postal Code: ${request.postalCode?.trim() || "-"}`,
    "Wholesale Requested: yes"
  ];

  return lines.join("\n");
};

const sendViaSendmail = (message: string): Promise<void> =>
  new Promise((resolve, reject) => {
    const process = spawn(config.sendmailPath, ["-t", "-i"]);
    let stderr = "";

    process.on("error", (error) => {
      reject(error);
    });

    process.stderr.on("data", (chunk: Buffer | string) => {
      stderr += chunk.toString();
    });

    process.on("close", (code) => {
      if (code === 0) {
        resolve();
        return;
      }
      reject(new Error(stderr.trim() || `sendmail exited with code ${code}`));
    });

    process.stdin.write(message);
    process.stdin.end();
  });
