package org.egov.demand.repository.querybuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;

import org.egov.demand.amendment.model.AmendmentCriteria;
import org.egov.demand.amendment.model.enums.AmendmentStatus;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ContextConfiguration(classes = {AmendmentQueryBuilder.class})
@TestPropertySource(properties = "amendment.default.limit=100")
@ExtendWith(SpringExtension.class)
class AmendmentQueryBuilderTest {
    @Autowired
    private AmendmentQueryBuilder amendmentQueryBuilder;


    @Test
    void testGetSearchQuery() {
        AmendmentCriteria amendmentCriteria = new AmendmentCriteria();
        MapSqlParameterSource mapSqlParameterSource = mock(MapSqlParameterSource.class);
        when(mapSqlParameterSource.addValue((String) any(), (Object) any())).thenReturn(mock(MapSqlParameterSource.class));
        String expected = AmendmentQueryBuilder.AMENDMENT_SEARCH_QUERY
                .replace(AmendmentQueryBuilder.AMENDMENT_SEARCH_QUERY_WHERE, " amendment.tenantid = :tenantid ")
                .replace(AmendmentQueryBuilder.AMENDMENT_SEARCH_QUERY_PAGINATION, " OFFSET :offset LIMIT :limit ");
        assertEquals(expected, this.amendmentQueryBuilder.getSearchQuery(amendmentCriteria, mapSqlParameterSource));
        verify(mapSqlParameterSource, atLeast(1)).addValue((String) any(), (Object) any());
    }

}

