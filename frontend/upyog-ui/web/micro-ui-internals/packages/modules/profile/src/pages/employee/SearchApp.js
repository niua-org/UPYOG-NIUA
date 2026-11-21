import React, { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { useForm, Controller } from "react-hook-form";
import {
  Header,
  Card,
  SearchForm,
  SearchField,
  TextInput,
  Dropdown,
  SubmitBar,
  Table,
  Loader,
  Toast,
} from "@nudmcdgnpm/digit-ui-react-components";

/**
 * Utility renderer for table cell text content.
 *
 * @param {string|number} value - Text or numerical content.
 * @returns {React.ReactElement} Formatted span cell element.
 */
const GetCell = (value) => <span className="cell-text">{value}</span>;

/**
 * Utility badge renderer for application status codes.
 *
 * @param {string} status - Application status code (e.g. APPROVED, PENDING_FOR_APPROVAL).
 * @param {Function} t - Localization translation function.
 * @returns {React.ReactElement} Formatted status badge element.
 */
const GetStatusBadge = (status, t) => {
  let badgeClass = "status-default";
  if (status === "APPROVED") badgeClass = "status-approved";
  else if (status === "PENDING_FOR_APPROVAL") badgeClass = "status-pending";
  else if (status === "REJECTED") badgeClass = "status-rejected";

  return (
    <span className={`family-status-badge ${badgeClass}`}>
      {t(status)}
    </span>
  );
};

/**
 * SearchApp component for Family Service application search.
 * Allows employees to search for families by Family ID, Mobile Number, or Application Status.
 * Uses family.scss stylesheet classes for layout, typography, and styling.
 *
 * @param {Object} props - Component properties.
 * @param {string} props.parentRoute - Base route prefix for navigation links.
 * @returns {React.ReactElement} Search application page.
 */
const SearchApp = ({ parentRoute }) => {
  const { t } = useTranslation();
  const tenantId = Digit.ULBService.getCurrentTenantId();
  const isMobile = window.Digit.Utils.browser.isMobile();

  const { register, control, handleSubmit, setValue, getValues, reset } = useForm({
    defaultValues: {
      familyId: "",
      mobileNumber: "",
      status: undefined,
      offset: 0,
      limit: !isMobile && 10,
    },
  });

  const [isLoading, setIsLoading] = useState(false);
  const [data, setData] = useState(null);
  const [showToast, setShowToast] = useState(null);
  const [count, setCount] = useState(undefined);

  useEffect(() => {
    register("offset");
    register("limit");
  }, [register]);

  /**
   * Available application status dropdown filter options.
   */
  const statusOptions = useMemo(
    () => [
      { code: "PENDING_FOR_APPROVAL", name: t("PENDING_FOR_APPROVAL") },
      { code: "APPROVED", name: t("APPROVED") },
      { code: "REJECTED", name: t("REJECTED") },
    ],
    [t]
  );

  /**
   * Updates page size and triggers search with reset offset (WT pattern).
   *
   * @param {Event} e - Change event from rows per page dropdown.
   */
  function onPageSizeChange(e) {
    setValue("limit", Number(e.target.value));
    setValue("offset", 0);
    handleSubmit(onSubmit)();
  }

  /**
   * Advances to next page and triggers search (WT pattern).
   */
  function nextPage() {
    setValue("offset", getValues("offset") + getValues("limit"));
    handleSubmit(onSubmit)();
  }

  /**
   * Navigates to previous page and triggers search (WT pattern).
   */
  function previousPage() {
    setValue("offset", getValues("offset") - getValues("limit"));
    handleSubmit(onSubmit)();
  }

  /**
   * Submits search request to backend with current filters and pagination values.
   *
   * @param {Object} _data - Form data containing filters, offset, and limit.
   */
  const onSubmit = async (_data) => {
    const familyId = _data?.familyId?.trim();
    const mobileNumber = _data?.mobileNumber?.trim();
    const statusCode =
      _data?.status?.code && _data.status.code !== "ALL"
        ? _data.status.code
        : undefined;

    const offset = _data?.offset !== undefined ? _data.offset : getValues("offset") || 0;
    const limit = _data?.limit !== undefined ? _data.limit : getValues("limit") || 10;

    if (!familyId && !mobileNumber && !statusCode) {
      setShowToast({ error: true, label: t("ES_SEARCH_APPLICATION_CRITERIA_MANDATORY")});
      return;
    }

    if (mobileNumber && !/^[6-9]\d{9}$/.test(mobileNumber)) {
      setShowToast({ error: true, label: t("CORE_COMMON_APPLICANT_MOBILE_NUMBER_INVALID")});
      return;
    }

    try {
      setIsLoading(true);
      setShowToast(null);

      const params = {
        tenantId,
        limit,
        offset,
      };
      if (familyId) params.familyId = familyId;
      if (mobileNumber) params.mobileNumber = mobileNumber;
      if (statusCode) params.applicationStatus = statusCode;

      const response = await Digit.UserService.familySearch(tenantId, params);
      const families = response?.Families || [];

      setData(families);

      const respTotal = response?.totalCount;
      if (typeof respTotal === "number" && respTotal > offset + families.length) {
        setCount(respTotal);
      } else if (families.length < limit) {
        setCount(offset + families.length);
      } else {
        setCount(undefined);
      }
    } catch (err) {
      setShowToast({ error: true, label: t("ES_FAMILY_FETCH_FAILED") });
      setData([]);
      setCount(0);
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * Handles user submission from search form (resets offset to 0).
   *
   * @param {Object} formData - Form input values.
   */
  const handleSearch = (formData) => {
    setValue("offset", 0);
    onSubmit({ ...formData, offset: 0 });
  };

  /**
   * Resets all search form fields and clears result table.
   */
  const handleClear = () => {
    reset({
      familyId: "",
      mobileNumber: "",
      status: undefined,
      offset: 0,
      limit: 10,
    });
    setShowToast(null);
    setData([]);
    setCount(0);
  };

  /**
   * Column definitions for the search results table.
   */
  const columns = useMemo(
    () => [
      {
        Header: t("ES_INBOX_APPLICATION_NO"),
        accessor: "applicationNo",
        Cell: ({ row }) => {
          const fid = row?.original?.familyId;
          const appNo = row?.original?.applicationNo;
          return (
            <div>
              <span className="link">
                <Link to={`${parentRoute}/application-details/${fid}`}>{appNo}</Link>
              </span>
            </div>
          );
        },
      },
      {
        Header: t("CORE_COMMON_FAMILY_ID"),
        accessor: "familyId",
        Cell: ({ row }) => {
          const fid = row?.original?.familyId;
          return (
            <div>
              <span className="link">
                <Link to={`${parentRoute}/application-details/${fid}`}>{fid}</Link>
              </span>
            </div>
          );
        },
      },
      {
        Header: t("HEAD_OF_FAMILY"),
        Cell: ({ row }) => {
          return GetCell(row?.original?.headName);
        },
      },
      {
        Header: t("ES_PROFILE_MOBILE_NUMBER"),
        Cell: ({ row }) => {
          const head = row?.original?.members?.find((m) => m.isHead);
          return GetCell(head?.mobileNumber);
        },
      },
      {
        Header: t("CS_INBOX_STATUS_FILTER"),
        Cell: ({ row }) => {
          return GetStatusBadge(row?.original?.applicationStatus, t);
        },
      },
    ],
    [parentRoute, t]
  );

  return (
    <React.Fragment>
      <Header>{t("ES_COMMON_APPLICATION_SEARCH")}</Header>

      <SearchForm onSubmit={handleSearch} handleSubmit={handleSubmit}>
        <SearchField>
          <label>{t("CORE_COMMON_FAMILY_ID")}</label>
          <Controller
            control={control}
            name="familyId"
            render={({ field }) => (
              <TextInput
                name={field.name}
                value={field.value}
                onChange={field.onChange}
                onBlur={field.onBlur}
                inputRef={field.ref}
              />
            )}
          />
        </SearchField>

        <SearchField>
          <label>{t("CORE_COMMON_PROFILE_MOBILE_NUMBER")}</label>
          <Controller
            control={control}
            name="mobileNumber"
            render={({ field }) => (
              <TextInput
                name={field.name}
                value={field.value}
                onChange={field.onChange}
                onBlur={field.onBlur}
                inputRef={field.ref}
              />
            )}
          />
        </SearchField>

        <SearchField>
          <label>{t("CS_INBOX_STATUS_FILTER")}</label>
          <Controller
            control={control}
            name="status"
            render={({ field }) => (
              <Dropdown
                selected={field.value}
                select={field.onChange}
                onBlur={field.onBlur}
                option={statusOptions}
                optionKey="name"
                t={t}
              />
            )}
          />
        </SearchField>

        <SearchField className="submit">
          <SubmitBar label={t("ES_COMMON_SEARCH")} submit />
          <p
            className="family-search-clear-btn"
            onClick={handleClear}
            role="button"
            tabIndex={0}
          >
            {t("ES_COMMON_CLEAR_ALL")}
          </p>
        </SearchField>
      </SearchForm>

      {isLoading && <Loader />}

      {!isLoading && data !== null && (
        <React.Fragment>
          {data.length === 0 ? (
            <Card className="family-empty-card">
              <p className="family-empty-title">{t("CS_MYAPPLICATIONS_NO_APPLICATION")}</p>
            </Card>
          ) : (
            <Table
              t={t}
              data={data}
              totalRecords={count}
              columns={columns}
              getCellProps={(cellInfo) => ({
                className: `family-table-cell ${
                  cellInfo.column.Header === t("ES_INBOX_APPLICATION_NO") ||
                  cellInfo.column.Header === t("CORE_COMMON_FAMILY_ID")
                    ? "family-id-cell"
                    : ""
                }`,
              })}
              onPageSizeChange={onPageSizeChange}
              currentPage={getValues("offset") / getValues("limit")}
              onNextPage={nextPage}
              onPrevPage={previousPage}
              pageSizeLimit={getValues("limit")}
              disableSort={true}
            />
          )}
        </React.Fragment>
      )}

      {showToast && (
        <Toast
          error={showToast.error}
          warning={showToast.warning}
          label={showToast.label}
          onClose={() => setShowToast(null)}
        />
      )}
    </React.Fragment>
  );
};

export default SearchApp;
