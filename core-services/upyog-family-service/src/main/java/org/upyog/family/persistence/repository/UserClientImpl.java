package org.upyog.family.persistence.repository;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.upyog.family.config.FamilyConfiguration;
import org.upyog.family.web.contract.CitizenProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link UserClient} communicating with UPYOG user services to validate and enrich citizen profiles.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Repository
@Slf4j
public class UserClientImpl implements UserClient {

    private final RestTemplate restTemplate;
    private final FamilyConfiguration configuration;

    @Autowired
    public UserClientImpl(RestTemplate restTemplate, FamilyConfiguration configuration) {
        this.restTemplate = restTemplate;
        this.configuration = configuration;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, CitizenProfile> searchUsersByUuids(List<String> uuids, RequestInfo requestInfo) {
        if (uuids == null || uuids.isEmpty()) {
            return Collections.emptyMap();
        }

        String url = configuration.getUserHost() + configuration.getUserSearchPath();
        log.debug("[UPYOG-USER-CLIENT] Querying user service for {} UUIDs from: {}", uuids.size(), url);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("RequestInfo", requestInfo);
        requestBody.put("uuid", uuids);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            if (response != null && response.containsKey("user")) {
                List<Map<String, Object>> userList = (List<Map<String, Object>>) response.get("user");
                if (userList != null) {
                    return userList.stream()
                            .map(this::mapToCitizenProfile)
                            .filter(p -> StringUtils.isNotBlank(p.getUuid()))
                            .collect(Collectors.toMap(CitizenProfile::getUuid, p -> p, (existing, replacement) -> existing));
                }
            }
        } catch (Exception e) {
            log.warn("[UPYOG-USER-CLIENT] UPYOG user search failed or unreachable: {}", e.getMessage());
        }

        return Collections.emptyMap();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CitizenProfile> searchUsersByMobile(String mobileNumber, String tenantId, RequestInfo requestInfo) {
        if (StringUtils.isBlank(mobileNumber)) {
            return Collections.emptyList();
        }

        String url = configuration.getUserHost() + configuration.getUserSearchPath();
        log.debug("[UPYOG-USER-CLIENT] Querying user service for mobile: {} from: {}", maskMobile(mobileNumber), url);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("RequestInfo", requestInfo);
        requestBody.put("mobileNumber", mobileNumber);
        if (StringUtils.isNotBlank(tenantId)) {
            requestBody.put("tenantId", tenantId);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            if (response != null && response.containsKey("user")) {
                List<Map<String, Object>> userList = (List<Map<String, Object>>) response.get("user");
                if (userList != null) {
                    return userList.stream()
                            .map(this::mapToCitizenProfile)
                            .collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            log.warn("[UPYOG-USER-CLIENT] UPYOG user mobile search failed or unreachable: {}", e.getMessage());
        }

        return Collections.emptyList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public CitizenProfile createCitizenProfile(CitizenProfile profile, RequestInfo requestInfo) {
        if (profile == null) {
            return null;
        }

        String url = configuration.getUserHost() + "/user/citizen/_create";
        log.info("[UPYOG-USER-CLIENT] Auto-creating citizen user for mobile: {} at: {}", maskMobile(profile.getMobileNumber()), url);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("RequestInfo", requestInfo);

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", StringUtils.isNotBlank(profile.getName()) ? profile.getName() : "Citizen");
        userMap.put("userName", profile.getMobileNumber());
        userMap.put("mobileNumber", profile.getMobileNumber());
        userMap.put("gender", StringUtils.isNotBlank(profile.getGender()) ? profile.getGender() : "OTHERS");
        userMap.put("type", "CITIZEN");
        userMap.put("active", true);

        requestBody.put("user", userMap);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            if (response != null && response.containsKey("user")) {
                List<Map<String, Object>> userList = (List<Map<String, Object>>) response.get("user");
                if (userList != null && !userList.isEmpty()) {
                    return mapToCitizenProfile(userList.get(0));
                }
            }
        } catch (Exception e) {
            log.warn("[UPYOG-USER-CLIENT] Remote citizen creation failed: {}. Provisioning local UUID.", e.getMessage());
        }

        return CitizenProfile.builder()
                .uuid(UUID.randomUUID().toString())
                .name(profile.getName())
                .mobileNumber(profile.getMobileNumber())
                .gender(profile.getGender())
                .build();
    }

    private CitizenProfile mapToCitizenProfile(Map<String, Object> map) {
        Long dob = null;
        Object dobObj = map.get("dob");
        if (dobObj instanceof Number) {
            dob = ((Number) dobObj).longValue();
        }

        return CitizenProfile.builder()
                .uuid((String) map.get("uuid"))
                .userName((String) map.get("userName"))
                .name((String) map.get("name"))
                .mobileNumber((String) map.get("mobileNumber"))
                .gender((String) map.get("gender"))
                .dob(dob)
                .photo((String) map.get("photo"))
                .emailId((String) map.get("emailId"))
                .build();
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 10) {
            return "******";
        }
        return mobile.substring(0, 2) + "******" + mobile.substring(mobile.length() - 2);
    }
}
