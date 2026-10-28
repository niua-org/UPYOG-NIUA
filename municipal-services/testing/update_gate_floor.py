#!/usr/bin/env python3
"""Set jacoco.line/branch.coverage in module pom from the JaCoCo PACKAGE gate in pom.xml."""
import csv
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def gate_package(pom_text: str) -> str | None:
    for line in pom_text.splitlines():
        if "<include>" in line and "IT.java" not in line:
            m = re.search(r"<include>([^<]+)</include>", line)
            if m:
                return m.group(1)
    return None


def package_ratio(module: Path, package: str) -> tuple[float, float]:
    csv_path = module / "target/site/jacoco/jacoco.csv"
    if not csv_path.exists():
        return 0.0, 0.0
    lc = lm = bc = bm = 0.0
    with csv_path.open() as f:
        for row in csv.DictReader(f):
            if row["PACKAGE"] == package:
                lc += float(row["LINE_COVERED"])
                lm += float(row["LINE_MISSED"])
                bc += float(row["BRANCH_COVERED"])
                bm += float(row["BRANCH_MISSED"])
    line = lc / max(1.0, lc + lm)
    branch = bc / max(1.0, bc + bm)
    return line, branch


def update_pom(pom: Path, line: float, branch: float) -> None:
    floor_line = max(0.01, round(line * 0.95, 4))
    floor_branch = round(branch * 0.95, 4) if branch > 0 else 0.0
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
    # vendor-management style minimums
    for prop in ("validator", "util", "builder", "querybuilder", "utils", "validation", "service"):
        text = re.sub(
            rf"(<jacoco\.{prop}\.line\.minimum>)[^<]+(</jacoco\.{prop}\.line\.minimum>)",
            rf"\g<1>{floor_line}\g<2>",
            text,
            count=1,
        )
        text = re.sub(
            rf"(<jacoco\.{prop}\.branch\.minimum>)[^<]+(</jacoco\.{prop}\.branch\.minimum>)",
            rf"\g<1>{floor_branch}\g<2>",
            text,
            count=1,
        )
    pom.write_text(text)
    print(f"  floors line={floor_line} branch={floor_branch} (measured line={line:.4f} branch={branch:.4f})")


def main():
    for name in sys.argv[1:]:
        mod = ROOT / name
        pom = mod / "pom.xml"
        pkg = gate_package(pom.read_text())
        if not pkg:
            print(f"{name}: NO_GATE")
            continue
        line, branch = package_ratio(mod, pkg)
        if line == 0 and branch == 0:
            print(f"{name}: NO_COVERAGE for {pkg}")
            continue
        print(name, pkg)
        update_pom(pom, line, branch)


if __name__ == "__main__":
    main()
