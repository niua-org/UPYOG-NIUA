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
import React, { useState, useMemo, useRef, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useLocation } from "react-router-dom";
import { FORM_TYPES } from "../../utils/workbenchUtils";
import FormSchemaPreview from "../../components/FormSchemaPreview";
import {
  buildFormSchemaSkeleton,
  buildVisibilityRules,
  fieldToSchemaItem,
  getEditableSections,
  replaceSectionForm,
  schemaItemToField,
  sectionFromKey,
  submitFormSchema,
  toFieldName,
  toLocalizationKey as generateLocalizationKey,
} from "../../utils/formSchemaUtils";

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
  // Display-only items (no input): saved as a sectionHeader / paragraph item in the form schema
  { name: "Heading", code: "heading" },
  { name: "Paragraph", code: "paragraph" },
];

// Field types that only show text and collect no value
const DISPLAY_FIELD_TYPES = ["heading", "paragraph"];

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
  const formType = queryParams?.formType || stateData?.formType || FORM_TYPES.ACCORDION;
  const isSingleForm = formType === FORM_TYPES.SINGLE;
  const formName = queryParams?.formName || stateData?.formName || "Not Specified";

  // Last submitted form schema (template JSON). Sections not yet saved keep form: [].
  // Comes from the create popup / JSON upload; on a page refresh it is rebuilt empty from the query.
  const [schema, setSchema] = useState(
    () =>
      stateData?.schema ||
      buildFormSchemaSkeleton({
        tenantId: Digit.ULBService.getStateId ? Digit.ULBService.getStateId() : "pg",
        moduleName,
        formType,
        formName,
        sections: (queryParams?.sections || "").split(",").filter(Boolean).map(sectionFromKey),
      })
  );
  const sections = useMemo(() => getEditableSections(schema, formName), [schema, formName]);

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

  // Starter fields for the first section when a form opens without saved fields (Edit/Copy sample vs New)
  const getStarterFields = () => {
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

  // Builder fields per section: { [sectionKey]: fields[] }. Saved/uploaded sections are prefilled from their form[].
  const [sectionFields, setSectionFields] = useState(() =>
    sections.reduce((acc, section, sectionIdx) => {
      if (section.form.length > 0) {
        acc[section.key] = section.form.map((item, idx) => schemaItemToField(item, `field_${section.key}_${idx}`, PREDEFINED_REGEX_OPTIONS));
      } else {
        acc[section.key] = sectionIdx === 0 ? getStarterFields().map((field) => ({ ...field, name: toFieldName(field.label) })) : [];
      }
      return acc;
    }, {})
  );
  const [activeSectionKey, setActiveSectionKey] = useState(() => sections[0]?.key);
  // Sections whose fields have been sent (uploaded sections that already have fields count as saved)
  const [savedSectionKeys, setSavedSectionKeys] = useState(() => sections.filter((section) => section.form.length > 0).map((section) => section.key));

  const activeSection = sections.find((section) => section.key === activeSectionKey) || {};
  // Accordion forms move section by section: "Save & Next" until the last section, then "Save"
  const nextSection = isSingleForm ? null : sections[sections.findIndex((section) => section.key === activeSectionKey) + 1];
  const allFields = useMemo(() => Object.values(sectionFields).flat(), [sectionFields]);

  // Fields of the active section; every canvas handler below works on these
  const fields = sectionFields[activeSectionKey] || [];
  const setFields = (updater) =>
    setSectionFields((prev) => ({
      ...prev,
      [activeSectionKey]: typeof updater === "function" ? updater(prev[activeSectionKey] || []) : updater,
    }));
  const [toast, setToast] = useState(null);
  const [showPreviewModal, setShowPreviewModal] = useState(false);
  const [activeLogicField, setActiveLogicField] = useState(null);
  const [previewFormData, setPreviewFormData] = useState({});

  // A newly added field is scrolled into view and its first input focused
  const [focusFieldId, setFocusFieldId] = useState(null);
  const fieldCardRefs = useRef({});
  const fieldFirstInputRefs = useRef({});
  // Top of the builder, scrolled to when "Save & Next" opens the next section
  const builderTopRef = useRef(null);

  // Field cards collapsed to their header: { [fieldId]: true }
  const [collapsedFieldIds, setCollapsedFieldIds] = useState({});
  const setFieldsCollapsed = (fieldList, collapsed) =>
    setCollapsedFieldIds((prev) => fieldList.reduce((acc, field) => ({ ...acc, [field.id]: collapsed }), prev));

  // "Scroll to top" button shows once the page header has scrolled out of view
  const pageTopRef = useRef(null);
  const [showScrollTop, setShowScrollTop] = useState(false);
  useEffect(() => {
    const handleScroll = () => setShowScrollTop((pageTopRef.current?.getBoundingClientRect().top || 0) < -300);
    // Capture phase so scrolling inside the app's scroll container is seen as well as window scrolling
    window.addEventListener("scroll", handleScroll, true);
    return () => window.removeEventListener("scroll", handleScroll, true);
  }, []);

  useEffect(() => {
    if (!focusFieldId) return;
    fieldCardRefs.current[focusFieldId]?.scrollIntoView({ behavior: "smooth", block: "center" });
    fieldFirstInputRefs.current[focusFieldId]?.focus({ preventScroll: true });
    setFocusFieldId(null);
  }, [focusFieldId, fields]);

  // Check if field label is a duplicate among form fields
  const isDuplicateFieldLabel = (currentField) => {
    if (!currentField?.label || !currentField.label.trim()) return false;
    const cleanLabel = currentField.label.trim().toLowerCase();
    return fields.some((f) => f.id !== currentField.id && (f.label || "").trim().toLowerCase() === cleanLabel);
  };

  // Check if field key / ID is a duplicate among the fields of every section
  const isDuplicateFieldKey = (currentField) => {
    if (!currentField?.fieldKey || !currentField.fieldKey.trim()) return false;
    const cleanKey = currentField.fieldKey.trim().toLowerCase();
    return allFields.some((f) => !f.passthrough && f.id !== currentField.id && (f.fieldKey || "").trim().toLowerCase() === cleanKey);
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
    while (allFields.some((f) => (f.label || "").trim().toLowerCase() === fieldLabel.trim().toLowerCase())) {
      fieldLabel = `${baseLabel} ${counter}`;
      counter++;
    }

    const newField = {
      id: `field_${Date.now()}`,
      label: fieldLabel,
      fieldKey: generateLocalizationKey(moduleName, fieldLabel),
      name: toFieldName(fieldLabel),
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
    setFocusFieldId(newField.id);
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
          // Uploaded fields keep their key and data name; renaming them could break rules that reference them
          if (key === "label" && !field.source) {
            updatedField.fieldKey = generateLocalizationKey(moduleName, value);
            updatedField.name = toFieldName(value);
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

  // Form schema JSON from the current builder state: every section's fields become its form[] and
  // Logic drawer rules become visibleWhen on the child fields. Used by Save and by Preview.
  const buildSchemaFromBuilder = () => {
    const visibilityRules = buildVisibilityRules(allFields);
    return sections.reduce(
      (acc, section) =>
        replaceSectionForm(
          acc,
          formName,
          section.key,
          (sectionFields[section.key] || []).map((field, idx) => fieldToSchemaItem(field, idx + 1, visibilityRules[field.id]))
        ),
      schema
    );
  };

  // Preview renders the same JSON that Save would send
  const [previewSchema, setPreviewSchema] = useState(null);
  const handleOpenPreview = () => {
    setPreviewSchema(buildSchemaFromBuilder());
    setPreviewFormData({});
    setShowPreviewModal(true);
  };

  // Save: every section's current builder fields replace that section's form[] and the full form schema is
  // submitted. Sections without fields stay form: [] (pending) until a later save.
  const handleSaveSchema = (goToNextSection = false) => {
    const filledSections = sections.filter((section) => (sectionFields[section.key] || []).length > 0);

    if (filledSections.length === 0) {
      setToast({ label: t("WBH_EMPTY_SECTION_ERROR") || "Add at least one field before saving.", error: true });
      return;
    }

    // 1. Validate Empty / Duplicate Field Labels (per section) and Keys (across the whole form).
    // On an error, open the section that has it so the user sees the field.
    for (const section of filledSections) {
      const labelSet = new Set();
      const showSectionError = (label) => {
        setActiveSectionKey(section.key);
        setToast({ label: isSingleForm ? label : `${section.labelDefault || section.key}: ${label}`, error: true });
      };

      for (const field of sectionFields[section.key].filter((f) => !f.passthrough)) {
        const normLabel = (field.label || "").trim().toLowerCase();

        if (!normLabel) {
          showSectionError(t("WBH_EMPTY_FIELD_LABEL_ERROR") || "Field Label cannot be empty. Please specify a label for all fields.");
          return;
        }
        if (labelSet.has(normLabel)) {
          showSectionError(
            t("WBH_DUPLICATE_FIELD_LABEL_ERROR") || `Duplicate field name found: "${field.label}". Each field must have a unique name.`
          );
          return;
        }
        if (isDuplicateFieldKey(field)) {
          showSectionError(t("WBH_DUPLICATE_FIELD_KEY_ERROR") || `Duplicate field key found: "${field.fieldKey}". Each field key must be unique.`);
          return;
        }
        labelSet.add(normLabel);
      }
    }

    // 2. Build every section's form[]
    const nextSchema = buildSchemaFromBuilder();
    const nextSavedKeys = filledSections.map((section) => section.key);

    submitFormSchema(nextSchema).then(() => {
      setSchema(nextSchema);
      setSavedSectionKeys(nextSavedKeys);
      if (goToNextSection && nextSection) {
        setActiveSectionKey(nextSection.key);
        builderTopRef.current?.scrollIntoView({ behavior: "smooth", block: "start" });
      }
      setToast({
        label: isSingleForm
          ? isEdit
            ? t("WBH_FORM_SCHEMA_UPDATED") || "Form Configuration Schema updated successfully!"
            : t("WBH_FORM_SCHEMA_SAVED") || "Form Configuration Schema saved successfully!"
          : `${t("WBH_SECTIONS_SAVED") || "Sections saved"}: ${nextSavedKeys.length}/${sections.length}`,
        error: false,
      });
    });
  };

  return (
    <React.Fragment>
      {/* Header and Top Action Bar */}
      <div className="jk-header-btn-wrapper form-builder-header-wrapper" ref={pageTopRef}>
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
            onButtonClick={handleOpenPreview}
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
            <span className="context-item-label">{t("WBH_FORM_TYPE") || "Form Type"}</span>
            <span className="context-item-value highlight-blue">{t(`WBH_FORM_TYPE_${formType}`)}</span>
          </div>

          <div className="context-item-box">
            <span className="context-item-label">{t("WBH_FORM_NAME") || "Form Name"}</span>
            <span className="context-item-value highlight-green">{formName}</span>
          </div>
        </div>
      </Card>

      {/* Accordion Section Switcher (each section is built and saved separately) */}
      {!isSingleForm && sections.length > 0 && (
        <div className="theme-builder-tabs-container form-section-tabs" ref={builderTopRef}>
          {sections.map((section) => {
            const isActive = section.key === activeSectionKey;
            const isSaved = savedSectionKeys.includes(section.key);
            return (
              <button
                key={section.key}
                type="button"
                className={`theme-tab-btn ${isActive ? "active" : ""}`}
                onClick={() => setActiveSectionKey(section.key)}
              >
                {section.labelDefault || t(section.i18nKey)}
                <span className={`section-status-badge ${isSaved ? "saved" : "pending"}`}>
                  {isSaved ? t("WBH_SECTION_STATUS_SAVED") || "Saved" : t("WBH_SECTION_STATUS_PENDING") || "Pending"}
                </span>
              </button>
            );
          })}
        </div>
      )}

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
              {fields.length > 0 && (
                <div className="canvas-collapse-actions">
                  <button type="button" className="canvas-collapse-btn" onClick={() => setFieldsCollapsed(fields, true)}>
                    {t("WBH_COLLAPSE_ALL") || "Collapse All"}
                  </button>
                  <button type="button" className="canvas-collapse-btn" onClick={() => setFieldsCollapsed(fields, false)}>
                    {t("WBH_EXPAND_ALL") || "Expand All"}
                  </button>
                </div>
              )}
            </div>

            {fields.length === 0 ? (
              <div className="canvas-empty-state">
                <SVG.Edit fill="#94a3b8" width="48" height="48" />
                <p>{t("WBH_NO_FIELDS_ADDED") || "No fields added yet. Click field types from the left sidebar to start building!"}</p>
              </div>
            ) : (
              <div className="canvas-fields-list">
                {fields.map((field, index) => {
                  const parentField = allFields.find((pf) => pf.logic?.targetFieldId === field.id);
                  const isChildField = !!parentField;
                  // Show/hide rule that came with the uploaded JSON (shown read-only)
                  const uploadedVisibleWhen = !isChildField ? field.source?.visibleWhen : null;
                  const isCollapsed = !!collapsedFieldIds[field.id];

                  return (
                    <div
                      key={field.id}
                      ref={(el) => (fieldCardRefs.current[field.id] = el)}
                      className={`canvas-field-card ${isChildField ? "child-field-card" : ""}`}
                      style={
                        isChildField
                          ? {
                            borderLeft: "4px solid #3b82f6",
                            marginLeft: "1.75rem",
                            background: "#f8fafc",
                            borderColor: "#3b82f6",
                          }
                          : {}
                      }
                    >
                      <div className="field-card-header">
                        {isChildField ? (
                          <span
                            className="field-number-tag child-field-tag"
                            style={{ background: "#eff6ff", color: "#1d4ed8", border: "1px solid #bfdbfe", fontWeight: "700" }}
                          >
                            ↳ Child of: {parentField.label || parentField.id}
                          </span>
                        ) : (
                          <span className="field-number-tag">Field #{index + 1}</span>
                        )}

                        {/* Field Ordering Re-sequence Controls (only shown for root parent fields to keep child fields grouped) */}
                        {!isChildField && (
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
                        )}

                        <span className="field-type-badge">{field.type.toUpperCase()}</span>
                        {!field.passthrough && !DISPLAY_FIELD_TYPES.includes(field.type) && (
                          <React.Fragment>
                            <span className={`field-requirement-badge ${field.required === "required" ? "req-mandatory" : "req-optional"}`}>
                              {field.required === "required" ? "REQUIRED" : "OPTIONAL"}
                            </span>
                            <button
                              type="button"
                              className="field-logic-btn"
                              onClick={() => setActiveLogicField(field)}
                              title="Configure Field Logic & Conditions"
                            >
                              ⚡ Logic
                            </button>
                          </React.Fragment>
                        )}
                        {uploadedVisibleWhen?.field && (
                          <span className="field-visible-when-badge">
                            👁 {t("WBH_VISIBLE_WHEN") || "Visible when"}: {uploadedVisibleWhen.field}{" "}
                            {Array.isArray(uploadedVisibleWhen.in)
                              ? `in ${uploadedVisibleWhen.in.join(", ")}`
                              : `= ${uploadedVisibleWhen.equals}`}
                          </span>
                        )}
                        {field.logic?.targetFieldId && (
                          <span
                            style={{
                              fontSize: "0.75rem",
                              background: "#f0fdf4",
                              color: "#166534",
                              border: "1px solid #bbf7d0",
                              padding: "2px 8px",
                              borderRadius: "4px",
                              fontWeight: "600",
                            }}
                          >
                            🔗 Displays: {field.logic.targetFieldLabel || "Field"}
                          </span>
                        )}
                        {isCollapsed && <span className="field-collapsed-summary">{field.label || field.fieldKey}</span>}
                        <button
                          type="button"
                          className="field-collapse-btn"
                          onClick={() => setFieldsCollapsed([field], !isCollapsed)}
                          title={isCollapsed ? t("WBH_EXPAND_FIELD") || "Expand Field" : t("WBH_COLLAPSE_FIELD") || "Collapse Field"}
                        >
                          {isCollapsed ? <SVG.ArrowDropDown width="22" height="22" /> : <SVG.ArrowDropUp width="22" height="22" />}
                        </button>
                        <button
                          type="button"
                          className="field-remove-btn"
                          onClick={() => handleRemoveField(field.id)}
                          title="Remove Field"
                        >
                          ✕
                        </button>
                      </div>

                      {isCollapsed ? null : field.passthrough ? (
                        <div className="passthrough-field-note">
                          <strong>{field.label}</strong> ({field.fieldKey}) —{" "}
                          {t("WBH_PASSTHROUGH_FIELD_NOTE") || "configured in the uploaded JSON; kept as is and not editable in the builder."}
                        </div>
                      ) : DISPLAY_FIELD_TYPES.includes(field.type) ? (
                        // Heading / Paragraph: only the text to show and its localization key
                        <div className="field-card-grid row-1">
                          <div className={`field-config-item ${field.type === "paragraph" ? "span-2" : ""}`}>
                            <CardLabel className="config-label">
                              {field.type === "heading" ? t("WBH_HEADING_TEXT") || "Heading Text" : t("WBH_PARAGRAPH_TEXT") || "Paragraph Text"}{" "}
                              <span className="mandatory-asterisk">*</span>
                            </CardLabel>
                            {field.type === "heading" ? (
                              <TextInput
                                inputRef={(el) => (fieldFirstInputRefs.current[field.id] = el)}
                                value={field.label}
                                onChange={(e) => handleFieldChange(field.id, "label", e.target.value)}
                                placeholder="e.g. Applicant Details"
                              />
                            ) : (
                              <textarea
                                ref={(el) => (fieldFirstInputRefs.current[field.id] = el)}
                                value={field.label}
                                onChange={(e) => handleFieldChange(field.id, "label", e.target.value)}
                                placeholder="e.g. Fill in the details as per your documents"
                                rows={3}
                                className="preview-textarea"
                              />
                            )}
                          </div>

                          <div className="field-config-item">
                            <CardLabel className="config-label">{t("WBH_LOCALIZATION_KEY") || "Field Key / ID"}</CardLabel>
                            <TextInput
                              value={field.fieldKey}
                              onChange={(e) => handleFieldChange(field.id, "fieldKey", e.target.value)}
                              className={isDuplicateFieldKey(field) ? "has-duplicate-error" : ""}
                            />
                            {isDuplicateFieldKey(field) && (
                              <CardLabelError className="duplicate-error-msg">
                                ⚠️ {t("WBH_DUPLICATE_KEY_WARN") || "Duplicate field key! Field ID must be unique."}
                              </CardLabelError>
                            )}
                          </div>
                        </div>
                      ) : (
                        <React.Fragment>
                          {/* Row 1: Basic Configs */}
                          <div className="field-card-grid row-1">
                            <div className="field-config-item">
                              <CardLabel className="config-label">
                                {t("WBH_FIELD_LABEL") || "Field Label"} <span className="mandatory-asterisk">*</span>
                              </CardLabel>
                              <TextInput
                                inputRef={(el) => (fieldFirstInputRefs.current[field.id] = el)}
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
                                {Array.isArray(field.options) && field.options.length === 0 && field.source?.field?.dataSource && (
                                  <span className="canvas-subtitle">
                                    {t("WBH_OPTIONS_FROM_MDMS") || "Options are loaded from MDMS"}: {field.source.field.dataSource.moduleName}.
                                    {field.source.field.dataSource.masterName}
                                  </span>
                                )}
                                {(Array.isArray(field.options) ? field.options : [
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
                        </React.Fragment>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </Card>
        </div>
      </div>

      {/* Fixed Bottom Action Bar */}
      {showScrollTop && (
        <button
          type="button"
          className="form-builder-scroll-top"
          onClick={() => pageTopRef.current?.scrollIntoView({ behavior: "smooth", block: "start" })}
          title={t("WBH_SCROLL_TO_TOP") || "Scroll to top"}
          aria-label={t("WBH_SCROLL_TO_TOP") || "Scroll to top"}
        >
          <SVG.ArrowDropUp width="28" height="28" fill="#ffffff" />
        </button>
      )}

      <ActionBar>
        <SubmitBar
          disabled={false}
          onSubmit={() => handleSaveSchema(!!nextSection)}
          label={nextSection ? t("WBH_SAVE_AND_NEXT") || "Save & Next" : isEdit ? t("WBH_UPDATE_FORM_CONFIG") || "Update" : t("WBH_SAVE") || "Save"}
        />
      </ActionBar>

      {/* Form Live Preview Popup Modal: renders the JSON Save would send, as the form will be used */}
      {showPreviewModal && previewSchema && (
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
                      {t("WBH_MODULE") || "Module"}: <strong>{moduleName}</strong> | {t("WBH_FORM_TYPE") || "Form Type"}:{" "}
                      <strong>{t(`WBH_FORM_TYPE_${formType}`)}</strong>
                    </p>
                  </div>
                </div>
                <button type="button" className="modal-close-btn" onClick={() => setShowPreviewModal(false)} aria-label="Close">
                  <CloseSvg fill="#64748b" width="18" height="18" />
                </button>
              </div>

              {/* Modal Body - Interactive Form Preview */}
              <div className="preview-modal-body">
                <FormSchemaPreview schema={previewSchema} formName={formName} values={previewFormData} onChange={setPreviewFormData} />
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

      {/* Right-Side Drawer Modal for Field Logic */}
      {activeLogicField && (() => {
        const availableFieldOptions =
          Array.isArray(activeLogicField.options) && activeLogicField.options.length > 0
            ? activeLogicField.options.map((opt) => ({
              name: opt.name || opt.code,
              code: opt.code || opt.name,
            }))
            : [
              { name: "Male", code: "Male" },
              { name: "Female", code: "Female" },
              { name: "Option 1", code: "OPTION_1" },
              { name: "Option 2", code: "OPTION_2" },
            ];

        const availableTargetFields = [
          ...FIELD_TYPES.map((ft) => ({
            name: ft.name,
            code: ft.code,
          })),
          // Existing editable fields of every section (fields in other sections are prefixed with their section)
          ...sections.flatMap((section) =>
            (sectionFields[section.key] || [])
              .filter((f) => !f.passthrough && f.id !== activeLogicField.id)
              .map((f) => ({
                name: section.key === activeSectionKey ? f.label || f.fieldKey || f.id : `${section.labelDefault} › ${f.label || f.fieldKey || f.id}`,
                code: f.id,
              }))
          ),
        ];

        return (
          <PopUp>
            <div
              style={{
                position: "fixed",
                top: 0,
                left: 0,
                right: 0,
                bottom: 0,
                background: "rgba(15, 23, 42, 0.4)",
                display: "flex",
                justifyContent: "flex-end",
                zIndex: 9999,
              }}
              onClick={() => setActiveLogicField(null)}
            >
              <div
                style={{
                  background: "#ffffff",
                  width: "100%",
                  maxWidth: "440px",
                  height: "100vh",
                  display: "flex",
                  flexDirection: "column",
                  boxShadow: "-4px 0 25px rgba(0, 0, 0, 0.15)",
                  boxSizing: "border-box",
                }}
                onClick={(e) => e.stopPropagation()}
              >
                {/* Drawer Header */}
                <div
                  style={{
                    padding: "1.25rem 1.5rem",
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    borderBottom: "1px solid #f1f5f9",
                    background: "#ffffff",
                  }}
                >
                  <div style={{ display: "flex", alignItems: "center", gap: "0.75rem" }}>
                    <div
                      style={{
                        width: "36px",
                        height: "36px",
                        borderRadius: "8px",
                        background: "#eff6ff",
                        border: "1px solid #bfdbfe",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                      }}
                    >
                      <span style={{ fontSize: "1.1rem" }}>⚡</span>
                    </div>
                    <div>
                      <h3 style={{ margin: 0, fontSize: "1.1rem", fontWeight: "700", color: "#0f172a" }}>
                        {t("WBH_FIELD_LOGIC") || "Configure Logic"}
                      </h3>
                      <p style={{ margin: "0.15rem 0 0 0", fontSize: "0.8rem", color: "#64748b" }}>
                        Field: <strong>{activeLogicField.label || activeLogicField.id}</strong>
                      </p>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => setActiveLogicField(null)}
                    style={{ background: "none", border: "none", fontSize: "1.25rem", cursor: "pointer", color: "#64748b" }}
                    aria-label="Close"
                  >
                    <CloseSvg fill="#64748b" width="18" height="18" />
                  </button>
                </div>

                {/* Drawer Body */}
                <div style={{ padding: "1.5rem", flex: 1, overflowY: "auto", display: "flex", flexDirection: "column", gap: "1.5rem" }}>
                  {/* Section 1: If answer */}
                  <div className="form-group-item">
                    <CardLabel className="config-label" style={{ fontSize: "1rem", fontWeight: "700", color: "#1e293b", marginBottom: "0.75rem" }}>
                      If answer
                    </CardLabel>

                    <div style={{ display: "flex", flexDirection: "column", gap: "0.85rem" }}>
                      {/* Condition Dropdown (Equals / Not Equals) */}
                      <div>
                        <Dropdown
                          option={[
                            { name: "Equals", code: "EQUALS" },
                            { name: "Not Equals", code: "NOT_EQUALS" },
                          ]}
                          optionKey="name"
                          selected={[
                            { name: "Equals", code: "EQUALS" },
                            { name: "Not Equals", code: "NOT_EQUALS" },
                          ].find((c) => c.code === (activeLogicField.logic?.condition || "EQUALS"))}
                          select={(val) =>
                            setActiveLogicField((prev) => ({
                              ...prev,
                              logic: { ...prev?.logic, condition: val?.code },
                            }))
                          }
                          placeholder="Equals"
                          t={t}
                        />
                      </div>

                      {/* Select Option Dropdown */}
                      <div>
                        <Dropdown
                          option={availableFieldOptions}
                          optionKey="name"
                          selected={availableFieldOptions.find(
                            (opt) => opt.code === activeLogicField.logic?.selectedOption || opt.name === activeLogicField.logic?.selectedOption
                          )}
                          select={(val) =>
                            setActiveLogicField((prev) => ({
                              ...prev,
                              logic: { ...prev?.logic, selectedOption: val?.code || val?.name },
                            }))
                          }
                          placeholder="Select Option"
                          t={t}
                        />
                      </div>
                    </div>
                  </div>

                  {/* Section 2: Then display */}
                  <div className="form-group-item">
                    <CardLabel className="config-label" style={{ fontSize: "1rem", fontWeight: "700", color: "#1e293b", marginBottom: "0.75rem" }}>
                      Then display
                    </CardLabel>

                    <div>
                      <Dropdown
                        option={availableTargetFields}
                        optionKey="name"
                        selected={availableTargetFields.find((f) => f.code === activeLogicField.logic?.targetFieldId)}
                        select={(val) =>
                          setActiveLogicField((prev) => ({
                            ...prev,
                            logic: { ...prev?.logic, targetFieldId: val?.code, targetFieldLabel: val?.name },
                          }))
                        }
                        placeholder="Select Field to Display"
                        t={t}
                      />
                    </div>
                  </div>
                </div>

                {/* Drawer Footer */}
                <div
                  style={{
                    padding: "1rem 1.5rem",
                    borderTop: "1px solid #f1f5f9",
                    display: "flex",
                    justifyContent: "flex-end",
                    gap: "0.75rem",
                    background: "#ffffff",
                  }}
                >
                  <Button
                    type="button"
                    className="modal-btn-cancel"
                    label="Cancel"
                    variation="secondary"
                    onButtonClick={() => setActiveLogicField(null)}
                  />
                  <Button
                    type="button"
                    className="modal-btn-submit"
                    label="Save Logic >"
                    onButtonClick={() => {
                      const selectedTargetId = activeLogicField.logic?.targetFieldId;
                      const isFieldType = FIELD_TYPES.some((ft) => ft.code === selectedTargetId);

                      if (isFieldType) {
                        // Selected a field type from FIELD_TYPES -> Automatically create new field!
                        const fieldTypeObj = FIELD_TYPES.find((ft) => ft.code === selectedTargetId);
                        const isFile = selectedTargetId === "file";
                        const isDropdown = selectedTargetId === "dropdown";
                        const isOptionsField = ["radio", "checkbox"].includes(selectedTargetId);

                        const baseLabel = `New ${fieldTypeObj?.name || "Field"}`;
                        let fieldLabel = baseLabel;
                        let counter = 1;
                        while (allFields.some((f) => (f.label || "").trim().toLowerCase() === fieldLabel.trim().toLowerCase())) {
                          fieldLabel = `${baseLabel} ${counter}`;
                          counter++;
                        }

                        const newCreatedField = {
                          id: `field_${Date.now()}`,
                          label: fieldLabel,
                          fieldKey: generateLocalizationKey(moduleName, fieldLabel),
                          name: toFieldName(fieldLabel),
                          type: selectedTargetId,
                          required: "required",
                          isReadonly: false,
                          isDisabled: false,
                          placeholder: `Enter ${(fieldTypeObj?.name || "field").toLowerCase()}`,
                          minLength: "",
                          maxLength: "",
                          heading: "",
                          paragraph: "",
                          regexType: "none",
                          pattern: "",
                          patternErrorMessage: "",
                          options: isOptionsField
                            ? [
                              { code: "OPTION_1", name: "Option 1" },
                              { code: "OPTION_2", name: "Option 2" },
                            ]
                            : undefined,
                          mdmsModuleName: isDropdown ? moduleName : undefined,
                          mdmsMasterName: isDropdown ? "" : undefined,
                          uploadMode: isFile ? "single" : undefined,
                          allowedFileTypes: isFile ? "pdf_image" : undefined,
                          maxFileSize: isFile ? "5" : undefined,
                          maxFileCount: isFile ? "5" : undefined,
                          fileErrorMessage: isFile ? "Allowed formats: PDF, JPG, PNG up to 5MB" : undefined,
                        };

                        const updatedLogicData = {
                          ...activeLogicField.logic,
                          targetFieldId: newCreatedField.id,
                          targetFieldLabel: newCreatedField.label,
                        };

                        setFields((prev) => {
                          const parentIdx = prev.findIndex((f) => f.id === activeLogicField.id);
                          const updatedList = prev.map((f) => (f.id === activeLogicField.id ? { ...f, logic: updatedLogicData } : f));

                          if (parentIdx !== -1) {
                            updatedList.splice(parentIdx + 1, 0, newCreatedField);
                            return updatedList;
                          }
                          return [...updatedList, newCreatedField];
                        });

                        setFocusFieldId(newCreatedField.id);
                        setActiveLogicField(null);
                        setToast({
                          label: t("WBH_LOGIC_FIELD_CREATED_SUCCESS") || `Created new ${newCreatedField.label} field and linked logic!`,
                          error: false,
                        });
                      } else {
                        // Selected an existing canvas field
                        const logicData = activeLogicField.logic;
                        setFields((prev) =>
                          prev.map((f) => (f.id === activeLogicField.id ? { ...f, logic: logicData } : f))
                        );
                        setActiveLogicField(null);
                        setToast({ label: t("WBH_LOGIC_SAVED_SUCCESS") || "Field logic configuration saved successfully!", error: false });
                      }
                    }}
                  />
                </div>
              </div>
            </div>
          </PopUp>
        );
      })()}

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



