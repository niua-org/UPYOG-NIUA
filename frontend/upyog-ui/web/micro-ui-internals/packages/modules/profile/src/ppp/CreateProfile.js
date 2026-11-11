import React, { useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { Dropdown, FormStep, Loader, TextInput, Toast, UploadFile } from "@nudmcdgnpm/digit-ui-react-components";
import EditProfileForm from "../components/EditProfileForm";
import FamilyCreateTabs from "../config/FamilyCreateTabs";
import { FormField, useProfileUser } from "../utils";
import { profileStyles } from "../utils/profileStyles";
import { DEFAULT_CASTE, SPECIALLY_ABLED_YES } from "../utils/constants";

/**
 * CreateProfile — first step ("Profile") of the Create Family flow.
 *
 * Reuses EditProfileForm for the personal details (same UI and Save behaviour as the
 * Edit Profile page) and adds the household fields: Occupation, Caste (+ proof),
 * Specially-Abled (+ proof). The step tabs come from FamilyCreateTabs (config-driven).
 *
 * "Save & Next" (FormStep submit bar) saves the personal details, then hands the extra fields to FamilyCreate through `onSelect`, which stores
 * them in session storage and navigates to the step configured as `nextStep`.
 */

const MAX_SIZE = 5 * 1024 * 1024;
const ACCEPTED = ".jpg,.jpeg,.png,.pdf";
const ALLOWED_TYPES = ["image/jpeg", "image/png", "image/jpg", "application/pdf"];

// Proof upload: validates the file, uploads it straight to the filestore and keeps { name, fileStoreId }
const ProofUpload = ({ value, onChange, t }) => {
  const [uploadError, setUploadError] = useState(null);
  const stateId = Digit.ULBService.getStateId();

  const handleUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    if (!ALLOWED_TYPES.includes(file.type)) return setUploadError(t("PROFILE_DOC_INVALID_TYPE"));
    if (file.size > MAX_SIZE) return setUploadError(t("PROFILE_DOC_SIZE_EXCEEDED"));
    try {
      const res = await Digit.UploadServices.Filestorage("profile", file, stateId);
      const fileStoreId = res?.data?.files?.[0]?.fileStoreId;
      if (!fileStoreId) throw new Error("NO_FILESTORE_ID");
      setUploadError(null);
      onChange({ name: file.name, fileStoreId });
    } catch (err) {
      setUploadError(t("CORE_COMMON_PROFILE_FILE_UPLOAD_ERROR"));
    }
  };

  return (
    <UploadFile
      accept={ACCEPTED}
      onUpload={handleUpload}
      onDelete={() => { setUploadError(null); onChange(null); }}
      file={value}
      message={value ? t("PROFILE_DOC_UPLOADED") : t("PROFILE_NO_FILE_SELECTED")}
      iserror={uploadError}
    />
  );
};

// TODO: Add to MDMS
const casteOptions = [
  { i18nKey: "PROFILE_CASTE_GENERAL", code: "General" },
  { i18nKey: "PROFILE_CASTE_OBC", code: "OBC" },
  { i18nKey: "PROFILE_CASTE_SC", code: "SC" },
  { i18nKey: "PROFILE_CASTE_ST", code: "ST" },
];

// TODO: Add to MDMS
const speciallyAbledOptions = [
  { i18nKey: "YES", code: "Yes" },
  { i18nKey: "NO", code: "No" }
];

const CreateProfile = ({ config, onSelect, formData }) => {
  const { t } = useTranslation();
  const stateId = Digit.ULBService.getStateId();
  const { user, loading } = useProfileUser();
  const saved = formData?.[config?.key] || {};

  const [occupation, setOccupation] = useState(saved.occupation || "");
  const [caste, setCaste] = useState(saved.caste || null);
  const [casteProof, setCasteProof] = useState(saved.casteProof || null);
  const [speciallyAbled, setSpeciallyAbled] = useState(saved.speciallyAbled || null);
  const [disabilityProof, setDisabilityProof] = useState(saved.disabilityProof || null);
  const [errors, setErrors] = useState({});
  const [toast, setToast] = useState(null);
  const [saving, setSaving] = useState(false);
  const profileFormRef = useRef(null);

  // Hooks to fetch the Caste and Specially-Abled dropdown options from MDMS, keeping only active ones
  const toOptions = (list) => (list || []).filter((option) => option.active).map((option) => ({ i18nKey: `${option.code}`, code: `${option.code}`, name: `${option.name}` }));
  // const { data: casteOptions } = Digit.Hooks.useEnabledMDMS(stateId, "CasteCategory", [{ name: "CategoryOptions" }], {
  //   select: (data) => toOptions(data?.["CasteCategory"]?.["CategoryOptions"]),
  // });
  // const { data: speciallyAbledOptions } = Digit.Hooks.useEnabledMDMS(stateId, "SpeciallyAbled", [{ name: "AbledOptions" }], {
  //   select: (data) => toOptions(data?.["SpeciallyAbled"]?.["AbledOptions"]),
  // });

  const isSpeciallyAbled = speciallyAbled?.code === SPECIALLY_ABLED_YES;
  const needsCasteProof = caste && caste.code !== DEFAULT_CASTE;

  // A proof is required only when the answer actually needs one
  const validate = () => {
    const next = {
      casteProof: needsCasteProof && !casteProof ? "PROFILE_PROOF_REQUIRED" : null,
      disabilityProof: isSpeciallyAbled && !disabilityProof ? "PROFILE_PROOF_REQUIRED" : null,
    };
    setErrors(next);
    return !Object.values(next).some(Boolean);
  };

  // Save & Next: validate the extras, save the personal details, then move to the next step
  const handleNext = async () => {
    if (saving) return;
    if (!validate()) return setToast({ error: true, label: t("PROFILE_PROOF_REQUIRED") });
    setSaving(true);
    const saved = await profileFormRef.current?.save();
    setSaving(false);
    if (!saved) return;
    onSelect(config.key, {
      occupation: occupation.trim(),
      caste,
      casteProof: needsCasteProof ? casteProof : null,
      speciallyAbled,
      disabilityProof: isSpeciallyAbled ? disabilityProof : null,
    });
  };

  if (loading) return <Loader />;

  return (
    <React.Fragment>
      <FamilyCreateTabs t={t} />
      <FormStep
        config={{ ...config, texts: { ...config.texts, header: "ES_TITILE_PROFILE_DETAILS" } }}
        onSelect={handleNext}
        isDisabled={saving}
        t={t}
      >
        {/* Same personal-details UI as /profile/edit-details; its own Save is hidden, Save & Next saves it */}
        <EditProfileForm ref={profileFormRef} hideSave stateCode={stateId} userType="citizen" userDetails={user || {}} />

        <div style={profileStyles.grid}>
          <FormField label={t("PROFILE_OCCUPATION")} t={t}>
            <TextInput t={t} type="text" name="occupation" value={occupation} placeholder={t("PROFILE_OCCUPATION")} onChange={(e) => setOccupation(e.target.value)} />
          </FormField>

          <FormField label={t("PROFILE_CASTE")} t={t}>
            <Dropdown selected={caste} select={setCaste} option={casteOptions || []} optionKey="i18nKey" t={t} placeholder={t("COMMON_SELECT")} />
          </FormField>

          {needsCasteProof && (
            <FormField label={t("PROFILE_CASTE_PROOF")} required error={errors.casteProof} t={t}>
              <ProofUpload value={casteProof} onChange={setCasteProof} t={t} />
            </FormField>
          )}

          <FormField label={t("PROFILE_SPECIALLY_ABLED")} t={t}>
            <Dropdown selected={speciallyAbled} select={setSpeciallyAbled} option={speciallyAbledOptions || []} optionKey="i18nKey" t={t} placeholder={t("COMMON_SELECT")} />
          </FormField>

          {isSpeciallyAbled && (
            <FormField label={t("PROFILE_DISABILITY_PROOF")} required error={errors.disabilityProof} t={t}>
              <ProofUpload value={disabilityProof} onChange={setDisabilityProof} t={t} />
            </FormField>
          )}
        </div>
      </FormStep>

      {toast && (
        <div className="profile-toast-container">
          <Toast error={toast.error} label={toast.label} onClose={() => setToast(null)} />
        </div>
      )}
    </React.Fragment>
  );
};

export default CreateProfile;
