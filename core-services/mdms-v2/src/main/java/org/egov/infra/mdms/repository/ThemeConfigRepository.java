package org.egov.infra.mdms.repository;

import java.util.List;

import org.egov.infra.mdms.model.ThemeConfig;

public interface ThemeConfigRepository {

    void create(ThemeConfig themeConfig);

    void createStaging(ThemeConfig themeConfig);

    void update(ThemeConfig themeConfig);

    List<ThemeConfig> search(String tenantId, String themeType, Boolean isActive);
    
    // Checks if employee already has a pending modification request
    boolean existsPendingTheme(String tenantId, String themeType);

    boolean existsTheme(String tenantId, String themeType);

    boolean existsThemeName(String tenantId, String themeType, String themeName);


    void deactivateAllThemes(String tenantId, String themeType);

    boolean existsActiveTheme(String tenantId, String themeType);

    void activateOldestTheme(String tenantId, String themeType);

    void setDefaultTheme(String id, String tenantId, String themeType);
}
