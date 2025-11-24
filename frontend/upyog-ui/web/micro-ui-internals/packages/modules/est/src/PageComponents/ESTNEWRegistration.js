import React, { useState, useEffect, Fragment} from "react";
import {
  Header,
  Card,
  CardLabel,
  TextInput,
  Dropdown,
  SubmitBar,
  Toast,
} from "@upyog/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import { useHistory } from "react-router-dom";
import { Config } from "../config/Create/config";

const FIELD_TYPE = {
  TEXT: "TEXT",
  NUMBER: "NUMBER",
  ALPHANUMERIC: "ALPHANUMERIC",
  SPECIALCHARACTERS: "SPEC",
};

const NewRegistration = ({ parentRoute, t: propT, onSelect, formData = {}, config }) => {
  console.log("NewRegistration received props:", { onSelect, config, formData });
  const { t } = useTranslation();
  const history = useHistory();

  const tenantId = Digit.ULBService.getCurrentTenantId();
  const [_formData, setFormData, _clear] = Digit.Hooks.useSessionStorage("EST_CREATE_DATA", null);
  const [mutationHappened, setMutationHappened, clear] = Digit.Hooks.useSessionStorage("EST_MUTATION_HAPPENED", false);
  const [successData, setSuccessData, clearSuccessData] = Digit.Hooks.useSessionStorage("EST_MUTATION_SUCCESS_DATA", {});

  // 🔹 All Cities (hook may return array or { data: [] })
  const allCities = Digit.Hooks.estate.useTenants();
  const cityList = allCities?.data || allCities || [];

const [selectedCity, setSelectedCity] = useState(null);
const [errors, setErrors] = useState({});
const [showToast, setShowToast] = useState(false);
const mutation = Digit.Hooks.estate.useESTCreateAPI(tenantId);



  const { data: Asset_Type } = Digit.Hooks.useEnabledMDMS(
    Digit.ULBService.getStateId(),
    "ASSET",
    [{ name: "assetParentCategory" }],
    {
      select: (data) => {
        const formattedData = data?.["ASSET"]?.["assetParentCategory"];
        const activeData = formattedData?.filter((item) => item.active === true);
        return activeData?.map((item) => ({
          i18nKey: `ASSET_TYPE_${item.code}`,
          code: item.code,
          label: item.name,
        }));
      },
    }
  );

  // 🔹 Auto-select city based on tenantId
  
  useEffect(() => {
    if (Array.isArray(cityList) && tenantId) {
      const matchedCity = cityList.find((city) => city.code === tenantId);
      if (matchedCity) {
        setSelectedCity(matchedCity);
        setAssetFormData((prev) => {
          // Only update if different to avoid unnecessary clears
          if (prev.city !== matchedCity.code) {
            return {
              ...prev,
              city: matchedCity.code,
              serviceType: "", // clear locality because city auto-changed
            };
          }
          return prev;
        });
      }
    }
  }, [cityList, tenantId, selectedCity?.code]);

  useEffect(() => {
    setMutationHappened(false);
    clearSuccessData();
  }, []);

  const { data: fetchedLocalities } = Digit.Hooks.useBoundaryLocalities(
    selectedCity?.code,
    "revenue",
    { enabled: !!selectedCity },
    t
  );

  const structuredLocality = fetchedLocalities?.map((locality) => ({
  ...locality,
  i18nKey: `TENANT_TENANTS_${locality.code.toUpperCase()}`,
})) || [];


  const [assetFormData, setAssetFormData] = useState({
    // estateNo: formData?.Assetdata?.estateNo || "",
    buildingName: formData?.Assetdata?.buildingName || "",
    buildingNo: formData?.Assetdata?.buildingNo || "",
    buildingFloor: formData?.Assetdata?.buildingFloor || "",
    buildingBlock: formData?.Assetdata?.buildingBlock || "",
    city: formData?.Assetdata?.city || "",
    serviceType: formData?.Assetdata?.serviceType || "",
    totalFloorArea: formData?.Assetdata?.totalFloorArea || "",
    dimensionLength: formData?.Assetdata?.dimensionLength || "",
    dimensionWidth: formData?.Assetdata?.dimensionWidth || "",
    rate: formData?.Assetdata?.rate || "",
    assetRef: formData?.Assetdata?.assetRef || "",
    assetType: formData?.Assetdata?.assetType || "",
  });

  const sanitizeInput = (type, value) => {
    switch (type) {
      case FIELD_TYPE.NUMBER:
        return value.replace(/[^0-9]/g, "");
      case FIELD_TYPE.ALPHANUMERIC:
        return value.replace(/[^a-zA-Z0-9 ]/g, "");
      case FIELD_TYPE.SPEC:
        return value.replace(/[^a-zA-Z0-9-\/ ]/g, "");
      default:
        return value;
    }
  };

  const handleChange = (field, value, type = FIELD_TYPE.TEXT) => {
    const sanitizedValue = sanitizeInput(type, value);
    setAssetFormData((prev) => ({ ...prev, [field]: sanitizedValue }));
    setErrors((prev) => ({ ...prev, [field]: "" }));
  };

  const validateForm = () => {
    const newErrors = {};
    // if (!assetFormData.estateNo.trim()) newErrors.estateNo = t("EST_ERROR_ASSET_NO_REQUIRED");
    if (!assetFormData.buildingName.trim()) newErrors.buildingName = t("EST_ERROR_BUILDING_NAME_REQUIRED");
    if (!assetFormData.buildingNo.trim()) newErrors.buildingNo = t("EST_ERROR_BUILDING_NUMBER_REQUIRED");
    if (!assetFormData.buildingFloor.trim()) newErrors.buildingFloor = t("EST_ERROR_BUILDING_FLOOR_REQUIRED");
    if (!assetFormData.buildingBlock.trim()) newErrors.buildingBlock = t("EST_ERROR_BUILDING_BLOCK_REQUIRED");
    if (!assetFormData.city) newErrors.city = t("EST_ERROR_CITY_REQUIRED");
    if (!assetFormData.serviceType) newErrors.serviceType = t("EST_ERROR_LOCALITY_REQUIRED");
    if (!assetFormData.totalFloorArea.trim()) newErrors.totalFloorArea = t("EST_ERROR_TOTAL_PLOT_AREA_REQUIRED");
    if (!assetFormData.dimensionLength.trim()) newErrors.dimensionLength = t("EST_ERROR_LENGTH_REQUIRED");
    if (!assetFormData.dimensionWidth.trim()) newErrors.dimensionWidth = t("EST_ERROR_BREADTH_REQUIRED");
    if (!assetFormData.rate.trim()) newErrors.rate = t("EST_ERROR_RATE_REQUIRED");
    if (!assetFormData.assetRef.trim()) newErrors.assetRef = t("EST_ERROR_ASSET_REF_REQUIRED");
    if (!assetFormData.assetType) newErrors.assetType = t("EST_ERROR_ASSET_TYPE_REQUIRED");
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e) => {
  e.preventDefault();
  
  if (validateForm()) {
    if (onSelect) {
      onSelect(config?.key, { Assetdata: assetFormData }, false);
    }
  }
};



  const RequiredLabel = ({ label }) => (
    <CardLabel>
      {t(label)} <span style={{ color: "red" }}>*</span>
    </CardLabel>
  );

  return (
    <div>
      <Header>{t("EST_COMMON_NEW_REGISTRATION")}</Header>
      <Card style={{ padding: "16px" }}>
        <form onSubmit={handleSubmit}>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(2, 1fr)", gap: "16px" }}>
            
           {/* <div> 
              <RequiredLabel label="EST_ASSETS_NO" />
              <TextInput
                placeholder={t("EST_ENTER_ASSET_NO")}
                value={assetFormData.estateNo}
                onChange={(e) => handleChange("estateNo", e.target.value, FIELD_TYPE.NUMBER)}
              />
              {errors.estateNo && <p style={{ color: "red" }}>{errors.estateNo}</p>}
            </div> */}

            {/* Building Name */}
            <div>
              <RequiredLabel label="EST_BUILDING_NAME" />
              <TextInput
                placeholder={t("EST_ENTER_BUILDING_NAME")}
                value={assetFormData.buildingName}
                onChange={(e) => handleChange("buildingName", e.target.value, FIELD_TYPE.ALPHANUMERIC)}
              />
              {errors.buildingName && <p style={{ color: "red" }}>{errors.buildingName}</p>}
            </div>

            {/* Building Number */}
            <div>
              <RequiredLabel label="EST_BUILDING_NUMBER" />
              <TextInput
                placeholder={t("EST_ENTER_BUILDING_NUMBER")}
                value={assetFormData.buildingNo}
                onChange={(e) => handleChange("buildingNo", e.target.value, FIELD_TYPE.NUMBER)}
              />
              {errors.buildingNo && <p style={{ color: "red" }}>{errors.buildingNo}</p>}
            </div>

            {/* Building Floor */}
            <div>
              <RequiredLabel label="EST_BUILDING_FLOOR" />
              <TextInput
                placeholder={t("EST_ENTER_BUILDING_FLOOR")}
                value={assetFormData.buildingFloor}
                onChange={(e) => handleChange("buildingFloor", e.target.value, FIELD_TYPE.NUMBER)}
              />
              {errors.buildingFloor && <p style={{ color: "red" }}>{errors.buildingFloor}</p>}
            </div>

            {/* Building Block */}
            <div>
              <RequiredLabel label="EST_BUILDING_BLOCK" />
              <TextInput
                placeholder={t("EST_ENTER_BUILDING_BLOCK")}
                value={assetFormData.buildingBlock}
                onChange={(e) => handleChange("buildingBlock", e.target.value, FIELD_TYPE.ALPHANUMERIC)}
              />
              {errors.buildingBlock && <p style={{ color: "red" }}>{errors.buildingBlock}</p>}
            </div>

              {/* 🔹 City (Auto Prefill) */}
              <div>
                <RequiredLabel label="EST_CITY" />
                <Dropdown
                  option={cityList}
                  optionKey="i18nKey"
                  selected={selectedCity}
                  select={(city) => {
                    setSelectedCity(city);
                    setAssetFormData((prev) => ({
                      ...prev,
                      city: city?.code || "",
                      serviceType:
                        prev.city !== city?.code ? "" : prev.serviceType,
                    }));
                  }}
                  placeholder={t("EST_SELECT_CITY")}
                  t={t}
                />
                {errors.city && <p style={{ color: "red" }}>{errors.city}</p>}
              </div>

              {/* 🔹 Locality */}
              <div>
                <RequiredLabel label="EST_LOCALITY" />
                <Dropdown
                  option={structuredLocality}
                  optionKey="i18nKey"
                  selected={
                    structuredLocality?.find(
                      (loc) => loc.code === assetFormData.serviceType
                    ) || null
                  }
                  select={(loc) =>
                    setAssetFormData((prev) => ({
                      ...prev,
                      serviceType: loc.code,
                    }))
                  }
                  placeholder={
                    !selectedCity
                      ? t("EST_SELECT_CITY_FIRST")
                      : structuredLocality?.length
                      ? t("EST_SELECT_LOCALITY")
                      : t("EST_NO_LOCALITIES_FOUND")
                  }
                  t={t}
                />
                {errors.serviceType && (
                  <p style={{ color: "red" }}>{errors.serviceType}</p>
                )}
              </div>

            {/* Total Plot Area */}
            <div>
              <RequiredLabel label="EST_TOTAL_PLOT_AREA" />
              <TextInput
                placeholder={t("EST_ENTER_TOTAL_PLOT_AREA")}
                value={assetFormData.totalFloorArea}
                onChange={(e) => handleChange("totalFloorArea", e.target.value, FIELD_TYPE.NUMBER)}
              />
              {errors.totalFloorArea && <p style={{ color: "red" }}>{errors.totalFloorArea}</p>}
            </div>

            {/* Dimension */}
            <div>
              <RequiredLabel label="EST_DIMENSION" />
              <div style={{ display: "flex", gap: "8px" }}>
                <TextInput
                  placeholder={t("EST_LENGTH")}
                  value={assetFormData.dimensionLength}
                  onChange={(e) => handleChange("dimensionLength", e.target.value, FIELD_TYPE.NUMBER)}
                />
                <span>X</span>
                <TextInput
                  placeholder={t("EST_WIDTH")}
                  value={assetFormData.dimensionWidth}
                  onChange={(e) => handleChange("dimensionWidth", e.target.value, FIELD_TYPE.NUMBER)}
                />
              </div>
              {(errors.dimensionLength || errors.dimensionWidth) && (
                <p style={{ color: "red" }}>{errors.dimensionLength || errors.dimensionWidth}</p>
              )}
            </div>

            {/* Rate */}
            <div>
              <RequiredLabel label="EST_RATES" />
              <TextInput
                placeholder={t("EST_ENTER_RATE")}
                value={assetFormData.rate}
                onChange={(e) => handleChange("rate", e.target.value, FIELD_TYPE.NUMBER)}
              />
              {errors.rate && <p style={{ color: "red" }}>{errors.rate}</p>}
            </div>

            {/* Asset Reference */}
            <div>
              <RequiredLabel label="EST_ASSET_REFERENCE_NUMBER" />
              <TextInput
                placeholder={t("EST_ENTER_ASSET_REFERENCE_NUMBER")}
                value={assetFormData.assetRef}
                onChange={(e) => handleChange("assetRef", e.target.value, FIELD_TYPE.SPEC)}
              />
              {errors.assetRef && <p style={{ color: "red" }}>{errors.assetRef}</p>}
            </div>

            {/* Asset Type Dropdown */}
            <div>
              <RequiredLabel label="EST_ASSET_TYPE" />
              <Dropdown
                option={Asset_Type || []}
                optionKey="label"
                selected={Asset_Type?.find((opt) => opt.code === assetFormData.assetType) || null}
                select={(opt) => handleChange("assetType", opt.code)}
                placeholder={Asset_Type?.length ? t("EST_SELECT_ASSET_TYPE") : t("EST_NO_ASSET_TYPE_FOUND")}
                t={t}
              />
              {errors.assetType && <p style={{ color: "red" }}>{errors.assetType}</p>}
            </div>

          </div>
          <div style={{ marginTop: "24px", textAlign: "center" }}>
            <SubmitBar label={t("SAVE_&_NEXT")} submit />
          </div>
        </form>
      </Card>
      {showToast && (
        <Toast
          label={t("EST_FORM_SUBMIT_SUCCESS")}
          onClose={() => setShowToast(false)}
          type="success"
        />
      )}
    </div>
  );
};

export default NewRegistration;
