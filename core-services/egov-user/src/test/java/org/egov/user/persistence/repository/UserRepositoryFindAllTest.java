package org.egov.user.persistence.repository;

import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.repository.builder.UserTypeQueryBuilder;
import org.egov.user.repository.rowmapper.UserResultSetExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRepositoryFindAllTest {

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
    private org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate namedParameterJdbcTemplate;
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
    void findAll_withoutRoleCodes_queriesJdbcTemplate() {
        UserSearchCriteria criteria = UserSearchCriteria.builder().tenantId("pb").build();
        when(userTypeQueryBuilder.getQuery(any(), any())).thenReturn("SELECT users");
        when(jdbcTemplate.query(anyString(), any(Object[].class), any(UserResultSetExtractor.class)))
                .thenReturn(List.of(User.builder().id(1L).build()));

        List<User> users = userRepository.findAll(criteria);

        assertThat(users).hasSize(1);
    }

    @Test
    void findAll_roleSearchWithNoMatchingUsers_returnsEmpty() {
        UserSearchCriteria criteria = UserSearchCriteria.builder()
                .tenantId("pb")
                .roleCodes(Collections.singletonList("COLL_RECEIPT_CREATOR"))
                .build();
        when(userTypeQueryBuilder.getQueryUserRoleSearch(any(), any())).thenReturn("SELECT role users");
        when(jdbcTemplate.queryForList(anyString(), any(Object[].class), eq(Long.class)))
                .thenReturn(Collections.emptyList());

        assertThat(userRepository.findAll(criteria)).isEmpty();
    }
}
