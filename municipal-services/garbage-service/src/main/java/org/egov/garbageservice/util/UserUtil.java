package org.egov.garbageservice.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.request.Role;
import org.egov.garbageservice.web.models.user.User;
import org.egov.garbageservice.config.GarbageServiceConfig;
import org.egov.garbageservice.repository.ServiceRequestRepository;
import org.egov.garbageservice.web.models.user.UserDetailResponse;
import org.egov.tracer.model.CustomException;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Utility component for User Service operations, aligned with tp-services conventions.
 */
@Component
@Slf4j
public class UserUtil {

    private static final String LAST_MODIFIED_DATE = "lastModifiedDate";
    private static final String PWD_EXPIRY_DATE = "pwdExpiryDate";

    private final ObjectMapper mapper;
    private final ServiceRequestRepository serviceRequestRepository;
    private final GarbageServiceConfig config;

    public UserUtil(ObjectMapper mapper, ServiceRequestRepository serviceRequestRepository,
                    GarbageServiceConfig config) {
        this.mapper = mapper;
        this.serviceRequestRepository = serviceRequestRepository;
        this.config = config;
    }

    /**
     * Executes REST call to user service with given URI and object.
     *
     * @param userRequest request payload for user service
     * @param uri destination endpoint
     * @return parsed UserDetailResponse
     */
    @SuppressWarnings("unchecked")
    public UserDetailResponse userCall(Object userRequest, StringBuilder uri) {
        String dobFormat = null;
        if (uri.toString().contains(config.getUserSearchEndpoint())) {
            dobFormat = "yyyy-MM-dd";
        } else {
            dobFormat = "dd/MM/yyyy";
        }

        try {
            Optional<Object> response = serviceRequestRepository.fetchResult(uri, userRequest);
            if (response.isPresent()) {
                LinkedHashMap<String, Object> responseMap = (LinkedHashMap<String, Object>) response.get();
                parseResponse(responseMap, dobFormat);
                return mapper.convertValue(responseMap, UserDetailResponse.class);
            }
            return new UserDetailResponse();
        } catch (IllegalArgumentException e) {
            log.error("Failed to parse user service response: {}", e.getMessage(), e);
            throw new CustomException("ILLEGAL_ARGUMENT_EXCEPTION", "ObjectMapper unable to convertValue in userCall: " + e.getMessage());
        }
    }

    /**
     * Parses date formats in user response to epoch milliseconds.
     *
     * @param responseMap raw response map
     * @param dobFormat expected DOB pattern
     */
    @SuppressWarnings("unchecked")
    public void parseResponse(Map<String, Object> responseMap, String dobFormat) {
        List<Map<String, Object>> users = (List<Map<String, Object>>) responseMap.get("user");
        String format1 = "dd-MM-yyyy HH:mm:ss";

        if (users != null) {
            users.forEach(map -> {
                map.put("createdDate", dateToLong((String) map.get("createdDate"), format1));
                if (map.get(LAST_MODIFIED_DATE) != null) {
                    map.put(LAST_MODIFIED_DATE, dateToLong((String) map.get(LAST_MODIFIED_DATE), format1));
                }
                if (map.get("dob") != null) {
                    map.put("dob", dateToLong((String) map.get("dob"), dobFormat));
                }
                if (map.get(PWD_EXPIRY_DATE) != null) {
                    map.put(PWD_EXPIRY_DATE, dateToLong((String) map.get(PWD_EXPIRY_DATE), format1));
                }
            });
        }
    }

    public Long dateToLong(String date, String format) {
        if (date == null) return null;
        try {
            SimpleDateFormat f = new SimpleDateFormat(format);
            Date d = f.parse(date);
            return d.getTime();
        } catch (ParseException e) {
            log.debug("Unable to parse date string {}: {}", date, e.getMessage());
            return null;
        }
    }

    /**
     * Populates standard citizen roles and username on User payload.
     *
     * @param mobileNumber citizen mobile number
     * @param userInfo user contract object
     */
    public void addUserDefaultFields(String mobileNumber, User userInfo) {
        Role role = getCitizenRole();
        userInfo.setRoles(Collections.singletonList(role));
        userInfo.setType("CITIZEN");
        userInfo.setUserName(mobileNumber);
    }

    public Role getCitizenRole() {
        return Role.builder().code("CITIZEN").name("Citizen").build();
    }

    /**
     * Extracts state-level tenant identifier (e.g. pg from pg.citya).
     */
    public static String getStateLevelTenant(String tenantId) {
        if (tenantId == null) return null;
        return tenantId.split("\\.")[0];
    }
}
