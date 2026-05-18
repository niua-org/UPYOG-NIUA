package org.egov.inbox.service.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * ModuleHandlerRegistry — auto-collects every ModuleInboxHandler
 * bean from Spring context at startup.
 *
 * InboxOrchestrator calls hasHandler(moduleName) and getHandler(moduleName)
 * — no if-else chains needed anywhere.
 *
 * To register a NEW module handler:
 *   1. Create a class implementing ModuleInboxHandler.
 *   2. Annotate with @Service.
 *   3. Done — registry picks it up automatically.
 */
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

    /**
     * Find the handler that supports the given module name.
     *
     * @param moduleName  module name (e.g. "TL", "BPA", "PT")
     * @return Optional containing the matching handler, or empty if none found
     */
    public Optional<ModuleInboxHandler> getHandler(String moduleName) {
        if (moduleName == null) return Optional.empty();
        return handlers.stream().filter(h -> h.supports(moduleName)).findFirst();
    }

    public boolean hasHandler(String moduleName) {
        return getHandler(moduleName).isPresent();
    }

    /**
     * If a handler supports the given moduleName and provides a remapped
     * internal name (e.g. bsWs-service → WS), returns the remapped name.
     * Otherwise returns the original moduleName unchanged.
     */
    public String resolveModuleName(String moduleName) {
        if (moduleName == null) return null;
        return handlers.stream()
                .filter(h -> h.supports(moduleName))
                .findFirst()
                .map(h -> h.resolveInternalModuleName(moduleName))
                .orElse(moduleName);
    }

    public boolean workflowTotalCount(String moduleName) {
        return getHandler(moduleName).map(ModuleInboxHandler::workflowTotalCount).orElse(true);
    }

    public boolean workflowNearingSlaCount(String moduleName) {
        return getHandler(moduleName).map(ModuleInboxHandler::workflowNearingSlaCount).orElse(true);
    }
}