#!/usr/bin/env python3
"""Scan Java test sources for shallow patterns."""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "coverage-report" / "shallow-tests.md"

patterns = [
    ("contextLoads only", re.compile(r"void\s+contextLoads\s*\(", re.M)),
    ("assertNotNull(new", re.compile(r"assertNotNull\s*\(\s*new\s+\w+", re.M)),
    ("no assert/verify", None),
]

lines_out = [
    "# Shallow / low-value tests",
    "",
    "Heuristic scan of `src/test/java` under PR modules.",
    "",
]

shallow = []

for pom in ROOT.glob("*/pom.xml"):
    mod = pom.parent.name
    test_root = pom.parent / "src/test/java"
    if not test_root.exists():
        continue
    for java in test_root.rglob("*Test.java"):
        text = java.read_text(encoding="utf-8", errors="replace")
        rel = java.relative_to(pom.parent)
        reasons = []
        if "contextLoads" in text and text.count("@Test") <= 2:
            reasons.append("mostly contextLoads")
        if re.search(r"assertNotNull\s*\(\s*new\s+\w+", text):
            reasons.append("assertNotNull(new ...)")
        has_assert = bool(re.search(r"assert[A-Z]|verify\s*\(|assertThat", text))
        if re.search(r"@Test", text) and not has_assert:
            reasons.append("no assertions or verify()")
        if re.search(r"assertTrue\s*\(\s*true\s*\)", text):
            reasons.append("assertTrue(true)")
        if reasons:
            shallow.append((mod, str(rel), reasons))

for mod, path, reasons in sorted(shallow):
    lines_out.append(f"- **{mod}** `{path}`: {', '.join(reasons)}")

OUT.write_text("\n".join(lines_out) + "\n", encoding="utf-8")
print(f"Wrote {OUT} ({len(shallow)} entries)")
