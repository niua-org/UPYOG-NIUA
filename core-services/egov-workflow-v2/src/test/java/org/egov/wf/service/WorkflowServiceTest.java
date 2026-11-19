package org.egov.wf.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.tracer.model.CustomException;
import org.egov.wf.config.WorkflowConfig;
import org.egov.wf.producer.Producer;
import org.egov.wf.repository.BusinessServiceRepository;
import org.egov.wf.repository.WorKflowRepository;
import org.egov.wf.util.WorkflowUtil;
import org.egov.wf.validator.WorkflowValidator;
import org.egov.wf.web.models.DashboardProcessInstance;
import org.egov.wf.web.models.DashboardProcessInstanceResponse;
import org.egov.wf.web.models.ProcessInstance;
import org.egov.wf.web.models.ProcessInstanceRequest;
import org.egov.wf.web.models.ProcessInstanceSearchCriteria;
import org.egov.wf.web.models.ProcessStateAndAction;
import org.egov.wf.web.models.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    private static final String TRANSITION_TOPIC = "save-wf-transition";

    @Mock
    private TransitionService transitionService;
    @Mock
    private EnrichmentService enrichmentService;
    @Mock
    private WorkflowValidator workflowValidator;
    @Mock
    private WorKflowRepository workflowRepository;
    @Mock
    private WorkflowUtil util;
    @Mock
    private BusinessServiceRepository businessServiceRepository;
    @Mock
    private MDMSService mdmsService;
    @Mock
    private BusinessMasterService businessMasterService;
    @Mock
    private Producer producer;
    @Mock
    private WorkflowConfig config;

    @Captor
    private ArgumentCaptor<Object> eventPayloadCaptor;
    @Captor
    private ArgumentCaptor<ProcessInstanceSearchCriteria> criteriaCaptor;

    private WorkflowService workflowService;

    @BeforeEach
    void setUp() {
        lenient().when(config.getSaveTransitionTopic()).thenReturn(TRANSITION_TOPIC);
        StatusUpdateService statusUpdateService = new StatusUpdateService(producer, config);
        workflowService = new WorkflowService(config, transitionService, enrichmentService, workflowValidator,
                statusUpdateService, workflowRepository, util, businessServiceRepository);
        ReflectionTestUtils.setField(workflowService, "mdmsService", mdmsService);
        ReflectionTestUtils.setField(workflowService, "businessMasterService", businessMasterService);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("stateTransitions")
    void transitionPublishesResultantState(String name, String currentStateUuid, String nextStateName,
            String expectedPreviousStatus, String expectedStateName) {
        RequestInfo requestInfo = requestInfo("user-1");
        ProcessInstance instance = processInstance("BIZ-1");
        if (currentStateUuid != null) {
            instance.setState(State.builder().uuid(currentStateUuid).state("CURRENT").build());
        }
        State resultant = nextStateName == null ? null : State.builder().uuid("next-id").state(nextStateName).build();
        ProcessStateAndAction action = actionFor(instance, resultant);
        ProcessInstanceRequest request = request(requestInfo, List.of(instance));
        when(transitionService.getProcessStateAndActions(request.getProcessInstances(), true))
                .thenReturn(List.of(action));

        List<ProcessInstance> result = workflowService.transition(request);

        assertThat(result).containsExactly(instance);
        assertThat(instance.getPreviousStatus()).isEqualTo(expectedPreviousStatus);
        if (expectedStateName == null) {
            assertThat(instance.getState()).isNull();
        } else {
            assertThat(instance.getState().getState()).isEqualTo(expectedStateName);
        }

        verify(producer).push(eq(TRANSITION_TOPIC), eventPayloadCaptor.capture());
        ProcessInstanceRequest published = (ProcessInstanceRequest) eventPayloadCaptor.getValue();
        assertThat(published.getRequestInfo()).isSameAs(requestInfo);
        assertThat(published.getProcessInstances()).containsExactly(instance);

        InOrder order = inOrder(transitionService, enrichmentService, workflowValidator, producer);
        order.verify(transitionService).getProcessStateAndActions(request.getProcessInstances(), true);
        order.verify(enrichmentService).enrichProcessRequest(requestInfo, List.of(action));
        order.verify(workflowValidator).validateRequest(requestInfo, List.of(action));
        order.verify(producer).push(eq(TRANSITION_TOPIC), any());
    }

    @Test
    void transitionPublishesInstancesInRequestOrder() {
        RequestInfo requestInfo = requestInfo("user-1");
        ProcessInstance first = processInstance("BIZ-1");
        ProcessInstance second = processInstance("BIZ-2");
        State approved = State.builder().uuid("approved").state("APPROVED").build();
        State rejected = State.builder().uuid("rejected").state("REJECTED").build();
        ProcessInstanceRequest request = request(requestInfo, List.of(first, second));
        when(transitionService.getProcessStateAndActions(request.getProcessInstances(), true))
                .thenReturn(List.of(actionFor(first, approved), actionFor(second, rejected)));

        List<ProcessInstance> result = workflowService.transition(request);

        assertThat(result).containsExactly(first, second);
        verify(producer).push(eq(TRANSITION_TOPIC), eventPayloadCaptor.capture());
        ProcessInstanceRequest published = (ProcessInstanceRequest) eventPayloadCaptor.getValue();
        assertThat(published.getProcessInstances()).extracting(ProcessInstance::getBusinessId)
                .containsExactly("BIZ-1", "BIZ-2");
        assertThat(published.getProcessInstances()).extracting(instance -> instance.getState().getState())
                .containsExactly("APPROVED", "REJECTED");
    }

    @Test
    void transitionWithEmptyProcessInstancesStillPublishesEmptyPayload() {
        RequestInfo requestInfo = requestInfo("user-1");
        ProcessInstanceRequest request = request(requestInfo, Collections.emptyList());
        when(transitionService.getProcessStateAndActions(request.getProcessInstances(), true))
                .thenReturn(Collections.emptyList());

        assertThat(workflowService.transition(request)).isEmpty();

        verify(producer).push(eq(TRANSITION_TOPIC), eventPayloadCaptor.capture());
        ProcessInstanceRequest published = (ProcessInstanceRequest) eventPayloadCaptor.getValue();
        assertThat(published.getProcessInstances()).isEmpty();
        assertThat(published.getRequestInfo()).isSameAs(requestInfo);
    }

    @Test
    void transitionWithNullProcessInstanceListPublishesEmptyPayloadAndReturnsNull() {
        RequestInfo requestInfo = requestInfo("user-1");
        ProcessInstanceRequest request = request(requestInfo, null);
        when(transitionService.getProcessStateAndActions(null, true)).thenReturn(Collections.emptyList());

        assertThat(workflowService.transition(request)).isNull();

        verify(producer).push(eq(TRANSITION_TOPIC), eventPayloadCaptor.capture());
        ProcessInstanceRequest published = (ProcessInstanceRequest) eventPayloadCaptor.getValue();
        assertThat(published.getProcessInstances()).isEmpty();
    }

    @Test
    void transitionDoesNotPublishWhenValidatorRejectsTheAction() {
        ProcessInstanceRequest request = request(requestInfo("user-1"), List.of(processInstance("BIZ-1")));
        when(transitionService.getProcessStateAndActions(any(), eq(true)))
                .thenReturn(List.of(actionFor(request.getProcessInstances().get(0), State.builder().state("APPROVED").build())));
        org.mockito.Mockito.doThrow(new CustomException("INVALID ACTION", "Action not found for businessIds: PT"))
                .when(workflowValidator).validateRequest(any(), any());

        assertThatThrownBy(() -> workflowService.transition(request))
                .isInstanceOf(CustomException.class)
                .satisfies(thrown -> {
                    CustomException exception = (CustomException) thrown;
                    assertThat(exception.getCode()).isEqualTo("INVALID ACTION");
                    assertThat(exception.getMessage()).contains("Action not found");
                });

        verify(producer, never()).push(any(), any());
    }

    @Test
    void transitionDoesNotPublishWhenResolvingTheNextStateFails() {
        ProcessInstanceRequest request = request(requestInfo("user-1"), List.of(processInstance("BIZ-1")));
        when(transitionService.getProcessStateAndActions(any(), eq(true)))
                .thenThrow(new CustomException("INVALID ACTION", "No transition from PENDING on REJECT"));

        assertThatThrownBy(() -> workflowService.transition(request))
                .isInstanceOf(CustomException.class)
                .satisfies(thrown -> assertThat(((CustomException) thrown).getCode()).isEqualTo("INVALID ACTION"));

        verifyNoInteractions(enrichmentService, workflowValidator, producer);
    }

    @Test
    void transitionDoesNotPublishWhenTheActionPayloadHasNoProcessInstance() {
        ProcessInstanceRequest request = request(requestInfo("user-1"), List.of(processInstance("BIZ-1")));
        when(transitionService.getProcessStateAndActions(any(), eq(true)))
                .thenReturn(List.of(new ProcessStateAndAction()));

        assertThatThrownBy(() -> workflowService.transition(request)).isInstanceOf(NullPointerException.class);

        verify(producer, never()).push(any(), any());
    }

    @Test
    void transitionRejectsANullRequest() {
        assertThatThrownBy(() -> workflowService.transition(null)).isInstanceOf(NullPointerException.class);
        verifyNoInteractions(producer, transitionService);
    }

    @ParameterizedTest(name = "businessIds={0}, ids={1}, assignee={2}, status={3}, inbox={4}")
    @MethodSource("directVersusInboxSearch")
    void searchUsesInboxOnlyWhenCriteriaHasNoIdsStatusOrAssignee(List<String> businessIds, List<String> ids,
            String assignee, List<String> status, boolean inbox) {
        ProcessInstanceSearchCriteria criteria = criteria(businessIds, ids, assignee, status);
        RequestInfo requestInfo = requestInfo("user-1");
        if (inbox) {
            when(workflowRepository.getProcessInstancesForUserInbox(criteria)).thenReturn(Collections.emptyList());
        } else {
            when(workflowRepository.getProcessInstances(criteria)).thenReturn(Collections.emptyList());
        }

        assertThat(workflowService.search(requestInfo, criteria)).isEmpty();

        if (inbox) {
            assertThat(criteria.getAssignee()).isEqualTo("user-1");
            verify(util).enrichStatusesInSearchCriteria(requestInfo, criteria);
            verify(workflowRepository).getProcessInstancesForUserInbox(criteria);
            verify(workflowRepository, never()).getProcessInstances(any());
        } else {
            verify(workflowRepository).getProcessInstances(criteria);
            verify(workflowRepository, never()).getProcessInstancesForUserInbox(any());
            verify(util, never()).enrichStatusesInSearchCriteria(any(), any());
        }
        verifyNoInteractions(enrichmentService);
    }

    @Test
    void searchReturnsRepositoryNullWithoutEnrichment() {
        ProcessInstanceSearchCriteria criteria = criteria(List.of("BIZ-1"), null, null, null);
        when(workflowRepository.getProcessInstances(criteria)).thenReturn(null);

        assertThat(workflowService.search(requestInfo("user-1"), criteria)).isNull();

        verifyNoInteractions(enrichmentService, workflowValidator);
    }

    @Test
    void searchEnrichesANonEmptyDirectResultAndSkipsRoleValidation() {
        ProcessInstanceSearchCriteria criteria = criteria(List.of("BIZ-1"), null, null, null);
        ProcessInstance instance = processInstance("BIZ-1");
        RequestInfo requestInfo = requestInfo("user-1");
        when(workflowRepository.getProcessInstances(criteria)).thenReturn(List.of(instance));
        when(enrichmentService.enrichNextActionForSearch(requestInfo, List.of(instance)))
                .thenReturn(Collections.emptyList());

        assertThat(workflowService.search(requestInfo, criteria)).containsExactly(instance);

        verify(enrichmentService).enrichUsersFromSearch(requestInfo, List.of(instance));
        verify(enrichmentService).enrichNextActionForSearch(requestInfo, List.of(instance));
        verify(enrichmentService).enrichAndUpdateSlaForSearch(List.of(instance));
        verifyNoInteractions(workflowValidator);
    }

    @Test
    void searchInboxKeepsTheLatestRowForADuplicateBusinessId() {
        ProcessInstanceSearchCriteria criteria = new ProcessInstanceSearchCriteria();
        ProcessInstance older = processInstance("BIZ-1");
        older.setId("old");
        ProcessInstance latest = processInstance("BIZ-1");
        latest.setId("latest");
        ProcessInstance other = processInstance("BIZ-2");
        other.setId("other");
        RequestInfo requestInfo = requestInfo("user-1");
        when(workflowRepository.getProcessInstancesForUserInbox(criteria))
                .thenReturn(List.of(older, other, latest));
        when(enrichmentService.enrichNextActionForSearch(eq(requestInfo), any())).thenReturn(Collections.emptyList());

        List<ProcessInstance> result = workflowService.search(requestInfo, criteria);

        assertThat(result).extracting(ProcessInstance::getId).containsExactly("latest", "other");
        assertThat(criteria.getAssignee()).isEqualTo("user-1");
        verify(enrichmentService).enrichUsersFromSearch(eq(requestInfo), eq(result));
    }

    @Test
    void searchInboxRejectsANullRow() {
        ProcessInstanceSearchCriteria criteria = new ProcessInstanceSearchCriteria();
        when(workflowRepository.getProcessInstancesForUserInbox(criteria))
                .thenReturn(Arrays.asList(processInstance("BIZ-1"), null));

        assertThatThrownBy(() -> workflowService.search(requestInfo("user-1"), criteria))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void searchRejectsANullCriteria() {
        assertThatThrownBy(() -> workflowService.search(requestInfo("user-1"), null))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(workflowRepository);
    }

    @Test
    void searchInboxRequiresUserInfo() {
        ProcessInstanceSearchCriteria criteria = new ProcessInstanceSearchCriteria();

        assertThatThrownBy(() -> workflowService.search(new RequestInfo(), criteria))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(workflowRepository);
    }

    @ParameterizedTest(name = "nearingSla={0}, inboxCriteria={1}")
    @CsvSource({
            "false, true",
            "false, false"
    })
    void countUsesInboxOnlyWhenCriteriaIsNull(boolean nearingSla, boolean inboxCriteria) {
        ProcessInstanceSearchCriteria criteria = new ProcessInstanceSearchCriteria();
        criteria.setIsNearingSlaCount(nearingSla);
        if (!inboxCriteria) {
            criteria.setStatus(List.of("PENDING"));
        }
        RequestInfo requestInfo = requestInfo("user-1");
        if (inboxCriteria) {
            when(workflowRepository.getInboxCount(criteria)).thenReturn(4);
        } else {
            when(workflowRepository.getProcessInstancesCount(criteria)).thenReturn(4);
        }

        assertThat(workflowService.count(requestInfo, criteria)).isEqualTo(4);

        if (inboxCriteria) {
            assertThat(criteria.getAssignee()).isEqualTo("user-1");
            verify(util).enrichStatusesInSearchCriteria(requestInfo, criteria);
            verify(workflowRepository).getInboxCount(criteria);
            verify(workflowRepository, never()).getProcessInstancesCount(any());
        } else {
            verify(workflowRepository).getProcessInstancesCount(criteria);
            verify(util, never()).enrichStatusesInSearchCriteria(any(), any());
        }
        verifyNoInteractions(mdmsService, businessMasterService);
    }

    @ParameterizedTest(name = "maxSla={0}, slotPercent={1}, limit={2}")
    @CsvSource({
            "100, 20, 80",
            "200, 10, 180",
            "50, 25, 50",
            "99, 50, 99",
            "1, 100, 1"
    })
    void nearingSlaLimitUsesIntegerDivision(long maxSla, int slotPercent, long expectedLimit) {
        ProcessInstanceSearchCriteria criteria = new ProcessInstanceSearchCriteria();
        criteria.setIsNearingSlaCount(true);
        criteria.setBusinessService("PT");
        criteria.setStatus(List.of("PENDING"));
        RequestInfo requestInfo = requestInfo("user-1");
        when(mdmsService.fetchSlotPercentageForNearingSla(requestInfo)).thenReturn(slotPercent);
        when(businessMasterService.getMaxBusinessServiceSla(criteria)).thenReturn(maxSla);
        when(workflowRepository.getProcessInstancesCount(criteria)).thenReturn(3);

        assertThat(workflowService.count(requestInfo, criteria)).isEqualTo(3);
        assertThat(criteria.getSlotPercentageSlaLimit()).isEqualTo(expectedLimit);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void nearingSlaRequiresABusinessService(String businessService) {
        ProcessInstanceSearchCriteria criteria = new ProcessInstanceSearchCriteria();
        criteria.setIsNearingSlaCount(true);
        criteria.setBusinessService(businessService);

        assertThatThrownBy(() -> workflowService.count(requestInfo("user-1"), criteria))
                .isInstanceOf(CustomException.class)
                .satisfies(thrown -> {
                    CustomException exception = (CustomException) thrown;
                    assertThat(exception.getCode()).isEqualTo("EG_WF_BUSINESSSRV_ERR");
                    assertThat(exception.getMessage())
                            .isEqualTo("Providing business service is mandatory for nearing escalation count");
                });

        verifyNoInteractions(mdmsService, businessMasterService, workflowRepository);
    }

    @Test
    void countRejectsANullNearingSlaFlag() {
        ProcessInstanceSearchCriteria criteria = new ProcessInstanceSearchCriteria();

        assertThatThrownBy(() -> workflowService.count(requestInfo("user-1"), criteria))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(workflowRepository, mdmsService);
    }

    @Test
    void nearingSlaRejectsANullSlotPercentage() {
        ProcessInstanceSearchCriteria criteria = nearingSlaCriteria();
        when(mdmsService.fetchSlotPercentageForNearingSla(any())).thenReturn(null);
        when(businessMasterService.getMaxBusinessServiceSla(criteria)).thenReturn(100L);

        assertThatThrownBy(() -> workflowService.count(requestInfo("user-1"), criteria))
                .isInstanceOf(NullPointerException.class);
        verify(workflowRepository, never()).getProcessInstancesCount(any());
    }

    @Test
    void nearingSlaRejectsANullMaxSla() {
        ProcessInstanceSearchCriteria criteria = nearingSlaCriteria();
        when(mdmsService.fetchSlotPercentageForNearingSla(any())).thenReturn(20);
        when(businessMasterService.getMaxBusinessServiceSla(criteria)).thenReturn(null);

        assertThatThrownBy(() -> workflowService.count(requestInfo("user-1"), criteria))
                .isInstanceOf(NullPointerException.class);
        verify(workflowRepository, never()).getProcessInstancesCount(any());
    }

    @Test
    void countPropagatesARepositoryFailure() {
        ProcessInstanceSearchCriteria criteria = new ProcessInstanceSearchCriteria();
        criteria.setIsNearingSlaCount(false);
        criteria.setStatus(List.of("PENDING"));
        when(workflowRepository.getProcessInstancesCount(criteria)).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> workflowService.count(requestInfo("user-1"), criteria))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("db down");
    }

    @Test
    void userInboxCountIgnoresRequestInfo() {
        ProcessInstanceSearchCriteria criteria = new ProcessInstanceSearchCriteria();
        when(workflowRepository.getProcessInstancesForUserInboxCount(criteria)).thenReturn(9);

        assertThat(workflowService.getUserBasedProcessInstancesCount(null, criteria)).isEqualTo(9);

        verify(util, never()).enrichStatusesInSearchCriteria(any(), any());
        assertThat(criteria.getAssignee()).isNull();
    }

    @ParameterizedTest(name = "inboxCriteria={0}, businessService={1}, inboxCount={2}")
    @MethodSource("statusCountRoutes")
    void statusCountUsesInboxOnlyForNonFsmWhenCriteriaIsNull(boolean inboxCriteria, String businessService,
            boolean inboxCount) {
        ProcessInstanceSearchCriteria criteria = inboxCriteria
                ? new ProcessInstanceSearchCriteria()
                : criteria(null, null, null, List.of("PENDING"));
        criteria.setBusinessService(businessService);
        RequestInfo requestInfo = requestInfo("user-1");
        if (inboxCount) {
            when(workflowRepository.getInboxStatusCount(criteria)).thenReturn(List.of("INBOX"));
        } else {
            when(workflowRepository.getProcessInstancesStatusCount(criteria)).thenReturn(List.of("ALL"));
        }

        assertThat(workflowService.statusCount(requestInfo, criteria))
                .containsExactly(inboxCount ? "INBOX" : "ALL");

        if (inboxCount) {
            assertThat(criteria.getAssignee()).isEqualTo("user-1");
            verify(util).enrichStatusesInSearchCriteria(requestInfo, criteria);
            verify(workflowRepository, never()).getProcessInstancesStatusCount(any());
        } else {
            verify(util, never()).enrichStatusesInSearchCriteria(any(), any());
            verify(workflowRepository, never()).getInboxStatusCount(any());
        }
    }

    @Test
    void statusCountRejectsANullCriteria() {
        assertThatThrownBy(() -> workflowService.statusCount(requestInfo("user-1"), null))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(workflowRepository);
    }

    @Test
    void escalatedSearchReturnsEmptyWhenNoBusinessIdsMatch() {
        ProcessInstanceSearchCriteria criteria = new ProcessInstanceSearchCriteria();
        criteria.setTenantId("pb.amritsar");
        criteria.setBusinessService("PT");
        criteria.setLimit(10);
        RequestInfo requestInfo = requestInfo("user-1");
        when(workflowRepository.fetchEscalatedApplicationsBusinessIdsFromDb(requestInfo, criteria))
                .thenReturn(Collections.emptyList());

        assertThat(workflowService.escalatedApplicationsSearch(requestInfo, criteria)).isEmpty();
        assertThat(criteria.getIsEscalatedCount()).isFalse();
        verify(workflowRepository, never()).getProcessInstances(any());
    }

    @Test
    void escalatedSearchRequeriesByBusinessIdAndDropsPaging() {
        ProcessInstanceSearchCriteria criteria = new ProcessInstanceSearchCriteria();
        criteria.setTenantId("pb.amritsar");
        criteria.setBusinessService("PT");
        criteria.setLimit(5);
        criteria.setOffset(10);
        criteria.setHistory(true);
        RequestInfo requestInfo = requestInfo("user-1");
        ProcessInstance instance = processInstance("BIZ-9");
        when(workflowRepository.fetchEscalatedApplicationsBusinessIdsFromDb(requestInfo, criteria))
                .thenReturn(List.of("BIZ-9", "BIZ-8"));
        when(workflowRepository.getProcessInstances(any())).thenReturn(List.of(instance));
        when(enrichmentService.enrichNextActionForSearch(eq(requestInfo), any())).thenReturn(Collections.emptyList());

        assertThat(workflowService.escalatedApplicationsSearch(requestInfo, criteria)).containsExactly(instance);
        assertThat(criteria.getIsEscalatedCount()).isFalse();

        verify(workflowRepository).getProcessInstances(criteriaCaptor.capture());
        ProcessInstanceSearchCriteria followUp = criteriaCaptor.getValue();
        assertThat(followUp).isNotSameAs(criteria);
        assertThat(followUp.getBusinessIds()).containsExactly("BIZ-9", "BIZ-8");
        assertThat(followUp.getTenantId()).isEqualTo("pb.amritsar");
        assertThat(followUp.getBusinessService()).isEqualTo("PT");
        assertThat(followUp.getLimit()).isNull();
        assertThat(followUp.getOffset()).isNull();
        assertThat(followUp.getHistory()).isFalse();
    }

    @Test
    void escalatedSearchReturnsNullWhenTheFollowUpQueryReturnsNull() {
        ProcessInstanceSearchCriteria criteria = new ProcessInstanceSearchCriteria();
        criteria.setTenantId("pb");
        criteria.setBusinessService("PT");
        RequestInfo requestInfo = requestInfo("user-1");
        when(workflowRepository.fetchEscalatedApplicationsBusinessIdsFromDb(requestInfo, criteria))
                .thenReturn(List.of("BIZ-1"));
        when(workflowRepository.getProcessInstances(any())).thenReturn(null);

        assertThat(workflowService.escalatedApplicationsSearch(requestInfo, criteria)).isNull();
    }

    @Test
    void countEscalatedApplicationsMarksTheCriteriaAndReturnsTheRepositoryCount() {
        ProcessInstanceSearchCriteria criteria = new ProcessInstanceSearchCriteria();
        RequestInfo requestInfo = requestInfo("user-1");
        when(workflowRepository.getEscalatedApplicationsCount(requestInfo, criteria)).thenReturn(6);

        assertThat(workflowService.countEscalatedApplications(requestInfo, criteria)).isEqualTo(6);
        assertThat(criteria.getIsEscalatedCount()).isTrue();
        verify(workflowRepository).getEscalatedApplicationsCount(requestInfo, criteria);
    }

    @Test
    void dashboardCountReturnsTheRepositoryCount() {
        ProcessInstanceSearchCriteria criteria = new ProcessInstanceSearchCriteria();
        criteria.setTenantId("pb");
        criteria.setStatus(List.of("APPROVED"));
        criteria.setCreatedBy("creator-1");
        when(workflowRepository.getDashboardApplicationCount(criteria)).thenReturn(7);

        assertThat(workflowService.getDashboardApplicationCount(requestInfo("user-1"), criteria)).isEqualTo(7);
        verify(workflowRepository).getDashboardApplicationCount(criteria);
    }

    @Test
    void dashboardCountRejectsANullCriteria() {
        assertThatThrownBy(() -> workflowService.getDashboardApplicationCount(requestInfo("user-1"), null))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(workflowRepository);
    }

    @Test
    void dashboardApplicationsMapProcessFieldsAndTreatNullOrEmptyAsEmpty() {
        ProcessInstanceSearchCriteria criteria = new ProcessInstanceSearchCriteria();
        ProcessInstance instance = processInstance("BIZ-3");
        instance.setAction("FORWARD");
        instance.setModuleName("PT");
        when(workflowRepository.getDashboardApplications(criteria)).thenReturn(List.of(instance), null,
                Collections.emptyList());

        List<DashboardProcessInstance> mapped = workflowService.getDashboardApplications(requestInfo("user-1"), criteria);

        assertThat(mapped).singleElement().satisfies(row -> {
            assertThat(row.getId()).isEqualTo("id-BIZ-3");
            assertThat(row.getTenantId()).isEqualTo("pb.amritsar");
            assertThat(row.getBusinessService()).isEqualTo("PT");
            assertThat(row.getBusinessId()).isEqualTo("BIZ-3");
            assertThat(row.getAction()).isEqualTo("FORWARD");
            assertThat(row.getModuleName()).isEqualTo("PT");
        });
        assertThat(workflowService.getDashboardApplications(requestInfo("user-1"), criteria)).isEmpty();
        assertThat(workflowService.getDashboardApplications(requestInfo("user-1"), criteria)).isEmpty();
    }

    @Test
    void dashboardApplicationsWithCountReturnsTheRepositoryResponse() {
        ProcessInstanceSearchCriteria criteria = new ProcessInstanceSearchCriteria();
        DashboardProcessInstanceResponse expected = DashboardProcessInstanceResponse.builder().totalCount(2).build();
        when(workflowRepository.getDashboardApplicationsWithCount(criteria)).thenReturn(expected);

        assertThat(workflowService.getDashboardApplicationsWithCount(requestInfo("user-1"), criteria)).isSameAs(expected);
    }

    @Test
    void dashboardApplicationsWithCountRejectsANullCriteria() {
        assertThatThrownBy(() -> workflowService.getDashboardApplicationsWithCount(requestInfo("user-1"), null))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(workflowRepository);
    }

    private static Stream<Arguments> stateTransitions() {
        return Stream.of(
                Arguments.of("copies previous status and moves to the next state", "state-1", "APPROVED", "state-1",
                        "APPROVED"),
                Arguments.of("null current state leaves previous status empty", null, "APPROVED", null, "APPROVED"),
                Arguments.of("null resultant state clears the current state", "state-1", null, "state-1", null));
    }

    private static Stream<Arguments> directVersusInboxSearch() {
        return Stream.of(
                Arguments.of(null, null, null, null, true),
                Arguments.of(List.of("BIZ-1"), null, null, null, false),
                Arguments.of(null, List.of("id-1"), null, null, false),
                Arguments.of(null, null, "assignee-1", null, false),
                Arguments.of(null, null, null, List.of("PENDING"), false),
                Arguments.of(Collections.emptyList(), null, null, null, false));
    }

    private static Stream<Arguments> statusCountRoutes() {
        return Stream.of(
                Arguments.of(true, "PT", true),
                Arguments.of(true, "FSM", false),
                Arguments.of(true, "fsm", false),
                Arguments.of(true, " FSM", true),
                Arguments.of(true, null, false),
                Arguments.of(false, "PT", false),
                Arguments.of(false, "FSM", false));
    }

    private static RequestInfo requestInfo(String uuid) {
        User user = new User();
        user.setUuid(uuid);
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setUserInfo(user);
        return requestInfo;
    }

    private static ProcessInstance processInstance(String businessId) {
        return ProcessInstance.builder()
                .id("id-" + businessId)
                .tenantId("pb.amritsar")
                .businessService("PT")
                .businessId(businessId)
                .action("FORWARD")
                .moduleName("PT")
                .build();
    }

    private static ProcessInstanceRequest request(RequestInfo requestInfo, List<ProcessInstance> processInstances) {
        return ProcessInstanceRequest.builder().requestInfo(requestInfo).processInstances(processInstances).build();
    }

    private static ProcessStateAndAction actionFor(ProcessInstance instance, State resultant) {
        ProcessStateAndAction action = new ProcessStateAndAction();
        action.setProcessInstanceFromRequest(instance);
        action.setResultantState(resultant);
        return action;
    }

    private static ProcessInstanceSearchCriteria criteria(List<String> businessIds, List<String> ids, String assignee,
            List<String> status) {
        ProcessInstanceSearchCriteria criteria = new ProcessInstanceSearchCriteria();
        criteria.setBusinessIds(businessIds);
        criteria.setIds(ids);
        criteria.setAssignee(assignee);
        criteria.setStatus(status);
        return criteria;
    }

    private static ProcessInstanceSearchCriteria nearingSlaCriteria() {
        ProcessInstanceSearchCriteria criteria = new ProcessInstanceSearchCriteria();
        criteria.setIsNearingSlaCount(true);
        criteria.setBusinessService("PT");
        criteria.setStatus(List.of("PENDING"));
        return criteria;
    }
}
