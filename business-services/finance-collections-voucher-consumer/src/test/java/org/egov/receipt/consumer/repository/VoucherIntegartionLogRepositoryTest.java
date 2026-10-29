package org.egov.receipt.consumer.repository;

import org.egov.receipt.consumer.entity.VoucherIntegrationLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VoucherIntegartionLogRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @InjectMocks
    private VoucherIntegartionLogRepository repository;

    @Test
    void saveVoucherIntegrationLog_success() {
        VoucherIntegrationLog log = new VoucherIntegrationLog();
        log.setStatus("SUCCESS");

        repository.saveVoucherIntegrationLog(log);

        verify(namedParameterJdbcTemplate).update(anyString(), any(SqlParameterSource.class));
    }

    @Test
    void saveVoucherIntegrationLog_failureIsSwallowed() {
        VoucherIntegrationLog log = new VoucherIntegrationLog();
        doThrow(new RuntimeException("db down")).when(namedParameterJdbcTemplate)
                .update(anyString(), any(SqlParameterSource.class));

        repository.saveVoucherIntegrationLog(log);

        verify(namedParameterJdbcTemplate).update(anyString(), any(SqlParameterSource.class));
    }
}
