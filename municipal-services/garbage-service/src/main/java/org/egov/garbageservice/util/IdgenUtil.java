package org.egov.garbageservice.util;

import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.request.RequestInfo;
import org.egov.garbageservice.repository.IdGenRepository;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * IdGen utility component aligned with standard UPYOG patterns.
 * Delegates ID generation strictly to eGov IDGen service.
 */
@Component
@Slf4j
public class IdgenUtil {

    private final IdGenRepository idGenRepository;

    @Value("${egov.idgen.garbageservice.applicationNum.name}")
    private String applicationNumName;

    @Value("${egov.idgen.garbageservice.applicationNum.format}")
    private String applicationNumFormat;

    @Value("${egov.idgen.garbageservice.garbageNum.name}")
    private String garbageNumName;

    @Value("${egov.idgen.garbageservice.garbageNum.format}")
    private String garbageNumFormat;

    public IdgenUtil(IdGenRepository idGenRepository) {
        this.idGenRepository = idGenRepository;
    }

    public List<String> getIdList(RequestInfo requestInfo, String tenantId, String idName, String idFormat, Integer count) {
        return idGenRepository.getIdList(requestInfo, tenantId, idName, idFormat, count);
    }

    public String generateApplicationNumber(RequestInfo requestInfo, String tenantId) {
        List<String> ids = getIdList(requestInfo, tenantId, applicationNumName, applicationNumFormat, 1);
        if (CollectionUtils.isEmpty(ids)) {
            log.error("IDGen service returned no ID for application number with name: {}", applicationNumName);
            throw new CustomException("IDGEN_ERROR", "No ids returned from idgen Service for application number");
        }
        return ids.get(0);
    }

    public String generateGarbageNumber(RequestInfo requestInfo, String tenantId) {
        List<String> ids = getIdList(requestInfo, tenantId, garbageNumName, garbageNumFormat, 1);
        if (CollectionUtils.isEmpty(ids)) {
            log.error("IDGen service returned no ID for garbage number with name: {}", garbageNumName);
            throw new CustomException("IDGEN_ERROR", "No ids returned from idgen Service for garbage number");
        }
        return ids.get(0);
    }

    public String generateRequestNumber(RequestInfo requestInfo, String tenantId, String type) {
        String idName = "garbage.disconnection.num";
        if ("RECONNECTION".equalsIgnoreCase(type)) {
            idName = "garbage.reconnection.num";
        } else if ("PERMANENT_DISCONNECTION".equalsIgnoreCase(type)) {
            idName = "garbage.permanent.disconnection.num";
        }
        try {
            List<String> ids = getIdList(requestInfo, tenantId, idName, null, 1);
            if (!CollectionUtils.isEmpty(ids)) {
                return ids.get(0);
            }
        } catch (Exception e) {
            log.warn("IDGen service unavailable for request number {}, generating timestamp sequence: {}", type, e.getMessage());
        }
        String prefix = "GC-DISC-";
        if ("RECONNECTION".equalsIgnoreCase(type)) {
            prefix = "GC-REC-";
        } else if ("PERMANENT_DISCONNECTION".equalsIgnoreCase(type)) {
            prefix = "GC-PDISC-";
        }
        return prefix + System.currentTimeMillis();
    }
}
