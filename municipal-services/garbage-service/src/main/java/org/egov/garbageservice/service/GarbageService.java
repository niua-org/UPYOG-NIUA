package org.egov.garbageservice.service;

import org.egov.common.contract.request.RequestInfo;
import org.egov.garbageservice.web.models.garbage.*;

import java.util.List;

/**
 * Service interface defining Garbage Service operations:
 * multi-application create, workflow updates, connection lifecycle (disconnect, reconnect, permanent),
 * and persona searches.
 */
public interface GarbageService {

    GarbageResponse create(GarbageRequest garbageRequest);

    GarbageResponse update(GarbageRequest garbageRequest);

    GarbageResponse search(GarbageSearchCriteria criteria, RequestInfo requestInfo);

    Integer getApplicationsCount(GarbageSearchCriteria criteria, RequestInfo requestInfo);

    GarbageConnectionResponse disconnect(GarbageConnectionRequest connectionRequest);

    GarbageConnectionResponse reconnect(GarbageConnectionRequest connectionRequest);

    GarbageConnectionResponse permanentDisconnect(GarbageConnectionRequest connectionRequest);

    GarbageConnectionResponse updateConnection(GarbageConnectionRequest connectionRequest);

    List<GarbageConnectionLog> searchConnectionHistory(String propertyId, String applicationNo, RequestInfo requestInfo);
}
