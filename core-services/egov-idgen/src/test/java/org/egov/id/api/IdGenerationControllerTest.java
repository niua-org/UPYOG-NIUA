package org.egov.id.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.id.model.IdGenerationRequest;
import org.egov.id.model.IdGenerationResponse;
import org.egov.id.model.IdResponse;
import org.egov.id.model.RequestInfo;
import org.egov.id.service.IdGenerationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {IdGenerationController.class})
@ExtendWith(SpringExtension.class)
class IdGenerationControllerTest {

    @Autowired
    private IdGenerationController idGenerationController;

    @MockBean
    private IdGenerationService idGenerationService;

    @Test
    void generateIdResponse_delegatesToServiceAndReturnsBody() throws Exception {
        IdGenerationResponse serviceResponse = new IdGenerationResponse();
        IdResponse idResponse = new IdResponse();
        idResponse.setId("0001");
        serviceResponse.setIdResponses(List.of(idResponse));
        when(idGenerationService.generateIdResponse(any(IdGenerationRequest.class))).thenReturn(serviceResponse);

        IdGenerationRequest idGenerationRequest = new IdGenerationRequest();
        idGenerationRequest.setIdRequests(List.of());
        idGenerationRequest.setRequestInfo(new RequestInfo());
        String content = new ObjectMapper().writeValueAsString(idGenerationRequest);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(idGenerationController).build();

        mockMvc.perform(post("/id/_generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idResponses[0].id").value("0001"));

        verify(idGenerationService).generateIdResponse(any(IdGenerationRequest.class));
    }
}
