package org.egov.user.persistence.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.user.config.UserConfiguration;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.web.contract.property.UrbanPropertyInfo;
import org.egov.user.web.contract.property.UrbanPropertyOwnerInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST client communicating with UPYOG Property Tax service (property-services) to fetch
 * municipal property assessment details and registered owners.
 */
@Repository
@Slf4j
public class PropertyServiceClient {

    private final RestTemplate restTemplate;
    private final UserConfiguration userConfiguration;
    private final ObjectMapper objectMapper;

    /**
     * Constructs a {@link PropertyServiceClient} with required HTTP template, configuration, and JSON mapper.
     *
     * @param restTemplate Spring RestTemplate for making remote HTTP calls
     * @param userConfiguration user configuration providing property service endpoint properties
     * @param objectMapper Jackson ObjectMapper for parsing property responses
     */
    @Autowired
    public PropertyServiceClient(@Autowired(required = false) RestTemplate restTemplate,
                                 UserConfiguration userConfiguration,
                                 @Autowired(required = false) ObjectMapper objectMapper) {
        this.restTemplate = restTemplate != null ? restTemplate : new RestTemplate();
        this.userConfiguration = userConfiguration;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Searches for a property by its UPYOG property assessment ID and tenantId.
     *
     * @param tenantId tenant identifier
     * @param propertyId municipal property identifier (e.g. PB-PT-2026-001234)
     * @param requestInfo platform RequestInfo envelope
     * @return populated UrbanPropertyInfo, or null if not found
     */
    public UrbanPropertyInfo fetchPropertyById(String tenantId, String propertyId, RequestInfo requestInfo) {
        if (StringUtils.isBlank(tenantId) || StringUtils.isBlank(propertyId)) {
            throw new CustomException(UserConstants.ERROR_INVALID_PROPERTY_SEARCH_CRITERIA, UserConstants.MSG_PROPERTY_SEARCH_MANDATORY_FIELDS);
        }

        String host = userConfiguration.getPropertyServiceHost();
        String endpoint = userConfiguration.getPropertyServiceSearchEndpoint();
        if (StringUtils.isBlank(host) || StringUtils.isBlank(endpoint)) {
            log.error("[PROPERTY-CLIENT] Property service host or endpoint is not configured.");
            throw new CustomException(UserConstants.ERROR_PROPERTY_SERVICE_CONFIG_MISSING, UserConstants.MSG_PROPERTY_SERVICE_CONFIG_MISSING);
        }

        String url = UriComponentsBuilder.fromHttpUrl(host.endsWith("/") ? host.substring(0, host.length() - 1) + "/" + endpoint.replaceFirst("^/", "") : host + "/" + endpoint.replaceFirst("^/", ""))
                .queryParam(UserConstants.PT_JSON_TENANT_ID, tenantId.trim())
                .queryParam(UserConstants.PT_JSON_PROPERTY_IDS, propertyId.trim())
                .toUriString();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put(UserConstants.OTP_JSON_REQUEST_INFO, requestInfo != null ? requestInfo : new RequestInfo());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            log.info("[PROPERTY-CLIENT] Fetching property details from URL: {}", url);
            String rawJson = restTemplate.postForObject(url, entity, String.class);
            if (StringUtils.isBlank(rawJson)) {
                log.warn("[PROPERTY-CLIENT] Blank response received from property-services for ID: {}", propertyId);
                return null;
            }

            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode propertiesNode = root.get(UserConstants.PT_JSON_PROPERTIES);
            if (propertiesNode == null || !propertiesNode.isArray() || propertiesNode.isEmpty()) {
                log.warn("[PROPERTY-CLIENT] No properties found for ID: {} in tenant: {}", propertyId, tenantId);
                return null;
            }

            JsonNode propNode = propertiesNode.get(0);
            return mapJsonNodeToUrbanPropertyInfo(propNode, tenantId);

        } catch (CustomException ce) {
            throw ce;
        } catch (Exception e) {
            log.error("[PROPERTY-CLIENT] Error querying property-services for ID {}: {}", propertyId, e.getMessage(), e);
            throw new CustomException(UserConstants.ERROR_PROPERTY_SERVICE_ERROR, UserConstants.MSG_PROPERTY_SERVICE_ERROR_PREFIX + e.getMessage());
        }
    }

    /**
     * Maps raw JSON property data from property-services into an {@link UrbanPropertyInfo} aggregate.
     *
     * @param node JSON tree node representing a single property
     * @param fallbackTenantId tenant ID passed from query context if omitted in payload
     * @return populated UrbanPropertyInfo instance
     */
    private UrbanPropertyInfo mapJsonNodeToUrbanPropertyInfo(JsonNode node, String fallbackTenantId) {
        String propId = node.path(UserConstants.PT_JSON_PROPERTY_ID).asText(null);
        String tenantId = node.path(UserConstants.PT_JSON_TENANT_ID).asText(fallbackTenantId);
        String status = node.path(UserConstants.PT_JSON_STATUS).asText(null);
        String propertyType = node.path(UserConstants.PT_JSON_PROPERTY_TYPE).asText(null);
        String ownershipCategory = node.path(UserConstants.PT_JSON_OWNERSHIP_CATEGORY).asText(null);

        // Address formatting
        String formattedAddress = null;
        JsonNode addressNode = node.get(UserConstants.PT_JSON_ADDRESS);
        if (addressNode != null) {
            List<String> parts = new ArrayList<>();
            if (addressNode.hasNonNull(UserConstants.PT_JSON_DOOR_NO)) parts.add(addressNode.get(UserConstants.PT_JSON_DOOR_NO).asText());
            if (addressNode.hasNonNull(UserConstants.PT_JSON_BUILDING_NAME)) parts.add(addressNode.get(UserConstants.PT_JSON_BUILDING_NAME).asText());
            if (addressNode.hasNonNull(UserConstants.PT_JSON_STREET)) parts.add(addressNode.get(UserConstants.PT_JSON_STREET).asText());
            if (addressNode.hasNonNull(UserConstants.PT_JSON_LANDMARK)) parts.add(addressNode.get(UserConstants.PT_JSON_LANDMARK).asText());
            if (addressNode.hasNonNull(UserConstants.PT_JSON_CITY)) parts.add(addressNode.get(UserConstants.PT_JSON_CITY).asText());
            if (addressNode.hasNonNull(UserConstants.PT_JSON_PINCODE)) parts.add(addressNode.get(UserConstants.PT_JSON_PINCODE).asText());
            formattedAddress = !parts.isEmpty() ? String.join(", ", parts) : null;
        }

        // Owners list
        List<UrbanPropertyOwnerInfo> ownerList = new ArrayList<>();
        JsonNode ownersNode = node.get(UserConstants.PT_JSON_OWNERS);
        if (ownersNode != null && ownersNode.isArray()) {
            for (JsonNode ownerNode : ownersNode) {
                String ownerStatus = ownerNode.path(UserConstants.PT_JSON_STATUS).asText(UserConstants.STATUS_ACTIVE_UPPERCASE);
                if (UserConstants.STATUS_INACTIVE_UPPERCASE.equalsIgnoreCase(ownerStatus)) {
                    continue; // Skip inactive owners
                }

                String rawMobile = ownerNode.path(UserConstants.PT_JSON_MOBILE_NUMBER).asText(null);
                ownerList.add(UrbanPropertyOwnerInfo.builder()
                        .uuid(ownerNode.path(UserConstants.PT_JSON_UUID).asText(null))
                        .name(ownerNode.path(UserConstants.PT_JSON_NAME).asText(null))
                        .rawMobileNumber(rawMobile)
                        .maskedMobileNumber(org.egov.user.util.MaskUtils.maskMobile(rawMobile))
                        .isPrimaryOwner(ownerNode.path(UserConstants.PT_JSON_IS_PRIMARY_OWNER).asBoolean(false))
                        .ownerShipPercentage(ownerNode.has(UserConstants.PT_JSON_OWNERSHIP_PERCENTAGE) ? ownerNode.get(UserConstants.PT_JSON_OWNERSHIP_PERCENTAGE).asDouble() : null)
                        .ownerType(ownerNode.path(UserConstants.PT_JSON_OWNER_TYPE).asText(null))
                        .alreadyLinked(false)
                        .build());
            }
        }

        return UrbanPropertyInfo.builder()
                .propertyId(propId)
                .tenantId(tenantId)
                .status(status)
                .propertyType(propertyType)
                .ownershipCategory(ownershipCategory)
                .urbanAddress(formattedAddress)
                .totalOwners(ownerList.size())
                .linkedOwnersCount(0)
                .eligibleForLinking(true)
                .owners(ownerList)
                .build();
    }
}
