import React, { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { CheckBox, SubmitBar } from "@nudmcdgnpm/digit-ui-react-components";

/**
 * Filter component for the Family Service Inbox sidebar.
 * Displays application status checkboxes with dynamic record counts from statusMap.
 * Enables employees to filter inbox results by status and reset filters.
 *
 * @param {Object} props - Component properties.
 * @param {Object} props.searchParams - Current search parameter object.
 * @param {Function} props.onFilterChange - Callback invoked when filters are applied or reset.
 * @param {Array<Object>} [props.statusMap=[]] - Distribution array mapping status codes to record counts.
 * @returns {React.ReactElement} Sidebar filter card element.
 */
const Filter = ({ searchParams, onFilterChange, statusMap = [] }) => {
  const { t } = useTranslation();

  const statuses = [
    { code: "PENDING_FOR_APPROVAL", name: t("PENDING_FOR_APPROVAL") },
    { code: "APPROVED", name: t("APPROVED") },
    { code: "REJECTED", name: t("REJECTED") },
  ];

  const [selectedStatuses, setSelectedStatuses] = useState(
    Array.isArray(searchParams?.applicationStatus) ? searchParams.applicationStatus : []
  );

  useEffect(() => {
    setSelectedStatuses(
      Array.isArray(searchParams?.applicationStatus) ? searchParams.applicationStatus : []
    );
  }, [searchParams?.applicationStatus]);

  /**
   * Toggles status selection in local component state.
   *
   * @param {Object} status - Status object with code and name.
   */
  const handleStatusChange = (status) => {
    let updated;
    if (selectedStatuses.includes(status.code)) {
      updated = selectedStatuses.filter((s) => s !== status.code);
    } else {
      updated = [...selectedStatuses, status.code];
    }
    setSelectedStatuses(updated);
  };

  /**
   * Dispatches updated status filters to the parent inbox controller.
   */
  const applyFilters = () => {
    onFilterChange({
      ...searchParams,
      applicationStatus: selectedStatuses,
    });
  };

  /**
   * Clears selected statuses and resets the inbox filter state.
   */
  const clearAll = () => {
    setSelectedStatuses([]);
    onFilterChange({
      ...searchParams,
      applicationStatus: [],
    });
  };

  /**
   * Retrieves record count for a given status code from the API statusMap.
   *
   * @param {string} statusCode - Application status code.
   * @returns {number|null} Record count if found, otherwise null.
   */
  const getStatusCount = (statusCode) => {
    if (!statusMap || !Array.isArray(statusMap)) return null;
    const match = statusMap.find((item) => item?.applicationstatus === statusCode);
    return match?.count != null ? match.count : null;
  };

  return (
    <div className="filter family-filter-card">
      <div className="filter-card">
        <div className="family-filter-header">
          <div className="family-filter-title-wrap">
            <svg width="17" height="17" viewBox="0 0 22 22" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path
                d="M0.66666 2.48016C3.35999 5.9335 8.33333 12.3335 8.33333 12.3335V20.3335C8.33333 21.0668 8.93333 21.6668 9.66666 21.6668H12.3333C13.0667 21.6668 13.6667 21.0668 13.6667 20.3335V12.3335C13.6667 12.3335 18.6267 5.9335 21.32 2.48016C22 1.60016 21.3733 0.333496 20.2667 0.333496H1.71999C0.613327 0.333496 -0.01334 1.60016 0.66666 2.48016Z"
                fill="#505A5F"
              />
            </svg>
            <span className="family-filter-title-text">{t("ES_COMMON_FILTER_BY")}:</span>
          </div>
          <span
            className="family-filter-refresh-btn"
            onClick={clearAll}
            role="button"
            tabIndex={0}
            title={t("ES_COMMON_CLEAR_ALL")}
          >
            <svg width="17" height="17" viewBox="0 0 16 22" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path
                d="M8 5V8L12 4L8 0V3C3.58 3 0 6.58 0 11C0 12.57 0.46 14.03 1.24 15.26L2.7 13.8C2.25 12.97 2 12.01 2 11C2 7.69 4.69 5 8 5ZM14.76 6.74L13.3 8.2C13.74 9.04 14 9.99 14 11C14 14.31 11.31 17 8 17V14L4 18L8 22V19C12.42 19 16 15.42 16 11C16 9.43 15.54 7.97 14.76 6.74Z"
                fill="#505A5F"
              />
            </svg>
          </span>
        </div>

        <div className="family-filter-section">
          <div className="filter-label family-filter-label">
            {t("CS_INBOX_STATUS_FILTER")}
          </div>
          {statuses.map((status) => {
            const count = getStatusCount(status.code);
            const labelText = count != null ? `${status.name} (${count})` : status.name;
            return (
              <div key={status.code} className="family-filter-checkbox-item">
                <CheckBox
                  onChange={() => handleStatusChange(status)}
                  checked={selectedStatuses.includes(status.code)}
                  label={labelText}
                />
              </div>
            );
          })}
        </div>

        <div className="family-filter-action">
          <SubmitBar label={t("ES_COMMON_APPLY")} onSubmit={applyFilters} />
        </div>
      </div>
    </div>
  );
};

export default Filter;
