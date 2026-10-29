package org.egov.user.persistence.repository;

import org.egov.user.domain.model.User;
import org.egov.user.domain.model.enums.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Mock
    private JdbcTemplate jdbcTemplate;

    private AuditRepository auditRepository;

    @BeforeEach
    void setUp() {
        auditRepository = new AuditRepository(namedParameterJdbcTemplate, jdbcTemplate);
    }

    @Test
    void auditUser_persistsSnapshot() {
        User user = User.builder()
                .id(1L)
                .uuid("uuid-1")
                .username("alice")
                .type(UserType.CITIZEN)
                .tenantId("pb")
                .active(true)
                .build();

        auditRepository.auditUser(user, 1L, "uuid-1");

        verify(namedParameterJdbcTemplate).update(eq(AuditRepository.INSERT_AUDIT_DETAILS), any(Map.class));
    }
}
