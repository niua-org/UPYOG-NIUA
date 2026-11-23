import React from "react";
import { Card, Loader } from "@nudmcdgnpm/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import ApplicationTable from "./inbox/ApplicationTable";
import Filter from "./inbox/Filter";
import SearchApplication from "./inbox/SearchApplication";
import InboxLinks from "./inbox/InboxLinks";

/**
 * FamilyDesktopInbox component providing standard desktop layout for the Family Service Inbox.
 * Organizes sidebar filters, search bar, and the application results table.
 * All styling is driven by family.scss classes without inline styles.
 *
 * @param {Object} props - Component properties.
 * @param {Array<Object>} props.data - Inbox row items to display in table.
 * @param {Array<Object>} props.columns - React table column definitions.
 * @param {boolean} props.isLoading - Whether the inbox data query is currently loading.
 * @param {string} props.parentRoute - Base routing path for navigation links.
 * @param {Object} props.searchParams - Active search and filter parameters.
 * @param {Function} props.onFilterChange - Callback when sidebar filters are modified.
 * @param {Function} props.onSearch - Callback when search form is submitted.
 * @param {Function} props.clearSearch - Callback to clear active search criteria.
 * @param {Array<Object>} props.statusMap - Status count distribution array from inbox search response.
 * @param {number} props.totalRecords - Total number of matching records for pagination.
 * @param {number} props.pageOffset - Current page record offset index.
 * @param {number} props.pageSize - Number of records displayed per page.
 * @param {Function} props.onNextPage - Handler to advance to next table page.
 * @param {Function} props.onPrevPage - Handler to return to previous table page.
 * @param {Function} props.onPageSizeChange - Handler when user changes page size limit.
 * @returns {React.ReactElement} Desktop inbox view.
 */
const FamilyDesktopInbox = ({
  data,
  columns,
  isLoading,
  parentRoute,
  searchParams,
  onFilterChange,
  onSearch,
  clearSearch,
  statusMap,
  totalRecords,
  pageOffset,
  pageSize,
  onNextPage,
  onPrevPage,
  onPageSizeChange,
}) => {
  const { t } = useTranslation();

  let result;
  if (isLoading) {
    result = <Loader />;
  } else if (!data || data.length === 0 || data?.[0]?.dataEmpty) {
    result = (
      <Card className="family-empty-card">
        <p className="family-empty-title">
          {t("CS_MYAPPLICATIONS_NO_APPLICATION")}
        </p>
      </Card>
    );
  } else {
    result = (
      <ApplicationTable
        t={t}
        data={data}
        columns={columns}
        getCellProps={(cellInfo) => ({
          className: `family-table-cell ${
            cellInfo.column.id === "familyId" || cellInfo.column.id === "applicationNo"
              ? "family-id-cell"
              : ""
          }`,
        })}
        currentPage={Math.floor(pageOffset / pageSize)}
        pageSizeLimit={pageSize}
        onNextPage={onNextPage}
        onPrevPage={onPrevPage}
        onPageSizeChange={onPageSizeChange}
        totalRecords={totalRecords}
      />
    );
  }

  return (
    <div className="inbox-container family-desktop-inbox">
      <div className="filters-container">
        <InboxLinks />
        <Filter
          searchParams={searchParams}
          onFilterChange={onFilterChange}
          statusMap={statusMap}
        />
      </div>

      <div className="family-inbox-content">
        <SearchApplication
          searchParams={searchParams}
          onSearch={onSearch}
          clearSearch={clearSearch}
        />
        <div className="result inbox-result--with-margin family-inbox-result">
          {result}
        </div>
      </div>
    </div>
  );
};

export default FamilyDesktopInbox;
