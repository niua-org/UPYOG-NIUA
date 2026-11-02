import React, { useState } from "react";
import { CameraIcon, CardLabelError } from "@nudmcdgnpm/digit-ui-react-components";
import { profileStyles } from "./profileStyles";

/**
 * All the Utilities Fucntions for Profile Module are defined here.
 * These are shared across EditProfile, EditAddress, EditDocuments and ProfileDetails.
 * Includes:
 * - getPersonalFields, getAddressBlocks, getDocumentBlocks
 * - toGenderOption, extractThumb, fetchThumbnail
 * - ProfilePhoto, AvatarUpload, FormField, InfoField, FieldGrid, Section
 * - openDocument
 * 
 * 
 * TODO: Move some of the component to React-Component-Library, as many are upgraded and many are not developed yet.
 */

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
  const getProp = (a, prop) => {
    if (prop === "addressProof") return a.filestoreId;
    return a[prop];
  };

  const blocks = addresses
    .map((a) => ({
      type: a.type || a.addressType,
      fields: withValues(ADDRESS_FIELDS.map(([key, prop]) => [key, getProp(a, prop)])),
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
// export const InfoField = ({ label, value }) => (
//   <div>
//     <div style={profileStyles.fieldLabel}>{label}</div>
//     <div style={profileStyles.fieldValue}>{value}</div>
//   </div>
// );

export const InfoField = ({ label, value, fieldKey }) => {
  const isDoc = fieldKey === "COMMON_ADDRESS_PROOF" && value && value !== "CS_NA";

  return (
    <div>
      <div style={profileStyles.fieldLabel}>{label}</div>
      {isDoc ? (
        <div
          onClick={() => openDocument(value, "pg")}
          style={{ cursor: "pointer", display: "inline-block", marginTop: 4 }}
          title="Click to view document"
        >
          <PDFSvg width={24} height={24} />
        </div>
      ) : (
        <div style={profileStyles.fieldValue}>{value}</div>
      )}
    </div>
  );
};


export const Field = ({ label, value, fieldKey }) => {
  const isDoc = fieldKey === "COMMON_ADDRESS_PROOF";
  return (
    <div>
      <div style={profileStyles.fieldLabel}>{label}</div>
      {isDoc ? (
        <div
          onClick={() => openDocument(value, "pg")}
          style={{ cursor: "pointer", display: "inline-block", marginTop: 4 }}
          title="Click to view document"
        >
          <PDFSvg width={28} height={28} />
        </div>
      ) : (
        <div style={profileStyles.fieldValue}>{value}</div>
      )}
    </div>
  );
};

export const FieldGrid = ({ fields, t }) => (
  <div style={profileStyles.grid}>
    {fields.map(([key, value]) => (
      <Field key={key} fieldKey={key} label={t(key)} value={value} t={t} />
    ))}
  </div>
);

export const Section = ({ title, children }) => (
  <div style={profileStyles.section}>
    <h3 style={profileStyles.sectionTitle}>{title}</h3>
    {children}
  </div>
);

export const PDFSvg = ({ width = 36, height = 36 }) => (
  <svg xmlns="http://www.w3.org/2000/svg" width={width} height={height} viewBox="0 0 24 24" fill="#D4351C">
    <path d="M20 2H8c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-8.5 7.5c0 .83-.67 1.5-1.5 1.5H9v2H7.5V7H10c.83 0 1.5.67 1.5 1.5v1zm5 2c0 .83-.67 1.5-1.5 1.5h-2.5V7H15c.83 0 1.5.67 1.5 1.5v3zm4-3H19v1h1.5V11H19v2h-1.5V7h3v1.5zM9 9.5h1v-1H9v1zM4 6H2v14c0 1.1.9 2 2 2h14v-2H4V6zm10 5.5h1v-3h-1v3z" />
  </svg>
);

export const getDocumentBlocks = (documents = []) => {
  return (documents || [])
    .filter((doc) => doc.active !== false)
    .map((doc) => {
      // to get the url
      const url = doc.fileStoreUrl?.split(",")?.[0] || doc.fileStoreUrl || doc.fileUrl;
      return {
        id: doc.id || doc.uuid,
        documentType: doc.documentType,
        filestoreId: doc.filestoreId,
        fileUrl: url,
        title: `PROFILE_${doc.documentType}`,
      };
    });
};


export const openDocument = async (fileOrId, stateId) => {
  if (!fileOrId) return;
  // agar filestoreId hai to URL fetch karke open karo
  try {
    const res = await Digit.UploadServices.Filefetch([fileOrId], stateId);
    const url = res?.data?.fileStoreIds?.[0]?.url?.split(",")?.[0];
    if (url) {
      window.open(url, "_blank", "noopener,noreferrer");
    }
  } catch (err) {
    console.error("Error opening document:", err);
  }
};