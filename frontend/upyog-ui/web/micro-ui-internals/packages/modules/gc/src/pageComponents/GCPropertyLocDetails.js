import React, { useState, useEffect } from "react";
import {
  FormStep,
  TextInput,
  CardLabel,
  CardSectionHeader,
  SearchIcon,
  Toast,
  Loader,
  SubmitBar,
  StatusTable,
  Row,
} from "@nudmcdgnpm/digit-ui-react-components";

const PROPERTY_ID_PATTERN = /^[A-Z]{2}-PT-\d{4}-\d{6}$/;

/**
 * GCPropertyLocDetails Component
 * 
 * Step 1 in the GC Application Wizard:
 * - Enter & Search Property ID (e.g. PG-PT-1013-001138)
 * - Fetch & set ACTIVE property from Property Tax
 * - Show verified Owner & Address details
 * - Auto-fills Step 2 (Applicant Details) and Step 3 (Address Details)
 * - For Inactive/Not Found property:
 *   - Citizen: Warning banner + Apply PT button
 *   - Employee: Only warning banner (no button)
 */
const GCPropertyLocDetails = ({ t, config, onSelect, formData, renewApplication }) => {
  const user = Digit.UserService.getUser().info;
  const initialData = formData?.[config?.key] || formData?.gcpropertylocdetails || {};

  const [propertyId, setPropertyId] = useState(initialData.propertyId || renewApplication?.propertyId || "");
  const [property, setProperty] = useState(initialData.propertyDetails || null);
  const [isSearching, setIsSearching] = useState(false);
  const [searchError, setSearchError] = useState(null);
  const [showToast, setShowToast] = useState(null);

  const handlePropertyIdChange = (e) => {
    setPropertyId(e.target.value.toUpperCase());
    setSearchError(null);
  };

  const handleApplyPT = () => {
    window.location.href = "/upyog-ui/citizen/pt/property/new-application";
  };

  const handleSearchClick = async () => {
    const formattedPropertyId = propertyId.trim().toUpperCase();

    if (!formattedPropertyId) {
      setShowToast({ error: true, label: t("GC_ENTER_PROPERTY_ID_PROMPT") });
      return;
    }

    if (!PROPERTY_ID_PATTERN.test(formattedPropertyId)) {
      setShowToast({ error: true, label: t("GC_PROPERTY_ID_ERROR_MESSAGE") });
      return;
    }

    setPropertyId(formattedPropertyId);
    setIsSearching(true);
    setSearchError(null);

    try {
      const tenantId = Digit.ULBService.getCitizenCurrentTenant(true) || Digit.ULBService.getCurrentTenantId();

      const ptResponse = await Digit.PTService.search({
        tenantId,
        filters: { propertyIds: formattedPropertyId },
      });

      const prop = ptResponse?.Properties?.[0];

      if (!prop) {
        setSearchError("NOT_FOUND");
        setProperty(null);
        setShowToast({ error: true, label: t("GC_PROPERTY_NOT_FOUND_TITLE") });
        return;
      }

      if (prop.status && prop.status !== "ACTIVE") {
        setSearchError("INACTIVE");
        setProperty(null);
        setShowToast({ warning: true, label: t("GC_PROPERTY_INACTIVE_TITLE") });
        return;
      }

      setProperty(prop);
      setShowToast({ warning: false, label: t("GC_PROPERTY_VERIFIED_SUCCESS") });
    } catch (err) {
      console.error("Property fetch error:", err);
      setSearchError("NOT_FOUND");
      setProperty(null);
      setShowToast({ error: true, label: t("GC_PROPERTY_SEARCH_FAILED") });
    } finally {
      setIsSearching(false);
    }
  };

  useEffect(() => {
    if (showToast) {
      const timer = setTimeout(() => setShowToast(null), 3000);
      return () => clearTimeout(timer);
    }
  }, [showToast]);

  const owner = property?.owners?.[0];
  const address = property?.address || {};
  const localityName = address.locality?.name || "";
  const addressLine1 = address.doorNo && address.street ? `${address.doorNo}, ${address.street}` : (address.doorNo || address.street || "");

  const goNext = () => {
    const payload = {
      propertyId: property?.propertyId || propertyId,
      propertyDetails: property,
      ownerInfo: {
        ...owner,
        name: owner?.name || "",
        mobileNumber: owner?.mobileNumber || "",
        alternateNumber: owner?.altContactNumber || "",
        emailId: owner?.emailId || "",
        gender: owner?.gender || "",
        dateOfBirth: owner?.dob ? new Date(owner.dob).toISOString().split("T")[0] : "",
        guardianName: owner?.fatherOrHusbandName || "",
        fatherOrHusbandName: owner?.fatherOrHusbandName || "",
        relationship: owner?.relationship || "",
      },
      detectedFloorCount: property?.noOfFloors || 1,
      houseNo: address.doorNo || "",
      doorNo: address.doorNo || "",
      streetName: address.street || "",
      street: address.street || "",
      houseName: address.buildingName || "",
      buildingName: address.buildingName || "",
      landmark: address.landmark || "",
      city: address.city || property?.tenantId || "",
      locality: {
        code: address.locality?.code || "",
        name: localityName,
        label: localityName,
        i18nKey: address.locality?.code || "",
      },
      pincode: address.pincode || "",
      addressline1: addressLine1,
      addressline2: localityName,
      addressType: "PERMANENT",
    };

    onSelect(config.key, payload, false);
  };

  const onSkip = () => onSelect();

  const isFormValid = propertyId && !searchError && property && owner;

  return (
    <React.Fragment>
      <FormStep
        config={config}
        onSelect={goNext}
        onSkip={onSkip}
        t={t}
        isDisabled={!isFormValid}
      >
        <div>
          {/* Step 1 Property Search */}
          <CardLabel>{`${t("GC_PROPERTY_NO")}`} <span className="check-page-link-button">*</span></CardLabel>
          <div className={`gc-property-search-wrapper ${user?.type === "EMPLOYEE" ? "employee" : ""}`}>
            <TextInput
              t={t}
              type={"text"}
              isMandatory={true}
              name="propertyId"
              value={propertyId}
              placeholder="e.g. PG-PT-1013-001154"
              onChange={handlePropertyIdChange}
            />
            <div
              onClick={handleSearchClick}
              className="gc-property-search-btn"
              title={t("ES_COMMON_SEARCH")}
            >
              {isSearching ? <Loader inline /> : <SearchIcon />}
            </div>
          </div>

          {/* Property Not Found or Inactive Alert Banner */}
          {searchError && (
            <div className={`gc-pt-warning-banner ${user?.type === "EMPLOYEE" ? "employee" : ""}`}>
              <div className="gc-pt-warning-title">
                <span className="gc-warning-icon">⚠️</span>
                <span>
                  {searchError === "INACTIVE"
                    ? t("GC_PROPERTY_INACTIVE_BANNER")
                    : t("GC_PROPERTY_NOT_FOUND_BANNER")}
                </span>
              </div>
              <p className="gc-pt-warning-desc">
                {t("GC_APPLY_PT_GUIDANCE")}
              </p>
              {user?.type !== "EMPLOYEE" && (
                <div className="gc-pt-apply-btn-wrapper">
                  <SubmitBar
                    label={t("GC_APPLY_FOR_PROPERTY_IN_PT")}
                    onSubmit={handleApplyPT}
                  />
                </div>
              )}
            </div>
          )}

          {/* Verified Property, Owner & Address Details Card */}
          {property && owner && (
            <div className={`gc-owner-details-card ${user?.type === "EMPLOYEE" ? "employee" : ""}`}>
              <div className="gc-verified-header-row">
                <div className="gc-verified-title-group">
                  <span className="gc-verified-tick">✓</span>
                  <span className="gc-verified-title-text">
                    {t("GC_PT_VERIFIED_PROPERTY_DETAILS")}
                  </span>
                </div>
                <span className="gc-verified-badge">
                  {t("GC_DETECTED_FLOORS")}: {property.noOfFloors || 1}
                </span>
              </div>

              {/* Owner Details Section */}
              <div className="gc-owner-section">
                <CardSectionHeader className="gc-section-header">
                  {t("ES_TITILE_OWNER_DETAILS")}
                </CardSectionHeader>
                <StatusTable>
                  <Row label={t("GC_OWNER_NAME")} text={owner?.name || t("CS_NA")} />
                  <Row label={t("GC_MOBILE_NUMBER")} text={owner?.mobileNumber || t("CS_NA")} />
                  <Row label={t("GC_ALT_MOBILE_NUMBER")} text={owner?.altContactNumber || t("CS_NA")} />
                  <Row label={t("GC_EMAIL")} text={owner?.emailId || t("CS_NA")} />
                  <Row label={t("GC_GENDER")} text={owner?.gender ? t(`COMMON_GENDER_${owner.gender}`) : t("CS_NA")} />
                  <Row label={t("COMMON_BIRTH_DATE")} text={owner?.dob ? new Date(owner.dob).toISOString().split("T")[0] : t("CS_NA")} />
                  <Row label={t("GC_GUARDIAN_NAME")} text={owner?.fatherOrHusbandName || t("CS_NA")} />
                  <Row label={t("GC_RELATIONSHIP")} text={owner?.relationship ? t(`COMMON_${owner.relationship}`) : t("CS_NA")} />
                </StatusTable>
              </div>

              {/* Address Details Section */}
              <div>
                <CardSectionHeader className="gc-section-header">
                  {t("ES_ADDRESS_DETAILS")}
                </CardSectionHeader>
                <StatusTable>
                  <Row label={t("GC_PROPERTY_ID")} text={property?.propertyId || t("CS_NA")} />
                  <Row label={t("COMMON_ADDRESS_TYPE")} text={t("COMMON_ADDRESS_TYPE_PERMANENT")} />
                  <Row label={t("GC_HOUSE_NO")} text={address?.doorNo || t("CS_NA")} />
                  <Row label={t("GC_HOUSE_NAME")} text={address?.buildingName || t("CS_NA")} />
                  <Row label={t("GC_STREET_NAME")} text={address?.street || t("CS_NA")} />
                  <Row label={t("GC_ADDRESS_LINE1")} text={addressLine1 || t("CS_NA")} />
                  <Row label={t("GC_ADDRESS_LINE2")} text={localityName || t("CS_NA")} />
                  <Row label={t("GC_LANDMARK")} text={address?.landmark || t("CS_NA")} />
                  <Row label={t("GC_CITY")} text={address?.city || t("CS_NA")} />
                  <Row label={t("GC_LOCALITY")} text={localityName || t("CS_NA")} />
                  <Row label={t("GC_ADDRESS_PINCODE")} text={address?.pincode || t("CS_NA")} />
                </StatusTable>
              </div>

              <div className="gc-address-runtime-note">
                <span>{t("GC_ADDRESS_RUNTIME_NOTE")}</span>
              </div>
            </div>
          )}
        </div>
      </FormStep>

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

export default GCPropertyLocDetails;
