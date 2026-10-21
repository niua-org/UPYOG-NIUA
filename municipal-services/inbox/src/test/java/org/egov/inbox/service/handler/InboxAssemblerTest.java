// generated-by: generate_per_class_unit_tests.py
package org.egov.inbox.service.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class InboxAssemblerTest {


    @InjectMocks
    private InboxAssembler subject;


    @Test
    void assemble_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
