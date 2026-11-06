import React, { useState, useEffect } from "react";
import { Header, Loader, TextInput, Dropdown, SubmitBar, CardLabel, Card, RadioButtons } from "@nudmcdgnpm/digit-ui-react-components";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import GCApplication from "./gc-application";
import { getGCStatusOptions } from "../../../utils";

/**
 * GCMyApplications Component
 * 
 * Renders the "My Applications" page for citizens, displaying a list of GC applications
 * associated with the user's mobile number. Provides persona switching between Owner and Tenant,
 * and search/filter functionality by application number, garbage number, and status.
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
    <React.Fragment>
      <Header>{`${t("GC_MY_APPLICATIONS_HEADER")} (${filteredApplications.length})`}</Header>
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
        {filteredApplications.length === 0 && !isLoading && (
          <p className="gc-index-top-spacing-3">{t("GC_NO_APPLICATION_FOUND_MSG") || "No applications found"}</p>
        )}

        {filteredApplications.length !== 0 && totalCount >= t1 && (
          <div>
            <p className="gc-index-top-spacing-3">
              <span className="link">
                <Link to={`/upyog-ui/citizen/gc/my-applications/${t1}`}>{t("CS_LOAD_MORE")}</Link>
              </span>
            </p>
          </div>
        )}
      </div>
    </React.Fragment>
  );
};

export default GCMyApplications;
