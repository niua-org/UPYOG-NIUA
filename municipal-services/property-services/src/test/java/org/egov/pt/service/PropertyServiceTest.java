package org.egov.pt.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.pt.config.PropertyConfiguration;
import org.egov.pt.models.OwnerInfo;
import org.egov.pt.models.Property;
import org.egov.pt.models.PropertyCriteria;
import org.egov.pt.models.collection.Bill;
import org.egov.pt.models.collection.BillResponse;
import org.egov.pt.models.enums.CreationReason;
import org.egov.pt.models.enums.Source;
import org.egov.pt.models.enums.Status;
import org.egov.pt.producer.PropertyProducer;
import org.egov.pt.repository.PropertyRepository;
import org.egov.pt.util.EncryptionDecryptionUtil;
import org.egov.pt.util.PropertyUtil;
import org.egov.pt.util.UnmaskingUtil;
import org.egov.pt.validator.PropertyValidator;
import org.egov.pt.web.contracts.PropertyRequest;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PropertyServiceTest {

    @Mock private BillingService billingService;
    @Mock private UnmaskingUtil unmaskingUtil;
    @Mock private PropertyProducer producer;
    @Mock private NotificationService notifService;
    @Mock private PropertyConfiguration config;
    @Mock private PropertyRepository repository;
    @Mock private EnrichmentService enrichmentService;
    @Mock private PropertyValidator propertyValidator;
    @Mock private UserService userService;
    @Mock private WorkflowService wfService;
    @Mock private PropertyUtil util;
    @Mock private com.fasterxml.jackson.databind.ObjectMapper mapper;
    @Mock private CalculationService calculatorService;
    @Mock private FuzzySearchService fuzzySearchService;
    @Mock private EncryptionDecryptionUtil encryptionDecryptionUtil;

    @InjectMocks
    private PropertyService propertyService;

    @BeforeEach
    void stubEncryptionPassThrough() {
        when(encryptionDecryptionUtil.encryptObject(any(), anyString(), eq(PropertyCriteria.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(encryptionDecryptionUtil.decryptObject(any(), anyString(), eq(Property.class), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(encryptionDecryptionUtil.decryptObject(any(List.class), anyString(), eq(Property.class), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(config.getSavePropertyTopic()).thenReturn("save-topic");
        when(config.getUpdatePropertyTopic()).thenReturn("update-topic");
        when(config.getMaxSearchLimit()).thenReturn(100L);
        when(config.getDefaultLimit()).thenReturn(10L);
        when(config.getDefaultOffset()).thenReturn(0L);
    }

    private PropertyRequest propertyRequest(CreationReason reason, String ownerUuid, String mobile) {
        OwnerInfo owner = OwnerInfo.builder().uuid(ownerUuid).mobileNumber(mobile).build();
        Property property = Property.builder()
                .tenantId("pb.amritsar")
                .creationReason(reason)
                .source(Source.MUNICIPAL_RECORDS)
                .status(Status.ACTIVE)
                .owners(new ArrayList<>(List.of(owner)))
                .build();
        return PropertyRequest.builder()
                .requestInfo(RequestInfo.builder().userInfo(User.builder().uuid("user-1").build()).build())
                .property(property)
                .build();
    }

    @Test
    void createProperty_whenWorkflowDisabled_setsActiveAndPushesToKafka() {
        PropertyRequest request = propertyRequest(CreationReason.CREATE, "o1", "9999999999");
        when(config.getIsWorkflowEnabled()).thenReturn(false);

        Property result = propertyService.createProperty(request);

        assertThat(result.getStatus()).isEqualTo(Status.ACTIVE);
        verify(propertyValidator).validateCreateRequest(request);
        verify(enrichmentService).enrichCreateRequest(request);
        verify(userService).createUser(request);
        verify(wfService, never()).updateWorkflow(any(), any());
        verify(producer).pushAfterEncrytpion(eq("save-topic"), eq(request));
        verify(encryptionDecryptionUtil).decryptObject(eq(request.getProperty()), anyString(), eq(Property.class), any());
    }

    @Test
    void createProperty_whenDataUpload_skipsWorkflow() {
        PropertyRequest request = propertyRequest(CreationReason.DATA_UPLOAD, "o1", "9999999999");
        when(config.getIsWorkflowEnabled()).thenReturn(true);

        propertyService.createProperty(request);

        assertThat(request.getProperty().getStatus()).isEqualTo(Status.ACTIVE);
        verify(wfService, never()).updateWorkflow(any(), any());
    }

    @Test
    void createProperty_whenWorkflowEnabled_invokesWorkflow() {
        PropertyRequest request = propertyRequest(CreationReason.CREATE, "o1", "9999999999");
        when(config.getIsWorkflowEnabled()).thenReturn(true);

        propertyService.createProperty(request);

        verify(wfService).updateWorkflow(request, CreationReason.CREATE);
    }

    @Test
    void updateProperty_whenStatusChangeAndOutstandingBill_throwsActiveBillPresent() {
        PropertyRequest request = propertyRequest(CreationReason.STATUS, "o1", "9999999999");
        Property existing = propertyRequest(CreationReason.CREATE, "o1", "9999999999").getProperty();
        when(unmaskingUtil.getPropertyUnmasked(request)).thenReturn(existing);
        BillResponse billResponse = new BillResponse();
        Bill bill = new Bill();
        bill.setTotalAmount(BigDecimal.TEN);
        billResponse.setBill(List.of(bill));
        when(billingService.fetchBill(any(), any())).thenReturn(billResponse);

        assertThatThrownBy(() -> propertyService.updateProperty(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Clear Pending dues");
    }

    @Test
    void updateProperty_whenMobileNumberChanged_updatesUserAndNotifies() {
        PropertyRequest request = propertyRequest(CreationReason.UPDATE, "o1", "8888888888");
        Property existing = propertyRequest(CreationReason.CREATE, "o1", "9999999999").getProperty();
        when(unmaskingUtil.getPropertyUnmasked(request)).thenReturn(existing);
        when(config.getIsWorkflowEnabled()).thenReturn(false);

        propertyService.updateProperty(request);

        verify(userService).updateUserMobileNumber(eq(request), any());
        verify(notifService).sendNotificationForMobileNumberUpdate(eq(request), eq(existing), any());
        verify(producer).pushAfterEncrytpion(eq("update-topic"), eq(request));
    }

    @Test
    void updateProperty_whenStandardUpdateWithoutWorkflow_pushesUpdateTopic() {
        PropertyRequest request = propertyRequest(CreationReason.UPDATE, "o1", "9999999999");
        Property existing = propertyRequest(CreationReason.CREATE, "o1", "9999999999").getProperty();
        when(unmaskingUtil.getPropertyUnmasked(request)).thenReturn(existing);
        when(config.getIsWorkflowEnabled()).thenReturn(false);

        propertyService.updateProperty(request);

        verify(propertyValidator).validateRequestForUpdate(request, existing);
        verify(enrichmentService).enrichUpdateRequest(request, existing);
        verify(producer).pushAfterEncrytpion(eq("update-topic"), eq(request));
    }

    @Test
    void searchProperty_whenAuditWithMultipleIds_throwsAuditError() {
        PropertyCriteria criteria = PropertyCriteria.builder()
                .audit(true)
                .propertyIds(Set.of("p1", "p2"))
                .tenantId("pb.amritsar")
                .build();

        assertThatThrownBy(() -> propertyService.searchProperty(criteria, RequestInfo.builder().build()))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("single propertyId");
    }

    @Test
    void searchProperty_whenUserEnrichmentEmpty_returnsEmptyList() {
        PropertyCriteria criteria = PropertyCriteria.builder()
                .tenantId("pb.amritsar")
                .mobileNumber("9999999999")
                .build();
        when(repository.enrichCriteriaFromUser(criteria, RequestInfo.builder().build())).thenReturn(true);

        List<Property> result = propertyService.searchProperty(criteria, RequestInfo.builder().build());

        assertThat(result).isEmpty();
        verify(repository, never()).getPropertiesWithOwnerInfo(any(), any(), anyBoolean());
    }

    @Test
    void searchProperty_whenTenantOnlySearch_enrichesBoundaryAndDecrypts() {
        PropertyCriteria criteria = PropertyCriteria.builder().tenantId("pb.amritsar").build();
        Property property = Property.builder().tenantId("pb.amritsar").propertyId("pid-1").build();
        when(repository.getPropertiesWithOwnerInfo(criteria, RequestInfo.builder().build(), false))
                .thenReturn(new ArrayList<>(List.of(property)));

        List<Property> result = propertyService.searchProperty(criteria, RequestInfo.builder().build());

        assertThat(result).hasSize(1);
        verify(enrichmentService).enrichBoundary(property, RequestInfo.builder().build());
    }

    @Test
    void searchProperty_whenOldPropertyId_usesFuzzySearch() {
        PropertyCriteria criteria = PropertyCriteria.builder()
                .tenantId("pb.amritsar")
                .oldPropertyId("old-1")
                .isRequestForDuplicatePropertyValidation(false)
                .build();
        when(fuzzySearchService.getProperties(any(), eq(criteria))).thenReturn(List.of());

        List<Property> result = propertyService.searchProperty(criteria, RequestInfo.builder().build());

        assertThat(result).isEmpty();
        verify(fuzzySearchService).getProperties(any(), eq(criteria));
    }

    @Test
    void count_whenCriteriaGiven_delegatesToRepository() {
        PropertyCriteria criteria = PropertyCriteria.builder().tenantId("pb.amritsar").build();
        when(repository.getCount(criteria, RequestInfo.builder().build())).thenReturn(42);

        Integer count = propertyService.count(RequestInfo.builder().build(), criteria);

        assertThat(count).isEqualTo(42);
        assertThat(criteria.getIsInboxSearch()).isFalse();
    }

    @Test
    void addAlternateNumber_whenValidRequest_pushesUpdateAndSetsFlag() {
        PropertyRequest request = propertyRequest(CreationReason.UPDATE, "o1", "9999999999");
        request.getProperty().getOwners().get(0).setAlternatemobilenumber("7777777777");
        Property existing = propertyRequest(CreationReason.CREATE, "o1", "9999999999").getProperty();
        existing.getOwners().get(0).setAlternatemobilenumber("6666666666");
        when(unmaskingUtil.getPropertyUnmasked(request)).thenReturn(existing);

        Property result = propertyService.addAlternateNumber(request);

        assertThat(result.isAlternateUpdated()).isTrue();
        verify(userService).createUserForAlternateNumber(request);
        verify(notifService).sendNotificationForAlternateNumberUpdate(eq(request), eq(existing), any());
        verify(producer).pushAfterEncrytpion(eq("update-topic"), eq(request));
    }

    @Test
    void searchPropertyPlainSearch_whenPropertiesFound_enrichesBoundary() {
        PropertyCriteria criteria = PropertyCriteria.builder()
                .tenantId("pb.amritsar")
                .uuids(Set.of("uuid-1"))
                .limit(5L)
                .build();
        Property property = Property.builder().tenantId("pb.amritsar").owners(new ArrayList<>()).build();
        when(repository.getPropertiesForBulkSearch(any(), eq("pb.amritsar"), eq(true)))
                .thenReturn(List.of(property));
        when(userService.getBaseUserSearchRequest(any(), any())).thenReturn(new org.egov.pt.models.user.UserSearchRequest());
        when(userService.getUser(any())).thenReturn(new org.egov.pt.models.user.UserDetailResponse());

        List<Property> result = propertyService.searchPropertyPlainSearch(criteria, RequestInfo.builder().build());

        assertThat(result).hasSize(1);
        verify(enrichmentService).enrichBoundary(property, RequestInfo.builder().build());
    }
}
