import React, { useState, useCallback } from "react";
import { SubmitBar } from "@nudmcdgnpm/digit-ui-react-components";
import DynamicFormField from "./DynamicFormField";

/**
 * DynamicForm
 * Drives an entire form from the FormConfig JSON route definition.
 *
 * Usage:
 *   import formConfig from "../../configs/FormConfig.json";
 *   const routeConfig = formConfig.estModule[0].routes[0]; // newRegistration
 *
 *   <DynamicForm
 *     routeConfig={routeConfig}
 *     initialData={{}}          // pre-fill for edit mode
 *     dropdownData={dropdownData}
 *     onSubmit={handleSubmit}
 *     isEditMode={false}
 *     t={t}
 *   />
 *
 * @param {Object}   routeConfig   - A single routes[] item from FormConfig
 * @param {Object}   initialData   - Pre-populated values { fieldName: value }
 * @param {Object}   dropdownData  - { EST_CITY: [{code, i18nKey}], ... }
 * @param {Function} onSubmit      - (formData) => void   called on valid submit
 * @param {Boolean}  isEditMode    - switches button label to "edit" variant
 * @param {Boolean}  isDisabled    - makes every field read-only
 * @param {Function} t             - i18n translation function
 */
const DynamicForm = ({
  routeConfig,
  initialData = {},
  dropdownData = {},
  onSubmit,
  isEditMode = false,
  isDisabled = false,
  t = (k) => k,
}) => {
  const [formData, setFormData] = useState(initialData);
  const [errors, setErrors] = useState({});

  // ── Field change handler ────────────────────────────────────────────────
  const handleChange = useCallback((fieldName, value) => {
    setFormData((prev) => ({ ...prev, [fieldName]: value }));
    // Clear error on change
    setErrors((prev) => ({ ...prev, [fieldName]: false }));
  }, []);

  // ── Validation ──────────────────────────────────────────────────────────
  const validate = () => {
    const newErrors = {};

    const validateField = (fieldConfig) => {
      // Recurse into group children
      if (fieldConfig.type === "group") {
        (fieldConfig.children || []).forEach(validateField);
        return;
      }

      const { field, validation = {} } = fieldConfig;
      if (!field) return;

      const { name } = field;
      const value = formData[name] ?? "";

      if (validation.required && !value) {
        newErrors[name] = true;
        return;
      }

      if (value && validation.pattern) {
        const regex = new RegExp(validation.pattern);
        if (!regex.test(value)) {
          newErrors[name] = true;
          return;
        }
      }

      if (value && validation.maxLength && value.length > validation.maxLength) {
        newErrors[name] = true;
      }
    };

    (routeConfig.form || []).forEach(validateField);
    return newErrors;
  };

  // ── Submit ──────────────────────────────────────────────────────────────
  const handleSubmit = () => {
    const validationErrors = validate();
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }
    onSubmit && onSubmit(formData);
  };

  // ── Sort fields by order ────────────────────────────────────────────────
  const sortedFields = [...(routeConfig.form || [])].sort((a, b) => a.order - b.order);

  const buttonLabel = isEditMode
    ? routeConfig.actionButton?.text?.edit || "UPDATE"
    : routeConfig.actionButton?.text?.create || "SAVE & NEXT";

  return (
    <div className="dynamic-form-container">
      {sortedFields.map((fieldConfig) => (
        <DynamicFormField
          key={fieldConfig.key}
          fieldConfig={fieldConfig}
          formData={formData}
          onChange={handleChange}
          errors={errors}
          dropdownData={dropdownData}
          t={t}
          isDisabled={isDisabled}
        />
      ))}

      {!isDisabled && (
        <div className="Dynamic-form-action">
          <SubmitBar
            label={t(buttonLabel)}
            onSubmit={handleSubmit}
            variant={routeConfig.actionButton?.variant || "contained"}
          />
        </div>
      )}
    </div>
  );
};

export default DynamicForm;
