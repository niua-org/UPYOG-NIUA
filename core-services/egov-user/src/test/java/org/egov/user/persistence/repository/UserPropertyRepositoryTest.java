package org.egov.user.persistence.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.user.domain.model.asset.UserProperty;
import org.egov.user.repository.builder.UserPropertyQueryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests verifying persistence and retrieval operations in {@link UserPropertyRepository}.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class UserPropertyRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private UserPropertyQueryBuilder queryBuilder;
    private ObjectMapper objectMapper;
    private UserPropertyRepository propertyRepository;

    @BeforeEach
    void setUp() {
        queryBuilder = new UserPropertyQueryBuilder();
        objectMapper = new ObjectMapper();
        propertyRepository = new UserPropertyRepository(namedParameterJdbcTemplate, objectMapper, queryBuilder);
    }

    @Test
    @DisplayName("findPropertiesByUserUuid should return empty list when userUuid is blank")
    void testFindPropertiesByUserUuid_BlankUserUuid() {
        List<UserProperty> results = propertyRepository.findPropertiesByUserUuid("   ", true);
        assertNotNull(results);
        assertTrue(results.isEmpty());

        results = propertyRepository.findPropertiesByUserUuid(null);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
}
