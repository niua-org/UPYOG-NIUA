// generated-by: generate_per_class_unit_tests.py
package org.egov.tl.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.tl.config.TLConfiguration;
import org.egov.tl.repository.IdGenRepository;
import org.egov.tl.service.BoundaryService;
import org.egov.tl.service.UserService;
import org.egov.tl.util.TradeUtil;
import org.egov.tl.workflow.WorkflowService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class EnrichmentServiceTest {
    @Mock
    private IdGenRepository idGenRepository;
    @Mock
    private TLConfiguration config;
    @Mock
    private TradeUtil tradeUtil;
    @Mock
    private BoundaryService boundaryService;
    @Mock
    private UserService userService;
    @Mock
    private WorkflowService workflowService;

    private EnrichmentService subject;

    @BeforeEach
    void setUp() {
        subject = new EnrichmentService(idGenRepository, config, tradeUtil, boundaryService, userService, workflowService);
    }


    @Test
    void enrichTLCreateRequest_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
