#!/usr/bin/env python3

from __future__ import annotations

import argparse
import json
import sys
import urllib.error
import urllib.parse
import urllib.request
from typing import Any


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Login through grifon-gateway and inspect or seed customer favorites/recent activity."
    )
    parser.add_argument("--base-url", default="http://127.0.0.1:3000", help="Gateway base URL")
    parser.add_argument("--email", required=True, help="Customer email")
    parser.add_argument("--password", required=True, help="Customer password")
    parser.add_argument("--shop-id", type=int, choices=[1, 4], default=4, help="PrestaShop shop id")
    parser.add_argument("--limit", type=int, default=20, help="Recent products fetch limit")
    parser.add_argument("--product-id", type=int, help="Product id for seed test")
    parser.add_argument("--title", default="Gateway Test Product", help="Snapshot title for seed test")
    parser.add_argument("--price", type=float, default=None, help="Snapshot price for seed test")
    parser.add_argument("--currency", default="EUR", help="Snapshot currency for seed test")
    parser.add_argument("--image-url", default="", help="Snapshot image URL for seed test")
    parser.add_argument("--brand", default="Gateway Test", help="Snapshot brand for seed test")
    parser.add_argument(
        "--seed",
        action="store_true",
        help="Also POST one favorite and one recent-product event before fetching results",
    )
    parser.add_argument("--json", action="store_true", help="Output JSON")
    return parser.parse_args()


def fail(message: str, code: int = 1) -> None:
    print(message, file=sys.stderr)
    raise SystemExit(code)


def http_json(method: str, url: str, payload: dict[str, Any] | None = None) -> dict[str, Any]:
    data = None
    headers = {"Content-Type": "application/json"}
    if payload is not None:
        data = json.dumps(payload).encode("utf-8")

    request = urllib.request.Request(url=url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=20) as response:
            raw = response.read().decode("utf-8")
            return json.loads(raw) if raw else {}
    except urllib.error.HTTPError as exc:
        body = exc.read().decode("utf-8", errors="ignore")
        fail(f"{method} {url} failed with HTTP {exc.code}: {body or exc.reason}")
    except urllib.error.URLError as exc:
        fail(f"{method} {url} failed: {exc.reason}")


def build_snapshot(args: argparse.Namespace) -> dict[str, Any]:
    return {
        "title": args.title,
        "price": args.price,
        "currency": args.currency,
        "imageUrl": args.image_url,
        "brand": args.brand,
    }


def main() -> None:
    args = parse_args()
    base_url = args.base_url.rstrip("/")
    country_iso = "SE" if args.shop_id == 1 else "GR"

    login_response = http_json(
        "POST",
        f"{base_url}/v1/auth/login",
        {
            "email": args.email,
            "password": args.password,
            "countryIso": country_iso,
        },
    )

    customer_id = login_response.get("id_customer")
    if not login_response.get("ok") or not customer_id:
        fail(f"Login failed: {json.dumps(login_response, ensure_ascii=False)}")

    if args.seed:
        if not args.product_id:
            fail("--seed requires --product-id")

        snapshot = build_snapshot(args)

        http_json(
            "POST",
            f"{base_url}/v1/customer-activity/favorites",
            {
                "customerId": int(customer_id),
                "shopId": args.shop_id,
                "productId": args.product_id,
                "isFavorite": True,
                "product": snapshot,
            },
        )

        http_json(
            "POST",
            f"{base_url}/v1/customer-activity/recent-products",
            {
                "customerId": int(customer_id),
                "shopId": args.shop_id,
                "productId": args.product_id,
                "product": snapshot,
            },
        )

    favorites = http_json(
        "GET",
        f"{base_url}/v1/customer-activity/favorites?"
        + urllib.parse.urlencode(
            {
                "customerId": int(customer_id),
                "shopId": args.shop_id,
            }
        ),
    )

    recent = http_json(
        "GET",
        f"{base_url}/v1/customer-activity/recent-products?"
        + urllib.parse.urlencode(
            {
                "customerId": int(customer_id),
                "shopId": args.shop_id,
                "limit": args.limit,
            }
        ),
    )

    result = {
        "login": {
            "customerId": int(customer_id),
            "shopId": args.shop_id,
            "countryIso": country_iso,
            "canViewPrices": login_response.get("can_view_prices"),
        },
        "seededProductId": args.product_id if args.seed else None,
        "counts": {
            "favorites": len(favorites.get("items", [])),
            "recent": len(recent.get("items", [])),
        },
        "favorites": favorites.get("items", []),
        "recent": recent.get("items", []),
    }

    if args.seed and args.product_id:
        result["seedVerification"] = {
            "favoriteFound": any(int(item.get("productId", 0)) == args.product_id for item in result["favorites"]),
            "recentFound": any(int(item.get("productId", 0)) == args.product_id for item in result["recent"]),
        }

    if args.json:
        print(json.dumps(result, ensure_ascii=False, indent=2))
        return

    print(
        f"Login ok: customerId={result['login']['customerId']} shopId={result['login']['shopId']} "
        f"canViewPrices={result['login']['canViewPrices']}"
    )
    print(f"Favorites count: {result['counts']['favorites']}")
    print(f"Recent count: {result['counts']['recent']}")

    if args.seed and args.product_id:
        print(
            "Seed verification: "
            f"favoriteFound={result['seedVerification']['favoriteFound']} "
            f"recentFound={result['seedVerification']['recentFound']}"
        )

    if result["favorites"]:
        print("\nFavorites:")
        for item in result["favorites"]:
            print(
                f"- shop={item.get('shopId')} product={item.get('productId')} "
                f"title={item.get('title', '')} updatedAt={item.get('updatedAt', '')}"
            )

    if result["recent"]:
        print("\nRecent:")
        for item in result["recent"]:
            print(
                f"- shop={item.get('shopId')} product={item.get('productId')} "
                f"title={item.get('title', '')} visitedAt={item.get('visitedAt', '')}"
            )


if __name__ == "__main__":
    main()
