package org.egov.bpa.workflow;

import org.egov.bpa.web.model.workflow.BusinessService;
import org.egov.bpa.web.model.workflow.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.ServiceRequestRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

    @Mock
    private BPAConfiguration config;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private ObjectMapper mapper;

    @InjectMocks
    private WorkflowService workflowService;

    private BusinessService businessService;

    @BeforeEach
    void setUp() {
        businessService = BusinessService.builder()
                .states(List.of(
                        State.builder().applicationStatus("INPROGRESS").isStateUpdatable(true).state("In Progress").build(),
                        State.builder().applicationStatus("APPROVED").isStateUpdatable(false).state("Approved").build()))
                .build();
    }

    @Test
    void isStateUpdatable_whenStatusMatches_returnsConfiguredFlag() {
        assertThat(workflowService.isStateUpdatable("INPROGRESS", businessService)).isTrue();
        assertThat(workflowService.isStateUpdatable("APPROVED", businessService)).isFalse();
    }

    @Test
    void isStateUpdatable_whenStatusUnknown_returnsFalse() {
        assertThat(workflowService.isStateUpdatable("UNKNOWN", businessService)).isFalse();
    }

    @Test
    void getCurrentStateObj_whenStatusMatches_returnsState() {
        State state = workflowService.getCurrentStateObj("INPROGRESS", businessService);

        assertThat(state).isNotNull();
        assertThat(state.getState()).isEqualTo("In Progress");
    }

    @Test
    void getCurrentState_whenStatusMatches_returnsStateName() {
        assertThat(workflowService.getCurrentState("APPROVED", businessService)).isEqualTo("Approved");
    }
}
