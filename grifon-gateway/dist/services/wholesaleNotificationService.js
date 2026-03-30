"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.notifyWholesaleRequest = void 0;
const child_process_1 = require("child_process");
const env_1 = require("../config/env");
const notifyWholesaleRequest = async (request, result) => {
    if (!request.wholesaleRequested) {
        return;
    }
    const recipient = env_1.config.wholesaleNotificationTo?.trim();
    if (!recipient) {
        return;
    }
    const message = buildWholesaleNotificationMessage(recipient, request, result);
    await sendViaSendmail(message);
};
exports.notifyWholesaleRequest = notifyWholesaleRequest;
const buildWholesaleNotificationMessage = (recipient, request, result) => {
    const customerName = `${request.firstName} ${request.lastName}`.trim();
    const submittedAt = new Date().toISOString();
    const lines = [
        `To: ${recipient}`,
        `From: ${env_1.config.wholesaleNotificationFrom}`,
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
const sendViaSendmail = (message) => new Promise((resolve, reject) => {
    const process = (0, child_process_1.spawn)(env_1.config.sendmailPath, ["-t", "-i"]);
    let stderr = "";
    process.on("error", (error) => {
        reject(error);
    });
    process.stderr.on("data", (chunk) => {
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
