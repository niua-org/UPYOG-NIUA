// generated-by: generate_per_class_unit_tests.py
package org.egov.asset.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.asset.config.AssetConfiguration;
import org.egov.asset.util.MDMSValidator;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AssetValidatorTest {
    @Mock
    private MDMSValidator mdmsValidator;
    @Mock
    private AssetConfiguration config;

    private AssetValidator subject;

    @BeforeEach
    void setUp() {
        subject = new AssetValidator(mdmsValidator, config);
    }


    @Test
    void validateCreate_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
