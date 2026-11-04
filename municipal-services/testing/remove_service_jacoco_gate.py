#!/usr/bin/env python3
"""Remove service/impl JaCoCo package gate rule and related properties from module pom."""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def remove_gate(pom: Path) -> bool:
    text = pom.read_text()
    orig = text
    for suffix in ("service", "impl"):
        text = re.sub(
            rf"\s*<rule>\s*<element>PACKAGE</element>.*?<include>[^<]+</include>.*?"
            rf"<minimum>\$\{{jacoco\.{suffix}\.line\.minimum\}}</minimum>.*?</rule>\s*",
            "\n",
            text,
            flags=re.DOTALL,
            count=1,
        )
        text = re.sub(rf"\s*<jacoco\.{suffix}\.line\.minimum>[^<]+</jacoco\.{suffix}\.line\.minimum>\s*\n", "\n", text)
        text = re.sub(rf"\s*<jacoco\.{suffix}\.branch\.minimum>[^<]+</jacoco\.{suffix}\.branch\.minimum>\s*\n", "\n", text)
    if text != orig:
        pom.write_text(text)
        return True
    return False


def main():
    for name in sys.argv[1:]:
        pom = ROOT / name / "pom.xml"
        print(f"{name}: {'removed' if remove_gate(pom) else 'skip'}")


if __name__ == "__main__":
    main()
