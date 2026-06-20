import { Header, Dropdown, Card, CardLabel, Button } from "@upyog/workbench-ui-react-components";
import React, { useState, useEffect, useCallback, useRef } from "react";
import { Info } from "@nudmcdgnpm/workbench-ui-svg-components";
import { useTranslation } from "react-i18next";
import { Config as Configg } from "../../configs/searchMDMSConfig";
import _ from "lodash";

function sortByKey(arr, key) {
  return arr.slice().sort((a, b) => (a[key] < b[key] ? -1 : a[key] > b[key] ? 1 : 0));
}

function getPaginationRange(currentPage, totalPages) {
  const SIBLINGS = 1;
  const range = [];
  const left  = Math.max(2, currentPage - SIBLINGS);
  const right = Math.min(totalPages - 1, currentPage + SIBLINGS);
  range.push(1);
  if (left > 2)               range.push("...");
  for (let i = left; i <= right; i++) range.push(i);
  if (right < totalPages - 1) range.push("...");
  if (totalPages > 1)         range.push(totalPages);
  return range;
}

const PageBtn = ({ label, onClick, disabled, active }) => (
  <button
    onClick={onClick}
    disabled={disabled}
    style={{
      minWidth: "32px", height: "32px", padding: "0 6px",
      border: "1px solid",
      borderColor: active ? "#e07b39" : disabled ? "#f0f0f0" : "#d6d5d4",
      borderRadius: "4px",
      background: active ? "#e07b39" : disabled ? "#f9f9f9" : "#fff",
      color: active ? "#fff" : disabled ? "#ccc" : "#0B0C0C",
      cursor: disabled ? "not-allowed" : "pointer",
      fontSize: "12px", fontWeight: active ? 600 : 400,
      display: "flex", alignItems: "center", justifyContent: "center",
      transition: "all 0.15s",
    }}
  >
    {label}
  </button>
);

const PAGE_SIZE = 10;

const MDMSManageMaster = () => {
  const Config = _.clone(Configg);
  const { t } = useTranslation();
  const navigate = Digit.Hooks.useCustomNavigate();
  const modalRef = useRef(null);

  let { masterName: modulee, moduleName: master, tenantId } = Digit.Hooks.useQueryParams();
  tenantId = tenantId || Digit.ULBService.getCurrentTenantId();

  const [masterName,    setMasterName]    = useState(null);
  const [moduleName,    setModuleName]    = useState(null);
  const [masterOptions, setMasterOptions] = useState([]);
  const [moduleOptions, setModuleOptions] = useState([]);
  const [schemaData,    setSchemaData]    = useState(null);
  const [showInfo,      setShowInfo]      = useState(false);
  const [showModal,     setShowModal]     = useState(false);
  const [modalLoading,  setModalLoading]  = useState(false);
  const [modalSearch,   setModalSearch]   = useState("");
  const [currentPage,   setCurrentPage]   = useState(1);

  // allModules → complete list fetched ONCE; never changes until modal closes
  const [allModules,    setAllModules]    = useState([]);

  // ── Derived: client-side filter + pagination ──────────────────────────────
  const filteredModules = modalSearch.trim()
    ? allModules.filter((m) =>
        m.toLowerCase().includes(modalSearch.trim().toLowerCase())
      )
    : allModules;

  const totalPages   = Math.max(1, Math.ceil(filteredModules.length / PAGE_SIZE));
  const pagedModules = filteredModules.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE
  );

  const toDropdownObj = (master = "", mod = "") => ({
    name: mod || master,
    code: Digit.Utils.locale.getTransformedLocale(
      mod ? `WBH_MDMS_${master}_${mod}` : `WBH_MDMS_MASTER_${master}`
    ),
    translatedValue: t(
      Digit.Utils.locale.getTransformedLocale(
        mod ? `WBH_MDMS_${master}_${mod}` : `WBH_MDMS_MASTER_${master}`
      )
    ),
  });

  const callSchemaAPI = async (searchText) => {
    try {
      const response = await Digit.CustomService.getResponse({
        url: `/${Digit.Hooks.workbench.getMDMSContextPath()}/schema/v1/_search`,
        useCache: false,
        method: "POST",
        userService: false,
        body: { SchemaDefCriteria: { tenantId, limit: 200, moduleName: searchText } },
      });

      const schemas = response?.SchemaDefinitions || [];
      const obj = { mastersAvailable: [], schemas };

      schemas.forEach((schema) => {
        const [mas, mod] = schema.code.split(".");
        obj[mas] = obj[mas]?.length > 0
          ? [...obj[mas], toDropdownObj(mas, mod)]
          : [toDropdownObj(mas, mod)];
        obj.mastersAvailable.push(mas);
      });

      obj.mastersAvailable = [...new Set(obj.mastersAvailable)].map((mas) => toDropdownObj(mas));
      obj.mastersAvailable = sortByKey(obj.mastersAvailable, "translatedValue");

      setSchemaData(obj);
      setMasterOptions(obj.mastersAvailable);
    } catch (err) {
      console.error("Schema API Error:", err);
    }
  };

  const debouncedSearch = useCallback(
    _.debounce((text) => {
      if (text?.trim().length >= 3) callSchemaAPI(text.trim());
    }, 500),
    [tenantId]
  );

  const handleModuleSearch = (e) => {
    if (e.target.tagName !== "INPUT") return;
    debouncedSearch(e.target.value || "");
  };

  // Close modal on outside click
  useEffect(() => {
    const handleOutsideClick = (e) => {
      if (modalRef.current && !modalRef.current.contains(e.target)) closeModal();
    };
    if (showModal) document.addEventListener("mousedown", handleOutsideClick);
    return () => document.removeEventListener("mousedown", handleOutsideClick);
  }, [showModal]);

  const closeModal = () => {
    setShowModal(false);
    setAllModules([]);
    setModalSearch("");
    setCurrentPage(1);
  };

  // ── Fetch ALL modules once (high limit) → store for client-side search ────
  const fetchAllModules = async () => {
    setModalLoading(true);
    try {
      const response = await Digit.CustomService.getResponse({
        url: `/${Digit.Hooks.workbench.getMDMSContextPath()}/schema/v1/_search`,
        useCache: false,
        method: "POST",
        userService: false,
        body: {
          SchemaDefCriteria: {
            tenantId,
            limit: 500,   // fetch everything in one shot
            offset: 0,
            isGetAllCodes: true,
          },
        },
      });

      const schemas = response?.SchemaDefinitions || [];

      // schema.code can be "Asset.AssetCategory" OR just "Asset" depending on API
      // Extract the part before the dot (module name) and deduplicate
      const uniqueModules = [
        ...new Set(schemas.map((s) => s.code.includes(".") ? s.code.split(".")[0] : s.code))
      ].sort((a, b) => a.localeCompare(b));

      setAllModules(uniqueModules);
    } catch (err) {
      console.error("Error fetching all modules:", err);
    } finally {
      setModalLoading(false);
    }
  };

  const getAllModuleData = () => {
    setShowModal(true);
    setModalSearch("");
    setCurrentPage(1);
    fetchAllModules();
  };

  // ── Search input: instant client-side filter, always reset to page 1 ──────
  const handleModalSearchChange = (e) => {
    setModalSearch(e.target.value);
    setCurrentPage(1);  // reset page on every keystroke
  };

  const clearModalSearch = () => {
    setModalSearch("");
    setCurrentPage(1);
  };

  const handlePageChange = (page) => {
    if (page < 1 || page > totalPages || page === currentPage) return;
    setCurrentPage(page);
  };

  const handleClear = () => {
    setMasterName(null);
    setModuleName(null);
    setModuleOptions([]);
    setMasterOptions([]);
  };

  useEffect(() => {
    if (masterName?.name && schemaData?.[masterName.name]?.length > 0) {
      setModuleOptions(sortByKey(schemaData[masterName.name], "translatedValue"));
    } else {
      setModuleOptions([]);
    }
  }, [masterName, schemaData]);

  useEffect(() => {
    if (masterName?.name && moduleName?.name) {
      navigate(
        `/${window?.contextPath}/employee/workbench/mdms-search-v2?moduleName=${masterName.name}&masterName=${moduleName.name}`
      );
    }
  }, [moduleName]);

  return (
    <React.Fragment>
      <Header className="works-header-search">{t(Config?.label)}</Header>

      <div className="jk-header-btn-wrapper">
        <Card className="manage-master-wrapper" style={{ alignItems: "end", gap: "1rem" }}>

          {/* Module Name + Info Icon */}
          <div style={{ width: "80%" }} onKeyUp={handleModuleSearch}>
            <CardLabel style={{ margin: 0 }}>{t("WBH_MODULE_NAME")}</CardLabel>
            <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
              <div style={{ flex: 1 }}>
                <Dropdown
                  style={{ width: "100%", margin: 0, flexShrink: 0 }}
                  option={masterOptions}
                  className="form-field"
                  optionKey="name"
                  selected={master && modulee ? toDropdownObj(master) : masterName}
                  select={(e) => { setMasterName(e); setModuleName(null); }}
                  t={t}
                  placeholder={t("ASSET_SEARCH_ENTER_MIN_3_CHARS")}
                  disable={!!master}
                />
              </div>

              {/* Info Icon */}
              <div
                style={{ position: "relative", flexShrink: 0, marginTop: "2px" }}
                onMouseEnter={() => setShowInfo(true)}
                onMouseLeave={() => setShowInfo(false)}
              >
                <Info
                  style={{ cursor: "pointer", border: "1px solid #d6d5d4", background: "#fff", width: "32px", height: "32px", borderRadius: "50%" }}
                  onClick={getAllModuleData}
                />
                {showInfo && !showModal && (
                  <div style={{
                    position: "absolute", top: "38px", left: "50%", transform: "translateX(-50%)",
                    whiteSpace: "nowrap", background: "#333", color: "#fff",
                    fontSize: "11px", padding: "4px 10px", borderRadius: "4px",
                    zIndex: 1000, pointerEvents: "none",
                  }}>
                    Browse all modules
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Master Name */}
          <div style={{ width: "100%" }}>
            <CardLabel style={{ margin: 0 }}>{t("WBH_MASTER_NAME")}</CardLabel>
            <Dropdown
              style={{ width: "100%", margin: 0 }}
              option={moduleOptions}
              className="form-field"
              optionKey="code"
              selected={master && modulee ? toDropdownObj(master, modulee) : moduleName}
              select={(e) => setModuleName(e)}
              t={t}
              placeholder={t("WBH_MASTER_NAME")}
              disable={!masterName}
            />
          </div>

          <Button
            style={{ width: "100%", maxWidth: "100px" }}
            label={t("CLEAR")}
            variation="secondary"
            onButtonClick={handleClear}
            type="button"
          />
        </Card>
      </div>

      {/* Module List Modal */}
      {showModal && (
        <div style={{
          position: "fixed", inset: 0, background: "rgba(0,0,0,0.45)",
          zIndex: 1000, display: "flex", alignItems: "center", justifyContent: "center",
        }}>
          <div ref={modalRef} style={{
            background: "#fff", borderRadius: "8px", width: "420px",
            maxWidth: "95vw", maxHeight: "80vh", display: "flex",
            flexDirection: "column", boxShadow: "0 8px 32px rgba(0,0,0,0.18)",
          }}>

            {/* Header */}
            <div style={{
              display: "flex", alignItems: "center", justifyContent: "space-between",
              padding: "16px 20px", borderBottom: "1px solid #e0e0e0", flexShrink: 0,
            }}>
              <div>
                <div style={{ fontWeight: 600, fontSize: "15px", color: "#0B0C0C" }}>
                  {t("Available Modules")}
                </div>
                {!modalLoading && (
                  <div style={{ fontSize: "12px", color: "#6E7882", marginTop: "2px" }}>
                    {modalSearch.trim()
                      ? `${filteredModules.length} result${filteredModules.length !== 1 ? "s" : ""} for "${modalSearch}"`
                      : `Showing ${Math.min((currentPage - 1) * PAGE_SIZE + 1, filteredModules.length)}–${Math.min(currentPage * PAGE_SIZE, filteredModules.length)} of ${filteredModules.length}`
                    }
                  </div>
                )}
              </div>
              <button
                onClick={closeModal}
                style={{ background: "none", border: "none", cursor: "pointer", fontSize: "20px", color: "#6E7882", lineHeight: 1, padding: "4px" }}
                aria-label="Close"
              >
                ✕
              </button>
            </div>

            {/* Search bar */}
            <div style={{ padding: "10px 16px", borderBottom: "1px solid #f0f0f0", flexShrink: 0 }}>
              <div style={{ position: "relative" }}>
                <span style={{
                  position: "absolute", left: "10px", top: "50%", transform: "translateY(-50%)",
                  color: "#aaa", fontSize: "14px", pointerEvents: "none",
                }}>
                  🔍
                </span>
                <input
                  autoFocus
                  type="text"
                  value={modalSearch}
                  onChange={handleModalSearchChange}
                  placeholder="Search module name…"
                  style={{
                    width: "100%", padding: "8px 32px 8px 32px",
                    border: "1px solid #d6d5d4", borderRadius: "4px",
                    fontSize: "13px", outline: "none", color: "#0B0C0C",
                    boxSizing: "border-box", transition: "border-color 0.2s",
                  }}
                  onFocus={(e) => e.target.style.borderColor = "#e07b39"}
                  onBlur={(e)  => e.target.style.borderColor = "#d6d5d4"}
                />
                {modalSearch && (
                  <button
                    onClick={clearModalSearch}
                    style={{
                      position: "absolute", right: "8px", top: "50%", transform: "translateY(-50%)",
                      background: "none", border: "none", cursor: "pointer",
                      color: "#aaa", fontSize: "16px", lineHeight: 1, padding: 0,
                    }}
                  >
                    ✕
                  </button>
                )}
              </div>
            </div>

            {/* List */}
            <div style={{ overflowY: "auto", padding: "8px 12px", flex: 1 }}>
              {modalLoading ? (
                <div style={{ textAlign: "center", padding: "40px 0", color: "#6E7882", fontSize: "14px" }}>
                  Loading modules…
                </div>
              ) : pagedModules.length === 0 ? (
                <div style={{ textAlign: "center", padding: "40px 0", color: "#6E7882", fontSize: "14px" }}>
                  {modalSearch.trim() ? `No results for "${modalSearch}"` : "No modules found"}
                </div>
              ) : pagedModules.map((modName, index) => (
                <div
                  key={modName}
                  style={{
                    display: "flex", alignItems: "center", gap: "10px",
                    padding: "10px 12px", marginBottom: "4px",
                    borderRadius: "4px", border: "1px solid #f0f0f0",
                  }}
                >
                  <span style={{
                    minWidth: "24px", height: "24px", borderRadius: "50%",
                    background: "#FFF3E0", color: "#e07b39", fontSize: "11px",
                    fontWeight: 600, display: "flex", alignItems: "center",
                    justifyContent: "center", flexShrink: 0,
                  }}>
                    {(currentPage - 1) * PAGE_SIZE + index + 1}
                  </span>
                  <span style={{ fontSize: "14px", color: "#0B0C0C", fontWeight: 500 }}>
                    {modName}
                  </span>
                </div>
              ))}
            </div>

            {/* Pagination Footer — only when more than 1 page */}
            {!modalLoading && totalPages > 1 && (
              <div style={{
                padding: "10px 20px", borderTop: "1px solid #e0e0e0", flexShrink: 0,
                display: "flex", alignItems: "center", justifyContent: "space-between", gap: "8px",
              }}>
                <span style={{ fontSize: "12px", color: "#6E7882", whiteSpace: "nowrap" }}>
                  Page {currentPage} of {totalPages}
                </span>

                <div style={{ display: "flex", alignItems: "center", gap: "4px", flexWrap: "wrap" }}>
                  <PageBtn label="«" onClick={() => handlePageChange(1)}                disabled={currentPage === 1}          />
                  <PageBtn label="‹" onClick={() => handlePageChange(currentPage - 1)} disabled={currentPage === 1}          />

                  {getPaginationRange(currentPage, totalPages).map((item, i) =>
                    item === "..." ? (
                      <span key={`dot-${i}`} style={{ fontSize: "13px", color: "#aaa", padding: "0 2px" }}>…</span>
                    ) : (
                      <PageBtn
                        key={item}
                        label={item}
                        onClick={() => handlePageChange(item)}
                        active={currentPage === item}
                      />
                    )
                  )}

                  <PageBtn label="›" onClick={() => handlePageChange(currentPage + 1)} disabled={currentPage === totalPages} />
                  <PageBtn label="»" onClick={() => handlePageChange(totalPages)}      disabled={currentPage === totalPages} />
                </div>
              </div>
            )}

          </div>
        </div>
      )}
    </React.Fragment>
  );
};

export default MDMSManageMaster;