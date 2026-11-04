package org.egov.swservice.validator;

import org.egov.common.contract.request.RequestInfo;
import org.egov.swservice.util.SWConstants;
import org.egov.swservice.web.models.OwnerInfo;
import org.egov.swservice.web.models.SewerageConnection;
import org.egov.swservice.web.models.SewerageConnectionRequest;
import org.egov.tracer.model.CustomException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MDMSValidatorTest {

    private final MDMSValidator mdmsValidator = new MDMSValidator();

    private static SewerageConnection sewerageWithTenant(String tenantId) {
        SewerageConnection connection = SewerageConnection.builder().build();
        connection.setTenantId(tenantId);
        return connection;
    }

    @Test
    void validateCodesForCreateRequest_whenInvalidOwnerType_throws() {
        OwnerInfo holder = new OwnerInfo();
        holder.setOwnerType("UNKNOWN");
        SewerageConnection connection = SewerageConnection.builder().build();
        connection.setConnectionHolders(List.of(holder));
        SewerageConnectionRequest request = SewerageConnectionRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .sewerageConnection(connection)
                .build();
        Map<String, List<String>> codes = Map.of(SWConstants.PROPERTY_OWNERTYPE, List.of("OWNER"));

        assertThatThrownBy(() -> mdmsValidator.validateCodesForCreateRequest(request, codes))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("INVALID_CONNECTION_HOLDER_TYPE");
    }

    @Test
    void validateCodesForCreateRequest_whenOwnerTypeValid_passes() {
        OwnerInfo holder = new OwnerInfo();
        holder.setOwnerType("OWNER");
        SewerageConnection connection = SewerageConnection.builder().build();
        connection.setConnectionHolders(List.of(holder));
        SewerageConnectionRequest request = SewerageConnectionRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .sewerageConnection(connection)
                .build();
        Map<String, List<String>> codes = Map.of(SWConstants.PROPERTY_OWNERTYPE, List.of("OWNER"));

        mdmsValidator.validateCodesForCreateRequest(request, codes);
    }

    @Test
    void validateMasterData_whenUnknownReqType_doesNotThrow() {
        SewerageConnectionRequest request = SewerageConnectionRequest.builder()
                .requestInfo(RequestInfo.builder().build())
                .sewerageConnection(sewerageWithTenant("pb.amritsar"))
                .build();

        mdmsValidator.validateMasterData(request, -1);
    }
}
