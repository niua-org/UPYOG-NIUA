/**
 * All the constants used in the Profile Module are defined here. 
 * These are shared across EditProfile, EditAddress, EditDocuments and ProfileDetails.
 */

export const AADHAAR_LENGTH = 12;
export const FAMILY_ID_LENGTH = 12;
export const OTP_LENGTH = 6;

export const PROFILE_ROUTES = {
  DETAILS: "/upyog-ui/citizen/profile/profile-details",
  FAMILY_JOINED: "/upyog-ui/citizen/profile/family-joined",
  FOUND_FAMILY: "/upyog-ui/citizen/profile/found-family"
};

export const getFamilyIdLength = (value = "") => value.replace(/-/g, "").length;