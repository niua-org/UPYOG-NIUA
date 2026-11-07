// All request payload builders used by the Edit Profile screen

// Body for Digit.UserService.updateUser
export const buildUpdateUserPayload = ({ userInfo, form, photoId, locale }) => ({
  ...userInfo,
  name: form.name,
  dob: form.dob ? form.dob.split("-").reverse().join("/") : "", // yyyy-mm-dd -> dd/mm/yyyy
  gender: form.gender?.value,
  emailId: form.emailId,
  altContactNumber: form.altContactNumber,
  photo: photoId,
  locale,
  fatherName: form?.fatherName,
  motherName: form?.motherName,
  spouseName: form?.spouseName,
  guardianName: form?.guardianName
});

// Updated session user (stored in localStorage) after a successful update
export const buildSessionUser = ({ sessionUser, form, photoThumb }) => ({
  ...sessionUser,
  info: {
    ...sessionUser.info,
    name: form.name,
    mobileNumber: form.mobileNumber,
    altContactNumber: form.altContactNumber,
    emailId: form.emailId,
    permanentCity: form.city,
    photo: photoThumb,
    fatherName: form?.fatherName,
    motherName: form?.motherName,
    spouseName: form?.spouseName,
    guardianName: form?.guardianName
  },
});

// Body for Digit.UserService.changePassword
// NOTE: change-password UI was commented out in the old component, so this is not wired yet
export const buildChangePasswordPayload = ({ currentPassword, newPassword, confirmPassword, tenantId, userName }) => ({
  existingPassword: currentPassword,
  newPassword,
  confirmPassword,
  tenantId,
  type: "EMPLOYEE",
  username: userName,
});

// Body for Digit.UserService.updateUserDocument (`document` is singular)
export const buildUpdateDocumentPayload = ({ existingDoc, tenantId, filestoreId, fileName }) => ({
  id: existingDoc.id,
  tenantId,
  documentType: existingDoc.documentType,
  documentUid: existingDoc.documentUid || "",
  filestoreId,
  fileName: fileName || existingDoc.fileName || "",
  isVerified: existingDoc.isVerified ?? true,
  additionalDetails: existingDoc.additionalDetails || {},
});

// Body item for Digit.UserService.createUserDocument (`documents` is an array)
export const buildCreateDocumentPayload = ({ userUuid, tenantId, documentType, filestoreId, fileName }) => ({
  userUuid,
  tenantId,
  documentType,
  filestoreId,
  ...(fileName && { fileName }),
  active: true,
});



// Body for Digit.UserService.createFamily
export const buildCreateFamilyPayload = ({ membersList, tenantId, selectedHeadUuid }) => {
  // 1. Use ONLY the members explicitly verified and added to the screen
  const members = membersList || [];

  // 2. Head UUID from the radio button (or fallback to the first member)
  const finalHeadUuid = selectedHeadUuid || members[0]?.userUuid;

  // 3. Keep relationshipWithHead exactly as selected from the dropdown,
  // and set isHead: true ONLY for the member selected as Head via radio button
  const finalMembers = members.map((member) => ({
    userUuid: member.userUuid,
    name: member.name || "Family Member",
    relationshipWithHead: member.relationshipWithHead,
    isHead: member.userUuid === finalHeadUuid,
  }));

  return {
    Family: {
      tenantId: tenantId,
      headUserUuid: finalHeadUuid,
      members: finalMembers,
    },
  };
};

// Maps and standardizes the Aadhaar OTP verification response into a verified member object
export const buildVerifiedMemberInfo = ({ response = {}, cleanAadhaar = "" }) => ({
  ...response,
  userUuid: response?.userUuid || response?.uuid,
  uuid: response?.userUuid || response?.uuid,
  message: response?.message || "Aadhaar verified successfully",
  maskedAadhaar: response?.maskedAadhaar || (cleanAadhaar ? `XXXXXXXX${cleanAadhaar.slice(-4)}` : "NA"),
  name: response?.demographics?.name || response?.name,
  dob: response?.demographics?.dob || response?.dob,
  gender: response?.demographics?.gender || response?.gender,
  demographics: response?.demographics || {
    name: response?.name,
    dob: response?.dob,
    gender: response?.gender,
  },
});
