import React, { useEffect, useState } from "react";
import { FormStep, Dropdown, TextInput, UploadFile } from "@nudmcdgnpm/digit-ui-react-components";
import { TAX_VALIDATORS, FormField } from "../utils";
import { profileStyles } from "../utils/profileStyles";
import FamilyCreateTabs from "../config/FamilyCreateTabs";

/// TODO - Need to Add all these in MDMS
const TAX_ID_OPTIONS = [
  { i18nKey: "PROFILE_PAN_NUMBER", code: "PAN" },
  { i18nKey: "PROFILE_TAN_NUMBER", code: "TAN" },
  { i18nKey: "PROFILE_GST_NUMBER", code: "GST" },
];

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

const fullWidth = { width: "100%" };

const IncomeDetails = ({ t, config, onSelect, userType, formData }) => {
  const saved = formData?.[config?.key] || {};
  const [incomeProof, setIncomeProof] = useState(saved.incomeProof || null);
  const [taxIdType, setTaxIdType] = useState(saved.taxIdType || null);
  const [taxIdNumber, setTaxIdNumber] = useState(saved.taxIdNumber || "");
  const [annualIncome, setAnnualIncome] = useState(saved.annualIncome || "");
  const [occupation, setOccupation] = useState(saved.occupation || null);
  const [fileError, setFileError] = useState(null);
  const [uploadedIncomeFile, setUploadedIncomeFile] = useState(saved.uploadedIncomeFile || null);
  const [taxIdError, setTaxIdError] = useState(null);
  const [taxProof, setTaxProof] = useState(saved.taxProof || null);
  const [uploadedTaxProof, setUploadedTaxProof] = useState(saved.uploadedTaxProof || null);
  const [taxProofError, setTaxProofError] = useState(null);
  const [taxIdValidated, setTaxIdValidated] = useState(saved.taxIdValidated || false);
  const [validateStatus, setValidateStatus] = useState(null);

  const goNext = () => {
    onSelect(
      config.key,
      {
        incomeProof,
        uploadedIncomeFile,
        taxIdType,
        taxIdNumber,
        taxIdValidated,
        taxProof,
        uploadedTaxProof,
        annualIncome,
        occupation,
      },
      false,
    );
  };

  const onSkip = () => onSelect();

  useEffect(() => {
    if (userType === "citizen") goNext();
  }, [taxIdType, taxIdNumber, taxIdValidated, annualIncome, occupation, uploadedIncomeFile, uploadedTaxProof]);

  const handleTaxIdNumberChange = (e) => {
    const val = e.target.value.toUpperCase();
    const maxLen = TAX_VALIDATORS[taxIdType?.code]?.maxLength;
    if (maxLen && val.length > maxLen) return;
    setTaxIdNumber(val);
    setTaxIdValidated(false);
    setValidateStatus(null);
    const isValid = TAX_VALIDATORS[taxIdType?.code]?.pattern.test(val);
    setTaxIdError(val && !isValid ? t("PROFILE_INVALID_TAX_ID") : null);
  };

  const handleTaxIdTypeChange = (val) => {
    setTaxIdType(val);
    setTaxIdNumber("");
    setTaxIdValidated(false);
    setValidateStatus(null);
    setTaxIdError(null);
  };

  const handleValidateTaxId = () => {
    if (!taxIdNumber || !taxIdType) return;
    const isValid = TAX_VALIDATORS[taxIdType.code]?.pattern.test(taxIdNumber);
    if (isValid) {
      setTaxIdValidated(true);
      setValidateStatus("success");
      setTaxIdError(null);
      setTaxProof(null);
      setUploadedTaxProof(null);
      setTaxProofError(null);
    } else {
      setTaxIdValidated(false);
      setValidateStatus("error");
      setTaxIdError(t("PROFILE_INVALID_TAX_ID"));
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
    setIncomeProof(file);
    (async () => {
      try {
        const response = await Digit.UploadServices.Filestorage("profile", file, Digit.ULBService.getStateId());
        if (response?.data?.files?.length > 0) setUploadedIncomeFile(response.data.files[0].fileStoreId);
        else setFileError(t("CS_FILE_UPLOAD_ERROR"));
      } catch {
        setFileError(t("CS_FILE_UPLOAD_ERROR"));
      }
    })();
  };

  const handleTaxProofSelect = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    if (file.size >= 5242880) {
      setTaxProofError(t("CS_MAXIMUM_UPLOAD_SIZE_EXCEEDED"));
      return;
    }
    setTaxProofError(null);
    setTaxProof(file);
    (async () => {
      try {
        const response = await Digit.UploadServices.Filestorage("profile", file, Digit.ULBService.getStateId());
        if (response?.data?.files?.length > 0) setUploadedTaxProof(response.data.files[0].fileStoreId);
        else setTaxProofError(t("CS_FILE_UPLOAD_ERROR"));
      } catch {
        setTaxProofError(t("CS_FILE_UPLOAD_ERROR"));
      }
    })();
  };

  const isValidateBtnDisabled = !taxIdNumber || !!taxIdError;
  const showTaxProof = validateStatus === "error";
  const taxIdSatisfied = !taxIdType || taxIdValidated || !!uploadedTaxProof;
  const isDisabled = !annualIncome || !occupation || !uploadedIncomeFile || !!taxIdError || (taxIdType && !taxIdNumber) || !taxIdSatisfied;

  return (
    <React.Fragment>
      <FamilyCreateTabs t={t} />

      <FormStep config={config} onSelect={goNext} onSkip={onSkip} t={t} isDisabled={isDisabled}>
        <div style={profileStyles.gridStyle}>
          <FormField label={t("PROFILE_OCCUPATION")} required t={t}>
            <Dropdown
              selected={occupation}
              select={setOccupation}
              option={OCCUPATION_OPTIONS}
              optionKey="i18nKey"
              t={t}
              style={fullWidth}
              placeholder={t("PROFILE_SELECT_OCCUPATION")}
            />
          </FormField>

          <FormField label={t("PROFILE_ANNUAL_INCOME")} required t={t}>
            <TextInput
              t={t}
              type="number"
              name="annualIncome"
              value={annualIncome}
              onChange={(e) => setAnnualIncome(e.target.value)}
              placeholder="0"
              style={fullWidth}
            />
          </FormField>

          <FormField label={t("PROFILE_TAX_ID_TYPE")} required t={t}>
            <Dropdown
              selected={taxIdType}
              select={handleTaxIdTypeChange}
              option={TAX_ID_OPTIONS}
              optionKey="i18nKey"
              t={t}
              style={fullWidth}
              placeholder={t("PROFILE_SELECT_TAX_ID_TYPE")}
            />
          </FormField>

          {taxIdType && (
            <FormField label={t(taxIdType.i18nKey)} required error={taxIdError} t={t}>
              <div style={profileStyles.taxIdInputRow}>
                <div style={profileStyles.taxIdInputWrap}>
                  <TextInput
                    t={t}
                    type="text"
                    name="taxIdNumber"
                    value={taxIdNumber}
                    onChange={handleTaxIdNumberChange}
                    placeholder={TAX_VALIDATORS[taxIdType.code]?.placeholder}
                    maxLength={TAX_VALIDATORS[taxIdType.code]?.maxLength}
                    style={fullWidth}
                  />
                </div>
                <button
                  type="button"
                  onClick={handleValidateTaxId}
                  disabled={isValidateBtnDisabled}
                  style={{
                    ...profileStyles.validateBtn,
                    ...(taxIdValidated ? profileStyles.validateBtnSuccess : {}),
                    ...(isValidateBtnDisabled ? profileStyles.validateBtnDisabled : {}),
                  }}
                >
                  {taxIdValidated ? `✓ ${t("PROFILE_VALIDATED")}` : t("PROFILE_VALIDATE")}
                </button>
              </div>
              {validateStatus === "success" && (
                <p style={profileStyles.validateSuccessMsg}>{t("PROFILE_TAX_ID_VALID")}</p>
              )}
            </FormField>
          )}

          {showTaxProof && (
            <FormField label={t("PROFILE_TAX_PROOF")} required t={t} error={taxProofError}>
              <UploadFile
                onUpload={handleTaxProofSelect}
                onDelete={() => {
                  setTaxProof(null);
                  setUploadedTaxProof(null);
                  setTaxProofError(null);
                }}
                message={taxProof ? taxProof.name : t("PROFILE_NO_FILE_SELECTED")}
                accept=".pdf,.jpg,.jpeg,.png"
                iserror={taxProofError}
              />
            </FormField>
          )}

          <FormField label={t("PROFILE_INCOME_PROOF")} required t={t} error={fileError}>
            <UploadFile
              onUpload={handleFileSelect}
              onDelete={() => {
                setIncomeProof(null);
                setUploadedIncomeFile(null);
                setFileError(null);
              }}
              message={incomeProof ? incomeProof.name : t("PROFILE_NO_FILE_SELECTED")}
              accept=".pdf,.jpg,.jpeg,.png"
              iserror={fileError}
            />
          </FormField>
        </div>
      </FormStep>
    </React.Fragment>
  );
};

export default IncomeDetails;
