import React, { useState, useEffect } from "react";
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

/**
 * Normalizes a gender code or dropdown option into the Dropdown option shape.
 * @param {string|{code?: string, value?: string}|null|undefined} gender
 * @returns {{i18nKey: string, code: string, value: string}|null}
 */
export const toGenderOption = (gender) => {
  if (!gender) return null;
  const code = typeof gender === "object" ? (gender.code || gender.value) : gender;
  return code ? { i18nKey: `PT_COMMON_GENDER_${code}`, code, value: code } : null;
};

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

// Fetches the logged-in user with address, profile and document details
export const useProfileUser = () => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const uuid = Digit.UserService.getUser()?.info?.uuid;
  const tenantId = Digit.ULBService.getCurrentTenantId();

  useEffect(() => {
    let isActive = true; // avoids state update after unmount
    const fetchUser = async () => {
      if (!uuid) { setLoading(false); return; }
      try {
        // The updated /user/profile/_search endpoint expects a single uuid string rather than a list of UUIDs
        const response = await Digit.UserService.userSearch(tenantId, { uuid: uuid, include: ["ADDRESS", "PROFILE", "DOCUMENT"] }, {});
        if (isActive) setUser(response?.user?.[0] || null);
      } catch (e) {
        if (isActive) setError(true);
      } finally {
        if (isActive) setLoading(false);
      }
    };
    fetchUser();
    return () => { isActive = false; };
  }, [uuid, tenantId]);

  return { user, loading, error };
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


export const Field = ({ label, value, fieldKey, tenantId }) => {
  const isDoc = fieldKey === "COMMON_ADDRESS_PROOF";
  return (
    <div>
      <div style={profileStyles.fieldLabel}>{label}</div>
      {isDoc ? (
        <div
          onClick={() => openDocument(value, tenantId)}
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

export const FieldGrid = ({ fields, t, tenantId }) => (
  <div style={profileStyles.grid}>
    {fields.map(([key, value]) => (
      <Field key={key} fieldKey={key} label={t(key)} value={value} t={t} tenantId={tenantId} />
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
  // to fetch the url from filestoreid
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


// when verify from aadhar, will show these details of the family head
export const getFamilyHeadFields = (verifiedData, t) => {
  // Aadhaar flow — demographics object present
  if (verifiedData?.demographics) {
    return withValues([
      ["CORE_COMMON_FAMILY_ID", verifiedData?.familyId],
      ["CORE_COMMON_PROFILE_NAME", verifiedData?.demographics?.name],
      ["CORE_COMMON_PROFILE_GENDER", t(verifiedData?.demographics?.gender)],
      ["CORE_COMMON_PROFILE_DOB", verifiedData?.demographics?.dob],
    ]);
  }
  // Family ID flow — find the head member from family.members
  const headMember = verifiedData?.family?.members?.find((m) => m.isHead === true);
  return withValues([
    ["CORE_COMMON_FAMILY_ID", verifiedData?.familyId],
    ["CORE_COMMON_PROFILE_NAME", headMember?.name],
    ["CORE_COMMON_PROFILE_MOBILE_NUMBER", headMember?.mobileNumber],
  ]);
};

// when verify from family Id, will show these details of the family head
export const getFamilyFields = (family) =>
  withValues([
    ["CORE_COMMON_FAMILY_ID", family?.familyId],
    ["PROFILE_FAMILY_CATEGORY", family?.category],
    ["PROFILE_FAMILY_STATUS", family?.status],
    ["PROFILE_FAMILY_MEMBERS_COUNT", family?.members?.length],
  ]);


// Key used to stage family members during multi-step Create Family flow
const KEY = "Digit.family.members";

/**
 * Retrieves the current list of staged family members from sessionStorage.
 *
 * @returns {Array<Object>} List of family members, or empty array if none found or on storage error.
 */
export const getFamilyMembers = () => {
    try {
        return JSON.parse(sessionStorage.getItem(KEY) || "[]");
    } catch {
        return [];
    }
};

/**
 * Appends a new family member to the staged list in sessionStorage.
 * Silently ignores and returns the existing list if a member with matching userUuid already exists.
 *
 * @param {Object} member - The member object to add (must include userUuid).
 * @returns {Array<Object>} The updated list of family members (or unchanged list if duplicate).
 */
export const addFamilyMember = (member) => {
    const list = getFamilyMembers();
    // Prevent duplicate Aadhaar / userUuid
    if (list.some((m) => m.userUuid === member.userUuid)) return list;
    const updated = [...list, member];
    try {
        sessionStorage.setItem(KEY, JSON.stringify(updated));
    } catch (e) {
        console.error("Failed to persist family member in sessionStorage", e);
    }
    return updated;
};

/**
 * Overwrites the staged family members list in sessionStorage.
 *
 * @param {Array<Object>} list - The full list of family members to persist.
 * @returns {void}
 */
export const setFamilyMembers = (list) => {
    try {
        sessionStorage.setItem(KEY, JSON.stringify(list || []));
    } catch (e) {
        console.error("Failed to persist family members list in sessionStorage", e);
    }
};

/**
 * Clears the staged family members list from sessionStorage.
 *
 * @returns {void}
 */
export const clearFamilyMembers = () => {
    try {
        sessionStorage.removeItem(KEY);
    } catch (e) {
        console.error("Failed to clear family members from sessionStorage", e);
    }
};


// Validation as per Selections
export const TAX_VALIDATORS = {
  PAN: { pattern: /^[A-Z]{5}[0-9]{4}[A-Z]{1}$/, placeholder: "ABCDE1234F",    maxLength: 10 },
  TAN: { pattern: /^[A-Z]{4}[0-9]{5}[A-Z]{1}$/, placeholder: "ABCD12345E",    maxLength: 10 },
  GST: { pattern: /^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$/, placeholder: "22ABCDE1234F1Z5", maxLength: 15 },
};
