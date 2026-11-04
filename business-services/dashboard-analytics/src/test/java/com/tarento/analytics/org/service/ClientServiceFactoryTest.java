package com.tarento.analytics.org.service;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tarento.analytics.ConfigurationLoader;
import com.tarento.analytics.constant.Constants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceFactoryTest {

    @Mock
    private TarentoServiceImpl tarentoServiceImpl;
    @Mock
    private MdmsServiceImpl mdmsServiceImpl;
    @Mock
    private ConfigurationLoader configurationLoader;

    @InjectMocks
    private ClientServiceFactory factory;

    @Test
    void getInstance_defaultClient_returnsTarentoService() {
        assertThat(factory.getInstance(Constants.ClienServiceType.DEFAULT_CLIENT)).isSameAs(tarentoServiceImpl);
    }

    @Test
    void getInstance_mdmsClient_returnsMdmsService() {
        assertThat(factory.getInstance(Constants.ClienServiceType.MDMS_CLIENT)).isSameAs(mdmsServiceImpl);
    }

    @Test
    void getInstance_unknownType_throwsRuntimeException() {
        assertThatThrownBy(() -> factory.getInstance(null))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void get_mdmsEnabledChart_returnsMdmsService() {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        ObjectNode chart = JsonNodeFactory.instance.objectNode();
        chart.put(Constants.JsonPaths.IS_MDMS_ENABALED, true);
        root.set("chart-1", chart);
        when(configurationLoader.get(Constants.ConfigurationFiles.CHART_API_CONFIG)).thenReturn(root);

        assertThat(factory.get("chart-1")).isSameAs(mdmsServiceImpl);
    }

    @Test
    void get_mdmsDisabledChart_returnsTarentoService() {
        ObjectNode root = JsonNodeFactory.instance.objectNode();
        ObjectNode chart = JsonNodeFactory.instance.objectNode();
        root.set("chart-2", chart);
        when(configurationLoader.get(Constants.ConfigurationFiles.CHART_API_CONFIG)).thenReturn(root);

        assertThat(factory.get("chart-2")).isSameAs(tarentoServiceImpl);
    }
}
