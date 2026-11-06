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
          const appNo = row.original.searchData?.applicationNo || row.original.searchData?.grbgApplicationNumber;
          return (
            <div>
              <span className="link">
                <Link to={`${props.parentRoute}/application-details/` + `${encodeURIComponent(appNo)}`}>
                  {appNo}
                </Link>
              </span>
            </div>
          );
        },
        mobileCell: (original) => GetMobCell(original.searchData?.applicationNo || original.searchData?.grbgApplicationNumber),
      },
      {
        Header: t("GC_APPLICANT_NAME"),
        Cell: ({ row }) => {
          const name = row.original.searchData?.applicantDetails?.[0]?.name || row.original.searchData?.name;
          return GetCell(name || t("CS_NA"));
        },
        mobileCell: (original) => GetMobCell(original.searchData?.applicantDetails?.[0]?.name || original.searchData?.name || t("CS_NA")),
      },
      {
        Header: t("GC_MOBILE_NUMBER"),
        Cell: ({ row }) => {
          const mobile = row.original.searchData?.applicantDetails?.[0]?.mobileNumber || row.original.searchData?.mobileNumber;
          return GetCell(mobile || t("CS_NA"));
        },
        mobileCell: (original) => GetMobCell(original.searchData?.applicantDetails?.[0]?.mobileNumber || original.searchData?.mobileNumber || t("CS_NA")),
      },
      {
        Header: t("ES_INBOX_LOCALITY"),
        Cell: ({ row }) => {
          const locality = row.original.searchData?.propertyLocation?.locality || row.original.searchData?.addresses?.[0]?.additionalDetail?.locality;
          return GetCell(locality ? t(locality) : t("CS_NA"));
        },
        mobileCell: (original) => {
          const locality = original.searchData?.propertyLocation?.locality || original.searchData?.addresses?.[0]?.additionalDetail?.locality;
          return GetMobCell(locality ? t(locality) : t("CS_NA"));
        },
      },
      {
        Header: t("PT_COMMON_TABLE_COL_STATUS_LABEL"),
        Cell: ({ row }) => {
          const status = row.original.workflowData?.state?.applicationStatus || row.original.searchData?.status;
          return GetCell(status ? t(`GC_STATUS_${status}`) : t("CS_NA"));
        },
        mobileCell: (original) => {
          const status = original.workflowData?.state?.applicationStatus || original.searchData?.status;
          return GetMobCell(status ? t(`GC_STATUS_${status}`) : t("CS_NA"));
        },
      },
    ],
    serviceRequestIdKey: (original) => {
      return original?.searchData?.applicationNo || original?.searchData?.grbgApplicationNumber;
    },
  },
});

export default TableConfig;