#!/usr/bin/env python3
"""Add JaCoCo service package gate rule + property placeholders to a module pom."""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def add_gate(pom: Path, package: str, prop_suffix: str = "service") -> bool:
    text = pom.read_text()
    prop_line = f"jacoco.{prop_suffix}.line.minimum"
    if prop_line in text:
        return False
    props = f"""        <{prop_line}>0.01</{prop_line}>
        <jacoco.{prop_suffix}.branch.minimum>0.0</jacoco.{prop_suffix}.branch.minimum>
"""
    text = re.sub(r"(<properties>\s*\n)", r"\1" + props, text, count=1)
    rule = f"""                                <rule>
                                    <element>PACKAGE</element>
                                    <includes>
                                        <include>{package}</include>
                                    </includes>
                                    <limits>
                                        <limit>
                                            <counter>LINE</counter>
                                            <value>COVEREDRATIO</value>
                                            <minimum>${{jacoco.{prop_suffix}.line.minimum}}</minimum>
                                        </limit>
                                        <limit>
                                            <counter>BRANCH</counter>
                                            <value>COVEREDRATIO</value>
                                            <minimum>${{jacoco.{prop_suffix}.branch.minimum}}</minimum>
                                        </limit>
                                    </limits>
                                </rule>
"""
    if "</rules>" not in text:
        print(f"{pom.parent.name}: no </rules>")
        return False
    text = text.replace("                            </rules>", rule + "                            </rules>", 1)
    pom.write_text(text)
    return True


def main():
    # module:package[:prop_suffix]
    for spec in sys.argv[1:]:
        parts = spec.split(":")
        mod, pkg = parts[0], parts[1]
        suffix = parts[2] if len(parts) > 2 else "service"
        pom = ROOT / mod / "pom.xml"
        if add_gate(pom, pkg, suffix):
            print(f"added {mod} gate {pkg} ({suffix})")
        else:
            print(f"skip {mod} (already has {suffix} gate or no rules)")


if __name__ == "__main__":
    main()
