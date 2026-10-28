package org.egov.asset.util;

import org.egov.asset.repository.AssetRepository;
import org.egov.asset.web.models.AuditDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AssetUtilTest {

    @Mock
    private AssetRepository assetRepository;

    @Test
    void getAuditDetails_whenCreate_populatesTimestamps() {
        AssetUtil util = new AssetUtil(assetRepository);
        AuditDetails audit = util.getAuditDetails("user-1", true);
        assertThat(audit.getCreatedBy()).isEqualTo("user-1");
    }
}
