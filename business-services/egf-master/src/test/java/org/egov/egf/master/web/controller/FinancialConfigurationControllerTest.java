package org.egov.egf.master.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.egov.common.domain.model.Pagination;
import org.egov.common.utils.RequestJsonReader;
import org.egov.egf.master.TestConfiguration;
import org.egov.egf.master.domain.model.FinancialConfiguration;
import org.egov.egf.master.domain.model.FinancialConfigurationSearch;
import org.egov.egf.master.domain.service.FinancialConfigurationService;
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
@WebMvcTest(FinancialConfiguration.class)
@Import(TestConfiguration.class)
class FinancialConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FinancialConfigurationService financialConfigurationService;

    private final RequestJsonReader resources = new RequestJsonReader();

    @Test
    void search_returnsOk() throws Exception {
        Pagination<FinancialConfiguration> page = new Pagination<>();
        when(financialConfigurationService.search(any(FinancialConfigurationSearch.class), any(BindingResult.class))).thenReturn(page);

        mockMvc.perform(post("/financialconfigurations/_search?tenantId=default")
                .content(resources.getRequestInfo())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
