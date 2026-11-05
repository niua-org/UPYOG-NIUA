#!/usr/bin/env python3
from pathlib import Path

ROOT = Path("/workspace/municipal-services")

TEST_DEPS = """
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>${testcontainers.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <version>${testcontainers.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.wiremock</groupId>
            <artifactId>wiremock-standalone</artifactId>
            <version>3.3.1</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.tngtech.archunit</groupId>
            <artifactId>archunit-junit5</artifactId>
            <version>1.2.1</version>
            <scope>test</scope>
        </dependency>
"""

for pom in ROOT.glob("*/pom.xml"):
    if pom.parent.name == "core-smoke-tests":
        continue
    text = pom.read_text()
    original = text
    while TEST_DEPS.strip() in text.replace("\t", "        "):
        text = text.replace(TEST_DEPS, "", 1)
    # also remove tab-indented variant
    tabbed = TEST_DEPS.replace("        ", "\t")
    while tabbed.strip() in text:
        text = text.replace(tabbed, "", 1)

    if "testcontainers.version" not in text and "<java.version>" in text:
        text = text.replace(
            "<java.version>17</java.version>",
            "<java.version>17</java.version>\n        <testcontainers.version>1.19.7</testcontainers.version>",
            1,
        )

    needs_testcontainers = "org.testcontainers" not in text.split("</dependencies>", 1)[0]
    needs_archunit = "archunit-junit5" not in text.split("</dependencies>", 1)[0]
    if (needs_testcontainers or needs_archunit) and "</dependencies>" in text:
        head, tail = text.rsplit("</dependencies>", 1)
        insert = ""
        if needs_testcontainers:
            insert += TEST_DEPS.split("<dependency>\n            <groupId>com.tngtech.archunit")[0]
        if needs_archunit and "archunit-junit5" not in insert:
            insert += """
        <dependency>
            <groupId>com.tngtech.archunit</groupId>
            <artifactId>archunit-junit5</artifactId>
            <version>1.2.1</version>
            <scope>test</scope>
        </dependency>
"""
        text = head + insert + "\n    </dependencies>" + tail

    if text != original:
        pom.write_text(text)
        print("fixed", pom.parent.name)
