#!/usr/bin/env python3
"""Scan shallow tests in the 15-module campaign scope."""
import re
from pathlib import Path

CORE = Path(__file__).resolve().parents[1]
WS = CORE.parent
MODULE_DIRS = [
    CORE / "core-smoke-tests",
    CORE / "egov-idgen",
    CORE / "egov-indexer",
    CORE / "egov-notification-sms",
    CORE / "egov-pg-service",
    CORE / "egov-user",
    CORE / "egov-workflow-v2",
    CORE / "gateway",
    CORE / "gis-service",
    CORE / "mdms-v2",
    CORE / "tenant",
    CORE / "upyog-draft-service",
    WS / "business-services" / "finance-collections-voucher-consumer",
    WS / "business-services" / "dashboard-analytics",
]
OUT = CORE / "coverage-report" / "shallow-tests.md"

lines_out = [
    "# Shallow / low-value tests (15-module scope)",
    "",
    "Modules: core-services PR set + finance-collections-voucher-consumer + dashboard-analytics.",
    "",
]
shallow = []

for mod_root in MODULE_DIRS:
    if not mod_root.exists():
        continue
    mod = mod_root.name
    test_root = mod_root / "src/test/java"
    if not test_root.exists():
        continue
    for java in test_root.rglob("*Test.java"):
        text = java.read_text(encoding="utf-8", errors="replace")
        rel = java.relative_to(mod_root)
        reasons = []
        if "contextLoads" in text and text.count("@Test") <= 2:
            reasons.append("mostly contextLoads")
        if re.search(r"assertNotNull\s*\(\s*new\s+\w+", text):
            reasons.append("assertNotNull(new ...)")
        has_assert = bool(re.search(r"assert[A-Z]|verify\s*\(|assertThat", text))
        if re.search(r"@Test", text) and not has_assert:
            reasons.append("no assertions or verify()")
        if reasons:
            shallow.append((mod, str(rel), reasons))

for mod, path, reasons in sorted(shallow):
    lines_out.append(f"- **{mod}** `{path}`: {', '.join(reasons)}")

OUT.write_text("\n".join(lines_out) + "\n", encoding="utf-8")
print(f"Wrote {OUT} ({len(shallow)} entries)")
