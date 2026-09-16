import React, { useState, useEffect } from "react";
import { Header, Loader, TextInput, Dropdown, SubmitBar, CardLabel, Card } from "@nudmcdgnpm/digit-ui-react-components";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import GCApplication from "./gc-application";
import { getGCStatusOptions } from "../../../utils";

/**
 * GCMyApplications Component
 * 
 * Renders the "My Applications" page for citizens, displaying a list of GC applications
 * associated with the user's mobile number. Provides search/filter functionality by
 * application number and status. Supports load-more pagination.
 * 
 * Uses `useGCSearch` hook to fetch applications and applies local client-side filtering
 * for accurate search results.
 */
export const GCMyApplications = () => {
  const { t } = useTranslation();
  const tenantId = Digit.ULBService.getCitizenCurrentTenant(true) || Digit.ULBService.getCurrentTenantId();
  const user = Digit.UserService.getUser().info;

  const [searchTerm, setSearchTerm] = useState("");
  const [status, setStatus] = useState(null);
  const [filters, setFilters] = useState(null);

  let filter = window.location.href.split("/").pop();
  let t1;
  let off;
  if (!isNaN(parseInt(filter))) {
    off = filter;
    t1 = parseInt(filter) + 50;
  } else {
    t1 = 4;
  }

  let initialFilters = !isNaN(parseInt(filter))
    ? { mobileNumber: user?.mobileNumber, limit: "50", offset: off }
    : { mobileNumber: user?.mobileNumber, limit: "4", offset: "0" };

  useEffect(() => {
    setFilters(initialFilters);
  }, [filter]);

  const { isLoading, data } = Digit.Hooks.gc.useGCSearch({
    tenantId,
    filters: filters || initialFilters,
  });

  const handleSearch = () => {
    const trimmedSearchTerm = searchTerm.trim();
    setFilters({
      mobileNumber: user?.mobileNumber,
      ...(trimmedSearchTerm && { applicationNumber: trimmedSearchTerm }),
      ...(status?.code && { status: status.code }),
    });
  };

  const clearAll = () => {
    setSearchTerm("");
    setStatus(null);
    setFilters(initialFilters);
  };

  if (isLoading) {
    return <Loader />;
  }

  const statusOptions = getGCStatusOptions(t);

  let filteredApplications = data?.garbageAccounts || data?.GarbageApplications || data?.data || [];
  const totalCount = data?.applicationCount || data?.count || 0;

  return (
    <React.Fragment>
      <Header>{`${t("GC_MY_APPLICATIONS_HEADER")} (${filteredApplications.length})`}</Header>
      
      {/* Interactive Search & Filter Card */}
      <Card className="gc-search-card">
        <div className="gc-filter-row">
          {/* Application Number Filter */}
          <div className="gc-filter-col">
            <CardLabel>{t("GC_APPLICATION_NUMBER_LABEL")}</CardLabel>
            <TextInput
              placeholder={t("GC_SEARCH_APP_NO_PLACEHOLDER")}
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>

          {/* Status Filter */}
          <div className="gc-filter-col">
            <CardLabel>{t("PT_COMMON_TABLE_COL_STATUS_LABEL")}</CardLabel>
            <Dropdown
              className="form-field"
              selected={status}
              select={setStatus}
              option={statusOptions}
              placeholder={t("CS_COMMON_SELECT_STATUS")}
              optionKey="value"
              t={t}
            />
          </div>

          {/* Action Buttons: Search & Clear All */}
          <div className="gc-filter-actions">
            <button
              type="button"
              onClick={handleSearch}
              className="gc-search-btn"
            >
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                />
              </svg>
              <span>{t("ES_COMMON_SEARCH")}</span>
            </button>
            <button
              type="button"
              onClick={clearAll}
              className="gc-clear-btn"
            >
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
              <span>{t("ES_COMMON_CLEAR_ALL")}</span>
            </button>
          </div>
        </div>
      </Card>

      {/* Applications List */}
      <div>
        {filteredApplications.length > 0 &&
          filteredApplications.map((application, index) => (
            <div key={application.applicationNo || index}>
              <GCApplication 
                application={application} 
                tenantId={tenantId} 
              />
            </div>
          ))}

        {/* Empty State */}
        {filteredApplications.length === 0 && !isLoading && (
          <div className="gc-empty-state">
            <svg className="w-12 h-12 text-gray-300 mb-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1.5}
                d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
              />
            </svg>
            <div className="text-base font-semibold text-gray-700">{t("GC_NO_APPLICATION_FOUND_MSG")}</div>
            <div className="text-xs text-gray-500 mt-1">
              Try adjusting your search criteria or clear filters to view all applications.
            </div>
          </div>
        )}

        {/* Load More Button */}
        {filteredApplications.length !== 0 && totalCount >= t1 && (
          <div className="flex justify-center my-6">
            <Link to={`/upyog-ui/citizen/gc/my-applications/${t1}`} className="gc-load-more-btn">
              <span>{t("CS_LOAD_MORE")}</span>
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
              </svg>
            </Link>
          </div>
        )}
      </div>
    </React.Fragment>
  );
};

export default GCMyApplications;

