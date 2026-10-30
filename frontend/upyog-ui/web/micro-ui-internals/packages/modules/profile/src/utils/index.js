import React, { useState } from "react";
import { CameraIcon, CardLabelError } from "@nudmcdgnpm/digit-ui-react-components";
import { profileStyles } from "./profileStyles";

export const ADDRESS_TYPE_LABEL = {
  PERMANENT: "CORE_PERMANENT",
  CORRESPONDENCE: "CORE_CORRESPONDENCE",
  OTHER: "CORE_OTHER",
};

export const ADDRESS_FIELDS = [
  ["COMMON_HOUSE_NO", "houseNumber"],
  ["COMMON_STREET_NAME", "streetName"],
  ["COMMON_ADDRESS_LINE1", "address"],
  ["COMMON_ADDRESS_LINE2", "address2"],
  ["COMMON_LANDMARK", "landmark"],
  ["COMMON_CITY", "city"],
  ["COMMON_LOCALITY", "locality"],
  ["COMMON_ADDRESS_PINCODE", "pinCode"],
  ["COMMON_ADDRESS_PROOF", "addressProof"]
];

export const hasValue = (value) => value !== null && value !== undefined && String(value).trim() !== "";
export const withValues = (pairs) => pairs.filter(([, value]) => hasValue(value));

export const getPersonalFields = (user) =>
  withValues([
    ["CORE_COMMON_PROFILE_NAME", user.name],
    ["CORE_COMMON_PROFILE_GENDER", user.gender],
    ["CORE_COMMON_PROFILE_DOB", user.dob],
    ["CORE_COMMON_PROFILE_MOBILE_NUMBER", user.mobileNumber],
    ["CORE_COMMON_PROFILE_ALT_MOBILE_NUMBER", user.altContactNumber],
    ["CORE_COMMON_PROFILE_EMAIL", user.emailId],
    ["CORE_COMMON_PROFILE_PAN", user.pan],
    ["CORE_COMMON_FATHER_NAME", user.fatherName],
    ["CORE_COMMON_FATHER_NAME", user.motherName],
    ["CORE_COMMON_FATHER_NAME", user.spouseName]
  ]);

export const getAddressBlocks = (addresses = []) => {
  const blocks = addresses
    .map((a) => ({
      type: a.type || a.addressType,
      fields: withValues(ADDRESS_FIELDS.map(([key, prop]) => [key, a[prop]])),
    }))
    .filter((block) => block.fields.length > 0);

  const seen = new Set();
  return blocks.filter((block) => {
    const key = block.type + JSON.stringify(block.fields);
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
};

export const getInitials = (name = "") =>
  name.trim().split(/\s+/).slice(0, 2).map((w) => w[0]?.toUpperCase()).join("");

// Converts a gender code string into the Dropdown option shape
export const toGenderOption = (code) =>
  code ? { i18nKey: `PT_COMMON_GENDER_${code}`, code, value: code } : null;

// photo field from API is comma-separated S3 URLs: original, large, medium, small
export const extractThumb = (photo) => {
  if (!photo) return "";
  const urls = photo.split(",");
  return urls[3]?.trim() || urls[0]?.trim() || "";
};

// Used after a new upload — filestore returns a real ID, resolves to URL
export const fetchThumbnail = async (fileStoreId, tenantId) => {
  const res = await Digit.UploadServices.Filefetch([fileStoreId], tenantId);
  return res?.data?.fileStoreIds?.[0]?.url?.split(",")?.[3]?.trim() || null;
};

// --- Shared presentational components ---

export const ProfilePhoto = ({ photo, name }) => {
  const [hasFailed, setHasFailed] = useState(false);
  const photoUrl = photo;
  if (!photoUrl || hasFailed)
    return <div style={profileStyles.photoPlaceholder}>{getInitials(name) || "?"}</div>;
  return <img src={photoUrl} alt={name} style={profileStyles.photo} onError={() => setHasFailed(true)} />;
};

// Avatar (photo or initials) with camera button — used in EditProfile
export const AvatarUpload = ({ imageUrl, name, onCameraClick }) => (
  <div style={profileStyles.avatarWrap}>
    {imageUrl ? (
      <img src={imageUrl} alt={name} style={profileStyles.photo} />
    ) : (
      <div style={profileStyles.photoPlaceholder}>{getInitials(name) || "?"}</div>
    )}
    <button type="button" style={profileStyles.cameraButton} onClick={onCameraClick} title="Update Profile Photo">
      <CameraIcon />
    </button>
  </div>
);

// Label + input + error — used in EditProfile form fields
export const FormField = ({ label, required, error, t, children }) => (
  <div>
    <label style={profileStyles.label}>
      {label}
      {required && <span style={profileStyles.required}> *</span>}
    </label>
    {children}
    {error && <CardLabelError>{t(error)}</CardLabelError>}
  </div>
);

// Read-only label + value — used in address cards
export const InfoField = ({ label, value }) => (
  <div>
    <div style={profileStyles.fieldLabel}>{label}</div>
    <div style={profileStyles.fieldValue}>{value}</div>
  </div>
);

const Field = ({ label, value }) => (
  <div>
    <div style={profileStyles.fieldLabel}>{label}</div>
    <div style={profileStyles.fieldValue}>{value}</div>
  </div>
);

export const FieldGrid = ({ fields, t }) => (
  <div style={profileStyles.grid}>
    {fields.map(([key, value]) => (
      <Field key={key} label={t(key)} value={value} />
    ))}
  </div>
);

export const Section = ({ title, children }) => (
  <div style={profileStyles.section}>
    <h3 style={profileStyles.sectionTitle}>{title}</h3>
    {children}
  </div>
);
