#!/usr/bin/env python3
"""JaCoCo baseline: per-module and per-class coverage, shallow-test detection, reports."""
from __future__ import annotations

import argparse
import csv
import json
import re
import subprocess
import xml.etree.ElementTree as ET
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPORT = ROOT / "coverage-report"
SETTINGS = ROOT / ".mvn/settings-public.xml"

PRIORITY_MODULES = [
    "property-services",
    "ws-services",
    "sw-services",
    "pgr-services",
    "tl-services",
    "bpa-services",
]

SKIP_CLASS_PATTERNS = re.compile(
    r"(?i)(/models/|/web/models/|/web/contracts/|/contracts/|\.config\.|Configuration$|Constants$|Constant$|/enums/|Enum$|RowMapper$|package-info)"
)

TRIVIAL_SUFFIXES = (
    "Request",
    "Response",
    "Criteria",
    "Detail",
    "Details",
    "Wrapper",
)


@dataclass
class ClassCov:
    module: str
    fqcn: str
    source_file: str
    line_missed: int
    line_covered: int
    branch_missed: int
    branch_covered: int

    @property
    def line_pct(self) -> float:
        t = self.line_missed + self.line_covered
        return self.line_covered / t if t else 0.0

    @property
    def branch_pct(self) -> float:
        t = self.branch_missed + self.branch_covered
        return self.branch_covered / t if t else 0.0

    def is_priority_target(self) -> bool:
        if SKIP_CLASS_PATTERNS.search(self.source_file.replace("\\", "/")):
            return False
        if any(x in self.fqcn for x in (".models.", ".web.models.", ".config.", ".enums.")):
            return False
        simple = self.fqcn.split(".")[-1]
        if simple.endswith(TRIVIAL_SUFFIXES) and "Validator" not in simple and "Service" not in simple:
            return False
        return any(
            k in self.source_file
            for k in (
                "/service/",
                "/validator/",
                "/util/",
                "/consumer/",
                "/kafka/",
                "/repository/builder/",
                "QueryBuilder",
                "Enrichment",
            )
        )


def module_names() -> list[str]:
    return sorted(
        p.parent.name
        for p in ROOT.glob("*/pom.xml")
        if p.parent.name != "core-smoke-tests"
    )


def run_jacoco(modules: list[str], clean: bool = False) -> None:
    env = {
        **dict(subprocess.os.environ),
        "JAVA_HOME": "/usr/lib/jvm/java-17-openjdk-amd64",
        "PATH": f"/usr/lib/jvm/java-17-openjdk-amd64/bin:{subprocess.os.environ.get('PATH', '')}",
    }
    for mod in modules:
        pom = ROOT / mod / "pom.xml"
        goal = "clean test jacoco:report" if clean else "test jacoco:report"
        print(f"=== {mod} {goal} ===", flush=True)
        subprocess.run(
            ["mvn", "-f", str(pom), "-s", str(SETTINGS), "-q", *goal.split()],
            cwd=str(ROOT),
            env=env,
            check=False,
        )


def parse_jacoco_xml(module: str) -> list[ClassCov]:
    xml_path = ROOT / module / "target/site/jacoco/jacoco.xml"
    if not xml_path.exists():
        return []
    tree = ET.parse(xml_path)
    root = tree.getroot()
    out: list[ClassCov] = []
    for pkg in root.findall("package"):
        for cls in pkg.findall("class"):
            fqcn = cls.get("name", "").replace("/", ".")
            src = cls.get("sourcefilename", "")
            lm = lc = bm = bc = 0
            for counter in cls.findall("counter"):
                kind = counter.get("type")
                missed = int(counter.get("missed", 0))
                covered = int(counter.get("covered", 0))
                if kind == "LINE":
                    lm, lc = missed, covered
                elif kind == "BRANCH":
                    bm, bc = missed, covered
            # Resolve approximate source path
            pkg_name = fqcn.rsplit(".", 1)[0] if "." in fqcn else ""
            rel = Path(*pkg_name.split(".")) / src if pkg_name else Path(src)
            source_file = str(ROOT / module / "src/main/java" / rel)
            out.append(
                ClassCov(module, fqcn, source_file, lm, lc, bm, bc)
            )
    return out


def module_totals(classes: list[ClassCov]) -> tuple[float, float, int, int]:
    lm = sum(c.line_missed for c in classes)
    lc = sum(c.line_covered for c in classes)
    bm = sum(c.branch_missed for c in classes)
    bc = sum(c.branch_covered for c in classes)
    line = lc / max(1, lc + lm)
    branch = bc / max(1, bc + bm)
    return line, branch, lm + lc, bm + bc


def is_shallow_test(path: Path) -> tuple[bool, str]:
    text = path.read_text(encoding="utf-8", errors="replace")
    if "generated-by: generate_per_class_unit_tests.py" in text:
        if re.search(r"assertThat\(subject\)\.isNotNull\(\)", text):
            return True, "generated subject-is-not-null scaffold"
        if re.search(r"assertThat\(new \w+\(\)\)\.isNotNull\(\)", text):
            return True, "generated newInstance-is-not-null scaffold"
        if re.search(r"\.class\)\.isNotNull\(\)", text) and "assertThat" in text:
            if text.count("@Test") <= 2:
                return True, "generated class-load scaffold"
        if "mockMvc_whenStandaloneSetup_isNotNull" in text:
            return True, "generated controller mockMvc scaffold"
    if not re.search(r"assert\w+|verify\(|assertThatThrownBy", text):
        return True, "no assertions"
    meaningful = len(re.findall(r"assertThatThrownBy|verify\(|ArgumentCaptor|assertEquals|assertNotEquals|containsExactly|hasMessage", text))
    trivial = len(re.findall(r"isNotNull\(\)|isNotBlank\(\)|values\(\)", text))
    if meaningful == 0 and trivial > 0 and "@Test" in text:
        return True, "only trivial assertions"
    return False, ""


def collect_shallow(module: str | None = None) -> list[dict]:
    rows = []
    mods = [module] if module else module_names()
    for mod in mods:
        test_root = ROOT / mod / "src/test/java"
        if not test_root.exists():
            continue
        for p in test_root.rglob("*Test.java"):
            if "architecture" in str(p):
                continue
            shallow, reason = is_shallow_test(p)
            if shallow:
                rows.append(
                    {
                        "module": mod,
                        "test_class": p.stem,
                        "path": str(p.relative_to(ROOT)),
                        "reason": reason,
                    }
                )
    return rows


def write_baseline(all_classes: dict[str, list[ClassCov]], shallow: list[dict]) -> None:
    REPORT.mkdir(parents=True, exist_ok=True)
    mod_rows = []
    for mod in module_names():
        classes = all_classes.get(mod, [])
        line, branch, _, _ = module_totals(classes)
        priority = [c for c in classes if c.is_priority_target()]
        priority.sort(key=lambda c: c.line_missed, reverse=True)
        mod_rows.append(
            {
                "module": mod,
                "line_pct": round(line * 100, 2),
                "branch_pct": round(branch * 100, 2),
                "priority_classes": len(priority),
                "top_missed": [
                    {
                        "class": c.fqcn,
                        "line_missed": c.line_missed,
                        "branch_missed": c.branch_missed,
                        "line_pct": round(c.line_pct * 100, 1),
                    }
                    for c in priority[:15]
                ],
            }
        )
    mod_rows.sort(key=lambda r: (-r["line_pct"], r["module"]))
    # Re-rank by total missed lines in priority targets
    mod_rows.sort(
        key=lambda r: -sum(x["line_missed"] for x in r["top_missed"]),
    )

    baseline_md = ["# Coverage baseline (JDK 17, JaCoCo)\n"]
    baseline_md.append("| Module | Line % | Branch % | Priority classes (sample top missed lines) |")
    baseline_md.append("|--------|-------:|---------:|------------------------------------------|")
    for r in mod_rows:
        top = ", ".join(f"{t['class'].split('.')[-1]}({t['line_missed']})" for t in r["top_missed"][:5])
        baseline_md.append(
            f"| {r['module']} | {r['line_pct']} | {r['branch_pct']} | {top or '—'} |"
        )
    (REPORT / "baseline.md").write_text("\n".join(baseline_md) + "\n", encoding="utf-8")

    per_class = []
    for mod, classes in all_classes.items():
        for c in sorted(classes, key=lambda x: x.line_missed, reverse=True):
            per_class.append(
                {
                    "module": mod,
                    "class": c.fqcn,
                    "source_file": c.source_file,
                    "line_pct": round(c.line_pct * 100, 2),
                    "branch_pct": round(c.branch_pct * 100, 2),
                    "line_missed": c.line_missed,
                    "branch_missed": c.branch_missed,
                    "priority_target": c.is_priority_target(),
                }
            )
    (REPORT / "baseline-per-class.json").write_text(
        json.dumps(per_class, indent=2), encoding="utf-8"
    )

    shallow_md = ["# Shallow tests\n", f"Total shallow: **{len(shallow)}**\n"]
    shallow_md.append("| Module | Test class | Reason | Path |")
    shallow_md.append("|--------|------------|--------|------|")
    for s in sorted(shallow, key=lambda x: (x["module"], x["test_class"])):
        shallow_md.append(
            f"| {s['module']} | {s['test_class']} | {s['reason']} | `{s['path']}` |"
        )
    (REPORT / "shallow-tests.md").write_text("\n".join(shallow_md) + "\n", encoding="utf-8")

    rank = [c for classes in all_classes.values() for c in classes if c.is_priority_target()]
    rank.sort(key=lambda c: c.line_missed, reverse=True)
    rank_md = ["# Priority class ranking (by missed lines)\n"]
    for c in rank[:200]:
        rank_md.append(
            f"- **{c.module}** `{c.fqcn}` — missed lines {c.line_missed}, branches {c.branch_missed}, line {c.line_pct*100:.1f}%"
        )
    (REPORT / "class-ranking.md").write_text("\n".join(rank_md) + "\n", encoding="utf-8")

    print(f"wrote {REPORT}/baseline.md ({len(mod_rows)} modules)")
    print(f"wrote {REPORT}/shallow-tests.md ({len(shallow)} tests)")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--run", action="store_true", help="Run mvn test jacoco:report first")
    ap.add_argument("--clean", action="store_true", help="Use mvn clean test")
    ap.add_argument("--modules", nargs="*", default=None)
    args = ap.parse_args()
    mods = args.modules or module_names()
    if args.run:
        run_jacoco(mods, clean=args.clean)
    all_classes: dict[str, list[ClassCov]] = {}
    for mod in mods:
        all_classes[mod] = parse_jacoco_xml(mod)
    shallow = collect_shallow()
    write_baseline(all_classes, shallow)


if __name__ == "__main__":
    main()
