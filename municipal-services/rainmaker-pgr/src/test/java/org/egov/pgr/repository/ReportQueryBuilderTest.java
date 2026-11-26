package org.egov.pgr.repository;

import org.egov.pgr.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportQueryBuilderTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ReportQueryBuilder reportQueryBuilder;

    @Test
    void getCreateViewQuery_substitutesSlaHours() {
        when(notificationService.getSlaHours()).thenReturn(48L);

        String sql = reportQueryBuilder.getCreateViewQuery();

        assertThat(sql).contains("48");
        assertThat(sql).contains("slaservicerequestidview");
    }

    @Test
    void getDropViewQuery_dropsView() {
        assertThat(reportQueryBuilder.getDropViewQuery()).contains("DROP VIEW");
    }
}
