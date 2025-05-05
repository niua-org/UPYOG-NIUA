package org.egov.pgr.web.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.pgr.service.PGRService;
import org.egov.pgr.util.ResponseInfoFactory;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.pgr.web.models.*;
import org.egov.pgr.web.modelsV2.ServiceRequestV3;
import org.egov.pgr.web.modelsV2.ServiceResponseV3;
import org.egov.pgr.web.modelsV2.ServiceWrapperV3;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

import javax.validation.Valid;

public class PgrAiControllerV3 extends RequestsApiController{

    private final ObjectMapper objectMapper;

    private PGRService pgrService;

    private ResponseInfoFactory responseInfoFactory;

    public PgrAiControllerV3(ObjectMapper objectMapper, PGRService pgrService, ResponseInfoFactory responseInfoFactory, ObjectMapper objectMapper1) {
        super(objectMapper, pgrService, responseInfoFactory);
        this.objectMapper = objectMapper1;
    }
    /**
     * Endpoint to create a PGR application (V3) by saving a citizen grievance.
     * The grievance input is categorized using an AI-based LLM model.
     *
     * @param request the V3 grievance request payload
     * @return a response containing the enriched grievance details
     * @throws IOException in case of any IO-related issues
     */
    @RequestMapping(value="/request/v3/_create", method = RequestMethod.POST)
    public ResponseEntity<ServiceResponseV3> requestsCreateV3(@Valid @RequestBody ServiceRequestV3 request) throws IOException {
        ServiceRequestV3 enrichedReq = pgrService.createV3(request);
        ResponseInfo responseInfo = responseInfoFactory.createResponseInfoFromRequestInfo(request.getRequestInfo(), true);
        ServiceWrapperV3 serviceWrapper = ServiceWrapperV3.builder().service(enrichedReq.getService()).workflow(enrichedReq.getWorkflow()).build();
        ServiceResponseV3 response = ServiceResponseV3.builder().responseInfo(responseInfo).serviceWrappers(Collections.singletonList(serviceWrapper)).build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
