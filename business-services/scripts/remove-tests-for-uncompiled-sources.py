#!/usr/bin/env python3
"""Remove generated tests when the main type is not compiled (block-commented)."""
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def is_compilable(java_path: Path) -> bool:
    content = java_path.read_text(encoding="utf-8", errors="ignore")
    name = java_path.stem
    # strip license header block comment
    if content.lstrip().startswith("/*"):
        idx = content.find("*/")
        if idx != -1:
            content = content[idx + 2 :]
    return bool(re.search(rf"\b(class|interface|enum)\s+{re.escape(name)}\b", content))


removed = 0
for mod in ROOT.iterdir():
    if not mod.is_dir() or not (mod / "pom.xml").exists():
        continue
    main = mod / "src/main/java"
    test = mod / "src/test/java"
    if not main.exists() or not test.exists():
        continue
    for test_file in test.rglob("*Test.java"):
        cls = test_file.stem.replace("Test", "")
        # find main file
        candidates = list(main.rglob(f"{cls}.java"))
        if not candidates:
            test_file.unlink(missing_ok=True)
            removed += 1
            continue
        if not is_compilable(candidates[0]):
            test_file.unlink(missing_ok=True)
            removed += 1

print(f"Removed {removed} tests for uncompiled/commented sources")
