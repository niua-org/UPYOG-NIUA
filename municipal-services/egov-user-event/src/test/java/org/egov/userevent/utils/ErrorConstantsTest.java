package org.egov.userevent.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ErrorConstantsTest {


    @InjectMocks
    private ErrorConstants subject;


    @Test
    void subject_whenConstructed_isNotNull() {
        assertThat(subject).isNotNull();
    }

}
