import React, { useRef, useState, useEffect, useMemo } from "react";
import { useTranslation } from "react-i18next";
import {
  Dropdown,
  FormStep,
  Loader,
  TextInput,
  Toast,
  DatePicker,
  MobileNumber,
  CardLabelError,
} from "@nudmcdgnpm/digit-ui-react-components";
import UploadDrawer from "../components/UploadDrawer";
import FamilyCreateTabs from "../config/FamilyCreateTabs";
import { FormField, AvatarUpload, toGenderOption, fetchThumbnail, useProfileUser } from "../utils";
import { profileStyles } from "../utils/profileStyles";
import { DEFAULT_CASTE, SPECIALLY_ABLED_YES } from "../utils/constants";
import { buildUpdateUserPayload, buildSessionUser } from "../utils/payloads";
import { validators } from "../utils/profileValidators";

/**
 * CreateProfile — Step 1 ("Personal Details") of the Create Family flow.
 *
 * Designed according to Figma UI specification:
 * - Profile avatar upload with camera icon (UploadDrawer integration).
 * - Strictly 3-column per row layout across all screen rows.
 * - Missing dropdowns fully integrated:
 *   1. Relationship with HOF (relationWithHof)
 *   2. Identity Proof (identityProof) + document upload
 *   3. Date of Birth Proof (birthDocument) + document upload
 *   4. Caste Certificate (casteDocumentOptions) + document upload
 *   5. Divyang / Specially Abled Proof (disabilityDocumentOptions) + document upload
 * - Save as Draft & Save & Next actions.
 */

const MAX_SIZE = 5 * 1024 * 1024; // 5 MB
const ACCEPTED = ".jpg,.jpeg,.png,.pdf";
const ALLOWED_TYPES = ["image/jpeg", "image/png", "image/jpg", "application/pdf"];

// Default gender choices
const defaultGenderOptions = [
  { i18nKey: "COMMON_GENDER_MALE", code: "MALE", value: "MALE", name: "Male" },
  { i18nKey: "COMMON_GENDER_FEMALE", code: "FEMALE", value: "FEMALE", name: "Female" },
  { i18nKey: "COMMON_GENDER_TRANSGENDER", code: "TRANSGENDER", value: "TRANSGENDER", name: "Transgender" },
];

// Caste category options
const casteOptions = [
  { i18nKey: "PROFILE_CASTE_GENERAL", code: "General", name: "General" },
  { i18nKey: "PROFILE_CASTE_OBC", code: "OBC", name: "OBC" },
  { i18nKey: "PROFILE_CASTE_SC", code: "SC", name: "SC" },
  { i18nKey: "PROFILE_CASTE_ST", code: "ST", name: "ST" },
];

// Specially abled / Divyang options
const speciallyAbledOptions = [
  { i18nKey: "YES", code: "Yes", name: "Yes" },
  { i18nKey: "NO", code: "No", name: "No" },
];

// Relationship with Head of Family (HOF)
const relationWithHof = [
  { i18nKey: "RELATION_SELF", code: "Self", name: "Self" },
  { i18nKey: "RELATION_SPOUSE", code: "Spouse", name: "Spouse" },
  { i18nKey: "RELATION_SON", code: "Son", name: "Son" },
  { i18nKey: "RELATION_DAUGHTER", code: "Daughter", name: "Daughter" },
  { i18nKey: "RELATION_CHILD", code: "Child", name: "Child" },
  { i18nKey: "RELATION_FATHER", code: "Father", name: "Father" },
  { i18nKey: "RELATION_MOTHER", code: "Mother", name: "Mother" },
  { i18nKey: "RELATION_BROTHER", code: "Brother", name: "Brother" },
  { i18nKey: "RELATION_DEPENDENT", code: "Dependent", name: "Dependent" },
  { i18nKey: "RELATION_SISTER", code: "Sister", name: "Sister" },
  { i18nKey: "RELATION_GUARDIAN", code: "Guardian", name: "Guardian" },
  { i18nKey: "RELATION_SON_IN_LAW", code: "Son In Law", name: "Son In Law" },
  { i18nKey: "RELATION_DAUGHTERINLAW", code: "Daughter In Law", name: "Daughter In Law" },
  { i18nKey: "RELATION_GRAND_SON", code: "Grandson", name: "Grandson" },
  { i18nKey: "RELATION_GRAND_DAUGHTER", code: "Granddaughter", name: "Granddaughter" },
  { i18nKey: "RELATION_GRAND_FATER", code: "Grandfather", name: "Grandfather" },
  { i18nKey: "RELATION_GRAND_MOTHER", code: "Grandmother", name: "Grandmother" },
  { i18nKey: "RELATION_OTHER", code: "Other", name: "Other" },
];

// Date of Birth Proof types
const birthDocument = [
  { i18nKey: "10TH", code: "10th", name: "10th" },
  { i18nKey: "BIRTH_CERTIFICATE", code: "Birth Certificate", name: "Birth Certificate" },
];

// Identity Proof types
const identityProof = [
  { i18nKey: "AADHAAR_CARD", code: "Aadhaar Card", name: "Aadhaar Card" },
  { i18nKey: "VOTER_ID", code: "Voter Id", name: "Voter Id" },
  { i18nKey: "DRIVING_LICENCE", code: "Driving Licence", name: "Driving Licence" },
  { i18nKey: "PASSPORT", code: "Passport", name: "Passport" },
];

// Caste Certificate document types
const casteDocumentOptions = [
  { i18nKey: "CASTE_CERTIFICATE", code: "Caste Certificate", name: "Caste Certificate" },
  { i18nKey: "COMMUNITY_CERTIFICATE", code: "Community Certificate", name: "Community Certificate" },
];

// Disability Certificate document types
const disabilityDocumentOptions = [
  { i18nKey: "DISABILITY_CERTIFICATE", code: "Disability Certificate", name: "Disability Certificate" },
  { i18nKey: "UDID_CARD", code: "UDID Card", name: "UDID Card" },
];

/**
 * Format date value to YYYY-MM-DD for DatePicker component
 */
const formatDateForInput = (dob) => {
  if (!dob) return "";
  if (typeof dob === "number") {
    const d = new Date(dob);
    if (!isNaN(d.getTime())) {
      return d.toISOString().split("T")[0];
    }
  }
  if (typeof dob === "string" && dob.includes("/")) {
    const parts = dob.split("/");
    if (parts.length === 3) {
      const [d, m, y] = parts;
      return `${y}-${m.padStart(2, "0")}-${d.padStart(2, "0")}`;
    }
  }
  return dob;
};

/**
 * Document Upload Card: matches Figma design with Type Dropdown on top and
 * styled file upload button below ([Choose file] File uploaded).
 */
const DocumentUploadCard = ({
  label,
  required,
  options,
  selectedType,
  onSelectType,
  typePlaceholder,
  file,
  onFileChange,
  error,
  t,
  stateId,
  disabled = false,
}) => {
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState(null);
  const fileInputRef = useRef(null);

  const handleFileSelect = async (e) => {
    const selectedFile = e.target.files?.[0];
    if (!selectedFile) return;

    if (!ALLOWED_TYPES.includes(selectedFile.type)) {
      setUploadError(t("PROFILE_DOC_INVALID_TYPE", "Only JPG, PNG, and PDF files are allowed"));
      return;
    }
    if (selectedFile.size > MAX_SIZE) {
      setUploadError(t("PROFILE_DOC_SIZE_EXCEEDED", "File size cannot exceed 5MB"));
      return;
    }

    setUploadError(null);
    setUploading(true);
    try {
      const res = await Digit.UploadServices.Filestorage("profile", selectedFile, stateId);
      const fileStoreId = res?.data?.files?.[0]?.fileStoreId;
      if (!fileStoreId) throw new Error("NO_FILESTORE_ID");
      onFileChange({ name: selectedFile.name, fileStoreId });
    } catch (err) {
      setUploadError(t("CORE_COMMON_PROFILE_FILE_UPLOAD_ERROR", "Failed to upload file"));
    } finally {
      setUploading(false);
    }
  };

  const handleFileDelete = (e) => {
    e.stopPropagation();
    onFileChange(null);
    setUploadError(null);
    if (fileInputRef.current) fileInputRef.current.value = "";
  };

  return (
    <div style={{ display: "flex", flexDirection: "column", width: "100%" }}>
      <label style={profileStyles.label}>
        {label}
        {required && <span style={profileStyles.required}> *</span>}
      </label>

      {/* Document Type Dropdown */}
      <Dropdown
        selected={selectedType}
        select={onSelectType}
        option={options}
        optionKey="name"
        t={t}
        placeholder={typePlaceholder}
        disable={disabled}
        style={{ width: "100%", marginBottom: "8px" }}
      />

      {/* Styled File Upload Box */}
      <div
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          border: uploadError || error ? "1px solid #D4351C" : "1px solid #D6D5D5",
          borderRadius: "6px",
          padding: "6px 10px",
          background: disabled ? "#F4F5F7" : "#FFFFFF",
          minHeight: "44px",
          boxSizing: "border-box",
        }}
      >
        <div style={{ display: "flex", alignItems: "center", gap: "10px", overflow: "hidden", flex: 1 }}>
          <input
            type="file"
            ref={fileInputRef}
            accept={ACCEPTED}
            onChange={handleFileSelect}
            disabled={disabled || uploading}
            style={{ display: "none" }}
          />
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            disabled={disabled || uploading}
            style={{
              background: disabled ? "#E0E0E0" : "#D1D1D6",
              color: "#0B0C0C",
              border: "none",
              borderRadius: "4px",
              padding: "6px 14px",
              fontSize: "13px",
              fontWeight: 500,
              cursor: disabled || uploading ? "not-allowed" : "pointer",
              whiteSpace: "nowrap",
              flexShrink: 0,
            }}
          >
            {uploading ? t("UPLOADING", "Uploading...") : t("CHOOSE_FILE", "Choose file")}
          </button>

          <span
            style={{
              fontSize: "13px",
              color: file?.name ? "#0B0C0C" : "#7C7E80",
              overflow: "hidden",
              textOverflow: "ellipsis",
              whiteSpace: "nowrap",
            }}
            title={file?.name || ""}
          >
            {uploading
              ? t("UPLOADING", "Uploading...")
              : file?.name
              ? file.name
              : t("NO_FILE_SELECTED", "No file selected")}
          </span>
        </div>

        {file && !disabled && (
          <button
            type="button"
            onClick={handleFileDelete}
            title={t("REMOVE", "Remove file")}
            style={{
              background: "transparent",
              border: "none",
              cursor: "pointer",
              padding: "4px 8px",
              color: "#505A5F",
              fontSize: "16px",
              fontWeight: "bold",
              lineHeight: 1,
            }}
          >
            ✕
          </button>
        )}
      </div>

      {(uploadError || error) && (
        <CardLabelError style={{ marginTop: "4px" }}>
          {t(uploadError || error)}
        </CardLabelError>
      )}
    </div>
  );
};

const CreateProfile = ({ config, onSelect, formData }) => {
  const { t } = useTranslation();
  const stateId = Digit.ULBService.getStateId();
  const { user, loading } = useProfileUser();
  const saved = formData?.[config?.key] || {};

  // Profile Avatar State
  const [photoId, setPhotoId] = useState(saved.photo || user?.photo || "");
  const [photoThumb, setPhotoThumb] = useState(null);
  const [showUploadDrawer, setShowUploadDrawer] = useState(false);

  // Form Fields State
  const [form, setForm] = useState({
    name: saved.name || user?.name || "",
    gender: saved.gender || toGenderOption(user?.gender),
    dob: saved.dob || formatDateForInput(user?.dob) || "",
    mobileNumber: saved.mobileNumber || user?.mobileNumber || "",
    altContactNumber: saved.altContactNumber || user?.altContactNumber || "",
    emailId: saved.emailId || user?.emailId || "",
    fatherName: saved.fatherName || user?.fatherName || "",
    motherName: saved.motherName || user?.motherName || "",
    spouseName: saved.spouseName || user?.spouseName || "",
    guardianName: saved.guardianName || user?.guardianName || "",
  });

  // Additional Dropdown Fields
  const [relationshipWithHof, setRelationshipWithHof] = useState(saved.relationshipWithHof || null);
  const [occupation, setOccupation] = useState(saved.occupation || "");
  const [caste, setCaste] = useState(saved.caste || casteOptions[0]);
  const [speciallyAbled, setSpeciallyAbled] = useState(saved.speciallyAbled || speciallyAbledOptions[1]);

  // Document Proofs State
  const [identityProofType, setIdentityProofType] = useState(saved.identityProofType || null);
  const [identityProofFile, setIdentityProofFile] = useState(saved.identityProofFile || null);

  const [birthProofType, setBirthProofType] = useState(saved.birthProofType || null);
  const [birthProofFile, setBirthProofFile] = useState(saved.birthProofFile || null);

  const [casteProofType, setCasteProofType] = useState(saved.casteProofType || null);
  const [casteProofFile, setCasteProofFile] = useState(saved.casteProofFile || saved.casteProof || null);

  const [disabilityProofType, setDisabilityProofType] = useState(saved.disabilityProofType || null);
  const [disabilityProofFile, setDisabilityProofFile] = useState(saved.disabilityProofFile || saved.disabilityProof || null);

  const [errors, setErrors] = useState({});
  const [toast, setToast] = useState(null);
  const [saving, setSaving] = useState(false);

  // Fetch thumbnail for initial photo
  useEffect(() => {
    const existingPhoto = saved.photo || user?.photo;
    if (existingPhoto) {
      setPhotoId(existingPhoto);
      fetchThumbnail(existingPhoto, stateId).then(setPhotoThumb);
    }
  }, [user?.photo, saved.photo]);

  // Sync loaded user info
  useEffect(() => {
    if (user && !form.name) {
      setForm((prev) => ({
        name: prev.name || user.name || "",
        gender: prev.gender || toGenderOption(user.gender),
        dob: prev.dob || formatDateForInput(user.dob) || "",
        mobileNumber: prev.mobileNumber || user.mobileNumber || "",
        altContactNumber: prev.altContactNumber || user.altContactNumber || "",
        emailId: prev.emailId || user.emailId || "",
        fatherName: prev.fatherName || user.fatherName || "",
        motherName: prev.motherName || user.motherName || "",
        spouseName: prev.spouseName || user.spouseName || "",
        guardianName: prev.guardianName || user.guardianName || "",
      }));
    }
  }, [user]);

  // Gender options hook
  const { data: genderData } = Digit.Hooks.pt.useGenderMDMS(stateId, "common-masters", "GenderType");
  const genderOptions = useMemo(() => {
    if (genderData && genderData.length > 0) {
      return genderData.map((g) => {
        const opt = toGenderOption(g.code) || { i18nKey: `PT_COMMON_GENDER_${g.code}`, code: g.code, value: g.code };
        return {
          ...opt,
          name: g.code.charAt(0).toUpperCase() + g.code.slice(1).toLowerCase(),
        };
      });
    }
    return defaultGenderOptions;
  }, [genderData]);

  // Derived state for conditional proofs
  const isSpeciallyAbled = speciallyAbled?.code === "Yes" || speciallyAbled?.code === SPECIALLY_ABLED_YES;
  const needsCasteProof = caste && caste.code !== "General" && caste.code !== DEFAULT_CASTE;

  const handleCasteChange = (val) => {
    setCaste(val);
    setErrors((prev) => ({ ...prev, caste: null }));
    if (val?.code === "General" || val?.code === DEFAULT_CASTE) {
      setCasteProofFile(null);
      setCasteProofType(null);
      setErrors((prev) => ({ ...prev, casteProofFile: null }));
    }
  };

  const handleSpeciallyAbledChange = (val) => {
    setSpeciallyAbled(val);
    setErrors((prev) => ({ ...prev, speciallyAbled: null }));
    if (val?.code === "No") {
      setDisabilityProofFile(null);
      setDisabilityProofType(null);
      setErrors((prev) => ({ ...prev, disabilityProofFile: null }));
    }
  };

  // Profile photo upload callbacks
  const handlePhotoUploaded = async (fileStoreId) => {
    setPhotoId(fileStoreId);
    const thumb = fileStoreId ? await fetchThumbnail(fileStoreId, stateId) : null;
    setPhotoThumb(thumb);
    setShowUploadDrawer(false);
    setToast({ error: false, label: t("PROFILE_PHOTO_UPLOAD_SUCCESS", "Profile photo uploaded successfully") });
  };

  const removeProfilePic = () => {
    setPhotoId(null);
    setPhotoThumb(null);
    setShowUploadDrawer(false);
    setToast({ error: false, label: t("PROFILE_PHOTO_REMOVED", "Profile photo removed") });
  };

  // Validation function
  const validateForm = () => {
    const nextErrors = {};

    if (!form.name?.trim()) nextErrors.name = "CORE_COMMON_PROFILE_NAME_REQUIRED";
    if (!form.gender) nextErrors.gender = "CORE_COMMON_PROFILE_GENDER_REQUIRED";
    if (!form.dob) nextErrors.dob = "CORE_COMMON_PROFILE_DOB_REQUIRED";
    if (!form.mobileNumber?.trim()) nextErrors.mobileNumber = "CORE_COMMON_PROFILE_MOBILE_REQUIRED";
    if (!form.altContactNumber?.trim()) nextErrors.altContactNumber = "CORE_COMMON_PROFILE_ALT_MOBILE_REQUIRED";
    if (!form.emailId?.trim()) nextErrors.emailId = "CORE_COMMON_PROFILE_EMAIL_REQUIRED";
    if (!form.fatherName?.trim()) nextErrors.fatherName = "CORE_COMMON_PROFILE_FATHER_REQUIRED";
    if (!form.motherName?.trim()) nextErrors.motherName = "CORE_COMMON_PROFILE_MOTHER_REQUIRED";
    if (!form.spouseName?.trim()) nextErrors.spouseName = "CORE_COMMON_PROFILE_SPOUSE_REQUIRED";
    if (!form.guardianName?.trim()) nextErrors.guardianName = "CORE_COMMON_PROFILE_GUARDIAN_REQUIRED";

    if (form.altContactNumber && validators.altMobileNumber(form.altContactNumber)) {
      nextErrors.altContactNumber = validators.altMobileNumber(form.altContactNumber);
    }
    if (form.emailId && validators.email(form.emailId)) {
      nextErrors.emailId = validators.email(form.emailId);
    }

    if (!caste) nextErrors.caste = "PROFILE_CASTE_REQUIRED";
    if (!speciallyAbled) nextErrors.speciallyAbled = "PROFILE_SPECIALLY_ABLED_REQUIRED";

    // Identity Proof validation
    if (!identityProofType) nextErrors.identityProofType = "PROFILE_SELECT_DOC_TYPE";
    if (!identityProofFile?.fileStoreId) nextErrors.identityProofFile = "PROFILE_PROOF_REQUIRED";

    // DOB Proof validation
    if (!birthProofType) nextErrors.birthProofType = "PROFILE_SELECT_DOC_TYPE";
    if (!birthProofFile?.fileStoreId) nextErrors.birthProofFile = "PROFILE_PROOF_REQUIRED";

    // Caste Certificate validation (when caste !== General)
    if (needsCasteProof && !casteProofFile?.fileStoreId) {
      nextErrors.casteProofFile = "PROFILE_PROOF_REQUIRED";
    }

    // Disability Proof validation (when specially abled === Yes)
    if (isSpeciallyAbled && !disabilityProofFile?.fileStoreId) {
      nextErrors.disabilityProofFile = "PROFILE_PROOF_REQUIRED";
    }

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  // Compile full form payload
  const compilePayload = () => ({
    ...form,
    photo: photoId,
    relationshipWithHof,
    occupation: occupation?.trim(),
    caste,
    casteProof: needsCasteProof ? casteProofFile : null,
    casteProofType,
    speciallyAbled,
    disabilityProof: isSpeciallyAbled ? disabilityProofFile : null,
    disabilityProofType,
    identityProofType,
    identityProofFile,
    birthProofType,
    birthProofFile,
  });

  // Save & Next
  const handleNext = async () => {
    if (saving) return;

    if (!validateForm()) {
      setToast({
        error: true,
        label: t("PLEASE_FILL_MANDATORY_FIELDS", "Please fill all mandatory fields and upload required documents."),
      });
      return;
    }

    setSaving(true);
    try {
      const userInfo = Digit.UserService.getUser()?.info || {};
      const payload = buildUpdateUserPayload({
        userInfo,
        form: {
          ...form,
          gender: form.gender?.value || form.gender?.code || form.gender,
        },
        photoId: photoId || user?.photo || "",
        locale: Digit.StoreData.getCurrentLanguage(),
      });

      const { responseInfo } = await Digit.UserService.updateUser(payload, stateId);
      if (responseInfo?.status === "SUCCESS") {
        const sessionUser = Digit.UserService.getUser();
        if (sessionUser) {
          Digit.UserService.setUser(buildSessionUser({ sessionUser, form, photoThumb }));
        }
      }
    } catch (err) {
      console.warn("User update background error:", err);
    } finally {
      setSaving(false);
    }

    onSelect(config.key, compilePayload());
  };

  if (loading) return <Loader />;

  return (
    <React.Fragment>
      <div>
        <FamilyCreateTabs t={t} />
      </div>

      <FormStep
        config={{
          ...config,
          texts: {
            ...config?.texts,
            header: config?.texts?.header || "ES_TITLE_PROFILE_DETAILS",
            submitBarLabel: config?.texts?.submitBarLabel || "COMMON_SAVE_NEXT",
          },
        }}
        onSelect={handleNext}
        isDisabled={saving}
        t={t}
      >
        {/* Profile Avatar / Photo Upload Card */}
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: "24px",
            padding: "16px 20px",
            marginBottom: "24px",
            background: "#F8F9FA",
            borderRadius: "10px",
            border: "1px solid #E8EAED",
            width: "100%",
            boxSizing: "border-box",
          }}
        >
          <AvatarUpload
            imageUrl={photoThumb}
            name={form.name}
            onCameraClick={() => setShowUploadDrawer(true)}
          />
          <div style={{ display: "flex", flexDirection: "column", gap: "4px" }}>
            <h3 style={{ margin: 0, fontSize: "18px", fontWeight: 600, color: "#0B0C0C" }}>
              {form.name || t("CORE_COMMON_PROFILE_DETAILS", "Profile Details")}
            </h3>
            <span style={{ fontSize: "14px", color: "#505A5F" }}>
              {t("PROFILE_PHOTO_UPLOAD_HELP", "Click on the camera icon to upload or update your profile photo.")}
            </span>
            <div style={{ display: "flex", gap: "10px", marginTop: "4px" }}>
              <button
                type="button"
                onClick={() => setShowUploadDrawer(true)}
                style={{
                  background: "#FFFFFF",
                  border: "1px solid #D6D5D5",
                  borderRadius: "6px",
                  padding: "6px 14px",
                  fontSize: "13px",
                  fontWeight: 500,
                  color: "#0B0C0C",
                  cursor: "pointer",
                }}
              >
                📷 {t("UPDATE_PHOTO", "Upload Photo")}
              </button>
              {photoThumb && (
                <button
                  type="button"
                  onClick={removeProfilePic}
                  style={{
                    background: "#FFFFFF",
                    border: "1px solid #FAD2CF",
                    borderRadius: "6px",
                    padding: "6px 14px",
                    fontSize: "13px",
                    fontWeight: 500,
                    color: "#D4351C",
                    cursor: "pointer",
                  }}
                >
                  🗑️ {t("REMOVE_PHOTO", "Remove")}
                </button>
              )}
            </div>
          </div>
        </div>

        {/* Form Fields: Strict 3-Column Per Row Grid */}
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(3, minmax(0, 1fr))",
            gap: "20px 24px",
            width: "100%",
            boxSizing: "border-box",
          }}
        >
          {/* Row 1 */}
          <FormField label={t("CORE_COMMON_PROFILE_NAME", "Name")} required error={errors.name} t={t}>
            <TextInput
              t={t}
              type="text"
              name="name"
              value={form.name}
              placeholder={t("TESTING_INSTANCE", "Testing Instance")}
              onChange={(e) => setFieldValue("name", e.target.value)}
              disable={true}
            />
          </FormField>

          <FormField label={t("CORE_COMMON_PROFILE_GENDER", "Gender")} required error={errors.gender} t={t}>
            <Dropdown
              selected={form.gender}
              select={(v) => setFieldValue("gender", v)}
              option={genderOptions}
              optionKey="name"
              t={t}
              name="gender"
              placeholder={t("SELECT_GENDER", "Select gender")}
            />
          </FormField>

          <FormField label={t("CORE_COMMON_PROFILE_DOB", "Date of Birth")} required error={errors.dob} t={t}>
            <DatePicker
              date={form.dob}
              onChange={(v) => setFieldValue("dob", v)}
              placeholder="DD/MM/YYYY"
            />
          </FormField>

          {/* Row 2 */}
          <FormField label={t("CORE_COMMON_PROFILE_MOBILE_NUMBER", "Mobile no.")} required error={errors.mobileNumber} t={t}>
            <MobileNumber
              value={form.mobileNumber}
              name="mobileNumber"
              placeholder="6894526358"
              onChange={(v) => setFieldValue("mobileNumber", v)}
              disable={true}
            />
          </FormField>

          <FormField label={t("CORE_COMMON_PROFILE_ALT_MOBILE_NUMBER", "Alternate Mobile No.")} required error={errors.altContactNumber} t={t}>
            <MobileNumber
              value={form.altContactNumber}
              name="altContactNumber"
              placeholder="+91"
              onChange={(v) => setFieldValue("altContactNumber", v)}
            />
          </FormField>

          <FormField label={t("CORE_COMMON_PROFILE_EMAIL", "Email")} required error={errors.emailId} t={t}>
            <TextInput
              t={t}
              type="email"
              name="emailId"
              value={form.emailId}
              placeholder={t("ENTER_EMAIL", "Enter email")}
              onChange={(e) => setFieldValue("emailId", e.target.value)}
            />
          </FormField>

          {/* Row 3 */}
          <FormField label={t("CORE_COMMON_FATHER_NAME", "Father Name")} required error={errors.fatherName} t={t}>
            <TextInput
              t={t}
              type="text"
              name="fatherName"
              value={form.fatherName}
              placeholder={t("ENTER_FATHER_NAME", "Enter father's name")}
              onChange={(e) => setFieldValue("fatherName", e.target.value)}
            />
          </FormField>

          <FormField label={t("CORE_COMMON_MOTHER_NAME", "Mother Name")} required error={errors.motherName} t={t}>
            <TextInput
              t={t}
              type="text"
              name="motherName"
              value={form.motherName}
              placeholder={t("ENTER_MOTHER_NAME", "Enter mother's name")}
              onChange={(e) => setFieldValue("motherName", e.target.value)}
            />
          </FormField>

          <FormField label={t("CORE_COMMON_SPOUSENAME", "Spouse Name")} required error={errors.spouseName} t={t}>
            <TextInput
              t={t}
              type="text"
              name="spouseName"
              value={form.spouseName}
              placeholder={t("ENTER_SPOUSE_NAME", "Enter spouse name")}
              onChange={(e) => setFieldValue("spouseName", e.target.value)}
            />
          </FormField>

          {/* Row 4 */}
          <FormField label={t("CORE_COMMON_GUARDIAN_NAME", "Guardian Name")} required error={errors.guardianName} t={t}>
            <TextInput
              t={t}
              type="text"
              name="guardianName"
              value={form.guardianName}
              placeholder={t("ENTER_GUARDIAN_NAME", "Enter guardian name")}
              onChange={(e) => setFieldValue("guardianName", e.target.value)}
            />
          </FormField>

          <FormField label={t("PROFILE_RELATIONSHIP_WITH_HOF", "Relationship with HOF")} error={errors.relationshipWithHof} t={t}>
            <Dropdown
              selected={relationshipWithHof}
              select={(v) => {
                setRelationshipWithHof(v);
                setErrors((prev) => ({ ...prev, relationshipWithHof: null }));
              }}
              option={relationWithHof}
              optionKey="name"
              t={t}
              placeholder={t("SELECT", "Select")}
            />
          </FormField>

          <FormField label={t("PROFILE_OCCUPATION", "Occupation")} t={t}>
            <TextInput
              t={t}
              type="text"
              name="occupation"
              value={occupation}
              placeholder={t("PROFILE_OCCUPATION", "Enter occupation")}
              onChange={(e) => setOccupation(e.target.value)}
            />
          </FormField>

          {/* Row 5 */}
          <FormField label={t("PROFILE_SPECIALLY_ABLED", "Divyang")} required error={errors.speciallyAbled} t={t}>
            <Dropdown
              selected={speciallyAbled}
              select={handleSpeciallyAbledChange}
              option={speciallyAbledOptions}
              optionKey="name"
              t={t}
              placeholder={t("YES_NO", "Yes / No")}
            />
          </FormField>

          <FormField label={t("PROFILE_CASTE", "Caste")} required error={errors.caste} t={t}>
            <Dropdown
              selected={caste}
              select={handleCasteChange}
              option={casteOptions}
              optionKey="name"
              t={t}
              placeholder={t("GENERAL_OBC_SC_ST", "General / OBC / SC / ST")}
            />
          </FormField>
        </div>

        {/* Supporting Verification Documents: Strict 3-Column Per Row Grid */}
        <div style={{ marginTop: "28px", marginBottom: "36px" }}>
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(3, minmax(0, 1fr))",
              gap: "20px 24px",
              width: "100%",
              boxSizing: "border-box",
            }}
          >
            {/* Document 1: Identity Proof (Always Shown) */}
            <DocumentUploadCard
              label={t("PROFILE_IDENTITY_PROOF", "Identity Proof")}
              required={true}
              options={identityProof}
              selectedType={identityProofType}
              onSelectType={(v) => {
                setIdentityProofType(v);
                setErrors((prev) => ({ ...prev, identityProofType: null }));
              }}
              typePlaceholder={t("CHOOSE_IDENTITY_TYPE", "Choose Identity Type")}
              file={identityProofFile}
              onFileChange={(f) => {
                setIdentityProofFile(f);
                setErrors((prev) => ({ ...prev, identityProofFile: null }));
              }}
              error={errors.identityProofType || errors.identityProofFile}
              t={t}
              stateId={stateId}
            />

            {/* Document 2: Date of Birth Proof (Always Shown) */}
            <DocumentUploadCard
              label={t("PROFILE_DOB_PROOF", "Date of Birth Proof")}
              required={true}
              options={birthDocument}
              selectedType={birthProofType}
              onSelectType={(v) => {
                setBirthProofType(v);
                setErrors((prev) => ({ ...prev, birthProofType: null }));
              }}
              typePlaceholder={t("CHOOSE_DOCUMENT_TYPE", "Choose Document Type")}
              file={birthProofFile}
              onFileChange={(f) => {
                setBirthProofFile(f);
                setErrors((prev) => ({ ...prev, birthProofFile: null }));
              }}
              error={errors.birthProofType || errors.birthProofFile}
              t={t}
              stateId={stateId}
            />

            {/* Document 3: Caste Certificate (Shown ONLY if Caste !== General) */}
            {needsCasteProof && (
              <DocumentUploadCard
                label={t("PROFILE_CASTE_PROOF", "Caste Certificate")}
                required={true}
                options={casteDocumentOptions}
                selectedType={casteProofType}
                onSelectType={(v) => {
                  setCasteProofType(v);
                  setErrors((prev) => ({ ...prev, casteProofType: null }));
                }}
                typePlaceholder={t("CHOOSE_DOCUMENT_TYPE", "Choose Document Type")}
                file={casteProofFile}
                onFileChange={(f) => {
                  setCasteProofFile(f);
                  setErrors((prev) => ({ ...prev, casteProofFile: null }));
                }}
                error={errors.casteProofFile}
                t={t}
                stateId={stateId}
              />
            )}

            {/* Document 4: Divyang Proof (Shown ONLY if Divyang === Yes) */}
            {isSpeciallyAbled && (
              <DocumentUploadCard
                label={t("PROFILE_DISABILITY_PROOF", "Divyang Proof")}
                required={true}
                options={disabilityDocumentOptions}
                selectedType={disabilityProofType}
                onSelectType={(v) => {
                  setDisabilityProofType(v);
                  setErrors((prev) => ({ ...prev, disabilityProofType: null }));
                }}
                typePlaceholder={t("CHOOSE_DOCUMENT_TYPE", "Choose Document Type")}
                file={disabilityProofFile}
                onFileChange={(f) => {
                  setDisabilityProofFile(f);
                  setErrors((prev) => ({ ...prev, disabilityProofFile: null }));
                }}
                error={errors.disabilityProofFile}
                t={t}
                stateId={stateId}
              />
            )}
          </div>
        </div>
      </FormStep>

      {/* UploadDrawer for Photo */}
      {showUploadDrawer && (
        <UploadDrawer
          setProfilePic={handlePhotoUploaded}
          closeDrawer={() => setShowUploadDrawer(false)}
          userType="citizen"
          removeProfilePic={removeProfilePic}
          showToast={(type, message) => setToast({ error: type === "error", label: message })}
        />
      )}

      {/* Toast Notification Container */}
      {toast && (
        <div className="profile-toast-container">
          <Toast error={toast.error} label={toast.label} onClose={() => setToast(null)} />
        </div>
      )}
    </React.Fragment>
  );
};

export default CreateProfile;
