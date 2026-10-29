#!/usr/bin/env python3
"""Purge generated tests and regenerate for FAIL_MODULES only."""
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
FAIL = (Path(__file__).parent / "FAIL_MODULES.txt").read_text().split()


def purge(module: str) -> int:
    from generate_per_class_unit_tests import looks_generated_test

    test_root = ROOT / module / "src/test/java"
    if not test_root.exists():
        return 0
    n = 0
    for p in test_root.rglob("*Test.java"):
        if "architecture" in str(p) or p.name == "LayeredArchitectureTest.java":
            continue
        if looks_generated_test(p.read_text(encoding="utf-8", errors="replace")):
            p.unlink()
            n += 1
    return n


def main():
    gen = Path(__file__).parent / "generate_per_class_unit_tests.py"
    for m in FAIL:
        m = m.strip()
        if not m:
            continue
        r = purge(m)
        subprocess.check_call([sys.executable, str(gen), m])
        print(f"{m}: purged={r}")


if __name__ == "__main__":
    main()
