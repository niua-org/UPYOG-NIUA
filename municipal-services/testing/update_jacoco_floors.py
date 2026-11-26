#!/usr/bin/env python3
"""Set jacoco.line/branch.coverage in module pom to ~95% of measured (floor)."""
from __future__ import annotations

import csv
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def ratios(module: Path) -> tuple[float, float]:
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
    line = lc / max(1.0, lc + lm)
    branch = bc / max(1.0, bc + bm)
    return line, branch


def update_pom(pom: Path, line: float, branch: float) -> bool:
    text = pom.read_text()
    floor_line = max(0.01, round(line * 0.95, 4))
    floor_branch = max(0.01, round(branch * 0.95, 4))
    orig = text
    text = re.sub(
        r"(<jacoco\.line\.coverage>)[^<]+(</jacoco\.line\.coverage>)",
        rf"\g<1>{floor_line}\g<2>",
        text,
        count=1,
    )
    text = re.sub(
        r"(<jacoco\.branch\.coverage>)[^<]+(</jacoco\.branch\.coverage>)",
        rf"\g<1>{floor_branch}\g<2>",
        text,
        count=1,
    )
    if text != orig:
        pom.write_text(text)
        return True
    return False


def main():
    modules = sys.argv[1:] or [p.parent.name for p in ROOT.glob("*/pom.xml")]
    for name in modules:
        mod = ROOT / name
        if not (mod / "pom.xml").exists():
            continue
        line, branch = ratios(mod)
        if line == 0 and branch == 0:
            continue
        if update_pom(mod / "pom.xml", line, branch):
            print(f"{name}: line={line:.3f} branch={branch:.3f}")


if __name__ == "__main__":
    main()
