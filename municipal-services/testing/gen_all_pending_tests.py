#!/usr/bin/env python3
"""Generate meaningful tests for pending municipal-services modules."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

MODULES = [
    "asset-calculator",
    "asset-services",
    "birth-death-services",
    "challan-generation",
    "cnd-service",
    "echallan-services",
    "egov-user-event",
    "estate-management",
    "ewaste-services",
    "land-services",
    "pgr-ai-services",
    "pqm",
    "pqm-anomaly-finder",
    "pt-services-v2",
    "tp-services",
    "turn-io-adapter",
    "vehicle",
    "vendor-management",
]


def write(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content.strip() + "\n", encoding="utf-8")


def response_info_test(pkg: str) -> str:
    return f"""package {pkg};

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseInfoFactoryTest {{

    private final ResponseInfoFactory factory = new ResponseInfoFactory();

    @Test
    void createResponseInfoFromRequestInfo_whenSuccess_returnsSuccessfulStatus() {{
        RequestInfo requestInfo = RequestInfo.builder().apiId("api").msgId("m1").build();
        ResponseInfo response = factory.createResponseInfoFromRequestInfo(requestInfo, true);
        assertThat(response.getStatus()).isEqualTo("successful");
    }}

    @Test
    void createResponseInfoFromRequestInfo_whenFailure_returnsFailedStatus() {{
        ResponseInfo response = factory.createResponseInfoFromRequestInfo(null, false);
        assertThat(response.getStatus()).isEqualTo("failed");
    }}
}}
"""


def sanitize_test(pkg: str) -> str:
    return f"""package {pkg};

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SanitizeHtmlValidatorTest {{

    private SanitizeHtmlValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {{
        validator = new SanitizeHtmlValidator();
    }}

    @Test
    void isValid_whenNull_returnsTrue() {{
        assertThat(validator.isValid(null, context)).isTrue();
    }}

    @Test
    void isValid_whenPlainText_returnsTrue() {{
        assertThat(validator.isValid("hello", context)).isTrue();
    }}

    @Test
    void isValid_whenScriptTag_returnsFalse() {{
        assertThat(validator.isValid("<script>x</script>", context)).isFalse();
    }}
}}
"""


def find_class(module: str, name: str) -> tuple[str, Path] | None:
    main = ROOT / module / "src/main/java"
    for p in main.rglob(f"{name}.java"):
        text = p.read_text(encoding="utf-8", errors="replace")
        m = re.search(r"^package\s+([\w.]+);", text, re.M)
        if m:
            return m.group(1), p
    return None


def rel_test_path(main_java: Path, class_name: str) -> Path:
    rel = main_java.relative_to(main_java.parents[len(main_java.parts) - len(Path("src/main/java").parts) - 1 :])
    # simpler: under src/test/java mirror package
    parts = list(main_java.parts)
    idx = parts.index("main")
    parts[idx] = "test"
    parts[-1] = f"{class_name}Test.java"
    return Path(*parts)


def test_path(module: str, pkg: str, class_name: str) -> Path:
    return ROOT / module / "src/test/java" / pkg.replace(".", "/") / f"{class_name}Test.java"


def gen_for_module(module: str) -> int:
    n = 0
    rif = find_class(module, "ResponseInfoFactory")
    if rif:
        pkg, _ = rif
        write(test_path(module, pkg, "ResponseInfoFactory"), response_info_test(pkg))
        n += 1

    san = find_class(module, "SanitizeHtmlValidator")
    if san:
        pkg, _ = san
        write(test_path(module, pkg, "SanitizeHtmlValidator"), sanitize_test(pkg))
        n += 1

    return n


# --- hand-crafted high-value tests ---
CUSTOM: dict[str, list[tuple[str, str]]] = {
    "asset-calculator": [
        (
            "org/egov/asset/calculator/utils/CalculationUtilsTest.java",
            """package org.egov.asset.calculator.utils;

import org.egov.asset.calculator.web.models.AuditDetails;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalculationUtilsTest {

    private final CalculationUtils utils = new CalculationUtils();

    @Test
    void getAuditDetails_whenCreate_setsCreatedFields() {
        AuditDetails audit = utils.getAuditDetails("user", true);
        assertThat(audit.getCreatedBy()).isEqualTo("user");
        assertThat(audit.getCreatedTime()).isNotNull();
    }
}
""",
        ),
        (
            "org/egov/asset/calculator/validator/MDMSValidatorTest.java",
            """package org.egov.asset.calculator.validator;

import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class MDMSValidatorTest {

    @InjectMocks
    private MDMSValidator validator;

    @Test
    void getAttributeValues_whenInvalidPayload_throws() {
        assertThatThrownBy(() -> validator.getAttributeValues(Map.of()))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void validateMdmsData_whenMasterMissing_throws() {
        Map<String, Object> mdms = Map.of("MdmsRes", Map.of("FSM", Map.of()));
        assertThatThrownBy(() -> validator.validateMdmsData(mdms))
                .isInstanceOf(CustomException.class);
    }
}
""",
        ),
    ],
    "asset-services": [
        (
            "org/egov/asset/util/AssetUtilTest.java",
            """package org.egov.asset.util;

import org.egov.asset.repository.AssetRepository;
import org.egov.asset.web.models.AuditDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AssetUtilTest {

    @Mock
    private AssetRepository assetRepository;

    @Test
    void getAuditDetails_whenCreate_populatesTimestamps() {
        AssetUtil util = new AssetUtil(assetRepository);
        AuditDetails audit = util.getAuditDetails("user-1", true);
        assertThat(audit.getCreatedBy()).isEqualTo("user-1");
    }
}
""",
        ),
        (
            "org/egov/asset/repository/querybuilder/AssetQueryBuilderTest.java",
            """package org.egov.asset.repository.querybuilder;

import org.egov.asset.web.models.AssetSearchCriteria;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AssetQueryBuilderTest {

    private final AssetQueryBuilder builder = new AssetQueryBuilder();

    @Test
    void getAssetSearchQuery_withTenantId_addsPredicate() {
        AssetSearchCriteria criteria = AssetSearchCriteria.builder().tenantId("pb").build();
        List<Object> params = new ArrayList<>();
        String query = builder.getAssetSearchQuery(criteria, params);
        assertThat(query).contains("tenantid");
        assertThat(params).isNotEmpty();
    }
}
""",
        ),
    ],
    "birth-death-services": [
        (
            "org/bel/birthdeath/utils/CommonUtilsTest.java",
            """package org.bel.birthdeath.utils;

import org.bel.birthdeath.common.model.AuditDetails;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommonUtilsTest {

    private final CommonUtils utils = new CommonUtils();

    @Test
    void addfullName_concatenatesNonNullParts() {
        assertThat(utils.addfullName("A", "B", "C")).isEqualTo("A B C");
    }

    @Test
    void getAuditDetails_whenCreate_setsCreatedBy() {
        AuditDetails audit = utils.getAuditDetails("user", true);
        assertThat(audit.getCreatedBy()).isEqualTo("user");
    }
}
""",
        ),
    ],
    "echallan-services": [
        (
            "org/egov/echallan/util/CommonUtilsTest.java",
            """package org.egov.echallan.util;

import org.egov.echallan.model.AuditDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CommonUtilsTest {

    @InjectMocks
    private CommonUtils commonUtils;

    @Test
    void getAuditDetails_whenCreate_populatesFields() {
        AuditDetails audit = commonUtils.getAuditDetails("user", true);
        assertThat(audit.getCreatedBy()).isEqualTo("user");
    }
}
""",
        ),
    ],
    "egov-user-event": [
        (
            "org/egov/userevent/utils/UserEventsUtilsTest.java",
            """package org.egov.userevent.utils;

import org.egov.userevent.web.contract.EventSearchCriteria;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserEventsUtilsTest {

    private final UserEventsUtils utils = new UserEventsUtils();

    @Test
    void buildRecepientListForSearch_addsAllKeyword() {
        EventSearchCriteria criteria = EventSearchCriteria.builder().tenantId("pb.amritsar").build();
        utils.buildRecepientListForSearch(criteria);
        assertThat(criteria.getRecepients()).contains(UserEventsConstants.ALL_KEYWORD);
    }

    @Test
    void buildRecepientListForSearch_withRole_expandsPatterns() {
        EventSearchCriteria criteria = EventSearchCriteria.builder()
                .roles(List.of("CITIZEN.EMPLOYEE"))
                .tenantId("pb")
                .build();
        utils.buildRecepientListForSearch(criteria);
        assertThat(criteria.getRecepients()).anyMatch(r -> r.contains("CITIZEN"));
    }
}
""",
        ),
    ],
    "estate-management": [
        (
            "upyog/util/BillingPeriodUtilTest.java",
            """package upyog.util;

import org.junit.jupiter.api.Test;
import upyog.web.models.BillingCycle;
import upyog.web.models.BillingPeriod;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BillingPeriodUtilTest {

    @Test
    void getBillingPeriod_monthly_returnsOneMonthWindow() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        BillingPeriod period = BillingPeriodUtil.getBillingPeriod(start, BillingCycle.MONTHLY, null);
        assertThat(period.getFromDate()).isEqualTo(start);
        assertThat(period.getToDate()).isEqualTo(LocalDate.of(2024, 1, 31));
    }
}
""",
        ),
    ],
    "ewaste-services": [
        (
            "org/egov/ewst/util/EwasteUtilTest.java",
            """package org.egov.ewst.util;

import org.egov.ewst.config.EwasteConfiguration;
import org.egov.ewst.models.AuditDetails;
import org.egov.ewst.repository.ServiceRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EwasteUtilTest {

    @Mock
    private EwasteConfiguration configs;
    @Mock
    private ServiceRequestRepository restRepo;

    @Test
    void getAuditDetails_whenCreate_setsCreatedBy() {
        EwasteUtil util = new EwasteUtil(new ObjectMapper(), configs, restRepo);
        AuditDetails audit = util.getAuditDetails("user", true);
        assertThat(audit.getCreatedBy()).isEqualTo("user");
    }
}
""",
        ),
    ],
    "land-services": [
        (
            "org/egov/land/validator/LandMDMSValidatorTest.java",
            """package org.egov.land.validator;

import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class LandMDMSValidatorTest {

    @InjectMocks
    private LandMDMSValidator validator;

    @Test
    void getAttributeValues_whenInvalidPayload_throws() {
        assertThatThrownBy(() -> validator.getAttributeValues(Map.of()))
                .isInstanceOf(CustomException.class);
    }
}
""",
        ),
    ],
    "pqm": [
        (
            "org/egov/pqm/util/JsonParserTest.java",
            """package org.egov.pqm.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.pqm.error.CustomException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonParserTest {

    private final JsonParser<Map<String, Object>> parser = new JsonParser<>(new ObjectMapper());

    @Test
    void parseJsonToMap_whenInvalidJson_throwsCustomException() {
        assertThatThrownBy(() -> parser.parseJsonToMap("{}", Map.class))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void parseJsonToMap_whenValidPayload_returnsCodeMap() {
        String json = "{\\"mdms\\":[{\\"data\\":{\\"code\\":\\"C1\\",\\"name\\":\\"n\\"}}]}";
        Map<String, Map> out = parser.parseJsonToMap(json, Map.class);
        assertThat(out).containsKey("C1");
    }
}
""",
        ),
    ],
    "pqm-anomaly-finder": [
        (
            "org/egov/pqm/anomaly/finder/util/PqmAnomalyFinderUtilTest.java",
            """package org.egov.pqm.anomaly.finder.util;

import org.egov.pqm.anomaly.finder.web.model.AuditDetails;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PqmAnomalyFinderUtilTest {

    private final PqmAnomalyFinderUtil util = new PqmAnomalyFinderUtil();

    @Test
    void getAuditDetails_whenCreate_setsCreatedBy() {
        AuditDetails audit = util.getAuditDetails("user", true);
        assertThat(audit.getCreatedBy()).isEqualTo("user");
    }

    @Test
    void defaultJsonPathConfig_doesNotThrow() {
        util.defaultJsonPathConfig();
    }
}
""",
        ),
    ],
    "pt-services-v2": [
        (
            "org/egov/pt/util/PropertyUtilTest.java",
            """package org.egov.pt.util;

import org.egov.common.contract.request.RequestInfo;
import org.egov.pt.web.models.AuditDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PropertyUtilTest {

    @InjectMocks
    private PropertyUtil propertyUtil;

    @Test
    void getAuditDetails_whenCreate_populatesFields() {
        AuditDetails audit = propertyUtil.getAuditDetails("user", true);
        assertThat(audit.getCreatedBy()).isEqualTo("user");
    }

    @Test
    void prepareMdMsRequest_buildsModuleCriteria() {
        RequestInfo ri = RequestInfo.builder().apiId("pt").build();
        var req = propertyUtil.prepareMdMsRequest("pb", "PropertyTax", List.of("UsageCategory"), null, ri);
        assertThat(req.getMdmsCriteria().getTenantId()).isEqualTo("pb");
    }
}
""",
        ),
        (
            "org/egov/pt/repository/builder/PropertyQueryBuilderTest.java",
            """package org.egov.pt.repository.builder;

import org.egov.pt.models.oldProperty.PropertyCriteria;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PropertyQueryBuilderTest {

    private final PropertyQueryBuilder builder = new PropertyQueryBuilder();

    @Test
    void getPropertySearchQuery_withTenantId_addsPredicate() {
        PropertyCriteria criteria = PropertyCriteria.builder().tenantId("pb.amritsar").build();
        List<Object> params = new ArrayList<>();
        String query = builder.getPropertySearchQuery(criteria, params);
        assertThat(query.toLowerCase()).contains("tenant");
        assertThat(params).isNotEmpty();
    }
}
""",
        ),
    ],
    "turn-io-adapter": [
        (
            "org/egov/rb/pgr/v2/models/AdditionalDetailValidatorTest.java",
            """package org.egov.rb.pgr.v2.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.validation.ConstraintValidatorContext;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AdditionalDetailValidatorTest {

    private AdditionalDetailValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new AdditionalDetailValidator();
        CharacterConstraint constraint = new CharacterConstraint() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return CharacterConstraint.class;
            }

            @Override
            public String message() {
                return "m";
            }

            @Override
            public Class<?>[] groups() {
                return new Class[0];
            }

            @Override
            public Class<? extends jakarta.validation.Payload>[] payload() {
                return new Class[0];
            }

            @Override
            public int size() {
                return 5;
            }
        };
        validator.initialize(constraint);
    }

    @Test
    void isValid_whenNull_returnsTrue() {
        assertThat(validator.isValid(null, context)).isTrue();
    }

    @Test
    void isValid_whenTooLong_returnsFalse() {
        assertThat(validator.isValid("123456", context)).isFalse();
    }

    @Test
    void isValid_whenWithinSize_returnsTrue() {
        assertThat(validator.isValid("12345", context)).isTrue();
    }
}
""",
        ),
        (
            "org/egov/rb/util/MDMSUtilsTest.java",
            """package org.egov.rb.util;

import org.egov.common.contract.request.RequestInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MDMSUtilsTest {

    @InjectMocks
    private MDMSUtils mdmsUtils;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(mdmsUtils, "mdmsHost", "http://mdms");
        ReflectionTestUtils.setField(mdmsUtils, "mdmsSearchEndpoint", "/mdms-v2/_search");
    }

    @Test
    void getMDMSRequest_includesTenantId() {
        RequestInfo ri = RequestInfo.builder().apiId("rb").build();
        var req = mdmsUtils.getMDMSRequest(ri, "pb");
        assertThat(req.getMdmsCriteria().getTenantId()).isEqualTo("pb");
    }
}
""",
        ),
    ],
    "vendor-management": [
        (
            "org/egov/vendor/utils/VendorUtilTest.java",
            """package org.egov.vendor.utils;

import org.egov.vendor.web.models.AuditDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class VendorUtilTest {

    @InjectMocks
    private VendorUtil vendorUtil;

    @Test
    void getAuditDetails_whenCreate_setsCreatedBy() {
        AuditDetails audit = vendorUtil.getAuditDetails("user", true);
        assertThat(audit.getCreatedBy()).isEqualTo("user");
    }
}
""",
        ),
        (
            "org/egov/vendor/service/EnrichmentServiceTest.java",
            """package org.egov.vendor.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.vendor.config.VendorConfiguration;
import org.egov.vendor.repository.IdGenRepository;
import org.egov.vendor.utils.VendorUtil;
import org.egov.vendor.web.models.AuditDetails;
import org.egov.vendor.web.models.VendorAdditionalDetails;
import org.egov.vendor.web.models.VendorAdditionalDetailsRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrichmentServiceTest {

    @Mock
    private VendorConfiguration config;
    @Mock
    private VendorUtil vendorUtil;
    @Mock
    private IdGenRepository idGenRepository;

    @InjectMocks
    private EnrichmentService enrichmentService;

    @Test
    void enrichAssetOtherOperationsUpdateRequest_setsAuditDetails() {
        when(vendorUtil.getAuditDetails(eq("uuid-1"), eq(true)))
                .thenReturn(AuditDetails.builder().createdBy("uuid-1").build());
        VendorAdditionalDetailsRequest request = VendorAdditionalDetailsRequest.builder()
                .requestInfo(RequestInfo.builder().userInfo(User.builder().uuid("uuid-1").build()).build())
                .vendorAdditionalDetails(VendorAdditionalDetails.builder().build())
                .build();

        enrichmentService.enrichAssetOtherOperationsUpdateRequest(request);

        assertThat(request.getVendorAdditionalDetails().getAuditDetails()).isNotNull();
    }
}
""",
        ),
    ],
}


def main():
    total = 0
    for module in MODULES:
        c = gen_for_module(module)
        for rel, body in CUSTOM.get(module, []):
            write(ROOT / module / "src/test/java" / rel, body)
            c += 1
        print(f"{module}: {c} test files")
        total += c
    print(f"total: {total}")


if __name__ == "__main__":
    main()
