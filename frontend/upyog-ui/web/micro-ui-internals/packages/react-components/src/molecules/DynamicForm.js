import React, { useState, useCallback } from "react";
import { SubmitBar } from "@nudmcdgnpm/digit-ui-react-components";
import DynamicFormField from "./DynamicFormField";

const DynamicForm = ({
  routeConfig,
  initialData = {},
  dropdownData = {},
  onSubmit,
  onSelect,        // ← for "go next" flow (non-edit)
  config,          // ← route config key e.g. { key: "assetDetails" }
  isEditMode = false,
  isDisabled = false,
  updateMutation,  // ← mutation hook for edit flow
  editData = {},   // ← existing record data for edit flow
  tenantId = "",
  t = (k) => k,
}) => {
  const [formData, setFormData] = useState(initialData);
  const [errors, setErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [dimensionError, setDimensionError] = useState(false);

  // ── Field change handler ──────────────────────────────────────────────
  const handleChange = useCallback((fieldName, value, resetFields = []) => {
    setFormData((prev) => {
      const updated = { ...prev, [fieldName]: value };
      resetFields.forEach((f) => { updated[f] = null; });
      return updated;
    });
    setErrors((prev) => {
      const updated = { ...prev, [fieldName]: false };
      resetFields.forEach((f) => { updated[f] = false; });
      return updated;
    });
  }, []);

  // ── Validation ────────────────────────────────────────────────────────
  const validate = useCallback(() => {
    const newErrors = {};

    const validateField = (fieldConfig) => {
      // Recurse into groups
      if (fieldConfig.type === "group") {
        (fieldConfig.children || []).forEach(validateField);
        return;
      }

      const { field, validation = {} } = fieldConfig;
      if (!field) return;

      const { name, type } = field;
      const value = formData[name];

      if (validation.required) {
        const isEmpty =
          type === "dropdown"
            ? !value || !value.code
            : value === undefined || value === null || String(value).trim() === "";

        if (isEmpty) {
          newErrors[name] = true;
          return;
        }
      }

      // Pattern check for text fields
      if (value && type !== "dropdown" && validation.pattern) {
        if (!new RegExp(validation.pattern).test(value)) {
          newErrors[name] = true;
          return;
        }
      }

      // MaxLength check
      if (value && validation.maxLength && String(value).length > validation.maxLength) {
        newErrors[name] = true;
      }
    };

    (routeConfig.form || []).forEach(validateField);
    return newErrors;
  }, [formData, routeConfig]);

  // ── Build payload ─────────────────────────────────────────────────────
  // Flattens dropdown objects { code, name } → just the code string
  // You can customise this per your API contract
  const buildPayload = useCallback(() => {
    return Object.entries(formData).reduce((acc, [key, val]) => {
      if (val && typeof val === "object" && val.code !== undefined) {
        // Dropdown field — expand to both code and name for convenience
        acc[key] = val.code;
        acc[`${key}Name`] = val.name || "";
      } else {
        acc[key] = val ?? "";
      }
      return acc;
    }, {});
  }, [formData]);

  // ── goNext ────────────────────────────
  const goNext = useCallback(() => {
    // ── Step 1: Field-level validation
    const validationErrors = validate();
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      setTimeout(() => {
        const firstError = document.querySelector(".field-error");
        if (firstError) {
          firstError.scrollIntoView({ behavior: "smooth", block: "center" });
        }
      }, 100);
      return;
    }

    // ── Step 2: Cross-field business validation
    // Length × Width must not exceed Total Plot Area
    const length = parseFloat(formData.dimensionLength) || 0;
    const width = parseFloat(formData.dimensionWidth) || 0;
    const totalArea = parseFloat(formData.totalFloorArea) || 0;
    const computedArea = length * width;

    if (totalArea > 0 && computedArea > totalArea) {
      setErrors((prev) => ({
        ...prev,
        dimensionLength: true,
        dimensionWidth: true,
      }));
      // Show error inside form — no alert()
      setDimensionError(true);
      return;
    }

    setDimensionError(false);

    // ── Step 3: Build payload
    const formVal = buildPayload();

    const  payload = { Assets: [formVal] }
    payload.tenantId="pg.citya"
    
    console.log('payload in dyanmic form    ',payload)


    // ── Step 4: Edit vs Create flow
    if (isEditMode) {
      if (!updateMutation) {
        console.error("updateMutation is required in edit mode");
        return;
      }

      setIsSubmitting(true);

      const updatePayload = {
        Assets: {
          ...payload,
          id: editData.id,
          estateNo: editData.estateNo,
          tenantId,
        },
      };

      updateMutation.mutate(updatePayload, {
        onSuccess: (data) => {
          setIsSubmitting(false);
          onSubmit && onSubmit({ payload, response: data, isEditMode: true });
        },
        onError: (error) => {
          setIsSubmitting(false);
          console.error("Update failed:", error);
          onSubmit && onSubmit({ payload, error, isEditMode: true });
        },
      });

    } else {
      // Create flow — go to next screen
      if (onSelect) {
        console.log('in save form')
        onSelect(config?.key, { Assets: [formVal] }, false);
      }
      onSubmit && onSubmit({ payload, isEditMode: false });
    }
  }, [
    validate,
    buildPayload,
    formData,
    isEditMode,
    updateMutation,
    editData,
    tenantId,
    onSelect,
    onSubmit,
    config,
  ]);

  const sortedFields = [...(routeConfig.form || [])].sort(
    (a, b) => (a.order ?? 0) - (b.order ?? 0)
  );

  const buttonLabel = isEditMode
    ? routeConfig.actionButton?.text?.edit || "UPDATE"
    : routeConfig.actionButton?.text?.create || "SAVE & NEXT";

  return (
    <div className="dynamic-form-container">
      {sortedFields.map((fieldConfig) => (
        <>
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
          {fieldConfig.key === "EST_DIMENSION" && dimensionError && (
            <p style={{ color: "red", fontSize: "12px" }}>
              {t("EST_DIMENSION_ERROR_LENGTH_WIDTH_EXCEEDS_PLOT_AREA")}
            </p>
          )}
        </>
      ))}

      {!isDisabled && (
        <div className="dynamic-form-action" style={{ marginTop: "24px" }}>
          <SubmitBar
            label={t(buttonLabel)}
            onSubmit={goNext}
            disabled={isSubmitting}
            variant={routeConfig.actionButton?.variant || "contained"}
          />
        </div>
      )}
    </div>
  );
};

export default DynamicForm;