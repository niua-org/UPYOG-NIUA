#!/usr/bin/env python3
"""Remove generated per-class scaffolds under priority test packages."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MARKER = "// generated-by: generate_per_class_unit_tests.py"
PRIORITY_SEGMENTS = (
    "/service/",
    "/validator/",
    "/util/",
    "/consumer/",
    "/repository/builder/",
    "/kafka/",
)


def purge_module(module: str) -> int:
    test_root = ROOT / module / "src/test/java"
    if not test_root.exists():
        return 0
    n = 0
    for path in test_root.rglob("*Test.java"):
        if "architecture" in str(path):
            continue
        rel = str(path).replace("\\", "/")
        if not any(seg in rel for seg in PRIORITY_SEGMENTS):
            continue
        text = path.read_text(encoding="utf-8", errors="replace")
        if text.startswith(MARKER) or MARKER in text[:120]:
            path.unlink()
            n += 1
    return n


if __name__ == "__main__":
    import sys

    mods = sys.argv[1:] or [
        p.parent.name for p in sorted(ROOT.glob("*/pom.xml")) if p.parent.name != "core-smoke-tests"
    ]
    for m in mods:
        r = purge_module(m)
        if r:
            print(f"{m}: purged={r}")
