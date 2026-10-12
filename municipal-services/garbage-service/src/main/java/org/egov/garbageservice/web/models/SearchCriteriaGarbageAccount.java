package org.egov.garbageservice.web.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.egov.tracer.annotations.CustomSafeHtml;

import java.util.List;

@AllArgsConstructor
@Data
@Builder(toBuilder = true)
/**
 * Search filters for finding garbage accounts (tenant, status, mobile, property, dates, etc.).
 * Embedded in SearchCriteriaGarbageAccountRequest for authenticated and open search APIs.
 */
@NoArgsConstructor
public class SearchCriteriaGarbageAccount {

    private List<Long> id;

    private List<Long> garbageId;

    private List<String> propertyId;

    private List<String> uuid;

    private List<String> user_uuid;

    private List<String> type;

    private List<String> name;

    private List<String> mobileNumber;

    private List<String> createdBy;

    private List<String> applicationNumber;

    @CustomSafeHtml
    private String tenantId;

    private List<String> status;

    private List<String> statusList;

    private List<String> paymentStatus;

    private List<String> channels;

    private List<String> wardNames;

    private List<String> oldGarbageIds;

    private List<String> unitTypes;

    private List<String> unitCategories;

    private Boolean isOwner;

    @CustomSafeHtml
    private String parentAccount;

    @CustomSafeHtml
    private String orderBy = "DESC";

    private Long startId;

    private Long endId;

    private Boolean isActiveSubAccount;

    private Boolean isActiveAccount;

    private Boolean isUserUuidNull;

    private Boolean isMonthlyBilling;

    @Builder.Default
    private Boolean isPayNow = false;

    @Builder.Default
    private Boolean isSchedulerCall = false;

    private Long offset;

    private Long limit;

    @CustomSafeHtml
    private String userType;

    private Long fromDate;

    private Long toDate;
}
