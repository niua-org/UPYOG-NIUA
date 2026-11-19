#!/usr/bin/env python3
"""Scaffold Jest smoke tests for each src/**/*.js file in a Node municipal service."""
from __future__ import annotations

import argparse
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MARKER = "// generated-by: generate_node_unit_tests.py\n"


def rel_require(service: Path, src_file: Path) -> str:
    rel = src_file.relative_to(service / "src")
    without_ext = rel.with_suffix("")
    ups = 2 + len(rel.parent.parts)  # test/unit/...
    prefix = "/".join([".."] * ups)
    return f"{prefix}/src/" + "/".join(without_ext.parts)


def test_path(service: Path, src_file: Path) -> Path:
    rel = src_file.relative_to(service / "src")
    return service / "test" / "unit" / rel.parent / (rel.stem + ".test.js")


def scaffold(require_path: str, export_hint: str) -> str:
    return f"""{MARKER}describe('{require_path}', () => {{
  test('module_whenRequired_loadsWithoutThrow', () => {{
    jest.resetModules();
    let mod;
    expect(() => {{
      mod = require('{require_path}');
    }}).not.toThrow();
    expect(mod).toBeDefined();
    {export_hint}
  }});
}});
"""


def export_hint(src_text: str) -> str:
    if "module.exports" in src_text or "exports." in src_text:
        return "// CommonJS export surface exercised via require above."
    return ""


def process_service(service_name: str, dry_run: bool = False) -> int:
    service = ROOT / service_name
    src_root = service / "src"
    if not src_root.exists():
        return 0
    created = 0
    skip_names = {
        "index.js",
        "db.js",
        "consumer.js",
        "producer.js",
    }
    for src in sorted(src_root.rglob("*.js")):
        if src.name in skip_names:
            continue
        out = test_path(service, src)
        if out.exists():
            text = out.read_text(encoding="utf-8", errors="replace")
            if not text.startswith(MARKER.strip()[:20]) and "generated-by" not in text:
                continue
        req = rel_require(service, src)
        body = scaffold(req, export_hint(src.read_text(encoding="utf-8", errors="replace")))
        if dry_run:
            print("would create", out.relative_to(service))
            created += 1
            continue
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(body, encoding="utf-8")
        created += 1
    return created


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("services", nargs="+", help="e.g. firenoc-services firenoc-calculator")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()
    for s in args.services:
        n = process_service(s, dry_run=args.dry_run)
        print(f"{s}: created={n}")


if __name__ == "__main__":
    main()
