package org.upyog.reconciliation.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationResultTest {

    @Test
    void isValid_initialState_returnsTrue() {
        ValidationResult result = new ValidationResult();
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void addError_marksInvalidAndAggregatesMessages() {
        ValidationResult result = new ValidationResult();
        result.addError("tenantId", "must not be blank");
        result.addError("module", "unknown module");

        assertThat(result.isValid()).isFalse();
        assertThat(result.getInvalidFieldsConcatenated()).contains("tenantId", "module");
        assertThat(result.getErrorSummary()).contains("must not be blank");
    }
}
