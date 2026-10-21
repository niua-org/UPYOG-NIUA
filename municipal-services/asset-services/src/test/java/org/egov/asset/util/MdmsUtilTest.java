// generated-by: generate_per_class_unit_tests.py
package org.egov.asset.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.asset.config.AssetConfiguration;
import org.egov.asset.repository.ServiceRequestRepository;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MdmsUtilTest {
    @Mock
    private AssetConfiguration config;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    private MdmsUtil subject;

    @BeforeEach
    void setUp() {
        subject = new MdmsUtil(config, serviceRequestRepository);
    }


    @Test
    void mDMSCall_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
