package org.egov.inbox.service.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class ModuleHandlerRegistry {

    private final List<ModuleInboxHandler> handlers;

    @Autowired
    public ModuleHandlerRegistry(List<ModuleInboxHandler> handlers) {
        this.handlers = handlers;
        log.info("ModuleHandlerRegistry initialized with {} handlers: {}",
                handlers.size(),
                handlers.stream()
                        .map(h -> h.getClass().getSimpleName())
                        .toList());
    }

    public Optional<ModuleInboxHandler> getHandler(String moduleName) {
        if (moduleName == null)
            return Optional.empty();
        return handlers.stream().filter(h -> h.supports(moduleName)).findFirst();
    }

    public boolean hasHandler(String moduleName) {
        return getHandler(moduleName).isPresent();
    }

    public String getModuleName(String moduleName) {

        if (moduleName == null)
            return null;

        return handlers.stream()
                .filter(h -> h.supports(moduleName))
                .findFirst()
                .map(h -> h.getInternalModuleName(moduleName))
                .orElse(moduleName);
    }

    public boolean isWorkflowTotalCountRequired(String moduleName) {

        return getHandler(moduleName)
                .map(ModuleInboxHandler::isWorkflowTotalCountRequired)
                .orElse(true);
    }

    public boolean isWorkflowNearingSlaCountRequired(String moduleName) {

        return getHandler(moduleName)
                .map(ModuleInboxHandler::isWorkflowNearingSlaCountRequired)
                .orElse(true);
    }
}