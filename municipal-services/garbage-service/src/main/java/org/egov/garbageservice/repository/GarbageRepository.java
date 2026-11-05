package org.egov.garbageservice.repository;

import org.egov.garbageservice.web.models.garbage.*;
import org.egov.garbageservice.web.models.user.*;
import org.egov.garbageservice.web.models.workflow.*;
import org.egov.garbageservice.web.models.billing.*;

import java.util.List;

/**
 * Repository interface for Garbage Service data persistence and retrieval,
 * structured after UPYOG municipal service patterns (e.g. TreePruningRepository).
 */
public interface GarbageRepository {

    void saveGarbageApplication(GarbageRequest garbageRequest);

    void updateGarbageApplication(GarbageRequest garbageRequest);

    void saveConnectionLog(GarbageConnectionLog connectionLog);

    void saveMonthlyBill(GarbageMonthlyRentPayment monthlyRentPayment);

    void saveBillGenerate(GarbageBillGenerate billGenerate);

    List<GarbageApplication> getGarbageApplications(GarbageSearchCriteria criteria);

    Integer getApplicationsCount(GarbageSearchCriteria criteria);

    List<GarbageConnectionLog> getConnectionLogs(String propertyId, String applicationNo);

    Long getNextSequence();
}
