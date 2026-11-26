#!/usr/bin/env python3
"""Print module line/branch % from JaCoCo CSV (respects pom excludes after report)."""
import csv
import sys
from pathlib import Path

mod = sys.argv[1]
csv_path = Path(__file__).resolve().parents[1] / mod / "target/site/jacoco/jacoco.csv"
if not csv_path.exists():
    print("NO_REPORT")
    sys.exit(1)
lc = lm = bc = bm = 0.0
with csv_path.open() as f:
    for row in csv.DictReader(f):
        lc += float(row["LINE_COVERED"])
        lm += float(row["LINE_MISSED"])
        bc += float(row["BRANCH_COVERED"])
        bm += float(row["BRANCH_MISSED"])
line = lc / max(1, lc + lm)
branch = bc / max(1, bc + bm)
print(f"LINE={line:.4f} BRANCH={branch:.4f}")
