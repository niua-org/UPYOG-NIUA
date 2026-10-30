import React, { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { Dropdown, MobileNumber, TextInput, Toast, Loader, DatePicker, SubmitBar } from "@nudmcdgnpm/digit-ui-react-components";
import UploadDrawer from "./UploadDrawer";
import { AvatarUpload, FormField, toGenderOption, fetchThumbnail } from "../utils";
import { buildUpdateUserPayload, buildSessionUser } from "../utils/payloads";
import { validators, validateProfile, hasErrors } from "../utils/profileValidators";
import { profileStyles } from "../utils/profileStyles";


/*
 * EditProfileForm
 *
 * Handles everything related to editing a user's personal profile details.
 * Manages its own form state: name, email, mobile, alternate mobile, DOB, gender, city.
 * Handles profile photo upload via UploadDrawer and fetches the thumbnail after upload.
 * Validates fields on change and on save using profileValidators.
 * On successful save, updates the user session in Digit.UserService so the header reflects changes immediately.
 * Shows a Toast on success or failure.
 */



const EditProfileForm = ({ stateCode, userType, cityDetails, userDetails }) => {
  const { t } = useTranslation();
  const isEmployee = userType === "employee";
  const stateId = Digit.ULBService.getStateId();
  const tenantId = Digit.ULBService.getCurrentTenantId();
  const userInfo = Digit.UserService.getUser()?.info || {};
  const [form, setForm] = useState({
    name: userInfo.name || "",
    email: userDetails.emailId || "",
    mobileNumber: userInfo.mobileNumber || "",
    altMobileNumber: userDetails.altContactNumber || "",
    city: userInfo.permanentCity || cityDetails?.name || "",
    dob: userDetails?.dob || "",
    gender: toGenderOption(userDetails?.gender),
    fatherName: userDetails?.fatherName || "",
    motherName: userDetails?.motherName || "",
    spouseName: userDetails?.spouseName || "",
    guardianName: userDetails?.guardianName || ""
  });

  console.log("userDetailsuserDetails", userDetails)
  const [photoId, setPhotoId] = useState(userDetails?.photo || "");
  const [photoThumb, setPhotoThumb] = useState(null);
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState(null);
  const [showUploadDrawer, setShowUploadDrawer] = useState(false);

  const { data: genderData } = Digit.Hooks.pt.useGenderMDMS(stateId, "common-masters", "GenderType");
  const genderOptions = (genderData || []).map((gender) => toGenderOption(gender.code));

  useEffect(() => {
    if (userDetails?.photo) {
      fetchThumbnail(userDetails.photo, stateId).then(setPhotoThumb);
    }
  }, []);

  const showToast = (type, message, duration = 5000) => {
    setToast({ key: type, action: message });
    setTimeout(() => setToast(null), duration);
  };

  const setField = (field, value) => {
    setForm((prev) => ({ ...prev, [field]: value }));
    if (validators[field]) setErrors((prev) => ({ ...prev, [field]: validators[field](value) }));
  };

  const handlePhotoUploaded = async (fileStoreId) => {
    setPhotoId(fileStoreId);
    setPhotoThumb(fileStoreId ? await fetchThumbnail(fileStoreId, stateId) : null);
    setShowUploadDrawer(false);
  };

  const removeProfilePic = () => { setPhotoId(null); setPhotoThumb(null); };

  const handleSave = async () => {
    const validationErrors = validateProfile(form, isEmployee);
    setErrors(validationErrors);
    if (hasErrors(validationErrors)) {
      showToast("error", t(Object.values(validationErrors).find(Boolean)));
      return;
    }
    setSaving(true);
    try {
      const payload = buildUpdateUserPayload({ userInfo, form, photoId, locale: Digit.StoreData.getCurrentLanguage() });
      const { responseInfo } = await Digit.UserService.updateUser(payload, stateCode);
      if (responseInfo?.status === "SUCCESS") {
        const sessionUser = Digit.UserService.getUser();
        if (sessionUser) Digit.UserService.setUser(buildSessionUser({ sessionUser, form, photoThumb }));
        showToast("success", t("CORE_COMMON_PROFILE_UPDATE_SUCCESS"));
      }
    } catch (error) {
      showToast("error", error?.Errors?.[0]?.description || t("CORE_COMMON_PROFILE_UPDATE_ERROR"));
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <div style={profileStyles.card}>
        <AvatarUpload imageUrl={photoThumb} name={form.name} onCameraClick={() => setShowUploadDrawer(true)} />
        <div style={profileStyles.grid}>
          <FormField label={t("CORE_COMMON_PROFILE_NAME")} required error={errors.name} t={t}>
            <TextInput t={t} type="text" name="name" value={form.name} onChange={(e) => setField("name", e.target.value)} disable={true} />
          </FormField>
          <FormField label={t("CORE_COMMON_PROFILE_GENDER")} required={isEmployee} error={errors.gender} t={t}>
            <Dropdown selected={form.gender} select={(v) => setField("gender", v)} option={genderOptions} optionKey="i18nKey" t={t} name="gender" disable={true} />
          </FormField>
          <FormField label={t("CORE_COMMON_PROFILE_DOB")} required t={t}>
            <DatePicker date={form.dob} onChange={(v) => setField("dob", v)} disabled={true} />
          </FormField>
          <FormField label={t("CORE_COMMON_PROFILE_MOBILE_NUMBER")} required error={errors.mobileNumber} t={t}>
            <MobileNumber value={form.mobileNumber} name="mobileNumber" onChange={(v) => setField("mobileNumber", v)} disable={true} />
          </FormField>
          <FormField label={t("CORE_COMMON_PROFILE_ALT_MOBILE_NUMBER")} required error={errors.altMobileNumber} t={t}>
            <MobileNumber value={form.altMobileNumber} name="altMobileNumber" onChange={(v) => setField("altMobileNumber", v)} />
          </FormField>
          <FormField label={t("CORE_COMMON_PROFILE_EMAIL")} error={errors.email} t={t}>
            <TextInput t={t} type="email" name="email" value={form.email} onChange={(e) => setField("email", e.target.value)} disable={true} />
          </FormField>
          <FormField label={t("CORE_COMMON_FATHER_NAME")} error={errors.fatherName} t={t}>
            <TextInput t={t} type="text" name="email" value={form.fatherName} onChange={(e) => setField("fatherName", e.target.value)} disable={true} />
          </FormField>
          <FormField label={t("CORE_COMMON_MOTHER_NAME")} error={errors.motherName} t={t}>
            <TextInput t={t} type="text" name="motherName" value={form.motherName} onChange={(e) => setField("motherName", e.target.value)} disable={true} />
          </FormField>
          <FormField label={t("CORE_COMMON_SPOUSENAME")} error={errors.spouseName} t={t}>
            <TextInput t={t} type="text" name="spouseName" value={form.spouseName} onChange={(e) => setField("spouseName", e.target.value)} />
          </FormField>
          <FormField label={t("CORE_COMMON_GUARDIAN_NAME")} error={errors.guardianName} t={t}>
            <TextInput t={t} type="text" name="spouseName" value={form.guardianName} onChange={(e) => setField("guardianName", e.target.value)} />
          </FormField>
          {isEmployee && (
            <FormField label={t("CORE_COMMON_PROFILE_CITY")} t={t}>
              <TextInput t={t} type="text" name="city" value={t(form.city)} disable={true} />
            </FormField>
          )}
        </div>
      </div>
      <div style={profileStyles.actions}>
        <SubmitBar
          label={t("CORE_COMMON_SAVE")}
          onSubmit={handleSave}
          disabled={saving}
        />
      </div>

      {toast && (
        <div className="profile-toast-container">
          <Toast
            error={toast.key === "error"}
            label={t(toast.key === "success" ? "CORE_COMMON_PROFILE_UPDATE_SUCCESS" : toast.action)}
            onClose={() => setToast(null)}
          />
        </div>
      )}

      {showUploadDrawer && (
        <UploadDrawer
          setProfilePic={handlePhotoUploaded}
          closeDrawer={() => setShowUploadDrawer(false)}
          userType={userType}
          removeProfilePic={removeProfilePic}
          showToast={showToast}
        />
      )}
    </>
  );
};

export default EditProfileForm;
