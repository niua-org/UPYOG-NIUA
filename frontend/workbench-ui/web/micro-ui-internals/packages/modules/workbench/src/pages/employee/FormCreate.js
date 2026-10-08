import {
  Header,
  Card,
  CardLabel,
  CardLabelError,
  TextInput,
  Dropdown,
  ToggleSwitch,
  SubmitBar,
  ActionBar,
  Button,
  SVG,
  CloseSvg,
  PopUp,
  Toast,
} from "@upyog/workbench-ui-react-components";
import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { useLocation } from "react-router-dom";

// Available Form Field Type Options for the Form Builder Canvas (Includes Password Input)
const FIELD_TYPES = [
  { name: "Text Input", code: "text" },
  { name: "Password Input", code: "password" },
  { name: "Dropdown Select", code: "dropdown" },
  { name: "Radio Buttons", code: "radio" },
  { name: "Checkbox", code: "checkbox" },
  { name: "Date Picker", code: "date" },
  { name: "Text Area", code: "textarea" },
  { name: "File Upload", code: "file" },
];

// Requirement Options: Required vs Optional
const REQUIREMENT_OPTIONS = [
  { name: "Required (Mandatory)", code: "required" },
  { name: "Optional", code: "optional" },
];

// Predefined Regex Options for Pattern Validation
const PREDEFINED_REGEX_OPTIONS = [
  { name: "None (No Regex)", code: "none", pattern: "" },
  { name: "Email Address", code: "email", pattern: "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$" },
  { name: "Numeric Only (Digits)", code: "numeric", pattern: "^[0-9]+$" },
  { name: "Alphanumeric", code: "alphanumeric", pattern: "^[a-zA-Z0-9]+$" },
  { name: "Mobile Number (10 Digits)", code: "mobile", pattern: "^[6-9]\\d{9}$" },
  { name: "PAN Card", code: "pan", pattern: "^[A-Z]{5}[0-9]{4}[A-Z]{1}$" },
  { name: "Aadhaar Number (12 Digits)", code: "aadhaar", pattern: "^\\d{12}$" },
  { name: "Custom Regex Pattern", code: "custom", pattern: "" },
];

// File Upload Mode Options: Single File vs Multiple Files
const UPLOAD_MODE_OPTIONS = [
  { name: "Single File", code: "single" },
  { name: "Multiple Files", code: "multiple" },
];

// Allowed File Type / Format Options for File Upload
const FILE_TYPE_OPTIONS = [
  { name: "PDF Only (.pdf)", code: "pdf" },
  { name: "Images Only (.jpg, .jpeg, .png)", code: "image" },
  { name: "PDF & Images (.pdf, .jpg, .png)", code: "pdf_image" },
  { name: "Word Documents (.doc, .docx)", code: "doc" },
  { name: "Spreadsheets (.xls, .xlsx, .csv)", code: "excel" },
  { name: "All Supported Formats", code: "all" },
];

// File Size Limit Options (in Megabytes - MB)
const FILE_SIZE_OPTIONS = [
  { name: "1 MB", code: "1" },
  { name: "2 MB", code: "2" },
  { name: "5 MB", code: "5" },
  { name: "10 MB", code: "10" },
  { name: "25 MB", code: "25" },
];

const FormCreate = () => {
  const { t } = useTranslation();
  const location = useLocation();
  const navigate = Digit.Hooks.useCustomNavigate();

  // Read query params from URL
  const queryParams = Digit.Hooks.useQueryParams();
  const stateData = location?.state || {};

  // Detect Edit Mode vs Copy Mode vs New Mode
  const isEdit = queryParams?.isEdit === "true" || stateData?.isEdit || false;
  const isCopy = queryParams?.isCopy === "true" || stateData?.isCopy || false;

  // Resolved parameters passed from popup / table row
  const moduleName = queryParams?.moduleName || stateData?.moduleName || "Not Specified";
  const accordionName = queryParams?.accordionName || stateData?.accordionName || "Not Specified";
  const formName = queryParams?.formName || stateData?.formName || "Not Specified";

  const cityResponseObject = Digit.Hooks.useEnabledMDMS(
    "PG",
    "ASSET",
    [
      {
        name: "AssetParentCategoryFields",
      },
    ],
    {
      select: (data) => {
        const formattedData = data?.["ASSET"]?.["AssetParentCategoryFields"];
        return formattedData;
      },
    },
  );

  console.log("MDMS AssetParentCategoryFields Response: ", cityResponseObject?.data);
  // Helper function to auto-generate Localization Key based on Module Name and Field Label
  // Format: WBH_<MODULE_NAME>_<FIELD_LABEL> (e.g. WBH_TRADE_LICENCE_APPLICANT_NAME)
  const generateLocalizationKey = (module, label) => {
    if (!label) return "";
    const cleanModule = (module && module !== "Not Specified" ? module : "COMMON")
      .trim()
      .toUpperCase()
      .replace(/[^A-Z0-9]/g, "_")
      .replace(/_+/g, "_")
      .replace(/^_+|_+$/g, "");

    const cleanLabel = label
      .trim()
      .toUpperCase()
      .replace(/[^A-Z0-9]/g, "_")
      .replace(/_+/g, "_")
      .replace(/^_+|_+$/g, "");

    return `${cleanModule}_${cleanLabel}`;
  };

  // Initial field configuration state depending on Edit vs Copy vs New mode
  const getInitialFields = () => {
    if (stateData?.existingFields && Array.isArray(stateData.existingFields) && stateData.existingFields.length > 0) {
      return stateData.existingFields.map((item, idx) => {
        const fieldObj = item.field || {};
        const validationObj = item.validation || {};
        const dataSourceObj = fieldObj.dataSource || {};

        return {
          id: item.id || `field_${Date.now()}_${idx}`,
          label: item.label || fieldObj.name || fieldObj.code || `Field ${idx + 1}`,
          fieldKey: fieldObj.code || item.fieldKey || item.key || `field_${idx + 1}`,
          type: fieldObj.type || item.type || "text",
          required: (validationObj.required ?? (item.required === "required")) ? "required" : "optional",
          isReadonly: validationObj.readOnly ?? item.isReadonly ?? false,
          isDisabled: validationObj.disabled ?? item.isDisabled ?? false,
          placeholder: fieldObj.placeholder || item.placeholder || "",
          minLength: validationObj.minLength || item.minLength || "",
          maxLength: validationObj.maxLength || item.maxLength || "",
          regexType: item.regexType || "none",
          pattern: validationObj.pattern || item.pattern || "",
          patternErrorMessage: item.patternErrorMessage || item.messages?.error || "",
          heading: item.heading || fieldObj.heading || "",
          paragraph: item.paragraph || fieldObj.paragraph || "",
          // MDMS Dropdown Specific Settings
          mdmsModuleName: dataSourceObj.moduleName || item.mdmsModuleName || moduleName,
          mdmsMasterName: dataSourceObj.masterName || item.mdmsMasterName || "",
          // Radio / Checkbox Options
          options: Array.isArray(fieldObj.options)
            ? fieldObj.options
            : Array.isArray(item.options)
              ? item.options
              : ["radio", "checkbox"].includes(fieldObj.type || item.type)
                ? [
                  { code: "OPTION_1", name: "Option 1" },
                  { code: "OPTION_2", name: "Option 2" },
                ]
                : undefined,
          // File Upload Specific Settings
          uploadMode: fieldObj.uploadMode || item.uploadMode || "single",
          allowedFileTypes: fieldObj.allowedFileTypes || item.allowedFileTypes || "pdf_image",
          maxFileSize: fieldObj.maxFileSize || item.maxFileSize || "5",
          maxFileCount: fieldObj.maxFileCount || item.maxFileCount || "5",
          fileErrorMessage: item.fileErrorMessage || item.messages?.error || "Allowed formats: PDF, JPG, PNG up to 5MB",
        };
      });
    }
    if (isEdit || isCopy) {
      // Pre-populated schema fields for EDIT / COPY case
      return [
        {
          id: "field_101",
          label: "Applicant Full Name",
          fieldKey: generateLocalizationKey(moduleName, "Applicant Full Name"),
          type: "text",
          required: "required",
          placeholder: "Enter full name",
          minLength: "3",
          maxLength: "100",
          regexType: "alphanumeric",
          pattern: "^[a-zA-Z0-9 ]+$",
          patternErrorMessage: "Only letters and numbers are allowed",
        },
        {
          id: "field_102",
          label: "Mobile Number",
          fieldKey: generateLocalizationKey(moduleName, "Mobile Number"),
          type: "text",
          required: "required",
          placeholder: "Enter 10 digit mobile number",
          minLength: "10",
          maxLength: "10",
          regexType: "mobile",
          pattern: "^[6-9]\\d{9}$",
          patternErrorMessage: "Please enter a valid 10-digit mobile number",
        },
        {
          id: "field_103",
          label: "Trade Details Category",
          fieldKey: generateLocalizationKey(moduleName, "Trade Details Category"),
          type: "dropdown",
          required: "optional",
          placeholder: "Select category",
          minLength: "",
          maxLength: "",
          regexType: "none",
          pattern: "",
          patternErrorMessage: "",
        },
      ];
    }
    // Default single text field for NEW case when opened for the first time
    return [
      {
        id: "field_1",
        label: "Text Field 1",
        fieldKey: generateLocalizationKey(moduleName, "Text Field 1"),
        type: "text",
        required: "required",
        placeholder: "Enter text value",
        minLength: "",
        maxLength: "",
        regexType: "none",
        pattern: "",
        patternErrorMessage: "",
      },
    ];
  };

  const [fields, setFields] = useState(getInitialFields);
  const [toast, setToast] = useState(null);
  const [showPreviewModal, setShowPreviewModal] = useState(false);
  const [previewFormData, setPreviewFormData] = useState({});

  // Check if field label is a duplicate among form fields
  const isDuplicateFieldLabel = (currentField) => {
    if (!currentField?.label || !currentField.label.trim()) return false;
    const cleanLabel = currentField.label.trim().toLowerCase();
    return fields.some((f) => f.id !== currentField.id && (f.label || "").trim().toLowerCase() === cleanLabel);
  };

  // Check if field key / ID is a duplicate among form fields
  const isDuplicateFieldKey = (currentField) => {
    if (!currentField?.fieldKey || !currentField.fieldKey.trim()) return false;
    const cleanKey = currentField.fieldKey.trim().toLowerCase();
    return fields.some((f) => f.id !== currentField.id && (f.fieldKey || "").trim().toLowerCase() === cleanKey);
  };

  // Add new field to the canvas
  const handleAddField = (fieldType) => {
    const isFile = fieldType.code === "file";
    const isDropdown = fieldType.code === "dropdown";
    const isOptionsField = ["radio", "checkbox"].includes(fieldType.code);

    // Auto-generate unique field label to prevent duplicate names on field addition
    const baseLabel = `New ${fieldType.name}`;
    let fieldLabel = baseLabel;
    let counter = 1;
    while (fields.some((f) => (f.label || "").trim().toLowerCase() === fieldLabel.trim().toLowerCase())) {
      fieldLabel = `${baseLabel} ${counter}`;
      counter++;
    }

    const newField = {
      id: `field_${Date.now()}`,
      label: fieldLabel,
      fieldKey: generateLocalizationKey(moduleName, fieldLabel),
      type: fieldType.code,
      required: "required",
      isReadonly: false,
      isDisabled: false,
      placeholder: `Enter ${fieldType.name.toLowerCase()}`,
      minLength: "",
      maxLength: "",
      heading: "",
      paragraph: "",
      regexType: "none",
      pattern: "",
      patternErrorMessage: "",
      // Radio & Checkbox Options
      options: isOptionsField
        ? [
          { code: "OPTION_1", name: "Option 1" },
          { code: "OPTION_2", name: "Option 2" },
        ]
        : undefined,
      // MDMS Dropdown Specific Settings
      mdmsModuleName: isDropdown ? moduleName : undefined,
      mdmsMasterName: isDropdown ? "" : undefined,
      // File Upload Specific Settings
      uploadMode: isFile ? "single" : undefined,
      allowedFileTypes: isFile ? "pdf_image" : undefined,
      maxFileSize: isFile ? "5" : undefined,
      maxFileCount: isFile ? "5" : undefined,
      fileErrorMessage: isFile ? "Allowed formats: PDF, JPG, PNG up to 5MB" : undefined,
    };
    setFields((prev) => [...prev, newField]);
    setToast({ label: t("WBH_FIELD_ADDED_SUCCESS") || `Added new ${fieldType.name} field`, error: false });
  };

  // Manage Radio / Checkbox Options: Add new option
  const handleAddOption = (fieldId) => {
    setFields((prev) =>
      prev.map((field) => {
        if (field.id === fieldId) {
          const currentOptions = Array.isArray(field.options) ? field.options : [];
          const newOptNum = currentOptions.length + 1;
          const newOption = {
            code: `OPTION_${newOptNum}`,
            name: `Option ${newOptNum}`,
          };
          return { ...field, options: [...currentOptions, newOption] };
        }
        return field;
      })
    );
  };

  // Manage Radio / Checkbox Options: Update option label/code
  const handleOptionChange = (fieldId, optIndex, key, value) => {
    setFields((prev) =>
      prev.map((field) => {
        if (field.id === fieldId) {
          const currentOptions = Array.isArray(field.options) ? [...field.options] : [];
          if (currentOptions[optIndex]) {
            currentOptions[optIndex] = {
              ...currentOptions[optIndex],
              [key]: value,
            };
          }
          return { ...field, options: currentOptions };
        }
        return field;
      })
    );
  };

  // Manage Radio / Checkbox Options: Delete option
  const handleRemoveOption = (fieldId, optIndex) => {
    setFields((prev) =>
      prev.map((field) => {
        if (field.id === fieldId) {
          const currentOptions = Array.isArray(field.options) ? field.options.filter((_, i) => i !== optIndex) : [];
          return { ...field, options: currentOptions };
        }
        return field;
      })
    );
  };

  // Remove field from canvas
  const handleRemoveField = (fieldId) => {
    setFields((prev) => prev.filter((f) => f.id !== fieldId));
  };

  // Handle Field Ordering - Move field UP or DOWN in sequence
  const handleMoveField = (index, direction) => {
    if (direction === "UP" && index > 0) {
      setFields((prev) => {
        const updated = [...prev];
        const temp = updated[index];
        updated[index] = updated[index - 1];
        updated[index - 1] = temp;
        return updated;
      });
    } else if (direction === "DOWN" && index < fields.length - 1) {
      setFields((prev) => {
        const updated = [...prev];
        const temp = updated[index];
        updated[index] = updated[index + 1];
        updated[index + 1] = temp;
        return updated;
      });
    }
  };

  // Update specific field properties with auto-generating localization key when label changes
  const handleFieldChange = (fieldId, key, value) => {
    console.log("Testing :- ", fieldId, key, value);
    setFields((prev) =>
      prev.map((field) => {
        if (field.id === fieldId) {
          const updatedField = { ...field, [key]: value };
          if (key === "label") {
            updatedField.fieldKey = generateLocalizationKey(moduleName, value);
          }
          return updatedField;
        }
        return field;
      })
    );
  };

  // Handle Predefined Regex Dropdown Selection
  const handleRegexTypeChange = (fieldId, selectedOption) => {
    const code = selectedOption?.code || "none";
    const presetPattern = selectedOption?.pattern || "";
    let defaultErrorMsg = "";

    if (code === "email") defaultErrorMsg = "Please enter a valid email address";
    else if (code === "numeric") defaultErrorMsg = "Only numbers/digits are allowed";
    else if (code === "alphanumeric") defaultErrorMsg = "Only letters and numbers are allowed";
    else if (code === "mobile") defaultErrorMsg = "Please enter a valid 10-digit mobile number";
    else if (code === "pan") defaultErrorMsg = "Please enter a valid PAN Card number (e.g. ABCDE1234F)";
    else if (code === "aadhaar") defaultErrorMsg = "Please enter a valid 12-digit Aadhaar number";

    setFields((prev) =>
      prev.map((field) => {
        if (field.id === fieldId) {
          return {
            ...field,
            regexType: code,
            pattern: code === "custom" ? field.pattern : presetPattern,
            patternErrorMessage: defaultErrorMsg || field.patternErrorMessage,
          };
        }
        return field;
      })
    );
  };

  // Save form configuration schema (Generates UPYOG Standard Single Field MDMS/Config JSON)
  const handleSaveSchema = () => {
    // 1. Validate Duplicate Field Names / Labels or Keys
    const labelSet = new Set();
    const keySet = new Set();
    let duplicateLabelName = null;
    let duplicateKeyName = null;

    for (const field of fields) {
      const normLabel = (field.label || "").trim().toLowerCase();
      const normKey = (field.fieldKey || "").trim().toLowerCase();

      if (!normLabel) {
        setToast({
          label: t("WBH_EMPTY_FIELD_LABEL_ERROR") || "Field Label cannot be empty. Please specify a label for all fields.",
          error: true,
        });
        return;
      }

      if (labelSet.has(normLabel)) {
        duplicateLabelName = field.label;
        break;
      }
      if (normKey && keySet.has(normKey)) {
        duplicateKeyName = field.fieldKey;
        break;
      }

      labelSet.add(normLabel);
      if (normKey) keySet.add(normKey);
    }

    if (duplicateLabelName) {
      setToast({
        label: t("WBH_DUPLICATE_FIELD_LABEL_ERROR") || `Duplicate field name found: "${duplicateLabelName}". Each field must have a unique name.`,
        error: true,
      });
      return;
    }

    if (duplicateKeyName) {
      setToast({
        label: t("WBH_DUPLICATE_FIELD_KEY_ERROR") || `Duplicate field key found: "${duplicateKeyName}". Each field key must be unique.`,
        error: true,
      });
      return;
    }

    const stateCode = Digit.ULBService.getStateId ? Digit.ULBService.getStateId() : "pg";
    const formattedKeyName = formName ? formName.charAt(0).toLowerCase() + formName.slice(1) : "form";

    const formFieldsArray = fields.map((field, idx) => {
      const fieldItem = {
        order: idx + 1,
        key: field.fieldKey || `field_${idx + 1}`,
        heading: field.heading || "",
        paragraph: field.paragraph || "",
        field: {
          code: field.fieldKey || `field_${idx + 1}`,
          name: field.fieldKey || `field_${idx + 1}`,
          heading: field.heading || "",
          paragraph: field.paragraph || "",
          placeholder: field.placeholder || "",
          type: field.type,
          ...(field.type === "dropdown" && {
            dataSource: {
              type: "MDMS",
              moduleName: field.mdmsModuleName || moduleName || "",
              masterName: field.mdmsMasterName || "",
              customiztionRequired: true,
            },
          }),
          ...(["radio", "checkbox"].includes(field.type) && {
            options: (Array.isArray(field.options) && field.options.length > 0 ? field.options : [
              { code: "OPTION_1", name: "Option 1" },
              { code: "OPTION_2", name: "Option 2" },
            ]).map((opt) => ({
              code: opt.code || opt.name || "",
              name: opt.name || opt.code || "",
            })),
          }),
          ...(field.type === "file" && {
            uploadMode: field.uploadMode || "single",
            allowedFileTypes: field.allowedFileTypes || "pdf_image",
            maxFileSize: field.maxFileSize ? Number(field.maxFileSize) : 5,
            maxFileCount: field.uploadMode === "multiple" ? (field.maxFileCount ? Number(field.maxFileCount) : 5) : 1,
          }),
        },
        validation: {
          required: field.required === "required",
          disabled: !!field.isDisabled,
          readOnly: !!field.isReadonly,
          ...(field.minLength ? { minLength: Number(field.minLength) } : {}),
          ...(field.maxLength ? { maxLength: Number(field.maxLength) } : {}),
          ...(field.pattern ? { pattern: field.pattern } : {}),
        },
        messages: {
          error:
            field.type === "file"
              ? field.fileErrorMessage || "Invalid file format or size"
              : field.patternErrorMessage || "Invalid field value",
        },
      };
      return fieldItem;
    });

    // Complete Standard UPYOG Form JSON Schema structure
    const formSchemaJSON = {
      tenantId: stateCode,
      moduleName: moduleName,
      [formName]: [
        {
          head: accordionName,
          body: [
            {
              key: formattedKeyName,
              route: formattedKeyName,
              component: formName,
              nextStep: null,
              isPreview: false,
              withoutLabel: true,
              type: "component",
              hideInEmployee: false,
              isMandatory: true,
              sectionHeading: null,
              payloadKey: moduleName,
              apiId: "Rainmaker",
              form: formFieldsArray,
            },
          ],
        },
      ],
    };

    console.log(`${isEdit ? "Updating" : "Saving"} Full Form Schema JSON Payload: `, JSON.stringify(formSchemaJSON, null, 2));

    setToast({
      label: isEdit
        ? t("WBH_FORM_SCHEMA_UPDATED") || "Form Configuration Schema updated successfully!"
        : t("WBH_FORM_SCHEMA_SAVED") || "Form Configuration Schema saved successfully!",
      error: false,
    });
  };

  return (
    <React.Fragment>
      {/* Header and Top Action Bar */}
      <div className="jk-header-btn-wrapper form-builder-header-wrapper">
        <Header className="works-header-search">
          {isEdit
            ? t("WBH_EDIT_FORM_CONFIG_BUILDER") || "Edit Form Field Configuration"
            : isCopy
              ? t("WBH_COPY_FORM_CONFIG_BUILDER") || "Copy Form Field Configuration (Cloned Scope)"
              : t("WBH_FORM_CONFIG_BUILDER") || "Form Field Configuration Builder"}
        </Header>
        <div className="header-action-bar-container">
          <Button
            type="button"
            variation="secondary"
            className="header-btn-back-to-forms"
            label={t("WBH_PREVIEW_FORM") || "👁️ Preview Form"}
            onButtonClick={() => setShowPreviewModal(true)}
          />
          <Button
            type="button"
            variation="secondary"
            className="header-btn-back-to-forms"
            label={t("WBH_BACK_TO_LIST") || "← Back to Forms"}
            onButtonClick={() => navigate(`/${window?.contextPath}/employee/workbench/form-builder`)}
          />
        </div>
      </div>

      {/* Selected Parameters Context Banner */}
      <Card className="form-context-banner-card">
        <div className="form-context-header">
          <span className="context-title-badge">
            {isEdit
              ? `✏️ ${t("WBH_EDITING_CONFIG_SCOPE") || "Editing Scope Parameters (Edit Mode)"}`
              : isCopy
                ? `📋 ${t("WBH_COPYING_CONFIG_SCOPE") || "Copied Scope Parameters (Copy Mode)"}`
                : `📋 ${t("WBH_SELECTED_CONFIG_SCOPE") || "Configured Scope Parameters (New Mode)"}`}
          </span>
        </div>
        <div className="form-context-grid">
          <div className="context-item-box">
            <span className="context-item-label">{t("WBH_MODULE_NAME") || "Module Name"}</span>
            <span className="context-item-value highlight-blue">{moduleName}</span>
          </div>
          <div className="context-item-box">
            <span className="context-item-label">{t("WBH_ACCORDION_NAME") || "Accordion Section"}</span>
            <span className="context-item-value highlight-orange">{accordionName}</span>
          </div>
          <div className="context-item-box">
            <span className="context-item-label">{t("WBH_FORM_NAME") || "Form Name"}</span>
            <span className="context-item-value highlight-green">{formName}</span>
          </div>
        </div>
      </Card>

      {/* Main Form Builder Workspace */}
      <div className="form-builder-workspace-grid">
        {/* Left Field Palette Sidebar */}
        <Card className="field-palette-sidebar">
          <h3 className="palette-title">{t("WBH_ADD_FIELD_TYPES") || "Add Form Fields"}</h3>
          <p className="palette-subtitle">{t("WBH_CLICK_TO_ADD_FIELD") || "Click any field type below to insert into form canvas"}</p>

          <div className="palette-buttons-list">
            {FIELD_TYPES.map((type) => (
              <button
                key={type.code}
                type="button"
                className="palette-field-btn"
                onClick={() => handleAddField(type)}
              >
                <span className="palette-btn-icon">+</span>
                <span>{type.name}</span>
              </button>
            ))}
          </div>
        </Card>

        {/* Right Form Canvas Workspace */}
        <div className="form-canvas-workspace">
          <Card className="form-canvas-card">
            <div className="canvas-header-bar">
              <h3 className="canvas-title">{t("WBH_FORM_CANVAS") || "Form Fields Canvas"} ({fields.length} {t("WBH_FIELDS") || "fields"})</h3>
              <span className="canvas-subtitle">{t("WBH_CONFIGURE_PROPERTIES") || "Configure labels, requirement rules, and regex pattern validations"}</span>
            </div>

            {fields.length === 0 ? (
              <div className="canvas-empty-state">
                <SVG.Edit fill="#94a3b8" width="48" height="48" />
                <p>{t("WBH_NO_FIELDS_ADDED") || "No fields added yet. Click field types from the left sidebar to start building!"}</p>
              </div>
            ) : (
              <div className="canvas-fields-list">
                {fields.map((field, index) => (
                  <div key={field.id} className="canvas-field-card">
                    <div className="field-card-header">
                      <span className="field-number-tag">Field #{index + 1}</span>

                      {/* Field Ordering Re-sequence Controls */}
                      <div className="field-ordering-controls">
                        <button
                          type="button"
                          className="ordering-btn"
                          onClick={() => handleMoveField(index, "UP")}
                          disabled={index === 0}
                          title="Move Field Up"
                        >
                          ▲
                        </button>
                        <button
                          type="button"
                          className="ordering-btn"
                          onClick={() => handleMoveField(index, "DOWN")}
                          disabled={index === fields.length - 1}
                          title="Move Field Down"
                        >
                          ▼
                        </button>
                      </div>

                      <span className="field-type-badge">{field.type.toUpperCase()}</span>
                      <span className={`field-requirement-badge ${field.required === "required" ? "req-mandatory" : "req-optional"}`}>
                        {field.required === "required" ? "REQUIRED" : "OPTIONAL"}
                      </span>
                      <button
                        type="button"
                        className="field-remove-btn"
                        onClick={() => handleRemoveField(field.id)}
                        title="Remove Field"
                      >
                        ✕
                      </button>
                    </div>

                    {/* Row 1: Basic Configs */}
                    <div className="field-card-grid row-1">
                      <div className="field-config-item">
                        <CardLabel className="config-label">
                          {t("WBH_FIELD_LABEL") || "Field Label"} <span className="mandatory-asterisk">*</span>
                        </CardLabel>
                        <TextInput
                          value={field.label}
                          onChange={(e) => handleFieldChange(field.id, "label", e.target.value)}
                          placeholder="e.g. Applicant Name"
                          className={isDuplicateFieldLabel(field) ? "has-duplicate-error" : ""}
                        />
                        {isDuplicateFieldLabel(field) && (
                          <CardLabelError className="duplicate-error-msg">
                            ⚠️ {t("WBH_DUPLICATE_NAME_WARN") || "Duplicate field name! Each field must have a unique name."}
                          </CardLabelError>
                        )}
                      </div>

                      <div className="field-config-item">
                        <CardLabel className="config-label">{t("WBH_LOCALIZATION_KEY") || "Field Key / ID"}</CardLabel>
                        <TextInput
                          value={field.fieldKey}
                          onChange={(e) => handleFieldChange(field.id, "fieldKey", e.target.value)}
                          placeholder="e.g. applicantName"
                          className={isDuplicateFieldKey(field) ? "has-duplicate-error" : ""}
                        />
                        {isDuplicateFieldKey(field) && (
                          <CardLabelError className="duplicate-error-msg">
                            ⚠️ {t("WBH_DUPLICATE_KEY_WARN") || "Duplicate field key! Field ID must be unique."}
                          </CardLabelError>
                        )}
                      </div>

                      <div className="field-config-item">
                        <CardLabel className="config-label">{t("WBH_FIELD_HEADING") || "Heading"}</CardLabel>
                        <TextInput
                          value={field.heading || ""}
                          onChange={(e) => handleFieldChange(field.id, "heading", e.target.value)}
                          placeholder="e.g. Enter heading"
                        />
                      </div>

                      <div className="field-config-item">
                        <CardLabel className="config-label">{t("WBH_FIELD_PLACEHOLDER") || "Placeholder Text"}</CardLabel>
                        <TextInput
                          value={field.placeholder}
                          onChange={(e) => handleFieldChange(field.id, "placeholder", e.target.value)}
                          placeholder="e.g. Enter value"
                        />
                      </div>

                      <div className="field-config-item span-2">
                        <CardLabel className="config-label">{t("WBH_FIELD_PARAGRAPH") || "Paragraph"}</CardLabel>
                        <textarea
                          value={field.paragraph || ""}
                          onChange={(e) => handleFieldChange(field.id, "paragraph", e.target.value)}
                          placeholder="e.g. Enter paragraph text"
                          rows={2}
                          className="preview-textarea"
                        />
                      </div>

                      <div className="field-config-item">
                        <CardLabel className="config-label">{t("WBH_FIELD_REQUIREMENT") || "Requirement"}</CardLabel>
                        <div className="requirement-toggle-wrapper">
                          <ToggleSwitch
                            value={field.required === "required"}
                            onChange={(e) => handleFieldChange(field.id, "required", e.target.checked ? "required" : "optional")}
                            name={`required_${field.id}`}
                          />
                          <span className="toggle-status-label">
                            {field.required === "required" ? "true" : "false"}
                          </span>
                        </div>
                      </div>

                      <div className="field-config-item">
                        <CardLabel className="config-label">{t("WBH_FIELD_READONLY") || "Read Only"}</CardLabel>
                        <div className="requirement-toggle-wrapper">
                          <ToggleSwitch
                            value={!!field.isReadonly}
                            onChange={(e) => handleFieldChange(field.id, "isReadonly", e.target.checked)}
                            name={`readonly_${field.id}`}
                          />
                          <span className="toggle-status-label">
                            {field.isReadonly ? "true" : "false"}
                          </span>
                        </div>
                      </div>

                      <div className="field-config-item">
                        <CardLabel className="config-label">{t("WBH_FIELD_DISABLED") || "Disabled"}</CardLabel>
                        <div className="requirement-toggle-wrapper">
                          <ToggleSwitch
                            value={!!field.isDisabled}
                            onChange={(e) => handleFieldChange(field.id, "isDisabled", e.target.checked)}
                            name={`disabled_${field.id}`}
                          />
                          <span className="toggle-status-label">
                            {field.isDisabled ? "true" : "false"}
                          </span>
                        </div>
                      </div>
                    </div>

                    {/* Row 2: Validation, Length & Regex Settings */}
                    {["text", "password", "textarea"].includes(field.type) && (
                      <div className="field-validation-section">
                        <div className="validation-section-header">
                          <span>⚙️ {t("WBH_VALIDATION_LENGTH_SETTINGS") || "Length & Regex Pattern Validation Settings"}</span>
                        </div>

                        <div className="field-card-grid row-2">
                          <div className="field-config-item">
                            <CardLabel className="config-label">{t("WBH_MIN_LENGTH") || "Min Length / Min Value"}</CardLabel>
                            <TextInput
                              type="number"
                              value={field.minLength || ""}
                              onChange={(e) => handleFieldChange(field.id, "minLength", e.target.value)}
                              placeholder="e.g. 1"
                            />
                          </div>

                          <div className="field-config-item">
                            <CardLabel className="config-label">{t("WBH_MAX_LENGTH") || "Max Length / Max Value"}</CardLabel>
                            <TextInput
                              type="number"
                              value={field.maxLength || ""}
                              onChange={(e) => handleFieldChange(field.id, "maxLength", e.target.value)}
                              placeholder="e.g. 100"
                            />
                          </div>
                          <div className="field-config-item">
                            <CardLabel className="config-label">{t("WBH_PREDEFINED_REGEX") || "Pattern Validation Dropdown"}</CardLabel>
                            <Dropdown
                              option={PREDEFINED_REGEX_OPTIONS}
                              optionKey="name"
                              selected={PREDEFINED_REGEX_OPTIONS.find((r) => r.code === (field.regexType || "none"))}
                              select={(val) => handleRegexTypeChange(field.id, val)}
                              t={t}
                              placeholder="Select Regex Pattern"
                            />
                          </div>

                          <div className="field-config-item">
                            <CardLabel className="config-label">
                              {t("WBH_REGEX_PATTERN") || "Regex Pattern Rule"}
                              {field.regexType === "custom" && <span className="mandatory-asterisk"> *</span>}
                            </CardLabel>
                            <TextInput
                              value={field.pattern || ""}
                              onChange={(e) => handleFieldChange(field.id, "pattern", e.target.value)}
                              placeholder="e.g. ^[a-zA-Z0-9]+$"
                              disabled={field.regexType !== "custom" && field.regexType !== "none" && !!field.pattern}
                            />
                          </div>

                          <div className="field-config-item span-2">
                            <CardLabel className="config-label">{t("WBH_PATTERN_ERROR_MSG") || "Validation Error Message"}</CardLabel>
                            <TextInput
                              value={field.patternErrorMessage || ""}
                              onChange={(e) => handleFieldChange(field.id, "patternErrorMessage", e.target.value)}
                              placeholder="e.g. Please enter a valid value"
                            />
                          </div>
                        </div>
                      </div>
                    )}

                    {/* Row 2: MDMS Dropdown Data Source Configuration */}
                    {field.type === "dropdown" && (
                      <div className="field-validation-section">
                        <div className="validation-section-header">
                          <span>🗂️ {t("WBH_MDMS_DROPDOWN_SETTINGS") || "MDMS Dropdown Data Source Configuration"}</span>
                        </div>

                        <div className="field-card-grid row-2">
                          <div className="field-config-item">
                            <CardLabel className="config-label">
                              {t("WBH_MDMS_MODULE_NAME") || "Module Name"} <span className="mandatory-asterisk">*</span>
                            </CardLabel>
                            <TextInput
                              value={field.mdmsModuleName || ""}
                              onChange={(e) => handleFieldChange(field.id, "mdmsModuleName", e.target.value)}
                              placeholder="e.g. ASSET or PropertyTax"
                            />
                          </div>

                          <div className="field-config-item">
                            <CardLabel className="config-label">
                              {t("WBH_MDMS_MASTER_NAME") || "MDMS File Name / Master Name"} <span className="mandatory-asterisk">*</span>
                            </CardLabel>
                            <TextInput
                              value={field.mdmsMasterName || ""}
                              onChange={(e) => handleFieldChange(field.id, "mdmsMasterName", e.target.value)}
                              placeholder="e.g. AssetParentCategoryFields"
                            />
                          </div>
                        </div>
                      </div>
                    )}

                    {/* Row 2: Radio & Checkbox Options Configuration */}
                    {["radio", "checkbox"].includes(field.type) && (
                      <div className="field-validation-section">
                        <div className="validation-section-header validation-header-flex">
                          <span>🔘 {t("WBH_OPTIONS_CONFIG_HEADER") || "Radio / Checkbox Options Configuration"}</span>
                          <Button
                            type="button"
                            variation="secondary"
                            className="add-option-btn"
                            label={t("WBH_ADD_OPTION") || "+ Add Option"}
                            onButtonClick={() => handleAddOption(field.id)}
                          />
                        </div>

                        <div className="options-list-container">
                          {(Array.isArray(field.options) && field.options.length > 0 ? field.options : [
                            { code: "OPTION_1", name: "Option 1" },
                            { code: "OPTION_2", name: "Option 2" },
                          ]).map((opt, optIdx) => (
                            <div key={optIdx} className="option-item-row">
                              <span className="option-index-badge">#{optIdx + 1}</span>
                              <div className="option-field-flex">
                                <CardLabel className="config-label">
                                  {t("WBH_OPTION_NAME") || "Display Label"}
                                </CardLabel>
                                <TextInput
                                  value={opt.name || ""}
                                  onChange={(e) => handleOptionChange(field.id, optIdx, "name", e.target.value)}
                                  placeholder={t("WBH_OPTION_LABEL_PLACEHOLDER") || "e.g. Option 1 / Yes"}
                                />
                              </div>
                              <div className="option-field-flex">
                                <CardLabel className="config-label">
                                  {t("WBH_OPTION_CODE") || "Value Code"}
                                </CardLabel>
                                <TextInput
                                  value={opt.code || ""}
                                  onChange={(e) => handleOptionChange(field.id, optIdx, "code", e.target.value)}
                                  placeholder={t("WBH_OPTION_VALUE_PLACEHOLDER") || "e.g. OPTION_1 / YES"}
                                />
                              </div>
                              <button
                                type="button"
                                className="option-remove-btn"
                                onClick={() => handleRemoveOption(field.id, optIdx)}
                                title={t("WBH_REMOVE_OPTION") || "Remove Option"}
                              >
                                🗑️
                              </button>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    {/* Row 2: File Upload Specific Configuration & Validation Settings */}
                    {field.type === "file" && (
                      <div className="field-validation-section">
                        <div className="validation-section-header">
                          <span>📎 {t("WBH_FILE_UPLOAD_SETTINGS") || "File Upload Rules & Validation Settings"}</span>
                        </div>

                        <div className="field-card-grid row-2">
                          <div className="field-config-item">
                            <CardLabel className="config-label">{t("WBH_FILE_UPLOAD_MODE") || "Upload Mode (Single / Multiple)"}</CardLabel>
                            <Dropdown
                              option={UPLOAD_MODE_OPTIONS}
                              optionKey="name"
                              selected={UPLOAD_MODE_OPTIONS.find((m) => m.code === (field.uploadMode || "single"))}
                              select={(val) => handleFieldChange(field.id, "uploadMode", val?.code)}
                              t={t}
                              placeholder="Select Upload Mode"
                            />
                          </div>

                          <div className="field-config-item">
                            <CardLabel className="config-label">{t("WBH_ALLOWED_FILE_TYPES") || "File Format Validation"}</CardLabel>
                            <Dropdown
                              option={FILE_TYPE_OPTIONS}
                              optionKey="name"
                              selected={FILE_TYPE_OPTIONS.find((ft) => ft.code === (field.allowedFileTypes || "pdf_image"))}
                              select={(val) => handleFieldChange(field.id, "allowedFileTypes", val?.code)}
                              t={t}
                              placeholder="Select Allowed Formats"
                            />
                          </div>

                          <div className="field-config-item">
                            <CardLabel className="config-label">{t("WBH_MAX_FILE_SIZE") || "Max File Size Limit"}</CardLabel>
                            <Dropdown
                              option={FILE_SIZE_OPTIONS}
                              optionKey="name"
                              selected={FILE_SIZE_OPTIONS.find((fs) => fs.code === (field.maxFileSize || "5"))}
                              select={(val) => handleFieldChange(field.id, "maxFileSize", val?.code)}
                              t={t}
                              placeholder="Select Max Size"
                            />
                          </div>

                          {field.uploadMode === "multiple" && (
                            <div className="field-config-item">
                              <CardLabel className="config-label">{t("WBH_MAX_FILE_COUNT") || "Max File Count Limit"}</CardLabel>
                              <TextInput
                                type="number"
                                value={field.maxFileCount || "5"}
                                onChange={(e) => handleFieldChange(field.id, "maxFileCount", e.target.value)}
                                placeholder="e.g. 5"
                              />
                            </div>
                          )}

                          <div className="field-config-item span-2">
                            <CardLabel className="config-label">{t("WBH_FILE_ERROR_MSG") || "Validation Error Message"}</CardLabel>
                            <TextInput
                              value={field.fileErrorMessage || ""}
                              onChange={(e) => handleFieldChange(field.id, "fileErrorMessage", e.target.value)}
                              placeholder="e.g. Allowed formats: PDF, JPG, PNG up to 5MB"
                            />
                          </div>
                        </div>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </Card>
        </div>
      </div>

      {/* Fixed Bottom Action Bar */}
      <ActionBar>
        <SubmitBar
          disabled={false}
          onSubmit={handleSaveSchema}
          label={isEdit ? t("WBH_UPDATE_FORM_CONFIG") || "Update Configuration" : t("WBH_SAVE_FORM_CONFIG") || "Save Configuration"}
        />
      </ActionBar>

      {/* Form Live Preview Popup Modal */}
      {showPreviewModal && (
        <PopUp>
          <div className="create-form-modal-overlay">
            <div className="create-form-modal-container preview-modal-container">
              {/* Modal Header */}
              <div className="create-form-modal-header preview-modal-header">
                <div className="modal-title-wrapper preview-modal-title-wrapper">
                  <div className="modal-icon-badge preview-modal-badge">
                    <span className="preview-modal-icon">👁️</span>
                  </div>
                  <div>
                    <h2 className="modal-title-text preview-modal-title">
                      {t("WBH_FORM_PREVIEW") || "Form Live Preview"}: {formName}
                    </h2>
                    <p className="modal-subtitle-text preview-modal-subtitle">
                      {t("WBH_ACCORDION") || "Section"}: <strong>{accordionName}</strong> | {t("WBH_MODULE") || "Module"}: <strong>{moduleName}</strong> ({fields.length} {t("WBH_FIELDS") || "fields"})
                    </p>
                  </div>
                </div>
                <button type="button" className="modal-close-btn" onClick={() => setShowPreviewModal(false)} aria-label="Close">
                  <CloseSvg fill="#64748b" width="18" height="18" />
                </button>
              </div>

              {/* Modal Body - Interactive Form Preview */}
              <div className="preview-modal-body">
                <div className="preview-card-container">
                  <h3 className="preview-section-title">
                    {accordionName}
                  </h3>

                  {fields.length === 0 ? (
                    <div className="preview-empty-state">
                      <p>{t("WBH_NO_FIELDS_TO_PREVIEW") || "No form fields added yet. Add fields on the canvas to preview your form."}</p>
                    </div>
                  ) : (
                    <div className="preview-fields-grid">
                      {fields.map((field, idx) => {
                        const isRequired = field.required === "required";
                        const isReadOnly = !!field.isReadonly;
                        const isDisabled = !!field.isDisabled;
                        const fieldVal = previewFormData[field.id] || "";

                        return (
                          <div key={field.id} className="preview-field-item">
                            {field.heading && (
                              <h4 className="preview-field-heading" style={{ fontSize: "0.95rem", fontWeight: "600", color: "#0f172a", margin: "0 0 4px 0" }}>
                                {field.heading}
                              </h4>
                            )}
                            <CardLabel className="config-label preview-field-label">
                              {field.label || `Field ${idx + 1}`}
                              {isRequired && <span className="mandatory-asterisk"> *</span>}
                            </CardLabel>
                            {field.paragraph && (
                              <p className="preview-field-paragraph" style={{ fontSize: "0.85rem", color: "#64748b", margin: "2px 0 8px 0" }}>
                                {field.paragraph}
                              </p>
                            )}

                            {/* Render Text / Password Input */}
                            {["text", "password"].includes(field.type) && (
                              <TextInput
                                type={field.type}
                                value={fieldVal}
                                onChange={(e) => setPreviewFormData((prev) => ({ ...prev, [field.id]: e.target.value }))}
                                placeholder={field.placeholder || `Enter ${field.label || "value"}`}
                                disabled={isDisabled}
                                readOnly={isReadOnly}
                              />
                            )}

                            {/* Render Date Picker */}
                            {field.type === "date" && (
                              <TextInput
                                type="date"
                                value={fieldVal}
                                onChange={(e) => setPreviewFormData((prev) => ({ ...prev, [field.id]: e.target.value }))}
                                disabled={isDisabled}
                                readOnly={isReadOnly}
                              />
                            )}

                            {/* Render Textarea */}
                            {field.type === "textarea" && (
                              <textarea
                                value={fieldVal}
                                onChange={(e) => setPreviewFormData((prev) => ({ ...prev, [field.id]: e.target.value }))}
                                placeholder={field.placeholder || `Enter ${field.label || "details"}`}
                                disabled={isDisabled}
                                readOnly={isReadOnly}
                                rows={3}
                                className="preview-textarea"
                              />
                            )}

                            {/* Render Dropdown Select */}
                            {field.type === "dropdown" && (
                              <Dropdown
                                option={
                                  field.mdmsMasterName
                                    ? [{ name: `${field.mdmsMasterName} Option 1` }, { name: `${field.mdmsMasterName} Option 2` }]
                                    : [{ name: "Sample Option 1" }, { name: "Sample Option 2" }]
                                }
                                optionKey="name"
                                selected={fieldVal}
                                select={(val) => setPreviewFormData((prev) => ({ ...prev, [field.id]: val }))}
                                placeholder={field.placeholder || `Select ${field.label || "option"}`}
                                disabled={isDisabled}
                                readOnly={isReadOnly}
                                t={t}
                              />
                            )}

                            {/* Render Radio Options */}
                            {field.type === "radio" && (
                              <div className="preview-options-group">
                                {(Array.isArray(field.options) && field.options.length > 0
                                  ? field.options
                                  : [
                                    { code: "OPT_1", name: "Option 1" },
                                    { code: "OPT_2", name: "Option 2" },
                                  ]
                                ).map((opt, oIdx) => (
                                  <label key={oIdx} className={`preview-option-label ${isDisabled ? "disabled" : ""}`}>
                                    <input
                                      type="radio"
                                      name={`preview_radio_${field.id}`}
                                      value={opt.code}
                                      checked={fieldVal === opt.code}
                                      disabled={isDisabled}
                                      onChange={() => setPreviewFormData((prev) => ({ ...prev, [field.id]: opt.code }))}
                                      className="preview-control-input"
                                    />
                                    <span>{opt.name || opt.code}</span>
                                  </label>
                                ))}
                              </div>
                            )}

                            {/* Render Checkbox Options */}
                            {field.type === "checkbox" && (
                              <div className="preview-options-group">
                                {(Array.isArray(field.options) && field.options.length > 0
                                  ? field.options
                                  : [
                                    { code: "OPT_1", name: "Option 1" },
                                    { code: "OPT_2", name: "Option 2" },
                                  ]
                                ).map((opt, oIdx) => {
                                  const currentCheck = Array.isArray(fieldVal) ? fieldVal : [];
                                  const isChecked = currentCheck.includes(opt.code);
                                  return (
                                    <label key={oIdx} className={`preview-option-label ${isDisabled ? "disabled" : ""}`}>
                                      <input
                                        type="checkbox"
                                        value={opt.code}
                                        checked={isChecked}
                                        disabled={isDisabled}
                                        onChange={(e) => {
                                          const updated = e.target.checked
                                            ? [...currentCheck, opt.code]
                                            : currentCheck.filter((c) => c !== opt.code);
                                          setPreviewFormData((prev) => ({ ...prev, [field.id]: updated }));
                                        }}
                                        className="preview-control-input"
                                      />
                                      <span>{opt.name || opt.code}</span>
                                    </label>
                                  );
                                })}
                              </div>
                            )}

                            {/* Render File Upload Preview Box */}
                            {field.type === "file" && (
                              <div className="preview-file-box">
                                <span className="preview-file-icon">📁</span>
                                <span className="preview-file-title">{t("WBH_CLICK_TO_UPLOAD") || "Choose file to upload"}</span>
                                <span className="preview-file-hint">
                                  Formats: {field.allowedFileTypes || "PDF, JPG"} | Max: {field.maxFileSize || 5}MB
                                </span>
                              </div>
                            )}

                            {/* Helper hint error or format msg if specified */}
                            {field.patternErrorMessage && (
                              <span className="preview-rule-hint">Rules: {field.patternErrorMessage}</span>
                            )}
                          </div>
                        );
                      })}
                    </div>
                  )}
                </div>
              </div>

              {/* Modal Footer */}
              <div className="create-form-modal-footer">
                <Button
                  type="button"
                  className="modal-btn-cancel"
                  label={t("WBH_RESET_PREVIEW") || "↺ Reset Form"}
                  variation="secondary"
                  onButtonClick={() => setPreviewFormData({})}
                />
                <Button
                  type="button"
                  className="modal-btn-submit"
                  label={t("WBH_CLOSE_PREVIEW") || "Close Preview"}
                  onButtonClick={() => setShowPreviewModal(false)}
                />
              </div>
            </div>
          </div>
        </PopUp>
      )}

      {toast && (
        <Toast
          label={toast.label}
          error={toast.error}
          onClose={() => setToast(null)}
          isDleteBtn={true}
        />
      )}
    </React.Fragment>
  );
};

export default FormCreate;
