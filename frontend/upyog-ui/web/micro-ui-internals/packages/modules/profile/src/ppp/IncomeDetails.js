import React, { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { FormStep, Dropdown, TextInput, UploadFile } from "@nudmcdgnpm/digit-ui-react-components";
import { TAX_VALIDATORS, FormField } from "../utils";
import FamilyCreateTabs from "../config/FamilyCreateTabs";
import DocumentUploadCard from "../utils/DocumentUploadCard";

/// TODO - Need to Add all these in MDMS
const TAX_ID_OPTIONS = [
  { i18nKey: "PROFILE_PAN_NUMBER", code: "PAN" },
  { i18nKey: "PROFILE_TAN_NUMBER", code: "TAN" },
  { i18nKey: "PROFILE_GST_NUMBER", code: "GST" },
];

const INCOME_PROOF_OPTIONS = [
  { i18nKey: "PROFILE_INCOME_PROOF_SALARY_SLIP", code: "SALARY_SLIP", name: "Salary Slip" },
  { i18nKey: "PROFILE_INCOME_PROOF_ITR", code: "ITR", name: "ITR" },
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
  const [searchParams] = useSearchParams();
  const isViewMode = searchParams.get("mode") === "view" || !!config?.isViewMode;
  const saved = formData?.[config?.key] || {};
  const [taxIdType, setTaxIdType] = useState(saved.taxIdType || null);
  const [taxIdNumber, setTaxIdNumber] = useState(saved.taxIdNumber || "");
  const [annualIncome, setAnnualIncome] = useState(saved.annualIncome || "");
  const [occupation, setOccupation] = useState(saved.occupation || null);
  const [taxIdError, setTaxIdError] = useState(null);
  const [taxProof, setTaxProof] = useState(saved.uploadedTaxProof ? { name: t("PROFILE_FILE_UPLOADED") } : null);
  const [uploadedTaxProof, setUploadedTaxProof] = useState(saved.uploadedTaxProof || null);
  const [taxProofError, setTaxProofError] = useState(null);
  const [taxIdValidated, setTaxIdValidated] = useState(saved.taxIdValidated || false);
  const [validateStatus, setValidateStatus] = useState(null);
  const [incomeProofType, setIncomeProofType] = useState(saved.incomeProofType || null);
  const [incomeProofFile, setIncomeProofFile] = useState(saved.incomeProofFile || null);

  // Hook Call to get the Occupation from MDMS
  const { data: Occupations } = Digit.Hooks.useEnabledMDMS(Digit.ULBService.getStateId(), "common-masters", [{ name: "Occupation" }],
    {
      select: (data) => {
        const formattedData = data?.["common-masters"]?.["Occupation"]
        return formattedData;
      },
    });
  let OCCUPATION_OPTIONS = []; // Empty array which will store the options of Occupations

  Occupations &&
      Occupations.map((occupations) => {
        OCCUPATION_OPTIONS.push({ i18nKey: `PROFILE_OCCUPATION_${occupations.code}`, code: `${occupations.code}`, value: `${occupations.name}` });
      });

  const goNext = () => {
    if (isViewMode) {
      onSelect(config.key, saved, false);
      return;
    }
   onSelect(config.key, {
        occupation,
        annualIncome,
        taxIdType,
        taxIdNumber,
        taxIdValidated,
        uploadedTaxProof,
        incomeProofType,                                    
        incomeProofFile,                                   
        uploadedIncomeFile: incomeProofFile?.fileStoreId || null,
    }, false);
  };

  const onSkip = () => onSelect();

  useEffect(() => {
    if (userType === "citizen" && !isViewMode) goNext();
  }, [taxIdType, taxIdNumber, taxIdValidated, annualIncome, occupation, incomeProofFile, uploadedTaxProof]);

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

  // When Salariedd only Pan and GST will render
  const filteredTaxIdOptions = occupation?.code === "SALARIED"
  ? TAX_ID_OPTIONS.filter((opt) => opt.code !== "TAN")
  : TAX_ID_OPTIONS;

  const handleOccupationChange = (val) => {
  setOccupation(val);
  if (val?.code === "SALARIED" && taxIdType?.code === "TAN") {
    setTaxIdType(null);
    setTaxIdNumber("");
    setTaxIdValidated(false);
    setValidateStatus(null);
    setTaxIdError(null);
  }
};


  const isValidateBtnDisabled = !taxIdNumber || !!taxIdError;
  const showTaxProof = validateStatus === "error";
  const taxIdSatisfied = !taxIdType || taxIdValidated || !!uploadedTaxProof;
  const isDisabled = !annualIncome || !occupation || !incomeProofType || !incomeProofFile?.fileStoreId || !!taxIdError || (taxIdType && !taxIdNumber) || !taxIdSatisfied;

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
        <div className="profile-grid-3col">
          <FormField label={t("PROFILE_OCCUPATION")} required t={t}>
            <Dropdown
              selected={occupation}
              select={handleOccupationChange}
              option={OCCUPATION_OPTIONS}
              optionKey="i18nKey"
              t={t}
              style={fullWidth}
              placeholder={t("PROFILE_SELECT_OCCUPATION")}
              disable={isViewMode}
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
              disable={isViewMode}
            />
          </FormField>

          <FormField label={t("PROFILE_TAX_ID_TYPE")} required t={t}>
            <Dropdown
              selected={taxIdType}
              select={handleTaxIdTypeChange}
              option={filteredTaxIdOptions}
              optionKey="i18nKey"
              t={t}
              style={fullWidth}
              placeholder={t("PROFILE_SELECT_TAX_ID_TYPE")}
              disable={isViewMode}
            />
          </FormField>

          {taxIdType && (
            <FormField label={t(taxIdType.i18nKey)} required error={taxIdError} t={t}>
              <div className="income-details-tax-row">
                <div className="income-details-tax-wrap">
                  <TextInput
                    t={t}
                    type="text"
                    name="taxIdNumber"
                    value={taxIdNumber}
                    onChange={handleTaxIdNumberChange}
                    placeholder={TAX_VALIDATORS[taxIdType.code]?.placeholder}
                    maxLength={TAX_VALIDATORS[taxIdType.code]?.maxLength}
                    style={fullWidth}
                    disable={isViewMode}
                  />
                </div>
                <button
                  type="button"
                  onClick={handleValidateTaxId}
                  disabled={isViewMode || isValidateBtnDisabled}
                  className={`income-details-validate-btn ${taxIdValidated ? "income-details-validate-btn-success" : ""} ${
                    isViewMode || isValidateBtnDisabled ? "income-details-validate-btn-disabled" : ""
                  }`}
                >
                  {taxIdValidated ? `✓ ${t("PROFILE_VALIDATED")}` : t("PROFILE_VALIDATE")}
                </button>
              </div>
              {validateStatus === "success" && (
                <p className="income-details-validate-success-msg">{t("PROFILE_TAX_ID_VALID")}</p>
              )}
            </FormField>
          )}

          {showTaxProof && (
            <FormField label={t("PROFILE_TAX_PROOF")} required t={t} error={taxProofError}>
              <UploadFile
                onUpload={handleTaxProofSelect}
                onDelete={isViewMode ? undefined : () => {
                  setTaxProof(null);
                  setUploadedTaxProof(null);
                  setTaxProofError(null);
                }}
                message={taxProof ? taxProof.name : t("PROFILE_NO_FILE_SELECTED")}
                accept=".pdf,.jpg,.jpeg,.png"
                iserror={taxProofError}
                disabled={isViewMode}
              />
              <p className="document-upload-file-restriction">
                {t("CS_FILE_SIZE_RESTRICTIONS")}
              </p>
            </FormField>
          )}

          <DocumentUploadCard
            label={t("PROFILE_INCOME_PROOF")}
            required
            options={INCOME_PROOF_OPTIONS}
            selectedType={incomeProofType}
            onSelectType={(v) => setIncomeProofType(v)}
            typePlaceholder={t("PROFILE_SELECT_PROOF_TYPE")}
            file={incomeProofFile}
            onFileChange={(f) => setIncomeProofFile(f)}
            t={t}
            stateId={Digit.ULBService.getStateId()}
            disabled={isViewMode}
          />
        </div>
      </FormStep>
    </React.Fragment>
  );
};

export default IncomeDetails;
