package org.egov.garbageservice.repository;

import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.request.RequestInfo;
import org.egov.garbageservice.config.GarbageServiceConfig;
import org.egov.garbageservice.web.models.bill.Demand;
import org.egov.garbageservice.web.models.bill.DemandRequest;
import org.egov.garbageservice.web.models.bill.DemandResponse;
import org.egov.garbageservice.util.RequestInfoWrapper;
import org.egov.tracer.model.ServiceCallException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Repository component for Demand database interactions.
 *
 * <p>Provides data access, query building, and persistence operations for the garbage service domain.
 */
@Repository
@Slf4j
public class DemandRepository {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private GarbageServiceConfig config;

    /**
     * Queries database for records matching the provided criteria.
     *
     * <p>The operation performs the following steps:
     * <ol>
     *   <li>Constructs a dynamic SQL query based on active search criteria parameters.</li>
     *   <li>Appends pagination boundaries (limit and offset) and sorting clauses.</li>
     *   <li>Executes the SQL query via JdbcTemplate using custom row mapping.</li>
     *   <li>Assembles and returns the resulting entity list.</li>
     * </ol>
     *
     * @param requestInfo     the request information containing user session details
     * @param tenantId        the tenant ID associated with the request
     * @param consumerCode    the consumerCode parameter for this operation
     * @param businessService the businessService parameter for this operation
     * @return the output result of type {@link List{@code <Demand>}}
     */

    public List<Demand> searchAllDemands(RequestInfo requestInfo, String tenantId, String consumerCode, String businessService, boolean unpaidOnly) {
        StringBuilder url = new StringBuilder(config.getBillingHost());
        url.append(config.getDemandSearchEndpoint());
        url.append("?tenantId=").append(tenantId);
        url.append("&consumerCode=").append(consumerCode);
        url.append("&businessService=").append(businessService);
        if(unpaidOnly){url.append("&isPaymentCompleted=false");}

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        RequestInfoWrapper requestInfoWrapper = new RequestInfoWrapper(requestInfo);
        HttpEntity<RequestInfoWrapper> requestEntity = new HttpEntity<>(requestInfoWrapper, headers);

        try {
            ResponseEntity<DemandResponse> response =
                    restTemplate.postForEntity(url.toString(), requestEntity, DemandResponse.class);
            return response.getBody().getDemands();
        } catch (Exception e) {
            log.error("Error while fetching demands from url {}: {}", url, e.getMessage(), e);
            throw new ServiceCallException("Error while fetching demands: " + e.getMessage());
        }
    }
}