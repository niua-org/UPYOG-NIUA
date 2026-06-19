import React, { useState, useEffect, useRef } from "react";
import {
  Header,
  Card,
  CardLabel,
  TextInput,
  Dropdown,
  FormStep,
  SubmitBar,
  FormFields,
} from "@nudmcdgnpm/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import { useForm, Controller } from "react-hook-form";
import { useLocation } from "react-router-dom";
import FormDropdown from '../components/FormDropdown'

/**
 * NewRegistration Component
 * ------------------------------------------------
 * This component is responsible for:
 * - Creating a new Estate Asset
 * - Editing an existing Estate Asset
 *
 * The behavior is controlled by:
 * - `isEditMode` flag from route state
 * - `editData` passed via navigation
 */

const NewRegistration = ({ parentRoute, t: propT, onSelect, onSkip, formData = {}, config }) => {

  console.log('config is  ',config.form)

// Translation handler (prop-based or hook-based fallback)
  const { t: hookT } = useTranslation();
  const t = propT || hookT;
  const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);
  

  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth <= 768);
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const location = useLocation();
  const editData = location.state?.assetData;

    // Read routing state for edit flow
  const isEditMode = location.state?.isEdit;
 
   // Current tenant (ULB) id
  const tenantId = Digit.ULBService.getCurrentTenantId();
  // Session storage hook to persist draft form data (if needed)
  const [_formData, setFormData, _clear] = Digit.Hooks.useSessionStorage("EST_CREATE_DATA", null);

 // Track component mount status (useful for async safety)
  const isMounted = useRef(true);
  useEffect(() => {
    return () => {
      isMounted.current = false;
    };
  }, []);


  /**
   * Fetch Asset Types from MDMS
   * Only active asset categories are shown in the dropdown
   */
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
  // Fetch all cities / ULBs
  const allCities = Digit.Hooks.estate.useTenants();
  const cityList = allCities?.data || allCities || [];
  const [dimensionError, setDimensionError] = useState("");

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
  // react-hook-form controller instance
const { control } = useForm();

/**
   * Fetch localities based on selected city
   */
  const { data: fetchedLocalities } = Digit.Hooks.useBoundaryLocalities(
    selectedCity?.code,
    "revenue",
    { enabled: !!selectedCity },
    t
  );
/**
   * Format locality data for Dropdown usage
   */
  const structuredLocality =
    fetchedLocalities?.map((locality) => ({
      ...locality,
      i18nKey: `TENANT_TENANTS_${locality.code.toUpperCase()}`,
      label: locality.name || locality.i18nKey || locality.label,
    })) || [];


     /**
   * Populate form fields in Edit Mode
   * Runs when edit data or locality list is available
   */
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

 /**
   * Automatically select the city based on tenant ID
   */
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

 /**
   * Form validation:
   * If any required field is missing, form is invalid
   */
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

const validateDimensions = (length, width, totalArea) => {
  if (!length || !width || !totalArea) {
    setDimensionError("");
    return true;
  }

  const dimensionArea = Number(length) * Number(width);
  const totalAreaNum = Number(totalArea);

  if (dimensionArea > totalAreaNum) {
    const errorMsg = "Length × Width (" + dimensionArea + ") cannot be greater than Total Plot Area (" + totalAreaNum + ")";
    setDimensionError(errorMsg);
    return false;
  }
  setDimensionError("");
  return true;
};


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
    !assetType ||
    !!dimensionError;

     /**
   * Generic input sanitizer
   * - Removes invalid characters
   * - Enforces max length
   */
  const sanitizeAndSet = (value, setter, { maxLength = null, regex = null } = {}) => {
    let v = value ?? "";
    if (regex) v = v.replace(regex, "");
    if (maxLength && v.length > maxLength) v = v.slice(0, maxLength);
    setter(v);
  };


   /**
   * Submit handler:
   * - Prepares payload
   * - Handles create or update flow
   */
  const goNext = () => {
    
    // Force validation check before proceeding
    const isValid = validateDimensions(dimensionLength, dimensionWidth, totalFloorArea);
    
    if (!isValid || dimensionError) {
      alert("Error: Length × Width cannot be greater than Total Plot Area");
      return;
    }

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
    // Update flow
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
        }
      });
          // Create flow
    } else {
      onSelect(config?.key, { Assetdata: payload }, false);
    }
  };



  /**
   * Label component for required fields
   */
  const RequiredLabel = ({ label, unit, required }) => (
    <CardLabel>
      {t(label)}{" "}
      {unit && <span style={{ fontSize: "0.9em", marginLeft: "6px" }}>{unit}</span>}{" "}
      {required && <span style={{ color: "red" }}>*</span>}
    </CardLabel>
  );
// Standard input width styling
  const fullWidthStyle = { width: "70%", marginBottom: "16px" };
  const cardStyle = {
    padding: isMobile ? "12px" : "16px"
  };

  const dimensionContainerStyle = {
    ...fullWidthStyle,
    display: "flex",
    flexDirection: isMobile ? "column" : "row",
    gap: isMobile ? "8px" : "16px",
    alignItems: "flex-start",
  };
  return (
    <>
    {/* <FormWizard  /> */}
    <FormStep t={t} config={config} onSelect={goNext} onSkip={onSkip} isDisabled={isFormInvalid}>
      <Header style={{ fontSize: isMobile ? '18px' : '24px', marginBottom: '15px' }}>
        {isEditMode ? t("EST_UPDATE_ASSET") : t("EST_COMMON_NEW_REGISTRATION")}
      </Header>
      {/* <FormFields formFields={config.form} /> */}
      {config.form.map((item) => (
        <div key={item.key}>
          <RequiredLabel label={item.field?.code} required={item.validation?.required || false} />
          <p>{item.field?.type}</p>
          { item.field?.type === "text" ? 
          <TextInput
          name= {item.field?.name}
          placeholder={t(`${item.field?.placeholder}`)}
          value={buildingName}
          onChange={(e) =>
            sanitizeAndSet(e.target.value, setBuildingName, {
              maxLength: item.validation?.maxLength,
              regex: `${item.validation?.regex?.pattern}/${item.validation?.regex?.flags}` 
            })
          }
          pattern={item.validation?.pattern}
          title={t("EST_INVALID_BUILDING_NAME")}
          required={item.validation?.required}
          style={fullWidthStyle}
        /> : 
        <>
        <p>form dropdown</p>
        <FormDropdown entry={item} propT={t}/>
        <p>"name: "+{item.field?.name}</p>
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
        </>
       }
      </div>
      ))} 

      <Card style={cardStyle}>
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
          onChange={(e) => {
            const value = e.target.value.replace(/\D/g, "");
            setTotalFloorArea(value);
            validateDimensions(dimensionLength, dimensionWidth, value);
          }}
          pattern="^[0-9]+$"
          title={t("EST_INVALID_TOTAL_PLOT_AREA")}
          required
          style={fullWidthStyle}
        />

        <RequiredLabel label="EST_DIMENSION" unit="( In sq.ft)" />
        <div style={dimensionContainerStyle}>
          <div style={{ flex: 1 }}>
            <CardLabel style={{ fontSize: isMobile ? '14px' : '16px' }}>{t("EST_LENGTH")}</CardLabel>
            <TextInput
              name="dimensionLength"
              placeholder={t("EST_LENGTH")}
              value={dimensionLength}
              onChange={(e) => {
  const value = e.target.value.replace(/\D/g, "");
  setDimensionLength(value);
  validateDimensions(value, dimensionWidth, totalFloorArea);
}}

              pattern="^[0-9]+$"
              title={t("EST_INVALID_LENGTH")}
              required
              style={{ width: "100%" }}
            />
          </div>

          <div style={{ flex: 1 }}>
            <CardLabel style={{ fontSize: isMobile ? '14px' : '16px' }}>{t("EST_WIDTH")}</CardLabel>
            <TextInput
              name="dimensionWidth"
              placeholder={t("EST_WIDTH")}
              value={dimensionWidth}
              onChange={(e) => {
  const value = e.target.value.replace(/\D/g, "");
  setDimensionWidth(value);
  validateDimensions(dimensionLength, value, totalFloorArea);
}}

              pattern="^[0-9]+$"
              title={t("EST_INVALID_WIDTH")}
              required
              style={{ width: "100%" }}
            />
          </div>
        </div>
        {dimensionError && (
  <div style={{ color: "red", marginTop: "8px", fontSize: "14px" }}>
    {dimensionError}
  </div>
)}


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
    </>
  );
};

export default NewRegistration;