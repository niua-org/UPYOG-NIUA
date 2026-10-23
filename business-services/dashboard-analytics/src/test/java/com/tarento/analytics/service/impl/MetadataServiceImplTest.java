package com.tarento.analytics.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tarento.analytics.ConfigurationLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetadataServiceImplTest {

    @Mock
    private ConfigurationLoader configurationLoader;

    @InjectMocks
    private MetadataServiceImpl metadataService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        org.springframework.test.util.ReflectionTestUtils.setField(metadataService, "objectMapper", objectMapper);
        ObjectNode master = (ObjectNode) objectMapper.readTree(getClass().getResourceAsStream("/schema/MasterDashboardConfig.json"));
        ObjectNode roles = (ObjectNode) objectMapper.readTree(getClass().getResourceAsStream("/schema/RoleDashboardMappingsConf.json"));

        when(configurationLoader.get(ConfigurationLoader.MASTER_DASHBOARD_CONFIG)).thenReturn(master);
        when(configurationLoader.get(ConfigurationLoader.ROLE_DASHBOARD_CONFIG)).thenReturn(roles);
    }

    @Test
    void getDashboardConfiguration_filtersByDashboardId() throws Exception {
        var dashboards = metadataService.getDashboardConfiguration("home", null, Collections.emptyList());

        assertThat(dashboards).isNotEmpty();
        assertThat(dashboards.get(0).get("id").asText()).isEqualTo("home");
    }
}
