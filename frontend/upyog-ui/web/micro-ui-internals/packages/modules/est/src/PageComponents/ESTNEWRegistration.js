import React, { useState, useEffect } from "react";
import {
  Header,
  Card,
  CardLabel,
  TextInput,
  Dropdown,
  Toast,
  FormStep,
  SubmitBar,
} from "@upyog/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import { useHistory } from "react-router-dom";
import { useForm, Controller } from "react-hook-form";

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

  const allCities = Digit.Hooks.estate.useTenants();
  const cityList = allCities?.data || allCities || [];

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

  const init = formData?.Assetdata || {};

  const [buildingName, setBuildingName] = useState(init.buildingName || "");
  const [buildingNo, setBuildingNo] = useState(init.buildingNo || "");
  const [buildingFloor, setBuildingFloor] = useState(init.buildingFloor || "");
  const [buildingBlock, setBuildingBlock] = useState(init.buildingBlock || "");
  const [selectedCity, setSelectedCity] = useState(init.city ? cityList.find(c => c.code === init.city) : null);
  const [serviceType, setServiceType] = useState(init.serviceType || "");
  const [totalFloorArea, setTotalFloorArea] = useState(init.totalFloorArea || "");
  const [dimensionLength, setDimensionLength] = useState(init.dimensionLength || "");
  const [dimensionWidth, setDimensionWidth] = useState(init.dimensionWidth || "");
  const [rate, setRate] = useState(init.rate || "");
  const [assetRef, setAssetRef] = useState(init.assetRef || "");
  const [assetType, setAssetType] = useState(init.assetType || "");
  const [showToast, setShowToast] = useState(false);

  const { control } = useForm();

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
      label: locality.name || locality.i18nkey || locality.label,
    })) || [];

  useEffect(() => {
    if (Array.isArray(cityList) && tenantId) {
      const matchedCity = cityList.find((city) => city.code === tenantId);
      if (matchedCity) {
        setSelectedCity(matchedCity);
        setServiceType((prev) => (prev && prev !== matchedCity.code ? "" : prev));
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [cityList, tenantId]);

  const DEFAULT_ASSET_REF = "PG-1013-2025-I-001195";

  const isFormInvalid =
    !buildingName ||
    !buildingNo ||
    !buildingFloor ||
    !buildingBlock ||
    !selectedCity ||
    !serviceType ||
    !totalFloorArea ||
    !dimensionLength ||
    !dimensionWidth ||
    !rate ||
    !assetType;

  const sanitizeAndSet = (value, setter, { maxLength = null, regex = null } = {}) => {
    let v = value;
    if (regex) v = v.replace(regex, "");
    if (maxLength && v.length > maxLength) return;
    setter(v);
  };

  const goNext = () => {
    if (isFormInvalid) return;

    const payload = {
      buildingName,
      buildingNo,
      buildingFloor,
      buildingBlock,
      city: selectedCity?.code || "",
      serviceType,
      totalFloorArea,
      dimensionLength,
      dimensionWidth,
      rate,
      assetRef: DEFAULT_ASSET_REF,
      assetType,
    };

    try {
      if (onSelect) {
        onSelect(config?.key, { Assetdata: payload }, false);
      } else {
        console.warn("onSelect not provided. Payload:", payload);
        setShowToast(true);
      }
    } catch (err) {
      console.error("Submission failed:", err);
      setShowToast(false);
      return;
    }

    setShowToast(true);
  };

  const RequiredLabel = ({ label, unit }) => (
    <CardLabel>
      {t(label)} {unit && <span style={{ fontSize: "0.9em", marginLeft: "6px" }}>{unit}</span>} <span style={{ color: "red" }}>*</span>
    </CardLabel>
  );

  // change here: inputs now 70% width
  const fullWidthStyle = { width: "70%", marginBottom: "16px" };

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
        {/* Building Name */}
        <RequiredLabel label="EST_BUILDING_NAME" />
        <TextInput
          name="buildingName"
          placeholder={t("EST_ENTER_BUILDING_NAME")}
          value={buildingName}
          onChange={(e) =>
            sanitizeAndSet(e.target.value, setBuildingName, {
              maxLength: 100,
              regex: /[^a-zA-Z0-9\s]/g,
            })
          }
          pattern="^[a-zA-Z0-9\s]+$"
          title={t("EST_INVALID_BUILDING_NAME")}
          required
          style={fullWidthStyle}
        />

        {/* Building Number */}
        <RequiredLabel label="EST_BUILDING_NUMBER" />
        <TextInput
          name="buildingNo"
          placeholder={t("EST_ENTER_BUILDING_NUMBER")}
          value={buildingNo}
          onChange={(e) =>
            sanitizeAndSet(e.target.value, setBuildingNo, { maxLength: 10, regex: /\D/g })
          }
          pattern="^[0-9]+$"
          title={t("EST_INVALID_BUILDING_NUMBER")}
          required
          style={fullWidthStyle}
        />

        {/* Building Floor */}
        <RequiredLabel label="EST_BUILDING_FLOOR" />
        <TextInput
          name="buildingFloor"
          placeholder={t("EST_ENTER_BUILDING_FLOOR")}
          value={buildingFloor}
          onChange={(e) =>
            sanitizeAndSet(e.target.value, setBuildingFloor, { maxLength: 3, regex: /\D/g })
          }
          pattern="^[0-9]+$"
          title={t("EST_INVALID_BUILDING_FLOOR")}
          required
          style={fullWidthStyle}
        />

        {/* Building Block */}
        <RequiredLabel label="EST_BUILDING_BLOCK" />
        <TextInput
          name="buildingBlock"
          placeholder={t("EST_ENTER_BUILDING_BLOCK")}
          value={buildingBlock}
          onChange={(e) =>
            sanitizeAndSet(e.target.value, setBuildingBlock, {
              maxLength: 50,
              regex: /[^a-zA-Z0-9\s]/g,
            })
          }
          pattern="^[a-zA-Z0-9\s]+$"
          title={t("EST_INVALID_BUILDING_BLOCK")}
          required
          style={fullWidthStyle}
        />

        {/* City */}
        <RequiredLabel label="EST_CITY" />
        <Controller
          control={control}
          name="city"
          defaultValue={selectedCity}
          render={() => (
            <Dropdown
              option={cityList}
              optionKey="i18nKey"
              selected={selectedCity}
              select={(city) => {
                setSelectedCity(city);
                setServiceType((prev) => (prev && prev !== city?.code ? "" : prev));
              }}
              placeholder={t("EST_SELECT_CITY")}
              t={t}
              required
              style={fullWidthStyle}
            />
          )}
        />

        {/* Locality / Service Type */}
        <RequiredLabel label="EST_LOCALITY" />
        <Controller
          control={control}
          name="serviceType"
          defaultValue={serviceType}
          render={() => (
            <Dropdown
              option={structuredLocality}
              optionKey="i18nKey"
              selected={structuredLocality?.find((loc) => loc.code === serviceType) || null}
              select={(loc) => setServiceType(loc?.code)}
              placeholder={
                !selectedCity
                  ? t("EST_SELECT_CITY_FIRST")
                  : structuredLocality?.length
                  ? t("EST_SELECT_LOCALITY")
                  : t("EST_NO_LOCALITIES_FOUND")
              }
              t={t}
              required
              optionCardStyles={{ overflowY: "auto", maxHeight: "300px" }}
              style={fullWidthStyle}
            />
          )}
        />

        {/* Total Plot Area */}
        <RequiredLabel label="EST_TOTAL_PLOT_AREA" unit="( In sq.ft)" />
        <TextInput
          name="totalFloorArea"
          placeholder={t("EST_ENTER_TOTAL_PLOT_AREA")}
          value={totalFloorArea}
          onChange={(e) =>
            sanitizeAndSet(e.target.value, setTotalFloorArea, { maxLength: 10, regex: /\D/g })
          }
          pattern="^[0-9]+$"
          title={t("EST_INVALID_TOTAL_PLOT_AREA")}
          required
          style={fullWidthStyle}
        />

        {/* Dimension */}
        <RequiredLabel label="EST_DIMENSION" unit="( In sq.ft)" />
        {/* container uses fullWidthStyle to keep 70% width */}
        <div style={{ ...fullWidthStyle, display: "flex", gap: "16px", alignItems: "flex-start" }}>
          {/* Length */}
          <div style={{ flex: 1 }}>
            <CardLabel>{t("EST_LENGTH")}</CardLabel>
            <TextInput
              name="dimensionLength"
              placeholder={t("EST_LENGTH")}
              value={dimensionLength}
              onChange={(e) =>
                sanitizeAndSet(e.target.value, setDimensionLength, {
                  maxLength: 6,
                  regex: /\D/g,
                })
              }
              pattern="^[0-9]+$"
              title={t("EST_INVALID_LENGTH")}
              required
              style={{ width: "100%" }}
            />
          </div>

          {/* Width */}
          <div style={{ flex: 1 }}>
            <CardLabel>{t("EST_WIDTH")}</CardLabel>
            <TextInput
              name="dimensionWidth"
              placeholder={t("EST_WIDTH")}
              value={dimensionWidth}
              onChange={(e) =>
                sanitizeAndSet(e.target.value, setDimensionWidth, {
                  maxLength: 6,
                  regex: /\D/g,
                })
              }
              pattern="^[0-9]+$"
              title={t("EST_INVALID_BREADTH")}
              required
              style={{ width: "100%" }}
            />
          </div>
        </div>

        {/* Rate */}
        <RequiredLabel label="EST_RATES" unit="(Per sq ft)" />
        <TextInput
          name="rate"
          placeholder={t("EST_ENTER_RATE")}
          value={rate}
          onChange={(e) => sanitizeAndSet(e.target.value, setRate, { maxLength: 10, regex: /\D/g })}
          pattern="^[0-9]+$"
          title={t("EST_INVALID_RATE")}
          required
          style={fullWidthStyle}
        />

        {/* Asset Reference */}
        <CardLabel>{t("EST_ASSET_REFERENCE_NUMBER")}</CardLabel>
        <TextInput
          name="assetRef"
          placeholder={t("EST_ENTER_ASSET_REFERENCE_NUMBER")}
          value={assetRef}
          onChange={(e) => sanitizeAndSet(e.target.value, setAssetRef, { maxLength: 50 })}
          style={fullWidthStyle}
        />

        {/* Asset Type */}
        <RequiredLabel label="EST_ASSET_TYPE" />
        <Controller
          control={control}
          name="assetType"
          defaultValue={assetType}
          render={() => (
            <Dropdown
              option={Asset_Type || []}
              optionKey="label"
              selected={Asset_Type?.find((opt) => opt.code === assetType) || null}
              select={(opt) => setAssetType(opt?.code)}
              placeholder={
                Asset_Type?.length
                  ? t("EST_SELECT_ASSET_TYPE")
                  : t("EST_NO_ASSET_TYPE_FOUND")
              }
              t={t}
              required
              style={fullWidthStyle}
            />
          )}
        />

        {/* SAVE & NEXT BUTTON - left aligned now */}
        <div style={{ marginTop: "24px", textAlign: "left" }}>
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
