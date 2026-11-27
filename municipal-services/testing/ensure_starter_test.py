#!/usr/bin/env python3
from pathlib import Path

ROOT = Path("/workspace/municipal-services")
BLOCK = """
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
"""

for pom in ROOT.glob("*/pom.xml"):
    text = pom.read_text()
    if "spring-boot-starter-test" in text:
        continue
    if "</dependencies>" not in text:
        continue
    head, tail = text.rsplit("</dependencies>", 1)
    text = head + BLOCK + "\n    </dependencies>" + tail
    pom.write_text(text)
    print("added starter-test", pom.parent.name)
