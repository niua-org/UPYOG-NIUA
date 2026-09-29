import {
  Header,
  Card,
  CardLabel,
  TextInput,
  Dropdown,
  ToggleSwitch,
  SubmitBar,
  ActionBar,
  Button,
  SVG,
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

    return `WBH_${cleanModule}_${cleanLabel}`;
  };

  // Initial field configuration state depending on Edit vs Copy vs New mode
  const getInitialFields = () => {
    if (stateData?.existingFields && Array.isArray(stateData.existingFields) && stateData.existingFields.length > 0) {
      return stateData.existingFields;
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

  // Add new field to the canvas
  const handleAddField = (fieldType) => {
    const isFile = fieldType.code === "file";
    const isDropdown = fieldType.code === "dropdown";
    const fieldLabel = `New ${fieldType.name}`;
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
      regexType: "none",
      pattern: "",
      patternErrorMessage: "",
      // MDMS Dropdown Specific Settings
      mdmsModuleName: isDropdown ? moduleName || "ASSET" : undefined,
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
    const stateCode = Digit.ULBService.getStateId ? Digit.ULBService.getStateId() : "pg";
    const formattedKeyName = formName ? formName.charAt(0).toLowerCase() + formName.slice(1) : "form";

    const formFieldsArray = fields.map((field, idx) => {
      const fieldItem = {
        order: idx,
        key: field.fieldKey || `field_${idx + 1}`,
        field: {
          code: field.fieldKey || `field_${idx + 1}`,
          name: field.fieldKey || `field_${idx + 1}`,
          placeholder: field.placeholder || "",
          type: field.type,
          ...(field.type === "file" && {
            uploadMode: field.uploadMode || "single",
            allowedFileTypes: field.allowedFileTypes || "pdf_image",
            maxFileSize: field.maxFileSize ? Number(field.maxFileSize) : 5,
            maxFileCount: field.uploadMode === "multiple" ? (field.maxFileCount ? Number(field.maxFileCount) : 5) : 1,
          }),
        },
        validation: {
          required: field.required === "required",
          disabled: false,
          readOnly: false,
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
        <Button
          variation="secondary"
          className="header-btn-back-to-forms"
          label={t("WBH_BACK_TO_LIST") || "← Back to Forms"}
          onButtonClick={() => navigate(`/${window?.contextPath}/employee/workbench/form-builder`)}
        />
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
                        <CardLabel className="config-label">{t("WBH_FIELD_LABEL") || "Field Label"}</CardLabel>
                        <TextInput
                          value={field.label}
                          onChange={(e) => handleFieldChange(field.id, "label", e.target.value)}
                          placeholder="e.g. Applicant Name"
                        />
                      </div>

                      <div className="field-config-item">
                        <CardLabel className="config-label">{t("WBH_LOCALIZATION_KEY") || "Field Key / ID"}</CardLabel>
                        <TextInput
                          value={field.fieldKey}
                          onChange={(e) => handleFieldChange(field.id, "fieldKey", e.target.value)}
                          placeholder="e.g. applicantName"
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
