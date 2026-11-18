import React, { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { FormStep, Dropdown, TextInput, UploadFile, DatePicker, RadioButtons } from "@nudmcdgnpm/digit-ui-react-components";
import { FormField } from "../utils";
import { profileStyles } from "../utils/profileStyles";
import FamilyCreateTabs from "../config/FamilyCreateTabs";

/**
 * VehicleDetails — Step component in the Create Family Flow.
 *
 * Asks the user "Do you have a vehicle?" via a Yes/No radio (RadioButtons).
 *
 * - "No"  → saves { hasVehicle: "No" } and proceeds; no vehicle data goes into the payload.
 * - "Yes" → renders all vehicle fields and requires them to be filled before proceeding.
 *
 * Fields ( If User Have Vehicle)
 *  - Vehicle Type
 *  - Vehicle Category
 *  - Fuel Type
 *  - Vehicle Number
 *  - Purchase Year
 *  - RC Document
 *
 * View mode: all fields and the radio are read-only; state is restored from saved formData.
 */



const VEHICLE_NO_REGEX = /^([A-Z]{2}[0-9]{1,2}[A-Z]{1,3}[0-9]{4}|[0-9]{2}BH[0-9]{4}[A-Z]{1,2})$/;
const fullWidth = { width: "100%" };

const VEHICLE_TYPE = [
  { i18nKey: "2_WHEELER", code: "2_WHEELER" },
  { i18nKey: "3_WHEELER", code: "3_WHEELER" },
  { i18nKey: "4_WHEELER", code: "4_WHEELER" },
];

const VEHICLE_CATEGORY = [
  { i18nKey: "VEH_PERSONAL", code: "PERSONAL" },
  { i18nKey: "VEH_COMMERCIAL", code: "COMMERCIAL" },
  { i18nKey: "VEH_RENTAL", code: "RENTAL" },
  { i18nKey: "VEH_COMPANY_PROVIDED", code: "COMPANY_PROVIDED" },
];

const FUEL_TYPE = [
  { i18nKey: "VEH_PETROL", code: "PETROL" },
  { i18nKey: "VEH_DIESEL", code: "DIESEL" },
  { i18nKey: "VEH_CNG", code: "CNG" },
  { i18nKey: "VEH_ELECTRIC", code: "ELECTRIC" },
  { i18nKey: "VEH_HYBRID", code: "HYBRID" },
  { i18nKey: "VEH_PETROL_CNG", code: "PETROL_CNG" },
];

const VehicleDetails = ({ t, config, onSelect, userType, formData }) => {
  const [searchParams] = useSearchParams();
  const isViewMode = searchParams.get("mode") === "view" || !!config?.isViewMode;
  const saved = formData?.[config?.key] || {};

  const [hasVehicle, setHasVehicle] = useState(saved.hasVehicle || null);
  const [vehicleRc, setVehicleRc] = useState(saved.uploadedRCFile ? { name: t("PROFILE_FILE_UPLOADED") } : null);
  const [purchaseYear, setPurchaseYear] = useState(saved.purchaseYear || "");
  const [vehicleNo, setVehicleNo] = useState(saved.vehicleNo || "");
  const [fileError, setFileError] = useState(null);
  const [uploadedRCFile, setUploadedRCFile] = useState(saved.uploadedRCFile || null);
  const [vehicleNoError, setVehicleNoError] = useState(null);
  const [vehicleType, setVehicleType] = useState(saved.vehicleType || null);
  const [vehicleCategory, setVehicleCategory] = useState(saved.vehicleCategory || null);
  const [fuelType, setFuelType] = useState(saved.fuelType || null);
  const [vehicleNoValidated, setVehicleNoValidated] = useState(saved.vehicleNoValidated || false);
  const [vehicleNoValidateStatus, setVehicleNoValidateStatus] = useState(null);

  const goNext = () => {
    if (isViewMode) {
      onSelect(config.key, saved, false);
      return;
    }
    if (hasVehicle === "No") {
      onSelect(config.key, { hasVehicle: "No" }, false);
      return;
    }
    onSelect(config.key, { hasVehicle: "Yes", vehicleRc, uploadedRCFile, purchaseYear, vehicleNo, vehicleNoValidated, fuelType, vehicleCategory, vehicleType }, false);
  };

  const onSkip = () => onSelect();

  useEffect(() => {
    if (userType === "citizen" && !isViewMode) goNext();
  }, [vehicleNo, purchaseYear, uploadedRCFile, fuelType, vehicleCategory, vehicleType, vehicleNoValidated]);

  const handleVehicleNoChange = (e) => {
    const val = e.target.value.toUpperCase();
    if (val.length > 13) return;
    setVehicleNo(val);
    setVehicleNoValidated(false);
    setVehicleNoValidateStatus(null);
    setVehicleNoError(val && !VEHICLE_NO_REGEX.test(val) ? t("PROFILE_INVALID_VEHICLE_NO") : null);
  };

  const handleValidateVehicleNo = () => {
    if (!vehicleNo) return;
    if (VEHICLE_NO_REGEX.test(vehicleNo)) {
      setVehicleNoValidated(true);
      setVehicleNoValidateStatus("success");
      setVehicleNoError(null);
    } else {
      setVehicleNoValidated(false);
      setVehicleNoValidateStatus("error");
      setVehicleNoError(t("PROFILE_INVALID_VEHICLE_NO"));
    }
  };

  const handleFileSelect = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    if (file.size >= 5242880) {
      setFileError(t("CS_MAXIMUM_UPLOAD_SIZE_EXCEEDED"));
      return;
    }
    setFileError(null);
    setVehicleRc(file);
    (async () => {
      try {
        const response = await Digit.UploadServices.Filestorage("profile", file, Digit.ULBService.getStateId());
        if (response?.data?.files?.length > 0) setUploadedRCFile(response.data.files[0].fileStoreId);
        else setFileError(t("CS_FILE_UPLOAD_ERROR"));
      } catch {
        setFileError(t("CS_FILE_UPLOAD_ERROR"));
      }
    })();
  };

  const isValidateBtnDisabled = !vehicleNo || !!vehicleNoError;
  const isDisabled = hasVehicle === null
    ? true
    : hasVehicle === "No"
    ? false
    : !uploadedRCFile || !purchaseYear || !fuelType || !vehicleCategory || !vehicleType || !vehicleNoValidated;

  return (
    <React.Fragment>
      <FamilyCreateTabs t={t} />
      <FormStep
        config={{
          ...config,
          texts: {
            ...config?.texts,
            submitBarLabel: isViewMode ? "COMMON_NEXT" : (config?.texts?.submitBarLabel || "COMMON_SAVE_NEXT"),
          },
        }}
        onSelect={goNext}
        onSkip={onSkip}
        t={t}
        isDisabled={isViewMode ? false : isDisabled}
      >

          <FormField label={t("PROFILE_DO_YOU_HAVE_VEHICLE")} required t={t}></FormField>
          <RadioButtons
            options={[{ code: "Yes", i18nKey: "COMMON_YES" }, { code: "No", i18nKey: "COMMON_NO" }]}
            optionsKey="i18nKey"
            isDependent={true}
            selectedOption={hasVehicle ? { code: hasVehicle } : null}
            onSelect={(val) => !isViewMode && setHasVehicle(val.code)}
            disabled={isViewMode}
            name="hasVehicle"
          />

        {/* Fields — only when Yes */}
      {hasVehicle === "Yes" && (
        <div style={profileStyles.gridStyle}>
          <FormField label={t("PROFILE_VEHICLE_TYPE")} required t={t}>
            <Dropdown
              selected={vehicleType}
              select={setVehicleType}
              option={VEHICLE_TYPE}
              optionKey="i18nKey"
              t={t}
              style={fullWidth}
              placeholder={t("PROFILE_SELECT_VEHICLE_TYPE")}
              disable={isViewMode}
            />
          </FormField>

          <FormField label={t("PROFILE_VEHICLE_CATEGORY")} required t={t}>
            <Dropdown
              selected={vehicleCategory}
              select={setVehicleCategory}
              option={VEHICLE_CATEGORY}
              optionKey="i18nKey"
              t={t}
              style={fullWidth}
              placeholder={t("PROFILE_SELECT_VEHICLE_CATEGORY")}
              disable={isViewMode}
            />
          </FormField>

          <FormField label={t("PROFILE_VEHICLE_FUEL_TYPE")} required t={t}>
            <Dropdown
              selected={fuelType}
              select={setFuelType}
              option={FUEL_TYPE}
              optionKey="i18nKey"
              t={t}
              style={fullWidth}
              placeholder={t("PROFILE_SELECT_FUEL_CATEGORY")}
              disable={isViewMode}
            />
          </FormField>

          <FormField label={t("PROFILE_VEHICLE_NO")} required t={t} error={vehicleNoError}>
            <div style={profileStyles.taxIdInputRow}>
              <div style={profileStyles.taxIdInputWrap}>
                <TextInput
                  t={t}
                  type="text"
                  name="vehicleNo"
                  value={vehicleNo}
                  onChange={handleVehicleNoChange}
                  placeholder="MH12AB1234 / 22BH1234AB"
                  maxLength={10}
                  style={fullWidth}
                  disable={isViewMode}
                />
              </div>
              <button
                type="button"
                onClick={handleValidateVehicleNo}
                disabled={isViewMode || isValidateBtnDisabled}
                style={{
                  ...profileStyles.validateBtn,
                  ...(vehicleNoValidated ? profileStyles.validateBtnSuccess : {}),
                  ...(isViewMode || isValidateBtnDisabled ? profileStyles.validateBtnDisabled : {}),
                }}
              >
                {vehicleNoValidated ? `✓ ${t("PROFILE_VALIDATED")}` : t("PROFILE_VALIDATE")}
              </button>
            </div>
            {vehicleNoValidateStatus === "success" && (
              <p style={profileStyles.validateSuccessMsg}>{t("PROFILE_VEHICLE_NO_VALID")}</p>
            )}
          </FormField>

          <FormField label={t("PROFILE_VEHICLE_PURCHASE_YEAR")} required t={t}>
            <DatePicker
              date={purchaseYear}
              onChange={setPurchaseYear}
              max={new Date().toISOString().split("T")[0]}
              disabled={isViewMode}
            />
          </FormField>

          <FormField label={t("PROFILE_VEHICLE_RC")} required t={t} error={fileError}>
            <UploadFile
              onUpload={handleFileSelect}
              onDelete={isViewMode ? undefined : () => {
                setVehicleRc(null);
                setUploadedRCFile(null);
                setFileError(null);
              }}
              message={vehicleRc ? vehicleRc.name : t("PROFILE_NO_FILE_SELECTED")}
              accept=".pdf,.jpg,.jpeg,.png"
              iserror={fileError}
              disabled={isViewMode}
            />
            <p style={profileStyles.documentUploadCard.fileRestrictionText}>
              {t("CS_FILE_SIZE_RESTRICTIONS")}
            </p>
          </FormField>
        </div>
      )}
      </FormStep>
    </React.Fragment>
  );
};

export default VehicleDetails;
