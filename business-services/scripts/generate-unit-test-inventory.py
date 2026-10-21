#!/usr/bin/env python3
"""Generate business-services/docs/UNIT_TEST_INVENTORY.md from src/main/java."""
from __future__ import annotations

import re
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODULES = sorted(
    p.name
    for p in ROOT.iterdir()
    if p.is_dir() and (p / "pom.xml").exists() and p.name not in ("test-support", "core-smoke-tests")
)


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="ignore")


def class_kind(content: str, name: str) -> str:
    if re.search(rf"\binterface\s+{re.escape(name)}\b", content):
        return "interface"
    if re.search(rf"\benum\s+{re.escape(name)}\b", content):
        return "enum"
    if re.search(rf"\babstract\s+class\s+{re.escape(name)}\b", content):
        return "abstract"
    if re.search(rf"\bclass\s+{re.escape(name)}\b", content):
        return "class"
    return "unknown"


def is_commented_out(content: str, name: str) -> bool:
    stripped = content.lstrip()
    if not stripped.startswith("/*"):
        return False
    if "*/" not in content:
        return True
    after = content.split("*/", 1)[1]
    return not re.search(rf"\b(class|interface|enum)\s+{re.escape(name)}\b", after)


def has_matching_test(class_name: str, test_root: Path) -> bool:
    if not test_root.exists():
        return False
    for f in test_root.rglob("*.java"):
        stem = f.stem
        if stem in (class_name + "Test", class_name + "IT"):
            return True
    return False


def main() -> None:
    lines: list[str] = [
        "# business-services unit test inventory",
        "",
        "Auto-generated inventory: every `src/main/java` type and whether `<ClassName>Test` or `<ClassName>IT` exists.",
        "",
        "| Module | Total classes | Has test | Missing | Notes |",
        "|--------|---------------|----------|---------|-------|",
    ]

    grand_total = grand_tested = grand_missing = 0

    module_stats: list[tuple[str, int, int, int, dict]] = []

    for mod in MODULES:
        main_java = ROOT / mod / "src/main/java"
        test_java = ROOT / mod / "src/test/java"
        if not main_java.exists():
            continue

        by_status: dict[str, list[tuple[str, str, str]]] = defaultdict(list)
        total = tested = missing = 0

        for java_file in sorted(main_java.rglob("*.java")):
            rel = java_file.relative_to(main_java)
            pkg = ".".join(rel.parts[:-1])
            name = java_file.stem
            content = read_text(java_file)
            total += 1
            kind = class_kind(content, name)
            commented = is_commented_out(content, name)
            has_test = has_matching_test(name, test_java)

            if has_test:
                tested += 1
                status = "tested"
            elif commented:
                missing += 1
                status = "not-testable-commented"
            elif kind == "interface":
                missing += 1
                status = "interface-use-impl-test"
            elif kind == "abstract":
                missing += 1
                status = "abstract-use-subclass-test"
            else:
                missing += 1
                status = "missing-test"

            by_status[status].append((pkg, name, kind))

        grand_total += total
        grand_tested += tested
        grand_missing += missing
        lines.append(f"| {mod} | {total} | {tested} | {missing} | see detail below |")

        lines.extend(["", f"## {mod}", ""])
        lines.append(f"- **Total:** {total} | **With test:** {tested} | **Without dedicated test:** {missing}")
        lines.append("")

        for status in (
            "missing-test",
            "interface-use-impl-test",
            "abstract-use-subclass-test",
            "not-testable-commented",
            "tested",
        ):
            items = by_status.get(status, [])
            if not items and status != "tested":
                continue
            title = status.replace("-", " ").title()
            lines.append(f"### {title} ({len(items)})")
            lines.append("")
            if status == "tested" and len(items) > 50:
                lines.append(f"_{len(items)} classes — omitted for brevity._")
                lines.append("")
                continue
            for pkg, name, kind in items[:200]:
                lines.append(f"- `{pkg}.{name}` ({kind})")
            if len(items) > 200:
                lines.append(f"- _… and {len(items) - 200} more_")
            lines.append("")

    lines.extend(
        [
            "## Summary",
            "",
            f"- **Grand total classes:** {grand_total}",
            f"- **With dedicated test class:** {grand_tested}",
            f"- **Without dedicated test class:** {grand_missing}",
            "",
        ]
    )

    out = ROOT / "docs" / "UNIT_TEST_INVENTORY.md"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text("\n".join(lines), encoding="utf-8")
    print(f"Wrote {out} ({grand_total} classes, {grand_tested} tested, {grand_missing} without test)")


if __name__ == "__main__":
    main()
