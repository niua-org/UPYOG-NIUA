package org.egov.dx.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.dx.service.TenantVerificationConfigService;
import org.egov.dx.util.Configurations;
import org.egov.mdms.model.MasterDetail;
import org.egov.mdms.model.MdmsCriteria;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.egov.mdms.model.ModuleDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

/**
 * MDMS-backed implementation of {@link TenantVerificationConfigService}.
 *
 * <p>Strictly satisfies SOLID principles:</p>
 * <ul>
 *   <li><b>Single Responsibility Principle (SRP):</b> Solely handles querying MDMS for tenant verification enablement.</li>
 *   <li><b>Dependency Inversion Principle (DIP):</b> Injects Spring {@link RestTemplate} and configuration abstractions.</li>
 * </ul>
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Service
@Slf4j
public class MdmsTenantVerificationConfigServiceImpl implements TenantVerificationConfigService {

    private final RestTemplate restTemplate;
    private final Configurations configurations;

    /**
     * Constructor injection satisfying Dependency Inversion Principle (DIP).
     *
     * @param restTemplate standard Spring RestTemplate bean
     * @param configurations centralized application configurations
     */
    @Autowired
    public MdmsTenantVerificationConfigServiceImpl(RestTemplate restTemplate,
                                                   Configurations configurations) {
        this.restTemplate = restTemplate;
        this.configurations = configurations;
    }

    @Override
    public boolean isVerificationEnabled(RequestInfo requestInfo, String tenantId, String serviceType) {
        if (StringUtils.isBlank(tenantId) || StringUtils.isBlank(serviceType)) {
            log.warn("[MDMS-VERIFICATION-CHECK] Blank tenantId or serviceType provided (tenantId={}, serviceType={})",
                    tenantId, serviceType);
            return false;
        }

        String host = configurations.getMdmsHost();
        String endpoint = configurations.getMdmsSearchEndpoint();
        if (StringUtils.isBlank(host) || StringUtils.isBlank(endpoint)) {
            log.error("[MDMS-VERIFICATION-CHECK] egov.mdms.host or egov.mdms.search.endpoint is not configured");
            return false;
        }

        String stateTenant = tenantId.contains(".") ? tenantId.split("\\.")[0] : tenantId;
        String moduleName = configurations.getMdmsVerificationModule();
        String masterName = configurations.getMdmsVerificationMaster();
        String url = buildMdmsUrl(host, endpoint);
        MdmsCriteriaReq criteriaReq = buildCriteriaReq(requestInfo, stateTenant, moduleName, masterName);

        try {
            log.debug("[MDMS-VERIFICATION-CHECK] Calling MDMS at {} for tenant {} and service {}", url, tenantId, serviceType);
            JsonNode responseNode = restTemplate.postForObject(url, criteriaReq, JsonNode.class);
            Boolean enabled = isServiceEnabledInResponse(responseNode, moduleName, masterName, serviceType, tenantId);
            if (enabled != null) {
                return enabled;
            }
        } catch (Exception e) {
            log.error("[MDMS-VERIFICATION-CHECK] Failed to query MDMS for tenant verification check (tenant={}, service={}): {}",
                    tenantId, serviceType, e.getMessage(), e);
            return false;
        }

        log.warn("[MDMS-VERIFICATION-CHECK] No MDMS configuration found for service {} and tenant {}", serviceType, tenantId);
        return false;
    }

    private String buildMdmsUrl(String host, String endpoint) {
        return host.endsWith("/") ? host + endpoint : host + "/" + endpoint;
    }

    private MdmsCriteriaReq buildCriteriaReq(RequestInfo requestInfo, String stateTenant, String moduleName, String masterName) {
        MasterDetail masterDetail = MasterDetail.builder().name(masterName).build();
        ModuleDetail moduleDetail = ModuleDetail.builder()
                .moduleName(moduleName)
                .masterDetails(Collections.singletonList(masterDetail))
                .build();
        MdmsCriteria criteria = new MdmsCriteria();
        criteria.setTenantId(stateTenant);
        criteria.setModuleDetails(Collections.singletonList(moduleDetail));

        MdmsCriteriaReq criteriaReq = new MdmsCriteriaReq();
        criteriaReq.setRequestInfo(requestInfo != null ? requestInfo : new RequestInfo());
        criteriaReq.setMdmsCriteria(criteria);
        return criteriaReq;
    }

    private Boolean isServiceEnabledInResponse(JsonNode responseNode, String moduleName, String masterName,
                                               String serviceType, String tenantId) {
        if (responseNode == null) {
            return null;
        }
        JsonNode configs = responseNode.path("MdmsRes").path(moduleName).path(masterName);
        if (!configs.isArray() || configs.isEmpty()) {
            return null;
        }
        for (JsonNode config : configs) {
            String type = config.hasNonNull("type")
                    ? config.path("type").asText()
                    : config.path("service").asText();
            if (serviceType.equalsIgnoreCase(type)) {
                boolean enabled = config.path("enabled").asBoolean(false);
                log.info("[MDMS-VERIFICATION-CHECK] Verification service {} is {} in MDMS for tenant {}",
                        serviceType, enabled ? "ENABLED" : "DISABLED", tenantId);
                return enabled;
            }
        }
        return null;
    }
}
