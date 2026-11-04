package org.egov.ptr.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.ptr.models.PetApplicationSearchCriteria;
import org.egov.ptr.repository.PetRegistrationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetRegistrationServiceTest {

    @Mock private PetRegistrationRepository petRegistrationRepository;

    @InjectMocks
    private PetRegistrationService petRegistrationService;

    @Test
    void searchPtrApplications_whenRepositoryEmpty_returnsEmptyList() {
        PetApplicationSearchCriteria criteria = PetApplicationSearchCriteria.builder().tenantId("pb.amritsar").build();
        when(petRegistrationRepository.getApplications(criteria)).thenReturn(Collections.emptyList());

        assertThat(petRegistrationService.searchPtrApplications(RequestInfo.builder().build(), criteria)).isEmpty();
    }
}
