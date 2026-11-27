package org.upyog.family.persistence.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.egov.tracer.model.ServiceCallException;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Repository responsible for executing HTTP requests to external microservices.
 */
@Repository
@Slf4j
public class ServiceRequestRepository {

    private final ObjectMapper mapper;
    private final RestTemplate restTemplate;

    /**
     * Constructs a new {@code ServiceRequestRepository} with ObjectMapper and RestTemplate.
     *
     * @param mapper       Jackson object mapper
     * @param restTemplate Spring REST template
     */
    public ServiceRequestRepository(ObjectMapper mapper, RestTemplate restTemplate) {
        this.mapper = mapper;
        this.restTemplate = restTemplate;
        this.mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }

    /**
     * Dispatches an HTTP POST request to the specified URI with the provided request payload.
     *
     * @param uri     Target endpoint URI
     * @param request Request payload object
     * @return Response body as a Map
     * @throws ServiceCallException if an HTTP or connection error occurs
     */
    public Object fetchResult(StringBuilder uri, Object request) {
        Object response = null;
        try {
            response = restTemplate.postForObject(uri.toString(), request, Map.class);
        } catch (HttpClientErrorException e) {
            throw new ServiceCallException(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("[UPYOG-FAMILY-SERVICE] Exception while fetching from external service: ", e);
            throw new ServiceCallException(e.getMessage());
        }

        return response;
    }
}
