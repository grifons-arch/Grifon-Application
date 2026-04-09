"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.notifyWholesaleRequest = void 0;
const promises_1 = require("fs/promises");
const child_process_1 = require("child_process");
const net_1 = __importDefault(require("net"));
const os_1 = __importDefault(require("os"));
const tls_1 = __importDefault(require("tls"));
const env_1 = require("../config/env");
const notifyWholesaleRequest = async (request, result) => {
    if (!request.wholesaleRequested) {
        return;
    }
    const recipient = env_1.config.wholesaleNotificationTo?.trim();
    if (!recipient) {
        return;
    }
    if (env_1.config.wholesaleNotificationTransport === "disabled") {
        return;
    }
    const recipients = recipient
        .split(",")
        .map((value) => value.trim())
        .filter(Boolean);
    if (recipients.length === 0) {
        return;
    }
    const message = buildWholesaleNotificationMessage(recipient, request, result);
    const transport = resolveNotificationTransport();
    if (transport === "smtp") {
        await sendViaSmtp(message, recipients);
        return;
    }
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
        `Date: ${new Date().toUTCString()}`,
        "MIME-Version: 1.0",
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
const resolveNotificationTransport = () => {
    if (env_1.config.wholesaleNotificationTransport === "smtp") {
        return "smtp";
    }
    if (env_1.config.wholesaleNotificationTransport === "sendmail") {
        return "sendmail";
    }
    return env_1.config.smtpHost ? "smtp" : "sendmail";
};
const sendViaSendmail = async (message) => {
    try {
        await (0, promises_1.access)(env_1.config.sendmailPath);
    }
    catch {
        throw new Error(`sendmail binary not found at ${env_1.config.sendmailPath}. Configure SMTP_HOST or install sendmail.`);
    }
    await new Promise((resolve, reject) => {
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
};
const sendViaSmtp = async (message, recipients) => {
    if (!env_1.config.smtpHost) {
        throw new Error("SMTP transport selected but SMTP_HOST is empty. Set SMTP_HOST or switch WHOLESALE_NOTIFICATION_TRANSPORT.");
    }
    const fromAddress = extractEmailAddress(env_1.config.wholesaleNotificationFrom);
    if (!fromAddress) {
        throw new Error("WHOLESALE_NOTIFICATION_FROM must contain a valid sender email address.");
    }
    const smtp = new SmtpConnection({
        host: env_1.config.smtpHost,
        port: env_1.config.smtpPort,
        secure: env_1.config.smtpSecure,
        requireTls: env_1.config.smtpRequireTls,
        heloName: env_1.config.smtpHeloName ?? os_1.default.hostname() ?? "localhost",
        username: env_1.config.smtpUser,
        password: env_1.config.smtpPass
    });
    await smtp.connect();
    try {
        await smtp.sendMessage(fromAddress, recipients, message);
    }
    finally {
        await smtp.close();
    }
};
const extractEmailAddress = (value) => {
    const match = value.match(/<([^>]+)>/);
    const candidate = (match?.[1] ?? value).trim();
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(candidate) ? candidate : "";
};
class SmtpConnection {
    constructor(options) {
        this.options = options;
        this.socket = null;
        this.buffer = "";
    }
    async connect() {
        this.socket = await this.openSocket();
        await this.readResponse([220]);
        let capabilities = await this.ehlo();
        if (!this.options.secure && this.options.requireTls) {
            if (!this.serverSupports(capabilities, "STARTTLS")) {
                throw new Error("SMTP server does not support STARTTLS.");
            }
            await this.sendCommand("STARTTLS", [220]);
            await this.upgradeToTls();
            capabilities = await this.ehlo();
        }
        if (this.options.username) {
            await this.authenticate(capabilities);
        }
    }
    async sendMessage(from, recipients, message) {
        const uniqueRecipients = Array.from(new Set(recipients.map(extractEmailAddress).filter(Boolean)));
        if (uniqueRecipients.length === 0) {
            throw new Error("Wholesale notification recipient is invalid.");
        }
        await this.sendCommand(`MAIL FROM:<${from}>`, [250]);
        for (const recipient of uniqueRecipients) {
            await this.sendCommand(`RCPT TO:<${recipient}>`, [250, 251]);
        }
        await this.sendCommand("DATA", [354]);
        await this.writeRaw(`${normalizeMessageForSmtp(message)}\r\n.\r\n`);
        await this.readResponse([250]);
    }
    async close() {
        if (!this.socket) {
            return;
        }
        try {
            await this.sendCommand("QUIT", [221]);
        }
        catch {
            // Ignore close errors.
        }
        this.socket.end();
        this.socket.destroy();
        this.socket = null;
        this.buffer = "";
    }
    async ehlo() {
        const response = await this.sendCommand(`EHLO ${this.options.heloName || "localhost"}`, [250]);
        return response.lines
            .slice(1)
            .map((line) => line.replace(/^\d{3}[ -]/, "").trim())
            .filter(Boolean);
    }
    serverSupports(capabilities, capability) {
        const normalizedNeedle = capability.toUpperCase();
        return capabilities.some((line) => line.toUpperCase().startsWith(normalizedNeedle));
    }
    async authenticate(capabilities) {
        const username = this.options.username ?? "";
        const password = this.options.password ?? "";
        const authLine = capabilities.find((line) => line.toUpperCase().startsWith("AUTH "));
        const methods = authLine
            ? authLine
                .slice(5)
                .trim()
                .split(/\s+/)
                .map((value) => value.toUpperCase())
            : [];
        if (methods.includes("PLAIN")) {
            const token = Buffer.from(`\u0000${username}\u0000${password}`, "utf8").toString("base64");
            await this.sendCommand(`AUTH PLAIN ${token}`, [235]);
            return;
        }
        if (methods.includes("LOGIN") || methods.length === 0) {
            await this.sendCommand("AUTH LOGIN", [334]);
            await this.sendCommand(Buffer.from(username, "utf8").toString("base64"), [334]);
            await this.sendCommand(Buffer.from(password, "utf8").toString("base64"), [235]);
            return;
        }
        throw new Error(`SMTP AUTH mechanism not supported. Available methods: ${methods.join(", ") || "none"}`);
    }
    async openSocket() {
        return new Promise((resolve, reject) => {
            const handleError = (error) => reject(error);
            if (this.options.secure) {
                const socket = tls_1.default.connect({
                    host: this.options.host,
                    port: this.options.port,
                    servername: this.options.host
                }, () => resolve(socket));
                socket.once("error", handleError);
                return;
            }
            const socket = net_1.default.createConnection({
                host: this.options.host,
                port: this.options.port
            }, () => resolve(socket));
            socket.once("error", handleError);
        });
    }
    async upgradeToTls() {
        const currentSocket = this.ensureSocket();
        this.socket = await new Promise((resolve, reject) => {
            const secureSocket = tls_1.default.connect({
                socket: currentSocket,
                servername: this.options.host
            }, () => resolve(secureSocket));
            secureSocket.once("error", reject);
        });
    }
    async sendCommand(command, expectedCodes) {
        await this.writeRaw(`${command}\r\n`);
        return this.readResponse(expectedCodes);
    }
    async writeRaw(payload) {
        const socket = this.ensureSocket();
        await new Promise((resolve, reject) => {
            socket.write(payload, (error) => {
                if (error) {
                    reject(error);
                    return;
                }
                resolve();
            });
        });
    }
    async readResponse(expectedCodes) {
        while (true) {
            const parsed = this.tryExtractResponse();
            if (parsed) {
                if (!expectedCodes.includes(parsed.code)) {
                    throw new Error(parsed.lines.join("\n"));
                }
                return parsed;
            }
            const chunk = await this.readChunk();
            this.buffer += chunk;
        }
    }
    async readChunk() {
        const socket = this.ensureSocket();
        return new Promise((resolve, reject) => {
            const cleanup = () => {
                socket.off("data", onData);
                socket.off("error", onError);
                socket.off("close", onClose);
            };
            const onData = (chunk) => {
                cleanup();
                resolve(chunk.toString());
            };
            const onError = (error) => {
                cleanup();
                reject(error);
            };
            const onClose = () => {
                cleanup();
                reject(new Error("SMTP connection closed unexpectedly."));
            };
            socket.once("data", onData);
            socket.once("error", onError);
            socket.once("close", onClose);
        });
    }
    tryExtractResponse() {
        const linePattern = /(.*?)(\r\n|\n)/gs;
        const responseLines = [];
        let code = null;
        let consumedLength = 0;
        let match;
        while ((match = linePattern.exec(this.buffer)) !== null) {
            const line = match[1];
            const fullMatch = match[0];
            consumedLength += fullMatch.length;
            responseLines.push(line);
            const lineMatch = line.match(/^(\d{3})([ -])(.*)$/);
            if (!lineMatch) {
                continue;
            }
            const lineCode = Number(lineMatch[1]);
            if (code === null) {
                code = lineCode;
            }
            if (lineCode === code && lineMatch[2] === " ") {
                this.buffer = this.buffer.slice(consumedLength);
                return { code, lines: responseLines };
            }
        }
        return null;
    }
    ensureSocket() {
        if (!this.socket) {
            throw new Error("SMTP socket is not connected.");
        }
        return this.socket;
    }
}
const normalizeMessageForSmtp = (message) => message
    .replace(/\r?\n/g, "\r\n")
    .split("\r\n")
    .map((line) => (line.startsWith(".") ? `.${line}` : line))
    .join("\r\n");
