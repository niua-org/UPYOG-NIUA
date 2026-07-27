// Prefer the current MDMS fieldType contract while accepting legacy type configs.
export const getFieldType = (field) => field.fieldType ?? field.type ?? "text";

// Mask all but the final four digits before displaying an OTP destination.
export const maskMobileNumber = (mobileNumber) => {
  const value = String(mobileNumber || "").replace(/\D/g, "");
  if (!value) return "";
  return `${"X".repeat(Math.max(value.length - 4, 0))}${value.slice(-4)}`;
};

// Supports legacy string sources and the preferred { type, key } declaration.
export const getDataSourceKey = (field) => {
  const source = field.dataSource ?? field.optionsSource;
  if (typeof source === "string") return source;
  if (source && typeof source === "object") return source.key ?? source.name ?? source.type;
  return field.name;
};

// Option sources can customize their value property while common DIGIT shapes remain supported.
export const getOptionValue = (option, field = {}) => {
  if (option === null || option === undefined || typeof option !== "object") return option;
  return option[field.valueKey] ?? option.value ?? option.code;
};

// Resolve the label used by buttons/dropdowns across static, tenant, and API option shapes.
export const getOptionLabel = (option, field = {}) => {
  if (option === null || option === undefined || typeof option !== "object") return String(option ?? "");
  return option[field.labelKey] ?? option.label ?? option.i18nKey ?? option.name ?? option.code;
};

const isEmpty = (value) =>
  value === undefined || value === null || (typeof value === "string" && value.trim() === "");

const getAge = (dateValue) => {
  const dateOfBirth = new Date(`${dateValue}T00:00:00`);
  if (Number.isNaN(dateOfBirth.getTime())) return null;

  const today = new Date();
  let age = today.getFullYear() - dateOfBirth.getFullYear();
  const hasNotHadBirthday =
    today.getMonth() < dateOfBirth.getMonth() ||
    (today.getMonth() === dateOfBirth.getMonth() && today.getDate() < dateOfBirth.getDate());

  if (hasNotHadBirthday) age -= 1;
  return age;
};

/**
 * Converts MDMS validation metadata into a React Hook Form-compatible result:
 * an empty string means valid; a string message means invalid.
 */
export const validateField = (field, value) => {
  const validation = field.validation ?? {};
  const messages = validation.messages ?? {};
  const comparableValue = typeof value === "object" ? getOptionValue(value, field) : value;

  // A dropdown can temporarily yield an object without its configured value
  // (for a city, without `code`). Treat that as empty so required validation
  // remains inline and the form never calls its submit adapter with no city.
  if (validation.required && (isEmpty(value) || isEmpty(comparableValue))) {
    return messages.required || `${field.label || field.name} is required`;
  }
  if (isEmpty(value) || isEmpty(comparableValue)) return "";

  const stringValue = String(comparableValue ?? "");
  if (validation.minLength && stringValue.length < validation.minLength) {
    return messages.minLength || `Minimum ${validation.minLength} characters are required`;
  }
  if (validation.maxLength && stringValue.length > validation.maxLength) {
    return messages.maxLength || `Maximum ${validation.maxLength} characters are allowed`;
  }
  if (validation.pattern) {
    try {
      if (!new RegExp(validation.pattern).test(stringValue)) return messages.pattern || "Please enter a valid value";
    } catch {
      return messages.invalid || "The configured validation pattern is invalid";
    }
  }
  if (getFieldType(field) === "date") {
    const age = getAge(stringValue);
    if (age === null) return messages.invalid || "Please enter a valid date";
    if (validation.minimumAge !== undefined && age < validation.minimumAge) {
      return messages.minimumAge || `Minimum age should be ${validation.minimumAge} years`;
    }
  }

  return "";
};

// Submit only fields belonging to the active step. Onboarding context also
// contains cross-step values and private flow markers that must not leak into
// the form payload.
export const buildSubmittedData = (fields = [], values = {}) =>
  fields.reduce((data, field) => {
    if (values[field.name] !== undefined) {
      data[field.name] = values[field.name];
    } else if (field.defaultValue !== undefined) {
      data[field.name] = field.defaultValue;
    }
    return data;
  }, {});
