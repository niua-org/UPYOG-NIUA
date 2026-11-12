import React, { useEffect, useState } from "react";
import { FormStep, CardLabel, Dropdown, TextInput, UploadFile, CardLabelError } from "@nudmcdgnpm/digit-ui-react-components";
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

const VehicleDetails = ({ t, config, onSelect, userType, formData }) => {
  const saved = formData?.[config?.key] || {};
  const [vehicleRc, setVehicleRc] = useState(saved.vehicleRc || null);
  const [buyingYear, setBuyingYear] = useState(saved.buyingYear || "");
  const [vehicleNo, setVehicleNo] = useState(saved.vehicleNo || "");
  const [fileError, setFileError] = useState(null);
  const [uploadedFile, setUploadedFile] = useState(saved.uploadedFile || null);
  const [vehicleNoError, setVehicleNoError] = useState(null);

  const goNext = () => {
    onSelect(config.key, { vehicleRc, uploadedFile, buyingYear, vehicleNo }, false);
  };

  const onSkip = () => onSelect();

  // auto-save on field change for citizen (same pattern as PTRCitizenDetails)
  useEffect(() => {
    if (userType === "citizen") goNext();
  }, [vehicleNo, buyingYear, uploadedFile]);


  const handleVehicleNoChange = (e) => {
    const val = e.target.value.toUpperCase();
    if (val.length > 10) return;  // ← add karo
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
    // upload to filestore (same pattern as PTRSelectProofIdentity)
    (async () => {
      try {
        const response = await Digit.UploadServices.Filestorage("profile", file, Digit.ULBService.getStateId());
        if (response?.data?.files?.length > 0) {
          setUploadedFile(response.data.files[0].fileStoreId);
        } else {
          setFileError(t("CS_FILE_UPLOAD_ERROR"));
        }
      } catch {
        setFileError(t("CS_FILE_UPLOAD_ERROR"));
      }
    })();
  };

  const isDisabled = !vehicleRc || !vehicleNo || !buyingYear || !!vehicleNoError

  return (
    <React.Fragment>
    <div>
      <FamilyCreateTabs t={t} />
    </div>
      <FormStep config={config} onSelect={goNext} onSkip={onSkip} t={t} isDisabled={isDisabled}>
        <div style={profileStyles.componentBottomMargin}>
        
        <CardLabel>{t("PROFILE_VEHICLE_NO")} <span className="astericColor">*</span></CardLabel>
        <TextInput
            t={t}
            type="text"
            name="vehicleNo"
            value={vehicleNo}
            onChange={handleVehicleNoChange}
            placeholder="MH12AB1234"
            maxLength={10}
            />
        {vehicleNoError && <CardLabelError>{vehicleNoError}</CardLabelError>}

        <CardLabel>{t("PROFILE_VEHICLE_YEAR")} <span className="astericColor">*</span></CardLabel>
        <TextInput
            t={t}
            type="text"
            name="buyingYear"
            value={buyingYear}
            onChange={(e) => setBuyingYear(e.target.value)}
            placeholder="PROFILE_VEHICLE_BUYING_YEAR_PLACEHOLDER"
            maxLength={4}
            ValidationRequired={true}
            {...{
            isRequired: true,
            pattern: "",
            type: "text",
            title: t("PROFILE_INVALID_YEAR"),
            }}
        />

        <CardLabel>{t("PROFILE_VEHICLE_RC")} <span className="astericColor">*</span></CardLabel>
        <UploadFile
          onUpload={handleFileSelect}
          onDelete={() => {
            setVehicleRc(null);
            setUploadedFile(null);
            setFileError(null);
          }}
          message={vehicleRc ? vehicleRc.name : t("PROFILE_NO_FILE_SELECTED")}
          accept=".pdf,.jpg,.jpeg,.png"
          error={fileError}
        />
        </div>
      </FormStep>
    </React.Fragment>
  );
};

export default VehicleDetails;
