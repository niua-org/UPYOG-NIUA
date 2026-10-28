package org.egov.ptr.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.egov.ptr.models.AuditDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CommonUtilsTest {

    @Spy
    private ObjectMapper mapper = new ObjectMapper();

    @InjectMocks
    private CommonUtils commonUtils;

    @BeforeEach
    void setUp() {
        // field injection for @Autowired fields not used in these tests
    }

    @Test
    void getAuditDetails_whenCreate_populatesCreatedAndModified() {
        AuditDetails audit = commonUtils.getAuditDetails("user-1", true);

        assertThat(audit.getCreatedBy()).isEqualTo("user-1");
        assertThat(audit.getLastModifiedBy()).isEqualTo("user-1");
        assertThat(audit.getCreatedTime()).isNotNull();
    }

    @Test
    void jsonMerge_whenUpdateHasNewField_mergesIntoMain() {
        ObjectNode main = mapper.createObjectNode().put("a", 1);
        ObjectNode update = mapper.createObjectNode().put("b", 2);

        var merged = commonUtils.jsonMerge(main, update);

        assertThat(merged.get("a").asInt()).isEqualTo(1);
        assertThat(merged.get("b").asInt()).isEqualTo(2);
    }

    @Test
    void getFinancialYearStart_returnsPositiveEpoch() {
        assertThat(CommonUtils.getFinancialYearStart()).isPositive();
    }
}
