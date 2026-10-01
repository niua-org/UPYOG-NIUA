import React, { useCallback, useMemo } from "react";
import { useForm, Controller } from "react-hook-form";
import {
  TextInput,
  SubmitBar,
  DatePicker,
  CardLabelError,
  SearchForm,
  SearchField,
  Dropdown,
  Table,
  Card,
  MobileNumber,
  Loader,
  Header,
} from "@nudmcdgnpm/digit-ui-react-components";
import { Link } from "react-router-dom";
import { getGCStatusOptions } from "../utils";

/**
 * `GCSearchApplication` component provides a search interface for Garbage Collection (GC) applications.
 * It allows users to search, filter, and sort application records based on parameters like application number,
 * applicant name, mobile number, status, and date range.
 * 
 * Props:
 * - `tenantId`: Current tenant ID for data scoping
 * - `isLoading`: Boolean indicating if the search results are loading
 * - `t`: i18n translation function
 * - `onSubmit`: Callback invoked with form data on search submission
 * - `onClear`: Callback invoked when the clear button is clicked
 * - `data`: Array of search result objects to display in the table
 * - `count`: Total number of matching records (for pagination)
 * - `setShowToast`: Function to display toast notifications for validation errors
 */
const GCSearchApplication = ({ tenantId, isLoading, t, onSubmit, onClear, data, count, setShowToast }) => {
  const isMobile = window.Digit.Utils.browser.isMobile();
  const { register, control, handleSubmit, setValue, getValues, reset, formState } = useForm({
    defaultValues: {
      applicationNo: "",
      status: undefined,
      mobileNumber: "",
      fromDate: "",
      toDate: "",
      offset: 0,
      limit: !isMobile && 10,
    },
  });
  const user = Digit.UserService.getUser().info;
  const GetCell = (value) => <span className="cell-text">{value}</span>;

  const getStatusBadge = (status) => {
    if (!status) return <span className="cell-text">{t("CS_NA")}</span>;
    const s = String(status).toUpperCase();
    let badgeClass = "bg-blue-50 text-blue-700 border-blue-200";
    let dotClass = "bg-blue-500";

    if (s.includes("PAID") || s.includes("APPROVED")) {
      badgeClass = "bg-emerald-50 text-emerald-700 border-emerald-200";
      dotClass = "bg-emerald-500";
    } else if (s.includes("PENDING") || s.includes("PAYMENT")) {
      badgeClass = "bg-amber-50 text-amber-700 border-amber-200";
      dotClass = "bg-amber-500";
    } else if (s.includes("REJECT") || s.includes("CANCEL")) {
      badgeClass = "bg-red-50 text-red-700 border-red-200";
      dotClass = "bg-red-500";
    }

    return (
      <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold border ${badgeClass}`}>
        <span className={`w-1.5 h-1.5 rounded-full ${dotClass}`}></span>
        <span>{t(`GC_STATUS_${status}`) || status}</span>
      </span>
    );
  };

  const columns = useMemo(() => [
    {
      Header: t("GC_APPLICATION_NUMBER_LABEL"),
      accessor: "applicationNo",
      disableSortBy: true,
      Cell: ({ row }) => {
        const appNo = row.original?.grbgApplication?.applicationNo || row.original?.applicationNo;
        return (
          <div className="font-semibold text-red-700 hover:text-red-900 transition-colors">
            <Link to={`/upyog-ui/employee/gc/application-details/${encodeURIComponent(appNo)}`}>
              {appNo || t("CS_NA")}
            </Link>
          </div>
        );
      },
    },
    {
      Header: t("GC_APPLICANT_NAME"),
      disableSortBy: true,
      Cell: ({ row }) => {
        const owners = row.original?.applicantDetails || [];
        const name = owners?.map((o) => o?.name || o?.applicantName)?.join(", ") || row.original?.name || row.original?.garbageSpecification?.name;
        return <span className="text-slate-800 font-medium">{name || t("CS_NA")}</span>;
      },
    },
    {
      Header: t("GC_MOBILE_NUMBER"),
      disableSortBy: true,
      Cell: ({ row }) => {
        const owners = row.original?.applicantDetails || [];
        const mobile = owners?.map((o) => o?.mobileNumber)?.join(", ") || row.original?.mobileNumber || row.original?.garbageSpecification?.phoneNumber;
        return <span className="text-slate-600">{mobile || t("CS_NA")}</span>;
      },
    },
    {
      Header: t("PT_COMMON_TABLE_COL_STATUS_LABEL"),
      disableSortBy: true,
      Cell: ({ row }) => {
        const status = row.original?.applicationStatus || row.original?.status || row.original?.grbgApplication?.status;
        return getStatusBadge(status);
      },
    },
  ], [t]);

  const statusOptions = getGCStatusOptions(t);

  const onSort = useCallback((args) => {
    if (args.length === 0) return;
    setValue("sortBy", args.id);
    setValue("sortOrder", args.desc ? "DESC" : "ASC");
  }, []);

  function onPageSizeChange(e) {
    setValue("limit", Number(e.target.value));
    handleSubmit(onSubmit)();
  }

  function nextPage() {
    setValue("offset", getValues("offset") + getValues("limit"));
    handleSubmit(onSubmit)();
  }

  function previousPage() {
    setValue("offset", getValues("offset") - getValues("limit"));
    handleSubmit(onSubmit)();
  }

  return (
    <React.Fragment>
      <div style={{ padding: user?.type === "CITIZEN" ? "0 24px" : "0" }}>
        <Header styles={{ fontSize: "28px", marginBottom: "8px" }}>{t("GC_SEARCH_APPLICATION")}</Header>
        
        <Card className="gc-emp-search-card mb-6">
          <div className="text-xs text-slate-500 mb-4 pb-2 border-b border-slate-100">
            {t("PROVIDE_ATLEAST_ONE_PARAMETERS")}
          </div>

          <SearchForm onSubmit={onSubmit} handleSubmit={handleSubmit} className="gc-emp-search-form">
            <div className="gc-emp-search-grid">
              {/* Row 1 - Col 1: Application Number */}
              <div className="gc-emp-field">
                <label className="gc-emp-label">{t("GC_APPLICATION_NUMBER_LABEL")}</label>
                <Controller
                  control={control}
                  name="applicationNo"
                  render={({ field }) => (
                    <TextInput
                      name={field.name}
                      value={field.value}
                      placeholder="Enter Application No"
                      onChange={field.onChange}
                      onBlur={field.onBlur}
                      inputRef={field.ref}
                    />
                  )}
                />
              </div>

              {/* Row 1 - Col 2: Status */}
              <div className="gc-emp-field">
                <label className="gc-emp-label">{t("PT_COMMON_TABLE_COL_STATUS_LABEL")}</label>
                <Controller
                  control={control}
                  name="status"
                  render={({ field }) => (
                    <Dropdown
                      selected={field.value}
                      select={field.onChange}
                      onBlur={field.onBlur}
                      option={statusOptions}
                      optionKey="i18nKey"
                      t={t}
                      placeholder="Select Status"
                      disable={false}
                    />
                  )}
                />
              </div>

              {/* Row 1 - Col 3: Mobile Number */}
              <div className="gc-emp-field">
                <label className="gc-emp-label">{t("GC_MOBILE_NUMBER")}</label>
                <Controller
                  control={control}
                  name="mobileNumber"
                  rules={{
                    minLength: { value: 10, message: t("CORE_COMMON_MOBILE_ERROR") },
                    maxLength: { value: 10, message: t("CORE_COMMON_MOBILE_ERROR") },
                    pattern: { value: /[6789][0-9]{9}/, message: t("CORE_COMMON_MOBILE_ERROR") },
                  }}
                  render={({ field }) => (
                    <MobileNumber
                      name={field.name}
                      value={field.value}
                      onChange={field.onChange}
                      onBlur={field.onBlur}
                      inputRef={field.ref}
                    />
                  )}
                />
                <CardLabelError>{formState?.errors?.["mobileNumber"]?.message}</CardLabelError>
              </div>

              {/* Row 2 - Col 1: From Date */}
              <div className="gc-emp-field">
                <label className="gc-emp-label">{t("FROM_DATE")}</label>
                <Controller
                  render={({ field }) => (
                    <DatePicker
                      date={field.value}
                      disabled={false}
                      onChange={field.onChange}
                      max={new Date().toISOString().split("T")[0]}
                    />
                  )}
                  name="fromDate"
                  control={control}
                />
              </div>

              {/* Row 2 - Col 2: To Date */}
              <div className="gc-emp-field">
                <label className="gc-emp-label">{t("TO_DATE")}</label>
                <Controller
                  render={({ field }) => (
                    <DatePicker
                      date={field.value}
                      disabled={false}
                      onChange={field.onChange}
                    />
                  )}
                  name="toDate"
                  control={control}
                />
              </div>

              {/* Row 2 - Col 3: Actions (Clear All & Search) */}
              <div className="gc-emp-action-buttons">
                <button
                  type="button"
                  className="gc-clear-btn"
                  onClick={() => {
                    reset({ applicationNo: "", fromDate: "", toDate: "", mobileNumber: "", status: undefined, offset: 0, limit: 10 });
                    setShowToast(null);
                    onClear();
                  }}
                >
                  {t("ES_COMMON_CLEAR_ALL")}
                </button>
                <SubmitBar label={t("ES_COMMON_SEARCH")} submit className="gc-search-submit-btn" />
              </div>
            </div>
          </SearchForm>
        </Card>

        {/* Results Table */}
        {!isLoading && data?.display ? (
          <Card className="p-8 text-center text-slate-500 border border-dashed border-slate-200">
            {t(data.display).split("\\n").map((text, index) => (
              <p key={index} className="text-base font-medium">{text}</p>
            ))}
          </Card>
        ) : !isLoading && data !== "" ? (
          <div className="gc-emp-table-wrap">
            <Table
              t={t}
              data={data}
              totalRecords={count}
              columns={columns}
              getCellProps={(cellInfo) => ({
                style: {
                  minWidth: cellInfo.column.Header === t("GC_APPLICATION_NUMBER_LABEL") ? "220px" : "",
                  padding: "16px 20px",
                  fontSize: "14px",
                },
              })}
              onPageSizeChange={onPageSizeChange}
              currentPage={getValues("offset") / getValues("limit")}
              onNextPage={nextPage}
              onPrevPage={previousPage}
              pageSizeLimit={getValues("limit")}
              disableSort={true}
            />
          </div>
        ) : (
          data !== "" || (isLoading && <Loader />)
        )}
      </div>
    </React.Fragment>
  );
};

export default GCSearchApplication;