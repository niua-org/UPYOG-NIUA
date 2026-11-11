/**
 * All the constants used in the Profile Module are defined here. 
 * These are shared across EditProfile, EditAddress, EditDocuments and ProfileDetails.
 */

export const AADHAAR_LENGTH = 12;
export const FAMILY_ID_LENGTH = 13;
export const OTP_LENGTH = 6;

export const PROFILE_ROUTES = {
  DETAILS: "/upyog-ui/citizen/profile/profile-details",
  FAMILY_JOINED: "/upyog-ui/citizen/profile/family-joined",
  FOUND_FAMILY: "/upyog-ui/citizen/profile/found-family",
  PROFILE_JOIN_EXISTING_FAMILY: "/upyog-ui/citizen/profile/join-family",
  // PROFILE_CREATE_FAMILY: "/upyog-ui/citizen/profile/family-member-added",
  PROFILE_CREATE_FAMILY: "/upyog-ui/citizen/profile/create-family",
  PROFILE_FAMILY_DASHBOARD: "/upyog-ui/citizen/profile/family-dashboard",
  ADD_FAMILY_MEMBER: "/upyog-ui/citizen/profile/add-family-member",
  VERIFY: "/upyog-ui/citizen/profile/verify-profile",
  EDIT_PROFILE_URL: "/upyog-ui/citizen/profile/edit-details",
  FAMILY_CREATED: "/upyog-ui/citizen/profile/family-created",
};

// Create Family → Profile step: option codes (see casteOptions / speciallyAbledOptions in CreateProfile) that decide whether a proof upload is shown
export const DEFAULT_CASTE = "General";
export const SPECIALLY_ABLED_YES = "Yes";

export const getFamilyIdLength = (value = "") => value.replace(/-/g, "").length;