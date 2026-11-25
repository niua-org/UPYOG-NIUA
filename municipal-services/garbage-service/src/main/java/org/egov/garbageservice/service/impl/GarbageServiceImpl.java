package org.egov.garbageservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    @Autowired
    public GarbageServiceImpl(GarbageRepository garbageRepository, UserService userService,
                              PropertyService propertyService, NotificationService notificationService,
                              WorkflowService workflowService, DemandRepository demandRepository,
                              IdgenUtil idgenUtil, ResponseInfoFactory responseInfoFactory,
                              GarbageServiceConfig config, ObjectMapper objectMapper) {
        this.garbageRepository = garbageRepository;
        this.userService = userService;
        this.propertyService = propertyService;
        this.notificationService = notificationService;
        this.workflowService = workflowService;
        this.demandRepository = demandRepository;
        this.idgenUtil = idgenUtil;
        this.responseInfoFactory = responseInfoFactory;
        this.config = config;
        this.objectMapper = objectMapper;
    }

    @Override
    public GarbageResponse create(GarbageRequest garbageRequest) {
        if (garbageRequest != null && !CollectionUtils.isEmpty(garbageRequest.getGarbageApplications())) {
            garbageRequest.setGarbageApplications(flattenApplications(garbageRequest.getGarbageApplications()));
        }

        normalizeAndAllocateFloors(garbageRequest);

        log.info("Processing create GarbageRequest for applications count: {}",
                garbageRequest != null && garbageRequest.getGarbageApplications() != null ? garbageRequest.getGarbageApplications().size() : 0);

        validateCreateRequest(garbageRequest);
        RequestInfo requestInfo = garbageRequest.getRequestInfo();
        String userUuid = requestInfo.getUserInfo() != null ? requestInfo.getUserInfo().getUuid() : GrbgConstants.USER_TYPE_SYSTEM;
        long now = Instant.now().toEpochMilli();

        String commonOwnerUuid = null;
        String commonPropertyId = null;
        String commonTenantId = null;
        PropertyLocation commonPropertyLocation = null;
        String commonParentApplicationNo = null;

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
            if (isOwnerApp && commonParentApplicationNo == null) {
                commonParentApplicationNo = app.getApplicationNo();
            } else if (!isOwnerApp && StringUtils.isBlank(app.getParentApplicationNo()) && commonParentApplicationNo != null) {
                app.setParentApplicationNo(commonParentApplicationNo);
            }

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
                    app.getGarbageSpecification().setPropertyType(GrbgConstants.PROPERTY_TYPE_TENANT);
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

            if (primaryApplicant != null) {
                try {
                    com.fasterxml.jackson.databind.node.ObjectNode addObj;
                    if (app.getAdditionalDetail() != null && app.getAdditionalDetail().isObject()) {
                        addObj = (com.fasterxml.jackson.databind.node.ObjectNode) app.getAdditionalDetail();
                    } else {
                        addObj = objectMapper.createObjectNode();
                    }
                    if (!addObj.has("applicantDetails")) {
                        addObj.putPOJO("applicantDetails", app.getApplicantDetails());
                    }
                    if (!isOwnerApp && !addObj.has("tenantDetails")) {
                        addObj.putPOJO("tenantDetails", primaryApplicant);
                    }
                    app.setAdditionalDetail(addObj);
                } catch (Exception ex) {
                    log.warn("Failed to attach applicantDetails to additionalDetail: {}", ex.getMessage());
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
                    doc.setDocumentDetailId(UUID.randomUUID().toString());
                    doc.setGarbageId(app.getId());
                    doc.setAuditDetails(auditDetails);
                }
            }

            // Default status & workflow transition
            if (StringUtils.isBlank(app.getStatus())) {
                app.setStatus(GrbgConstants.STATUS_APPLICATION_CREATED);
            }
            if (app.getWorkflow() == null) {
                app.setWorkflow(WorkflowRequest.builder().action(GrbgConstants.ACTION_APPLY).build());
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
        String userUuid = requestInfo.getUserInfo() != null ? requestInfo.getUserInfo().getUuid() : GrbgConstants.USER_TYPE_SYSTEM;
        long now = Instant.now().toEpochMilli();

        for (GarbageApplication app : garbageRequest.getGarbageApplications()) {
            GarbageSearchCriteria searchCriteria = GarbageSearchCriteria.builder()
                    .tenantId(app.getTenantId())
                    .applicationNumber(Collections.singletonList(app.getApplicationNo()))
                    .build();

            List<GarbageApplication> existingApps = garbageRepository.getGarbageApplications(searchCriteria);
            if (CollectionUtils.isEmpty(existingApps)) {
                throw new CustomException(GrbgConstants.ERROR_APPLICATION_NOT_FOUND, "No application found with applicationNo: " + app.getApplicationNo());
            }

            GarbageApplication existingApp = existingApps.get(0);

            // Guard against modifying permanently disconnected applications
            if (GrbgConstants.STATUS_PERMANENTLY_DISCONNECTED.equalsIgnoreCase(existingApp.getStatus())) {
                throw new CustomException(GrbgConstants.ERROR_APPLICATION_TERMINATED, "Cannot modify permanently disconnected garbage application: " + app.getApplicationNo());
            }

            app.setId(existingApp.getId());
            app.setOwnerUuid(existingApp.getOwnerUuid());
            app.setTenantUuid(existingApp.getTenantUuid());

            AuditDetails auditDetails = existingApp.getAuditDetails() != null ? existingApp.getAuditDetails() : AuditDetails.builder().build();
            auditDetails.setLastModifiedBy(userUuid);
            auditDetails.setLastModifiedTime(now);
            app.setAuditDetails(auditDetails);

            String action = app.getWorkflow() != null ? app.getWorkflow().getAction() : null;

            if (GrbgConstants.ACTION_DISCONNECT.equalsIgnoreCase(action)) {
                validateOwnerAuthorization(existingApp, requestInfo);
                app.setBusinessService(GrbgConstants.BUSINESS_SERVICE_DISCONNECTION);
                workflowService.updateWorkflowStatus(requestInfo, app);
                if (StringUtils.isBlank(app.getStatus()) || GrbgConstants.STATUS_ACTIVE.equalsIgnoreCase(app.getStatus())) {
                    app.setStatus(GrbgConstants.STATUS_DISCONNECTED);
                    app.setConnectionStatus(GrbgConstants.STATUS_DISCONNECTED);
                } else if (GrbgConstants.STATUS_DISCONNECTED.equalsIgnoreCase(app.getStatus())) {
                    app.setConnectionStatus(GrbgConstants.STATUS_DISCONNECTED);
                }

                GarbageConnectionLog connLog = GarbageConnectionLog.builder()
                        .id(UUID.randomUUID().toString())
                        .garbageId(existingApp.getId())
                        .applicationNo(existingApp.getApplicationNo())
                        .requestNo(idgenUtil.generateRequestNumber(requestInfo, existingApp.getTenantId(), GrbgConstants.REQUEST_TYPE_DISCONNECTION))
                        .status(GrbgConstants.STATUS_APPROVED)
                        .reason(app.getWorkflow().getComments() != null ? app.getWorkflow().getComments() : GrbgConstants.REASON_TEMPORARY_DISCONNECTION)
                        .requestType(GrbgConstants.REQUEST_TYPE_DISCONNECTION)
                        .reasonDetails(app.getWorkflow().getComments())
                        .effectiveFrom(LocalDate.now())
                        .feeCharged(BigDecimal.ZERO)
                        .auditDetails(AuditDetails.builder().createdBy(userUuid).createdTime(now).build())
                        .build();

                garbageRepository.saveConnectionLog(connLog);
                notificationService.processDisconnectNotification(existingApp, LocalDate.now());
                log.info("Processed DISCONNECT for applicationNo: {}", existingApp.getApplicationNo());

            } else if (GrbgConstants.ACTION_RECONNECT.equalsIgnoreCase(action)) {
                validateOwnerAuthorization(existingApp, requestInfo);
                validateArrearsCleared(existingApp, requestInfo);

                GarbageConnectionLog connLog = GarbageConnectionLog.builder()
                        .id(UUID.randomUUID().toString())
                        .garbageId(existingApp.getId())
                        .applicationNo(existingApp.getApplicationNo())
                        .requestNo(idgenUtil.generateRequestNumber(requestInfo, existingApp.getTenantId(), GrbgConstants.REQUEST_TYPE_RECONNECTION))
                        .status(GrbgConstants.STATUS_APPROVED)
                        .reason(app.getWorkflow().getComments() != null ? app.getWorkflow().getComments() : GrbgConstants.REASON_SERVICE_RECONNECTION)
                        .requestType(GrbgConstants.REQUEST_TYPE_RECONNECTION)
                        .reasonDetails(app.getWorkflow().getComments())
                        .effectiveFrom(LocalDate.now())
                        .feeCharged(BigDecimal.ZERO)
                        .auditDetails(AuditDetails.builder().createdBy(userUuid).createdTime(now).build())
                        .build();

                app.setStatus(GrbgConstants.STATUS_ACTIVE);
                app.setConnectionStatus(GrbgConstants.STATUS_ACTIVE);
                garbageRepository.saveConnectionLog(connLog);
                notificationService.processReconnectNotification(existingApp);
                log.info("Processed RECONNECT for applicationNo: {}", existingApp.getApplicationNo());

            } else if (GrbgConstants.ACTION_PERMANENT_DISCONNECT.equalsIgnoreCase(action)) {
                validateOwnerAuthorization(existingApp, requestInfo);
                app.setBusinessService(GrbgConstants.BUSINESS_SERVICE_DISCONNECTION);
                workflowService.updateWorkflowStatus(requestInfo, app);
                if (StringUtils.isBlank(app.getStatus()) || GrbgConstants.STATUS_ACTIVE.equalsIgnoreCase(app.getStatus())) {
                    app.setStatus(GrbgConstants.STATUS_PERMANENTLY_DISCONNECTED);
                    app.setConnectionStatus(GrbgConstants.STATUS_PERMANENTLY_DISCONNECTED);
                } else if (GrbgConstants.STATUS_PERMANENTLY_DISCONNECTED.equalsIgnoreCase(app.getStatus())) {
                    app.setConnectionStatus(GrbgConstants.STATUS_PERMANENTLY_DISCONNECTED);
                }

                GarbageConnectionLog connLog = GarbageConnectionLog.builder()
                        .id(UUID.randomUUID().toString())
                        .garbageId(existingApp.getId())
                        .applicationNo(existingApp.getApplicationNo())
                        .requestNo(idgenUtil.generateRequestNumber(requestInfo, existingApp.getTenantId(), GrbgConstants.REQUEST_TYPE_PERMANENT_DISCONNECTION))
                        .status(GrbgConstants.STATUS_APPROVED)
                        .reason(app.getWorkflow().getComments() != null ? app.getWorkflow().getComments() : GrbgConstants.REQUEST_TYPE_PERMANENT_DISCONNECTION)
                        .requestType(GrbgConstants.REQUEST_TYPE_PERMANENT_DISCONNECTION)
                        .reasonDetails(app.getWorkflow().getComments())
                        .effectiveFrom(LocalDate.now())
                        .feeCharged(BigDecimal.ZERO)
                        .auditDetails(AuditDetails.builder().createdBy(userUuid).createdTime(now).build())
                        .build();

                garbageRepository.saveConnectionLog(connLog);
                notificationService.processDisconnectNotification(existingApp, LocalDate.now());
                log.info("Processed PERMANENT_DISCONNECT for applicationNo: {}", existingApp.getApplicationNo());

            } else if (app.getWorkflow() != null && StringUtils.isNotBlank(action)) {
                boolean isDiscAction = config.getDisconnectionActions() != null && config.getDisconnectionActions().stream().anyMatch(a -> a.equalsIgnoreCase(action));
                boolean isPermDiscAction = config.getPermanentDisconnectionActions() != null && config.getPermanentDisconnectionActions().stream().anyMatch(a -> a.equalsIgnoreCase(action));

                if (isDiscAction
                        || isPermDiscAction
                        || GrbgConstants.BUSINESS_SERVICE_DISCONNECTION.equalsIgnoreCase(app.getBusinessService())
                        || GrbgConstants.BUSINESS_SERVICE_DISCONNECTION.equalsIgnoreCase(existingApp.getBusinessService())) {
                    app.setBusinessService(GrbgConstants.BUSINESS_SERVICE_DISCONNECTION);
                }
                workflowService.updateWorkflowStatus(requestInfo, app);
                if (GrbgConstants.STATUS_APPROVED.equalsIgnoreCase(app.getStatus()) || GrbgConstants.STATUS_ACTIVE.equalsIgnoreCase(app.getStatus())) {
                    notificationService.processApprovalNotification(app);

                    // Section 2.2: Parent-child model: parent APPROVED automatically approves all children and sets connection_status = ACTIVE
                    if (existingApp.getParentApplicationNo() == null && StringUtils.isNotBlank(existingApp.getApplicationNo())) {
                        GarbageSearchCriteria childCrit = GarbageSearchCriteria.builder()
                                .tenantId(existingApp.getTenantId())
                                .propertyId(Collections.singletonList(existingApp.getPropertyId()))
                                .build();
                        List<GarbageApplication> propApps = garbageRepository.getGarbageApplications(childCrit);
                        List<GarbageApplication> childrenToApprove = propApps.stream()
                                .filter(c -> existingApp.getApplicationNo().equalsIgnoreCase(c.getParentApplicationNo()) && !GrbgConstants.STATUS_APPROVED.equalsIgnoreCase(c.getStatus()))
                                .collect(Collectors.toList());
                        if (!childrenToApprove.isEmpty()) {
                            for (GarbageApplication child : childrenToApprove) {
                                child.setStatus(GrbgConstants.STATUS_APPROVED);
                                child.setConnectionStatus(GrbgConstants.STATUS_ACTIVE);
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
                } else if (GrbgConstants.STATUS_DISCONNECTED.equalsIgnoreCase(app.getStatus())) {
                    app.setConnectionStatus(GrbgConstants.STATUS_DISCONNECTED);
                    GarbageConnectionLog connLog = GarbageConnectionLog.builder()
                            .id(UUID.randomUUID().toString())
                            .garbageId(existingApp.getId())
                            .applicationNo(existingApp.getApplicationNo())
                            .requestNo(idgenUtil.generateRequestNumber(requestInfo, existingApp.getTenantId(), GrbgConstants.REQUEST_TYPE_DISCONNECTION))
                            .status(GrbgConstants.STATUS_APPROVED)
                            .reason(app.getWorkflow().getComments() != null ? app.getWorkflow().getComments() : GrbgConstants.REASON_WORKFLOW_DISCONNECTION)
                            .requestType(GrbgConstants.REQUEST_TYPE_DISCONNECTION)
                            .reasonDetails(app.getWorkflow().getComments())
                            .effectiveFrom(LocalDate.now())
                            .feeCharged(BigDecimal.ZERO)
                            .auditDetails(AuditDetails.builder().createdBy(userUuid).createdTime(now).build())
                            .build();
                    garbageRepository.saveConnectionLog(connLog);
                    notificationService.processDisconnectNotification(existingApp, LocalDate.now());
                } else if (GrbgConstants.STATUS_PERMANENTLY_DISCONNECTED.equalsIgnoreCase(app.getStatus())) {
                    app.setConnectionStatus(GrbgConstants.STATUS_PERMANENTLY_DISCONNECTED);
                    GarbageConnectionLog connLog = GarbageConnectionLog.builder()
                            .id(UUID.randomUUID().toString())
                            .garbageId(existingApp.getId())
                            .applicationNo(existingApp.getApplicationNo())
                            .requestNo(idgenUtil.generateRequestNumber(requestInfo, existingApp.getTenantId(), GrbgConstants.REQUEST_TYPE_PERMANENT_DISCONNECTION))
                            .status(GrbgConstants.STATUS_APPROVED)
                            .reason(app.getWorkflow().getComments() != null ? app.getWorkflow().getComments() : GrbgConstants.REASON_WORKFLOW_PERMANENT_DISCONNECTION)
                            .requestType(GrbgConstants.REQUEST_TYPE_PERMANENT_DISCONNECTION)
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
        String resolvedOwnerType = criteria.getResolvedOwnerType();
        List<String> userUuids = Collections.emptyList();

        boolean hasDirectIdentifier = !CollectionUtils.isEmpty(criteria.getResolvedApplicationNumbers())
                || !CollectionUtils.isEmpty(criteria.getResolvedGarbageNumbers())
                || !CollectionUtils.isEmpty(criteria.getResolvedPropertyIds());

        if (!hasDirectIdentifier) {
            String mobileToSearch = StringUtils.isNotBlank(criteria.getMobileNumber())
                    ? criteria.getMobileNumber()
                    : (requestInfo != null && requestInfo.getUserInfo() != null ? requestInfo.getUserInfo().getMobileNumber() : null);

            if (StringUtils.isNotBlank(mobileToSearch) && CollectionUtils.isEmpty(criteria.getOwnerUuids()) && CollectionUtils.isEmpty(criteria.getTenantUuids())) {
                userUuids = userService.searchUserUuidsByMobile(criteria.getTenantId(), mobileToSearch, requestInfo);
                if (userUuids.isEmpty() && requestInfo != null && requestInfo.getUserInfo() != null
                        && mobileToSearch.equals(requestInfo.getUserInfo().getMobileNumber())) {
                    userUuids = Collections.singletonList(requestInfo.getUserInfo().getUuid());
                }
            } else if (CollectionUtils.isEmpty(criteria.getOwnerUuids()) && CollectionUtils.isEmpty(criteria.getTenantUuids())
                    && StringUtils.isBlank(criteria.getOwnerUuid()) && StringUtils.isBlank(criteria.getTenantUuid())) {
                if (requestInfo != null && requestInfo.getUserInfo() != null && StringUtils.isNotBlank(requestInfo.getUserInfo().getUuid())) {
                    boolean isCitizen = GrbgConstants.USER_TYPE_CITIZEN.equalsIgnoreCase(requestInfo.getUserInfo().getType());
                    if (isCitizen) {
                        userUuids = Collections.singletonList(requestInfo.getUserInfo().getUuid());
                    }
                }
            }

            if (!userUuids.isEmpty()) {
                if (GrbgConstants.PROPERTY_TYPE_TENANT.equalsIgnoreCase(resolvedOwnerType)) {
                    criteria.setTenantUuids(userUuids);
                    criteria.setTenantUuid(userUuids.get(0));
                    criteria.setOwnerUuids(null);
                    criteria.setOwnerUuid(null);
                } else if (GrbgConstants.PROPERTY_TYPE_OWNER.equalsIgnoreCase(resolvedOwnerType)) {
                    criteria.setOwnerUuids(userUuids);
                    criteria.setOwnerUuid(userUuids.get(0));
                    criteria.setTenantUuids(null);
                    criteria.setTenantUuid(null);
                } else {
                    criteria.setOwnerUuids(userUuids);
                    criteria.setTenantUuids(userUuids);
                    criteria.setOwnerUuid(userUuids.get(0));
                    criteria.setTenantUuid(userUuids.get(0));
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
                .filter(a -> a.getGarbageSpecification() != null && GrbgConstants.PROPERTY_TYPE_TENANT.equalsIgnoreCase(a.getGarbageSpecification().getPropertyType()))
                .filter(a -> StringUtils.isNotBlank(a.getPropertyId()))
                .collect(Collectors.groupingBy(GarbageApplication::getPropertyId));

        for (GarbageApplication app : applications) {
            if (app.getGarbageSpecification() != null && GrbgConstants.PROPERTY_TYPE_OWNER.equalsIgnoreCase(app.getGarbageSpecification().getPropertyType())) {
                List<GarbageApplication> children = tenantAppsByProp.get(app.getPropertyId());
                if (CollectionUtils.isEmpty(children) && StringUtils.isNotBlank(app.getPropertyId())) {
                    GarbageSearchCriteria childCrit = GarbageSearchCriteria.builder()
                            .tenantId(app.getTenantId())
                            .propertyId(Collections.singletonList(app.getPropertyId()))
                            .build();
                    List<GarbageApplication> propApps = garbageRepository.getGarbageApplications(childCrit);
                    if (!CollectionUtils.isEmpty(propApps)) {
                        children = propApps.stream()
                                .filter(c -> (app.getApplicationNo() != null && app.getApplicationNo().equalsIgnoreCase(c.getParentApplicationNo()))
                                        || (c.getGarbageSpecification() != null && GrbgConstants.PROPERTY_TYPE_TENANT.equalsIgnoreCase(c.getGarbageSpecification().getPropertyType()) && !c.getApplicationNo().equalsIgnoreCase(app.getApplicationNo())))
                                .collect(Collectors.toList());
                        if (!CollectionUtils.isEmpty(children)) {
                            for (GarbageApplication child : children) {
                                userService.enrichApplicationWithUserDetails(child, criteria);
                            }
                        }
                    }
                }
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
                    .anyMatch(r -> r.startsWith("GC_") || GrbgConstants.USER_TYPE_EMPLOYEE.equalsIgnoreCase(r));
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
        throw new CustomException(GrbgConstants.ERROR_UNAUTHORIZED_CONNECTION_ACTION,
                "Disconnection, Reconnection, and Permanent Disconnection can ONLY be performed by the registered PROPERTY OWNER.");
    }

    private void validateArrearsCleared(GarbageApplication app, RequestInfo requestInfo) {
        try {
            List<Demand> demands = demandRepository.searchAllDemands(requestInfo, app.getTenantId(),
                    app.getApplicationNo(), app.getBusinessService(), true);
            if (!CollectionUtils.isEmpty(demands)) {
                for (Demand demand : demands) {
                    if (GrbgConstants.STATUS_ACTIVE.equalsIgnoreCase(demand.getStatus().toString())) {
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
                            throw new CustomException(GrbgConstants.ERROR_PENDING_ARREARS_EXIST,
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
                        .propertyType(GrbgConstants.PROPERTY_TYPE_TENANT)
                        .floorNo(child.getFloorNo())
                        .build());
            } else {
                child.setGarbageSpecification(GarbageSpecification.builder()
                        .propertyType(GrbgConstants.PROPERTY_TYPE_TENANT)
                        .floorNo(child.getFloorNo())
                        .build());
            }
        } else {
            child.getGarbageSpecification().setPropertyType(GrbgConstants.PROPERTY_TYPE_TENANT);
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
    public List<GarbageConnectionLog> searchConnectionHistory(String propertyId, String applicationNo, RequestInfo requestInfo) {
        return garbageRepository.getConnectionLogs(propertyId, applicationNo);
    }


    private void normalizeAndAllocateFloors(GarbageRequest garbageRequest) {
        if (garbageRequest == null || CollectionUtils.isEmpty(garbageRequest.getGarbageApplications())) {
            return;
        }

        Map<String, List<GarbageApplication>> appsByProperty = new LinkedHashMap<>();
        for (GarbageApplication app : garbageRequest.getGarbageApplications()) {
            String propId = StringUtils.isNotBlank(app.getPropertyId()) ? app.getPropertyId() : GrbgConstants.DEFAULT_STRING;
            appsByProperty.computeIfAbsent(propId, k -> new ArrayList<>()).add(app);
        }

        for (Map.Entry<String, List<GarbageApplication>> entry : appsByProperty.entrySet()) {
            List<GarbageApplication> propApps = entry.getValue();
            Set<String> usedFloors = new HashSet<>();

            // 1. Process owner application first
            for (GarbageApplication app : propApps) {
                boolean isOwner = Boolean.TRUE.equals(app.getIsOwner());
                if (isOwner) {
                    String floor = app.getFloorNo();
                    if (GrbgConstants.FLOOR_NONE.equalsIgnoreCase(floor) || GrbgConstants.FLOOR_NULL.equalsIgnoreCase(floor)) {
                        floor = null;
                    }
                    // Only default to "0" if single application (no tenants) and floor is blank
                    if (StringUtils.isBlank(floor) && propApps.size() == 1) {
                        floor = GrbgConstants.FLOOR_ZERO;
                    }
                    app.setFloorNo(floor);
                    if (app.getGarbageSpecification() != null) {
                        app.getGarbageSpecification().setFloorNo(floor);
                    }
                    if (StringUtils.isNotBlank(floor)) {
                        usedFloors.add(floor);
                    }
                }
            }

            // 2. Process non-owner (tenant) applications
            int tenantFloorCounter = usedFloors.contains(GrbgConstants.FLOOR_ZERO) ? 1 : 0;
            for (GarbageApplication app : propApps) {
                boolean isOwner = Boolean.TRUE.equals(app.getIsOwner());
                if (!isOwner) {
                    String floor = app.getFloorNo();
                    if (StringUtils.isBlank(floor) || usedFloors.contains(floor)) {
                        while (usedFloors.contains(String.valueOf(tenantFloorCounter))) {
                            tenantFloorCounter++;
                        }
                        floor = String.valueOf(tenantFloorCounter);
                        tenantFloorCounter++;
                    }
                    app.setFloorNo(floor);
                    if (app.getGarbageSpecification() != null) {
                        app.getGarbageSpecification().setFloorNo(floor);
                    }
                    usedFloors.add(floor);
                }
            }
        }
    }

    private void validateCreateRequest(GarbageRequest request) {
        if (request == null || request.getRequestInfo() == null) {
            throw new CustomException(GrbgConstants.ERROR_INVALID_REQUEST, "RequestInfo cannot be null");
        }
        if (CollectionUtils.isEmpty(request.getGarbageApplications())) {
            throw new CustomException(GrbgConstants.ERROR_INVALID_REQUEST, "garbageApplications list cannot be empty");
        }

        // Check for duplicate floors within the request itself (excluding null/non-resident floors)
        Set<String> requestFloorKeys = new HashSet<>();
        for (GarbageApplication app : request.getGarbageApplications()) {
            if (StringUtils.isNotBlank(app.getFloorNo())) {
                String propId = StringUtils.isNotBlank(app.getPropertyId()) ? app.getPropertyId() : "";
                String key = propId + "_" + app.getFloorNo();
                if (!requestFloorKeys.add(key)) {
                    throw new CustomException(GrbgConstants.ERROR_DUPLICATE_APPLICATION,
                            "Multiple applications in the same request cannot share Property: " + propId + " and Floor: " + app.getFloorNo());
                }
            }
        }

        for (GarbageApplication app : request.getGarbageApplications()) {
            if (StringUtils.isBlank(app.getTenantId())) {
                throw new CustomException(GrbgConstants.ERROR_INVALID_TENANT_ID, "TenantId is required for garbage application");
            }
            if (StringUtils.isBlank(app.getPropertyId())) {
                throw new CustomException(GrbgConstants.ERROR_INVALID_PROPERTY_ID, "PropertyId is mandatory for garbage application");
            }

            // Check if active connection already exists on this property and floor (if floor is occupied)
            if (StringUtils.isNotBlank(app.getFloorNo())) {
                final String targetFloor = app.getFloorNo();
                GarbageSearchCriteria criteria = GarbageSearchCriteria.builder()
                        .tenantId(app.getTenantId())
                        .propertyId(Collections.singletonList(app.getPropertyId()))
                        .build();
                List<GarbageApplication> existingApps = garbageRepository.getGarbageApplications(criteria);
                if (!CollectionUtils.isEmpty(existingApps)) {
                    boolean duplicateExists = existingApps.stream().anyMatch(existing -> {
                        String existingFloor = existing.getFloorNo();
                        if (StringUtils.isBlank(existingFloor) && existing.getGarbageSpecification() != null) {
                            existingFloor = existing.getGarbageSpecification().getFloorNo();
                        }
                        return targetFloor.equalsIgnoreCase(existingFloor)
                                && !GrbgConstants.STATUS_PERMANENTLY_DISCONNECTED.equalsIgnoreCase(existing.getConnectionStatus())
                                && !GrbgConstants.STATUS_REJECTED.equalsIgnoreCase(existing.getStatus());
                    });
                    if (duplicateExists) {
                        throw new CustomException(GrbgConstants.ERROR_DUPLICATE_APPLICATION,
                                "An active garbage connection already exists for Property: " + app.getPropertyId() + " on Floor: " + targetFloor);
                    }
                }
            }
        }
    }

    private void validateUpdateRequest(GarbageRequest request) {
        if (request == null || request.getRequestInfo() == null) {
            throw new CustomException(GrbgConstants.ERROR_INVALID_REQUEST, "RequestInfo cannot be null");
        }
        if (CollectionUtils.isEmpty(request.getGarbageApplications())) {
            throw new CustomException(GrbgConstants.ERROR_INVALID_REQUEST, "garbageApplications list cannot be empty");
        }
        for (GarbageApplication app : request.getGarbageApplications()) {
            if (StringUtils.isBlank(app.getApplicationNo()) && app.getId() == null) {
                throw new CustomException(GrbgConstants.ERROR_INVALID_APPLICATION, "ApplicationNo or ID is mandatory for update");
            }
        }
    }
}
