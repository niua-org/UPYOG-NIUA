package org.egov.infra.mdms.repository.impl;

import lombok.extern.slf4j.Slf4j;
import org.egov.infra.mdms.config.ApplicationConfig;
import org.egov.infra.mdms.model.ThemeConfig;
import org.egov.infra.mdms.producer.Producer;
import org.egov.infra.mdms.repository.ThemeConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;


/**
 * Repository implementation for Theme Configuration.
 *
 * All persistence operations are handled asynchronously
 * through Persister service using Kafka events.
 */
@Repository
@Slf4j
public class ThemeConfigRepositoryImpl implements ThemeConfigRepository {


    private final Producer producer;

    private final ApplicationConfig applicationConfig;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;


    @Autowired
    public ThemeConfigRepositoryImpl(
            Producer producer,
            ApplicationConfig applicationConfig,
            JdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper) {

        this.producer = producer;
        this.applicationConfig = applicationConfig;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }


    /**
     * Publishes theme configuration create request.
     *
     * Persister will store data into ug_theme_config table.
     *
     * This method is used for:
     * - Default theme creation
     * - New pending theme creation during update flow
     *
     * @param themeConfig theme configuration details
     */
    @Override
    public void create(ThemeConfig themeConfig) {

        log.info(
                "Publishing save theme config event: {}",
                themeConfig
        );

        producer.push(
                applicationConfig.getSaveThemeConfigTopicName(),
                themeConfig
        );
    }


    /**
     * Publishes theme configuration update request.
     *
     * Used after workflow approval or rejection
     * to update status and workflow details.
     *
     * @param themeConfig updated theme configuration
     */
    @Override
    public void update(ThemeConfig themeConfig) {

        log.info(
                "Publishing update theme config event: {}",
                themeConfig
        );

        producer.push(
                applicationConfig.getUpdateThemeConfigStatusTopicName(),
                themeConfig
        );
    }
    @Override
    public void createStaging(ThemeConfig themeConfig) {

        log.info(
                "Publishing save staging theme config event: {}",
                themeConfig
        );

        producer.push(
                applicationConfig.getSaveThemeConfigStagingTopicName(),
                themeConfig
        );
    }


    // Checks duplicate pending modification before creating workflow
    @Override
    public boolean existsPendingTheme(String tenantId, String themeType) {

        String query = "SELECT COUNT(*) FROM ug_theme_config WHERE tenantid=? AND themetype=? AND status='PENDING'";

        Integer count = jdbcTemplate.queryForObject(
                query,
                Integer.class,
                tenantId,
                themeType
        );

        return count != null && count > 0;
    }

    @Override
public boolean existsTheme(String tenantId, String themeType) {

    String query =
            "SELECT COUNT(*) FROM ug_theme_config " +
            "WHERE tenantid=? AND themetype=?";

    Integer count = jdbcTemplate.queryForObject(
            query,
            Integer.class,
            tenantId,
            themeType
    );

    return count != null && count > 0;
}


@Override
public void deactivateAllThemes(
        String tenantId,
        String themeType) {

    String query =
            "UPDATE ug_theme_config " +
            "SET isactive=false " +
            "WHERE tenantid=? " +
            "AND themetype=? " +
            "AND isactive=true";

    jdbcTemplate.update(
            query,
            tenantId,
            themeType
    );
}

        @Override
public boolean existsThemeName(String tenantId, String themeType, String themeName) {

    String query =
            "SELECT COUNT(*) FROM ug_theme_config " +
            "WHERE tenantid=? " +
            "AND themetype=? " +
            "AND LOWER(themename)=LOWER(?)";

    Integer count = jdbcTemplate.queryForObject(
            query,
            Integer.class,
            tenantId,
            themeType,
            themeName
    );

    return count != null && count > 0;
}


@Override
public boolean existsActiveTheme(String tenantId, String themeType) {

    String query =
            "SELECT COUNT(*) FROM ug_theme_config " +
            "WHERE tenantid=? " +
            "AND themetype=? " +
            "AND isactive=true";

    Integer count = jdbcTemplate.queryForObject(
            query,
            Integer.class,
            tenantId,
            themeType
    );

    return count != null && count > 0;
}


@Override
public void activateOldestTheme(
        String tenantId,
        String themeType) {

    String query =
            "UPDATE ug_theme_config " +
            "SET isactive=true " +
            "WHERE id = (" +
            "SELECT id FROM ug_theme_config " +
            "WHERE tenantid=? " +
            "AND themetype=? " +
            "AND status IN ('DEFAULT','APPROVED') " +
            "ORDER BY createdtime ASC " +
            "LIMIT 1" +
            ")";

    jdbcTemplate.update(
            query,
            tenantId,
            themeType
    );
}


@Override
public void setDefaultTheme(
        String id,
        String tenantId,
        String themeType) {

    deactivateAllThemes(tenantId, themeType);

    String query =
            "UPDATE ug_theme_config " +
            "SET isactive=true " +
            "WHERE id=? " +
            "AND tenantid=? " +
            "AND themetype=? " +
            "AND status IN ('DEFAULT','APPROVED')";

    jdbcTemplate.update(
            query,
            id,
            tenantId,
            themeType
    );
}

    @Override
public List<ThemeConfig> search(String tenantId, String themeType, Boolean isActive) {

        String query =
        "SELECT * FROM ug_theme_config " +
        "WHERE tenantid=? " +
        "AND themetype=? " +
        (Boolean.TRUE.equals(isActive) ? "AND isactive=true " : "") +
        "ORDER BY createdtime ASC";

        return jdbcTemplate.query(
                query,
                (rs, rowNum) -> {

                    ThemeConfig theme = new ThemeConfig();

                    theme.setId(rs.getString("id"));
                    theme.setTenantId(rs.getString("tenantid"));
                    theme.setThemeType(rs.getString("themetype"));
                    theme.setThemeName(rs.getString("themename"));
                    theme.setStatus(rs.getString("status"));
                    theme.setWorkflowId(rs.getString("workflowid"));
                    theme.setIsActive(rs.getBoolean("isactive"));
                    theme.setCreatedBy(rs.getString("createdby"));
                    theme.setCreatedTime(rs.getLong("createdtime"));
                    theme.setLastModifiedBy(rs.getString("lastmodifiedby"));
                    theme.setLastModifiedTime(rs.getLong("lastmodifiedtime"));

                    try {
                        theme.setConfig(
                                objectMapper.readValue(
                                        rs.getString("config"),
                                        Map.class
                                )
                        );
                    } catch (Exception e) {
                        throw new RuntimeException(
                                "Unable to parse theme config JSON",
                                e
                        );
                    }

                    return theme;
                },
                tenantId,
                themeType
        );
    }

}
