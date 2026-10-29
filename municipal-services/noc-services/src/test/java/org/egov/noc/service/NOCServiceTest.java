package org.egov.noc.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.noc.repository.NOCRepository;
import org.egov.noc.web.model.NocSearchCriteria;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NOCServiceTest {

    @Mock private NOCRepository nocRepository;

    @InjectMocks
    private NOCService nocService;

    @Test
    void search_whenNoMobile_usesRepositoryDirectly() {
        NocSearchCriteria criteria = NocSearchCriteria.builder().tenantId("pb.amritsar").build();
        when(nocRepository.getNocData(criteria)).thenReturn(Collections.emptyList());

        assertThat(nocService.search(criteria, RequestInfo.builder().build())).isEmpty();
    }
}
