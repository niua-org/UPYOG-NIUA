package org.egov.fsm.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.fsm.util.FSMUtil;
import org.egov.fsm.web.model.location.Address;
import org.egov.fsm.web.model.AuditDetails;
import org.egov.fsm.web.model.FSM;
import org.egov.fsm.web.model.FSMRequest;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrichmentServiceTest {

    @Mock private FSMUtil fsmUtil;

    @InjectMocks
    private EnrichmentService enrichmentService;

    @Test
    void enrichFSMUpdateRequest_whenAddressMissing_throws() {
        FSM oldFsm = FSM.builder()
                .auditDetails(AuditDetails.builder().createdBy("c").createdTime(1L).build())
                .build();
        FSMRequest request = FSMRequest.builder()
                .RequestInfo(RequestInfo.builder().userInfo(User.builder().uuid("u1").build()).build())
                .fsm(FSM.builder().build())
                .build();
        when(fsmUtil.getAuditDetails(eq("u1"), eq(false)))
                .thenReturn(AuditDetails.builder().lastModifiedBy("u1").lastModifiedTime(2L).build());

        assertThatThrownBy(() -> enrichmentService.enrichFSMUpdateRequest(request, oldFsm))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Address is mandatory");
    }

    @Test
    void enrichFSMUpdateRequest_whenAddressPresent_setsAuditOnAddress() {
        FSM oldFsm = FSM.builder()
                .auditDetails(AuditDetails.builder().createdBy("creator").createdTime(10L).build())
                .build();
        Address address = Address.builder().build();
        FSM fsm = FSM.builder().address(address).tenantId("pb.amritsar").build();
        FSMRequest request = FSMRequest.builder()
                .RequestInfo(RequestInfo.builder().userInfo(User.builder().uuid("u1").build()).build())
                .fsm(fsm)
                .build();
        AuditDetails updateAudit = AuditDetails.builder().lastModifiedBy("u1").lastModifiedTime(20L).build();
        when(fsmUtil.getAuditDetails(any(), eq(false))).thenReturn(updateAudit);

        enrichmentService.enrichFSMUpdateRequest(request, oldFsm);

        assertThat(request.getFsm().getAuditDetails().getCreatedBy()).isEqualTo("creator");
        assertThat(request.getFsm().getAddress().getAuditDetails()).isSameAs(updateAudit);
        assertThat(request.getFsm().getAddress().getId()).isNotBlank();
    }
}
