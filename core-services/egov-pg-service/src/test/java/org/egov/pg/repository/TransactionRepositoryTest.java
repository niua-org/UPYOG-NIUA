package org.egov.pg.repository;

import org.egov.pg.models.Transaction;
import org.egov.pg.web.models.TransactionCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private TransactionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new TransactionRepository(jdbcTemplate);
    }

    @Test
    void fetchTransactions_runsJdbcQuery() {
        TransactionCriteria criteria = TransactionCriteria.builder().tenantId("pb").build();
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                .thenReturn(Collections.emptyList());

        List<Transaction> result = repository.fetchTransactions(criteria);

        assertThat(result).isEmpty();
        verify(jdbcTemplate).query(anyString(), any(Object[].class), any(RowMapper.class));
    }

    @Test
    void fetchTransactionsByTimeRange_runsJdbcQuery() {
        TransactionCriteria criteria = TransactionCriteria.builder().tenantId("pb").build();
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(RowMapper.class)))
                .thenReturn(List.of(Transaction.builder().tenantId("pb").build()));

        List<Transaction> result = repository.fetchTransactionsByTimeRange(criteria, 1L, 2L);

        assertThat(result).hasSize(1);
    }
}
