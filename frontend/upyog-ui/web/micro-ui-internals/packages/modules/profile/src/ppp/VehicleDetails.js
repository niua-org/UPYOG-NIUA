import React, { useState } from "react";
import { useSearchParams } from "react-router-dom";
import { FormStep, Dropdown, TextInput, UploadFile, DatePicker, RadioButtons, DeleteIcon } from "@nudmcdgnpm/digit-ui-react-components";
import { FormField } from "../utils";
import { profileStyles } from "../utils/profileStyles";
import FamilyCreateTabs from "../config/FamilyCreateTabs";

/**
 * VehicleDetails — Step component in the Create Family Flow.
 *
 * @description
 * Multi-Vehicle Step Component for Create Family / Join Family flow.
 *
 * Features:
 * - Radio selection for "Do you have a vehicle?" (Yes / No).
 * - Supports adding up to 3 vehicles (Max 3).
 * - "+ Add Another Vehicle" button below vehicle cards (hidden when 3 vehicles are reached).
 * - Individual Delete button for vehicle cards (Vehicle 1 cannot be removed if Yes).
 * - Independent validation and independent RC document upload for each vehicle.
 * - Stores state in `vehicleList` array in `Family_Details` session storage.
 * - View mode support: read-only inspection.
 * - RC upload keyed by stable veh.id (not index) to prevent mid-upload race condition.
 *
 * @param {Object} props
 * @returns {React.ReactElement}
 */

// Regex / Validation for vehcile noumber accept both standard vehicle no and bharat no
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

// Helper to create an initial blank vehicle model
const createBlankVehicle = () => ({
  id: `veh-${Date.now()}-${Math.random().toString(36).substr(2, 4)}`,
  vehicleType: null,
  vehicleCategory: null,
  fuelType: null,
  vehicleNo: "",
  vehicleNoError: null,
  vehicleNoValidated: false,
  vehicleNoValidateStatus: null,
  purchaseYear: "",
  uploadedRCFile: null,
  vehicleRc: null,
  fileError: null,
});

const VehicleDetails = ({ t, config, onSelect, formData }) => {
  const [searchParams] = useSearchParams();
  const isViewMode = searchParams.get("mode") === "view" || !!config?.isViewMode;
  const saved = formData?.[config?.key] || {};

  const [hasVehicle, setHasVehicle] = useState(saved.hasVehicle || null);

  // Initialize vehicleList: restore from saved.vehicleList, or fallback from saved single vehicle
  const [vehicleList, setVehicleList] = useState(() => {
    if (Array.isArray(saved.vehicleList) && saved.vehicleList.length > 0) {
      return saved.vehicleList;
    }
    if (saved.vehicleNo) {
      return [
        {
          id: "veh-initial",
          vehicleType: saved.vehicleType || null,
          vehicleCategory: saved.vehicleCategory || null,
          fuelType: saved.fuelType || null,
          vehicleNo: saved.vehicleNo || "",
          vehicleNoError: null,
          vehicleNoValidated: saved.vehicleNoValidated || false,
          vehicleNoValidateStatus: saved.vehicleNoValidated ? "success" : null,
          purchaseYear: saved.purchaseYear || "",
          uploadedRCFile: saved.uploadedRCFile || null,
          vehicleRc: saved.uploadedRCFile ? { name: t("PROFILE_FILE_UPLOADED") } : null,
          fileError: null,
        },
      ];
    }
    return [createBlankVehicle()];
  });

  // Keyed by stable id — safe against mid-upload deletes
  const updateVehicleById = (id, patch) => {
    if (isViewMode) return;
    setVehicleList((prev) => prev.map((v) => (v.id === id ? { ...v, ...patch } : v)));
  };

  // Add more vehicle (Max 3)
  const handleAddVehicle = () => {
    if (isViewMode || vehicleList.length >= 3) return;
    setVehicleList((prev) => [...prev, createBlankVehicle()]);
  };

  // Remove vehicle (Vehicle 1 cannot be removed if hasVehicle === Yes)
  const handleRemoveVehicle = (id) => {
    if (isViewMode || vehicleList.length <= 1) return;
    setVehicleList((prev) => prev.filter((v) => v.id !== id));
  };

  const handleVehicleNoChange = (id, e) => {
    const val = e.target.value.toUpperCase();
    if (val.length > 13) return;
    const isDuplicate = vehicleList.some((v) => v.id !== id && v.vehicleNo === val && val !== "");
    updateVehicleById(id, {
      vehicleNo: val,
      vehicleNoValidated: false,
      vehicleNoValidateStatus: null,
      vehicleNoError: isDuplicate
        ? t("PROFILE_DUPLICATE_VEHICLE_NO", "This vehicle number is already added")
        : val && !VEHICLE_NO_REGEX.test(val)
        ? t("PROFILE_INVALID_VEHICLE_NO")
        : null,
    });
  };


  const handleValidateVehicleNo = (veh) => {
    if (!veh?.vehicleNo) return;
    if (VEHICLE_NO_REGEX.test(veh.vehicleNo)) {
      updateVehicleById(veh.id, { vehicleNoValidated: true, vehicleNoValidateStatus: "success", vehicleNoError: null });
    } else {
      updateVehicleById(veh.id, { vehicleNoValidated: false, vehicleNoValidateStatus: "error", vehicleNoError: t("PROFILE_INVALID_VEHICLE_NO") });
    }
  };

  // id captured at call time — index shift during upload cannot corrupt wrong vehicle
  const handleFileSelect = (id, e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (file.size >= 5242880) {
      updateVehicleById(id, { fileError: t("CS_MAXIMUM_UPLOAD_SIZE_EXCEEDED") });
      return;
    }
    updateVehicleById(id, { fileError: null, vehicleRc: file });
    (async () => {
      try {
        const response = await Digit.UploadServices.Filestorage("profile", file, Digit.ULBService.getStateId());
        if (response?.data?.files?.length > 0) {
          updateVehicleById(id, { uploadedRCFile: response.data.files[0].fileStoreId });
        } else {
          updateVehicleById(id, { fileError: t("CS_FILE_UPLOAD_ERROR") });
        }
      } catch {
        updateVehicleById(id, { fileError: t("CS_FILE_UPLOAD_ERROR") });
      }
    })();
  };

  const isVehicleComplete = (veh) => veh.vehicleType && veh.vehicleCategory && veh.fuelType && veh.vehicleNo && veh.vehicleNoValidated && veh.purchaseYear && veh.uploadedRCFile;

  // To check if user dows not enter duplicate number
  const hasDuplicateVehicleNo = () => {
    const filled = vehicleList.map((v) => v.vehicleNo).filter(Boolean);
    return filled.length !== new Set(filled).size;
  };


  const isDisabled = hasVehicle === null ? true : hasVehicle === "No" ? false : vehicleList.length === 0 || !vehicleList.every(isVehicleComplete) || hasDuplicateVehicleNo();;

  const goNext = () => {
    if (isViewMode) {
      onSelect(config.key, saved, false);
      return;
    }
    if (hasVehicle === "No") {
      onSelect(config.key, { hasVehicle: "No", vehicleList: [] }, false);
      return;
    }

    const firstVeh = vehicleList[0] || {};
    onSelect(
      config.key,
      {
        hasVehicle: "Yes",
        vehicleList,
        // Backward-compatible fallback properties mapping to first vehicle
        vehicleNo: firstVeh.vehicleNo,
        purchaseYear: firstVeh.purchaseYear,
        uploadedRCFile: firstVeh.uploadedRCFile,
        vehicleRc: firstVeh.vehicleRc,
        vehicleType: firstVeh.vehicleType,
        vehicleCategory: firstVeh.vehicleCategory,
        fuelType: firstVeh.fuelType,
        vehicleNoValidated: firstVeh.vehicleNoValidated,
      },
      false,
    );
  };

  const onSkip = () => onSelect();

  return (
    <React.Fragment>
      <FamilyCreateTabs t={t} />
      <FormStep
        config={{
          ...config,
          texts: {
            ...config?.texts,
            submitBarLabel: isViewMode ? "COMMON_NEXT" : config?.texts?.submitBarLabel || "COMMON_SAVE_NEXT",
          },
        }}
        onSelect={goNext}
        onSkip={onSkip}
        t={t}
        isDisabled={isViewMode ? false : isDisabled}
      >
        <FormField label={t("PROFILE_DO_YOU_HAVE_VEHICLE")} required t={t} />
        <RadioButtons
          options={[
            { code: "Yes", i18nKey: "COMMON_YES" },
            { code: "No", i18nKey: "COMMON_NO" },
          ]}
          optionsKey="i18nKey"
          isDependent={true}
          selectedOption={hasVehicle ? { code: hasVehicle } : null}
          onSelect={(val) => !isViewMode && setHasVehicle(val.code)}
          disabled={isViewMode}
          name="hasVehicle"
        />

        {/* Vehicle Cards — rendered only when hasVehicle === "Yes" */}
        {hasVehicle === "Yes" && (
          <div style={profileStyles.vehicleDetails.listWrapper}>
            {vehicleList.map((veh, index) => {
              const isValidateBtnDisabled = !veh.vehicleNo || !!veh.vehicleNoError;
              return (
                <div key={veh.id} style={profileStyles.vehicleDetails.card}>
                  <div style={profileStyles.vehicleDetails.cardHeader}>
                    <div style={profileStyles.vehicleDetails.cardTitle}>
                     {t("PROFILE_VEHICLE")} {index + 1}
                    </div>
                    {vehicleList.length > 1 && !isViewMode && (
                      <button
                        type="button"
                        onClick={() => handleRemoveVehicle(veh.id)}
                        style={profileStyles.vehicleDetails.deleteBtn}
                        title={t("REMOVE")}
                      >
                        <DeleteIcon fill="#a82227" style={{ width: "20px", height: "20px" }} />
                      </button>
                    )}
                  </div>

                  {/* 6 Vehicle Fields Grid */}
                  <div style={profileStyles.gridStyle}>
                    <FormField label={t("PROFILE_VEHICLE_TYPE")} required t={t}>
                      <Dropdown
                        selected={veh.vehicleType}
                        select={(val) => updateVehicleById(veh.id, { vehicleType: val })}
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
                        selected={veh.vehicleCategory}
                        select={(val) => updateVehicleById(veh.id, { vehicleCategory: val })}
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
                        selected={veh.fuelType}
                        select={(val) => updateVehicleById(veh.id, { fuelType: val })}
                        option={FUEL_TYPE}
                        optionKey="i18nKey"
                        t={t}
                        style={fullWidth}
                        placeholder={t("PROFILE_SELECT_FUEL_CATEGORY")}
                        disable={isViewMode}
                      />
                    </FormField>

                    <FormField label={t("PROFILE_VEHICLE_NO")} required t={t} error={veh.vehicleNoError}>
                      <div style={profileStyles.taxIdInputRow}>
                        <div style={profileStyles.taxIdInputWrap}>
                          <TextInput
                            t={t}
                            type="text"
                            name={`vehicleNo_${veh.id}`}
                            value={veh.vehicleNo}
                            onChange={(e) => handleVehicleNoChange(veh.id, e)}
                            placeholder="MH12AB1234 / 22BH1234AB"
                            maxLength={13}
                            style={fullWidth}
                            disable={isViewMode}
                          />
                        </div>
                        <button
                          type="button"
                          onClick={() => handleValidateVehicleNo(veh)}
                          disabled={isViewMode || isValidateBtnDisabled}
                          style={{
                            ...profileStyles.validateBtn,
                            ...(veh.vehicleNoValidated ? profileStyles.validateBtnSuccess : {}),
                            ...(isViewMode || isValidateBtnDisabled ? profileStyles.validateBtnDisabled : {}),
                          }}
                        >
                          {veh.vehicleNoValidated ? `✓ ${t("PROFILE_VALIDATED")}` : t("PROFILE_VALIDATE")}
                        </button>
                      </div>
                      {veh.vehicleNoValidateStatus === "success" && (
                        <p style={profileStyles.validateSuccessMsg}>{t("PROFILE_VEHICLE_NO_VALID")}</p>
                      )}
                    </FormField>

                    <FormField label={t("PROFILE_VEHICLE_PURCHASE_YEAR")} required t={t}>
                      <DatePicker
                        date={veh.purchaseYear}
                        onChange={(val) => updateVehicleById(veh.id, { purchaseYear: val })}
                        max={new Date().toISOString().split("T")[0]}
                        disabled={isViewMode}
                      />
                    </FormField>

                    <FormField label={t("PROFILE_VEHICLE_RC")} required t={t} error={veh.fileError}>
                      <UploadFile
                        onUpload={(e) => handleFileSelect(veh.id, e)}
                        onDelete={isViewMode ? undefined : () => updateVehicleById(veh.id, { vehicleRc: null, uploadedRCFile: null, fileError: null })}
                        message={veh.vehicleRc ? veh.vehicleRc.name : t("PROFILE_NO_FILE_SELECTED")}
                        accept=".pdf,.jpg,.jpeg,.png"
                        iserror={veh.fileError}
                        disabled={isViewMode}
                      />
                      <p style={profileStyles.documentUploadCard.fileRestrictionText}>{t("CS_FILE_SIZE_RESTRICTIONS")}</p>
                    </FormField>
                  </div>
                </div>
              );
            })}

            {/* Add Another Vehicle Button (Visible only when vehicleList < 3) */}
            {vehicleList.length < 3 && !isViewMode && (
              <div style={profileStyles.vehicleDetails.addVehicleWrapper}>
                <button
                  type="button"
                  onClick={handleAddVehicle}
                  style={{...profileStyles.validateBtn}}
                >
                  <span>{t("PROFILE_ADD_ANOTHER_VEHICLE")}</span>
                  <span style={profileStyles.vehicleDetails.vehcileMaxNumber}>({vehicleList.length}/3)</span>
                </button>
              </div>
            )}
          </div>
        )}
      </FormStep>
    </React.Fragment>
  );
};

export default VehicleDetails;
