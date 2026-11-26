package org.egov.pt.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.pt.config.PropertyConfiguration;
import org.egov.pt.models.Address;
import org.egov.pt.models.AuditDetails;
import org.egov.pt.models.OwnerInfo;
import org.egov.pt.models.Property;
import org.egov.pt.models.PropertyCriteria;
import org.egov.pt.models.enums.CreationReason;
import org.egov.pt.models.enums.Status;
import org.egov.pt.util.PropertyUtil;
import org.egov.pt.web.contracts.PropertyRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EnrichmentServiceTest {

    @Mock private PropertyUtil propertyutil;
    @Mock private BoundaryService boundaryService;
    @Mock private PropertyConfiguration config;
    @Mock private UserService userService;

    @InjectMocks
    private EnrichmentService enrichmentService;

    @Test
    void getPropertyCriteriaFromPropertyIds_whenPropertiesGiven_setsTenantAndIds() {
        Property p1 = Property.builder().propertyId("pid-1").tenantId("pb.amritsar").build();
        Property p2 = Property.builder().propertyId("pid-2").tenantId("pb.amritsar").build();

        PropertyCriteria criteria = enrichmentService.getPropertyCriteriaFromPropertyIds(List.of(p1, p2));

        assertThat(criteria.getTenantId()).isEqualTo("pb.amritsar");
        assertThat(criteria.getPropertyIds()).containsExactlyInAnyOrder("pid-1", "pid-2");
    }

    @Test
    void enrichBoundary_whenPropertyGiven_delegatesToBoundaryService() {
        Property property = Property.builder().tenantId("pb.amritsar").build();
        RequestInfo requestInfo = RequestInfo.builder().build();

        enrichmentService.enrichBoundary(property, requestInfo);

        verify(boundaryService).getAreaType(property, requestInfo, org.egov.pt.util.PTConstants.BOUNDARY_HEIRARCHY_CODE);
    }

    @Test
    void enrichCreateRequest_whenValidProperty_assignsIdsAndAcknowledgement() {
        RequestInfo requestInfo = RequestInfo.builder().userInfo(User.builder().uuid("user-1").build()).build();
        OwnerInfo owner = OwnerInfo.builder().build();
        Property property = Property.builder()
                .tenantId("pb.amritsar")
                .creationReason(CreationReason.CREATE)
                .address(Address.builder().build())
                .owners(new ArrayList<>(List.of(owner)))
                .build();
        PropertyRequest request = PropertyRequest.builder().requestInfo(requestInfo).property(property).build();

        when(propertyutil.getAuditDetails(anyString(), anyBoolean())).thenReturn(AuditDetails.builder().build());
        when(propertyutil.getIdList(any(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(List.of("PID-001"), List.of("ACK-001"));
        when(config.getIsWorkflowEnabled()).thenReturn(false);
        when(config.getPropertyIdGenName()).thenReturn("property.id");
        when(config.getPropertyIdGenFormat()).thenReturn("[SEQ]");
        when(config.getAckIdGenName()).thenReturn("property.ack");
        when(config.getAckIdGenFormat()).thenReturn("[SEQ]");

        enrichmentService.enrichCreateRequest(request);

        assertThat(property.getPropertyId()).isEqualTo("PID-001");
        assertThat(property.getAcknowldgementNumber()).isEqualTo("ACK-001");
        assertThat(property.getId()).isNotBlank();
        assertThat(property.getAccountId()).isEqualTo("user-1");
        assertThat(owner.getStatus()).isEqualTo(Status.ACTIVE);
        verify(boundaryService).getAreaType(property, requestInfo, org.egov.pt.util.PTConstants.BOUNDARY_HEIRARCHY_CODE);
    }

    @Test
    void enrichUpdateRequest_whenWorkflowDisabled_setsActiveAndMergesAdditionalDetails() {
        RequestInfo requestInfo = RequestInfo.builder().userInfo(User.builder().uuid("user-1").build()).build();
        Property property = Property.builder()
                .tenantId("pb.amritsar")
                .address(Address.builder().build())
                .owners(new ArrayList<>())
                .build();
        Property fromDb = Property.builder()
                .tenantId("pb.amritsar")
                .status(Status.ACTIVE)
                .accountId("acc-1")
                .address(Address.builder().id("addr-db").build())
                .build();
        PropertyRequest request = PropertyRequest.builder().requestInfo(requestInfo).property(property).build();

        when(config.getIsWorkflowEnabled()).thenReturn(false);
        when(propertyutil.getAuditDetails(anyString(), anyBoolean())).thenReturn(AuditDetails.builder().build());
        when(propertyutil.jsonMerge(any(), any())).thenReturn(null);

        enrichmentService.enrichUpdateRequest(request, fromDb);

        assertThat(property.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(property.getAddress().getId()).isEqualTo("addr-db");
        assertThat(property.getAccountId()).isEqualTo("acc-1");
    }
}
