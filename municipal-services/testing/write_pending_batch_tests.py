#!/usr/bin/env python3
"""Write meaningful unit tests for the 19 pending municipal-services modules."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

SANITIZE_TEMPLATE = '''package {pkg};

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
        assertThat(validator.isValid("<script>alert(1)</script>", context)).isFalse();
    }}
}}
'''

RESPONSE_INFO_TEMPLATE = '''package {pkg};

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
        assertThat(response.getApiId()).isEqualTo("api");
    }}

    @Test
    void createResponseInfoFromRequestInfo_whenFailure_returnsFailedStatus() {{
        ResponseInfo response = factory.createResponseInfoFromRequestInfo(null, false);

        assertThat(response.getStatus()).isEqualTo("failed");
    }}
}}
'''

MDMS_TEMPLATE = '''package {pkg};

import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class MDMSValidatorTest {{

    @InjectMocks
    private MDMSValidator validator;

    @Test
    void getAttributeValues_whenInvalidMdmsPayload_throwsCustomException() {{
        assertThatThrownBy(() -> validator.getAttributeValues(Map.of()))
                .isInstanceOf(CustomException.class);
    }}

    @Test
    void getAttributeValues_whenValidPayload_returnsMasterEntries() {{
        Map<String, Object> mdms = Map.of(
                "MdmsRes",
                Map.of("{module}", Map.of("{master}", List.of("CODE1"))));

        Map<String, List<String>> attributes = validator.getAttributeValues(mdms);

        assertThat(attributes).isNotEmpty();
    }}
}}
'''

TESTS: dict[str, dict[str, str]] = {
    "advertisement-service": {
        "org/upyog/adv/util/ResponseInfoFactoryTest.java": RESPONSE_INFO_TEMPLATE.format(pkg="org.upyog.adv.util"),
        "org/upyog/adv/util/PaymentTimerKeyBuilderTest.java": '''package org.upyog.adv.util;

import org.junit.jupiter.api.Test;
import org.upyog.adv.web.models.AdvertisementSlotSearchCriteria;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTimerKeyBuilderTest {

    @Test
    void toRedisSlotKey_buildsDeterministicKey() {
        String key = PaymentTimerKeyBuilder.toRedisSlotKey("pb", "HOARDING", "LOC", "10x10", true,
                LocalDate.of(2024, 1, 1));

        assertThat(key).startsWith("adv:payment-timer:slot:");
        assertThat(key).contains("pb:HOARDING:LOC:10x10:true:2024-01-01");
    }

    @Test
    void toRedisTimerRowKey_whenBookingIdMissing_throws() {
        assertThatThrownBy(() -> PaymentTimerKeyBuilder.toRedisTimerRowKey("pb", "A", "L", "F", false,
                LocalDate.of(2024, 1, 1), "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void buildTimerDetailsList_expandsDateRange() {
        var criteria = AdvertisementSlotSearchCriteria.builder()
                .addType("HOARDING").location("LOC").faceArea("10x10").nightLight(false)
                .bookingStartDate("2024-01-01").bookingEndDate("2024-01-02")
                .build();

        var details = PaymentTimerKeyBuilder.buildTimerDetailsList(List.of(criteria), "draft-1", "user-1", "pb", 1L);

        assertThat(details).hasSize(2);
    }
}
''',
        "org/upyog/adv/validator/MDMSValidatorTest.java": '''package org.upyog.adv.validator;

import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class MDMSValidatorTest {

    @InjectMocks
    private MDMSValidator validator;

    @Test
    void getAttributeValues_whenInvalidMdmsPayload_throwsCustomException() {
        assertThatThrownBy(() -> validator.getAttributeValues(Map.of()))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void getAttributeValues_whenValidPayload_returnsMasterEntries() {
        Map<String, Object> mdms = Map.of(
                "MdmsRes",
                Map.of("Advertisement", Map.of("AddType", List.of("HOARDING"))));

        Map<String, List<String>> attributes = validator.getAttributeValues(mdms);

        assertThat(attributes).containsKey("AddType");
    }
}
''',
        "org/upyog/adv/repository/querybuilder/AdvertisementBookingQueryBuilderTest.java": '''package org.upyog.adv.repository.querybuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.adv.config.BookingConfiguration;
import org.upyog.adv.web.models.AdvertisementSearchCriteria;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AdvertisementBookingQueryBuilderTest {

    @Mock
    private BookingConfiguration bookingConfiguration;

    @Test
    void getPaymentTimerInsertQuery_returnsInsertStatement() {
        var builder = new AdvertisementBookingQueryBuilder(bookingConfiguration);

        assertThat(builder.getPaymentTimerInsertQuery()).contains("INSERT");
    }

    @Test
    void getAdvertisementSearchQuery_withTenantId_addsPredicate() {
        var builder = new AdvertisementBookingQueryBuilder(bookingConfiguration);
        AdvertisementSearchCriteria criteria = AdvertisementSearchCriteria.builder().tenantId("pb").build();
        List<Object> params = new ArrayList<>();

        String query = builder.getAdvertisementSearchQuery(criteria, params);

        assertThat(query).contains("tenant_id");
        assertThat(params).isNotEmpty();
    }
}
''',
    },
}


def write_module(module: str, files: dict[str, str]) -> int:
    base = ROOT / module / "src/test/java"
    n = 0
    for rel, content in files.items():
        path = base / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content.strip() + "\n", encoding="utf-8")
        n += 1
    return n


if __name__ == "__main__":
    total = 0
    for mod, files in TESTS.items():
        c = write_module(mod, files)
        print(f"{mod}: wrote {c} tests")
        total += c
    print(f"total files: {total}")
