package org.egov.hrms.utils;

import org.egov.hrms.web.contract.EmployeeSearchCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HRMSUtilsTest {

    private HRMSUtils hrmsUtils;

    @BeforeEach
    void setUp() {
        hrmsUtils = new HRMSUtils();
        ReflectionTestUtils.setField(hrmsUtils, "pwdLength", 8);
        ReflectionTestUtils.setField(hrmsUtils, "allowedPasswordSpecialCharacters", "@#$%");
    }

    @Test
    void generatePassword_returnsConfiguredLength() {
        List<String> params = new ArrayList<>(Arrays.asList("ABC", "123"));

        String password = hrmsUtils.generatePassword(params);

        assertThat(password).isNotBlank();
        assertThat(password.length()).isLessThanOrEqualTo(8);
        assertThat(password).doesNotContain(" ");
    }

    @Test
    void isAssignmentSearchReqd_whenPositionsPresent_returnsTrue() {
        EmployeeSearchCriteria criteria = EmployeeSearchCriteria.builder()
                .positions(Collections.singletonList(1L))
                .build();

        assertThat(hrmsUtils.isAssignmentSearchReqd(criteria)).isTrue();
    }

    @Test
    void isAssignmentSearchReqd_whenNoAssignmentFields_returnsFalse() {
        EmployeeSearchCriteria criteria = EmployeeSearchCriteria.builder()
                .tenantId("pb.amritsar")
                .build();

        assertThat(hrmsUtils.isAssignmentSearchReqd(criteria)).isFalse();
    }

    @Test
    void isAssignmentSearchReqd_whenDepartmentsPresent_returnsTrue() {
        EmployeeSearchCriteria criteria = EmployeeSearchCriteria.builder()
                .departments(Collections.singletonList("DEPT"))
                .build();

        assertThat(hrmsUtils.isAssignmentSearchReqd(criteria)).isTrue();
    }

    @Test
    void generatePassword_onEmptyParams_usesFallback() {
        String password = hrmsUtils.generatePassword(new ArrayList<>());

        assertThat(password).isNotBlank();
    }
}
