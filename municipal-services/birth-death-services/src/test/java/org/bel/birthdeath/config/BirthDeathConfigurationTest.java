package org.bel.birthdeath.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BirthDeathConfigurationTest {


    @InjectMocks
    private BirthDeathConfiguration subject;


    @Test
    void subject_whenConstructed_isNotNull() {
        assertThat(subject).isNotNull();
    }

}
