// generated-by: generate_per_class_unit_tests.py
package org.upyog.adv.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.encryption.EncryptionService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EncryptionDecryptionUtilTest {
    @Mock
    private EncryptionService encryptionService;

    private EncryptionDecryptionUtil subject;

    @BeforeEach
    void setUp() {
        subject = new EncryptionDecryptionUtil(encryptionService);
    }


    @Test
    void encryptObject_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
