package org.egov.pgr.service;

import org.egov.pgr.contract.ReportRequest;
import org.egov.pgr.contract.ReportResponse;
import org.egov.pgr.repository.ReportRepository;
import org.egov.pgr.utils.ReportUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private ReportRepository repository;
    @Mock private ReportUtils reportUtils;

    @InjectMocks
    private ReportService reportService;

    @Test
    void getReports_whenDbEmpty_returnsDefaultResponse() {
        ReportRequest request = new ReportRequest();
        ReportResponse defaultResponse = new ReportResponse();
        when(repository.getDataFromDb(request)).thenReturn(Collections.emptyList());
        when(reportUtils.getDefaultResponse(request)).thenReturn(defaultResponse);

        assertThat(reportService.getReports(request)).isSameAs(defaultResponse);
        verify(reportUtils).validateReportRequest(request);
        verify(repository).createOrDropViewDb(eq(request), eq(false));
    }
}
