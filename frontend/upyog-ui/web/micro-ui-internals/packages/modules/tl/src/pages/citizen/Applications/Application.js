/**
 * Citizen My Applications — same layout as Estate.
 * Header, filters, card rows, and buttons come from
 * config/CitizenMyApplications.json. MDMS TradeLicense.CitizenMyApplicationsConfig
 * replaces that file when the master is present.
 * Page load searches by tenant only. The trade-license service then limits
 * the result to the logged-in citizen. JSON filters run on the client.
 */
import React, { useCallback, useMemo, useState } from "react";
import {
  Header,
  Loader,
  TextInput,
  Dropdown,
  SubmitBar,
  CardLabel,
  Card,
  KeyNote,
  sortByOrder,
} from "@nudmcdgnpm/digit-ui-react-components";
import { Link, useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";
import TLApplicationCard from "./tl-application";
import localMyApplications from "../../../config/CitizenMyApplications.json";
import {
  applyMyApplicationFilters,
  resolveCitizenMyApplicationsConfig,
  sortMyApplications,
} from "../../../utils/tlMyApplicationsConfig";
import styles from "../../../styles/TLMyApplications.module.scss";

/** Bill cards for view="bills". The list view below is the config-driven screen. */
const TLBills = () => {
  const { t } = useTranslation();
  const { mobileNumber, tenantId: userTenantId } = Digit.UserService.getUser()?.info || {};
  const tenantId =
    Digit.ULBService.getCitizenCurrentTenant(true) ||
    Digit.ULBService.getCurrentTenantId() ||
    userTenantId;
  const { isLoading, data } = Digit.Hooks.tl.useFetchBill({
    params: {
      businessService: "TL",
      tenantId,
      mobileNumber,
    },
    config: { enabled: true },
  });

  if (isLoading) return <Loader />;

  return (
    <>
      <Header>{t("TL_MY_APPLICATIONS_HEADER")}</Header>
      {(data || []).map((application, index) => (
        <div key={application?.raw?.applicationNumber || index}>
          <Card>
            {Object.keys(application)
              .filter((key) => key !== "raw" && application[key] !== null)
              .map((item) => (
                <KeyNote key={item} keyValue={t(item)} note={t(application[item])} />
              ))}
            <Link
              to={`/upyog-ui/citizen/tl/tradelicence/application/${application?.raw?.applicationNumber}/${application.raw?.tenantId}`}
            >
              <SubmitBar
                label={t(
                  application?.raw?.status !== "PENDINGPAYMENT"
                    ? "TL_VIEW_DETAILS"
                    : "TL_VIEW_DETAILS_PAY"
                )}
              />
            </Link>
          </Card>
        </div>
      ))}
    </>
  );
};

/** Inline JSON options win. Otherwise the MDMS master named on the filter. */
const dropdownOptions = (filter, mdmsOptions, t) => {
  if (Array.isArray(filter.options) && filter.options.length > 0) {
    return filter.options.map((item) => ({
      code: item.code,
      name: t(item.i18nKey || item.name || item.code),
    }));
  }
  return (mdmsOptions || []).map((item) => ({
    code: item.code,
    name: t(item.i18nKey || item.name || item.code),
  }));
};

const TLMyApplicationsList = () => {
  const { t } = useTranslation();
  const location = useLocation();
  const navigate = Digit.Hooks.useCustomNavigate();
  const { tenantId: userTenantId } = Digit.UserService.getUser()?.info || {};
  const tenantId =
    Digit.ULBService.getCitizenCurrentTenant(true) ||
    Digit.ULBService.getCurrentTenantId() ||
    userTenantId;
  const stateId = Digit.ULBService.getStateId();

  // MDMS replaces CitizenMyApplications.json when that master exists.
  const { data: mdmsMyApps } = Digit.Hooks.useEnabledMDMS(
    stateId,
    "TradeLicense",
    [{ name: "CitizenMyApplicationsConfig" }],
    {
      select: (mdms) => mdms?.TradeLicense?.CitizenMyApplicationsConfig || null,
    }
  );

  const config = useMemo(
    () => resolveCitizenMyApplicationsConfig(localMyApplications, mdmsMyApps),
    [mdmsMyApps]
  );

  const filters = useMemo(() => sortByOrder(config.filters || []), [config.filters]);
  const resultFields = useMemo(
    () => sortByOrder(config.resultFields || []),
    [config.resultFields]
  );
  const pageSize = config.paginationDefaults?.limit || 50;
  const listRoute =
    config.listRoute || "/upyog-ui/citizen/tl/tradelicence/my-application";

  // Only a dropdown that has no inline options needs an MDMS master.
  const mdmsDropdown = filters.find(
    (filter) =>
      filter.type === "dropdown" &&
      filter.dataSource?.type === "MDMS" &&
      !(Array.isArray(filter.options) && filter.options.length > 0)
  );
  const statusModule = mdmsDropdown?.dataSource?.moduleName || "TradeLicense";
  const statusMaster = mdmsDropdown?.dataSource?.masterName;

  const { data: statusMdms = [] } = Digit.Hooks.useEnabledMDMS(
    stateId,
    statusModule,
    [{ name: statusMaster || "WorkflowStatus" }],
    {
      enabled: Boolean(statusMaster && stateId),
      select: (mdms) => mdms?.[statusModule]?.[statusMaster] || [],
    }
  );

  const [draft, setDraft] = useState({});
  const [appliedFilters, setAppliedFilters] = useState({});

  // /my-application/50 is the next page. A non-numeric tail means the first page.
  const pathOffset = useMemo(() => {
    const segment = location.pathname.split("/").pop();
    const parsed = parseInt(segment, 10);
    return Number.isNaN(parsed) ? 0 : parsed;
  }, [location.pathname]);

  const apiFilters = useMemo(
    () => ({
      tenantId,
      offset: config.searchDefaults?.offset ?? 0,
      limit: config.searchDefaults?.limit ?? 100,
    }),
    [tenantId, config.searchDefaults]
  );

  const { isLoading, error, data } = Digit.Hooks.tl.useTradeLicenseSearch(
    { tenantId, filters: apiFilters },
    { enabled: Boolean(tenantId), structuralSharing: false }
  );

  // Filters run on the loaded page, then paginationDefaults sorts the cards.
  const applications = useMemo(() => {
    const list = data?.Licenses || data?.licenses || [];
    return sortMyApplications(
      applyMyApplicationFilters(list, filters, appliedFilters),
      config.paginationDefaults
    );
  }, [data, filters, appliedFilters, config.paginationDefaults]);

  const visibleApplications = useMemo(
    () => applications.slice(pathOffset, pathOffset + pageSize),
    [applications, pathOffset, pageSize]
  );

  const resetPage = useCallback(() => {
    if (pathOffset) navigate(listRoute);
  }, [pathOffset, navigate, listRoute]);

  // Search applies the draft. Dropdowns store the code; text stores the trimmed string.
  const handleSearch = useCallback(() => {
    const next = {};
    filters.forEach((filter) => {
      const value = draft[filter.name];
      if (filter.type === "dropdown") {
        if (value?.code) next[filter.name] = value.code;
        return;
      }
      if (typeof value === "string" && value.trim()) next[filter.name] = value.trim();
    });
    resetPage();
    setAppliedFilters(next);
  }, [draft, filters, resetPage]);

  const handleClear = useCallback(() => {
    setDraft({});
    setAppliedFilters({});
    resetPage();
  }, [resetPage]);

  if (isLoading && !data) return <Loader />;

  const totalCount = applications.length;
  const nextOffset = pathOffset + pageSize;
  const hasMore = nextOffset < totalCount;

  return (
    <>
      <Header>{`${t(config.header)} (${totalCount})`}</Header>
      <Card>
        <div className={styles["tl-myapps__container"]}>
          <div className={styles["tl-myapps__search-row"]}>
            {filters.map((filter) => (
              <div key={filter.name} className={styles["tl-myapps__field-col"]}>
                <div className={styles["tl-myapps__field-inner"]}>
                  <CardLabel>{t(filter.key)}</CardLabel>
                  {filter.type === "dropdown" ? (
                    <Dropdown
                      className={`form-field ${styles["tl-myapps__dropdown"]}`}
                      selected={draft[filter.name] || null}
                      select={(option) =>
                        setDraft((current) => ({ ...current, [filter.name]: option }))
                      }
                      option={dropdownOptions(
                        filter,
                        filter.name === mdmsDropdown?.name ? statusMdms : [],
                        t
                      )}
                      placeholder={t(filter.placeholder || filter.key)}
                      optionKey="name"
                      t={t}
                    />
                  ) : (
                    <TextInput
                      placeholder={t(filter.placeholder || filter.key)}
                      value={draft[filter.name] || ""}
                      onChange={(event) =>
                        setDraft((current) => ({
                          ...current,
                          [filter.name]: event.target.value,
                        }))
                      }
                      className={styles["tl-myapps__text-input"]}
                    />
                  )}
                </div>
              </div>
            ))}
            <div>
              <div className={styles["tl-myapps__search-btn-wrap"]}>
                <SubmitBar
                  label={t(config.actionButton?.search || "ES_COMMON_SEARCH")}
                  onSubmit={handleSearch}
                />
                <p
                  className={`link ${styles["tl-myapps__clear-link"]}`}
                  onClick={handleClear}
                  onKeyDown={(event) => {
                    // The clear control is a link-styled paragraph, so Enter and Space must work too.
                    if (event.key !== "Enter" && event.key !== " ") return;
                    event.preventDefault();
                    handleClear();
                  }}
                  role="button"
                  tabIndex={0}
                >
                  {t(config.actionButton?.clear || "ES_COMMON_CLEAR_ALL")}
                </p>
              </div>
            </div>
          </div>
        </div>
      </Card>

      {error ? (
        <p className={styles["tl-myapps__msg"]}>
          {t(config.messages?.error || "CS_SOMETHING_WENT_WRONG")}
        </p>
      ) : null}

      {!error &&
        visibleApplications.map((license, index) => (
          <div key={`${license.applicationNumber || license.id || "row"}-${index}`}>
            <TLApplicationCard
              application={license}
              resultFields={resultFields}
              cardActions={config.cardActions || []}
              emptyValue={config.emptyValue}
            />
          </div>
        ))}

      {!error && !isLoading && totalCount === 0 ? (
        <p className={styles["tl-myapps__msg"]}>
          {t(config.emptyState?.message || "PT_NO_APPLICATION_FOUND_MSG")}
        </p>
      ) : null}

      {hasMore ? (
        <p className={styles["tl-myapps__msg"]}>
          <span className="link">
            <Link to={`${listRoute}/${nextOffset}`}>
              {t(config.messages?.loadMore || "PT_LOAD_MORE_MSG")}
            </Link>
          </span>
        </p>
      ) : null}
    </>
  );
};

const TLMyApplications = ({ view }) => {
  if (view === "bills") return <TLBills />;
  return <TLMyApplicationsList />;
};

export default TLMyApplications;
