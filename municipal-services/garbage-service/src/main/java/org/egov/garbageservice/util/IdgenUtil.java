package org.egov.garbageservice.util;

import digit.models.coremodels.IdGenerationRequest;
import digit.models.coremodels.IdGenerationResponse;
import digit.models.coremodels.IdRequest;
import digit.models.coremodels.IdResponse;
import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.request.RequestInfo;
import org.egov.garbageservice.repository.IdGenRepository;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * IdGen utility component aligned with tp-services patterns.
 */
@Component
@Slf4j
public class IdgenUtil {

    private final IdGenRepository idGenRepository;

    @Value("${egov.idgen.garbageservice.applicationNum.name}")
    private String applicationNumName;

    @Value("${egov.idgen.garbageservice.applicationNum.format}")
    private String applicationNumFormat;

    public IdgenUtil(IdGenRepository idGenRepository) {
        this.idGenRepository = idGenRepository;
    }

    public List<String> getIdList(RequestInfo requestInfo, String tenantId, String idName, String idFormat, Integer count) {
        return idGenRepository.getIdList(requestInfo, tenantId, idName, idFormat, count);
    }

    public String generateApplicationNumber(RequestInfo requestInfo, String tenantId) {
        try {
            List<String> ids = getIdList(requestInfo, tenantId, applicationNumName, applicationNumFormat, 1);
            if (!CollectionUtils.isEmpty(ids)) {
                return ids.get(0);
            }
        } catch (Exception e) {
            log.warn("IDGen service unavailable for application number, falling back to local sequence: {}", e.getMessage());
        }
        String cityCode = UserUtil.getStateLevelTenant(tenantId).toUpperCase();
        return "GB-" + cityCode + "-" + System.currentTimeMillis();
    }

    public String generateGarbageNumber(RequestInfo requestInfo, String tenantId) {
        try {
            List<String> ids = getIdList(requestInfo, tenantId, "garbage.consumer.num", "GC-[CITY.CODE]-[SEQ_GC_NUM]", 1);
            if (!CollectionUtils.isEmpty(ids)) {
                return ids.get(0);
            }
        } catch (Exception e) {
            log.warn("IDGen service unavailable for garbage number, falling back to local timestamp: {}", e.getMessage());
        }
        return "GC-" + System.currentTimeMillis();
    }

    public String generateRequestNumber(RequestInfo requestInfo, String tenantId, String type) {
        String prefix = "GB-DISC-";
        if ("RECONNECTION".equalsIgnoreCase(type)) {
            prefix = "GB-REC-";
        } else if ("PERMANENT_DISCONNECTION".equalsIgnoreCase(type)) {
            prefix = "GB-PDISC-";
        }
        return prefix + System.currentTimeMillis();
    }
}
