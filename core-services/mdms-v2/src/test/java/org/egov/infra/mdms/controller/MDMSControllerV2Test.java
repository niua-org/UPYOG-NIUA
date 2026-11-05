package org.egov.infra.mdms.controller;

import org.egov.common.contract.request.RequestInfo;
import org.egov.infra.mdms.model.Mdms;
import org.egov.infra.mdms.model.MdmsCriteriaReqV2;
import org.egov.infra.mdms.model.MdmsRequest;
import org.egov.infra.mdms.model.MdmsResponseV2;
import org.egov.infra.mdms.service.MDMSServiceV2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MDMSControllerV2Test {

    @Mock
    private MDMSServiceV2 mdmsServiceV2;

    @InjectMocks
    private MDMSControllerV2 controller;

    @Test
    void create_returnsAccepted() {
        MdmsRequest request = MdmsRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .build();
        when(mdmsServiceV2.create(request)).thenReturn(List.of(Mdms.builder().id("m1").build()));

        ResponseEntity<MdmsResponseV2> response = controller.create(request, "common.StateInfo");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody().getMdms()).hasSize(1);
    }

    @Test
    void search_returnsOk() {
        MdmsCriteriaReqV2 criteria = MdmsCriteriaReqV2.builder().build();
        when(mdmsServiceV2.search(criteria)).thenReturn(List.of(Mdms.builder().id("m1").build()));

        ResponseEntity<MdmsResponseV2> response = controller.search(criteria);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void delete_returnsOk() {
        MdmsRequest request = MdmsRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .build();
        when(mdmsServiceV2.delete(request, "common.StateInfo"))
                .thenReturn(List.of(Mdms.builder().id("m1").build()));

        ResponseEntity<MdmsResponseV2> response = controller.delete(request, "common.StateInfo");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void update_returnsAccepted() {
        MdmsRequest request = MdmsRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .build();
        when(mdmsServiceV2.update(request)).thenReturn(List.of(Mdms.builder().id("m1").build()));

        ResponseEntity<MdmsResponseV2> response = controller.update(request, "common.StateInfo");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }
}
