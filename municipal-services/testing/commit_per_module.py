#!/usr/bin/env python3
import json
import subprocess
from pathlib import Path

ROOT = Path("/workspace")
META = json.loads((ROOT / "municipal-services/testing/modules-test-metadata.json").read_text())
SMOKE_ROOT = ROOT / "municipal-services/core-smoke-tests/src/test/java/org/niua/smoke"


def smoke_dirs_for(module: str):
    dirs = []
    flat = module.replace("-", "")
    camel = "".join(p.capitalize() for p in module.split("-"))
    for candidate in [flat, camel, camel.replace("Services", "services")]:
        p = SMOKE_ROOT / candidate.lower()
        if p.exists():
            dirs.append(p)
    # legacy short names
    legacy = {
        "property-services": "property",
        "pgr-services": "pgr",
        "ws-services": "ws",
        "vendor-management": "vendor",
        "community-hall-booking": "chb",
        "firenoc-services": "firenoc",
    }
    if module in legacy:
        lp = SMOKE_ROOT / legacy[module]
        if lp.exists():
            dirs.append(lp)
    return dirs


def git(*args):
    return subprocess.run(["git", *args], cwd=ROOT, capture_output=True, text=True)


modules = [m["module"] for m in META]
extra = ["firenoc-calculator", "pqm-scheduler", "upyog-aggregation-service", "community-hall-booking", "vendor-management"]
for module in modules + extra:
    mod_path = ROOT / "municipal-services" / module
    if not mod_path.exists():
        continue
    status = git("status", "--porcelain", f"municipal-services/{module}")
    smoke_paths = smoke_dirs_for(module)
    smoke_dirty = any(
        git("status", "--porcelain", str(p)).stdout.strip() for p in smoke_paths
    )
    if not status.stdout.strip() and not smoke_dirty:
        continue
    git("add", f"municipal-services/{module}")
    for p in smoke_dirs_for(module):
        if p.exists():
            git("add", str(p))
    if not subprocess.run(["git", "diff", "--cached", "--quiet"], cwd=ROOT).returncode == 0:
        subprocess.check_call(
            ["git", "commit", "-m", f"tests: layered tests and smoke hook for {module}"],
            cwd=ROOT,
        )
        print(f"committed {module}")

for shared in [
    "municipal-services/testing",
    "municipal-services/.mvn",
    "municipal-services/core-smoke-tests/src/test/resources/services.yaml",
    "municipal-services/core-smoke-tests/src/test/java/org/niua/smoke/firenoccanculator",
    "municipal-services/core-smoke-tests/src/test/java/org/niua/smoke/upyogaggregationservice",
    "municipal-services/docs/TEST_STRATEGY_REPORT.md",
]:
    p = ROOT / shared
    if p.exists():
        git("add", shared)
if subprocess.run(["git", "diff", "--cached", "--quiet"], cwd=ROOT).returncode != 0:
    subprocess.check_call(
        ["git", "commit", "-m", "tests: municipal-services shared test tooling and strategy report"],
        cwd=ROOT,
    )
