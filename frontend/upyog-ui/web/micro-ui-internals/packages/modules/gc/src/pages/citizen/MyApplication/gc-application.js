import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";

/**
 * GCApplication Component (Preview Card)
 * 
 * Displays a summary preview card for a single GC application in Citizen "My Applications".
 * Styled using the modern property card layout from Property Tax Assessment (pt-citizen-prop-card),
 * utilizing existing premade styles without modifying packages/css:
 * - Top accent gradient bar and card hover animations
 * - Header with Application Number label & value, Property Type badge, and Status pill (no bin icon)
 * - Info grid with field-specific SVG icons:
 * - Bottom action buttons matching PT view-details and make-payment styles
 */
const GCApplication = ({ application, tenantId }) => {
  const { t } = useTranslation();
  const navigate = Digit.Hooks.useCustomNavigate();

  const [billData, setBillData] = useState(null);
  const [loading, setLoading] = useState(false);

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
  const dueDate = application?.dueDate;
  const paymentStatus = application?.paymentStatus;
  const paymentAmount = application?.paymentAmount;

  // Helper to safely resolve i18n keys and fallback when untranslated
  const getLabel = (key, fallback) => {
    const val = t(key);
    if (!val || val === key) return fallback;
    return val;
  };

  // Fetch bill data matching Property Tax Assessment -> My Properties
  const fetchBillData = async () => {
    if (!appNo || appNo === t("CS_NA")) return;
    setLoading(true);
    try {
      const result = await Digit.PaymentService.fetchBill(tenantId || application?.tenantId, {
        businessService: "garbage-service",
        consumerCode: appNo,
      });
      setBillData(result);
    } catch (e) {
      // Fallback silently to application.paymentAmount
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBillData();
  }, [tenantId, application?.tenantId, appNo]);

  const billTotal = billData?.Bill?.[0]?.totalAmount;
  const totalDue =
    billTotal !== undefined && billTotal !== null
      ? billTotal
      : paymentAmount !== undefined && paymentAmount !== null
      ? Number(paymentAmount)
      : null;

  const hasDue = totalDue !== null && Number(totalDue) > 0;
  const isPaid = paymentStatus === "PAID" || (totalDue !== null && Number(totalDue) === 0);

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
      case "REJECTED":
        return "#d4351c";
      default:
        return "#1d70b8";
    }
  };

  return (
    <div className="pt-citizen-prop-card">
      {/* Card Header: Application Number, Property Type badge & Status Pill (Bin icon removed) */}
      <div className="pt-citizen-prop-card-header">
        <div className="pt-citizen-prop-card-id-wrap">
          <div className="pt-citizen-prop-id-block">
            <span className="pt-citizen-prop-id-label">{getLabel("GC_APPLICATION_NUMBER_LABEL", "Application Number")}</span>
            <span className="pt-citizen-prop-id-value">{appNo}</span>
          </div>
        </div>
        <div className="pt-citizen-prop-header-right" style={{ display: "flex", gap: "8px", alignItems: "center" }}>
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
          <span
            className={`pt-citizen-prop-status-pill status-${appStatus?.toLowerCase()}`}
            style={{
              color: getStatusColor(appStatus),
              borderColor: getStatusColor(appStatus),
            }}
          >
            {getLabel(`GC_STATUS_${appStatus}`, appStatus)}
          </span>
        </div>
      </div>

      {/* Card Body: Info Grid matching specified sequence */}
      <div className="pt-citizen-prop-card-body">
        <div className="pt-citizen-prop-info-grid">
          {/* a. Garbage number (shows "Garbage Number" instead of literal GC_GARBAGE_NUMBER_LABEL) */}
          <div className="pt-citizen-prop-info-item">
            <span className="pt-citizen-prop-info-label">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                <polyline points="14 2 14 8 20 8" />
              </svg>
              {getLabel("GC_GARBAGE_NUMBER", getLabel("GC_GARBAGE_NUMBER_LABEL", "Garbage Number"))}
            </span>
            <span className="pt-citizen-prop-info-value">{garbageNo || t("CS_NA")}</span>
          </div>

          {/* Old Garbage Number if present */}
          {oldGarbageNo && (
            <div className="pt-citizen-prop-info-item">
              <span className="pt-citizen-prop-info-label">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="1 4 1 10 7 10" />
                  <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10" />
                </svg>
                {getLabel("GC_OLD_GARBAGE_ID", "Old Garbage Number")}
              </span>
              <span className="pt-citizen-prop-info-value">{oldGarbageNo}</span>
            </div>
          )}

          {/* b. Applicant Name */}
          <div className="pt-citizen-prop-info-item">
            <span className="pt-citizen-prop-info-label">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                <circle cx="12" cy="7" r="4" />
              </svg>
              {getLabel("GC_APPLICANT_NAME", "Applicant Name")}
            </span>
            <span className="pt-citizen-prop-info-value">{name}</span>
          </div>

          {/* c. Mobile Number */}
          <div className="pt-citizen-prop-info-item">
            <span className="pt-citizen-prop-info-label">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z" />
              </svg>
              {getLabel("GC_MOBILE_NUMBER", "Mobile Number")}
            </span>
            <span className="pt-citizen-prop-info-value">{mobileNumber}</span>
          </div>

          {/* d. Property ID */}
          {propertyId && (
            <div className="pt-citizen-prop-info-item">
              <span className="pt-citizen-prop-info-label">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
                  <polyline points="9 22 9 12 15 12 15 22" />
                </svg>
                {getLabel("GC_PROPERTY_ID", "Property ID")}
              </span>
              <span className="pt-citizen-prop-info-value">{propertyId}</span>
            </div>
          )}

          {/* e. Floor unit (GC_FLOOR_UNIT) is excluded from preview card as requested */}

          {/* f. Category */}
          {category && (
            <div className="pt-citizen-prop-info-item">
              <span className="pt-citizen-prop-info-label">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="3" y="3" width="7" height="7" />
                  <rect x="14" y="3" width="7" height="7" />
                  <rect x="14" y="14" width="7" height="7" />
                  <rect x="3" y="14" width="7" height="7" />
                </svg>
                {getLabel("GC_CATEGORY", "Category")}
              </span>
              <span className="pt-citizen-prop-info-value">{t(category)}</span>
            </div>
          )}

          {/* g. Type of Collection */}
          {typeOfCollection && (
            <div className="pt-citizen-prop-info-item">
              <span className="pt-citizen-prop-info-label">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="1" y="3" width="15" height="13" />
                  <polygon points="16 8 20 8 23 11 23 16 16 16 8" />
                  <circle cx="5.5" cy="18.5" r="2.5" />
                  <circle cx="18.5" cy="18.5" r="2.5" />
                </svg>
                {getLabel("GC_TYPE_OF_COLLECTION", "Type of Collection")}
              </span>
              <span className="pt-citizen-prop-info-value">{t(typeOfCollection)}</span>
            </div>
          )}

          {/* h. Garbage Collection Amount & Payment Amount - shown BEFORE Connection Status (like PT Assessment -> My Properties) */}
          <div className="pt-citizen-prop-info-item pt-due-item">
            <span className="pt-citizen-prop-info-label">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <line x1="12" y1="1" x2="12" y2="23" />
                <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6" />
              </svg>
              {dueDate ? `${getLabel("GC_DUE_DATE", "Due Date")}: ${dueDate}` : getLabel("GC_COLLECTION_AMOUNT", getLabel("CS_COMMON_TOTAL_AMOUNT_DUE", "Garbage Collection Amount"))}
            </span>
            <span className="pt-citizen-prop-info-value">
              {loading ? (
                <span className="pt-prop-amount-loading">{t("CS_COMMON_LOADING")}...</span>
              ) : hasDue ? (
                <span className="pt-prop-amount-pill due">₹{totalDue}</span>
              ) : isPaid ? (
                <span className="pt-prop-amount-pill paid">{getLabel("PT_PAID_STATUS", "₹0 (Paid)")}</span>
              ) : (
                <span className="pt-prop-amount-pill na">{t("CS_NA")}</span>
              )}
            </span>
          </div>

          {/* Payment Status */}
          {paymentStatus && (
            <div className="pt-citizen-prop-info-item">
              <span className="pt-citizen-prop-info-label">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="1" y="4" width="22" height="16" rx="2" ry="2" />
                  <line x1="1" y1="10" x2="23" y2="10" />
                </svg>
                {getLabel("GC_PAYMENT_STATUS", "Payment Status")}
              </span>
              <span className="pt-citizen-prop-info-value">
                <span className={`pt-prop-amount-pill ${paymentStatus === "PAID" ? "paid" : "due"}`}>
                  {getLabel(`GC_PAYMENT_${paymentStatus}`, paymentStatus)}
                </span>
              </span>
            </div>
          )}

          {/* Connection Status - placed AFTER payment and garbage collection amount */}
          {connectionStatus && (
            <div className="pt-citizen-prop-info-item">
              <span className="pt-citizen-prop-info-label">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="12" r="10" />
                  <line x1="12" y1="8" x2="12" y2="12" />
                  <line x1="12" y1="16" x2="12.01" y2="16" />
                </svg>
                {getLabel("GC_CONNECTION_STATUS", "Connection Status")}
              </span>
              <span
                className="pt-citizen-prop-info-value"
                style={{
                  color: connectionStatus === "ACTIVE" ? "#00703c" : "#d4351c",
                  fontWeight: "bold",
                }}
              >
                {getLabel(`GC_STATUS_${connectionStatus}`, connectionStatus)}
              </span>
            </div>
          )}
        </div>
      </div>

      {/* Card Actions: View Details, Make Payment, Edit Application */}
      <div className="pt-citizen-prop-card-actions">
        <Link
          to={`/upyog-ui/citizen/gc/application-details/${encodeURIComponent(appNo)}`}
          className="pt-citizen-action-btn view-details"
        >
          {t("CS_VIEW_DETAILS")}
        </Link>
        {(appStatus === "PENDING_FOR_PAYMENT" || paymentStatus === "PENDING" || hasDue) && (
          <button
            type="button"
            className="pt-citizen-action-btn make-payment"
            onClick={handleMakePayment}
          >
            {t("CS_APPLICATION_DETAILS_MAKE_PAYMENT")}
          </button>
        )}
        {appStatus === "EDIT_APPLICATION" && (
          <button
            type="button"
            className="pt-citizen-action-btn view-details"
            onClick={handleEditApplication}
          >
            {t("GC_EDIT_APPLICATION")}
          </button>
        )}
      </div>
    </div>
  );
};

export default GCApplication;
