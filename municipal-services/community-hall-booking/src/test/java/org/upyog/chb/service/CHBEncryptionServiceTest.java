// generated-by: generate_per_class_unit_tests.py
package org.upyog.chb.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.upyog.chb.util.EncryptionDecryptionUtil;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CHBEncryptionServiceTest {
    @Mock
    private EncryptionDecryptionUtil encryptionDecryptionUtil;

    private CHBEncryptionService subject;

    @BeforeEach
    void setUp() {
        subject = new CHBEncryptionService(encryptionDecryptionUtil);
    }


    @Test
    void encryptObject_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
