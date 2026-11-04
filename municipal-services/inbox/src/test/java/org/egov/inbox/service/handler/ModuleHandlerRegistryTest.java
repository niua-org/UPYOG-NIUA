package org.egov.inbox.service.handler;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleHandlerRegistryTest {

    private final ModuleInboxHandler fsmHandler = new ModuleInboxHandler() {
        @Override
        public boolean supports(String moduleName) {
            return "fsm".equalsIgnoreCase(moduleName);
        }

        @Override
        public void fetchApplicationIds(InboxContext ctx) {
        }

        @Override
        public String getApplicationIdParamKey() {
            return "applicationNumber";
        }

        @Override
        public String getInternalModuleName(String moduleName) {
            return "FSM";
        }

        @Override
        public boolean isWorkflowTotalCountRequired() {
            return false;
        }

        @Override
        public boolean isWorkflowNearingSlaCountRequired() {
            return true;
        }
    };

    private final ModuleHandlerRegistry registry = new ModuleHandlerRegistry(List.of(fsmHandler));

    @Test
    void getHandler_whenModuleSupported_returnsHandler() {
        Optional<ModuleInboxHandler> handler = registry.getHandler("fsm");

        assertThat(handler).isPresent();
        assertThat(handler.get()).isSameAs(fsmHandler);
    }

    @Test
    void getHandler_whenModuleNull_returnsEmpty() {
        assertThat(registry.getHandler(null)).isEmpty();
    }

    @Test
    void getModuleName_whenHandlerPresent_returnsInternalName() {
        assertThat(registry.getModuleName("fsm")).isEqualTo("FSM");
    }

    @Test
    void isWorkflowTotalCountRequired_whenHandlerConfigured_returnsFalse() {
        assertThat(registry.isWorkflowTotalCountRequired("fsm")).isFalse();
    }
}
