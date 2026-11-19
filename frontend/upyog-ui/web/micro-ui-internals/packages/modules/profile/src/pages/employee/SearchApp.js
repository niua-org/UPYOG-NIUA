import React, { useMemo, useState } from "react";
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

  const { control, handleSubmit, reset } = useForm({
    defaultValues: {
      familyId: "",
      mobileNumber: "",
      status: undefined,
    },
  });

  const [isLoading, setIsLoading] = useState(false);
  const [data, setData] = useState(null);
  const [showToast, setShowToast] = useState(null);
  const [pageOffset, setPageOffset] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  /**
   * Available application status dropdown filter options.
   */
  const statusOptions = useMemo(
    () => [
      { code: "ALL", name: t("ALL") },
      { code: "PENDING_FOR_APPROVAL", name: t("PENDING_FOR_APPROVAL") },
      { code: "APPROVED", name: t("APPROVED") },
      { code: "REJECTED", name: t("REJECTED") },
    ],
    [t]
  );

  /**
   * Handles submission of the application search form.
   *
   * @param {Object} formData - Form input values.
   */
  const onSubmit = async (formData) => {
    const familyId = formData?.familyId?.trim();
    const mobileNumber = formData?.mobileNumber?.trim();
    const statusCode =
      formData?.status?.code && formData.status.code !== "ALL"
        ? formData.status.code
        : undefined;

    if (!familyId && !mobileNumber && !statusCode) {
      setShowToast({ warning: true, label: t("PROVIDE_ATLEAST_ONE_PARAMETERS") });
      return;
    }

    try {
      setIsLoading(true);
      setShowToast(null);

      const params = {
        tenantId,
        limit: 50,
        offset: 0,
      };
      if (familyId) params.familyId = familyId;
      if (mobileNumber) params.mobileNumber = mobileNumber;
      if (statusCode) params.applicationStatus = statusCode;

      const response = await Digit.UserService.familySearch(tenantId, params);
      const families = response?.Families || [];

      let filtered = families;
      if (statusCode) {
        filtered = families.filter((f) => f?.applicationStatus === statusCode);
      }

      setData(filtered);
      setPageOffset(0);
    } catch (err) {
      setShowToast({ error: true, label: t("ES_FAMILY_FETCH_FAILED") });
      setData([]);
    } finally {
      setIsLoading(false);
    }
  };

  /**
   * Resets all search form fields and clears result table.
   */
  const handleClear = () => {
    reset({
      familyId: "",
      mobileNumber: "",
      status: undefined,
    });
    setData(null);
    setShowToast(null);
    setPageOffset(0);
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

  /**
   * Client-side paginated slice of current search result records.
   */
  const paginatedData = useMemo(() => {
    if (!Array.isArray(data)) return [];
    return data.slice(pageOffset, pageOffset + pageSize);
  }, [data, pageOffset, pageSize]);

  return (
    <React.Fragment>
      <Header>{t("ES_COMMON_APPLICATION_SEARCH")}</Header>
      <Card className={"card-search-heading"}>
        <span className="family-search-helper-text">
          {t("PROVIDE_ATLEAST_ONE_PARAMETERS")}
        </span>
      </Card>

      <SearchForm onSubmit={onSubmit} handleSubmit={handleSubmit}>
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
              data={paginatedData}
              totalRecords={data.length}
              columns={columns}
              getCellProps={(cellInfo) => ({
                className: `family-table-cell ${
                  cellInfo.column.Header === t("ES_INBOX_APPLICATION_NO") ||
                  cellInfo.column.Header === t("CORE_COMMON_FAMILY_ID")
                    ? "family-id-cell"
                    : ""
                }`,
              })}
              onPageSizeChange={(e) => {
                setPageSize(Number(e.target.value));
                setPageOffset(0);
              }}
              currentPage={Math.floor(pageOffset / pageSize)}
              onNextPage={() => setPageOffset((prev) => prev + pageSize)}
              onPrevPage={() => setPageOffset((prev) => Math.max(0, prev - pageSize))}
              pageSizeLimit={pageSize}
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
