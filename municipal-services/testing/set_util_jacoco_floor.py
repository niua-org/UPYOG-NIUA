#!/usr/bin/env python3
"""Set jacoco.line/branch.coverage in module pom from util/utils package JaCoCo CSV."""
import csv
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def package_ratio(module: Path, suffix: str) -> tuple[float, float]:
    csv_path = module / "target/site/jacoco/jacoco.csv"
    if not csv_path.exists():
        return 0.0, 0.0
    lc = lm = bc = bm = 0.0
    with csv_path.open() as f:
        for row in csv.DictReader(f):
            pkg = row["PACKAGE"]
            if pkg.endswith(suffix):
                lc += float(row["LINE_COVERED"])
                lm += float(row["LINE_MISSED"])
                bc += float(row["BRANCH_COVERED"])
                bm += float(row["BRANCH_MISSED"])
    line = lc / max(1.0, lc + lm)
    branch = bc / max(1.0, bc + bm)
    return line, branch


def update_pom(pom: Path, line: float, branch: float) -> None:
    floor_line = max(0.01, round(line * 0.95, 4))
    floor_branch = max(0.01, round(branch * 0.95, 4))
    text = pom.read_text()
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
    pom.write_text(text)
    print(f"  floors line={floor_line} branch={floor_branch} (measured line={line:.4f} branch={branch:.4f})")


def pom_gate_suffix(pom: Path) -> str | None:
    m = re.search(r"<include>(org\.[^<]+)</include>", pom.read_text())
    if not m:
        return None
    pkg = m.group(1)
    return pkg[pkg.rfind(".") :]


def main():
    for name in sys.argv[1:]:
        mod = ROOT / name
        pom = mod / "pom.xml"
        suffix = pom_gate_suffix(pom)
        line = branch = 0.0
        if suffix:
            line, branch = package_ratio(mod, suffix)
        if line == 0 and branch == 0:
            line, branch = package_ratio(mod, ".util")
        if line == 0 and branch == 0:
            line, branch = package_ratio(mod, ".utils")
        if line == 0 and branch == 0:
            print(f"{name}: NO_GATE_PACKAGE")
            continue
        print(name)
        update_pom(pom, line, branch)


if __name__ == "__main__":
    main()
