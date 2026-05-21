package org.egov.commons.service;

import java.util.Collections;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class RestCallService {
    private static final Logger LOG = LogManager.getLogger(RestCallService.class);

    public Object fetchResult(StringBuilder uri, Object request) {
        Object response = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            LOG.info("URI: " + uri.toString());
            LOG.info("Request: " + mapper.writeValueAsString(request));

            // THIS is the fix - forces JSON instead of XML
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            HttpEntity<Object> entity = new HttpEntity<>(request, headers);

            RestTemplate restTemplate = new RestTemplate();
            response = restTemplate.postForObject(uri.toString(), entity, Map.class);

        } catch (HttpClientErrorException e) {
            LOG.error("Error occurred while calling API: status={}, body={}",
                e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw e;
        } catch (JsonProcessingException e) {
            LOG.error("Error serializing request: " + e.getMessage(), e);
        }
        return response;
    }
}