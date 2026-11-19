#!/usr/bin/env python3
"""Per-module class/test inventory and optional JaCoCo parse."""
from __future__ import annotations

import argparse
import csv
import json
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def main_classes(module: Path) -> list[str]:
    main = module / "src/main/java"
    if not main.exists():
        return []
    out = []
    for p in main.rglob("*.java"):
        if p.name == "package-info.java":
            continue
        if re.search(r"public\s+(?:class|enum|interface|record)\s+(\w+)", p.read_text(errors="replace")):
            out.append(p.stem)
    return sorted(out)


def test_classes(module: Path) -> set[str]:
    test = module / "src/test/java"
    if not test.exists():
        return set()
    return {p.stem.replace("Test", "") for p in test.rglob("*Test.java") if p.stem.endswith("Test")}


def jacoco_ratios(module: Path) -> tuple[float, float]:
    csv_path = module / "target/site/jacoco/jacoco.csv"
    if not csv_path.exists():
        return 0.0, 0.0
    lc = lm = bc = bm = 0.0
    with csv_path.open() as f:
        for row in csv.DictReader(f):
            lc += float(row["LINE_COVERED"])
            lm += float(row["LINE_MISSED"])
            bc += float(row["BRANCH_COVERED"])
            bm += float(row["BRANCH_MISSED"])
    line_cov = lc / max(1.0, lc + lm)
    branch_cov = bc / max(1.0, bc + bm)
    return line_cov, branch_cov


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--json", type=Path, default=ROOT / "testing/module-coverage-audit.json")
    args = ap.parse_args()
    rows = []
    for pom in sorted(ROOT.glob("*/pom.xml")):
        mod = pom.parent.name
        if mod == "core-smoke-tests":
            continue
        mains = main_classes(pom.parent)
        tests = test_classes(pom.parent)
        missing = [c for c in mains if c not in tests]
        line, branch = jacoco_ratios(pom.parent)
        rows.append(
            {
                "module": mod,
                "main_classes": len(mains),
                "with_test": len(mains) - len(missing),
                "missing": missing,
                "test_count": len(list((pom.parent / "src/test/java").rglob("*Test.java")))
                if (pom.parent / "src/test/java").exists()
                else 0,
                "line_coverage": round(line, 4),
                "branch_coverage": round(branch, 4),
            }
        )
    args.json.write_text(json.dumps(rows, indent=2))
    print(f"wrote {args.json}")


if __name__ == "__main__":
    main()
