import React, { useState, useEffect, useRef } from "react";
import {
  Header,
  Card,
  CardLabel,
  TextInput,
  Dropdown,
  FormStep,
  SubmitBar,
} from "@upyog/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import { useForm, Controller } from "react-hook-form";
import { useLocation } from "react-router-dom";

const NewRegistration = ({ parentRoute, t: propT, onSelect, onSkip, formData = {}, config }) => {
  const { t: hookT } = useTranslation();
  const t = propT || hookT;

  const location = useLocation();
  const editData = location.state?.assetData;
  const isEditMode = location.state?.isEdit;
 
  const tenantId = Digit.ULBService.getCurrentTenantId();
  const [_formData, setFormData, _clear] = Digit.Hooks.useSessionStorage("EST_CREATE_DATA", null);

  const isMounted = useRef(true);
  useEffect(() => {
    return () => {
      isMounted.current = false;
    };
  }, []);

  const { data: Asset_Type } = Digit.Hooks.useEnabledMDMS(
    Digit.ULBService.getStateId(),
    "ASSET",
    [{ name: "assetParentCategory" }],
    {
      select: (data) => {
        const formattedData = data?.ASSET?.assetParentCategory || [];
        const activeData = formattedData.filter((item) => item.active === true);
        return activeData.map((item) => ({
          i18nKey: `ASSET_TYPE_${item.code}`,
          code: item.code,
          label: item.name,
        }))
      },
    }
  );

  const allCities = Digit.Hooks.estate.useTenants();
  const cityList = allCities?.data || allCities || [];


const [buildingName, setBuildingName] = useState("");
const [buildingNo, setBuildingNo] = useState("");
const [buildingFloor, setBuildingFloor] = useState("");
const [buildingBlock, setBuildingBlock] = useState("");
const [selectedCity, setSelectedCity] = useState(null);
const [serviceType, setServiceType] = useState("");
const [totalFloorArea, setTotalFloorArea] = useState("");
const [dimensionLength, setDimensionLength] = useState("");
const [dimensionWidth, setDimensionWidth] = useState("");
const [rate, setRate] = useState("");
const [assetRef, setAssetRef] = useState("");
const [assetType, setAssetType] = useState("");

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
      label: locality.name || locality.i18nKey || locality.label,
    })) || [];

useEffect(() => {
 
  if (isEditMode && editData) {
    setBuildingName(editData.buildingName || "");
    setBuildingNo(editData.buildingNo || "");
    setBuildingFloor(editData.floor || editData.buildingFloor || "");
    setBuildingBlock(editData.buildingBlock || "");
    setTotalFloorArea(editData.totalFloorArea || "");
    setDimensionLength(editData.dimensionLength || "");
    setDimensionWidth(editData.dimensionWidth || "");
    setRate(editData.rate || "");
    setAssetRef(editData.refAssetNo || editData.assetRef || "");
    
    const assetTypeCode = editData.assetType?.code || editData.assetType;
    setAssetType(assetTypeCode || "");
    
    if (editData.locality && structuredLocality?.length > 0) {
      const matchedLocality = structuredLocality.find(
        loc => loc.label === editData.locality || loc.name === editData.locality
      );
      if (matchedLocality) {
        setServiceType(matchedLocality.code);
      }
    }
  }
}, [isEditMode, editData, structuredLocality]);


useEffect(() => {
  if (isEditMode && editData && editData.locality && structuredLocality?.length > 0) {
    const matchedLocality = structuredLocality.find(
      loc => loc.label === editData.locality || loc.name === editData.locality
    );
    if (matchedLocality) {
      setServiceType(matchedLocality.code);
    }
  }
}, [isEditMode, editData, structuredLocality]);


useEffect(() => {
  if (Array.isArray(cityList) && tenantId) {
    const matchedCity = cityList.find((city) => city.code === tenantId);
    if (matchedCity) {
      setSelectedCity(matchedCity);
      if (!isEditMode) {
       setServiceType((prev) => (prev && prev !== matchedCity?.code ? "" : prev));

      }
    }
  }
}, [cityList, tenantId, isEditMode]);

  const isFormInvalid =
    !buildingName ||
    !buildingNo ||
    !buildingFloor ||
    !selectedCity ||
    !serviceType ||
    !totalFloorArea ||
    !dimensionLength ||
    !dimensionWidth ||
    !rate ||
    !assetType;

  const sanitizeAndSet = (value, setter, { maxLength = null, regex = null } = {}) => {
    let v = value ?? "";
    if (regex) v = v.replace(regex, "");
    if (maxLength && v.length > maxLength) v = v.slice(0, maxLength);
    setter(v);
  };

  const goNext = () => {
    if (isFormInvalid) return;

    const selectedLocality =
      structuredLocality?.find((locality) => locality.code === serviceType) || null;

    const payload = {
      buildingName,
      buildingNo,
      buildingFloor,
      buildingBlock,
      city: selectedCity?.code || "",
      totalFloorArea,
      dimensionLength,
      dimensionWidth,
      rate,
      locality: selectedLocality?.label || "",
      localityCode: selectedLocality?.code || "",
      localityName: selectedLocality?.label || "", 
      assetRef: assetRef || "",
      assetType,
    };

    if (isEditMode) {
      const updatePayload = {
        Asset: {
          ...payload,
          id: editData.id,
          estateNo: editData.estateNo,
          tenantId: tenantId
        }
      };
      updateMutation.mutate(updatePayload, {
        onSuccess: () => {
          
        },
        onError: (error) => {
          console.error("Update failed:", error);
        }
      });
    } else {
      onSelect(config?.key, { Assetdata: payload }, false);
    }
  };

  const RequiredLabel = ({ label, unit }) => (
    <CardLabel>
      {t(label)}{" "}
      {unit && <span style={{ fontSize: "0.9em", marginLeft: "6px" }}>{unit}</span>}{" "}
      <span style={{ color: "red" }}>*</span>
    </CardLabel>
  );

  const fullWidthStyle = { width: "70%", marginBottom: "16px" };

  return (
    <FormStep t={t} config={config} onSelect={goNext} onSkip={onSkip} isDisabled={isFormInvalid}>
      <Header>{isEditMode ? t("EST_UPDATE_ASSET") : t("EST_COMMON_NEW_REGISTRATION")}</Header>

      <Card style={{ padding: "16px" }}>
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

        <CardLabel>{t("EST_BUILDING_BLOCK")}</CardLabel>
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

        <RequiredLabel label="EST_LOCALITY" />
        <Controller
          control={control}
          name="serviceType"
          defaultValue={serviceType}
          render={() => (
            <Dropdown
              option={structuredLocality}
              optionKey="i18nKey"
              selected={
                structuredLocality?.find((loc) => loc.code === serviceType) || null
              }
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

        <RequiredLabel label="EST_TOTAL_PLOT_AREA" unit="( In sq.ft)" />
        <TextInput
          name="totalFloorArea"
          placeholder={t("EST_ENTER_TOTAL_PLOT_AREA")}
          value={totalFloorArea}
          onChange={(e) =>
            sanitizeAndSet(e.target.value, setTotalFloorArea, {
              maxLength: 10,
              regex: /\D/g,
            })
          }
          pattern="^[0-9]+$"
          title={t("EST_INVALID_TOTAL_PLOT_AREA")}
          required
          style={fullWidthStyle}
        />

        <RequiredLabel label="EST_DIMENSION" unit="( In sq.ft)" />
        <div
          style={{
            ...fullWidthStyle,
            display: "flex",
            gap: "16px",
            alignItems: "flex-start",
          }}
        >
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

        <RequiredLabel label="EST_RATES" unit="(Per sq ft)" />
        <TextInput
          name="rate"
          placeholder={t("EST_ENTER_RATE")}
          value={rate}
          onChange={(e) =>
            sanitizeAndSet(e.target.value, setRate, {
              maxLength: 10,
              regex: /\D/g,
            })
          }
          pattern="^[0-9]+$"
          title={t("EST_INVALID_RATE")}
          required
          style={fullWidthStyle}
        />

        <CardLabel>{t("EST_ASSET_REFERENCE_NUMBER")}</CardLabel>
        <TextInput
          name="assetRef"
          placeholder={t("EST_ENTER_ASSET_REFERENCE_NUMBER")}
          value={assetRef}
          onChange={(e) =>
            sanitizeAndSet(e.target.value, setAssetRef, { maxLength: 50 })
          }
          style={fullWidthStyle}
        />

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
                Asset_Type && Asset_Type.length
                  ? t("EST_SELECT_ASSET_TYPE")
                  : t("EST_NO_ASSET_TYPE_FOUND")
              }
              t={t}
              required
              style={fullWidthStyle}
            />
          )}
        />

        <div style={{ marginTop: "24px", textAlign: "left" }}>
          <SubmitBar
            label={isEditMode ? t("UPDATE") : t("SAVE_&_NEXT")}
            onSubmit={goNext}
            disabled={isFormInvalid}
          />
        </div>
      </Card>
    </FormStep>
  );
};

export default NewRegistration;