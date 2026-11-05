package org.egov.garbageservice.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.garbageservice.config.GarbageServiceConfig;
import org.egov.garbageservice.repository.DemandRepository;
import org.egov.garbageservice.repository.GarbageRepository;
import org.egov.garbageservice.service.*;
import org.egov.garbageservice.util.GrbgConstants;
import org.egov.garbageservice.util.IdgenUtil;
import org.egov.garbageservice.util.ResponseInfoFactory;
import org.egov.garbageservice.web.models.garbage.*;
import org.egov.garbageservice.web.models.user.*;
import org.egov.garbageservice.web.models.workflow.*;
import org.egov.garbageservice.web.models.billing.*;
import org.egov.garbageservice.web.models.billing.Demand;
import org.egov.garbageservice.web.models.user.User;
import org.egov.garbageservice.service.WorkflowService;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core implementation of GarbageService implementing multi-application creation,
 * owner-only connection lifecycle guards, and dynamic persona search.
 */
@Service
@Slf4j
public class GarbageServiceImpl implements GarbageService {

    private final GarbageRepository garbageRepository;
    private final UserService userService;
    private final PropertyService propertyService;
    private final NotificationService notificationService;
    private final WorkflowService workflowService;
    private final DemandRepository demandRepository;
    private final IdgenUtil idgenUtil;
    private final ResponseInfoFactory responseInfoFactory;
    private final GarbageServiceConfig config;

    @Autowired
    public GarbageServiceImpl(GarbageRepository garbageRepository, UserService userService,
                              PropertyService propertyService, NotificationService notificationService,
                              WorkflowService workflowService, DemandRepository demandRepository,
                              IdgenUtil idgenUtil, ResponseInfoFactory responseInfoFactory,
                              GarbageServiceConfig config) {
        this.garbageRepository = garbageRepository;
        this.userService = userService;
        this.propertyService = propertyService;
        this.notificationService = notificationService;
        this.workflowService = workflowService;
        this.demandRepository = demandRepository;
        this.idgenUtil = idgenUtil;
        this.responseInfoFactory = responseInfoFactory;
        this.config = config;
    }

    @Override
    public GarbageResponse create(GarbageRequest garbageRequest) {
        if (garbageRequest != null && !CollectionUtils.isEmpty(garbageRequest.getGarbageApplications())) {
            garbageRequest.setGarbageApplications(flattenApplications(garbageRequest.getGarbageApplications()));
        }
        log.info("Processing create GarbageRequest for applications count: {}",
                garbageRequest != null && garbageRequest.getGarbageApplications() != null ? garbageRequest.getGarbageApplications().size() : 0);

        validateCreateRequest(garbageRequest);
        RequestInfo requestInfo = garbageRequest.getRequestInfo();
        String userUuid = requestInfo.getUserInfo() != null ? requestInfo.getUserInfo().getUuid() : "SYSTEM";
        long now = Instant.now().toEpochMilli();

        String commonOwnerUuid = null;
        String commonPropertyId = null;
        String commonTenantId = null;
        PropertyLocation commonPropertyLocation = null;

        for (GarbageApplication app : garbageRequest.getGarbageApplications()) {
            if (app.getId() == null) {
                app.setId(garbageRepository.getNextSequence());
            }
            if (StringUtils.isBlank(app.getUuid())) {
                app.setUuid(UUID.randomUUID().toString());
            }

            if (StringUtils.isBlank(app.getApplicationNo())) {
                app.setApplicationNo(idgenUtil.generateApplicationNumber(requestInfo, app.getTenantId()));
            }
            if (StringUtils.isBlank(app.getGarbageNo())) {
                app.setGarbageNo(idgenUtil.generateGarbageNumber(requestInfo, app.getTenantId()));
            }

            // Audit details
            AuditDetails auditDetails = AuditDetails.builder()
                    .createdBy(userUuid)
                    .createdTime(now)
                    .lastModifiedBy(userUuid)
                    .lastModifiedTime(now)
                    .build();
            app.setAuditDetails(auditDetails);

            // User Resolution & Creation (referencing tp-services pattern)
            ApplicantDetail primaryApplicant = !CollectionUtils.isEmpty(app.getApplicantDetails())
                    ? app.getApplicantDetails().get(0)
                    : null;

            boolean isOwnerApp = Boolean.TRUE.equals(app.getIsOwner());

            if (isOwnerApp) {
                commonPropertyId = app.getPropertyId();
                commonTenantId = app.getTenantId();
                commonPropertyLocation = app.getPropertyLocation();

                if (primaryApplicant != null && StringUtils.isNotBlank(primaryApplicant.getMobileNumber())) {
                    User ownerUser = userService.fetchExistingUser(app.getTenantId(), primaryApplicant, requestInfo);
                    if (ownerUser == null) {
                        ownerUser = userService.createUserHandler(requestInfo, primaryApplicant, app.getTenantId());
                    }
                    if (ownerUser != null) {
                        app.setOwnerUuid(ownerUser.getUuid());
                        commonOwnerUuid = ownerUser.getUuid();
                    }
                }
                if (commonOwnerUuid == null && StringUtils.isNotBlank(app.getOwnerUuid())) {
                    commonOwnerUuid = app.getOwnerUuid();
                }
                if (commonOwnerUuid == null && requestInfo.getUserInfo() != null && StringUtils.isNotBlank(requestInfo.getUserInfo().getUuid())) {
                    commonOwnerUuid = requestInfo.getUserInfo().getUuid();
                    if (StringUtils.isBlank(app.getOwnerUuid())) {
                        app.setOwnerUuid(commonOwnerUuid);
                    }
                }
            } else {
                // Tenant Application: inherit property context from owner application if missing
                if (StringUtils.isBlank(app.getPropertyId()) && commonPropertyId != null) {
                    app.setPropertyId(commonPropertyId);
                }
                if (StringUtils.isBlank(app.getTenantId()) && commonTenantId != null) {
                    app.setTenantId(commonTenantId);
                }
                if (app.getPropertyLocation() == null && commonPropertyLocation != null) {
                    app.setPropertyLocation(commonPropertyLocation);
                }
                if (app.getGarbageSpecification() != null && StringUtils.isBlank(app.getGarbageSpecification().getPropertyType())) {
                    app.getGarbageSpecification().setPropertyType("TENANT");
                }

                if (primaryApplicant != null && StringUtils.isNotBlank(primaryApplicant.getMobileNumber())) {
                    User tenantUser = userService.fetchExistingUser(app.getTenantId(), primaryApplicant, requestInfo);
                    if (tenantUser == null) {
                        tenantUser = userService.createUserHandler(requestInfo, primaryApplicant, app.getTenantId());
                    }
                    if (tenantUser != null) {
                        app.setTenantUuid(tenantUser.getUuid());
                    }
                }
                if (StringUtils.isBlank(app.getOwnerUuid()) && commonOwnerUuid != null) {
                    app.setOwnerUuid(commonOwnerUuid);
                }
            }

            // Dynamically enrich property address details
            propertyService.enrichApplicationWithPropertyDetails(app, requestInfo);

            // Enrich Specification
            if (app.getGarbageSpecification() != null) {
                GarbageSpecification spec = app.getGarbageSpecification();
                if (StringUtils.isBlank(spec.getId())) {
                    spec.setId(UUID.randomUUID().toString());
                }
                spec.setGarbageId(app.getId());
                spec.setAuditDetails(auditDetails);
            }

            // Enrich Documents
            if (!CollectionUtils.isEmpty(app.getDocuments())) {
                for (GarbageDocument doc : app.getDocuments()) {
                    if (StringUtils.isBlank(doc.getDocumentDetailId())) {
                        doc.setDocumentDetailId(UUID.randomUUID().toString());
                    }
                    doc.setGarbageId(app.getId());
                    doc.setAuditDetails(auditDetails);
                }
            }

            // Default status & workflow transition
            if (StringUtils.isBlank(app.getStatus())) {
                app.setStatus("APPLICATION_CREATED");
            }
            if (app.getWorkflow() == null) {
                app.setWorkflow(WorkflowRequest.builder().action("APPLY").build());
            }
            workflowService.updateWorkflowStatus(requestInfo, app);
        }

        // Persist to Kafka
        garbageRepository.saveGarbageApplication(garbageRequest);

        // Transactional SMS Notifications
        notificationService.processApplyNotification(garbageRequest);

        ResponseInfo responseInfo = responseInfoFactory.createResponseInfoFromRequestInfo(requestInfo, true);
        return GarbageResponse.of(responseInfo, garbageRequest.getGarbageApplications());
    }

    @Override
    public GarbageResponse update(GarbageRequest garbageRequest) {
        log.info("Processing update GarbageRequest for applications count: {}",
                garbageRequest.getGarbageApplications() != null ? garbageRequest.getGarbageApplications().size() : 0);

        validateUpdateRequest(garbageRequest);
        RequestInfo requestInfo = garbageRequest.getRequestInfo();
        String userUuid = requestInfo.getUserInfo() != null ? requestInfo.getUserInfo().getUuid() : "SYSTEM";
        long now = Instant.now().toEpochMilli();

        for (GarbageApplication app : garbageRequest.getGarbageApplications()) {
            GarbageSearchCriteria searchCriteria = GarbageSearchCriteria.builder()
                    .tenantId(app.getTenantId())
                    .applicationNumber(Collections.singletonList(app.getApplicationNo()))
                    .build();

            List<GarbageApplication> existingApps = garbageRepository.getGarbageApplications(searchCriteria);
            if (CollectionUtils.isEmpty(existingApps)) {
                throw new CustomException("APPLICATION_NOT_FOUND", "No application found with applicationNo: " + app.getApplicationNo());
            }

            GarbageApplication existingApp = existingApps.get(0);

            // Guard against modifying permanently disconnected applications
            if ("PERMANENTLY_DISCONNECTED".equalsIgnoreCase(existingApp.getStatus())) {
                throw new CustomException("APPLICATION_TERMINATED", "Cannot modify permanently disconnected garbage application: " + app.getApplicationNo());
            }

            app.setId(existingApp.getId());
            app.setOwnerUuid(existingApp.getOwnerUuid());
            app.setTenantUuid(existingApp.getTenantUuid());

            AuditDetails auditDetails = existingApp.getAuditDetails() != null ? existingApp.getAuditDetails() : AuditDetails.builder().build();
            auditDetails.setLastModifiedBy(userUuid);
            auditDetails.setLastModifiedTime(now);
            app.setAuditDetails(auditDetails);

            String action = app.getWorkflow() != null ? app.getWorkflow().getAction() : null;

            if ("DISCONNECT".equalsIgnoreCase(action)) {
                validateOwnerAuthorization(existingApp, requestInfo);

                GarbageConnectionLog connLog = GarbageConnectionLog.builder()
                        .id(UUID.randomUUID().toString())
                        .garbageId(existingApp.getId())
                        .applicationNo(existingApp.getApplicationNo())
                        .requestNo(idgenUtil.generateRequestNumber(requestInfo, existingApp.getTenantId(), "DISCONNECTION"))
                        .status("APPROVED")
                        .reason(app.getWorkflow().getComments() != null ? app.getWorkflow().getComments() : "TEMPORARY_DISCONNECTION")
                        .requestType("DISCONNECTION")
                        .reasonDetails(app.getWorkflow().getComments())
                        .effectiveFrom(LocalDate.now())
                        .feeCharged(BigDecimal.ZERO)
                        .auditDetails(AuditDetails.builder().createdBy(userUuid).createdTime(now).build())
                        .build();

                app.setStatus("DISCONNECTED");
                app.setConnectionStatus("DISCONNECTED");
                garbageRepository.saveConnectionLog(connLog);
                notificationService.processDisconnectNotification(existingApp, LocalDate.now());
                log.info("Processed DISCONNECT for applicationNo: {}", existingApp.getApplicationNo());

            } else if ("RECONNECT".equalsIgnoreCase(action)) {
                validateOwnerAuthorization(existingApp, requestInfo);
                validateArrearsCleared(existingApp, requestInfo);

                GarbageConnectionLog connLog = GarbageConnectionLog.builder()
                        .id(UUID.randomUUID().toString())
                        .garbageId(existingApp.getId())
                        .applicationNo(existingApp.getApplicationNo())
                        .requestNo(idgenUtil.generateRequestNumber(requestInfo, existingApp.getTenantId(), "RECONNECTION"))
                        .status("APPROVED")
                        .reason(app.getWorkflow().getComments() != null ? app.getWorkflow().getComments() : "SERVICE_RECONNECTION")
                        .requestType("RECONNECTION")
                        .reasonDetails(app.getWorkflow().getComments())
                        .effectiveFrom(LocalDate.now())
                        .feeCharged(BigDecimal.ZERO)
                        .auditDetails(AuditDetails.builder().createdBy(userUuid).createdTime(now).build())
                        .build();

                app.setStatus("ACTIVE");
                app.setConnectionStatus("ACTIVE");
                garbageRepository.saveConnectionLog(connLog);
                notificationService.processReconnectNotification(existingApp);
                log.info("Processed RECONNECT for applicationNo: {}", existingApp.getApplicationNo());

            } else if ("PERMANENT_DISCONNECT".equalsIgnoreCase(action)) {
                validateOwnerAuthorization(existingApp, requestInfo);

                GarbageConnectionLog connLog = GarbageConnectionLog.builder()
                        .id(UUID.randomUUID().toString())
                        .garbageId(existingApp.getId())
                        .applicationNo(existingApp.getApplicationNo())
                        .requestNo(idgenUtil.generateRequestNumber(requestInfo, existingApp.getTenantId(), "PERMANENT_DISCONNECTION"))
                        .status("APPROVED")
                        .reason(app.getWorkflow().getComments() != null ? app.getWorkflow().getComments() : "PERMANENT_DISCONNECTION")
                        .requestType("PERMANENT_DISCONNECTION")
                        .reasonDetails(app.getWorkflow().getComments())
                        .effectiveFrom(LocalDate.now())
                        .feeCharged(BigDecimal.ZERO)
                        .auditDetails(AuditDetails.builder().createdBy(userUuid).createdTime(now).build())
                        .build();

                app.setStatus("PERMANENTLY_DISCONNECTED");
                app.setConnectionStatus("PERMANENTLY_DISCONNECTED");
                garbageRepository.saveConnectionLog(connLog);
                notificationService.processDisconnectNotification(existingApp, LocalDate.now());
                log.info("Processed PERMANENT_DISCONNECT for applicationNo: {}", existingApp.getApplicationNo());

            } else if (app.getWorkflow() != null && StringUtils.isNotBlank(action)) {
                if (GrbgConstants.WORKFLOW_ACTION_DISCONNECT_INITIATE.equalsIgnoreCase(action)
                        || GrbgConstants.BUSINESS_SERVICE_DISCONNECTION.equalsIgnoreCase(app.getBusinessService())
                        || GrbgConstants.BUSINESS_SERVICE_DISCONNECTION.equalsIgnoreCase(existingApp.getBusinessService())) {
                    app.setBusinessService(GrbgConstants.BUSINESS_SERVICE_DISCONNECTION);
                }
                workflowService.updateWorkflowStatus(requestInfo, app);
                if ("APPROVED".equalsIgnoreCase(app.getStatus()) || "ACTIVE".equalsIgnoreCase(app.getStatus())) {
                    notificationService.processApprovalNotification(app);

                    // Section 2.2: Parent-child model: parent APPROVED automatically approves all children and sets connection_status = ACTIVE
                    if (existingApp.getParentApplicationNo() == null && StringUtils.isNotBlank(existingApp.getApplicationNo())) {
                        GarbageSearchCriteria childCrit = GarbageSearchCriteria.builder()
                                .tenantId(existingApp.getTenantId())
                                .propertyId(Collections.singletonList(existingApp.getPropertyId()))
                                .build();
                        List<GarbageApplication> propApps = garbageRepository.getGarbageApplications(childCrit);
                        List<GarbageApplication> childrenToApprove = propApps.stream()
                                .filter(c -> existingApp.getApplicationNo().equalsIgnoreCase(c.getParentApplicationNo()) && !"APPROVED".equalsIgnoreCase(c.getStatus()))
                                .collect(Collectors.toList());
                        if (!childrenToApprove.isEmpty()) {
                            for (GarbageApplication child : childrenToApprove) {
                                child.setStatus("APPROVED");
                                child.setConnectionStatus("ACTIVE");
                                if (child.getAuditDetails() != null) {
                                    child.getAuditDetails().setLastModifiedBy(userUuid);
                                    child.getAuditDetails().setLastModifiedTime(now);
                                }
                            }
                            GarbageRequest childUpdReq = GarbageRequest.builder()
                                    .requestInfo(requestInfo)
                                    .garbageApplications(childrenToApprove)
                                    .build();
                            garbageRepository.updateGarbageApplication(childUpdReq);
                        }
                    }
                } else if ("DISCONNECTED".equalsIgnoreCase(app.getStatus())) {
                    GarbageConnectionLog connLog = GarbageConnectionLog.builder()
                            .id(UUID.randomUUID().toString())
                            .garbageId(existingApp.getId())
                            .applicationNo(existingApp.getApplicationNo())
                            .requestNo(idgenUtil.generateRequestNumber(requestInfo, existingApp.getTenantId(), "DISCONNECTION"))
                            .status("APPROVED")
                            .reason(app.getWorkflow().getComments() != null ? app.getWorkflow().getComments() : "WORKFLOW_DISCONNECTION")
                            .requestType("DISCONNECTION")
                            .reasonDetails(app.getWorkflow().getComments())
                            .effectiveFrom(LocalDate.now())
                            .feeCharged(BigDecimal.ZERO)
                            .auditDetails(AuditDetails.builder().createdBy(userUuid).createdTime(now).build())
                            .build();
                    garbageRepository.saveConnectionLog(connLog);
                    notificationService.processDisconnectNotification(existingApp, LocalDate.now());
                }
            }
        }

        garbageRepository.updateGarbageApplication(garbageRequest);
        ResponseInfo responseInfo = responseInfoFactory.createResponseInfoFromRequestInfo(requestInfo, true);
        return GarbageResponse.of(responseInfo, garbageRequest.getGarbageApplications());
    }

    @Override
    public GarbageResponse search(GarbageSearchCriteria criteria, RequestInfo requestInfo) {
        log.info("Searching garbage applications with criteria: {}", criteria);

        if (criteria == null) {
            criteria = new GarbageSearchCriteria();
        }

        // Persona Search Routing (HLD Section 17 & 18)
        if (StringUtils.isNotBlank(criteria.getMobileNumber()) && StringUtils.isBlank(criteria.getOwnerUuid()) && StringUtils.isBlank(criteria.getTenantUuid())) {
            List<String> userUuids = userService.searchUserUuidsByMobile(criteria.getTenantId(), criteria.getMobileNumber(), requestInfo);
            if (userUuids.isEmpty() && requestInfo != null && requestInfo.getUserInfo() != null
                    && criteria.getMobileNumber().equals(requestInfo.getUserInfo().getMobileNumber())) {
                userUuids = Collections.singletonList(requestInfo.getUserInfo().getUuid());
            }
            if (!userUuids.isEmpty()) {
                String matchedUuid = userUuids.get(0);
                if (Boolean.TRUE.equals(criteria.getIsOwner())) {
                    criteria.setOwnerUuid(matchedUuid);
                } else {
                    criteria.setOwnerUuid(matchedUuid);
                    criteria.setTenantUuid(matchedUuid);
                }
            }
        } else if (StringUtils.isBlank(criteria.getMobileNumber()) && StringUtils.isBlank(criteria.getOwnerUuid()) && StringUtils.isBlank(criteria.getTenantUuid())) {
            if (requestInfo != null && requestInfo.getUserInfo() != null && StringUtils.isNotBlank(requestInfo.getUserInfo().getUuid())) {
                boolean isCitizen = requestInfo.getUserInfo().getRoles() != null && requestInfo.getUserInfo().getRoles().stream()
                        .anyMatch(r -> "CITIZEN".equalsIgnoreCase(r.getCode()));
                if (isCitizen) {
                    String callerUuid = requestInfo.getUserInfo().getUuid();
                    criteria.setOwnerUuid(callerUuid);
                    criteria.setTenantUuid(callerUuid);
                }
            }
        }

        List<GarbageApplication> applications = garbageRepository.getGarbageApplications(criteria);
        if (CollectionUtils.isEmpty(applications)) {
            ResponseInfo responseInfo = responseInfoFactory.createResponseInfoFromRequestInfo(requestInfo, true);
            return GarbageResponse.of(responseInfo, Collections.emptyList());
        }

        // Dynamically enrich contact and property details
        for (GarbageApplication application : applications) {
            userService.enrichApplicationWithUserDetails(application, criteria);
            propertyService.enrichApplicationWithPropertyDetails(application, requestInfo);
        }

        // Link child tenant applications into owner's childGarbageApplications
        Map<String, List<GarbageApplication>> tenantAppsByProp = applications.stream()
                .filter(a -> a.getGarbageSpecification() != null && "TENANT".equalsIgnoreCase(a.getGarbageSpecification().getPropertyType()))
                .filter(a -> StringUtils.isNotBlank(a.getPropertyId()))
                .collect(Collectors.groupingBy(GarbageApplication::getPropertyId));

        for (GarbageApplication app : applications) {
            if (app.getGarbageSpecification() != null && "OWNER".equalsIgnoreCase(app.getGarbageSpecification().getPropertyType())) {
                List<GarbageApplication> children = tenantAppsByProp.get(app.getPropertyId());
                if (!CollectionUtils.isEmpty(children)) {
                    app.setChildGarbageApplications(new ArrayList<>(children));
                }
            }
        }

        ResponseInfo responseInfo = responseInfoFactory.createResponseInfoFromRequestInfo(requestInfo, true);
        return GarbageResponse.of(responseInfo, applications);
    }

    @Override
    public Integer getApplicationsCount(GarbageSearchCriteria criteria, RequestInfo requestInfo) {
        return garbageRepository.getApplicationsCount(criteria);
    }

    private void validateOwnerAuthorization(GarbageApplication existingApp, RequestInfo requestInfo) {
        if (requestInfo == null || requestInfo.getUserInfo() == null) {
            return;
        }

        String callerUuid = requestInfo.getUserInfo().getUuid();
        String callerMobile = requestInfo.getUserInfo().getMobileNumber();

        if (!CollectionUtils.isEmpty(requestInfo.getUserInfo().getRoles())) {
            boolean isStaff = requestInfo.getUserInfo().getRoles().stream()
                    .map(Role::getCode)
                    .anyMatch(r -> r.startsWith("GC_") || "EMPLOYEE".equalsIgnoreCase(r));
            if (isStaff) {
                return;
            }
        }

        if (StringUtils.isNotBlank(existingApp.getOwnerUuid()) && existingApp.getOwnerUuid().equals(callerUuid)) {
            return;
        }

        if (StringUtils.isNotBlank(callerMobile) && StringUtils.isNotBlank(existingApp.getOwnerUuid())) {
            User ownerUser = userService.fetchExistingUserByMobile(existingApp.getTenantId(), callerMobile, requestInfo);
            if (ownerUser != null && existingApp.getOwnerUuid().equals(ownerUser.getUuid())) {
                return;
            }
        }

        log.warn("Unauthorized connection action attempt by user {} on application {}", callerUuid, existingApp.getApplicationNo());
        throw new CustomException("UNAUTHORIZED_CONNECTION_ACTION",
                "Disconnection, Reconnection, and Permanent Disconnection can ONLY be performed by the registered PROPERTY OWNER.");
    }

    private void validateArrearsCleared(GarbageApplication app, RequestInfo requestInfo) {
        try {
            List<Demand> demands = demandRepository.searchAllDemands(requestInfo, app.getTenantId(),
                    app.getApplicationNo(), app.getBusinessService(), true);
            if (!CollectionUtils.isEmpty(demands)) {
                for (Demand demand : demands) {
                    if ("ACTIVE".equalsIgnoreCase(demand.getStatus().toString())) {
                        BigDecimal totalDue = BigDecimal.ZERO;
                        if (!CollectionUtils.isEmpty(demand.getDemandDetails())) {
                            for (org.egov.garbageservice.web.models.billing.DemandDetail detail : demand.getDemandDetails()) {
                                BigDecimal due = detail.getTaxAmount().subtract(detail.getCollectionAmount());
                                if (due.compareTo(BigDecimal.ZERO) > 0) {
                                    totalDue = totalDue.add(due);
                                }
                            }
                        }
                        if (totalDue.compareTo(BigDecimal.ZERO) > 0) {
                            throw new CustomException("PENDING_ARREARS_EXIST",
                                    "Cannot reconnect service with pending unpaid arrears of Rs " + totalDue.toPlainString());
                        }
                    }
                }
            }
        } catch (CustomException ce) {
            throw ce;
        } catch (Exception e) {
            log.warn("Arrears check failed: {}", e.getMessage());
        }
    }

    private List<GarbageApplication> flattenApplications(List<GarbageApplication> applications) {
        if (CollectionUtils.isEmpty(applications)) {
            return Collections.emptyList();
        }
        List<GarbageApplication> flattened = new ArrayList<>();
        for (GarbageApplication app : applications) {
            flattened.add(app);

            // 1. Process childGarbageApplications (nested tenant applications)
            if (!CollectionUtils.isEmpty(app.getChildGarbageApplications())) {
                for (GarbageApplication child : app.getChildGarbageApplications()) {
                    enrichChildFromParent(app, child);
                    flattened.add(child);
                }
            }

            // 2. Also process tenants array if provided in additionalDetail
            if (app.getAdditionalDetail() != null && app.getAdditionalDetail().has("tenants")) {
                com.fasterxml.jackson.databind.JsonNode tenantsNode = app.getAdditionalDetail().get("tenants");
                if (tenantsNode.isArray() && !tenantsNode.isEmpty()) {
                    for (com.fasterxml.jackson.databind.JsonNode tNode : tenantsNode) {
                        String tName = tNode.has("name") ? tNode.get("name").asText() : "";
                        String tMobile = tNode.has("mobileNumber") ? tNode.get("mobileNumber").asText() : "";
                        String tEmail = tNode.has("email") ? tNode.get("email").asText() : (tNode.has("emailId") ? tNode.get("emailId").asText() : "");
                        String tFloor = tNode.has("floorNo") ? tNode.get("floorNo").asText() : (tNode.has("floor") ? tNode.get("floor").asText() : "1");

                        if (StringUtils.isNotBlank(tMobile)) {
                            GarbageApplication child = new GarbageApplication();
                            child.setApplicantDetails(Collections.singletonList(ApplicantDetail.builder()
                                    .applicantName(tName)
                                    .name(tName)
                                    .mobileNumber(tMobile)
                                    .emailId(tEmail)
                                    .build()));
                            child.setFloorNo(tFloor);
                            enrichChildFromParent(app, child);
                            flattened.add(child);
                        }
                    }
                }
            }
        }
        return flattened;
    }

    private void enrichChildFromParent(GarbageApplication parent, GarbageApplication child) {
        if (StringUtils.isBlank(child.getPropertyId())) {
            child.setPropertyId(parent.getPropertyId());
        }
        if (StringUtils.isBlank(child.getTenantId())) {
            child.setTenantId(parent.getTenantId());
        }
        if (child.getPropertyLocation() == null) {
            child.setPropertyLocation(parent.getPropertyLocation());
        }
        if (child.getGarbageSpecification() == null) {
            if (parent.getGarbageSpecification() != null) {
                child.setGarbageSpecification(parent.getGarbageSpecification().toBuilder()
                        .id(null)
                        .propertyType("TENANT")
                        .floorNo(child.getFloorNo())
                        .build());
            } else {
                child.setGarbageSpecification(GarbageSpecification.builder()
                        .propertyType("TENANT")
                        .floorNo(child.getFloorNo())
                        .build());
            }
        } else {
            child.getGarbageSpecification().setPropertyType("TENANT");
            if (StringUtils.isNotBlank(child.getFloorNo())) {
                child.getGarbageSpecification().setFloorNo(child.getFloorNo());
            }
            if (parent.getGarbageSpecification() != null) {
                if (StringUtils.isBlank(child.getGarbageSpecification().getCollectionType())) {
                    child.getGarbageSpecification().setCollectionType(parent.getGarbageSpecification().getCollectionType());
                }
                if (StringUtils.isBlank(child.getGarbageSpecification().getCategory())) {
                    child.getGarbageSpecification().setCategory(parent.getGarbageSpecification().getCategory());
                }
                if (StringUtils.isBlank(child.getGarbageSpecification().getSubCategory())) {
                    child.getGarbageSpecification().setSubCategory(parent.getGarbageSpecification().getSubCategory());
                }
                if (StringUtils.isBlank(child.getGarbageSpecification().getSubCategoryType())) {
                    child.getGarbageSpecification().setSubCategoryType(parent.getGarbageSpecification().getSubCategoryType());
                }
            }
        }
        if (child.getWorkflow() == null && parent.getWorkflow() != null) {
            child.setWorkflow(WorkflowRequest.builder().action(parent.getWorkflow().getAction()).build());
        }
    }

    @Override
    public GarbageConnectionResponse disconnect(GarbageConnectionRequest connectionRequest) {
        validateConnectionRequest(connectionRequest);
        RequestInfo requestInfo = connectionRequest.getRequestInfo();
        GarbageConnectionLog logInput = connectionRequest.getConnectionLog();
        String callerUuid = requestInfo.getUserInfo() != null ? requestInfo.getUserInfo().getUuid() : null;

        List<GarbageApplication> targets = resolveTargetApplications(connectionRequest);
        if (CollectionUtils.isEmpty(targets)) {
            throw new CustomException("INVALID_TARGET", "No valid target applications found for disconnection.");
        }

        // Section 7: Owner guard, state validation, and arrears checks
        for (GarbageApplication app : targets) {
            if (callerUuid != null && !callerUuid.equalsIgnoreCase(app.getOwnerUuid())) {
                throw new CustomException("UNAUTHORIZED_CONNECTION_ACTION", "Only property owner can disconnect connections.");
            }
            if ("PERMANENTLY_DISCONNECTED".equalsIgnoreCase(app.getConnectionStatus())) {
                throw new CustomException("TARGET_TERMINATED", "Cannot disconnect permanently terminated connection: " + app.getApplicationNo());
            }
            validateArrearsCleared(app, requestInfo);
        }

        long now = System.currentTimeMillis();
        String tenantId = targets.get(0).getTenantId();
        String requestNo = idgenUtil.generateRequestNumber(requestInfo, tenantId, "DISCONNECTION");
        String logId = UUID.randomUUID().toString();

        List<GarbageConnectionLogTarget> logTargets = new ArrayList<>();
        for (GarbageApplication app : targets) {
            logTargets.add(GarbageConnectionLogTarget.builder()
                    .id(UUID.randomUUID().toString())
                    .connectionLogId(logId)
                    .garbageId(app.getId())
                    .applicationNo(app.getApplicationNo())
                    .prevConnectionStatus(app.getConnectionStatus())
                    .createdTime(now)
                    .build());

            app.setConnectionStatus("DISCONNECTED");
            if (app.getAuditDetails() != null) {
                app.getAuditDetails().setLastModifiedBy(callerUuid);
                app.getAuditDetails().setLastModifiedTime(now);
            }
        }

        GarbageConnectionLog savedLog = GarbageConnectionLog.builder()
                .id(logId)
                .garbageId(targets.get(0).getId())
                .applicationNo(targets.get(0).getApplicationNo())
                .requestNo(requestNo)
                .requestType("DISCONNECTION")
                .scope(logInput.getScope() != null ? logInput.getScope() : "SELECTED")
                .propertyId(targets.get(0).getPropertyId())
                .status("APPROVED")
                .reason(logInput.getReason() != null ? logInput.getReason() : "DIRECT_DISCONNECTION")
                .reasonDetails(logInput.getReasonDetails())
                .effectiveFrom(logInput.getEffectiveFrom() != null ? logInput.getEffectiveFrom() : LocalDate.now())
                .effectiveTo(logInput.getEffectiveTo())
                .feeCharged(logInput.getFeeCharged() != null ? logInput.getFeeCharged() : BigDecimal.ZERO)
                .requestedByUuid(callerUuid)
                .approvedByUuid(callerUuid)
                .approvedTime(now)
                .targets(logTargets)
                .auditDetails(AuditDetails.builder().createdBy(callerUuid).createdTime(now).build())
                .build();

        garbageRepository.saveConnectionLog(savedLog);

        GarbageRequest updateRequest = GarbageRequest.builder()
                .requestInfo(requestInfo)
                .garbageApplications(targets)
                .build();
        garbageRepository.updateGarbageApplication(updateRequest);

        for (GarbageApplication app : targets) {
            notificationService.processDisconnectNotification(app, savedLog.getEffectiveFrom());
        }

        ResponseInfo responseInfo = responseInfoFactory.createResponseInfoFromRequestInfo(requestInfo, true);
        return GarbageConnectionResponse.builder()
                .responseInfo(responseInfo)
                .connectionLog(savedLog)
                .affectedApplications(targets)
                .build();
    }

    @Override
    public GarbageConnectionResponse reconnect(GarbageConnectionRequest connectionRequest) {
        validateConnectionRequest(connectionRequest);
        RequestInfo requestInfo = connectionRequest.getRequestInfo();
        GarbageConnectionLog logInput = connectionRequest.getConnectionLog();
        String callerUuid = requestInfo.getUserInfo() != null ? requestInfo.getUserInfo().getUuid() : null;

        List<GarbageApplication> targets = resolveTargetApplications(connectionRequest);
        if (CollectionUtils.isEmpty(targets)) {
            throw new CustomException("INVALID_TARGET", "No valid target applications found for reconnection.");
        }

        for (GarbageApplication app : targets) {
            if (callerUuid != null && !callerUuid.equalsIgnoreCase(app.getOwnerUuid())) {
                throw new CustomException("UNAUTHORIZED_CONNECTION_ACTION", "Only property owner can reconnect connections.");
            }
            if ("PERMANENTLY_DISCONNECTED".equalsIgnoreCase(app.getConnectionStatus())) {
                throw new CustomException("TARGET_TERMINATED", "Cannot reconnect permanently terminated connection: " + app.getApplicationNo());
            }
            validateArrearsCleared(app, requestInfo);
        }

        long now = System.currentTimeMillis();
        String tenantId = targets.get(0).getTenantId();
        String requestNo = idgenUtil.generateRequestNumber(requestInfo, tenantId, "RECONNECTION");
        String logId = UUID.randomUUID().toString();

        List<GarbageConnectionLogTarget> logTargets = new ArrayList<>();
        for (GarbageApplication app : targets) {
            logTargets.add(GarbageConnectionLogTarget.builder()
                    .id(UUID.randomUUID().toString())
                    .connectionLogId(logId)
                    .garbageId(app.getId())
                    .applicationNo(app.getApplicationNo())
                    .prevConnectionStatus(app.getConnectionStatus())
                    .createdTime(now)
                    .build());
        }

        GarbageConnectionLog savedLog = GarbageConnectionLog.builder()
                .id(logId)
                .garbageId(targets.get(0).getId())
                .applicationNo(targets.get(0).getApplicationNo())
                .requestNo(requestNo)
                .requestType("RECONNECTION")
                .scope(logInput.getScope() != null ? logInput.getScope() : "SELECTED")
                .propertyId(targets.get(0).getPropertyId())
                .status("INITIATED")
                .workflowStatus("PENDING_FOR_VERIFICATION")
                .reason(logInput.getReason() != null ? logInput.getReason() : "RECONNECTION_REQUEST")
                .reasonDetails(logInput.getReasonDetails())
                .effectiveFrom(logInput.getEffectiveFrom() != null ? logInput.getEffectiveFrom() : LocalDate.now())
                .effectiveTo(logInput.getEffectiveTo())
                .feeCharged(logInput.getFeeCharged() != null ? logInput.getFeeCharged() : BigDecimal.ZERO)
                .requestedByUuid(callerUuid)
                .requestSnapshot(logInput.getRequestSnapshot())
                .targets(logTargets)
                .auditDetails(AuditDetails.builder().createdBy(callerUuid).createdTime(now).build())
                .build();

        garbageRepository.saveConnectionLog(savedLog);

        ResponseInfo responseInfo = responseInfoFactory.createResponseInfoFromRequestInfo(requestInfo, true);
        return GarbageConnectionResponse.builder()
                .responseInfo(responseInfo)
                .connectionLog(savedLog)
                .affectedApplications(targets)
                .build();
    }

    @Override
    public GarbageConnectionResponse permanentDisconnect(GarbageConnectionRequest connectionRequest) {
        validateConnectionRequest(connectionRequest);
        RequestInfo requestInfo = connectionRequest.getRequestInfo();
        GarbageConnectionLog logInput = connectionRequest.getConnectionLog();
        String callerUuid = requestInfo.getUserInfo() != null ? requestInfo.getUserInfo().getUuid() : null;

        List<GarbageApplication> targets = resolveTargetApplications(connectionRequest);
        if (CollectionUtils.isEmpty(targets)) {
            throw new CustomException("INVALID_TARGET", "No valid target applications found for permanent disconnection.");
        }

        for (GarbageApplication app : targets) {
            if (callerUuid != null && !callerUuid.equalsIgnoreCase(app.getOwnerUuid())) {
                throw new CustomException("UNAUTHORIZED_CONNECTION_ACTION", "Only property owner can initiate permanent disconnection.");
            }
            if ("PERMANENTLY_DISCONNECTED".equalsIgnoreCase(app.getConnectionStatus())) {
                throw new CustomException("TARGET_TERMINATED", "Connection already permanently disconnected: " + app.getApplicationNo());
            }
            validateArrearsCleared(app, requestInfo);

            // Section 7 Rule: Parent permanent disconnection blocked if tenants are active unless scope is PROPERTY
            if (app.getParentApplicationNo() == null && !"PROPERTY".equalsIgnoreCase(logInput.getScope())) {
                GarbageSearchCriteria childCrit = GarbageSearchCriteria.builder()
                        .tenantId(app.getTenantId())
                        .propertyId(Collections.singletonList(app.getPropertyId()))
                        .build();
                List<GarbageApplication> propApps = garbageRepository.getGarbageApplications(childCrit);
                boolean hasActiveChildren = propApps.stream()
                        .anyMatch(c -> app.getApplicationNo().equalsIgnoreCase(c.getParentApplicationNo())
                                && !"PERMANENTLY_DISCONNECTED".equalsIgnoreCase(c.getConnectionStatus())
                                && !"DISCONNECTED".equalsIgnoreCase(c.getConnectionStatus()));
                if (hasActiveChildren) {
                    throw new CustomException("INVALID_CONNECTION_ACTION",
                            "Parent connection cannot be permanently disconnected while child tenant connections are active. Disconnect tenants first or use scope PROPERTY.");
                }
            }
        }

        long now = System.currentTimeMillis();
        String tenantId = targets.get(0).getTenantId();
        String requestNo = idgenUtil.generateRequestNumber(requestInfo, tenantId, "PERMANENT_DISCONNECTION");
        String logId = UUID.randomUUID().toString();

        List<GarbageConnectionLogTarget> logTargets = new ArrayList<>();
        for (GarbageApplication app : targets) {
            logTargets.add(GarbageConnectionLogTarget.builder()
                    .id(UUID.randomUUID().toString())
                    .connectionLogId(logId)
                    .garbageId(app.getId())
                    .applicationNo(app.getApplicationNo())
                    .prevConnectionStatus(app.getConnectionStatus())
                    .createdTime(now)
                    .build());
        }

        GarbageConnectionLog savedLog = GarbageConnectionLog.builder()
                .id(logId)
                .garbageId(targets.get(0).getId())
                .applicationNo(targets.get(0).getApplicationNo())
                .requestNo(requestNo)
                .requestType("PERMANENT_DISCONNECTION")
                .scope(logInput.getScope() != null ? logInput.getScope() : "SELECTED")
                .propertyId(targets.get(0).getPropertyId())
                .status("INITIATED")
                .workflowStatus("PENDING_FOR_VERIFICATION")
                .reason(logInput.getReason() != null ? logInput.getReason() : "PERMANENT_DISCONNECTION_REQUEST")
                .reasonDetails(logInput.getReasonDetails())
                .effectiveFrom(logInput.getEffectiveFrom() != null ? logInput.getEffectiveFrom() : LocalDate.now())
                .effectiveTo(logInput.getEffectiveTo())
                .feeCharged(logInput.getFeeCharged() != null ? logInput.getFeeCharged() : BigDecimal.ZERO)
                .requestedByUuid(callerUuid)
                .targets(logTargets)
                .auditDetails(AuditDetails.builder().createdBy(callerUuid).createdTime(now).build())
                .build();

        garbageRepository.saveConnectionLog(savedLog);

        ResponseInfo responseInfo = responseInfoFactory.createResponseInfoFromRequestInfo(requestInfo, true);
        return GarbageConnectionResponse.builder()
                .responseInfo(responseInfo)
                .connectionLog(savedLog)
                .affectedApplications(targets)
                .build();
    }

    @Override
    public GarbageConnectionResponse updateConnection(GarbageConnectionRequest connectionRequest) {
        validateConnectionRequest(connectionRequest);
        RequestInfo requestInfo = connectionRequest.getRequestInfo();
        GarbageConnectionLog logInput = connectionRequest.getConnectionLog();
        String userUuid = requestInfo.getUserInfo() != null ? requestInfo.getUserInfo().getUuid() : null;
        long now = System.currentTimeMillis();

        List<GarbageApplication> targets = resolveTargetApplications(connectionRequest);
        String action = logInput.getStatus() != null ? logInput.getStatus() : "APPROVED";

        if ("APPROVED".equalsIgnoreCase(action)) {
            for (GarbageApplication app : targets) {
                if ("RECONNECTION".equalsIgnoreCase(logInput.getRequestType())) {
                    app.setConnectionStatus("ACTIVE");
                    notificationService.processReconnectNotification(app);
                } else if ("PERMANENT_DISCONNECTION".equalsIgnoreCase(logInput.getRequestType())) {
                    app.setConnectionStatus("PERMANENTLY_DISCONNECTED");
                    notificationService.processDisconnectNotification(app, LocalDate.now());
                }
                if (app.getAuditDetails() != null) {
                    app.getAuditDetails().setLastModifiedBy(userUuid);
                    app.getAuditDetails().setLastModifiedTime(now);
                }
            }

            GarbageRequest updateRequest = GarbageRequest.builder()
                    .requestInfo(requestInfo)
                    .garbageApplications(targets)
                    .build();
            garbageRepository.updateGarbageApplication(updateRequest);

            logInput.setStatus("APPROVED");
            logInput.setApprovedByUuid(userUuid);
            logInput.setApprovedTime(now);
        } else if ("REJECTED".equalsIgnoreCase(action)) {
            logInput.setStatus("REJECTED");
        }

        garbageRepository.saveConnectionLog(logInput);

        ResponseInfo responseInfo = responseInfoFactory.createResponseInfoFromRequestInfo(requestInfo, true);
        return GarbageConnectionResponse.builder()
                .responseInfo(responseInfo)
                .connectionLog(logInput)
                .affectedApplications(targets)
                .build();
    }

    @Override
    public List<GarbageConnectionLog> searchConnectionHistory(String propertyId, String applicationNo, RequestInfo requestInfo) {
        return garbageRepository.getConnectionLogs(propertyId, applicationNo);
    }

    private List<GarbageApplication> resolveTargetApplications(GarbageConnectionRequest request) {
        GarbageConnectionLog logInput = request.getConnectionLog();
        List<String> targetNos = request.getTargetApplicationNos();

        if ("PROPERTY".equalsIgnoreCase(logInput.getScope()) && StringUtils.isNotBlank(logInput.getPropertyId())) {
            GarbageSearchCriteria crit = GarbageSearchCriteria.builder()
                    .propertyId(Collections.singletonList(logInput.getPropertyId()))
                    .build();
            return garbageRepository.getGarbageApplications(crit);
        } else if (!CollectionUtils.isEmpty(targetNos)) {
            GarbageSearchCriteria crit = GarbageSearchCriteria.builder()
                    .applicationNumber(targetNos)
                    .build();
            return garbageRepository.getGarbageApplications(crit);
        } else if (StringUtils.isNotBlank(logInput.getApplicationNo())) {
            GarbageSearchCriteria crit = GarbageSearchCriteria.builder()
                    .applicationNumber(Collections.singletonList(logInput.getApplicationNo()))
                    .build();
            return garbageRepository.getGarbageApplications(crit);
        }
        return Collections.emptyList();
    }

    private void validateConnectionRequest(GarbageConnectionRequest request) {
        if (request == null || request.getRequestInfo() == null) {
            throw new CustomException("INVALID_REQUEST", "RequestInfo cannot be null");
        }
        if (request.getConnectionLog() == null) {
            throw new CustomException("INVALID_REQUEST", "connectionLog cannot be null");
        }
    }

    private void validateCreateRequest(GarbageRequest request) {
        if (request == null || request.getRequestInfo() == null) {
            throw new CustomException("INVALID_REQUEST", "RequestInfo cannot be null");
        }
        if (CollectionUtils.isEmpty(request.getGarbageApplications())) {
            throw new CustomException("INVALID_REQUEST", "garbageApplications list cannot be empty");
        }
        for (GarbageApplication app : request.getGarbageApplications()) {
            if (StringUtils.isBlank(app.getTenantId())) {
                throw new CustomException("INVALID_TENANT_ID", "TenantId is required for garbage application");
            }
            if (StringUtils.isBlank(app.getPropertyId())) {
                throw new CustomException("INVALID_PROPERTY_ID", "PropertyId is mandatory for garbage application");
            }
        }
    }

    private void validateUpdateRequest(GarbageRequest request) {
        if (request == null || request.getRequestInfo() == null) {
            throw new CustomException("INVALID_REQUEST", "RequestInfo cannot be null");
        }
        if (CollectionUtils.isEmpty(request.getGarbageApplications())) {
            throw new CustomException("INVALID_REQUEST", "garbageApplications list cannot be empty");
        }
        for (GarbageApplication app : request.getGarbageApplications()) {
            if (StringUtils.isBlank(app.getApplicationNo()) && app.getId() == null) {
                throw new CustomException("INVALID_APPLICATION", "ApplicationNo or ID is mandatory for update");
            }
        }
    }
}
