package org.upyog.reconciliation.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.upyog.reconciliation.service.ReconciliationSchedulerService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TestReconciliationController.class)
class TestReconciliationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReconciliationSchedulerService reconciliationSchedulerService;

    @Test
    void executeExtractionForDate_withoutDate_triggersService() throws Exception {
        mockMvc.perform(post("/test/reconciliation/execute"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("triggered successfully")));

        verify(reconciliationSchedulerService).executeExtractionForDate(any());
    }

    @Test
    void executeExtractionForDate_withDate_passesDate() throws Exception {
        mockMvc.perform(post("/test/reconciliation/execute").param("date", "2024-06-01"))
                .andExpect(status().isOk());

        verify(reconciliationSchedulerService).executeExtractionForDate(java.time.LocalDate.parse("2024-06-01"));
    }
}
