package org.egov.egf.master.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.egov.common.domain.model.Pagination;
import org.egov.common.utils.RequestJsonReader;
import org.egov.egf.master.TestConfiguration;
import org.egov.egf.master.domain.model.Scheme;
import org.egov.egf.master.domain.model.SchemeSearch;
import org.egov.egf.master.domain.service.SchemeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BindingResult;

@ExtendWith(SpringExtension.class)
@WebMvcTest(Scheme.class)
@Import(TestConfiguration.class)
class SchemeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SchemeService schemeService;

    private final RequestJsonReader resources = new RequestJsonReader();

    @Test
    void search_returnsOk() throws Exception {
        Pagination<Scheme> page = new Pagination<>();
        when(schemeService.search(any(SchemeSearch.class), any(BindingResult.class))).thenReturn(page);

        mockMvc.perform(post("/schemes/_search?tenantId=default")
                .content(resources.getRequestInfo())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
