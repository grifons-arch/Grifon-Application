#!/usr/bin/env python3

from __future__ import annotations

import argparse
import json
import sys
import urllib.error
import urllib.request


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Clear favorite and recent-product activity through grifon-gateway."
    )
    parser.add_argument("--base-url", default="http://127.0.0.1:3000", help="Gateway base URL")
    parser.add_argument("--shop-id", type=int, choices=[1, 4], required=True, help="PrestaShop shop id")
    parser.add_argument("--json", action="store_true", help="Output JSON")
    return parser.parse_args()


def fail(message: str, code: int = 1) -> None:
    print(message, file=sys.stderr)
    raise SystemExit(code)


def http_json(method: str, url: str, payload: dict[str, int]) -> dict[str, object]:
    request = urllib.request.Request(
        url=url,
        data=json.dumps(payload).encode("utf-8"),
        headers={"Content-Type": "application/json"},
        method=method,
    )

    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            raw = response.read().decode("utf-8")
            return json.loads(raw) if raw else {}
    except urllib.error.HTTPError as exc:
        body = exc.read().decode("utf-8", errors="ignore")
        fail(f"{method} {url} failed with HTTP {exc.code}: {body or exc.reason}")
    except urllib.error.URLError as exc:
        fail(f"{method} {url} failed: {exc.reason}")


def main() -> None:
    args = parse_args()
    result = http_json(
        "POST",
        f"{args.base_url.rstrip('/')}/v1/customer-activity/clear",
        {"shopId": args.shop_id},
    )

    output = {
        "shopId": args.shop_id,
        "cleared": bool(result.get("ok")),
        "response": result,
    }

    if args.json:
        print(json.dumps(output, ensure_ascii=False, indent=2))
        return

    print(f"Clear request sent for shopId={args.shop_id}")
    print(f"Cleared: {output['cleared']}")


if __name__ == "__main__":
    main()
