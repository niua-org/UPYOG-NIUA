package org.upyog.tp.repository;
import java.util.List;

import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.upyog.tp.config.TreePruningConfiguration;
import org.upyog.tp.web.models.Demand;
import org.upyog.tp.web.models.DemandRequest;
import org.upyog.tp.web.models.DemandResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
public class DemandRepository {

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private TreePruningConfiguration config;

    @Autowired
    private ObjectMapper mapper;

    /**
     * Creates demand
     *
     * @param requestInfo The RequestInfo of the calculation Request
     * @param demand    The demands to be created
     * @return The list of demand created
     */
    public List<Demand> saveDemand(RequestInfo requestInfo, List<Demand> demand) {
        StringBuilder url = new StringBuilder(config.getBillingHost());
        url.append(config.getDemandCreateEndpoint());
        DemandRequest request = new DemandRequest(requestInfo, demand);
        log.info("Request object for fetchResult: " + request);
        log.info("URL for fetchResult: " + url);
        Object result = serviceRequestRepository.fetchResult(url, request);
        log.info("Result from fetchResult method: " + result);
        DemandResponse response = null;
        try {
            response = mapper.convertValue(result, DemandResponse.class);
            log.info("Demand response mapper: " + response);
        } catch (IllegalArgumentException e) {
            throw new CustomException("PARSING ERROR", "Failed to parse response of create demand");
        }
        return response.getDemands();
    }


}
