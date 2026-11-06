package org.egov.garbageservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.garbageservice.config.GarbageServiceConfig;
import org.egov.garbageservice.repository.ServiceRequestRepository;
import org.egov.garbageservice.util.RequestInfoWrapper;
import org.egov.garbageservice.web.models.garbage.GarbageApplication;
import org.egov.garbageservice.web.models.garbage.PropertyLocation;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service integrating with UPYOG Property Tax (PT) service.
 * Enriches garbage applications dynamically with property address and location details.
 */
@Service
@Slf4j
public class PropertyService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final ObjectMapper mapper;
    private final GarbageServiceConfig config;



    public PropertyService(ServiceRequestRepository serviceRequestRepository, ObjectMapper mapper,
                           GarbageServiceConfig config) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.mapper = mapper;
        this.config = config;
    }

    /**
     * Searches Property Tax service for a given property ID and returns the mapped PropertyLocation.
     *
     * @param tenantId ULB tenant
     * @param propertyId Property ID (e.g. PG-PT-1013-001154)
     * @param requestInfo caller RequestInfo
     * @return PropertyLocation or null if not found
     */
    public PropertyLocation fetchPropertyLocation(String tenantId, String propertyId, RequestInfo requestInfo) {
        if (StringUtils.isBlank(propertyId) || StringUtils.isBlank(tenantId)) {
            return null;
        }

        StringBuilder uri = new StringBuilder(config.getPtHost())
                .append(config.getPtSearchEndpoint())
                .append("?tenantId=").append(tenantId)
                .append("&propertyIds=").append(propertyId);

        try {
            RequestInfoWrapper requestInfoWrapper = new RequestInfoWrapper(requestInfo != null ? requestInfo : new RequestInfo());
            Optional<Object> response = serviceRequestRepository.fetchResult(uri, requestInfoWrapper);
            if (response.isEmpty()) {
                log.info("No response returned from PT service for propertyId: {}", propertyId);
                return null;
            }

            JsonNode root = mapper.valueToTree(response.get());
            JsonNode properties = root.path("Properties");
            if (properties.isArray() && !properties.isEmpty()) {
                JsonNode prop = properties.get(0);
                JsonNode addr = prop.path("address");
                JsonNode loc = addr.path("locality");

                return PropertyLocation.builder()
                        .propertyId(prop.path("propertyId").asText(propertyId))
                        .houseNo(addr.path("doorNo").asText(null))
                        .houseName(addr.path("buildingName").asText(null))
                        .streetName(addr.path("street").asText(null))
                        .landmark(addr.path("landmark").asText(null))
                        .city(addr.path("city").asText(null))
                        .pincode(addr.path("pincode").asText(null))
                        .locality(loc.has("name") ? loc.path("name").asText() : loc.path("code").asText(null))
                        .build();
            }
        } catch (Exception e) {
            log.error("Failed to fetch property details from PT service for propertyId {}: {}", propertyId, e.getMessage(), e);
        }
        return null;
    }

    /**
     * Dynamically enriches a GarbageApplication with property location details from PT.
     *
     * @param application the application to enrich
     * @param requestInfo caller request info
     */
    public void enrichApplicationWithPropertyDetails(GarbageApplication application, RequestInfo requestInfo) {
        if (application == null || StringUtils.isBlank(application.getPropertyId())) {
            return;
        }

        if (application.getPropertyLocation() == null || StringUtils.isBlank(application.getPropertyLocation().getHouseNo())) {
            PropertyLocation loc = fetchPropertyLocation(application.getTenantId(), application.getPropertyId(), requestInfo);
            if (loc != null) {
                application.setPropertyLocation(loc);
            }
        }
    }
}
