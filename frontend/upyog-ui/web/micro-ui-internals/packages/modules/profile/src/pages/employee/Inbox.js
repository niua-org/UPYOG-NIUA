import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Header } from "@nudmcdgnpm/digit-ui-react-components";
import FamilyDesktopInbox from "../../components/FamilyDesktopInbox";
import { TableConfig } from "../../config/inbox-table-config";
import useInbox from "../../hooks/useInbox";

/**
 * Inbox component for Family Service applications.
 * Coordinates inbox search, pagination, status filtering, and desktop inbox rendering.
 * All styling is driven by family.scss classes without inline styles.
 *
 * @param {Object} props - Component properties.
 * @param {string} props.parentRoute - Base route prefix for inbox action links.
 * @returns {React.ReactElement} Employee inbox page.
 */
const Inbox = ({ parentRoute }) => {
  const { t } = useTranslation();
  const tenantId = Digit.ULBService.getCurrentTenantId();

  const [pageOffset, setPageOffset] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [searchParams, setSearchParams] = useState({});

  const { isFetching, isLoading, data } = useInbox({
    tenantId,
    filters: {
      ...searchParams,
      limit: pageSize,
      offset: pageOffset,
    },
  });

  const totalRecords = data?.[0]?.totalCount || 0;
  const statusMap = data?.[0]?.statusMap || [];
  const rows = data?.[0]?.dataEmpty ? [] : data || [];

  /**
   * Updates active status filters and resets pagination offset.
   *
   * @param {Object} filterParam - Status filter parameters.
   */
  const handleFilterChange = (filterParam) => {
    let keys_to_delete = filterParam.delete;
    let _new = { ...searchParams, ...filterParam };
    if (keys_to_delete) keys_to_delete.forEach((key) => delete _new[key]);
    delete filterParam.delete;
    setSearchParams({ ..._new });
    setPageOffset(0);
  };

  /**
   * Applies search bar parameters and resets pagination offset.
   *
   * @param {Object} searchData - Submitted search inputs.
   */
  const handleSearch = (searchData) => {
    setSearchParams((prev) => ({
      ...prev,
      ...searchData,
    }));
    setPageOffset(0);
  };

  /**
   * Clears search criteria and resets pagination.
   */
  const handleClearSearch = () => {
    setSearchParams({});
    setPageOffset(0);
  };

  /**
   * Advances table view to the next page slice.
   */
  const fetchNextPage = () => {
    setPageOffset((prev) => prev + pageSize);
  };

  /**
   * Returns table view to the previous page slice.
   */
  const fetchPrevPage = () => {
    setPageOffset((prev) => Math.max(0, prev - pageSize));
  };

  /**
   * Updates page size limit from user selection and resets offset.
   *
   * @param {React.ChangeEvent<HTMLSelectElement>} e - Change event.
   */
  const handlePageSizeChange = (e) => {
    setPageSize(Number(e.target.value));
    setPageOffset(0);
  };

  const columns = TableConfig(t).PROFILE.inboxColumns({ parentRoute });

  return (
    <div className="family-inbox-wrapper">
      <Header>{t("ES_COMMON_INBOX")}</Header>
      <FamilyDesktopInbox
        data={rows}
        columns={columns}
        isLoading={isLoading || isFetching}
        parentRoute={parentRoute}
        searchParams={searchParams}
        onFilterChange={handleFilterChange}
        onSearch={handleSearch}
        clearSearch={handleClearSearch}
        statusMap={statusMap}
        totalRecords={totalRecords}
        pageOffset={pageOffset}
        pageSize={pageSize}
        onNextPage={fetchNextPage}
        onPrevPage={fetchPrevPage}
        onPageSizeChange={handlePageSizeChange}
      />
    </div>
  );
};

export default Inbox;
