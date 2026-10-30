// All request payload builders used by the Edit Profile screen

// Body for Digit.UserService.updateUser
export const buildUpdateUserPayload = ({ userInfo, form, photoId, locale }) => ({
  ...userInfo,
  name: form.name,
  dob: form.dob ? form.dob.split("-").reverse().join("/") : "", // yyyy-mm-dd -> dd/mm/yyyy
  gender: form.gender?.value,
  emailId: form.email,
  altContactNumber: form.altMobileNumber,
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
    altContactNumber: form.altMobileNumber,
    emailId: form.email,
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