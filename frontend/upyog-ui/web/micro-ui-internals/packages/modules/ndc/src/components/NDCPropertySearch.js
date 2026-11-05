import "../../css/ndc.css";
import React, { useState, useEffect, useRef, useMemo } from "react";
import { Dropdown, RadioButtons, Localities, TextInput, Toast } from "@nudmcdgnpm/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import { updateNDCForm } from "../redux/actions/NDCFormActions";
import { useLocation, Link } from "react-router-dom";
import { Loader } from "../components/Loader";

/*
 * =========================================================
 * ADDRESS HELPER
 * =========================================================
 */
const getAddress = (address, t) => {
  if (!address) return "";
  const parts = [];
  if (address.doorNo) parts.push(address.doorNo);
  if (address.street) parts.push(address.street);
  if (address.landmark) parts.push(address.landmark);
  if (address.locality?.name) {
    parts.push(address.locality.name);
  } else if (address.locality?.code && address.tenantId) {
    parts.push(t(Digit.Utils.pt.getMohallaLocale(address.locality.code, address.tenantId)));
  }
  if (address.city) {
    parts.push(address.city);
  } else if (address.tenantId) {
    parts.push(t(Digit.Utils.pt.getCityLocale(address.tenantId)));
  }
  if (address.pincode) parts.push(address.pincode);
  return parts.join(", ");
};

/*
 * =========================================================
 * GENERIC PROPERTY SEARCH & SELECTION COMPONENT (NDC)
 * =========================================================
 */
export const PropertySearchNSummary = ({ config, onSelect, formData }) => {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const { pathname } = useLocation();

  // Helper for safe translation with fallback
  const tr = (key, fallback) => {
    const res = t(key);
    return res && res !== key ? res : fallback;
  };

  const isEditScreen = pathname.includes("/modify-application/");

  // Tenants & Cities
  const defaultTenantId = Digit.ULBService.getCitizenCurrentTenant(true) || Digit.ULBService.getCurrentTenantId();
  const { data: globalTenants } = Digit.Hooks.useTenants();

  const allCities = useMemo(() => {
    let raw =
      Digit.SessionStorage.get("NDC_TENANTS") ||
      globalTenants ||
      Digit.SessionStorage.get("initData")?.tenants ||
      Digit.SessionStorage.get("PT_TENANTS") ||
      Digit.SessionStorage.get("TL_TENANTS") ||
      [];

    if (!Array.isArray(raw) || raw.length === 0) {
      if (defaultTenantId) {
        const cleanCode = defaultTenantId.toUpperCase().replace(/\./g, "_");
        const i18nKey = `TENANT_TENANTS_${cleanCode}`;
        const trans = t(i18nKey);
        const name = trans && trans !== i18nKey ? trans : defaultTenantId;
        return [{ code: defaultTenantId, i18nKey, name }];
      }
      return [];
    }

    return raw
      .map((item) => {
        const code = item.code || item.city?.code || defaultTenantId;
        const cleanCode = (code || "").toUpperCase().replace(/\./g, "_");
        const i18nKey = item.i18nKey || `TENANT_TENANTS_${cleanCode}`;
        const trans = t(i18nKey);
        const name = item.name || item.city?.name || (trans && trans !== i18nKey ? trans : code);
        return {
          ...item,
          code,
          i18nKey: trans && trans !== i18nKey ? i18nKey : name,
          name,
        };
      })
      .sort((a, b) => (a?.name || a?.code)?.localeCompare?.(b?.name || b?.code));
  }, [globalTenants, defaultTenantId, t]);

  // URL / API data for pre-fill
  const search = useLocation().search;
  const urlPropertyId =
    new URLSearchParams(search).get("propertyId") ||
    (typeof sessionStorage !== "undefined" && sessionStorage.getItem("NDC_LAST_PROPERTY_ID")) ||
    (typeof localStorage !== "undefined" && localStorage.getItem("NDC_LAST_PROPERTY_ID"));
  const apiDataCheck = useSelector((state) => state.ndc.NDCForm?.formData?.responseData);
  const ptFromApi = apiDataCheck?.[0]?.NdcDetails?.find((item) => item.businessService === "PT");

  // Radio Options: "I know my Property ID" vs "I want to search using Door Number"
  const radioOptions = useMemo(
    () => [
      { code: "0", name: tr("PT_KNOW_PTID", "I know my Property ID") },
      { code: "1", name: tr("PT_SEARCH_DOOR_NO", "I want to search using Door Number") },
    ],
    [t]
  );

  const [selectedMode, setSelectedMode] = useState(radioOptions[0]);
  const [selectedCity, setSelectedCity] = useState(null);

  // Search parameters for Mode 0 (Property ID / Mobile / Old ID)
  const [mobileNumber, setMobileNumber] = useState("");
  const [propertyIdSearch, setPropertyIdSearch] = useState(urlPropertyId || "");
  const [oldPropertyId, setOldPropertyId] = useState("");

  // Search parameters for Mode 1 (Door Number / Locality / Name)
  const [selectedLocality, setSelectedLocality] = useState(null);
  const [doorNumber, setDoorNumber] = useState("");
  const [ownerName, setOwnerName] = useState("");

  // Search execution states
  const [isSearching, setIsSearching] = useState(false);
  const [searchResults, setSearchResults] = useState([]);
  const [hasSearched, setHasSearched] = useState(false);

  // Selected property
  const [selectedProperty, setSelectedProperty] = useState(() => {
    if (formData?.cpt?.details && Object.keys(formData?.cpt?.details).length > 0) {
      return formData.cpt.details;
    }
    return null;
  });

  // Property dues & Bill states
  const [propertyDues, setPropertyDues] = useState(() => {
    if (formData?.cpt?.dues && Object.keys(formData?.cpt?.dues).length > 0) {
      return { dues: formData.cpt.dues };
    }
    return { dues: null };
  });

  const [getLoader, setLoader] = useState(false);
  const [getNoDue, setNoDue] = useState(() => {
    if (formData?.cpt?.dues && typeof formData.cpt.dues.totalAmount !== "undefined") {
      return Number(formData.cpt.dues.totalAmount) === 0;
    }
    return false;
  });
  const [getPayDuesButton, setPayDuesButton] = useState(() => {
    if (formData?.cpt?.dues && typeof formData.cpt.dues.totalAmount !== "undefined") {
      return Number(formData.cpt.dues.totalAmount) > 0;
    }
    return false;
  });
  const [showToast, setShowToast] = useState(null);
  const hasFetchedInitialRef = useRef(false);

  // Auto-select city matching current tenant
  useEffect(() => {
    if (allCities && allCities.length > 0 && !selectedCity) {
      const match = allCities.find((c) => c.code === defaultTenantId);
      setSelectedCity(match || allCities[0]);
    }
  }, [allCities, defaultTenantId, selectedCity]);

  // Auto-close toast
  useEffect(() => {
    if (!showToast) return;
    const timer = setTimeout(() => setShowToast(null), 3500);
    return () => clearTimeout(timer);
  }, [showToast]);

  // Sync dues into Redux
  useEffect(() => {
    if (propertyDues?.dues) {
      const updated = {
        ...formData?.[config.key],
        dues: propertyDues.dues,
      };
      onSelect(config.key, updated);
      dispatch(updateNDCForm(config.key, updated));
    }
  }, [propertyDues]);

  // Initial fetch if coming from edit or URL
  useEffect(() => {
    const existing = formData?.cpt?.details;
    if (existing && Object.keys(existing).length > 0) {
      setSelectedProperty(existing);
    } else if (ptFromApi?.consumerCode) {
      fetchInitialProperty(ptFromApi.consumerCode);
    } else if (urlPropertyId && urlPropertyId !== "null" && urlPropertyId !== "undefined" && !hasFetchedInitialRef.current) {
      hasFetchedInitialRef.current = true;
      setPropertyIdSearch(urlPropertyId);
      fetchInitialProperty(urlPropertyId);
    }
  }, [urlPropertyId, selectedCity?.code]);

  const fetchInitialProperty = async (pId) => {
    if (!pId) return;
    setLoader(true);
    try {
      const res = await Digit.PTService.search({
        tenantId: selectedCity?.code || defaultTenantId,
        filters: { propertyIds: pId },
        auth: true,
      });
      if (res?.Properties?.[0]) {
        selectProperty(res.Properties[0]);
      }
      setLoader(false);
    } catch (err) {
      console.error("Error fetching initial property:", err);
      setLoader(false);
    }
  };

  /*
   * =========================================================
   * FETCH BILL / DUES
   * =========================================================
   */
  const fetchBill = async (pId, pTenantId) => {
    if (!pId) return;
    setLoader(true);
    try {
      const result = await Digit.PaymentService.fetchBill(pTenantId || defaultTenantId, {
        businessService: "PT",
        consumerCode: pId,
      });

      if (result?.Bill?.length > 0) {
        const totalAmount = result.Bill[0]?.totalAmount || 0;
        if (totalAmount > 0) {
          setShowToast({
            error: true,
            label: t("NDC_MESSAGE_DUES_FOUND_PLEASE_PAY") || "Dues found, please pay",
          });
          setPayDuesButton(true);
          setNoDue(false);
        } else {
          setShowToast({
            error: false,
            label: t("NDC_NO_BILLS_FOUND_PROPERTY") || "No dues found for property",
          });
          setNoDue(true);
          setPayDuesButton(false);
        }
        setPropertyDues({ dues: result.Bill[0] });
        onSelect("PropertyDetails", {
          ...(formData?.PropertyDetails || {}),
          propertyBillData: {
            isLoading: false,
            billData: result.Bill[0],
          },
        });
      } else {
        setShowToast({
          error: false,
          label: t("NDC_NO_BILLS_FOUND_PROPERTY") || "No dues found for property",
        });
        const zeroBill = { totalAmount: 0 };
        setPropertyDues({ dues: zeroBill });
        setNoDue(true);
        setPayDuesButton(false);
        onSelect("PropertyDetails", {
          ...(formData?.PropertyDetails || {}),
          propertyBillData: {
            isLoading: false,
            billData: zeroBill,
          },
        });
      }
      setLoader(false);
    } catch (err) {
      console.error("Error fetching PT bill:", err);
      setLoader(false);
      setShowToast({
        error: true,
        label: t("NDC_MESSAGE_FETCH_FAILED") || "Failed to fetch bill status",
      });
    }
  };

  /*
   * =========================================================
   * HANDLE SEARCH EXECUTION
   * =========================================================
   */
  const handleSearch = async () => {
    const currentCityCode = selectedCity?.code || defaultTenantId;
    if (!currentCityCode) {
      setShowToast({ error: true, label: t("PT_SELECT_CITY") || "Please select city" });
      return;
    }

    const filters = {};
    if (selectedMode?.code === "0") {
      if (!mobileNumber?.trim() && !propertyIdSearch?.trim() && !oldPropertyId?.trim()) {
        setShowToast({ error: true, label: t("PT_PROVIDE_ONE_MORE_PARAM") || "Provide atleast one search parameter" });
        return;
      }
      if (mobileNumber?.trim()) filters.mobileNumber = mobileNumber.trim();
      if (propertyIdSearch?.trim()) filters.propertyIds = propertyIdSearch.trim();
      if (oldPropertyId?.trim()) filters.oldPropertyId = oldPropertyId.trim();
    } else {
      if (!selectedLocality && !doorNumber?.trim() && !ownerName?.trim()) {
        setShowToast({ error: true, label: t("PT_PROVIDE_ONE_MORE_PARAM") || "Provide atleast one search parameter" });
        return;
      }
      if (selectedLocality?.code) filters.locality = selectedLocality.code;
      if (doorNumber?.trim()) filters.doorNo = doorNumber.trim();
      if (ownerName?.trim()) filters.name = ownerName.trim();
    }

    setIsSearching(true);
    setHasSearched(true);
    setSearchResults([]);

    try {
      const response = await Digit.PTService.search({
        tenantId: currentCityCode,
        filters,
        auth: true,
      });

      const properties = response?.Properties || [];
      setSearchResults(properties);
      if (properties.length === 0) {
        setShowToast({
          error: true,
          label: t("CS_PT_NO_PROPERTIES_FOUND") || "No properties found for the given criteria",
        });
      }
      setIsSearching(false);
    } catch (err) {
      console.error("Error searching properties:", err);
      setIsSearching(false);
      setShowToast({
        error: true,
        label: err?.response?.data?.Errors?.[0]?.message || t("PT_SEARCH_ERROR") || "Failed to search property",
      });
    }
  };

  /*
   * =========================================================
   * SELECT PROPERTY
   * =========================================================
   */
  const selectProperty = (prop) => {
    if (!prop) return;
    setSelectedProperty(prop);
    setSearchResults([]);
    setHasSearched(false);

    const updatedCpt = {
      ...formData?.[config.key],
      id: prop.propertyId,
      details: prop,
    };
    onSelect(config.key, updatedCpt);
    dispatch(updateNDCForm(config.key, updatedCpt));

    const primaryOwner = prop.owners?.find((o) => o.status !== "INACTIVE") || prop.owners?.[0];
    onSelect("PropertyDetails", {
      email: primaryOwner?.emailId || "",
      propertyBillData: {
        isLoading: false,
        billData: {},
      },
      waterConnection: [],
      sewerageConnection: [],
      tradeLicense: [],
      firstName: primaryOwner?.name || "",
      mobileNumber: primaryOwner?.mobileNumber || "",
      address: getAddress(prop.address, t) || "",
    });
  };

  /*
   * =========================================================
   * CHANGE PROPERTY (RESET)
   * =========================================================
   */
  const handleChangeProperty = () => {
    setSelectedProperty(null);
    setNoDue(false);
    setPayDuesButton(false);
    setPropertyDues({ dues: null });
    setSearchResults([]);
    setHasSearched(false);

    const updatedCpt = {
      ...formData?.[config.key],
      id: "",
      details: null,
      dues: null,
    };
    onSelect(config.key, updatedCpt);
    dispatch(updateNDCForm(config.key, updatedCpt));

    onSelect("PropertyDetails", {
      email: "",
      propertyBillData: {
        isLoading: false,
        billData: {},
      },
      waterConnection: [],
      sewerageConnection: [],
      tradeLicense: [],
      firstName: "",
      mobileNumber: "",
      address: "",
    });
  };

  if (isEditScreen) {
    return <React.Fragment />;
  }

  return (
    <React.Fragment>
      <div className="ndc-generic-search-container">
        {/* =====================================================
            VIEW 1: SELECTED PROPERTY CARD
        ====================================================== */}
        {selectedProperty ? (
          <div className="ndc-selected-prop-card">
            <div className="ndc-selected-prop-header">
              <div className="ndc-selected-prop-title">
                <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path
                    d="M10 0C4.48 0 0 4.48 0 10C0 15.52 4.48 20 10 20C15.52 20 20 15.52 20 10C20 4.48 15.52 0 10 0ZM8 15L3 10L4.41 8.59L8 12.17L15.59 4.58L17 6L8 15Z"
                    fill="#00703C"
                  />
                </svg>
                <span>{t("PT_SELECTED_PROPERTY") || "Selected Property"}</span>
              </div>
              <button type="button" className="ndc-change-prop-btn" onClick={handleChangeProperty}>
                {t("PT_CHANGE_PROPERTY") || "Change Property"}
              </button>
            </div>

            <div className="ndc-selected-prop-grid">
              <div>
                <strong>{t("PT_PROPERTY_ID") || "Property ID"}:</strong> {selectedProperty.propertyId}
              </div>
              <div>
                <strong>{t("PT_OWNER_NAME") || "Owner Name"}:</strong>{" "}
                {selectedProperty.owners?.map((o) => o.name).filter(Boolean).join(", ") || t("NA")}
              </div>
              <div className="ndc-selected-prop-full">
                <strong>{t("PT_ADDRESS") || "Address"}:</strong> {getAddress(selectedProperty.address, t)}
              </div>
            </div>

            {/* DUES STATUS BADGE / CHECK STATUS BUTTON */}
            <div className="ndc-dues-status-wrapper" style={{ marginTop: "14px" }}>
              {getLoader && (
                <div style={{ display: "flex", alignItems: "center", gap: "8px" }}>
                  <Loader />
                  <span>{t("CS_INBOX_SEARCH_LOADING") || "Checking property dues..."}</span>
                </div>
              )}

              {!getLoader && !getNoDue && !getPayDuesButton && (
                <button
                  type="button"
                  className="submit-bar ndc-action-button"
                  style={{ width: "auto", minWidth: "160px", padding: "0 16px" }}
                  onClick={() => fetchBill(selectedProperty.propertyId, selectedProperty.tenantId || selectedCity?.code || defaultTenantId)}
                >
                  {t("CHECK_STATUS_PROPERTY") && t("CHECK_STATUS_PROPERTY") !== "CHECK_STATUS_PROPERTY"
                    ? t("CHECK_STATUS_PROPERTY")
                    : "Check Status for Property"}
                </button>
              )}

              {!getLoader && getNoDue && (
                <div className="ndc-badge-no-dues">
                  ✓ {t("NO_DUES_FOUND_FOR_PROPERTY") || "No dues found for property"}
                </div>
              )}

              {!getLoader && getPayDuesButton && (
                <div style={{ display: "flex", alignItems: "center", gap: "10px", flexWrap: "wrap" }}>
                  <div className="ndc-badge-dues-pending">
                    {t("PT_DUES_PENDING") || "Dues Pending"}: Rs. {propertyDues?.dues?.totalAmount}
                  </div>
                  <Link
                    to={`/upyog-ui/citizen/payment/my-bills/PT/${selectedProperty.propertyId}`}
                    target="_blank"
                    rel="noreferrer"
                  >
                    <button type="button" className="submit-bar ndc-action-button">
                      {t("PAY_DUES") || "Pay Dues"}
                    </button>
                  </Link>
                </div>
              )}
            </div>
          </div>
        ) : (
          /* =====================================================
              VIEW 2: GENERIC SEARCH PROPERTY FORM
          ====================================================== */
          <div>
            <div className="ndc-generic-search-title">
              {tr("SEARCH_PROPERTY", "Search Property")}
            </div>

            {/* Radio Mode Selection */}
            <div className="ndc-generic-radio-group">
              <RadioButtons
                selectedOption={selectedMode}
                onSelect={(d) => {
                  setSelectedMode(d);
                  setSearchResults([]);
                  setHasSearched(false);
                }}
                optionsKey="name"
                options={radioOptions}
              />
            </div>

            {/* City Dropdown */}
            <div className="ndc-search-city-container">
              <label>
                {tr("PT_SELECT_CITY", "City")} <span style={{ color: "#a82227" }}>*</span>
              </label>
              <Dropdown
                selected={selectedCity}
                select={(d) => {
                  setSelectedCity(d);
                  setSelectedLocality(null);
                }}
                option={allCities || []}
                optionKey="i18nKey"
                t={t}
              />
            </div>

            {/* Search Parameters Box */}
            <div className="ndc-generic-search-box">
              <div className="ndc-generic-search-box-title">
                {tr("PT_PROVIDE_ONE_MORE_PARAM", "Provide atleast one search parameter")}
              </div>

              {selectedMode?.code === "0" ? (
                /* Mode 0: Mobile / Property ID / Old ID */
                <div>
                  <div className="ndc-search-input-group">
                    <label>{tr("PT_OWNER_MOB_NO_LABEL", "Owner's mobile number")}</label>
                    <TextInput
                      type="mobileNumber"
                      value={mobileNumber}
                      onChange={(e) => setMobileNumber(e.target.value)}
                      placeholder={tr("CORE_COMMON_MOBILE_NUMBER_PLACEHOLDER", "Enter 10 digit mobile number")}
                    />
                  </div>

                  <div className="ndc-search-or-text">(or)</div>

                  <div className="ndc-search-input-group">
                    <label>{tr("PT_PROPERTY_ID", "Property ID")}</label>
                    <TextInput
                      type="text"
                      value={propertyIdSearch}
                      onChange={(e) => setPropertyIdSearch(e.target.value)}
                      placeholder={tr("PT_PROPERTY_ID_PLACEHOLDER", "Enter Property ID")}
                    />
                  </div>

                  <div className="ndc-search-or-text">(or)</div>

                  <div className="ndc-search-input-group">
                    <label>{tr("PT_PROPERTY_ADDRESS_OLDPID", "Old property ID")}</label>
                    <TextInput
                      type="text"
                      value={oldPropertyId}
                      onChange={(e) => setOldPropertyId(e.target.value)}
                      placeholder={tr("PT_OLD_PROPERTY_ID_PLACEHOLDER", "Enter Old Property ID")}
                    />
                  </div>
                </div>
              ) : (
                /* Mode 1: Locality / Door Number / Owner Name */
                <div>
                  <div className="ndc-search-input-group">
                    <label>
                      {tr("PT_SELECT_LOCALITY", "Select Locality")} <span style={{ color: "#a82227" }}>*</span>
                    </label>
                    <Localities
                      selectLocality={(d) => setSelectedLocality(d)}
                      tenantId={selectedCity?.code || defaultTenantId}
                      boundaryType="revenue"
                      keepNull={false}
                      optionCardStyles={{ height: "250px", overflow: "auto", zIndex: "100", maxHeight: "250px" }}
                      selected={selectedLocality}
                      disable={!(selectedCity?.code || defaultTenantId)}
                      disableLoader={true}
                    />
                  </div>

                  <div className="ndc-search-input-group" style={{ marginTop: "14px" }}>
                    <label>{tr("PT_DOOR_NUM_LABEL", "Door Number")}</label>
                    <TextInput
                      type="text"
                      value={doorNumber}
                      onChange={(e) => setDoorNumber(e.target.value)}
                      placeholder={tr("PT_DOOR_NUMBER_PLACEHOLDER", "Enter Door Number")}
                    />
                  </div>

                  <div className="ndc-search-input-group">
                    <label>{tr("PT_COMMON_PAYEE_NAME", "Owner's Name")}</label>
                    <TextInput
                      type="text"
                      value={ownerName}
                      onChange={(e) => setOwnerName(e.target.value)}
                      placeholder={tr("PT_OWNER_NAME_PLACEHOLDER", "Enter Owner Name")}
                    />
                  </div>
                </div>
              )}

              {/* Search Button */}
              <div style={{ marginTop: "16px" }}>
                <button
                  type="button"
                  className="ndc-search-btn"
                  onClick={handleSearch}
                  disabled={isSearching}
                >
                  {isSearching ? tr("CS_INBOX_SEARCH_LOADING", "Searching...") : tr("PT_HOME_SEARCH_RESULTS_BUTTON_SEARCH", "Search")}
                </button>
              </div>
            </div>

            {/* Loader */}
            {isSearching && (
              <div style={{ padding: "16px 0", textAlign: "center" }}>
                <Loader />
              </div>
            )}

            {/* Search Results Display */}
            {!isSearching && searchResults.length > 0 && (
              <div className="ndc-search-results-wrapper">
                <div className="ndc-search-results-header">
                  {t("CS_SEARCH_RESULTS") || "Search Results"} ({searchResults.length})
                </div>

                {searchResults.map((prop, idx) => (
                  <div key={prop.propertyId || idx} className="ndc-prop-result-card">
                    <div style={{ flex: 1 }}>
                      <div className="ndc-prop-info-title">{prop.propertyId}</div>
                      <div className="ndc-prop-info-detail">
                        <strong>{t("PT_OWNER_NAME") || "Owner"}:</strong>{" "}
                        {prop.owners?.map((o) => o.name).filter(Boolean).join(", ") || t("NA")}
                      </div>
                      <div className="ndc-prop-info-detail">
                        <strong>{t("CORE_COMMON_MOBILE_NUMBER") || "Mobile"}:</strong>{" "}
                        {prop.owners?.[0]?.mobileNumber || t("NA")}
                      </div>
                      <div className="ndc-prop-info-detail">
                        <strong>{t("PT_ADDRESS") || "Address"}:</strong> {getAddress(prop.address, t)}
                      </div>
                    </div>

                    <button
                      type="button"
                      className="ndc-prop-select-btn"
                      onClick={() => selectProperty(prop)}
                    >
                      {t("CS_COMMON_SELECT") || "Select"}
                    </button>
                  </div>
                ))}
              </div>
            )}

            {/* Zero Results Feedback */}
            {!isSearching && hasSearched && searchResults.length === 0 && (
              <div style={{ padding: "12px", background: "#fff3cd", border: "1px solid #ffeeba", borderRadius: "4px", color: "#856404", fontSize: "13px", marginTop: "10px" }}>
                {t("CS_PT_NO_PROPERTIES_FOUND") || "No properties found for the given search criteria."}
              </div>
            )}
          </div>
        )}

        {/* Global Toast */}
        {showToast && (
          <Toast
            isDleteBtn={true}
            labelClassName="ndc-label-width-100"
            error={showToast.error}
            warning={showToast.warning}
            label={t(showToast.label)}
            onClose={() => setShowToast(null)}
          />
        )}
      </div>

      {(getLoader) && <Loader page={true} />}
    </React.Fragment>
  );
};

export default PropertySearchNSummary;
