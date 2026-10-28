#!/usr/bin/env python3
"""Append module summary and per-class table from JaCoCo XML to stdout."""
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

POJO_SUFFIXES = ("Request", "Response", "DTO", "Dto", "Config", "Properties", "Model")
POJO_PACKAGES = (".model.", ".web.models.", ".contract.", ".config.", ".web.contract.")

def is_excluded(class_name: str) -> bool:
    if any(p in class_name for p in POJO_PACKAGES):
        return True
    short = class_name.rsplit(".", 1)[-1]
    return any(short.endswith(s) for s in POJO_SUFFIXES) and "Service" not in short and "Validator" not in short

def pct(covered: int, total: int) -> float:
    return 100.0 * covered / total if total else 100.0

def main():
    path, module = sys.argv[1], sys.argv[2]
    root = ET.parse(path).getroot()
    classes = []
    for pkg in root.findall("package"):
        pkg_name = pkg.get("name", "").replace("/", ".")
        for cls in pkg.findall("class"):
            name = cls.get("name", "").replace("/", ".")
            full = f"{pkg_name}.{name.split('.')[-1]}" if pkg_name else name
            if is_excluded(full):
                continue
            line_missed = line_total = branch_missed = branch_total = 0
            for counter in cls.findall("counter"):
                kind = counter.get("type")
                missed = int(counter.get("missed", 0))
                covered = int(counter.get("covered", 0))
                total = missed + covered
                if kind == "LINE":
                    line_missed, line_total = missed, total
                elif kind == "BRANCH":
                    branch_missed, branch_total = missed, total
            if line_total == 0:
                continue
            classes.append(
                {
                    "name": full,
                    "line_missed": line_missed,
                    "line_total": line_total,
                    "line_pct": pct(line_total - line_missed, line_total),
                    "branch_missed": branch_missed,
                    "branch_total": branch_total,
                    "branch_pct": pct(branch_total - branch_missed, branch_total) if branch_total else 100.0,
                }
            )

    total_line_m = sum(c["line_missed"] for c in classes)
    total_line_t = sum(c["line_total"] for c in classes)
    total_br_m = sum(c["branch_missed"] for c in classes)
    total_br_t = sum(c["branch_total"] for c in classes)

    print(f"## {module}")
    print(
        f"**Module line:** {pct(total_line_t - total_line_m, total_line_t):.1f}% "
        f"({total_line_t - total_line_m}/{total_line_t}) | "
        f"**Module branch:** {pct(total_br_t - total_br_m, total_br_t):.1f}% "
        f"({total_br_t - total_br_m}/{total_br_t})"
    )
    print("")
    print("| Class | Line % | Branch % | Missed lines | Missed branches |")
    print("| --- | ---: | ---: | ---: | ---: |")
    for c in sorted(classes, key=lambda x: -x["line_missed"])[:40]:
        print(
            f"| `{c['name']}` | {c['line_pct']:.1f} | {c['branch_pct']:.1f} | "
            f"{c['line_missed']} | {c['branch_missed']} |"
        )
    print("")

if __name__ == "__main__":
    main()
