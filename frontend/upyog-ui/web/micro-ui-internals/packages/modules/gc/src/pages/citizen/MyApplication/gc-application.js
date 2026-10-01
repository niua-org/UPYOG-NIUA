import React, { useState } from "react";
import { Card } from "@nudmcdgnpm/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import "../../../css/gc-inline-auto.css";

/**
 * Status color & theme configuration for GC Application
 */
const getStatusBadgeConfig = (status) => {
  switch (status) {
    case "APPROVED":
    case "PAID":
    case "COMPLETED":
    case "ACTIVE":
      return {
        bgClass: "bg-emerald-50 text-emerald-700 border-emerald-200",
        dotClass: "bg-emerald-500",
        accentColor: "#10b981",
      };
    case "PENDING_FOR_PAYMENT":
    case "PAYMENT_PENDING":
      return {
        bgClass: "bg-amber-50 text-amber-700 border-amber-200",
        dotClass: "bg-amber-500",
        accentColor: "#f59e0b",
      };
    case "EDIT_APPLICATION":
    case "RESUBMIT":
      return {
        bgClass: "bg-purple-50 text-purple-700 border-purple-200",
        dotClass: "bg-purple-500",
        accentColor: "#8b5cf6",
      };
    case "REJECTED":
    case "CANCELLED":
    case "INVALID":
      return {
        bgClass: "bg-rose-50 text-rose-700 border-rose-200",
        dotClass: "bg-rose-500",
        accentColor: "#ef4444",
      };
    case "INITIATED":
    case "PENDING_FOR_VERIFICATION":
    case "PENDING_FOR_FIELD_INSPECTION":
    default:
      return {
        bgClass: "bg-sky-50 text-sky-700 border-sky-200",
        dotClass: "bg-sky-500",
        accentColor: "#0284c7",
      };
  }
};

/**
 * GCApplication Component
 * 
 * An interactive, modern view card for Citizen's "My Applications" list.
 * Built with Digit UI Card wrapper for uniform size and Tailwind CSS classes for
 * responsive grids, copy-to-clipboard, quick-view accordion, and dynamic action buttons.
 */
const GCApplication = ({ application, tenantId }) => {
  const { t } = useTranslation();
  const navigate = Digit.Hooks.useCustomNavigate();
  const [isExpanded, setIsExpanded] = useState(false);
  const [copied, setCopied] = useState(false);

  const name = application?.name || application?.applicantName || application?.owners?.[0]?.name;
  const mobileNumber = application?.mobileNumber || application?.owners?.[0]?.mobileNumber;

  const appNo =
    application?.grbgApplication?.applicationNo ||
    application?.grbgApplicationNumber ||
    application?.applicationNo ||
    "";
  const appStatus =
    application?.grbgApplication?.status ||
    application?.applicationStatus ||
    application?.status ||
    "";

  const propertyId = application?.propertyId || application?.address?.propertyId;
  const collectionUnits = application?.grbgCollectionUnits || application?.collectionUnits || [];
  const primaryUnit = collectionUnits?.[0] || {};
  const category = primaryUnit?.category || application?.category;
  const typeOfCollection = primaryUnit?.unitType || application?.typeOfCollection;

  // Address details for quick view
  const address = application?.address || application?.grbgAddress || application?.propertyLocation || {};
  const addressString = [
    address?.doorNo || address?.houseNo,
    address?.street || address?.streetName,
    address?.landmark,
    address?.locality?.name || address?.locality?.code || (typeof address?.locality === "string" ? address.locality : ""),
    address?.city?.name || address?.city?.code || (typeof address?.city === "string" ? address.city : ""),
    address?.pincode,
  ]
    .filter(Boolean)
    .join(", ");

  const statusConfig = getStatusBadgeConfig(appStatus);

  const handleCopyAppNo = (e) => {
    e.stopPropagation();
    if (appNo && navigator?.clipboard) {
      navigator.clipboard.writeText(appNo);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const handleMakePayment = () => {
    navigate(`/upyog-ui/citizen/payment/my-bills/garbage-service/${appNo}`);
  };

  const handleEditApplication = () => {
    navigate(`/upyog-ui/citizen/gc/edit/${appNo}`);
  };

  return (
    <Card
      className="gc-app-card"
      style={{
        "--card-accent": statusConfig.accentColor,
        marginTop: "16px",
        marginBottom: "16px",
      }}
    >
      {/* Top Header Row: App Number, Copy, and Status */}
      <div className="flex flex-row items-center justify-between gap-3 pb-3.5 border-b border-gray-100 w-full">
        <div>
          <div className="text-[11px] font-semibold text-gray-500 uppercase tracking-wider">
            {t("GC_APPLICATION_NUMBER_LABEL")}
          </div>
          <div className="flex items-center gap-2 mt-0.5">
            <span className="text-base font-bold text-gray-900 tracking-tight">
              {appNo || t("CS_NA")}
            </span>
            {appNo && (
              <button
                type="button"
                onClick={handleCopyAppNo}
                title={copied ? t("CS_COPIED") || "Copied!" : t("CS_COPY") || "Copy Application Number"}
                className="gc-copy-btn p-1 text-gray-400 hover:text-gray-700 rounded-md border border-gray-200 bg-gray-50 hover:bg-gray-100 relative"
              >
                {copied ? (
                  <svg className="w-3.5 h-3.5 text-emerald-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                  </svg>
                ) : (
                  <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"
                    />
                  </svg>
                )}
                {copied && (
                  <span className="absolute -top-7 left-1/2 -translate-x-1/2 bg-gray-800 text-white text-[10px] px-1.5 py-0.5 rounded shadow whitespace-nowrap">
                    Copied!
                  </span>
                )}
              </button>
            )}
          </div>
        </div>

        {/* Dynamic Status Badge */}
        <div className="flex items-center shrink-0">
          <span
            className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold uppercase tracking-wide border ${statusConfig.bgClass}`}
          >
            <span className={`w-2 h-2 rounded-full animate-pulse ${statusConfig.dotClass}`} />
            {appStatus ? t(`GC_STATUS_${appStatus}`) : t("CS_NA")}
          </span>
        </div>
      </div>

      {/* Main Info Grid - Balanced 3 Column Grid */}
      <div className="gc-grid-container my-3.5 p-3.5 bg-gray-50/70 rounded-lg border border-gray-100 text-sm w-full">
        {/* Applicant Name */}
        <div className="flex items-start gap-2.5">
          <div className="p-1.5 text-gray-500 bg-white rounded border border-gray-200 shrink-0 mt-0.5">
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
              />
            </svg>
          </div>
          <div className="min-w-0">
            <div className="text-xs font-medium text-gray-500 truncate">{t("GC_NAME")}</div>
            <div className="font-semibold text-gray-800 mt-0.5 break-words">{name || t("CS_NA")}</div>
          </div>
        </div>

        {/* Mobile Number */}
        <div className="flex items-start gap-2.5">
          <div className="p-1.5 text-gray-500 bg-white rounded border border-gray-200 shrink-0 mt-0.5">
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z"
              />
            </svg>
          </div>
          <div className="min-w-0">
            <div className="text-xs font-medium text-gray-500 truncate">{t("GC_MOBILE_NUMBER")}</div>
            <div className="font-semibold text-gray-800 mt-0.5 break-words">{mobileNumber || t("CS_NA")}</div>
          </div>
        </div>

        {/* Property ID */}
        {propertyId && (
          <div className="flex items-start gap-2.5">
            <div className="p-1.5 text-gray-500 bg-white rounded border border-gray-200 shrink-0 mt-0.5">
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"
                />
              </svg>
            </div>
            <div className="min-w-0">
              <div className="text-xs font-medium text-gray-500 truncate">{t("GC_PROPERTY_ID")}</div>
              <div className="font-semibold text-gray-800 mt-0.5 break-words">{propertyId}</div>
            </div>
          </div>
        )}

        {/* Category */}
        {category && (
          <div className="flex items-start gap-2.5">
            <div className="p-1.5 text-gray-500 bg-white rounded border border-gray-200 shrink-0 mt-0.5">
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z"
                />
              </svg>
            </div>
            <div className="min-w-0">
              <div className="text-xs font-medium text-gray-500 truncate">{t("GC_CATEGORY")}</div>
              <div className="font-semibold text-gray-800 mt-0.5 break-words">{t(category)}</div>
            </div>
          </div>
        )}

        {/* Type of Collection */}
        {typeOfCollection && (
          <div className="flex items-start gap-2.5">
            <div className="p-1.5 text-gray-500 bg-white rounded border border-gray-200 shrink-0 mt-0.5">
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"
                />
              </svg>
            </div>
            <div className="min-w-0">
              <div className="text-xs font-medium text-gray-500 truncate">{t("GC_TYPE_OF_COLLECTION")}</div>
              <div className="font-semibold text-gray-800 mt-0.5 break-words">{t(typeOfCollection)}</div>
            </div>
          </div>
        )}

        {/* Due Date */}
        {application?.dueDate && (
          <div className="flex items-start gap-2.5">
            <div className="p-1.5 text-gray-500 bg-white rounded border border-gray-200 shrink-0 mt-0.5">
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
                />
              </svg>
            </div>
            <div className="min-w-0">
              <div className="text-xs font-medium text-gray-500 truncate">{t("GC_DUE_DATE")}</div>
              <div className="font-semibold text-amber-700 mt-0.5 break-words">{application.dueDate}</div>
            </div>
          </div>
        )}
      </div>

      {/* Interactive Quick View Accordion Toggle */}
      {(addressString || collectionUnits.length > 1) && (
        <div className="mb-3">
          <button
            type="button"
            onClick={() => setIsExpanded(!isExpanded)}
            className="gc-quick-view-toggle flex items-center gap-1.5 text-xs font-semibold text-gray-600 hover:text-red-700 transition-colors focus:outline-none"
          >
            <span>{isExpanded ? t("GC_HIDE_DETAILS") || "Hide Details" : t("GC_QUICK_VIEW") || "Quick View"}</span>
            <svg
              className={`w-3.5 h-3.5 transition-transform duration-200 ${isExpanded ? "rotate-180" : ""}`}
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
            </svg>
          </button>

          {/* Accordion Drawer */}
          <div className={`gc-accordion-content ${isExpanded ? "open mt-2.5" : ""}`}>
            <div className="p-3 bg-gray-50 rounded-lg border border-gray-200 text-xs space-y-2 text-gray-700">
              {addressString && (
                <div className="flex items-start gap-2">
                  <span className="font-semibold text-gray-600 shrink-0">{t("GC_ADDRESS") || "Address"}:</span>
                  <span className="text-gray-800">{addressString}</span>
                </div>
              )}
              {collectionUnits.length > 0 && (
                <div className="flex items-start gap-2">
                  <span className="font-semibold text-gray-600 shrink-0">
                    {t("GC_TOTAL_COLLECTION_UNITS") || "Total Collection Units"}:
                  </span>
                  <span className="text-gray-800">{collectionUnits.length}</span>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Interactive Action Buttons */}
      <div className="gc-btn-row pt-3 border-t border-gray-100 flex flex-wrap items-center gap-3">
        {/* View Details Button */}
        <Link
          to={`/upyog-ui/citizen/gc/application-details/${encodeURIComponent(appNo)}`}
          className="gc-action-primary-btn"
        >
          <span>{t("CS_VIEW_DETAILS")}</span>
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
          </svg>
        </Link>

        {/* Make Payment Button */}
        {appStatus === "PENDING_FOR_PAYMENT" && (
          <button
            type="button"
            onClick={handleMakePayment}
            className="gc-action-pay-btn"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z"
              />
            </svg>
            <span>{t("CS_APPLICATION_DETAILS_MAKE_PAYMENT")}</span>
          </button>
        )}

        {/* Edit Application Button */}
        {appStatus === "EDIT_APPLICATION" && (
          <button
            type="button"
            onClick={handleEditApplication}
            className="gc-action-secondary-btn"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"
              />
            </svg>
            <span>{t("GC_EDIT_APPLICATION")}</span>
          </button>
        )}
      </div>
    </Card>
  );
};

export default GCApplication;