import React from "react";
import { Link } from "react-router-dom";

/**
 * Formats plain text table cell content.
 *
 * @param {string|number} value - Cell text value.
 * @returns {React.ReactElement} Cell text span.
 */
const GetCell = (value) => <span className="cell-text">{value}</span>;

/**
 * Returns a styled badge for an application status code.
 *
 * @param {string} status - Application status code (e.g. APPROVED, PENDING_FOR_APPROVAL).
 * @param {Function} t - Localization translation function.
 * @returns {React.ReactElement} Styled status badge element.
 */
const GetStatusBadge = (status, t) => {
  let badgeClass = "status-default";

  if (status === "APPROVED") {
    badgeClass = "status-approved";
  } else if (status === "PENDING_FOR_APPROVAL") {
    badgeClass = "status-pending";
  } else if (status === "REJECTED") {
    badgeClass = "status-rejected";
  }

  return (
    <span className={`family-status-badge ${badgeClass}`}>
      {t(status)}
    </span>
  );
};

/**
 * Table configuration factory for Family Service employee views.
 *
 * @param {Function} t - Localization translation function.
 * @returns {Object} Table configuration object containing column builders.
 */
export const TableConfig = (t) => ({
  PROFILE: {
    /**
     * Builds table column definitions for the Family Service Inbox table.
     *
     * @param {Object} props - Table properties.
     * @param {string} props.parentRoute - Base route prefix for details navigation.
     * @returns {Array<Object>} React Table column definitions.
     */
    inboxColumns: (props) => [
      {
        Header: t("ES_INBOX_APPLICATION_NO"),
        id: "applicationNo",
        Cell: ({ row }) => {
          const familyId = row?.original?.searchData?.familyId;
          const applicationNo = row?.original?.searchData?.applicationNo;
          return (
            <div>
              <span className="link">
                <Link to={`${props?.parentRoute}/application-details/${familyId}`}>
                  {applicationNo}
                </Link>
              </span>
            </div>
          );
        },
      },
      {
        Header: t("CORE_COMMON_FAMILY_ID"),
        id: "familyId",
        Cell: ({ row }) => {
          const familyId = row?.original?.searchData?.familyId;
          return (
            <div>
              <span className="link">
                <Link to={`${props?.parentRoute}/application-details/${familyId}`}>
                  {familyId}
                </Link>
              </span>
            </div>
          );
        },
      },
      {
        Header: t("HEAD_OF_FAMILY"),
        Cell: ({ row }) => {
          return GetCell(row?.original?.searchData?.headName);
        },
      },
      {
        Header: t("ES_PROFILE_MOBILE_NUMBER"),
        Cell: ({ row }) => {
          const head = row?.original?.searchData?.members?.find((m) => m.isHead);
          return GetCell(head?.mobileNumber);
        },
      },
      {
        Header: t("CS_INBOX_STATUS_FILTER"),
        Cell: ({ row }) => {
          const status = row?.original?.searchData?.applicationStatus;
          return GetStatusBadge(status, t);
        },
      },
    ],
  },
});
