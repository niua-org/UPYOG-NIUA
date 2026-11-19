package com.tarento.analytics.controllers;

import com.tarento.analytics.org.service.ClientServiceFactory;
import com.tarento.analytics.service.AmazonS3ClientService;
import com.tarento.analytics.service.MetadataService;
import com.tarento.analytics.utils.PathRoutes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MetadataService metadataService;

    @MockBean
    private AmazonS3ClientService amazonS3ClientService;

    @MockBean
    private ClientServiceFactory clientServiceFactory;

    @Test
    void getTest_returnsSuccessPayload() throws Exception {
        mockMvc.perform(get(PathRoutes.DashboardApi.DASHBOARD_ROOT_PATH + PathRoutes.DashboardApi.TEST_PATH))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("success")));
    }
}
