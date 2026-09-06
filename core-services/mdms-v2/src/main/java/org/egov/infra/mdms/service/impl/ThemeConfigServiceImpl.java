package org.egov.infra.mdms.service.impl;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.egov.common.contract.request.RequestInfo;
import org.egov.infra.mdms.model.ThemeConfig;
import org.egov.infra.mdms.repository.ThemeConfigRepository;
import org.egov.infra.mdms.service.ThemeConfigService;
import org.egov.infra.mdms.service.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of ThemeConfigService.
 *
 * Handles theme configuration creation,
 * update and workflow status management.
 */
@Slf4j
@RequiredArgsConstructor    
@Service
public class ThemeConfigServiceImpl implements ThemeConfigService {

    private final ThemeConfigRepository themeConfigRepository;
    private final WorkflowService workflowService;


    /**
     * Creates theme configuration.
     *
     * @param themeConfig theme configuration details
     * @return created theme configuration
     */
    @Override
public ThemeConfig create(
        ThemeConfig themeConfig,
        RequestInfo requestInfo) {

    log.info(
            "Creating theme configuration for tenantId: {} and themeType: {}",
            themeConfig.getTenantId(),
            themeConfig.getThemeType()
    );

    if (themeConfig.getId() == null) {
        themeConfig.setId(UUID.randomUUID().toString());
    }

    long currentTime = System.currentTimeMillis();

    if (themeConfig.getCreatedTime() == null) {
        themeConfig.setCreatedTime(currentTime);
    }

    if (themeConfig.getLastModifiedTime() == null) {
        themeConfig.setLastModifiedTime(currentTime);
    }

    boolean hasThemes = themeConfigRepository.existsTheme(
            themeConfig.getTenantId(),
            themeConfig.getThemeType()
    );

    boolean hasActiveTheme = themeConfigRepository.existsActiveTheme(
            themeConfig.getTenantId(),
            themeConfig.getThemeType()
    );

    boolean makeDefault =
            !hasThemes ||
            Boolean.TRUE.equals(themeConfig.getSetAsDefault());

    if (!hasThemes) {
        themeConfig.setThemeName("System Default");
    } else {
        if (themeConfig.getThemeName() == null ||
                themeConfig.getThemeName().trim().isEmpty()) {
            throw new RuntimeException("Theme name is mandatory");
        }

        if (themeConfigRepository.existsThemeName(
                themeConfig.getTenantId(),
                themeConfig.getThemeType(),
                themeConfig.getThemeName())) {
            throw new RuntimeException("Theme name already exists");
        }
    }

    if ("System Default".equalsIgnoreCase(themeConfig.getThemeName()) && hasThemes) {
        throw new RuntimeException("Theme name 'System Default' is reserved");
    }

    if (makeDefault) {
        themeConfigRepository.deactivateAllThemes(
                themeConfig.getTenantId(),
                themeConfig.getThemeType()
        );

        themeConfig.setIsActive(true);
    } else {
        themeConfig.setIsActive(false);

        if (!hasActiveTheme) {
            themeConfigRepository.activateOldestTheme(
                    themeConfig.getTenantId(),
                    themeConfig.getThemeType()
            );
        }
    }

    if (!hasThemes) {
        themeConfig.setStatus("DEFAULT");
    } else {
        themeConfig.setStatus("APPROVED");
    }

    log.info("THEME CONFIG BEFORE PUBLISH: {}", themeConfig);
        themeConfigRepository.create(themeConfig);

    return themeConfig;
}


    /**
     * Creates updated theme configuration with pending status.
     *
     * A new configuration row is created for workflow approval.
     * Existing approved configuration remains unchanged.
     *
     * @param themeConfig updated theme configuration
     * @param requestInfo request information
     * @return pending theme configuration
     */
    @Override
    public ThemeConfig update(
            ThemeConfig themeConfig,
            RequestInfo requestInfo) {

        log.info(
                "Creating pending theme configuration for tenantId: {} and themeType: {}",
                themeConfig.getTenantId(),
                themeConfig.getThemeType()
        );

        // Theme name is mandatory for update.
        if (themeConfig.getThemeName() == null ||
                themeConfig.getThemeName().trim().isEmpty()) {

            throw new RuntimeException(
                    "Theme name is mandatory for update"
            );
        }

        if ("System Default".equalsIgnoreCase(themeConfig.getThemeName())) {
            throw new RuntimeException("Theme name 'System Default' is reserved");
        }

        if (themeConfigRepository.existsThemeName(
                themeConfig.getTenantId(),
                themeConfig.getThemeType(),
                themeConfig.getThemeName())) {
            throw new RuntimeException("Theme name already exists");
        }

        // Prevent duplicate pending modification requests.
        if (themeConfigRepository.existsPendingTheme(
                themeConfig.getTenantId(),
                themeConfig.getThemeType())) {

            throw new RuntimeException(
                    "Modification already sent to the Admin for verification"
            );
        }

        // Always create a NEW row for update.
        themeConfig.setId(UUID.randomUUID().toString());

        long currentTime = System.currentTimeMillis();

        themeConfig.setCreatedTime(currentTime);
        themeConfig.setLastModifiedTime(currentTime);

        // Updated theme must wait for workflow approval.
        themeConfig.setStatus("PENDING");

        // Updated theme must never become default automatically.
        themeConfig.setIsActive(false);

        log.info("THEME CONFIG BEFORE WORKFLOW : {}", themeConfig);

        String workflowId = workflowService.createWorkflow(
                themeConfig,
                requestInfo
        );

        themeConfig.setWorkflowId(workflowId);

        log.info("THEME CONFIG AFTER WORKFLOW : {}", themeConfig);

        // Store pending configuration as a new row.
        themeConfigRepository.createStaging(themeConfig);

        return themeConfig;
    }

    /**
     * Updates theme configuration workflow status.
     *
     * @param themeConfig theme configuration
     * @param action workflow action
     * @param requestInfo request information
     * @return updated theme configuration
     */
    @Override
    public ThemeConfig updateWorkflowStatus(
            ThemeConfig themeConfig,
            String action,
            RequestInfo requestInfo) {

        if ("SET_DEFAULT".equals(action)) {

            if (themeConfig.getId() == null) {
                throw new RuntimeException(
                        "Theme id is mandatory for setting default theme"
                );
            }

            themeConfigRepository.setDefaultTheme(
                    themeConfig.getId(),
                    themeConfig.getTenantId(),
                    themeConfig.getThemeType()
            );

            themeConfig.setIsActive(true);

            return themeConfig;
        }

        if ("APPROVE".equals(action)) {
            themeConfig.setStatus("APPROVED");
            themeConfig.setIsActive(false);
        } else if ("REJECT".equals(action)) {
            themeConfig.setStatus("REJECTED");
            themeConfig.setIsActive(false);
        }

        themeConfig.setLastModifiedTime(System.currentTimeMillis());

        themeConfig.setLastModifiedBy(
                requestInfo.getUserInfo().getUuid()
        );

        String workflowId = workflowService.transitionWorkflow(
                themeConfig,
                requestInfo,
                action
        );

        themeConfig.setWorkflowId(workflowId);

        themeConfigRepository.update(themeConfig);

        return themeConfig;
    }


    /**
     * Fetch theme configuration.
     *
     * @param tenantId tenant identifier
     * @param themeType theme type
     * @return theme configuration
     */
    @Override
public List<ThemeConfig> search(String tenantId, String themeType, Boolean isActive) {
    return themeConfigRepository.search(tenantId, themeType, isActive);
}
}
