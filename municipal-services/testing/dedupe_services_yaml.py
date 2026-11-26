#!/usr/bin/env python3
"""Deduplicate services.yaml keeping richest entry per service name."""
from pathlib import Path
import yaml

path = Path(__file__).resolve().parents[1] / "core-smoke-tests/src/test/resources/services.yaml"
data = yaml.safe_load(path.read_text())
services = data["services"]
by_name = {}
for entry in services:
    name = entry["name"]
    existing = by_name.get(name)
    if existing is None or len(yaml.dump(entry)) > len(yaml.dump(existing)):
        by_name[name] = entry
# preserve order: original first-seen order
ordered = []
seen = set()
for entry in services:
    n = entry["name"]
    if n in seen:
        continue
    seen.add(n)
    ordered.append(by_name[n])
extra = [
    {"name": "firenoc-calculator", "basePath": "/firenoc-calculator", "healthPath": "/health", "versionPath": "/info", "dependsOn": ["egov-mdms-service"]},
    {"name": "upyog-aggregation-service", "basePath": "/upyog-aggregation-service", "healthPath": "/health", "versionPath": "/info", "dependsOn": ["egov-mdms-service"]},
]
for e in extra:
    if e["name"] not in by_name:
        ordered.append(e)
path.write_text(yaml.dump({"services": ordered}, sort_keys=False))
