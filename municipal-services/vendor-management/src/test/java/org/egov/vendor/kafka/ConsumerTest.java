package org.egov.vendor.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.vendor.repository.VendorAdditionalDetailsRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ConsumerTest {
    @Mock
    private VendorAdditionalDetailsRepository contractorRepository;
    @Mock
    private ObjectMapper objectMapper;

    private Consumer subject;

    @BeforeEach
    void setUp() {
        subject = new Consumer(contractorRepository, objectMapper);
    }


    @Test
    void subject_whenConstructed_isNotNull() {
        assertThat(subject).isNotNull();
    }

}
