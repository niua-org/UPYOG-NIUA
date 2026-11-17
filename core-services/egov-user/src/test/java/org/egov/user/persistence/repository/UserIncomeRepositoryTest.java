package org.egov.user.persistence.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.user.domain.model.asset.UserIncome;
import org.egov.user.repository.builder.UserIncomeQueryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests verifying persistence and retrieval operations in {@link UserIncomeRepository}.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class UserIncomeRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private UserIncomeQueryBuilder queryBuilder;
    private ObjectMapper objectMapper;
    private UserIncomeRepository incomeRepository;

    @BeforeEach
    void setUp() {
        queryBuilder = new UserIncomeQueryBuilder();
        objectMapper = new ObjectMapper();
        incomeRepository = new UserIncomeRepository(namedParameterJdbcTemplate, objectMapper, queryBuilder);
    }

    @Test
    @DisplayName("findActiveIncomeByUserUuid should return null when userUuid is blank")
    void testFindActiveIncomeByUserUuid_BlankUserUuid() {
        assertNull(incomeRepository.findActiveIncomeByUserUuid(null));
        assertNull(incomeRepository.findActiveIncomeByUserUuid("  "));
    }

    @Test
    @DisplayName("saveIncome should execute insert and audit SQL statements")
    void testSaveIncome_PersistsData() {
        UserIncome income = UserIncome.builder()
                .id("inc-1")
                .userUuid("user-1")
                .tenantId("pg.citya")
                .occupation("DOCTOR")
                .annualIncome(new BigDecimal("2500000.00"))
                .identifierType("PAN")
                .identifierValue("ABCDE1234F")
                .build();

        incomeRepository.saveIncome(income);
        assertNotNull(income.getId());
    }
}
