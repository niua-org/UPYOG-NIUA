package org.egov.pgr.util;

import org.egov.pgr.web.models.Service;
import org.egov.pgr.web.models.ServiceRequest;
import org.egov.pgr.web.modelsV2.ServiceRequestV3;
import org.egov.pgr.web.modelsV2.ServiceV3;

public class ServiceRequestConverter {


    /*
    * Convert ServiceV3 to Service object
    * */
    public static Service toService(ServiceV3 serviceV3) {
        return Service.builder()
                .id(serviceV3.getId())
                .tenantId(serviceV3.getTenantId())
                .serviceCode(serviceV3.getServiceCode())
                .serviceRequestId(serviceV3.getServiceRequestId())
                .description(serviceV3.getDescription())
                .accountId(serviceV3.getAccountId())
                .rating(serviceV3.getRating())
                .additionalDetail(serviceV3.getAdditionalDetail())
                .applicationStatus(serviceV3.getApplicationStatus())
                .source(serviceV3.getSource())
                .address(serviceV3.getAddress())
                .auditDetails(serviceV3.getAuditDetails())
                .priority(serviceV3.getPriority())
                .citizen(serviceV3.getCitizen())
                .active(serviceV3.isActive())
                .build();
    }

    /*
    * Convert Service to ServiceV3 object
    * */
    public static ServiceV3 toServiceV3(Service service) {
        return ServiceV3.builder()
                .id(service.getId())
                .tenantId(service.getTenantId())
                .serviceCode(service.getServiceCode())
                .serviceRequestId(service.getServiceRequestId())
                .description(service.getDescription())
                .accountId(service.getAccountId())
                .rating(service.getRating())
                .additionalDetail(service.getAdditionalDetail())
                .applicationStatus(service.getApplicationStatus())
                .source(service.getSource())
                .address(service.getAddress())
                .auditDetails(service.getAuditDetails())
                .priority(service.getPriority())
                .citizen(service.getCitizen())
                .active(service.isActive())
                .build();
    }
}

