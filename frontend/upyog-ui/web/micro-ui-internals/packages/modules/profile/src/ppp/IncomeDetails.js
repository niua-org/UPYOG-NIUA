import React, { useEffect, useState } from "react";
import { FormStep, CardLabel, Dropdown, TextInput, UploadFile, CardLabelError } from "@nudmcdgnpm/digit-ui-react-components";
import { TAX_VALIDATORS } from "../utils";
import { profileStyles } from "../utils/profileStyles";
import FamilyCreateTabs from "../config/FamilyCreateTabs";

// TODO: Add to MDMS
const TAX_ID_OPTIONS = [
  { i18nKey: "PROFILE_PAN_NUMBER", code: "PAN" },
  { i18nKey: "PROFILE_TAN_NUMBER", code: "TAN" },
  { i18nKey: "PROFILE_GST_NUMBER", code: "GST" },
];

// TODO: Add to MDMS
const OCCUPATION_OPTIONS = [
  { i18nKey: "PROFILE_OCCUPATION_SALARIED", code: "SALARIED" },
  { i18nKey: "PROFILE_OCCUPATION_SELF_EMPLOYED", code: "SELF_EMPLOYED" },
  { i18nKey: "PROFILE_OCCUPATION_BUSINESS", code: "BUSINESS" },
  { i18nKey: "PROFILE_OCCUPATION_FARMER", code: "FARMER" },
  { i18nKey: "PROFILE_OCCUPATION_UNEMPLOYED", code: "UNEMPLOYED" },
  { i18nKey: "PROFILE_OCCUPATION_OTHER", code: "OTHER" },
];

/**
 * Income Details Page Developed to get the Income details of the family Members
 * These Details Will be filled by the Family Head
 * Head should have to provide below details :-
 *  1. Inome Proof
 *  2. Tax Id Type
 *  3. Tax Id Number
 *  4. Annual Income
 *  5. Occupation
 */

const IncomeDetails = ({ t, config, onSelect, userType, formData }) => {
  const saved = formData?.[config?.key] || {};
  const [incomeProof, setIncomeProof] = useState(saved.incomeProof || null);
  const [taxIdType, setTaxIdType] = useState(saved.taxIdType || null);
  const [taxIdNumber, setTaxIdNumber] = useState(saved.taxIdNumber || "");
  const [annualIncome, setAnnualIncome] = useState(saved.annualIncome || "");
  const [occupation, setOccupation] = useState(saved.occupation || null);
  const [fileError, setFileError] = useState(null);
  const [uploadedFile, setUploadedFile] = useState(saved.uploadedFile || null);
  const [taxIdError, setTaxIdError] = useState(null);


  const goNext = () => {
    onSelect(config.key, { incomeProof, uploadedFile, taxIdType, taxIdNumber, annualIncome, occupation }, false);
  };

  const onSkip = () => onSelect();

  // auto-save on field change for citizen (same pattern as PTRCitizenDetails)
  useEffect(() => {
    if (userType === "citizen") goNext();
  }, [taxIdType, taxIdNumber, annualIncome, occupation, uploadedFile]);

  const handleTaxIdNumberChange = (e) => {
    const val = e.target.value.toUpperCase();
    const maxLen = TAX_VALIDATORS[taxIdType.code]?.maxLength;
    
    // maxLength enforce karo — agar limit se zyada ho to ignore karo
    if (maxLen && val.length > maxLen) return;

    setTaxIdNumber(val);
    const isValid = TAX_VALIDATORS[taxIdType.code].pattern.test(val);
    setTaxIdError(val && !isValid ? t("PROFILE_INVALID_TAX_ID") : null);
  };


  const handleTaxIdTypeChange = (val) => {
    setTaxIdType(val);
    setTaxIdNumber("");
  };

  const handleFileSelect = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    if (file.size >= 5242880) {
      setFileError(t("CS_MAXIMUM_UPLOAD_SIZE_EXCEEDED"));
      return;
    }
    setFileError(null);
    setIncomeProof(file);
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

  const isDisabled = !annualIncome || !occupation || !!taxIdError || (taxIdType && !taxIdNumber);


  return (
    <React.Fragment>
    <div>
      <FamilyCreateTabs t={t} />
    </div>
      <FormStep config={config} onSelect={goNext} onSkip={onSkip} t={t} isDisabled={isDisabled}>
        <div style={profileStyles.componentBottomMargin}>
        <CardLabel>{t("PROFILE_TAX_ID_TYPE")} <span className="astericColor">*</span></CardLabel>
        <Dropdown
          className="form-field"
          selected={taxIdType}
          select={handleTaxIdTypeChange}
          option={TAX_ID_OPTIONS}
          optionKey="i18nKey"
          t={t}
          placeholder={t("PROFILE_SELECT_TAX_ID_TYPE")}
        />

        {taxIdType && (
          <>
            <CardLabel>{t(taxIdType.i18nKey)} <span className="astericColor">*</span></CardLabel>
            <TextInput
              t={t}
              type="text"
              name="taxIdNumber"
              value={taxIdNumber}
              onChange={handleTaxIdNumberChange}
              placeholder={TAX_VALIDATORS[taxIdType.code]?.placeholder}
              maxLength={TAX_VALIDATORS[taxIdType.code]?.maxLength}
              ValidationRequired={true}
              {...{
                isRequired: true,
                pattern: TAX_VALIDATORS[taxIdType.code]?.pattern?.source,
                type: "text",
                title: t("PROFILE_INVALID_TAX_ID"),
              }}
            />
            {taxIdError && <CardLabelError>{taxIdError}</CardLabelError>}
          </>
        )}

        <CardLabel>{t("PROFILE_ANNUAL_INCOME")} <span className="astericColor">*</span></CardLabel>
        <TextInput
          t={t}
          type="number"
          name="annualIncome"
          value={annualIncome}
          onChange={(e) => setAnnualIncome(e.target.value)}
          placeholder="0"
          ValidationRequired={true}
          {...{
            isRequired: true,
            pattern: "^[0-9]+$",
            type: "number",
            title: t("PROFILE_ANNUAL_INCOME_INVALID"),
          }}
        />

        <CardLabel>{t("PROFILE_OCCUPATION")} <span className="astericColor">*</span></CardLabel>
        <Dropdown
          className="form-field"
          selected={occupation}
          select={setOccupation}
          option={OCCUPATION_OPTIONS}
          optionKey="i18nKey"
          t={t}
          placeholder={t("PROFILE_SELECT_OCCUPATION")}
        />
        <CardLabel>{t("PROFILE_INCOME_PROOF")} <span className="astericColor">*</span></CardLabel>
        <UploadFile
          onUpload={handleFileSelect}
          onDelete={() => {
            setIncomeProof(null);
            setUploadedFile(null);
            setFileError(null);
          }}
          message={incomeProof ? incomeProof.name : t("PROFILE_NO_FILE_SELECTED")}
          accept=".pdf,.jpg,.jpeg,.png"
          error={fileError}
        />
        </div>
      </FormStep>
    </React.Fragment>
  );
};

export default IncomeDetails;
