package org.upyog.reconciliation.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationValidationEngineTest {

    @Mock
    private KieContainer kieContainer;

    @InjectMocks
    private ReconciliationValidationEngine engine;

    @Test
    void validate_whenKieContainerMissing_returnsEmptyValidResult() {
        ReconciliationValidationEngine engineWithoutKie = new ReconciliationValidationEngine();

        ValidationResult result = engineWithoutKie.validate(
                List.of(Map.of("k", "v")), "pg.citya", "finance");

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void validate_whenKiePresent_firesRulesAndDisposesSession() {
        KieSession session = mock(KieSession.class);
        when(kieContainer.newKieSession()).thenReturn(session);

        ValidationResult result = engine.validate(
                List.of(Map.of("amount", 10)), "pg.citya", "finance");

        verify(session).setGlobal("validationResult", result);
        verify(session).setGlobal("tenantId", "pg.citya");
        verify(session).setGlobal("module", "finance");
        verify(session).insert(any());
        verify(session).fireAllRules();
        verify(session).dispose();
    }

    @Test
    void validate_emptyDataset_stillRunsSession() {
        KieSession session = mock(KieSession.class);
        when(kieContainer.newKieSession()).thenReturn(session);

        engine.validate(Collections.emptyList(), "pg.citya", "finance");

        verify(session).fireAllRules();
        verify(session).dispose();
    }
}
