#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
from collections import defaultdict
from pathlib import Path


TABLE_PATTERNS = [
    re.compile(r"_DB_PREFIX_\s*\.\s*'([A-Za-z0-9_]+)'"),
    re.compile(r"Db::getInstance\(\)->(?:insert|update|delete)\(\s*'([A-Za-z0-9_]+)'"),
]

MODULE_LINK_PATTERN = re.compile(
    r"getModuleLink\(\s*'([^']+)'\s*,\s*'([^']+)'",
    re.IGNORECASE,
)

CONTROLLER_CLASS_PATTERN = re.compile(
    r"class\s+([A-Za-z0-9_]+(?:ModuleFrontController|ModuleAdminController))",
    re.IGNORECASE,
)


def read_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8", errors="ignore")
    except OSError:
        return ""


def normalize_table_name(name: str) -> str:
    return name.strip().strip("`").strip()


def controller_name_from_path(path: Path) -> str | None:
    parts = path.parts
    if "controllers" not in parts:
        return None
    try:
        idx = parts.index("controllers")
    except ValueError:
        return None
    if idx + 2 >= len(parts):
        return None
    if parts[idx + 1] not in {"front", "admin"}:
        return None
    if path.suffix.lower() != ".php":
        return None
    if path.stem.lower() == "index":
        return None
    return path.stem


def inspect_module(module_path: Path) -> dict:
    module_path = module_path.resolve()
    module_name = module_path.name

    table_hits: dict[str, set[str]] = defaultdict(set)
    links: list[dict[str, str]] = []
    controller_classes: list[dict[str, str]] = []
    front_controllers: set[str] = set()
    admin_controllers: set[str] = set()
    php_files: list[Path] = sorted(module_path.rglob("*.php"))

    for php_file in php_files:
        rel = php_file.relative_to(module_path).as_posix()
        text = read_text(php_file)
        if not text:
            continue

        for pattern in TABLE_PATTERNS:
            for match in pattern.findall(text):
                table_name = normalize_table_name(match)
                if table_name:
                    table_hits[table_name].add(rel)

        for module, controller in MODULE_LINK_PATTERN.findall(text):
            links.append(
                {
                    "module": module,
                    "controller": controller,
                    "file": rel,
                }
            )

        for match in CONTROLLER_CLASS_PATTERN.findall(text):
            controller_classes.append({"class": match, "file": rel})

        controller_name = controller_name_from_path(php_file)
        if controller_name:
            if "/controllers/front/" in f"/{rel}":
                front_controllers.add(controller_name)
            elif "/controllers/admin/" in f"/{rel}":
                admin_controllers.add(controller_name)

    urls = []
    for controller in sorted(front_controllers):
        urls.append(
            {
                "controller": controller,
                "url_pattern": f"/index.php?fc=module&module={module_name}&controller={controller}",
            }
        )
        urls.append(
            {
                "controller": controller,
                "url_pattern": f"/module/{module_name}/{controller}",
            }
        )

    return {
        "module_path": str(module_path),
        "module_name": module_name,
        "php_files_scanned": len(php_files),
        "tables": [
            {"table": table, "found_in": sorted(files)}
            for table, files in sorted(table_hits.items())
        ],
        "front_controllers": sorted(front_controllers),
        "admin_controllers": sorted(admin_controllers),
        "module_links": links,
        "controller_classes": controller_classes,
        "entry_urls": urls,
    }


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Inspect a PrestaShop module directory for table names and controllers."
    )
    parser.add_argument(
        "module_path",
        help="Path to the installed module directory, e.g. /var/www/html/modules/ets_wholesale",
    )
    parser.add_argument(
        "--pretty",
        action="store_true",
        help="Pretty-print JSON output.",
    )
    args = parser.parse_args()

    module_path = Path(args.module_path)
    if not module_path.exists():
        raise SystemExit(f"Module path not found: {module_path}")
    if not module_path.is_dir():
        raise SystemExit(f"Module path is not a directory: {module_path}")

    result = inspect_module(module_path)
    if args.pretty:
        print(json.dumps(result, ensure_ascii=False, indent=2))
    else:
        print(json.dumps(result, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
