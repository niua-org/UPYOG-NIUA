// generated-by: generate_per_class_unit_tests.py
package org.egov.tl.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.egov.tl.config.TLConfiguration;
import org.egov.tl.repository.TLRepository;
import org.egov.tl.service.UserService;
import org.egov.tl.util.TradeUtil;
import org.egov.tl.validator.MDMSValidator;
import org.egov.tl.validator.PropertyValidator;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TLValidatorTest {
    @Mock
    private TLRepository tlRepository;
    @Mock
    private TLConfiguration config;
    @Mock
    private PropertyValidator propertyValidator;
    @Mock
    private MDMSValidator mdmsValidator;
    @Mock
    private TradeUtil tradeUtil;
    @Mock
    private UserService userService;

    private TLValidator subject;

    @BeforeEach
    void setUp() {
        subject = new TLValidator(tlRepository, config, propertyValidator, mdmsValidator, tradeUtil, userService);
    }


    @Test
    void validateCreate_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
