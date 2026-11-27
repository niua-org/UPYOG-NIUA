package org.egov.user.domain.model;

import org.egov.user.domain.exception.InvalidUserSearchCriteriaException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UserSearchCriteriaTest {

    @Test
    public void validate_validCriteriaWithTenantAndUserName_doesNotThrow() {
        final UserSearchCriteria searchCriteria = UserSearchCriteria.builder()
                .tenantId("tenantId")
                .userName("greenfish424")
                .build();

        assertThatCode(() -> searchCriteria.validate(true)).doesNotThrowAnyException();
    }

    @Test
    public void validate_missingTenantId_throwsInvalidUserSearchCriteriaException() {
        final UserSearchCriteria searchCriteria = UserSearchCriteria.builder()
                .tenantId(null)
                .build();

        assertThatThrownBy(() -> searchCriteria.validate(true))
                .isInstanceOf(InvalidUserSearchCriteriaException.class);
    }
}