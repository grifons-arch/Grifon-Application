#!/usr/bin/env python3
"""Create missing Grifon activity tables in a PrestaShop database.

This script reads DB credentials and table prefix from a real PrestaShop install
and creates the `grifon_favorite_product` and `grifon_recent_product` tables if
they are missing.
"""

from __future__ import annotations

import argparse
import os
import re
import subprocess
import sys
from pathlib import Path


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Create missing Grifon activity tables in PrestaShop DB."
    )
    parser.add_argument(
        "--ps-root",
        required=True,
        help="Absolute path to the real PrestaShop root directory.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Print the SQL without executing it.",
    )
    return parser.parse_args()


def fail(message: str, exit_code: int = 1) -> None:
    print(message, file=sys.stderr)
    raise SystemExit(exit_code)


def read_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8", errors="ignore")
    except OSError as exc:
        fail(f"Cannot read {path}: {exc}")


def extract_define(text: str, constant_name: str) -> str | None:
    pattern = re.compile(
        rf"define\(\s*['\"]{re.escape(constant_name)}['\"]\s*,\s*['\"]([^'\"]+)['\"]\s*\)",
        re.IGNORECASE,
    )
    match = pattern.search(text)
    return match.group(1) if match else None


def extract_php_array_value(text: str, key: str) -> str | None:
    pattern = re.compile(
        rf"['\"]{re.escape(key)}['\"]\s*=>\s*['\"]([^'\"]+)['\"]",
        re.IGNORECASE,
    )
    match = pattern.search(text)
    return match.group(1) if match else None


def load_prestashop_db_config(ps_root: Path) -> dict[str, str]:
    config_inc = ps_root / "config" / "config.inc.php"
    parameters_php = ps_root / "app" / "config" / "parameters.php"

    if config_inc.exists():
        text = read_text(config_inc)
        config = {
            "host": extract_define(text, "_DB_SERVER_"),
            "name": extract_define(text, "_DB_NAME_"),
            "user": extract_define(text, "_DB_USER_"),
            "password": extract_define(text, "_DB_PASSWD_") or "",
            "prefix": extract_define(text, "_DB_PREFIX_"),
        }
    elif parameters_php.exists():
        text = read_text(parameters_php)
        config = {
            "host": extract_php_array_value(text, "database_host"),
            "name": extract_php_array_value(text, "database_name"),
            "user": extract_php_array_value(text, "database_user"),
            "password": extract_php_array_value(text, "database_password") or "",
            "prefix": extract_php_array_value(text, "database_prefix"),
        }
    else:
        fail(f"PrestaShop DB config not found under: {ps_root}")

    missing = [key for key, value in config.items() if value is None]
    if missing:
        fail(f"Failed to parse PrestaShop DB config. Missing: {', '.join(missing)}")

    return {key: value or "" for key, value in config.items()}


def build_sql(prefix: str) -> str:
    safe_prefix = prefix.replace("`", "")
    return f"""
CREATE TABLE IF NOT EXISTS `{safe_prefix}grifon_favorite_product` (
    `id_grifon_favorite_product` INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `id_customer` INT UNSIGNED NOT NULL,
    `id_product` INT UNSIGNED NOT NULL,
    `id_shop` INT UNSIGNED NOT NULL,
    `title` VARCHAR(255) NULL,
    `price` DECIMAL(20,6) NULL,
    `currency` VARCHAR(16) NULL,
    `image_url` TEXT NULL,
    `brand` VARCHAR(255) NULL,
    `date_add` DATETIME NOT NULL,
    `date_upd` DATETIME NOT NULL,
    PRIMARY KEY (`id_grifon_favorite_product`),
    UNIQUE KEY `uniq_customer_product_shop` (`id_customer`, `id_product`, `id_shop`),
    KEY `idx_customer_shop` (`id_customer`, `id_shop`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `{safe_prefix}grifon_recent_product` (
    `id_grifon_recent_product` INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `id_customer` INT UNSIGNED NOT NULL,
    `id_product` INT UNSIGNED NOT NULL,
    `id_shop` INT UNSIGNED NOT NULL,
    `title` VARCHAR(255) NULL,
    `price` DECIMAL(20,6) NULL,
    `currency` VARCHAR(16) NULL,
    `image_url` TEXT NULL,
    `brand` VARCHAR(255) NULL,
    `visited_at` DATETIME NOT NULL,
    `date_add` DATETIME NOT NULL,
    `date_upd` DATETIME NOT NULL,
    PRIMARY KEY (`id_grifon_recent_product`),
    UNIQUE KEY `uniq_customer_recent_product_shop` (`id_customer`, `id_product`, `id_shop`),
    KEY `idx_recent_customer_shop` (`id_customer`, `id_shop`, `visited_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
""".strip()


def run_mysql(config: dict[str, str], sql: str) -> None:
    env = os.environ.copy()
    env["MYSQL_PWD"] = config["password"]
    command = [
        "mysql",
        f"--host={config['host']}",
        f"--user={config['user']}",
        "--default-character-set=utf8mb4",
        config["name"],
        "-e",
        sql,
    ]

    try:
        result = subprocess.run(
            command,
            env=env,
            check=False,
            capture_output=True,
            text=True,
        )
    except FileNotFoundError:
        fail("`mysql` CLI is not installed on this machine.")

    if result.returncode != 0:
        stderr = result.stderr.strip() or "Unknown mysql error"
        fail(f"MySQL execution failed: {stderr}")


def main() -> None:
    args = parse_args()
    ps_root = Path(args.ps_root).expanduser().resolve()
    config = load_prestashop_db_config(ps_root)
    sql = build_sql(config["prefix"])

    print(f"PrestaShop root: {ps_root}")
    print(f"Database: {config['name']} @ {config['host']}")
    print(f"Prefix: {config['prefix']}")

    if args.dry_run:
        print("\nSQL to execute:\n")
        print(sql)
        return

    run_mysql(config, sql)
    print("\nActivity tables are ready:")
    print(f"- {config['prefix']}grifon_favorite_product")
    print(f"- {config['prefix']}grifon_recent_product")


if __name__ == "__main__":
    main()
