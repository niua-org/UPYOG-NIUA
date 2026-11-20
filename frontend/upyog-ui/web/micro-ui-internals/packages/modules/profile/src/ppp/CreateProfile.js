import React, { useRef, useState, useEffect, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useSearchParams } from "react-router-dom";
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
import { FormField, AvatarUpload, toGenderOption, fetchThumbnail, useProfileUser, getVerifiedMember, getFamilyMembers, setFamilyMembers } from "../utils";
import { profileStyles } from "../utils/profileStyles";
import { DEFAULT_CASTE, SPECIALLY_ABLED_YES } from "../utils/constants";
import { buildUpdateUserPayload, buildSessionUser } from "../utils/payloads";
import { validators } from "../utils/profileValidators";
import DocumentUploadCard from "../utils/DocumentUploadCard";

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
  { i18nKey: "HOF", code: "HOF", name: "Hof" },
  { i18nKey: "RELATION_SPOUSE", code: "SPOUSE", name: "Spouse" },
  { i18nKey: "RELATION_SON", code: "SON", name: "Son" },
  { i18nKey: "RELATION_DAUGHTER", code: "DAUGHTER", name: "Daughter"},
  { i18nKey: "RELATION_FATHER", code: "FATHER", name: "Father" },
  { i18nKey: "RELATION_MOTHER", code: "MOTHER", name: "Mother" },
  { i18nKey: "RELATION_BROTHER", code: "BROTHER", name: "Brother" },
  { i18nKey: "RELATION_DEPENDENT", code: "DEPENDENT", name: "Dependent" },
  { i18nKey: "RELATION_SISTER", code: "SISTER", name: "Sister" },
  { i18nKey: "RELATION_GUARDIAN", code: "GUARDIAN", name: "Guardian" },
  { i18nKey: "RELATION_SON_IN_LAW", code: "SON IN LAW", name: "Son In Law" },
  { i18nKey: "RELATION_DAUGHTERINLAW", code: "DAUGHTER IN LAW", name: "Daughter In Law" },
  { i18nKey: "RELATION_GRAND_SON", code: "GRANDSON", name: "Grandson" },
  { i18nKey: "RELATION_GRAND_DAUGHTER", code: "GRANDDAUGHTER", name: "Granddaughter" },
  { i18nKey: "RELATION_GRAND_FATER", code: "GRANDFATHER", name: "Grandfather" },
  { i18nKey: "RELATION_GRAND_MOTHER", code: "GRANDMOTHER", name: "Grandmother" },
  { i18nKey: "RELATION_OTHER", code: "OTHER", name: "Other" }
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
 * Normalizes gender value from user profile to standard Dropdown shape with display name
 */
const normalizeGender = (gender) => {
  if (!gender) return null;
  const rawCode = typeof gender === "object" ? (gender.code || gender.value || gender.name) : gender;
  if (!rawCode) return null;
  const code = String(rawCode).toUpperCase();
  const name = code.charAt(0) + code.slice(1).toLowerCase();
  return {
    i18nKey: `PT_COMMON_GENDER_${code}`,
    code,
    value: code,
    name,
  };
};

const CreateProfile = ({ config, onSelect, formData }) => {
  const { t } = useTranslation();
  const stateId = Digit.ULBService.getStateId() || Digit.ULBService.getCurrentTenantId() || "pg";
  const { user: loggedInUser, loading } = useProfileUser();
  const saved = formData?.[config?.key] || {};

  // `?member=<uuid>` = a member added through Aadhaar; no param (or own uuid) = the applicant themself.
  // Everything below prefills from `user`: the logged-in user's profile, or the Aadhaar-verified details of the member.
  const [searchParams] = useSearchParams();
  const memberParam = searchParams.get("member");
  const isViewMode = searchParams.get("mode") === "view" || !!config?.isViewMode;
  const isSelf = !memberParam || memberParam === Digit.UserService.getUser()?.info?.uuid;
  const user = useMemo(() => {
    if (isSelf) return loggedInUser;
    const verified = getVerifiedMember();
    const verifiedUuid = verified?.userUuid || verified?.uuid;
    if (memberParam && verifiedUuid && verifiedUuid !== memberParam) return {};
    const gender = verified?.gender || verified?.demographics?.gender;
    const mob = verified?.mobileNumber || verified?.demographics?.mobileNumber;
    return {
      ...verified,
      name: verified?.demographics?.name || verified?.name || "",
      dob: verified?.demographics?.dob || verified?.dob || "",
      mobileNumber: mob && /^\d{10}$/.test(String(mob)) ? String(mob) : mob || "",
      gender: String(gender || "").length === 1 ? { M: "MALE", F: "FEMALE", T: "TRANSGENDER" }[String(gender).toUpperCase()] : gender,
    };
  }, [isSelf, memberParam, loggedInUser]);

  // Profile Avatar State
  const [photoId, setPhotoId] = useState(saved.photo || user?.photo || "");
  const [photoThumb, setPhotoThumb] = useState(null);
  const [showUploadDrawer, setShowUploadDrawer] = useState(false);

  // Form Fields State
  const [form, setForm] = useState({
    name: saved.name || user?.name || "",
    gender: saved.gender || normalizeGender(user?.gender) || toGenderOption(user?.gender),
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

  // Auto-dismiss the toast after a few seconds
  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => setToast(null), 4000);
    return () => clearTimeout(timer);
  }, [toast]);

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
    if (user && (user.name || user.dob || user.gender || user.mobileNumber)) {
      setForm((prev) => ({
        ...prev,
        name: !isSelf ? (user.name || prev.name || "") : (prev.name || user.name || ""),
        gender: !isSelf
          ? (normalizeGender(user.gender) || toGenderOption(user.gender) || prev.gender)
          : (prev.gender || normalizeGender(user.gender) || toGenderOption(user.gender)),
        dob: !isSelf
          ? (formatDateForInput(user.dob) || prev.dob || "")
          : (prev.dob || formatDateForInput(user.dob) || ""),
        mobileNumber: !isSelf
          ? (user.mobileNumber || prev.mobileNumber || "")
          : (prev.mobileNumber || user.mobileNumber || ""),
        altContactNumber: prev.altContactNumber || user.altContactNumber || "",
        emailId: prev.emailId || user.emailId || "",
        fatherName: prev.fatherName || user.fatherName || "",
        motherName: prev.motherName || user.motherName || "",
        spouseName: prev.spouseName || user.spouseName || "",
        guardianName: prev.guardianName || user.guardianName || "",
      }));
    }
  }, [user, isSelf]);

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

  const setFieldValue = (field, valueOrEvent) => {
    const value = valueOrEvent && valueOrEvent.target !== undefined ? valueOrEvent.target.value : valueOrEvent;
    setForm((prev) => ({ ...prev, [field]: value }));
    setErrors((prev) => ({ ...prev, [field]: null }));
  };

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
    if (fileStoreId) {
      setErrors((prev) => {
        const next = { ...prev };
        delete next.photo;
        return next;
      });
    }
    setToast({ error: false, label: t("PROFILE_PHOTO_UPLOAD_SUCCESS", "Profile photo uploaded successfully") });
  };

  const removeProfilePic = () => {
    setPhotoId(null);
    setPhotoThumb(null);
    setShowUploadDrawer(false);
    setErrors((prev) => ({
      ...prev,
      photo: "CORE_COMMON_PROFILE_PHOTO_REQUIRED",
    }));
    setToast({ error: false, label: t("PROFILE_PHOTO_REMOVED", "Profile photo removed") });
  };

  // Collects every validation error for the current values (pure: does not touch state)
  const collectErrors = () => {
    const nextErrors = {};

    // Profile photo validation (mandatory)
    if (!photoId) {
      nextErrors.photo = "CORE_COMMON_PROFILE_PHOTO_REQUIRED";
    }

    if (!form.name?.trim()) nextErrors.name = "CORE_COMMON_PROFILE_NAME_REQUIRED";
    if (!form.gender) nextErrors.gender = "CORE_COMMON_PROFILE_GENDER_REQUIRED";
    if (!form.dob) nextErrors.dob = "CORE_COMMON_PROFILE_DOB_REQUIRED";
    if (!form.mobileNumber?.trim()) nextErrors.mobileNumber = "CORE_COMMON_PROFILE_MOBILE_REQUIRED";
    if (!form.altContactNumber?.trim()) nextErrors.altContactNumber = "CORE_COMMON_PROFILE_ALT_MOBILE_REQUIRED";
    if (!form.emailId?.trim()) nextErrors.emailId = "CORE_COMMON_PROFILE_EMAIL_REQUIRED";
    if (!form.fatherName?.trim()) nextErrors.fatherName = "CORE_COMMON_PROFILE_FATHER_REQUIRED";

    if (form.altContactNumber && validators.altMobileNumber(form.altContactNumber)) {
      nextErrors.altContactNumber = validators.altMobileNumber(form.altContactNumber);
    }
    if (form.emailId && validators.email(form.emailId)) {
      nextErrors.emailId = validators.email(form.emailId);
    }

    if (!caste) nextErrors.caste = "PROFILE_CASTE_REQUIRED";
    if (!speciallyAbled) nextErrors.speciallyAbled = "PROFILE_SPECIALLY_ABLED_REQUIRED";

    // Relationship with Head of Family is mandatory
    if (!relationshipWithHof) nextErrors.relationshipWithHof = "ERR_SELECT_RELATIONSHIP";

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

    return nextErrors;
  };

  // Save & Next stays disabled until every mandatory field / document is filled and valid
  const isFormComplete = Object.keys(collectErrors()).length === 0;

  // Validation on submit: shows the inline errors
  const validateForm = () => {
    const nextErrors = collectErrors();
    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  // Compile full form payload
  const compilePayload = () => ({
    ...form,
    photo: photoId,
    relationshipWithHof,
    occupation: saved.occupation || undefined,
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

  // Save & Next (or Next in view mode)
  const handleNext = async () => {
    if (saving) return;

    if (isViewMode) {
      onSelect(config.key, compilePayload());
      return;
    }

    if (!validateForm()) {
      setToast({
        error: true,
        label: t("PLEASE_FILL_MANDATORY_FIELDS", "Please fill all mandatory fields and upload required documents."),
      });
      return;
    }

    // Only the applicant's own profile is updated; another member's details are kept in the flow data only
    if (isSelf) {
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

        const res = await Digit.UserService.updateUser(payload, stateId);
        if (res?.responseInfo?.status === "SUCCESS" || res?.user) {
          const sessionUser = Digit.UserService.getUser();
          if (sessionUser) {
            Digit.UserService.setUser(buildSessionUser({ sessionUser, form, photoThumb }));
          }
        } else {
          throw new Error(res?.Errors?.[0]?.message || res?.Errors?.[0]?.description || "FAILED_TO_UPDATE_PROFILE");
        }
      } catch (err) {
        console.error("User update error:", err);
        const errorMessage =
          err?.response?.data?.Errors?.[0]?.message ||
          err?.response?.data?.Errors?.[0]?.description ||
          err?.Errors?.[0]?.message ||
          err?.Errors?.[0]?.description ||
          t("CORE_COMMON_PROFILE_UPDATE_ERROR", "Failed to update profile. Please try again.");
        setToast({
          error: true,
          label: errorMessage,
        });
        setSaving(false);
        return; // Stop the flow; do not advance to the next step when save fails
      } finally {
        setSaving(false);
      }

      // Sync applicant relationship directly into staged family members list
      try {
        const relCode = String(relationshipWithHof?.code || relationshipWithHof?.name || relationshipWithHof || "").toUpperCase();
        if (relCode) {
          const staged = getFamilyMembers();
          if (staged && staged.length > 0 && staged.some((m) => m.userUuid === user?.uuid)) {
            setFamilyMembers(staged.map((m) => (m.userUuid === user?.uuid ? { ...m, relationshipWithHead: relCode } : m)));
          }
        }
      } catch (err) {
        console.warn("Failed to sync applicant relationship to family members list", err);
      }
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
            submitBarLabel: isViewMode ? "COMMON_NEXT" : (config?.texts?.submitBarLabel || "COMMON_SAVE_NEXT"),
          },
        }}
        onSelect={handleNext}
        isDisabled={isViewMode ? false : (saving || !isFormComplete)}
        t={t}
      >
        {/* Profile Avatar / Photo Upload Card */}
        <div
          style={errors.photo ? profileStyles.photoUploadCardError : profileStyles.photoUploadCard}
        >
          <AvatarUpload
            imageUrl={photoThumb}
            name={form.name}
            onCameraClick={isViewMode ? undefined : () => setShowUploadDrawer(true)}
          />
          <div style={profileStyles.photoDetailsWrap}>
            <h3 style={profileStyles.photoTitle}>
              {form.name || t("CORE_COMMON_PROFILE_DETAILS", "Profile Details")}
            </h3>
            <span style={profileStyles.photoHelpText}>
              {t("PROFILE_PHOTO_UPLOAD_HELP", "Click on the camera icon to upload or update your profile photo.")}
              <span style={profileStyles.requiredAsterisk}>*</span>
            </span>
            {errors.photo && (
              <span style={profileStyles.photoErrorMessage}>
                {t(errors.photo, "Profile photo is mandatory.")}
              </span>
            )}
            <div style={profileStyles.photoButtonRow}>
              {!isViewMode && (
                <button
                  type="button"
                  onClick={() => setShowUploadDrawer(true)}
                  style={errors.photo ? profileStyles.photoUploadBtnError : profileStyles.photoUploadBtn}
                >
                  📷 {t("UPDATE_PHOTO", "Upload Photo")}
                </button>
              )}
              {photoThumb && !isViewMode && (
                <button
                  type="button"
                  onClick={removeProfilePic}
                  style={profileStyles.photoRemoveBtn}
                >
                  🗑️ {t("REMOVE_PHOTO", "Remove")}
                </button>
              )}
            </div>
          </div>
        </div>

        {/* Form Fields: Dynamic 3-Column Per Row Grid */}
        <div style={profileStyles.gridStyle}>
          {/* Row 1 */}
          <FormField label={t("CORE_COMMON_PROFILE_NAME", "Name")} required error={errors.name} t={t}>
            <TextInput
              t={t}
              type="text"
              name="name"
              value={form.name}
              placeholder={t("CORE_COMMON_PROFILE_NAME", "Name")}
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
              disable={true}
            />
          </FormField>

          <FormField label={t("CORE_COMMON_PROFILE_DOB", "Date of Birth")} required error={errors.dob} t={t}>
            <DatePicker
              date={form.dob}
              onChange={(v) => setFieldValue("dob", v)}
              placeholder="DD/MM/YYYY"
              disabled={true}
            />
          </FormField>

          {/* Row 2 */}
          <FormField label={t("CORE_COMMON_PROFILE_MOBILE_NUMBER", "Mobile no.")} required error={errors.mobileNumber} t={t}>
            <MobileNumber
              value={form.mobileNumber}
              name="mobileNumber"
              onChange={(v) => setFieldValue("mobileNumber", v)}
              disable={isSelf || !!user?.mobileNumber} // locked when known (applicant, or returned by Aadhaar); otherwise typed in
            />
          </FormField>

          <FormField label={t("CORE_COMMON_PROFILE_ALT_MOBILE_NUMBER", "Alternate Mobile No.")} required error={errors.altContactNumber} t={t}>
            <MobileNumber
              value={form.altContactNumber}
              name="altContactNumber"
              onChange={(v) => setFieldValue("altContactNumber", v)}
              disable={isViewMode}
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
              disable={isViewMode}
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
              disable={isViewMode}
            />
          </FormField>

          <FormField label={t("CORE_COMMON_MOTHER_NAME", "Mother Name")} error={errors.motherName} t={t}>
            <TextInput
              t={t}
              type="text"
              name="motherName"
              value={form.motherName}
              placeholder={t("ENTER_MOTHER_NAME", "Enter mother's name")}
              onChange={(e) => setFieldValue("motherName", e.target.value)}
              disable={isViewMode}
            />
          </FormField>

          <FormField label={t("CORE_COMMON_SPOUSENAME", "Spouse Name")} error={errors.spouseName} t={t}>
            <TextInput
              t={t}
              type="text"
              name="spouseName"
              value={form.spouseName}
              placeholder={t("ENTER_SPOUSE_NAME", "Enter spouse name")}
              onChange={(e) => setFieldValue("spouseName", e.target.value)}
              disable={isViewMode}
            />
          </FormField>

          {/* Row 4 */}
          <FormField label={t("CORE_COMMON_GUARDIAN_NAME", "Guardian Name")} error={errors.guardianName} t={t}>
            <TextInput
              t={t}
              type="text"
              name="guardianName"
              value={form.guardianName}
              placeholder={t("ENTER_GUARDIAN_NAME", "Enter guardian name")}
              onChange={(e) => setFieldValue("guardianName", e.target.value)}
              disable={isViewMode}
            />
          </FormField>

          <FormField label={t("PROFILE_RELATIONSHIP_WITH_HOF", "Relationship with HOF")} required error={errors.relationshipWithHof} t={t}>
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
              disable={isViewMode}
              optionCardStyles={profileStyles.dropDownHeight}
            />
          </FormField>

          {/* Divyang (appears right after Relationship with HOF) */}
          <FormField label={t("PROFILE_SPECIALLY_ABLED", "Divyang/Handicapped")} required error={errors.speciallyAbled} t={t}>
            <Dropdown
              selected={speciallyAbled}
              select={handleSpeciallyAbledChange}
              option={speciallyAbledOptions}
              optionKey="name"
              t={t}
              placeholder={t("YES_NO", "Yes / No")}
              disable={isViewMode}
            />
          </FormField>

          {/* Divyang Proof appears in the next column dynamically when Divyang is Yes */}
          {isSpeciallyAbled && (
            <DocumentUploadCard
              label={t("PROFILE_DISABILITY_PROOF", "Divyang/Handicapped Proof")}
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
              disabled={isViewMode}
            />
          )}

          {/* Caste follows in the next available column */}
          <FormField label={t("PROFILE_CASTE", "Caste")} required error={errors.caste} t={t}>
            <Dropdown
              selected={caste}
              select={handleCasteChange}
              option={casteOptions}
              optionKey="name"
              t={t}
              placeholder={t("GENERAL_OBC_SC_ST", "General / OBC / SC / ST")}
              disable={isViewMode}
            />
          </FormField>

          {/* Caste Certificate appears in the next column dynamically when Caste is not General */}
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
              disabled={isViewMode}
            />
          )}

          {/* Identity Proof (Always Shown, shifts dynamically) */}
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
            disabled={isViewMode}
          />

          {/* Date of Birth Proof (Always Shown, shifts dynamically) */}
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
            disabled={isViewMode}
          />
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
