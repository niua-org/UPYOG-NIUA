package org.egov.user.persistence.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Mock
    private RestTemplate restTemplate;

    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        roleRepository = new RoleRepository(namedParameterJdbcTemplate, restTemplate, new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @Test
    void getUserRoles_emptyJdbcResult_returnsEmptyList() {
        when(namedParameterJdbcTemplate.query(anyString(), any(Map.class), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(Collections.emptyList());

        assertThat(roleRepository.getUserRoles(1L, "pb")).isEmpty();
    }
}
