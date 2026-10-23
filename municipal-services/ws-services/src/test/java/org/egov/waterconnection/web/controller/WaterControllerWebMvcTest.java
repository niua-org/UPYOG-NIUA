package org.egov.waterconnection.web.controller;

import org.egov.waterconnection.service.WaterEncryptionService;
import org.egov.waterconnection.service.WaterService;
import org.egov.waterconnection.util.ResponseInfoFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WaterControllerWebMvcTest {

    private MockMvc mockMvc;

    @Mock
    private WaterService waterService;
    @Mock
    private ResponseInfoFactory responseInfoFactory;
    @Mock
    private WaterEncryptionService waterEncryptionService;

    @BeforeEach
    void setUp() {
        WaterController controller = WaterController.builder()
                .waterService(waterService)
                .responseInfoFactory(responseInfoFactory)
                .waterEncryptionService(waterEncryptionService)
                .build();
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("searchPost_whenEmptyBody_returns2xx")
    void searchPost_whenEmptyBody_returns2xx() throws Exception {
        mockMvc.perform(post("/wc/_search").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().is2xxSuccessful());
    }
}
