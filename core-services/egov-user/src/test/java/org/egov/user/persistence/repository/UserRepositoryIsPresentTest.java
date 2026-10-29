package org.egov.user.persistence.repository;

import org.egov.user.domain.model.enums.UserType;
import org.egov.user.repository.builder.UserTypeQueryBuilder;
import org.egov.user.repository.rowmapper.UserResultSetExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRepositoryIsPresentTest {

    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserTypeQueryBuilder userTypeQueryBuilder;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private UserResultSetExtractor userResultSetExtractor;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Mock
    private AuditRepository auditRepository;

    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository = new UserRepository(
                roleRepository,
                userTypeQueryBuilder,
                addressRepository,
                userResultSetExtractor,
                jdbcTemplate,
                namedParameterJdbcTemplate,
                auditRepository
        );
    }

    @Test
    void isUserPresent_countGreaterThanZero_returnsTrue() {
        when(userTypeQueryBuilder.getUserPresentByUserNameAndTenant()).thenReturn("SELECT count");
        when(namedParameterJdbcTemplate.queryForObject(anyString(), any(Map.class), eq(Integer.class))).thenReturn(1);

        assertThat(userRepository.isUserPresent("alice", "pb", UserType.CITIZEN)).isTrue();
    }

    @Test
    void isUserPresent_zeroCount_returnsFalse() {
        when(userTypeQueryBuilder.getUserPresentByUserNameAndTenant()).thenReturn("SELECT count");
        when(namedParameterJdbcTemplate.queryForObject(anyString(), any(Map.class), eq(Integer.class))).thenReturn(0);

        assertThat(userRepository.isUserPresent("bob", "pb", UserType.EMPLOYEE)).isFalse();
    }

    @Test
    void getUserIdByUuid_returnsIdFromDatabase() {
        when(userTypeQueryBuilder.getUserIdByUuid()).thenReturn("SELECT id FROM eg_user WHERE uuid=:uuid");
        when(namedParameterJdbcTemplate.queryForObject(anyString(), any(org.springframework.jdbc.core.namedparam.SqlParameterSource.class), eq(Long.class)))
                .thenReturn(42L);

        assertThat(userRepository.getUserIdByUuid("uuid-1")).isEqualTo(42L);
    }
}
