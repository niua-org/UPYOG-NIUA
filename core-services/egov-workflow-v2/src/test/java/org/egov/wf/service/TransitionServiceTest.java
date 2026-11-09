package org.egov.wf.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.egov.tracer.model.CustomException;
import org.egov.wf.repository.BusinessServiceRepository;
import org.egov.wf.repository.WorKflowRepository;
import org.egov.wf.util.WorkflowUtil;
import org.egov.wf.web.models.Action;
import org.egov.wf.web.models.BusinessService;
import org.egov.wf.web.models.ProcessInstance;
import org.egov.wf.web.models.ProcessStateAndAction;
import org.egov.wf.web.models.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransitionServiceTest {

    private static final String TENANT = "pb.amritsar";
    private static final String INITIAL_UUID = "11111111-1111-4111-8111-111111111111";
    private static final String NEXT_UUID = "22222222-2222-4222-8222-222222222222";

    @Mock
    private WorKflowRepository repository;
    @Mock
    private BusinessServiceRepository businessServiceRepository;
    @Mock
    private WorkflowUtil workflowUtil;

    private TransitionService transitionService;

    @BeforeEach
    void setUp() {
        transitionService = new TransitionService(repository, businessServiceRepository, workflowUtil);
    }

    @Test
    void getProcessStateAndActionsTransitionResolvesActionAndResultantStateForNewInstance() {
        BusinessService businessService = businessServiceWithTransition();
        when(businessServiceRepository.getBusinessServices(any())).thenReturn(List.of(businessService));
        when(repository.getProcessInstances(any())).thenReturn(Collections.emptyList());
        when(workflowUtil.rolesAllowedInService(businessService)).thenReturn(List.of("EMPLOYEE"));

        ProcessInstance request = processInstance("BIZ-1", "FORWARD");
        List<ProcessStateAndAction> result = transitionService.getProcessStateAndActions(List.of(request), true);

        assertThat(result).hasSize(1);
        ProcessStateAndAction psa = result.get(0);
        assertThat(psa.getAction().getAction()).isEqualToIgnoringCase("FORWARD");
        assertThat(psa.getResultantState().getUuid()).isEqualTo(NEXT_UUID);
        assertThat(psa.getCurrentState().getUuid()).isEqualTo(INITIAL_UUID);
        assertThat(request.getBusinesssServiceSla()).isEqualTo(5_000L);
        assertThat(request.getModuleName()).isEqualTo("PT");
    }

    @Test
    void getProcessStateAndActionsTransitionUsesStateFromDatabaseWhenPresent() {
        BusinessService businessService = businessServiceWithTransition();
        State dbState = State.builder().uuid("db-state").state("INPROGRESS").build();
        ProcessInstance fromDb = ProcessInstance.builder()
                .businessId("BIZ-2")
                .state(dbState)
                .build();
        when(businessServiceRepository.getBusinessServices(any())).thenReturn(List.of(businessService));
        when(repository.getProcessInstances(any())).thenReturn(List.of(fromDb));
        when(workflowUtil.rolesAllowedInService(businessService)).thenReturn(List.of("EMPLOYEE"));

        Action inProgressAction = Action.builder()
                .action("FORWARD")
                .nextState(NEXT_UUID)
                .roles(List.of("EMPLOYEE"))
                .build();
        dbState.setActions(List.of(inProgressAction));

        ProcessInstance request = processInstance("BIZ-2", "FORWARD");
        List<ProcessStateAndAction> result = transitionService.getProcessStateAndActions(List.of(request), true);

        assertThat(result.get(0).getCurrentState()).isSameAs(dbState);
        assertThat(result.get(0).getProcessInstanceFromDb()).isSameAs(fromDb);
        assertThat(request.getBusinesssServiceSla()).isNull();
    }

    @Test
    void getProcessStateAndActionsSearchPrefersRequestStateEvenWhenDatabaseRecordExists() {
        BusinessService businessService = businessServiceWithTransition();
        State dbState = State.builder().uuid("db-state").state("INPROGRESS").build();
        ProcessInstance fromDb = ProcessInstance.builder().businessId("BIZ-3").state(dbState).build();
        when(businessServiceRepository.getBusinessServices(any())).thenReturn(List.of(businessService));
        when(repository.getProcessInstances(any())).thenReturn(List.of(fromDb));
        when(workflowUtil.rolesAllowedInService(businessService)).thenReturn(List.of("EMPLOYEE"));

        State requestState = State.builder().uuid(INITIAL_UUID).state("").build();
        requestState.setActions(List.of(Action.builder()
                .action("FORWARD")
                .nextState(NEXT_UUID)
                .roles(List.of("EMPLOYEE"))
                .build()));
        ProcessInstance request = processInstance("BIZ-3", "FORWARD");
        request.setState(requestState);

        List<ProcessStateAndAction> result = transitionService.getProcessStateAndActions(List.of(request), false);

        assertThat(result.get(0).getCurrentState()).isSameAs(requestState);
        assertThat(result.get(0).getProcessInstanceFromDb()).isSameAs(fromDb);
    }

    @Test
    void getProcessStateAndActionsSearchUsesRequestState() {
        BusinessService businessService = businessServiceWithTransition();
        when(businessServiceRepository.getBusinessServices(any())).thenReturn(List.of(businessService));
        when(repository.getProcessInstances(any())).thenReturn(Collections.emptyList());
        when(workflowUtil.rolesAllowedInService(businessService)).thenReturn(List.of("EMPLOYEE"));

        State requestState = State.builder().uuid(INITIAL_UUID).state("").build();
        requestState.setActions(List.of(Action.builder()
                .action("FORWARD")
                .nextState(NEXT_UUID)
                .roles(List.of("EMPLOYEE"))
                .build()));
        ProcessInstance request = processInstance("BIZ-4", "FORWARD");
        request.setState(requestState);

        List<ProcessStateAndAction> result = transitionService.getProcessStateAndActions(List.of(request), false);

        assertThat(result.get(0).getCurrentState()).isSameAs(requestState);
        assertThat(result.get(0).getAction()).isNotNull();
        assertThat(result.get(0).getResultantState()).isNull();
    }

    @Test
    void getProcessStateAndActionsExpandsWildcardRoles() {
        BusinessService businessService = businessServiceWithTransition();
        businessService.getStates().get(0).getActions().get(0).setRoles(new ArrayList<>(List.of("*")));
        when(businessServiceRepository.getBusinessServices(any())).thenReturn(List.of(businessService));
        when(repository.getProcessInstances(any())).thenReturn(Collections.emptyList());
        List<String> allowed = List.of("CITIZEN", "EMPLOYEE");
        when(workflowUtil.rolesAllowedInService(businessService)).thenReturn(allowed);

        ProcessInstance request = processInstance("BIZ-5", "FORWARD");
        transitionService.getProcessStateAndActions(List.of(request), true);

        assertThat(businessService.getStates().get(0).getActions().get(0).getRoles()).containsExactlyElementsOf(allowed);
    }

    @Test
    void getProcessStateAndActionsThrowsWhenBusinessServiceMissing() {
        when(businessServiceRepository.getBusinessServices(any())).thenReturn(Collections.emptyList());
        ProcessInstance request = processInstance("BIZ-6", "FORWARD");

        assertThatThrownBy(() -> transitionService.getProcessStateAndActions(List.of(request), true))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("No bussinessService object found");
    }

    @Test
    void getProcessStateAndActionsThrowsWhenMultipleBusinessServicesReturned() {
        when(businessServiceRepository.getBusinessServices(any()))
                .thenReturn(List.of(new BusinessService(), new BusinessService()));
        ProcessInstance request = processInstance("BIZ-7", "FORWARD");

        assertThatThrownBy(() -> transitionService.getProcessStateAndActions(List.of(request), true))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Multiple bussinessService object found");
    }

    @Test
    void getProcessStateAndActionsThrowsWhenTransitionActionIsInvalid() {
        BusinessService businessService = businessServiceWithTransition();
        when(businessServiceRepository.getBusinessServices(any())).thenReturn(List.of(businessService));
        when(repository.getProcessInstances(any())).thenReturn(Collections.emptyList());
        when(workflowUtil.rolesAllowedInService(businessService)).thenReturn(List.of("EMPLOYEE"));

        ProcessInstance request = processInstance("BIZ-8", "UNKNOWN");

        assertThatThrownBy(() -> transitionService.getProcessStateAndActions(List.of(request), true))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Action UNKNOWN not found");
    }

    @Test
    void getProcessStateAndActionsResolvesMultipleInstancesInOneRequest() {
        BusinessService businessService = businessServiceWithTransition();
        when(businessServiceRepository.getBusinessServices(any())).thenReturn(List.of(businessService));
        when(repository.getProcessInstances(any())).thenReturn(Collections.emptyList());
        when(workflowUtil.rolesAllowedInService(businessService)).thenReturn(List.of("EMPLOYEE"));

        ProcessInstance first = processInstance("BIZ-9", "FORWARD");
        ProcessInstance second = processInstance("BIZ-10", "FORWARD");
        List<ProcessStateAndAction> result = transitionService.getProcessStateAndActions(List.of(first, second), true);

        assertThat(result).hasSize(2);
    }

    @Test
    void getProcessStateAndActionsSkipsNamedStatesWhenResolvingInitialState() {
        State named = State.builder().uuid("named").state("DRAFT").build();
        Action forward = Action.builder().action("FORWARD").nextState(NEXT_UUID).roles(List.of("EMPLOYEE")).build();
        State initial = State.builder().uuid(INITIAL_UUID).state("").build();
        initial.setActions(List.of(forward));
        State next = State.builder().uuid(NEXT_UUID).state("APPROVED").build();
        BusinessService businessService = BusinessService.builder()
                .tenantId(TENANT)
                .businessService("PT")
                .business("PT")
                .states(new ArrayList<>(List.of(named, initial, next)))
                .build();
        when(businessServiceRepository.getBusinessServices(any())).thenReturn(List.of(businessService));
        when(repository.getProcessInstances(any())).thenReturn(Collections.emptyList());
        when(workflowUtil.rolesAllowedInService(businessService)).thenReturn(List.of("EMPLOYEE"));

        List<ProcessStateAndAction> result = transitionService.getProcessStateAndActions(
                List.of(processInstance("BIZ-11", "FORWARD")), true);

        assertThat(result.get(0).getCurrentState().getUuid()).isEqualTo(INITIAL_UUID);
    }

    @Test
    void getProcessStateAndActionsSearchAllowsCurrentStateWithoutConfiguredActions() {
        BusinessService businessService = businessServiceWithTransition();
        businessService.getStates().get(0).setActions(Collections.emptyList());
        when(businessServiceRepository.getBusinessServices(any())).thenReturn(List.of(businessService));
        when(repository.getProcessInstances(any())).thenReturn(Collections.emptyList());
        when(workflowUtil.rolesAllowedInService(businessService)).thenReturn(List.of("EMPLOYEE"));

        State requestState = State.builder().uuid(INITIAL_UUID).state("").build();
        requestState.setActions(Collections.emptyList());
        ProcessInstance request = processInstance("BIZ-12", "FORWARD");
        request.setState(requestState);

        List<ProcessStateAndAction> result = transitionService.getProcessStateAndActions(List.of(request), false);

        assertThat(result.get(0).getAction()).isNull();
    }

    @Test
    void prepareProcessStateAndActionMapsInstancesFromRepository() {
        BusinessService businessService = businessServiceWithTransition();
        ProcessInstance fromDb = ProcessInstance.builder().businessId("BIZ-13").build();
        when(businessServiceRepository.getBusinessServices(any())).thenReturn(List.of(businessService));
        when(repository.getProcessInstances(any())).thenReturn(List.of(fromDb));
        when(workflowUtil.rolesAllowedInService(businessService)).thenReturn(List.of("EMPLOYEE"));

        ArgumentCaptor<org.egov.wf.web.models.ProcessInstanceSearchCriteria> captor =
                ArgumentCaptor.forClass(org.egov.wf.web.models.ProcessInstanceSearchCriteria.class);
        ProcessInstance request = processInstance("BIZ-13", "FORWARD");

        transitionService.getProcessStateAndActions(List.of(request), true);

        verify(repository).getProcessInstances(captor.capture());
        assertThat(captor.getValue().getBusinessIds()).containsExactly("BIZ-13");
        assertThat(captor.getValue().getTenantId()).isEqualTo(TENANT);
    }

    private static BusinessService businessServiceWithTransition() {
        Action forward = Action.builder()
                .action("FORWARD")
                .nextState(NEXT_UUID)
                .roles(new ArrayList<>(List.of("EMPLOYEE")))
                .build();
        State initial = State.builder()
                .uuid(INITIAL_UUID)
                .state("")
                .sla(100L)
                .build();
        initial.setActions(new ArrayList<>(List.of(forward)));
        State next = State.builder().uuid(NEXT_UUID).state("APPROVED").build();
        return BusinessService.builder()
                .tenantId(TENANT)
                .businessService("PT")
                .business("PT")
                .businessServiceSla(5_000L)
                .states(new ArrayList<>(List.of(initial, next)))
                .build();
    }

    private static ProcessInstance processInstance(String businessId, String action) {
        return ProcessInstance.builder()
                .tenantId(TENANT)
                .businessService("PT")
                .businessId(businessId)
                .action(action)
                .build();
    }
}
