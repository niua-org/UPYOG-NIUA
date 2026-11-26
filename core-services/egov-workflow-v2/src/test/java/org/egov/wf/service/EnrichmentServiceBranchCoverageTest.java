package org.egov.wf.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.common.contract.request.User;
import org.egov.tracer.model.CustomException;
import org.egov.wf.repository.BusinessServiceRepository;
import org.egov.wf.util.WorkflowUtil;
import org.egov.wf.config.WorkflowConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.wf.web.models.Action;
import org.egov.wf.web.models.AuditDetails;
import org.egov.wf.web.models.BusinessService;
import org.egov.wf.web.models.BusinessServiceRequest;
import org.egov.wf.web.models.Document;
import org.egov.wf.web.models.ProcessInstance;
import org.egov.wf.web.models.ProcessStateAndAction;
import org.egov.wf.web.models.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EnrichmentServiceBranchCoverageTest {

    private static final String STATE_UUID = "11111111-1111-4111-8111-111111111111";
    private static final String NEXT_UUID = "22222222-2222-4222-8222-222222222222";

    @Mock
    private WorkflowUtil util;
    @Mock
    private UserService userService;
    @Mock
    private TransitionService transitionService;

    private EnrichmentService enrichmentService;

    @BeforeEach
    void setUp() {
        enrichmentService = new EnrichmentService(util, userService, transitionService);
        lenient().when(userService.searchUser(any(), any())).thenAnswer(invocation -> {
            Map<String, User> users = new HashMap<>();
            List<String> uuids = invocation.getArgument(1);
            if (uuids != null) {
                for (String uuid : uuids) {
                    users.put(uuid, user(uuid));
                }
            }
            return users;
        });
    }

    @Test
    void enrichProcessRequestPreservesCreatedAuditWhenStateDoesNotChange() {
        AuditDetails freshAudit = auditDetails(900L, 900L, "modifier");
        when(util.getAuditDetails(eq("modifier"), eq(true))).thenReturn(freshAudit);

        ProcessInstance fromDb = ProcessInstance.builder()
                .businesssServiceSla(1_000L)
                .auditDetails(auditDetails(100L, 200L, "creator"))
                .build();
        Action action = Action.builder()
                .currentState(STATE_UUID)
                .nextState(STATE_UUID)
                .action("SAVE")
                .roles(List.of("EMPLOYEE"))
                .build();
        ProcessInstance request = ProcessInstance.builder()
                .tenantId("pb.amritsar")
                .auditDetails(auditDetails(400L, 500L, "modifier"))
                .build();
        ProcessStateAndAction psa = psa(request, fromDb, action, State.builder().uuid(STATE_UUID).build());

        enrichmentService.enrichProcessRequest(requestInfo("modifier"), List.of(psa));

        assertThat(freshAudit.getCreatedBy()).isEqualTo("creator");
        assertThat(freshAudit.getCreatedTime()).isEqualTo(100L);
        assertThat(request.getStateSla()).isNull();
    }

    @Test
    void enrichProcessRequestEnrichesDocumentsAndStateSlaWhenStateChanges() {
        AuditDetails freshAudit = auditDetails(1_000L, 1_000L, "modifier");
        when(util.getAuditDetails(eq("modifier"), eq(true))).thenReturn(freshAudit);

        Document document = Document.builder().build();
        ProcessInstance request = ProcessInstance.builder()
                .tenantId("pb.amritsar")
                .documents(new ArrayList<>(List.of(document)))
                .build();
        Action action = Action.builder()
                .currentState(STATE_UUID)
                .nextState(NEXT_UUID)
                .action("FORWARD")
                .roles(List.of("EMPLOYEE"))
                .build();
        State resultant = State.builder().uuid(NEXT_UUID).sla(250L).build();
        ProcessStateAndAction psa = psa(request, null, action, resultant);

        enrichmentService.enrichProcessRequest(requestInfo("modifier"), List.of(psa));

        assertThat(request.getStateSla()).isEqualTo(250L);
        assertThat(document.getTenantId()).isEqualTo("pb.amritsar");
        assertThat(document.getAuditDetails()).isSameAs(freshAudit);
        assertThat(document.getId()).isNotBlank();
    }

    @Test
    void enrichProcessRequestUpdatesRemainingStateSlaWhenTransitionDoesNotChangeState() {
        AuditDetails freshAudit = auditDetails(1_900L, 2_000L, "modifier");
        when(util.getAuditDetails(eq("modifier"), eq(true))).thenReturn(freshAudit);

        ProcessInstance fromDb = ProcessInstance.builder()
                .businesssServiceSla(8_000L)
                .stateSla(1_500L)
                .auditDetails(auditDetails(900L, 1_000L, "creator"))
                .build();
        ProcessInstance request = ProcessInstance.builder()
                .tenantId("pb.amritsar")
                .auditDetails(auditDetails(1_400L, 1_500L, "modifier"))
                .build();
        Action action = Action.builder()
                .currentState(STATE_UUID)
                .nextState(STATE_UUID)
                .action("SAVE")
                .roles(List.of("EMPLOYEE"))
                .build();
        ProcessStateAndAction psa = psa(request, fromDb, action, State.builder().uuid(STATE_UUID).build());

        enrichmentService.enrichProcessRequest(requestInfo("modifier"), List.of(psa));

        assertThat(request.getBusinesssServiceSla()).isEqualTo(7_000L);
        assertThat(request.getStateSla()).isEqualTo(500L);
    }

    @Test
    void enrichCreateBusinessServiceEnrichesActionsAndMapsNextStateByName() {
        AuditDetails audit = auditDetails(1L, 1L, "admin");
        when(util.getAuditDetails(eq("admin"), eq(true))).thenReturn(audit);

        Action action = Action.builder().action("FORWARD").nextState("APPROVED").build();
        State initial = State.builder().state("APPLIED").actions(new ArrayList<>(List.of(action))).build();
        State approved = State.builder().state("APPROVED").build();
        BusinessService businessService = BusinessService.builder()
                .tenantId("pb")
                .states(new ArrayList<>(List.of(initial, approved)))
                .build();
        BusinessServiceRequest request = new BusinessServiceRequest();
        request.setRequestInfo(requestInfo("admin"));
        request.setBusinessServices(new ArrayList<>(List.of(businessService)));

        enrichmentService.enrichCreateBusinessService(request);

        assertThat(action.getUuid()).isNotBlank();
        assertThat(action.getCurrentState()).isEqualTo(initial.getUuid());
        assertThat(action.getNextState()).isEqualTo(approved.getUuid());
    }

    @Test
    void enrichUpdateBusinessServiceUpdatesExistingStateAndActionAuditDetails() {
        AuditDetails audit = auditDetails(2L, 2L, "admin");
        when(util.getAuditDetails(eq("admin"), eq(true))).thenReturn(audit);

        Action existingAction = Action.builder()
                .uuid("33333333-3333-4333-8333-333333333333")
                .action("FORWARD")
                .nextState(NEXT_UUID)
                .build();
        State existingState = State.builder()
                .uuid(STATE_UUID)
                .state("APPLIED")
                .actions(new ArrayList<>(List.of(existingAction)))
                .build();
        BusinessService businessService = BusinessService.builder()
                .tenantId("pb")
                .states(new ArrayList<>(List.of(existingState)))
                .build();
        BusinessServiceRequest request = new BusinessServiceRequest();
        request.setRequestInfo(requestInfo("admin"));
        request.setBusinessServices(new ArrayList<>(List.of(businessService)));

        enrichmentService.enrichUpdateBusinessService(request);

        assertThat(existingState.getAuditDetails()).isSameAs(audit);
        assertThat(existingAction.getAuditDetails()).isSameAs(audit);
    }

    @Test
    void enrichUpdateBusinessServiceCreatesNewActionAndThrowsForUnknownNextState() {
        AuditDetails audit = auditDetails(3L, 3L, "admin");
        when(util.getAuditDetails(eq("admin"), eq(true))).thenReturn(audit);

        Action invalid = Action.builder().action("FORWARD").nextState("MISSING").build();
        State state = State.builder().uuid(STATE_UUID).state("APPLIED").actions(new ArrayList<>(List.of(invalid))).build();
        BusinessService businessService = BusinessService.builder()
                .tenantId("pb")
                .states(new ArrayList<>(List.of(state)))
                .build();
        BusinessServiceRequest request = new BusinessServiceRequest();
        request.setRequestInfo(requestInfo("admin"));
        request.setBusinessServices(new ArrayList<>(List.of(businessService)));

        assertThatThrownBy(() -> enrichmentService.enrichUpdateBusinessService(request))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void enrichUsersResolvesAssigneesFromSearchResponse() {
        User assignee = user("assignee-1");
        User assigner = user("assigner-1");
        Map<String, User> users = Map.of("assignee-1", assignee, "assigner-1", assigner);
        when(userService.searchUser(any(), any())).thenReturn(users);

        ProcessInstance request = ProcessInstance.builder()
                .assigner(User.builder().uuid("assigner-1").build())
                .assignes(new ArrayList<>(List.of(User.builder().uuid("assignee-1").build())))
                .build();
        ProcessStateAndAction psa = new ProcessStateAndAction();
        psa.setProcessInstanceFromRequest(request);

        enrichmentService.enrichUsers(requestInfo("assigner-1"), List.of(psa));

        assertThat(request.getAssignes()).containsExactly(assignee);
        assertThat(request.getAssigner()).isSameAs(assigner);
    }

    @Test
    void enrichUpdateBusinessServiceCreatesUuidForNewAction() {
        AuditDetails audit = auditDetails(4L, 4L, "admin");
        when(util.getAuditDetails(eq("admin"), eq(true))).thenReturn(audit);

        Action newAction = Action.builder().action("FORWARD").nextState(NEXT_UUID).build();
        State state = State.builder().uuid(STATE_UUID).state("APPLIED").actions(new ArrayList<>(List.of(newAction))).build();
        BusinessService businessService = BusinessService.builder()
                .tenantId("pb")
                .states(new ArrayList<>(List.of(state)))
                .build();
        BusinessServiceRequest request = new BusinessServiceRequest();
        request.setRequestInfo(requestInfo("admin"));
        request.setBusinessServices(new ArrayList<>(List.of(businessService)));

        enrichmentService.enrichUpdateBusinessService(request);

        assertThat(newAction.getUuid()).isNotBlank();
        assertThat(newAction.getCurrentState()).isEqualTo(STATE_UUID);
    }

    @Test
    void enrichProcessRequestAddsNextActionsFromResultantStateUsingWorkflowUtil() {
        WorkflowUtil realUtil = new WorkflowUtil(new ObjectMapper(), mock(WorkflowConfig.class),
                mock(BusinessServiceRepository.class));
        enrichmentService = new EnrichmentService(realUtil, userService, transitionService);

        Action next = Action.builder().action("APPROVE").roles(List.of("EMPLOYEE")).build();
        State resultant = State.builder().uuid(NEXT_UUID).sla(100L).actions(List.of(next)).build();
        ProcessInstance request = ProcessInstance.builder().tenantId("pb.amritsar").build();
        Action action = Action.builder()
                .currentState(STATE_UUID)
                .nextState(NEXT_UUID)
                .action("FORWARD")
                .roles(List.of("EMPLOYEE"))
                .build();
        ProcessStateAndAction psa = psa(request, null, action, resultant);

        RequestInfo requestInfo = requestInfo("modifier");
        requestInfo.getUserInfo().setRoles(List.of(Role.builder().code("EMPLOYEE").tenantId("pb.amritsar").build()));

        enrichmentService.enrichProcessRequest(requestInfo, List.of(psa));

        assertThat(request.getNextActions()).extracting(Action::getAction).containsExactly("APPROVE");
    }

    @Test
    void enrichNextActionForSearchUsesWorkflowUtilRoleChecksWhenFilteringActions() {
        WorkflowUtil realUtil = new WorkflowUtil(new ObjectMapper(), mock(WorkflowConfig.class),
                mock(BusinessServiceRepository.class));
        enrichmentService = new EnrichmentService(realUtil, userService, transitionService);

        Action allowed = Action.builder().action("APPROVE").roles(List.of("EMPLOYEE")).build();
        Action hidden = Action.builder().action("REJECT").roles(List.of("ADMIN")).build();
        State current = State.builder().uuid(STATE_UUID).state("PENDING").actions(List.of(allowed, hidden)).build();
        ProcessInstance instance = ProcessInstance.builder()
                .tenantId("pb.amritsar")
                .businessService("PT")
                .businessId("BIZ-1")
                .build();
        ProcessStateAndAction stateAndAction = new ProcessStateAndAction();
        stateAndAction.setProcessInstanceFromRequest(instance);
        stateAndAction.setCurrentState(current);
        when(transitionService.getProcessStateAndActions(any(), eq(false))).thenReturn(List.of(stateAndAction));

        RequestInfo requestInfo = requestInfo("user-1");
        requestInfo.getUserInfo().setRoles(List.of(
                Role.builder().code("EMPLOYEE").tenantId("pb").build(),
                Role.builder().code("ADMIN").tenantId("other").build()));

        enrichmentService.enrichNextActionForSearch(requestInfo, List.of(instance));

        assertThat(instance.getNextActions()).extracting(Action::getAction).containsExactly("APPROVE");
    }

    @Test
    void enrichNextActionForSearchIgnoresTransitionFailuresForOneBusinessService() {
        ProcessInstance ptInstance = ProcessInstance.builder()
                .tenantId("pb.amritsar")
                .businessService("PT")
                .businessId("BIZ-1")
                .build();
        when(transitionService.getProcessStateAndActions(List.of(ptInstance), false))
                .thenThrow(new CustomException("ERR", "fail"));

        List<ProcessStateAndAction> result = enrichmentService.enrichNextActionForSearch(requestInfo("user-1"),
                List.of(ptInstance));

        assertThat(result).isEmpty();
    }

    @Test
    void fetchStatesToIgnoreFromMdmsBuildsRequestForTenant() {
        ReflectionTestUtils.setField(enrichmentService, "mdmsHost", "http://mdms");
        ReflectionTestUtils.setField(enrichmentService, "mdmsUrl", "/search");

        assertThat(enrichmentService.fetchStatesToIgnoreFromMdms(requestInfo("u1"), "pb.amritsar")).isEmpty();
        assertThat(enrichmentService.fetchStatesToIgnoreFromMdms(requestInfo("u1"), "")).isEmpty();
    }

    private static ProcessStateAndAction psa(ProcessInstance request, ProcessInstance fromDb, Action action,
            State resultant) {
        ProcessStateAndAction psa = new ProcessStateAndAction();
        psa.setProcessInstanceFromRequest(request);
        psa.setProcessInstanceFromDb(fromDb);
        psa.setAction(action);
        psa.setResultantState(resultant);
        return psa;
    }

    private static AuditDetails auditDetails(long createdTime, long lastModified, String user) {
        AuditDetails audit = new AuditDetails();
        audit.setCreatedBy(user);
        audit.setCreatedTime(createdTime);
        audit.setLastModifiedBy(user);
        audit.setLastModifiedTime(lastModified);
        return audit;
    }

    private static RequestInfo requestInfo(String uuid) {
        User user = new User();
        user.setUuid(uuid);
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setUserInfo(user);
        return requestInfo;
    }

    private static User user(String uuid) {
        User user = new User();
        user.setUuid(uuid);
        return user;
    }
}
