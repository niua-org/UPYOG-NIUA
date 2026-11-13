import React, { useState, useEffect } from "react";
import { Header, Loader, TextInput, Dropdown, SubmitBar, CardLabel, Card, RadioButtons } from "@nudmcdgnpm/digit-ui-react-components";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import GCApplication from "./gc-application";
import { getGCStatusOptions } from "../../../utils";

/**
 * GCMyApplications Component
 * 
 * Renders the "My Applications" page for citizens in the Garbage Collection service.
 * Structured to match the Property Tax Assessment "My Properties" layout using existing
 * premade modern CSS classes (pt-citizen-my-properties-container, pt-citizen-properties-grid, etc.):
 * - Header with application count badge and "+ New Application" quick action button
 * - Filter & search card with owner/tenant persona switcher, application number, garbage number, and status
 * - Modern responsive card grid (pt-citizen-properties-grid)
 * - Empty state card with icon & registration call-to-action
 * - Modern footer pagination card (pt-citizen-properties-footer-card)
 */
export const GCMyApplications = () => {
  const { t } = useTranslation();
  const tenantId = Digit.ULBService.getCitizenCurrentTenant(true) || Digit.ULBService.getCurrentTenantId();
  const user = Digit.UserService.getUser()?.info || {};

  const ownerTypeOptions = [
    { name: t("GC_OWNER") || "Owner", code: "OWNER" },
    { name: t("GC_TENANT") || "Tenant", code: "TENANT" },
  ];

  const [selectedOwnerType, setSelectedOwnerType] = useState("OWNER");
  const [searchTerm, setSearchTerm] = useState("");
  const [garbageSearchTerm, setGarbageSearchTerm] = useState("");
  const [status, setStatus] = useState(null);

  let filter = window.location.href.split("/").pop();
  let t1;
  let off;
  if (!isNaN(parseInt(filter))) {
    off = filter;
    t1 = parseInt(filter) + 50;
  } else {
    t1 = 4;
  }

  const getFilterPayload = (ownerType = selectedOwnerType, appNo = searchTerm, grbgNo = garbageSearchTerm, statusObj = status) => {
    const trimmedApp = appNo.trim();
    const trimmedGrbg = grbgNo.trim();
    return {
      mobileNumber: user?.mobileNumber,
      ownerType: ownerType,
      limit: !isNaN(parseInt(filter)) ? "50" : "50",
      offset: !isNaN(parseInt(filter)) ? off : "0",
      ...(trimmedApp && { applicationNumber: trimmedApp }),
      ...(trimmedGrbg && { garbageNumber: trimmedGrbg }),
      ...(statusObj?.code && { status: statusObj.code }),
    };
  };

  const [filters, setFilters] = useState(getFilterPayload("OWNER", "", "", null));

  useEffect(() => {
    setFilters(getFilterPayload(selectedOwnerType, searchTerm, garbageSearchTerm, status));
  }, [filter]);

  const { isLoading, data } = Digit.Hooks.gc.useGCSearch({
    tenantId,
    filters: filters,
  });

  const handleOwnerTypeChange = (option) => {
    const newOwnerType = option?.code || "OWNER";
    setSelectedOwnerType(newOwnerType);
    setFilters(getFilterPayload(newOwnerType, searchTerm, garbageSearchTerm, status));
  };

  const handleSearch = () => {
    setFilters(getFilterPayload(selectedOwnerType, searchTerm, garbageSearchTerm, status));
  };

  const clearAll = () => {
    setSearchTerm("");
    setGarbageSearchTerm("");
    setStatus(null);
    setSelectedOwnerType("OWNER");
    setFilters(getFilterPayload("OWNER", "", "", null));
  };

  if (isLoading) {
    return <Loader />;
  }

  const statusOptions = getGCStatusOptions(t);
  const filteredApplications = data?.garbageApplications || [];
  const totalCount = data?.applicationCount || data?.count || 0;

  return (
    <div className="pt-citizen-my-properties-container">
      {/* Header section with Title, Count Badge and New Application button */}
      <div className="pt-citizen-my-properties-header-wrap">
        <div className="pt-citizen-my-properties-title-block">
          <Header className="pt-citizen-my-properties-title">
            {t("GC_MY_APPLICATIONS_HEADER")}
          </Header>
          {filteredApplications && (
            <span className="pt-citizen-my-properties-count-badge">
              {filteredApplications.length}
            </span>
          )}
        </div>
        <Link
          to="/upyog-ui/citizen/gc/new-registration"
          className="pt-citizen-register-prop-btn"
        >
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          <span>{t("GC_NEW_APPLICATION") || "New Application"}</span>
        </Link>
      </div>

      {/* Filter / Search Card */}
      <Card style={{ marginBottom: "24px", padding: "20px" }}>
        {/* Top Owner / Tenant Persona Selector */}
        <div style={{ display: "flex", alignItems: "center", gap: "20px", marginBottom: "20px", paddingBottom: "14px", borderBottom: "1px solid #d6d5d4", flexWrap: "wrap" }}>
          <span style={{ fontWeight: "700", fontSize: "16px", color: "#0b0c0c" }}>
            {t("GC_USER_TYPE") || "Property Type"}:
          </span>
          <RadioButtons
            onSelect={handleOwnerTypeChange}
            selectedOption={ownerTypeOptions.find((opt) => opt.code === selectedOwnerType)}
            optionsKey="name"
            options={ownerTypeOptions}
            innerStyles={{ display: "flex", gap: "24px", marginBottom: "0px" }}
            style={{ display: "flex", gap: "24px", marginBottom: "0px" }}
          />
        </div>

        {/* Search by Application Number, Garbage Number, and Status */}
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))", gap: "16px", alignItems: "flex-end" }}>
          <div>
            <CardLabel style={{ marginBottom: "8px" }}>{t("GC_APPLICATION_NUMBER_LABEL")}</CardLabel>
            <TextInput
              placeholder={t("GC_SEARCH_APP_NO_PLACEHOLDER") || "Search by Application Number"}
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              style={{ width: "100%" }}
            />
          </div>
          <div>
            <CardLabel style={{ marginBottom: "8px" }}>{t("GC_GARBAGE_NUMBER_LABEL") || "Garbage Number"}</CardLabel>
            <TextInput
              placeholder={t("GC_SEARCH_GARBAGE_NO_PLACEHOLDER") || "Search by Garbage Number"}
              value={garbageSearchTerm}
              onChange={(e) => setGarbageSearchTerm(e.target.value)}
              style={{ width: "100%" }}
            />
          </div>
          <div>
            <CardLabel style={{ marginBottom: "8px" }}>{t("PT_COMMON_TABLE_COL_STATUS_LABEL") || "Status"}</CardLabel>
            <Dropdown
              className="form-field"
              selected={status}
              select={setStatus}
              option={statusOptions}
              placeholder={t("CS_COMMON_SELECT_STATUS") || "Select Status"}
              optionKey="value"
              t={t}
            />
          </div>
          <div style={{ display: "flex", alignItems: "center", gap: "16px", paddingBottom: "6px" }}>
            <SubmitBar label={t("ES_COMMON_SEARCH")} onSubmit={handleSearch} />
            <p
              className="link"
              style={{ margin: "0", cursor: "pointer", whiteSpace: "nowrap" }}
              onClick={clearAll}
            >
              {t(`ES_COMMON_CLEAR_ALL`)}
            </p>
          </div>
        </div>
      </Card>

      {/* Applications Grid: Cards rendered in the same responsive grid as Property Tax */}
      {filteredApplications.length > 0 ? (
        <div className="pt-citizen-properties-grid">
          {filteredApplications.map((application, index) => (
            <GCApplication 
              key={application.applicationNo || index} 
              application={application} 
              tenantId={tenantId} 
            />
          ))}
        </div>
      ) : (
        !isLoading && (
          <div className="pt-citizen-empty-properties-card">
            <div className="pt-citizen-empty-icon-wrap">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#94a3b8" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="3 6 5 6 21 6" />
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                <line x1="10" y1="11" x2="10" y2="17" />
                <line x1="14" y1="11" x2="14" y2="17" />
              </svg>
            </div>
            <h3 className="pt-citizen-empty-title">{t("GC_NO_APPLICATION_FOUND_MSG") || "No applications found"}</h3>
            <p className="pt-citizen-empty-subtitle">{t("PT_TEXT_NOT_ABLE_TO_FIND_THE_APPLICATION")}</p>
            <Link
              to="/upyog-ui/citizen/gc/new-registration"
              className="pt-citizen-register-prop-btn"
            >
              {t("GC_NEW_APPLICATION") || "New Application"}
            </Link>
          </div>
        )
      )}

      {/* Modern Footer / Pagination Card */}
      {filteredApplications.length !== 0 && totalCount >= t1 && (
        <div className="pt-citizen-properties-footer-card">
          <div className="pt-citizen-load-more-wrap">
            <Link
              to={`/upyog-ui/citizen/gc/my-applications/${t1}`}
              className="pt-citizen-load-more-btn"
            >
              {t("CS_LOAD_MORE")}
            </Link>
          </div>
          <div className="pt-citizen-help-text">
            <span>{t("PT_TEXT_NOT_ABLE_TO_FIND_THE_APPLICATION")}</span>{" "}
            <Link
              to="/upyog-ui/citizen/gc/new-registration"
              className="pt-citizen-help-link"
            >
              {t("GC_NEW_APPLICATION") || "New Application"}
            </Link>
          </div>
        </div>
      )}
    </div>
  );
};

export default GCMyApplications;
