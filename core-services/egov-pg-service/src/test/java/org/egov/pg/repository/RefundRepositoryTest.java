package org.egov.pg.repository;

import org.egov.pg.models.Refund;
import org.egov.pg.web.models.RefundCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private RefundRepository repository;

    @BeforeEach
    void setUp() {
        repository = new RefundRepository(jdbcTemplate);
    }

    @Test
    void fetchRefundTransactions_returnsRows() {
        RefundCriteria criteria = RefundCriteria.builder().tenantId("pb").build();
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                .thenReturn(List.of(Refund.builder().id("r1").build()));

        assertThat(repository.fetchRefundTransactions(criteria)).hasSize(1);
    }

    @Test
    void fetchRefundTransactionsByTimeRange_returnsRows() {
        RefundCriteria criteria = RefundCriteria.builder().tenantId("pb").build();
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                .thenReturn(List.of());

        assertThat(repository.fetchRefundTransactionsByTimeRange(criteria, 0L, 1L)).isEmpty();
    }
}
