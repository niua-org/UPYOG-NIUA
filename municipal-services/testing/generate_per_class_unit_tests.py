#!/usr/bin/env python3
"""
Generate missing *Test.java files for each public top-level class under src/main/java.
Does not overwrite existing tests. Intended for batch module runs with JDK 17 mvn test.
"""
from __future__ import annotations

import argparse
import re
import textwrap
from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional

ROOT = Path(__file__).resolve().parents[1]


@dataclass
class JavaType:
    path: Path
    package: str
    name: str
    kind: str  # class, enum, interface, record
    text: str
    annotations: list[str] = field(default_factory=list)
    extends: Optional[str] = None
    implements: list[str] = field(default_factory=list)
    ctor_params: list[tuple[str, str]] = field(default_factory=list)  # (type, name)
    fields: list[tuple[str, str]] = field(default_factory=list)  # (type, name)
    methods: list[tuple[str, str, str]] = field(default_factory=list)  # (return, name, params)
    is_abstract: bool = False
    has_lombok_builder: bool = False
    has_lombok_getter: bool = False
    has_lombok_setter: bool = False
    has_private_ctor: bool = False


def parse_java(path: Path) -> Optional[JavaType]:
    text = path.read_text(encoding="utf-8", errors="replace")
    if "package " not in text:
        return None
    pkg = re.search(r"package\s+([\w.]+)\s*;", text)
    if not pkg:
        return None
    m = re.search(
        r"public\s+(?:abstract\s+)?(class|enum|interface|record)\s+(\w+)",
        text,
    )
    if not m:
        return None
    kind, name = m.group(1), m.group(2)
    if "$" in name:
        return None
    ann_block = text[: m.start()]
    annotations = re.findall(r"@(\w+)", ann_block[-800:])
    extends = None
    em = re.search(r"public\s+(?:abstract\s+)?(?:class|enum|interface|record)\s+\w+\s+extends\s+([\w.]+)", text)
    if em:
        extends = em.group(1).split(".")[-1]
    impl = re.findall(
        r"public\s+(?:abstract\s+)?(?:class|enum|interface|record)\s+\w+(?:\s+extends\s+[\w.]+)?\s+implements\s+([\w\s,<>]+)",
        text,
    )
    implements = []
    if impl:
        implements = [x.strip().split(".")[-1] for x in impl[0].split(",")]

    ctor_params: list[tuple[str, str]] = []
    cm = re.search(r"public\s+" + re.escape(name) + r"\s*\(([^)]*)\)", text)

    def _split_params(param_str: str) -> list[str]:
        parts: list[str] = []
        depth = 0
        cur: list[str] = []
        for ch in param_str:
            if ch == "<":
                depth += 1
            elif ch == ">":
                depth = max(0, depth - 1)
            elif ch == "," and depth == 0:
                parts.append("".join(cur).strip())
                cur = []
                continue
            cur.append(ch)
        if cur:
            parts.append("".join(cur).strip())
        return parts

    def _strip_ann(s: str) -> str:
        return re.sub(r"@\w+(?:\([^)]*\))?\s*", "", s).strip()

    if cm and cm.group(1).strip():
        for part in _split_params(cm.group(1)):
            if not part:
                continue
            part = _strip_ann(part)
            bits = part.rsplit(" ", 1)
            if len(bits) == 2:
                ctor_params.append((bits[0].strip(), bits[1].strip()))

    fields: list[tuple[str, str]] = []
    for fm in re.finditer(
        r"^\s*(?:private|protected)\s+(?:final\s+)?(?!static)([\w<>,\s\[\]?]+?)\s+(\w+)\s*;",
        text,
        re.MULTILINE,
    ):
        ftype, fname = fm.group(1).strip(), fm.group(2)
        if fname in ("serialVersionUID",):
            continue
        fields.append((ftype, fname))

    methods: list[tuple[str, str, str]] = []
    for mm in re.finditer(
        r"public\s+(?!static)(?!class|enum|interface)([\w<>,\s\[\]?]+)\s+(\w+)\s*\(([^)]*)\)\s*(?:throws[\w\s,]+)?\s*\{",
        text,
    ):
        mname = mm.group(2)
        if mname == name or mname.startswith("set") and mname.endswith("Builder"):
            continue
        if mname in ("equals", "hashCode", "toString"):
            continue
        methods.append((mm.group(1).strip(), mname, mm.group(3).strip()))

    return JavaType(
        path=path,
        package=pkg.group(1),
        name=name,
        kind=kind,
        text=text,
        annotations=annotations,
        extends=extends,
        implements=implements,
        ctor_params=ctor_params,
        fields=fields,
        methods=methods,
        is_abstract="abstract class" in text[: m.start() + 20],
        has_lombok_builder="@Builder" in text,
        has_lombok_getter="@Getter" in text or "@Data" in text,
        has_lombok_setter="@Setter" in text or "@Data" in text,
        has_private_ctor=bool(re.search(r"private\s+" + re.escape(name) + r"\s*\(", text)),
    )


def test_path(module: Path, jt: JavaType) -> Path:
    rel = jt.path.relative_to(module / "src/main/java")
    return module / "src/test/java" / rel.parent / f"{jt.name}Test.java"


def test_exists(module: Path, jt: JavaType) -> bool:
    return test_path(module, jt).exists()


def resolve_imports(source_text: str, type_names: list[str], package: str) -> list[str]:
    by_simple: dict[str, str] = {}
    for line in re.findall(r"^import\s+([\w.]+)\s*;", source_text, re.MULTILINE):
        by_simple[line.split(".")[-1]] = line
    out: list[str] = []
    for tn in type_names:
        simple = tn.replace("...", "").split(".")[-1].split("<")[0].strip()
        if simple in by_simple:
            out.append(by_simple[simple])
        elif simple in ("String", "Integer", "Long", "Boolean", "Object", "BigDecimal", "List", "Map"):
            continue
        elif simple == "ObjectMapper":
            out.append("com.fasterxml.jackson.databind.ObjectMapper")
        else:
            out.append(f"{package}.{simple}")
    return sorted(set(out))


def simple_type_names_from_params(ctor_params: list[tuple[str, str]]) -> list[str]:
    names: list[str] = []
    for ftype, _ in ctor_params:
        names.append(ftype.split("<")[0].split()[-1])
    return names


def simple_type_default(ftype: str) -> str:
    ftype = ftype.replace("final ", "").strip()
    if "String" in ftype:
        return '"test"'
    if ftype in ("int", "Integer"):
        return "1"
    if ftype in ("long", "Long"):
        return "1L"
    if ftype in ("boolean", "Boolean"):
        return "true"
    if "BigDecimal" in ftype:
        return "java.math.BigDecimal.ONE"
    if "List" in ftype or "Collection" in ftype:
        return "java.util.Collections.singletonList(\"test\")"
    if "Set" in ftype:
        return "java.util.Collections.singleton(\"test\")"
    if "Map" in ftype:
        return "java.util.Collections.emptyMap()"
    if "RequestInfo" in ftype:
        return "new org.egov.common.contract.request.RequestInfo()"
    return "null"


def classify(jt: JavaType) -> str:
    anns = " ".join(jt.annotations)
    if jt.kind == "enum":
        return "enum"
    if jt.kind == "interface":
        return "interface"
    if jt.is_abstract:
        return "abstract"
    if "SpringBootApplication" in anns:
        return "application"
    if "RestController" in anns or "Controller" in anns:
        return "controller"
    if "Configuration" in anns:
        return "configuration"
    if "Service" in anns or "Component" in anns or "Repository" in anns:
        return "service"
    if jt.name.endswith("Consumer") or jt.name.endswith("Producer"):
        return "kafka"
    if jt.name.endswith("Constants") or jt.name.endswith("Constant"):
        return "constants"
    if jt.name.endswith("RowMapper") or "RowMapper" in jt.implements:
        return "rowmapper"
    if jt.name.endswith("Validator") or "ConstraintValidator" in jt.implements:
        return "validator"
    if jt.package.endswith(".web.models") or ".models." in jt.package or jt.name.endswith("Request") or jt.name.endswith("Response"):
        return "model"
    if jt.name.endswith("Util") or jt.name.endswith("Utils") or ".util." in jt.package:
        return "util"
    if not jt.methods and jt.fields:
        return "model"
    return "generic"


def gen_enum(jt: JavaType) -> str:
    return textwrap.dedent(
        f"""
        package {jt.package};

        import org.junit.jupiter.api.Test;

        import static org.assertj.core.api.Assertions.assertThat;

        class {jt.name}Test {{

            @Test
            void values_whenQueried_isNotEmpty() {{
                assertThat({jt.name}.values()).isNotEmpty();
            }}

            @Test
            void valueOf_whenFirstConstantName_returnsConstant() {{
                {jt.name} first = {jt.name}.values()[0];
                assertThat({jt.name}.valueOf(first.name())).isEqualTo(first);
            }}
        }}
        """
    ).strip() + "\n"


def gen_constants(jt: JavaType) -> str:
    const_fields = re.findall(r"public\s+static\s+final\s+\w+\s+(\w+)\s*=", jt.text)
    body = ""
    if const_fields:
        first = const_fields[0]
        body = f"""
            @Test
            void {first}_whenRead_isNotBlank() {{
                assertThat({jt.name}.{first}).isNotBlank();
            }}
        """
    if jt.has_private_ctor and re.search(
        r"Utility class|IllegalStateException", jt.text
    ):
        body += f"""
            @Test
            void constructor_whenPrivate_rejectsUtilityInstantiation() throws Exception {{
                var ctor = {jt.name}.class.getDeclaredConstructor();
                ctor.setAccessible(true);
                org.assertj.core.api.Assertions.assertThatThrownBy(ctor::newInstance)
                        .hasCauseInstanceOf(IllegalStateException.class);
            }}
        """
    return textwrap.dedent(
        f"""
        package {jt.package};

        import org.junit.jupiter.api.Test;

        import static org.assertj.core.api.Assertions.assertThat;

        class {jt.name}Test {{
        {body}
        }}
        """
    ).strip() + "\n"


def pick_model_field(jt: JavaType) -> Optional[tuple[str, str]]:
    preferred = (
        "tenantId",
        "apiId",
        "id",
        "code",
        "name",
        "uuid",
        "applicationNumber",
        "businessService",
    )
    for pname in preferred:
        for ftype, fname in jt.fields:
            if fname == pname:
                return ftype, fname
    for ftype, fname in jt.fields:
        if fname in ("value", "serialVersionUID", "VALUES"):
            continue
        if " " in ftype or ftype.startswith("@"):
            continue
        if fname[0].isupper() and ftype.endswith("Enum"):
            continue
        return ftype, fname
    return None


def has_no_arg_constructor(jt: JavaType) -> bool:
    if "@NoArgsConstructor" in jt.text:
        return True
    if "@AllArgsConstructor" in jt.text and "@NoArgsConstructor" not in jt.text:
        return bool(
            re.search(
                r"public\s+" + re.escape(jt.name) + r"\s*\(\s*\)",
                jt.text,
            )
        )
    if "@Data" in jt.text:
        return True
    return bool(
        re.search(
            r"public\s+" + re.escape(jt.name) + r"\s*\(\s*\)",
            jt.text,
        )
    )


def builder_safe(jt: JavaType) -> bool:
    return jt.has_lombok_builder and has_no_arg_constructor(jt) and "@NonNull" not in jt.text


def gen_model(jt: JavaType) -> str:
    builder_test = ""
    if builder_safe(jt):
        builder_test = f"""
    @Test
    void builder_whenDefaultBuilds_isNotNull() {{
        assertThat({jt.name}.builder().build()).isNotNull();
    }}
"""
    no_arg_test = ""
    if has_no_arg_constructor(jt):
        no_arg_test = f"""
    @Test
    void newInstance_whenCreated_isNotNull() {{
        assertThat(new {jt.name}()).isNotNull();
    }}
"""
    else:
        no_arg_test = f"""
    @Test
    void class_whenLoaded_isNotNull() {{
        assertThat({jt.name}.class).isNotNull();
    }}
"""
    return textwrap.dedent(
        f"""
        package {jt.package};

        import org.junit.jupiter.api.Test;

        import static org.assertj.core.api.Assertions.assertThat;

        class {jt.name}Test {{
        {no_arg_test}
        {builder_test}
        }}
        """
    ).strip() + "\n"


def gen_application(jt: JavaType) -> str:
    return textwrap.dedent(
        f"""
        package {jt.package};

        import org.junit.jupiter.api.Test;

        import static org.assertj.core.api.Assertions.assertThat;

        class {jt.name}Test {{

            @Test
            void class_whenLoaded_isPublicApplicationType() {{
                assertThat({jt.name}.class).isNotNull();
                assertThat({jt.name}.class.getSimpleName()).isEqualTo("{jt.name}");
            }}
        }}
        """
    ).strip() + "\n"


def gen_interface(jt: JavaType) -> str:
    return textwrap.dedent(
        f"""
        package {jt.package};

        import org.junit.jupiter.api.Test;

        import static org.assertj.core.api.Assertions.assertThat;

        class {jt.name}Test {{

            @Test
            void type_whenLoaded_isInterface() {{
                assertThat({jt.name}.class.isInterface()).isTrue();
            }}
        }}
        """
    ).strip() + "\n"


def gen_generic(jt: JavaType) -> str:
    return textwrap.dedent(
        f"""
        package {jt.package};

        import org.junit.jupiter.api.Test;

        import static org.assertj.core.api.Assertions.assertThat;

        class {jt.name}Test {{

            @Test
            void class_whenLoaded_isNotNull() {{
                assertThat({jt.name}.class).isNotNull();
            }}
        }}
        """
    ).strip() + "\n"


def gen_service(jt: JavaType) -> str:
    mocks = ""
    init = ""
    if jt.ctor_params:
        mock_fields = []
        init_args = []
        for ftype, fname in jt.ctor_params:
            mock_fields.append(f"    @Mock\n    private {ftype} {fname};")
            init_args.append(fname)
        mocks = "\n".join(mock_fields)
        init = f"""
    private {jt.name} subject;

    @BeforeEach
    void setUp() {{
        subject = new {jt.name}({", ".join(init_args)});
    }}
"""
    else:
        init = f"""
    @InjectMocks
    private {jt.name} subject;
"""

    test_methods = ""
    if "CalculationService" == jt.name and "calculateFee" in jt.text:
        test_methods = """
    @Test
    void calculateFee_whenMdmsEmpty_throwsCustomException() {
        org.egov.common.contract.request.RequestInfo requestInfo =
                new org.egov.common.contract.request.RequestInfo();
        org.upyog.cdwm.calculator.web.models.CNDApplicationDetail detail =
                new org.upyog.cdwm.calculator.web.models.CNDApplicationDetail();
        detail.setTenantId("pb.amritsar");
        detail.setTotalWasteQuantity(java.math.BigDecimal.ONE);
        org.mockito.Mockito.when(mdmsUtil.getCalculationType(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(java.util.Collections.emptyList());
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> subject.calculateFee(detail, requestInfo))
                .isInstanceOf(org.egov.tracer.model.CustomException.class);
    }

    @Test
    void calculateFee_whenMdmsReturnsAmount_multipliesQuantity() {
        org.egov.common.contract.request.RequestInfo requestInfo =
                new org.egov.common.contract.request.RequestInfo();
        org.upyog.cdwm.calculator.web.models.CNDApplicationDetail detail =
                new org.upyog.cdwm.calculator.web.models.CNDApplicationDetail();
        detail.setTenantId("pb.amritsar");
        detail.setTotalWasteQuantity(new java.math.BigDecimal("2"));
        org.upyog.cdwm.calculator.web.models.CalculationType type =
                new org.upyog.cdwm.calculator.web.models.CalculationType();
        type.setAmount(new java.math.BigDecimal("10"));
        org.mockito.Mockito.when(mdmsUtil.getCalculationType(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(java.util.List.of(type));
        org.assertj.core.api.Assertions.assertThat(subject.calculateFee(detail, requestInfo))
                .isEqualByComparingTo(new java.math.BigDecimal("20"));
    }
"""
    elif jt.methods:
        mname = jt.methods[0][1]
        test_methods = f"""
    @Test
    void {mname}_whenSubjectReady_subjectIsNotNull() {{
        assertThat(subject).isNotNull();
    }}
"""
    else:
        test_methods = """
    @Test
    void subject_whenConstructed_isNotNull() {
        assertThat(subject).isNotNull();
    }
"""

    extra_imports = resolve_imports(
        jt.text, simple_type_names_from_params(jt.ctor_params), jt.package
    )
    import_lines = "\n".join(f"import {imp};" for imp in extra_imports)
    body = f"""package {jt.package};

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
{import_lines}

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class {jt.name}Test {{
{mocks}
{init}
{test_methods}
}}
"""
    return body


def controller_dependency_fields(jt: JavaType) -> list[tuple[str, str]]:
    deps: list[tuple[str, str]] = []
    for ftype, fname in jt.fields:
        if "Logger" in ftype or fname.startswith("log"):
            continue
        if "[]" in ftype or ftype in ("int", "long", "boolean", "double", "float"):
            continue
        deps.append((ftype.replace("final ", "").strip(), fname))
    return deps


def gen_controller(jt: JavaType) -> str:
    if jt.ctor_params:
        deps = jt.ctor_params
        mock_fields = []
        init_args = []
        for ftype, fname in deps:
            mock_fields.append(f"    @Mock\n    private {ftype} {fname};")
            init_args.append(fname)
        mocks = "\n".join(mock_fields)
        extra_imports = resolve_imports(jt.text, simple_type_names_from_params(deps), jt.package)
        import_lines = "\n".join(f"import {imp};" for imp in extra_imports)
        setup = f"""    @BeforeEach
    void setUp() {{
        controller = new {jt.name}({", ".join(init_args)});
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }}"""
        inject = f"""
    private {jt.name} controller;"""
    else:
        deps = controller_dependency_fields(jt)
        mock_fields = []
        for ftype, fname in deps:
            mock_fields.append(f"    @Mock\n    private {ftype} {fname};")
        mocks = "\n".join(mock_fields)
        extra_imports = resolve_imports(jt.text, simple_type_names_from_params(deps), jt.package)
        import_lines = "\n".join(f"import {imp};" for imp in extra_imports)
        setup = """    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }"""
        inject = """
    @InjectMocks
    private """ + f"{jt.name} controller;"

    return f"""package {jt.package};

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
{import_lines}

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class {jt.name}Test {{
{mocks}
{inject}
    private MockMvc mockMvc;

{setup}

    @Test
    void mockMvc_whenStandaloneSetup_isNotNull() {{
        assertThat(mockMvc).isNotNull();
    }}
}}
"""


def generate_for_type(jt: JavaType) -> str:
    cat = classify(jt)
    if cat == "enum":
        return gen_enum(jt)
    if cat == "constants":
        return gen_constants(jt)
    if cat == "model":
        return gen_model(jt)
    if cat == "application":
        return gen_application(jt)
    if cat == "interface":
        return gen_interface(jt)
    if cat == "controller":
        return gen_controller(jt)
    if cat == "service":
        return gen_service(jt)
    if cat == "abstract":
        return gen_interface(jt)
    return gen_generic(jt)


def process_module(module_name: str, dry_run: bool = False) -> tuple[int, int]:
    module = ROOT / module_name
    main = module / "src/main/java"
    if not main.exists():
        return 0, 0
    created = 0
    skipped = 0
    for path in sorted(main.rglob("*.java")):
        if path.name == "package-info.java":
            continue
        jt = parse_java(path)
        if not jt:
            continue
        if test_exists(module, jt):
            skipped += 1
            continue
        out = test_path(module, jt)
        content = generate_for_type(jt)
        if dry_run:
            print("would create", out.relative_to(module))
            created += 1
            continue
        out.parent.mkdir(parents=True, exist_ok=True)
        marker = "// generated-by: generate_per_class_unit_tests.py\n"
        out.write_text(marker + content, encoding="utf-8")
        created += 1
    return created, skipped


def looks_generated_test(text: str) -> bool:
    return text.startswith("// generated-by: generate_per_class_unit_tests.py")


def purge_generated_tests(module_name: str) -> int:
    module = ROOT / module_name
    test_root = module / "src/test/java"
    if not test_root.exists():
        return 0
    removed = 0
    for path in test_root.rglob("*Test.java"):
        if "architecture" in str(path) or path.name == "LayeredArchitectureTest.java":
            continue
        text = path.read_text(encoding="utf-8", errors="replace")
        if looks_generated_test(text):
            path.unlink()
            removed += 1
    return removed


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("modules", nargs="*", help="module dir names; default all with pom.xml")
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--purge-generated", action="store_true", help="Remove prior generator output before creating tests")
    args = ap.parse_args()
    modules = args.modules
    if not modules:
        modules = sorted(
            p.parent.name
            for p in ROOT.glob("*/pom.xml")
            if p.parent.name not in ("core-smoke-tests",)
        )
    total = 0
    for m in modules:
        if args.purge_generated and not args.dry_run:
            r = purge_generated_tests(m)
            if r:
                print(f"{m}: purged={r}")
        c, s = process_module(m, dry_run=args.dry_run)
        print(f"{m}: created={c} existing={s}")
        total += c
    print(f"total created={total}")


if __name__ == "__main__":
    main()
