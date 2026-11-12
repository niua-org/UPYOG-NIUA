import React, { useEffect, useState } from "react";
import { FormStep, Dropdown, TextInput, UploadFile, DatePicker } from "@nudmcdgnpm/digit-ui-react-components";
import { FormField } from "../utils";
import { profileStyles } from "../utils/profileStyles";
import FamilyCreateTabs from "../config/FamilyCreateTabs";

/**
 * Vehicle Details Page Developed to get the Vehicle details of the family Members
 * These Details Will be filled by the Family Head
 * Head should have to provide below details :-
 *  1. Vehicle RC 
 *  2. Vehicle Number
 *  3. Buying Year of Vehicle
 */

const VEHICLE_NO_REGEX = /^[A-Z]{2}[0-9]{1,2}[A-Z]{1,3}[0-9]{4}$/;
const fullWidth = { width: "100%" };

const VehicleDetails = ({ t, config, onSelect, userType, formData }) => {
  const saved = formData?.[config?.key] || {};
  const [vehicleRc, setVehicleRc] = useState(saved.vehicleRc || null);
  const [purchaseYear, setPurchaseYear] = useState(saved.purchaseYear || "");
  const [vehicleNo, setVehicleNo] = useState(saved.vehicleNo || "");
  const [fileError, setFileError] = useState(null);
  const [uploadedFile, setUploadedFile] = useState(saved.uploadedFile || null);
  const [vehicleNoError, setVehicleNoError] = useState(null);

  const goNext = () => {
    onSelect(config.key, { vehicleRc, uploadedFile, purchaseYear, vehicleNo }, false);
  };

  const onSkip = () => onSelect();

  useEffect(() => {
    if (userType === "citizen") goNext();
  }, [vehicleNo, purchaseYear, uploadedFile]);

  const handleVehicleNoChange = (e) => {
    const val = e.target.value.toUpperCase();
    if (val.length > 10) return;
    setVehicleNo(val);
    setVehicleNoError(val && !VEHICLE_NO_REGEX.test(val) ? t("PROFILE_INVALID_VEHICLE_NO") : null);
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
        if (response?.data?.files?.length > 0) setUploadedFile(response.data.files[0].fileStoreId);
        else setFileError(t("CS_FILE_UPLOAD_ERROR"));
      } catch {
        setFileError(t("CS_FILE_UPLOAD_ERROR"));
      }
    })();
  };

  const isDisabled = !vehicleRc || !vehicleNo || !purchaseYear || !!vehicleNoError;

  return (
    <React.Fragment>
      <FamilyCreateTabs t={t} />
      <FormStep config={config} onSelect={goNext} onSkip={onSkip} t={t} isDisabled={isDisabled}>
        <div style={profileStyles.gridStyle}>
          <FormField label={t("PROFILE_VEHICLE_NO")} required t={t} error={vehicleNoError}>
          <TextInput
            t={t}
            type="text"
            name="vehicleNo"
            value={vehicleNo}
            onChange={handleVehicleNoChange}
            placeholder="MH12AB1234"
            maxLength={10}
            style={fullWidth}
          />
        </FormField>

        <FormField label={t("PROFILE_VEHICLE_PURCHASE_YEAR")} required t={t}>
          <DatePicker
            date={purchaseYear}
            onChange={setPurchaseYear}
            max={new Date().toISOString().split("T")[0]}
          />
        </FormField>

      <FormField label={t("PROFILE_VEHICLE_RC")} required t={t} error={fileError}>
        <UploadFile
          onUpload={handleFileSelect}
          onDelete={() => {
            setVehicleRc(null);
            setUploadedFile(null);
            setFileError(null);
          }}
          message={vehicleRc ? vehicleRc.name : t("PROFILE_NO_FILE_SELECTED")}
          accept=".pdf,.jpg,.jpeg,.png"
          iserror={fileError}
        />
      </FormField>
        </div>
      </FormStep>
    </React.Fragment>
  );
};

export default VehicleDetails;
