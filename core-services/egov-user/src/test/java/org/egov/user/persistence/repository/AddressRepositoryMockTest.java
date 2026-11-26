package org.egov.user.persistence.repository;

import org.egov.user.domain.model.Address;
import org.egov.user.domain.model.enums.AddressType;
import org.egov.user.repository.builder.AddressQueryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressRepositoryMockTest {

    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private AddressQueryBuilder addressQueryBuilder;

    private AddressRepository repository;

    @BeforeEach
    void setUp() {
        repository = new AddressRepository(namedParameterJdbcTemplate, jdbcTemplate, addressQueryBuilder);
    }

    @Test
    void create_assignsSequenceAndPersists() {
        Address address = Address.builder()
                .type(AddressType.PERMANENT)
                .address("Street 1")
                .city("City")
                .pinCode("123456")
                .userId(99L)
                .build();
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(1000L);

        Address created = repository.create(address, 1L, "pb");

        verify(namedParameterJdbcTemplate).update(eq(AddressRepository.INSERT_ADDRESS_BYUSERID), any(Map.class));
        assertThat(created).isSameAs(address);
    }

    @Test
    void find_delegatesQuery() {
        when(namedParameterJdbcTemplate.query(anyString(), any(Map.class), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(Collections.emptyList());

        List<Address> addresses = repository.find(1L, "pb");

        assertThat(addresses).isEmpty();
    }
}
