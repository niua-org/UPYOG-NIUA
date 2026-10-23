/**
 * Citizen renewal list — same layout as My Applications.
 * Header, filters, card rows, and the Renew button come from
 * config/CitizenRenewalList.json. MDMS TradeLicense.CitizenRenewalListConfig
 * replaces that file when the master is present.
 * The search asks for licenses pending renewal. JSON filters run on the client.
 * A trade search can still open this page with license or mobile query params.
 */
import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  Header,
  Loader,
  TextInput,
  Dropdown,
  SubmitBar,
  CardLabel,
  Card,
  sortByOrder,
} from "@nudmcdgnpm/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import RenewalCard from "./renewal-card";
import localRenewalList from "../../../config/CitizenRenewalList.json";
import {
  applyMyApplicationFilters,
  resolveCitizenMyApplicationsConfig,
  sortMyApplications,
} from "../../../utils/tlMyApplicationsConfig";
import styles from "../../../styles/TLMyApplications.module.scss";

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

export const TLList = () => {
  const { t } = useTranslation();
  const userInfo = Digit.UserService.getUser();
  const {
    mobileNumber: queryMobile,
    LicenseNumber: queryLicense,
    tenantId: queryTenantId,
  } = Digit.Hooks.useQueryParams();
  const homeCity = Digit.SessionStorage.get("CITIZEN.COMMON.HOME.CITY")?.code;
  const tenantId = homeCity || userInfo?.info?.permanentCity || queryTenantId;
  const stateId = Digit.ULBService.getStateId();

  const { data: mdmsRenewal } = Digit.Hooks.useEnabledMDMS(
    stateId,
    "TradeLicense",
    [{ name: "CitizenRenewalListConfig" }],
    {
      select: (mdms) => mdms?.TradeLicense?.CitizenRenewalListConfig || null,
    }
  );
  const { data: fydata = {} } = Digit.Hooks.tl.useTradeLicenseMDMS(
    stateId,
    "egf-master",
    "FinancialYear"
  );

  const config = useMemo(
    () => resolveCitizenMyApplicationsConfig(localRenewalList, mdmsRenewal),
    [mdmsRenewal]
  );
  const filters = useMemo(() => sortByOrder(config.filters || []), [config.filters]);
  const resultFields = useMemo(
    () => sortByOrder(config.resultFields || []),
    [config.resultFields]
  );
  const pageSize = config.paginationDefaults?.limit || 50;
  const financialYears = useMemo(
    () => fydata?.["egf-master"]?.FinancialYear?.filter((year) => year.module === "TL") || [],
    [fydata]
  );

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
  // Paging stays on this screen. The address remains /tradelicence/renewal-list.
  const [pageOffset, setPageOffset] = useState(0);

  // Trade search lands here with a license or mobile. Otherwise use the logged-in mobile.
  const apiFilters = useMemo(() => {
    const defaults = config.searchDefaults || {};
    const next = {
      tenantId: tenantId || queryTenantId,
      offset: defaults.offset ?? 0,
      limit: defaults.limit ?? 1000,
      RenewalPending: defaults.RenewalPending !== false,
      status: defaults.status || "APPROVED,CANCELLED,EXPIRED,MANUALEXPIRED",
    };
    if (queryLicense) {
      next.licenseNumbers = queryLicense;
      if (queryTenantId) next.tenantId = queryTenantId;
    } else {
      next.mobileNumber = queryMobile || userInfo?.info?.mobileNumber;
    }
    return next;
  }, [tenantId, queryTenantId, queryLicense, queryMobile, userInfo?.info?.mobileNumber, config.searchDefaults]);

  const { isLoading, error, data } = Digit.Hooks.tl.useTradeLicenseSearch(
    { filters: apiFilters },
    { enabled: Boolean(apiFilters.tenantId), structuralSharing: false }
  );

  useEffect(() => {
    sessionStorage.setItem("isDirectRenewal", "true");
    localStorage.setItem("TLAppSubmitEnabled", "true");
  }, []);

  const applications = useMemo(() => {
    const list = data?.Licenses || data?.licenses || [];
    return sortMyApplications(
      applyMyApplicationFilters(list, filters, appliedFilters),
      config.paginationDefaults
    );
  }, [data, filters, appliedFilters, config.paginationDefaults]);

  const visibleApplications = useMemo(
    () => applications.slice(pageOffset, pageOffset + pageSize),
    [applications, pageOffset, pageSize]
  );

  const resetPage = useCallback(() => {
    setPageOffset(0);
  }, []);

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
  const nextOffset = pageOffset + pageSize;
  const hasMore = nextOffset < totalCount;

  return (
    <>
      <Header>{`${t(config.header)} (${totalCount})`}</Header>
      {config.description ? (
        <p className={styles["tl-myapps__description"]}>{t(config.description)}</p>
      ) : null}
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
          <div key={`${license.licenseNumber || license.applicationNumber || "row"}-${index}`}>
            <RenewalCard
              application={license}
              resultFields={resultFields}
              cardActions={config.cardActions || []}
              emptyValue={config.emptyValue}
              messages={config.messages || {}}
              financialYears={financialYears}
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
          <span
            className="link"
            onClick={() => setPageOffset(nextOffset)}
            onKeyDown={(event) => {
              if (event.key !== "Enter" && event.key !== " ") return;
              event.preventDefault();
              setPageOffset(nextOffset);
            }}
            role="button"
            tabIndex={0}
          >
            {t(config.messages?.loadMore || "PT_LOAD_MORE_MSG")}
          </span>
        </p>
      ) : null}
    </>
  );
};
