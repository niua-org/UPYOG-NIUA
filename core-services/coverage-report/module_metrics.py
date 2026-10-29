#!/usr/bin/env python3
"""Print CSV: module,line_pct,branch_pct from JaCoCo XML (non-excluded classes)."""
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

POJO_PACKAGES = (".model.", ".web.models.", ".contract.", ".config.", ".web.contract.")
# Align with gateway JaCoCo bundle excludes (model/constants/application entrypoints).
BUNDLE_EXTRA_EXCLUDES = (".constants.", "Application")

def is_excluded(name: str) -> bool:
    return any(p in name for p in POJO_PACKAGES + BUNDLE_EXTRA_EXCLUDES)

def metrics(xml_path: Path):
    root = ET.parse(xml_path).getroot()
    line_m = line_c = br_m = br_c = 0
    for pkg in root.findall("package"):
        pkg_name = pkg.get("name", "").replace("/", ".")
        for cls in pkg.findall("class"):
            name = cls.get("name", "").replace("/", ".")
            full = f"{pkg_name}.{name.split('.')[-1]}" if pkg_name else name
            if is_excluded(full):
                continue
            for counter in cls.findall("counter"):
                kind = counter.get("type")
                missed = int(counter.get("missed", 0))
                covered = int(counter.get("covered", 0))
                if kind == "LINE":
                    line_m += missed
                    line_c += covered
                elif kind == "BRANCH":
                    br_m += missed
                    br_c += covered
    line_pct = 100 * line_c / (line_c + line_m) if line_c + line_m else 0
    br_pct = 100 * br_c / (br_c + br_m) if br_c + br_m else 0
    return line_pct, br_pct

if __name__ == "__main__":
    root = Path(__file__).resolve().parents[1]
    modules = sys.argv[1:] or [
        "core-smoke-tests", "egov-idgen", "egov-indexer", "egov-notification-sms",
        "egov-pg-service", "egov-user", "egov-workflow-v2", "gateway", "gis-service",
        "mdms-v2", "tenant", "upyog-draft-service",
    ]
    ws = root.parent
    business = {
        "finance-collections-voucher-consumer",
        "dashboard-analytics",
    }
    for mod in modules:
        if mod in business:
            xml = ws / "business-services" / mod / "target" / "site" / "jacoco" / "jacoco.xml"
        else:
            xml = root / mod / "target" / "site" / "jacoco" / "jacoco.xml"
        if not xml.exists():
            print(f"{mod},NA,NA")
            continue
        lp, bp = metrics(xml)
        print(f"{mod},{lp:.1f},{bp:.1f}")
