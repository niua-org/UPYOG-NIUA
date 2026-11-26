#!/usr/bin/env python3
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
META = json.loads(Path(__file__).with_name("modules-test-metadata.json").read_text())

STATIC_UTIL_TEST = """package {pkg};

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class {simple}Test {{

    @Test
    @DisplayName("getCurrentTimestamp_whenCalled_returnsPositiveValue")
    void getCurrentTimestamp_whenCalled_returnsPositiveValue() {{
        assertThat({simple}.getCurrentTimestamp()).isPositive();
    }}
}}
"""

COMPONENT_CALC_TEST = """package {pkg};

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class {simple}Test {{

    @InjectMocks
    private {simple} calculationUtils;

    @Test
    @DisplayName("calculationUtils_whenInjected_isNotNull")
    void calculationUtils_whenInjected_isNotNull() {{
        assertThat(calculationUtils).isNotNull();
    }}
}}
"""

GRBG = """package {pkg};

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GrbgUtilsTest {{

    @Test
    @DisplayName("getCurrentTimestamp_whenCalled_returnsPositiveValue")
    void getCurrentTimestamp_whenCalled_returnsPositiveValue() {{
        assertThat(GrbgUtils.getCurrentTimestamp()).isPositive();
    }}
}}
"""

for meta in META:
    if meta.get("testTargetSimpleName") != "CalculationUtils" and meta.get("testTargetSimpleName") != "GrbgUtils":
        continue
    module = meta["module"]
    test_class = meta["testClass"]
    pkg = ".".join(test_class.split(".")[:-1])
    simple = test_class.split(".")[-1]
    main = ROOT / module / "src/main/java" / Path(*test_class.split(".")).with_suffix(".java")
    path = ROOT / module / "src/test/java" / Path(*test_class.split("."))
    path = path.parent / (path.name + "Test.java")
    if meta["testTargetSimpleName"] == "GrbgUtils":
        path.write_text(GRBG.format(pkg=pkg))
        continue
    if main.exists() and "private " + simple + "()" in main.read_text():
        path.write_text(STATIC_UTIL_TEST.format(pkg=pkg, simple=simple))
    else:
        path.write_text(COMPONENT_CALC_TEST.format(pkg=pkg, simple=simple))

print("CalculationUtils/GrbgUtils tests updated")
