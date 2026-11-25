package org.egov.garbageservice.util;

import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * Central configuration and constants for garbage-service (status codes, workflow actions, service URLs).
 * Injects host endpoints and feature flags from application properties; also holds static MDMS and business keys.
 */
@Data
@Component
public class GrbgConstants {
    public static final String MDMS_MODULE_NAME_FEE_STRUCTURE = "Garbage";

    public static final String MDMS_MASTER_NAME_FEE_STRUCTURE = "CalculationType";

    // Workflow Statuses
    public static final String STATUS_INITIATED = "INITIATED";
    public static final String STATUS_PENDING_FOR_VERIFICATION = "PENDING_FOR_VERIFICATION";
    public static final String STATUS_PENDING_FOR_APPROVAL = "PENDING_FOR_APPROVAL";
    public static final String STATUS_EDIT_APPLICATION = "EDIT_APPLICATION";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_DISCONNECTED = "DISCONNECTED";
    public static final String STATUS_PERMANENTLY_DISCONNECTED = "PERMANENTLY_DISCONNECTED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_PENDING_FOR_DISCONNECTION_VERIFICATION = "PENDING_FOR_DISCONNECTION_VERIFICATION";
    public static final String STATUS_PENDING_FOR_DISCONNECTION_APPROVAL = "PENDING_FOR_DISCONNECTION_APPROVAL";
    public static final String STATUS_PENDING_FOR_PERMANENT_DISCONNECTION_VERIFICATION = "PENDING_FOR_PERMANENT_DISCONNECTION_VERIFICATION";
    public static final String STATUS_PENDING_FOR_PERMANENT_DISCONNECTION_APPROVAL = "PENDING_FOR_PERMANENT_DISCONNECTION_APPROVAL";
    public static final String STATUS_PENDING_FOR_CITIZEN_ACTION = "PENDING_FOR_CITIZEN_ACTION";
    public static final String STATUS_RECONNECTED = "RECONNECTED";

    // Workflow Actions
    public static final String WORKFLOW_ACTION_APPLY = "APPLY";
    public static final String WORKFLOW_ACTION_VERIFY = "VERIFY";
    public static final String WORKFLOW_ACTION_REJECT = "REJECT";
    public static final String WORKFLOW_ACTION_APPROVE = "APPROVE";
    public static final String WORKFLOW_ACTION_DISCONNECT = "DISCONNECT";
    public static final String WORKFLOW_ACTION_PERMANENT_DISCONNECT = "PERMANENT_DISCONNECT";
    public static final String WORKFLOW_ACTION_APPROVE_DISCONNECTION = "APPROVE_FOR_DISCONNECTION";
    public static final String WORKFLOW_ACTION_APPROVE_PERMANENT = "APPROVE_PERMANENT";
    public static final String WORKFLOW_ACTION_APPROVE_PERMANENT_DISCONNECTION = "APPROVE_PERMANENT_DISCONNECTION";
    public static final String WORKFLOW_ACTION_RESUBMIT_APPLICATION = "RESUBMIT_APPLICATION";
    public static final String WORKFLOW_ACTION_RECONNECT = "RECONNECT";
    public static final String WORKFLOW_ACTION_RAISE_QUERY_TO_CITIZEN = "RAISE_QUERY_TO_CITIZEN";
    public static final String WORKFLOW_ACTION_SEND_BACK_TO_VERIFIER = "SEND_BACK_TO_VERIFIER";
    public static final String WORKFLOW_ACTION_EDIT = "EDIT";
    public static final String WORKFLOW_ACTION_PAY = "PAY";

    // Shorthand aliases for actions
    public static final String ACTION_APPLY = WORKFLOW_ACTION_APPLY;
    public static final String ACTION_VERIFY = WORKFLOW_ACTION_VERIFY;
    public static final String ACTION_REJECT = WORKFLOW_ACTION_REJECT;
    public static final String ACTION_APPROVE = WORKFLOW_ACTION_APPROVE;
    public static final String ACTION_DISCONNECT = WORKFLOW_ACTION_DISCONNECT;
    public static final String ACTION_PERMANENT_DISCONNECT = WORKFLOW_ACTION_PERMANENT_DISCONNECT;
    public static final String ACTION_RECONNECT = WORKFLOW_ACTION_RECONNECT;
    public static final String ACTION_EDIT = WORKFLOW_ACTION_EDIT;
    public static final String ACTION_PAY = WORKFLOW_ACTION_PAY;

    public static final String BUSINESS_SERVICE_GB_CITIZEN = "garbage-service";
    public static final String BUSINESS_SERVICE_GB_EMPLOYEE = "garbage-service";
    public static final String BUSINESS_SERVICE_GARBAGE = "garbage-service";
    public static final String BUSINESS_SERVICE_DISCONNECTION = "DisconnectGarbageConnection";

    public static final String WORKFLOW_MODULE_NAME = "GC";

    public static final String USER_TYPE_CITIZEN = "CITIZEN";

    public static final String USER_TYPE_EMPLOYEE = "EMPLOYEE";

    public static final String USER_TYPE_SYSTEM = "SYSTEM";

    public static final String CHANNEL_TYPE_CREATE = "CREATE";

    public static final String CHANNEL_TYPE_MIGRATE = "MIGRATION";

    public static final String BILLING_TAX_HEAD_MASTER_CODE = "LCF.Garbage_Collection_Fee";

    // Workflow Roles
    public static final String USER_ROLE_GC_CEMP = "GC_CEMP";
    public static final String USER_ROLE_GB_VERIFIER = "GC_VERIFIER";
    public static final String USER_ROLE_GB_APPROVER = "GC_APPROVER";

    public static final String GARBAGE_MODEL = "Garbage";

    public static final String GARBAGE_PENALTY_TAX_HEAD = "GARBAGE_PENALTY";

    // Service Disconnection & Reconnection Detail Keys
    public static final String DISCONNECTION_DATE = "disconnectionDate";
    public static final String DISCONNECTION_REASON = "disconnectionReason";
    public static final String DISCONNECTION_HISTORY = "disconnectionHistory";
    public static final String RECONNECTION_DATE = "reconnectionDate";
    public static final String SERVICE_STATUS_ACTIVE = "ACTIVE";
    public static final String SERVICE_STATUS_INACTIVE = "INACTIVE";

    // Property Types
    public static final String PROPERTY_TYPE_OWNER = "OWNER";
    public static final String PROPERTY_TYPE_TENANT = "TENANT";

    // Application Statuses (Static fallback constants)
    public static final String STATUS_APPLICATION_CREATED = "APPLICATION_CREATED";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_PENDING_FOR_PAYMENT = "PENDING_FOR_PAYMENT";

    // Connection Log & Request Types
    public static final String REQUEST_TYPE_DISCONNECTION = "DISCONNECTION";
    public static final String REQUEST_TYPE_RECONNECTION = "RECONNECTION";
    public static final String REQUEST_TYPE_PERMANENT_DISCONNECTION = "PERMANENT_DISCONNECTION";
    public static final String REASON_TEMPORARY_DISCONNECTION = "TEMPORARY_DISCONNECTION";
    public static final String REASON_SERVICE_RECONNECTION = "SERVICE_RECONNECTION";
    public static final String REASON_PERMANENT_DISCONNECTION = "PERMANENT_DISCONNECTION";

    // Floor and Property Spec Constants
    public static final String FLOOR_ZERO = "0";
    public static final String FLOOR_ONE = "1";
    public static final String FLOOR_NONE = "NONE";
    public static final String FLOOR_NULL = "NULL";
    public static final String DEFAULT_STRING = "DEFAULT";
    public static final String SPECIAL_CATEGORY_NONE = "NONE";

    // Workflow reasons & notifications
    public static final String REASON_WORKFLOW_DISCONNECTION = "WORKFLOW_DISCONNECTION";
    public static final String REASON_WORKFLOW_PERMANENT_DISCONNECTION = "WORKFLOW_PERMANENT_DISCONNECTION";
    public static final String NOTIFICATION_CATEGORY_TRANSACTION = "TRANSACTION";

    // Billing & Scheduler Constants
    public static final String BILL_STATUS_GENERATED = "GENERATED";
    public static final String BILL_STATUS_PENDING = "PENDING";
    public static final String PAYMENT_TYPE_MONTHLY = "MONTHLY";
    public static final String PAYMENT_TYPE_FULL = "FULL";
    public static final String SYSTEM_USER_SCHEDULER = "SCHEDULER";

    // IDGen Constants
    public static final String IDGEN_DISCONNECTION_NAME = "garbage.disconnection.num";
    public static final String IDGEN_RECONNECTION_NAME = "garbage.reconnection.num";
    public static final String IDGEN_PERMANENT_DISCONNECTION_NAME = "garbage.permanent.disconnection.num";
    public static final String PREFIX_DISCONNECTION = "GC-DISC-";
    public static final String PREFIX_RECONNECTION = "GC-REC-";
    public static final String PREFIX_PERMANENT_DISCONNECTION = "GC-PDISC-";

    // Role Constants
    public static final String ROLE_CITIZEN_CODE = "CITIZEN";
    public static final String ROLE_CITIZEN_NAME = "Citizen";

    // Error Codes
    public static final String ERROR_APPLICATION_NOT_FOUND = "APPLICATION_NOT_FOUND";
    public static final String ERROR_APPLICATION_TERMINATED = "APPLICATION_TERMINATED";
    public static final String ERROR_UNAUTHORIZED_CONNECTION_ACTION = "UNAUTHORIZED_CONNECTION_ACTION";
    public static final String ERROR_PENDING_ARREARS_EXIST = "PENDING_ARREARS_EXIST";
    public static final String ERROR_INVALID_REQUEST = "INVALID_REQUEST";
    public static final String ERROR_DUPLICATE_APPLICATION = "DUPLICATE_APPLICATION";
    public static final String ERROR_INVALID_TENANT_ID = "INVALID_TENANT_ID";
    public static final String ERROR_INVALID_PROPERTY_ID = "INVALID_PROPERTY_ID";
    public static final String ERROR_INVALID_APPLICATION = "INVALID_APPLICATION";
    public static final String ERROR_WORKFLOW_RESPONSE_NULL = "WORKFLOW_RESPONSE_NULL";
    public static final String ERROR_TECHNICAL = "ERR_TECHNICAL";
    public static final String ERROR_IDGEN = "IDGEN_ERROR";
    public static final String ERROR_INVALID_USER_RESPONSE = "INVALID_USER_RESPONSE";
    public static final String ERROR_ILLEGAL_ARGUMENT = "ILLEGAL_ARGUMENT_EXCEPTION";
}