package org.egov.egf.master.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.egov.common.domain.model.Pagination;
import org.egov.common.utils.RequestJsonReader;
import org.egov.egf.master.TestConfiguration;
import org.egov.egf.master.domain.model.Supplier;
import org.egov.egf.master.domain.model.SupplierSearch;
import org.egov.egf.master.domain.service.SupplierService;
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
@WebMvcTest(Supplier.class)
@Import(TestConfiguration.class)
class SupplierTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SupplierService supplierService;

    private final RequestJsonReader resources = new RequestJsonReader();

    @Test
    void search_returnsOk() throws Exception {
        Pagination<Supplier> page = new Pagination<>();
        when(supplierService.search(any(SupplierSearch.class), any(BindingResult.class))).thenReturn(page);

        mockMvc.perform(post("/suppliers/_search?tenantId=default")
                .content(resources.getRequestInfo())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
