package org.egov.waterconnection.web.controller;

import org.egov.waterconnection.service.WaterEncryptionService;
import org.egov.waterconnection.service.WaterService;
import org.egov.waterconnection.util.ResponseInfoFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WaterController.class)
class WaterControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WaterService waterService;
    @MockBean
    private ResponseInfoFactory responseInfoFactory;
    @MockBean
    private WaterEncryptionService waterEncryptionService;

    @Test
    @DisplayName("searchPost_whenEmptyBody_returns4xx")
    void searchPost_whenEmptyBody_returns4xx() throws Exception {
        mockMvc.perform(post("/wc/_search").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().is4xxClientError());
    }
}
