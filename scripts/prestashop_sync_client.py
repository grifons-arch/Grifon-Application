from __future__ import annotations

import base64
import hashlib
import hmac
import json
import time
from typing import Any, Dict, Tuple

import requests
from passlib.hash import bcrypt

SHOPS: Dict[str, Dict[str, str]] = {
    "shopA": {
        "base_url": "https://shopA.gr",
        "secret": "SECRET_A",
        "allowed_ip": "203.0.113.10",  # Optional: configured in module whitelist.
    },
    "shopB": {
        "base_url": "https://shopB.gr",
        "secret": "SECRET_B",
    },
}


def make_bcrypt_2y(plain_password: str, rounds: int = 12) -> str:
    """Return a PHP-compatible bcrypt hash string with a $2y$ prefix."""
    return bcrypt.using(rounds=rounds, ident="2y").hash(plain_password)


def build_payload(
    *,
    external_customer_id: str,
    email: str,
    firstname: str,
    lastname: str,
    password_hashed: str,
    company: str | None = None,
    website: str | None = None,
    groups_default: int,
    groups_list: list[int],
    addresses: list[dict[str, Any]],
) -> dict[str, Any]:
    """Build the expected customer payload for the sync endpoint."""
    customer: dict[str, Any] = {
        "email": email,
        "firstname": firstname,
        "lastname": lastname,
        "password_hashed": password_hashed,
    }
    if company:
        customer["company"] = company
    if website:
        customer["website"] = website

    return {
        "externalCustomerId": external_customer_id,
        "customer": customer,
        "groups": {"default": groups_default, "list": groups_list},
        "addresses": addresses,
    }


def sign_body(secret: str, ts: int, raw_body: str) -> str:
    """Sign '<timestamp>\\n<rawBody>' with HMAC-SHA256 and return base64 signature."""
    message = f"{ts}\n{raw_body}".encode("utf-8")
    digest = hmac.new(secret.encode("utf-8"), message, hashlib.sha256).digest()
    return base64.b64encode(digest).decode("utf-8")


def sync_customer(
    store_id: str,
    payload_dict: dict[str, Any],
    timeout: int = 15,
) -> Tuple[int, dict[str, Any]]:
    """Sync customer data to the selected PrestaShop store via module endpoint."""
    shop = SHOPS.get(store_id)
    if not shop:
        return 0, {"error": f"Unknown store_id '{store_id}'"}

    base_url = shop.get("base_url", "").rstrip("/")
    if not base_url.startswith("https://"):
        return 0, {"error": "HTTPS is required for base_url"}

    secret = shop.get("secret", "")
    if not secret:
        return 0, {"error": "Missing shared secret for store"}

    raw_body = json.dumps(payload_dict, ensure_ascii=False, separators=(",", ":"))
    ts = int(time.time())
    signature = sign_body(secret, ts, raw_body)

    headers = {
        "Content-Type": "application/json",
        "X-Grifon-Timestamp": str(ts),
        "X-Grifon-Signature": signature,
    }
    url = f"{base_url}/module/grifoncustomersync/sync"

    try:
        response = requests.post(url, data=raw_body.encode("utf-8"), headers=headers, timeout=timeout)
    except requests.RequestException as exc:
        return 0, {"error": f"Request failed: {exc}"}

    if response.status_code != 200:
        try:
            return response.status_code, {
                "error": f"Sync failed with HTTP {response.status_code}",
                "details": response.json(),
            }
        except ValueError:
            return response.status_code, {
                "error": f"Sync failed with HTTP {response.status_code}",
                "details": response.text,
            }

    try:
        return response.status_code, response.json()
    except ValueError:
        return response.status_code, {"error": "Invalid JSON response", "details": response.text}


if __name__ == "__main__":
    # Example: password arrives from your backend register flow.
    # Android app -> backend only; backend -> PrestaShop sync endpoint only.
    register_password = "PasswordFromRegisterFlow"
    password_hashed = make_bcrypt_2y(register_password)

    addresses = [
        {
            "externalAddressId": "addr_1",
            "alias": "Billing",
            "address1": "Ermou 1",
            "postcode": "10563",
            "city": "Athens",
            "countryIso": "GR",
            "vat_number": "EL123456789",
            "company": "ACME SA",
        }
    ]

    payload = build_payload(
        external_customer_id="cust_123",
        email="user@example.com",
        firstname="Name",
        lastname="Surname",
        password_hashed=password_hashed,
        company="ACME SA",
        website="https://example.com",
        groups_default=4,
        groups_list=[4, 7],
        addresses=addresses,
    )

    status_a, result_a = sync_customer("shopA", payload)
    status_b, result_b = sync_customer("shopB", payload)

    print("Shop A:", status_a, result_a)
    print("Shop B:", status_b, result_b)
