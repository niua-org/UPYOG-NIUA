const NAME_REGEX = /^[a-zA-Z ]+$/;
const MOBILE_REGEX = /^[6-9]{1}[0-9]{9}$/;
const EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-z.-]+\.(com|org|in)$/;

const mobileError = (value) => (MOBILE_REGEX.test(value) ? null : "CORE_COMMON_PROFILE_MOBILE_NUMBER_INVALID");

// Field validators: return an i18n error key, or null when the value is valid
export const validators = {
  name: (value) => (NAME_REGEX.test(value) && value.length <= 50 ? null : "CORE_COMMON_PROFILE_NAME_INVALID"),
  email: (value) => (!value || EMAIL_REGEX.test(value) ? null : "CORE_COMMON_PROFILE_EMAIL_INVALID"),
  altMobileNumber: mobileError,
};

// Validates the whole form on save, returns { field: errorKey | null }
export const validateProfile = (form, isEmployee) => {
  const errors = {
    name: validators.name(form.name),
    email: validators.email(form.email),
    altMobileNumber: validators.altMobileNumber(form.altMobileNumber),
  };
  if (isEmployee) {
    errors.gender = form.gender?.value ? null : "CORE_COMMON_PROFILE_GENDER_REQUIRED";
    errors.mobileNumber = mobileError(form.mobileNumber);
  }
  return errors;
};

export const hasErrors = (errors) => Object.values(errors).some(Boolean);