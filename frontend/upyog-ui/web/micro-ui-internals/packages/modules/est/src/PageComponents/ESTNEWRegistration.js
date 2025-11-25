import React, { useState, useEffect } from "react";
import {
  Header,
  Card,
  CardLabel,
  TextInput,
  Dropdown,
  Toast,
  FormStep,
  SubmitBar,          // 🔹 ADD THIS
} from "@upyog/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import { useHistory } from "react-router-dom";

const NewRegistration = ({ parentRoute, t: propT, onSelect, onSkip, formData = {}, config }) => {
  const { t: hookT } = useTranslation();
  const t = propT || hookT;
  const history = useHistory();

  const tenantId = Digit.ULBService.getCurrentTenantId();
  const [_formData, setFormData, _clear] = Digit.Hooks.useSessionStorage("EST_CREATE_DATA", null);
  const [mutationHappened, setMutationHappened, clear] = Digit.Hooks.useSessionStorage(
    "EST_MUTATION_HAPPENED",
    false
  );
  const [successData, setSuccessData, clearSuccessData] = Digit.Hooks.useSessionStorage(
    "EST_MUTATION_SUCCESS_DATA",
    {}
  );

  // 🔹 All Cities (hook may return array or { data: [] })
  const allCities = Digit.Hooks.estate.useTenants();
  const cityList = allCities?.data || allCities || [];

  const [selectedCity, setSelectedCity] = useState(null);
  const [showToast, setShowToast] = useState(false);

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

  // ------------------ FORM STATE ------------------
  const [assetFormData, setAssetFormData] = useState({
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

  // 🔹 simple handler like SVBankDetails
  const handleInputChange = (field, value, maxLength = null, regex = null) => {
    if (regex) value = value.replace(regex, "");
    if (maxLength && value.length > maxLength) return;
    setAssetFormData((prev) => ({ ...prev, [field]: value }));
  };

  // 🔹 Auto-select city based on tenantId
  useEffect(() => {
    if (Array.isArray(cityList) && tenantId) {
      const matchedCity = cityList.find((city) => city.code === tenantId);
      if (matchedCity) {
        setSelectedCity(matchedCity);
        setAssetFormData((prev) => ({
          ...prev,
          city: matchedCity.code,
          serviceType: prev.city !== matchedCity.code ? "" : prev.serviceType,
        }));
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

  const structuredLocality =
    fetchedLocalities?.map((locality) => ({
      ...locality,
      i18nKey: `TENANT_TENANTS_${locality.code.toUpperCase()}`,
    })) || [];

  // ------------------ VALIDATIONS + DISABLE LOGIC ------------------
  const isFormInvalid =
    !assetFormData.buildingName ||
    !assetFormData.buildingNo ||
    !assetFormData.buildingFloor ||
    !assetFormData.buildingBlock ||
    !selectedCity ||
    !assetFormData.serviceType ||
    !assetFormData.totalFloorArea ||
    !assetFormData.dimensionLength ||
    !assetFormData.dimensionWidth ||
    !assetFormData.rate ||
    !assetFormData.assetRef ||
    !assetFormData.assetType;

  // ------------------ goNext (like ESTAssignAssets) ------------------
  const goNext = () => {
    if (isFormInvalid) return; // safety
    onSelect && onSelect(config?.key, { Assetdata: assetFormData }, false);
    setShowToast(true);
  };

  const RequiredLabel = ({ label }) => (
    <CardLabel>
      {t(label)} <span style={{ color: "red" }}>*</span>
    </CardLabel>
  );

  return (
    <FormStep
      t={t}
      config={config}
      onSelect={goNext}
      onSkip={onSkip}
      isDisabled={isFormInvalid}
    >
      <Header>{t("EST_COMMON_NEW_REGISTRATION")}</Header>

      <Card style={{ padding: "16px" }}>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(2, 1fr)", gap: "16px" }}>
          {/* Building Name */}
          <div>
            <RequiredLabel label="EST_BUILDING_NAME" />
            <TextInput
              name="buildingName"
              placeholder={t("EST_ENTER_BUILDING_NAME")}
              value={assetFormData.buildingName}
              onChange={(e) =>
                handleInputChange(
                  "buildingName",
                  e.target.value,
                  100,
                  /[^a-zA-Z0-9\s]/g // only letters, numbers, space
                )
              }
              pattern="^[a-zA-Z0-9\s]+$"
              title={t("EST_INVALID_BUILDING_NAME")}
              required
            />
          </div>

          {/* Building Number */}
          <div>
            <RequiredLabel label="EST_BUILDING_NUMBER" />
            <TextInput
              name="buildingNo"
              placeholder={t("EST_ENTER_BUILDING_NUMBER")}
              value={assetFormData.buildingNo}
              onChange={(e) =>
                handleInputChange("buildingNo", e.target.value, 10, /\D/g) // digits only
              }
              pattern="^[0-9]+$"
              title={t("EST_INVALID_BUILDING_NUMBER")}
              required
            />
          </div>

          {/* Building Floor */}
          <div>
            <RequiredLabel label="EST_BUILDING_FLOOR" />
            <TextInput
              name="buildingFloor"
              placeholder={t("EST_ENTER_BUILDING_FLOOR")}
              value={assetFormData.buildingFloor}
              onChange={(e) =>
                handleInputChange("buildingFloor", e.target.value, 3, /\D/g)
              }
              pattern="^[0-9]+$"
              title={t("EST_INVALID_BUILDING_FLOOR")}
              required
            />
          </div>

          {/* Building Block */}
          <div>
            <RequiredLabel label="EST_BUILDING_BLOCK" />
            <TextInput
              name="buildingBlock"
              placeholder={t("EST_ENTER_BUILDING_BLOCK")}
              value={assetFormData.buildingBlock}
              onChange={(e) =>
                handleInputChange(
                  "buildingBlock",
                  e.target.value,
                  50,
                  /[^a-zA-Z0-9\s]/g
                )
              }
              pattern="^[a-zA-Z0-9\s]+$"
              title={t("EST_INVALID_BUILDING_BLOCK")}
              required
            />
          </div>

          {/* City */}
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
                  serviceType: prev.city !== city?.code ? "" : prev.serviceType,
                }));
              }}
              placeholder={t("EST_SELECT_CITY")}
              t={t}
              required
            />
          </div>

          {/* Locality */}
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
              required
            />
          </div>

          {/* Total Plot Area */}
          <div>
            <RequiredLabel label="EST_TOTAL_PLOT_AREA" />
            <TextInput
              name="totalFloorArea"
              placeholder={t("EST_ENTER_TOTAL_PLOT_AREA")}
              value={assetFormData.totalFloorArea}
              onChange={(e) =>
                handleInputChange("totalFloorArea", e.target.value, 10, /\D/g)
              }
              pattern="^[0-9]+$"
              title={t("EST_INVALID_TOTAL_PLOT_AREA")}
              required
            />
          </div>

          {/* Dimension (Length x Width) */}
          <div>
            <RequiredLabel label="EST_DIMENSION" />
            <div style={{ display: "flex", gap: "8px", alignItems: "center" }}>
              <TextInput
                name="dimensionLength"
                placeholder={t("EST_LENGTH")}
                value={assetFormData.dimensionLength}
                onChange={(e) =>
                  handleInputChange("dimensionLength", e.target.value, 6, /\D/g)
                }
                pattern="^[0-9]+$"
                title={t("EST_INVALID_LENGTH")}
                required
              />
              <span>X</span>
              <TextInput
                name="dimensionWidth"
                placeholder={t("EST_WIDTH")}
                value={assetFormData.dimensionWidth}
                onChange={(e) =>
                  handleInputChange("dimensionWidth", e.target.value, 6, /\D/g)
                }
                pattern="^[0-9]+$"
                title={t("EST_INVALID_BREADTH")}
                required
              />
            </div>
          </div>

          {/* Rate */}
          <div>
            <RequiredLabel label="EST_RATES" />
            <TextInput
              name="rate"
              placeholder={t("EST_ENTER_RATE")}
              value={assetFormData.rate}
              onChange={(e) => handleInputChange("rate", e.target.value, 10, /\D/g)}
              pattern="^[0-9]+$"
              title={t("EST_INVALID_RATE")}
              required
            />
          </div>

          {/* Asset Reference */}
          <div>
            <RequiredLabel label="EST_ASSET_REFERENCE_NUMBER" />
            <TextInput
              name="assetRef"
              placeholder={t("EST_ENTER_ASSET_REFERENCE_NUMBER")}
              value={assetFormData.assetRef}
              onChange={(e) =>
                handleInputChange(
                  "assetRef",
                  e.target.value,
                  50,
                  /[^a-zA-Z0-9\-\/\s]/g
                )
              }
              pattern="^[a-zA-Z0-9\-\/\s]+$"
              title={t("EST_INVALID_ASSET_REFERENCE_NUMBER")}
              required
            />
          </div>

          {/* Asset Type */}
          <div>
            <RequiredLabel label="EST_ASSET_TYPE" />
            <Dropdown
              option={Asset_Type || []}
              optionKey="label"
              selected={
                Asset_Type?.find((opt) => opt.code === assetFormData.assetType) ||
                null
              }
              select={(opt) =>
                setAssetFormData((prev) => ({
                  ...prev,
                  assetType: opt.code,
                }))
              }
              placeholder={
                Asset_Type?.length
                  ? t("EST_SELECT_ASSET_TYPE")
                  : t("EST_NO_ASSET_TYPE_FOUND")
              }
              t={t}
              required
            />
          </div>
        </div>

        {/* 🔹 CUSTOM SAVE & NEXT BUTTON */}
        <div style={{ marginTop: "24px", textAlign: "center" }}>
          <SubmitBar
            label={t("SAVE_&_NEXT")}
            onSubmit={goNext}
            disabled={isFormInvalid}
          />
        </div>
      </Card>

      {showToast && (
        <Toast
          label={t("EST_FORM_SUBMIT_SUCCESS")}
          onClose={() => setShowToast(false)}
          type="success"
        />
      )}
    </FormStep>
  );
};

export default NewRegistration;
