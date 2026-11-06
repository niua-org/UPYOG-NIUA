import React from "react";
import { Card, KeyNote, SubmitBar } from "@nudmcdgnpm/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";

/**
 * GCApplication Component
 * 
 * Displays a summary card for a single GC application in the citizen's "My Applications" list
 * as per UPYOG Garbage Service HLD Section 3:
 * - Shows Application Number, Applicant Name, Mobile Number, Property ID
 * - Shows Property Type (Owner or Tenant) and Floor Number (for Tenants)
 * - Shows Status (ACTIVE, DISCONNECTED, PERMANENTLY_DISCONNECTED, PENDING_FOR_PAYMENT, etc.)
 * - Provides buttons to view details and make payment
 */
const GCApplication = ({ application, tenantId }) => {
  const { t } = useTranslation();
  const navigate = Digit.Hooks.useCustomNavigate();

  const applicant = application?.applicantDetails?.[0] || {};
  const name = applicant?.name || applicant?.applicantName || t("CS_NA");
  const mobileNumber = applicant?.mobileNumber || t("CS_NA");

  const appNo = application?.applicationNo || t("CS_NA");
  const garbageNo = application?.garbageNo || null;
  const oldGarbageNo = application?.oldGarbageNo || application?.garbageSpecification?.oldGarbageNo || null;
  const appStatus = application?.status || t("CS_NA");
  const connectionStatus = application?.connectionStatus || null;

  const propertyId = application?.propertyId;
  const collectionUnit = application?.garbageSpecification || {};
  const category = collectionUnit?.category;
  const typeOfCollection = collectionUnit?.collectionType;
  const propertyType = collectionUnit?.propertyType || (application?.isOwner === false || application?.tenantUuid ? "TENANT" : "OWNER");
  const floorNo = collectionUnit?.floorNo || application?.floorNo;
  const dueDate = application?.dueDate;
  const paymentStatus = application?.paymentStatus;
  const paymentAmount = application?.paymentAmount;

  const handleMakePayment = () => {
    navigate(`/upyog-ui/citizen/payment/my-bills/garbage-service/${appNo}`);
  };

  const handleEditApplication = () => {
    navigate(`/upyog-ui/citizen/gc/edit/${encodeURIComponent(appNo)}`);
  };

  const getStatusColor = (status) => {
    switch (status) {
      case "APPROVED":
      case "ACTIVE":
        return "#00703c";
      case "DISCONNECTED":
        return "#a82227";
      case "PERMANENTLY_DISCONNECTED":
        return "#d4351c";
      case "REJECTED":
        return "#d4351c";
      default:
        return "#1d70b8";
    }
  };

  return (
    <Card className="gc-gc-application-card" style={{ marginBottom: "16px", padding: "16px" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "10px" }}>
        <KeyNote keyValue={t("GC_APPLICATION_NUMBER_LABEL")} note={appNo} />
        <span
          style={{
            backgroundColor: propertyType === "OWNER" ? "#1d70b8" : "#00703c",
            color: "#ffffff",
            padding: "2px 8px",
            borderRadius: "4px",
            fontSize: "12px",
            fontWeight: "bold",
          }}
        >
          {propertyType}
        </span>
      </div>

      {garbageNo && (
        <KeyNote keyValue={t("GC_GARBAGE_NUMBER_LABEL") || "Garbage Number"} note={garbageNo} />
      )}
      {oldGarbageNo && (
        <KeyNote keyValue={t("GC_OLD_GARBAGE_ID") || "Old Garbage Number"} note={oldGarbageNo} />
      )}
      <KeyNote keyValue={t("GC_APPLICANT_NAME")} note={name} />
      <KeyNote keyValue={t("GC_MOBILE_NUMBER")} note={mobileNumber} />
      {propertyId && <KeyNote keyValue={t("GC_PROPERTY_ID")} note={propertyId} />}
      {floorNo && (
        <KeyNote
          keyValue={t("GC_FLOOR_UNIT") || "Assigned Floor"}
          note={floorNo === "0" ? t("GC_FLOOR_GROUND") || "Ground Floor" : `${floorNo} Floor`}
        />
      )}
      {category && <KeyNote keyValue={t("GC_CATEGORY")} note={t(category)} />}
      {typeOfCollection && <KeyNote keyValue={t("GC_TYPE_OF_COLLECTION")} note={t(typeOfCollection)} />}

      {connectionStatus && (
        <KeyNote
          keyValue={t("GC_CONNECTION_STATUS") || "Connection Status"}
          note={
            <span style={{ color: connectionStatus === "ACTIVE" ? "#00703c" : "#d4351c", fontWeight: "600" }}>
              {t(`GC_STATUS_${connectionStatus}`) || connectionStatus}
            </span>
          }
        />
      )}

      <KeyNote
        keyValue={t("GC_APPLICATION_STATUS_LABEL")}
        note={
          <span style={{ color: getStatusColor(appStatus), fontWeight: "bold" }}>
            {t(`GC_STATUS_${appStatus}`) || appStatus}
          </span>
        }
      />

      {dueDate && <KeyNote keyValue={t("GC_DUE_DATE")} note={dueDate} />}
      {paymentStatus && (
        <KeyNote keyValue={t("GC_PAYMENT_STATUS") || "Payment Status"} note={t(`GC_PAYMENT_${paymentStatus}`) || paymentStatus} />
      )}
      {paymentAmount > 0 && (
        <KeyNote keyValue={t("GC_PAYMENT_AMOUNT") || "Fee Amount"} note={`₹ ${paymentAmount}`} />
      )}

      <div className="gc-btn-row" style={{ marginTop: "16px", display: "flex", gap: "12px", flexWrap: "wrap" }}>
        <Link to={`/upyog-ui/citizen/gc/application-details/${encodeURIComponent(appNo)}`}>
          <SubmitBar label={t("CS_VIEW_DETAILS")} />
        </Link>
        {(appStatus === "PENDING_FOR_PAYMENT" || application?.paymentStatus === "PENDING") && (
          <SubmitBar label={t("CS_APPLICATION_DETAILS_MAKE_PAYMENT")} onSubmit={handleMakePayment} />
        )}
        {appStatus === "EDIT_APPLICATION" && (
          <SubmitBar label={t("GC_EDIT_APPLICATION")} onSubmit={handleEditApplication} />
        )}
      </div>
    </Card>
  );
};

export default GCApplication;