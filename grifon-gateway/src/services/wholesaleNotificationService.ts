import { access } from "fs/promises";
import { spawn } from "child_process";
import net from "net";
import os from "os";
import tls from "tls";
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

  if (config.wholesaleNotificationTransport === "disabled") {
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

const resolveNotificationTransport = (): "smtp" | "sendmail" => {
  if (config.wholesaleNotificationTransport === "smtp") {
    return "smtp";
  }

  if (config.wholesaleNotificationTransport === "sendmail") {
    return "sendmail";
  }

  return config.smtpHost ? "smtp" : "sendmail";
};

const sendViaSendmail = async (message: string): Promise<void> => {
  try {
    await access(config.sendmailPath);
  } catch {
    throw new Error(
      `sendmail binary not found at ${config.sendmailPath}. Configure SMTP_HOST or install sendmail.`
    );
  }

  await new Promise<void>((resolve, reject) => {
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
};

const sendViaSmtp = async (message: string, recipients: string[]): Promise<void> => {
  if (!config.smtpHost) {
    throw new Error(
      "SMTP transport selected but SMTP_HOST is empty. Set SMTP_HOST or switch WHOLESALE_NOTIFICATION_TRANSPORT."
    );
  }

  const fromAddress = extractEmailAddress(config.wholesaleNotificationFrom);
  if (!fromAddress) {
    throw new Error("WHOLESALE_NOTIFICATION_FROM must contain a valid sender email address.");
  }

  const smtp = new SmtpConnection({
    host: config.smtpHost,
    port: config.smtpPort,
    secure: config.smtpSecure,
    requireTls: config.smtpRequireTls,
    heloName: config.smtpHeloName ?? os.hostname() ?? "localhost",
    username: config.smtpUser,
    password: config.smtpPass
  });

  await smtp.connect();
  try {
    await smtp.sendMessage(fromAddress, recipients, message);
  } finally {
    await smtp.close();
  }
};

const extractEmailAddress = (value: string): string => {
  const match = value.match(/<([^>]+)>/);
  const candidate = (match?.[1] ?? value).trim();
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(candidate) ? candidate : "";
};

type SmtpOptions = {
  host: string;
  port: number;
  secure: boolean;
  requireTls: boolean;
  heloName: string;
  username?: string;
  password?: string;
};

class SmtpConnection {
  private socket: net.Socket | tls.TLSSocket | null = null;
  private buffer = "";

  constructor(private readonly options: SmtpOptions) {}

  async connect(): Promise<void> {
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

  async sendMessage(from: string, recipients: string[], message: string): Promise<void> {
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

  async close(): Promise<void> {
    if (!this.socket) {
      return;
    }

    try {
      await this.sendCommand("QUIT", [221]);
    } catch {
      // Ignore close errors.
    }

    this.socket.end();
    this.socket.destroy();
    this.socket = null;
    this.buffer = "";
  }

  private async ehlo(): Promise<string[]> {
    const response = await this.sendCommand(`EHLO ${this.options.heloName || "localhost"}`, [250]);
    return response.lines
      .slice(1)
      .map((line) => line.replace(/^\d{3}[ -]/, "").trim())
      .filter(Boolean);
  }

  private serverSupports(capabilities: string[], capability: string): boolean {
    const normalizedNeedle = capability.toUpperCase();
    return capabilities.some((line) => line.toUpperCase().startsWith(normalizedNeedle));
  }

  private async authenticate(capabilities: string[]): Promise<void> {
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

  private async openSocket(): Promise<net.Socket | tls.TLSSocket> {
    return new Promise((resolve, reject) => {
      const handleError = (error: Error) => reject(error);

      if (this.options.secure) {
        const socket = tls.connect(
          {
            host: this.options.host,
            port: this.options.port,
            servername: this.options.host
          },
          () => resolve(socket)
        );
        socket.once("error", handleError);
        return;
      }

      const socket = net.createConnection(
        {
          host: this.options.host,
          port: this.options.port
        },
        () => resolve(socket)
      );
      socket.once("error", handleError);
    });
  }

  private async upgradeToTls(): Promise<void> {
    const currentSocket = this.ensureSocket();

    this.socket = await new Promise<tls.TLSSocket>((resolve, reject) => {
      const secureSocket = tls.connect(
        {
          socket: currentSocket,
          servername: this.options.host
        },
        () => resolve(secureSocket)
      );
      secureSocket.once("error", reject);
    });
  }

  private async sendCommand(command: string, expectedCodes: number[]): Promise<SmtpResponse> {
    await this.writeRaw(`${command}\r\n`);
    return this.readResponse(expectedCodes);
  }

  private async writeRaw(payload: string): Promise<void> {
    const socket = this.ensureSocket();

    await new Promise<void>((resolve, reject) => {
      socket.write(payload, (error?: Error | null) => {
        if (error) {
          reject(error);
          return;
        }
        resolve();
      });
    });
  }

  private async readResponse(expectedCodes: number[]): Promise<SmtpResponse> {
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

  private async readChunk(): Promise<string> {
    const socket = this.ensureSocket();

    return new Promise<string>((resolve, reject) => {
      const cleanup = () => {
        socket.off("data", onData);
        socket.off("error", onError);
        socket.off("close", onClose);
      };

      const onData = (chunk: Buffer | string) => {
        cleanup();
        resolve(chunk.toString());
      };

      const onError = (error: Error) => {
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

  private tryExtractResponse(): SmtpResponse | null {
    const linePattern = /(.*?)(\r\n|\n)/gs;
    const responseLines: string[] = [];
    let code: number | null = null;
    let consumedLength = 0;
    let match: RegExpExecArray | null;

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

  private ensureSocket(): net.Socket | tls.TLSSocket {
    if (!this.socket) {
      throw new Error("SMTP socket is not connected.");
    }

    return this.socket;
  }
}

type SmtpResponse = {
  code: number;
  lines: string[];
};

const normalizeMessageForSmtp = (message: string): string =>
  message
    .replace(/\r?\n/g, "\r\n")
    .split("\r\n")
    .map((line) => (line.startsWith(".") ? `.${line}` : line))
    .join("\r\n");
