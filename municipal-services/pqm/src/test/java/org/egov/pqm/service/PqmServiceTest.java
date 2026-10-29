package org.egov.pqm.service;

import org.egov.pqm.web.model.TestRequest;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PqmServiceTest {

    @InjectMocks
    private PqmService pqmService;

    @Test
    void create_whenTestsMissing_throws() {
        TestRequest request = new TestRequest();

        assertThatThrownBy(() -> pqmService.create(request))
                .isInstanceOf(CustomException.class);
    }
}
