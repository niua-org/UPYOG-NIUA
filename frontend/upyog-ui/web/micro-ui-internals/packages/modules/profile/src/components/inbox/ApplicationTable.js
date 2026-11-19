import React from "react";
import { Table } from "@nudmcdgnpm/digit-ui-react-components";

/**
 * ApplicationTable component wrapping the core Digit Table atom.
 * Provides unified table rendering with pagination, sorting, and custom cell styling
 * for Family Service application lists.
 *
 * @param {Object} props - Component properties.
 * @param {Function} props.t - Localization translation function.
 * @param {number} props.currentPage - Zero-based index of active page.
 * @param {Array<Object>} props.columns - React Table column definitions.
 * @param {Array<Object>} props.data - Row items for the current page.
 * @param {Function} props.getCellProps - Custom property and styling callback per table cell.
 * @param {boolean} [props.disableSort=false] - Whether sorting should be disabled.
 * @param {Function} [props.onSort] - Callback when a sortable column header is clicked.
 * @param {Function} props.onNextPage - Handler to advance to next page.
 * @param {Function} props.onPrevPage - Handler to go back to previous page.
 * @param {Function} props.onPageSizeChange - Handler when rows per page limit is changed.
 * @param {boolean} [props.isPaginationRequired=true] - Whether to render pagination controls.
 * @param {number} props.pageSizeLimit - Number of rows displayed per page.
 * @param {Object} [props.sortParams] - Current sort column and direction parameters.
 * @param {number} props.totalRecords - Total number of records across all pages.
 * @returns {React.ReactElement} Table component.
 */
const ApplicationTable = ({
  t,
  currentPage,
  columns,
  data,
  getCellProps,
  disableSort,
  onSort,
  onNextPage,
  onPrevPage,
  onPageSizeChange,
  isPaginationRequired = true,
  pageSizeLimit,
  sortParams,
  totalRecords,
}) => {
  return (
    <Table
      t={t}
      data={data}
      currentPage={currentPage}
      columns={columns}
      getCellProps={getCellProps}
      onNextPage={onNextPage}
      onPrevPage={onPrevPage}
      pageSizeLimit={pageSizeLimit}
      disableSort={disableSort}
      isPaginationRequired={isPaginationRequired}
      onPageSizeChange={onPageSizeChange}
      onSort={onSort}
      sortParams={sortParams}
      totalRecords={totalRecords}
    />
  );
};

export default ApplicationTable;
