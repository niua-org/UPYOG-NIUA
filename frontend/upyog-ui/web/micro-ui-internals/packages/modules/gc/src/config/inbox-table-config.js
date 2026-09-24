import React from "react";
import { Link } from "react-router-dom";

/**
 * Helper function to render a standard cell with the cell-text CSS class.
 * @param {string} value - The text to display in the cell
 * @returns {JSX.Element} A styled span element
 */
const GetCell = (value) => <span className="cell-text">{value}</span>;

/**
 * Helper function to render a mobile cell with the sla-cell CSS class.
 * @param {string} value - The text to display in the cell
 * @returns {JSX.Element} A styled span element
 */
const GetMobCell = (value) => <span className="sla-cell">{value}</span>;

const getStatusBadge = (t, status) => {
  if (!status) return <span className="cell-text">{t("CS_NA")}</span>;
  const s = String(status).toUpperCase();
  let badgeClass = "bg-blue-50 text-blue-700 border-blue-200";
  let dotClass = "bg-blue-500";

  if (s.includes("PAID") || s.includes("APPROVED")) {
    badgeClass = "bg-emerald-50 text-emerald-700 border-emerald-200";
    dotClass = "bg-emerald-500";
  } else if (s.includes("PENDING") || s.includes("PAYMENT") || s.includes("VERIF")) {
    badgeClass = "bg-amber-50 text-amber-700 border-amber-200";
    dotClass = "bg-amber-500";
  } else if (s.includes("REJECT") || s.includes("CANCEL")) {
    badgeClass = "bg-red-50 text-red-700 border-red-200";
    dotClass = "bg-red-500";
  }

  return (
    <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold border ${badgeClass}`}>
      <span className={`w-1.5 h-1.5 rounded-full ${dotClass}`}></span>
      <span>{t(`GC_STATUS_${status}`) || t(status) || status}</span>
    </span>
  );
};

/**
 * TableConfig
 * 
 * Defines column configurations for the GC inbox table, including desktop and mobile views.
 * Each column specifies:
 * - `Header`: Translated column header
 * - `Cell`: Desktop render function with link navigation to application details
 * - `mobileCell`: Mobile render function for compact display
 * 
 * Also exports `serviceRequestIdKey` to extract the application number for routing.
 * 
 * @param {function} t - i18n translation function
 * @returns {object} Configuration object keyed by module code (e.g., "GC")
 */
export const TableConfig = (t) => ({
  GC: {
    inboxColumns: (props) => [
      {
        Header: t("GC_APPLICATION_NUMBER_LABEL"),
        Cell: ({ row }) => {
          const appNo = row.original.searchData?.grbgApplicationNumber || row.original.searchData?.applicationNo;
          return (
            <div className="font-semibold text-red-700 hover:text-red-900 transition-colors">
              <Link to={`${props.parentRoute}/application-details/` + `${encodeURIComponent(appNo)}`}>
                {appNo || t("CS_NA")}
              </Link>
            </div>
          );
        },
        mobileCell: (original) => GetMobCell(original.searchData?.grbgApplicationNumber),
      },
      {
        Header: t("GC_APPLICANT_NAME"),
        Cell: ({ row }) => {
          return <span className="text-slate-800 font-medium">{row.original.searchData?.name || t("CS_NA")}</span>;
        },
        mobileCell: (original) => GetMobCell(original.searchData?.name || t("CS_NA")),
      },
      {
        Header: t("GC_MOBILE_NUMBER"),
        Cell: ({ row }) => <span className="text-slate-600">{row.original.searchData?.mobileNumber || t("CS_NA")}</span>,
        mobileCell: (original) => GetMobCell(original.searchData?.mobileNumber || t("CS_NA")),
      },
      {
        Header: t("ES_INBOX_LOCALITY"),
        Cell: ({ row }) => {
          const locality = row.original.searchData?.addresses?.[0]?.additionalDetail?.locality;
          return <span className="text-slate-600">{locality ? t(locality) : t("CS_NA")}</span>;
        },
        mobileCell: (original) => {
          const locality = original.searchData?.addresses?.[0]?.additionalDetail?.locality;
          return GetMobCell(locality ? t(locality) : t("CS_NA"));
        },
      },
      {
        Header: t("PT_COMMON_TABLE_COL_STATUS_LABEL"),
        Cell: ({ row }) => {
          const status = row.original.workflowData?.state?.applicationStatus || row.original.searchData?.applicationStatus;
          return getStatusBadge(t, status);
        },
        mobileCell: (original) => {
          const status = original.workflowData?.state?.applicationStatus || original.searchData?.applicationStatus;
          return GetMobCell(status ? t(`GC_STATUS_${status}`) : t("CS_NA"));
        },
      },
    ],
    serviceRequestIdKey: (original) => {
      return original?.searchData?.grbgApplicationNumber;
    },
  },
});

export default TableConfig;