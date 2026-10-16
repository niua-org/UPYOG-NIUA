import {
  Header,
  InboxSearchComposer,
  ActionBar,
  SubmitBar,
  PopUp,
  Dropdown,
  MultiSelectDropdown,
  TextInput,
  CardLabel,
  CardLabelError,
  Button,
  SVG,
  CloseSvg,
  Toast,
} from "@upyog/workbench-ui-react-components";
import React, { useState, useMemo, useEffect, useRef } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useForm, useWatch } from "react-hook-form";
import { Config } from "../../configs/FormBuilderConfig";
import { FORM_BUILDER_DATA } from "../../hooks/useFormBuilderSearch";
import {
  useModalState,
  getModuleListFromMDMS,
  getAccordionsForModule,
  getSingleFormsForModule,
  getFormTypesForModule,
  FORM_TYPES,
} from "../../utils/workbenchUtils";
import { buildFormSchemaSkeleton, parseFormSchema, toFormNameKey, sectionFromLabel } from "../../utils/formSchemaUtils";

const EMPTY_CREATE_FORM = { moduleName: "", formType: "", sections: [], formName: "" };

// Builds the form-create route + navigation state. The full form schema travels in state;
// the query keeps enough to rebuild an empty schema on page refresh.
const buildFormCreateNavigation = ({ moduleName, formType, formName, schema }, extraQuery = "", extraState = {}) => {
  const sectionKeys = (schema?.[formName]?.[0]?.body || []).filter((s) => s?.type !== "review").map((s) => s.key);
  return [
    `/${window?.contextPath}/employee/workbench/form-create?moduleName=${encodeURIComponent(moduleName || "")}&formType=${encodeURIComponent(
      formType || ""
    )}&formName=${encodeURIComponent(formName || "")}&sections=${encodeURIComponent(sectionKeys.join(","))}${extraQuery}`,
    {
      state: { ...extraState, moduleName, formType, formName, schema },
    },
  ];
};

const SAMPLE_UPYOG_JSON = {
  tenantId: "pg",
  moduleName: "TRADE_LICENSE",
  NEW_REGISTRATION: [
    {
      head: "TRADE_LICENSE_NEW_REGISTRATION",
      headDefault: "New Registration",
      navigation: {
        pattern: "accordion",
        formRoute: "apply",
        confirmRoute: "check",
        ackRoute: "acknowledgement",
        accordion: {
          defaultOpenStep: "tradeDetails",
          showFilledHint: true,
          submitLabel: "COMMON_SUBMIT_APPLICATION",
          submitLabelDefault: "Submit Application",
        },
        timeline: {
          citizenOnly: true,
          hideOnRoutes: ["acknowledgement"],
          steps: [
            { step: 1, label: "WBH_ACCORDION_TRADE_DETAILS", labelDefault: "Trade Details", sections: ["tradeDetails"], routes: ["apply"] },
            { step: 2, label: "WBH_ACCORDION_DOCUMENT_DETAILS", labelDefault: "Document Details", sections: ["documentDetails"] },
            { step: 3, label: "TRADE_LICENSE_STEP_CONFIRM", labelDefault: "Summary", routes: ["check"] },
          ],
        },
      },
      body: [
        {
          key: "tradeDetails",
          route: "trade-details",
          order: 1,
          i18nKey: "WBH_ACCORDION_TRADE_DETAILS",
          labelDefault: "Trade Details",
          component: "NEW_REGISTRATION",
          payloadKey: "TRADE_LICENSE",
          apiId: "Rainmaker",
          form: [
            {
              order: 1,
              key: "TRADE_LICENSE_TRADE_NAME",
              field: { code: "TRADE_LICENSE_TRADE_NAME", name: "tradeName", placeholder: "Enter trade name", type: "text" },
              validation: { required: true, minLength: 3, maxLength: 100, pattern: "^[a-zA-Z0-9 ]+$" },
              messages: { error: "Only letters and numbers are allowed", labelDefault: "Trade Name" },
            },
            {
              order: 2,
              key: "TRADE_LICENSE_STRUCTURE_TYPE",
              field: { code: "TRADE_LICENSE_STRUCTURE_TYPE", name: "structureType", type: "radio" },
              options: [
                { code: "IMMOVABLE", name: "Immovable" },
                { code: "MOVABLE", name: "Movable" },
              ],
              validation: { required: true },
              messages: { labelDefault: "Structure Type" },
            },
            {
              order: 3,
              key: "TRADE_LICENSE_BUILDING_TYPE",
              field: {
                code: "TRADE_LICENSE_BUILDING_TYPE",
                name: "buildingType",
                placeholder: "Select building type",
                type: "dropdown",
                dataSource: { type: "MDMS", moduleName: "common-masters", masterName: "StructureType", customiztionRequired: true },
              },
              visibleWhen: { field: "structureType", equals: "IMMOVABLE" },
              validation: { required: true },
              messages: { labelDefault: "Building Type" },
            },
          ],
        },
        {
          key: "documentDetails",
          route: "document-details",
          order: 2,
          i18nKey: "WBH_ACCORDION_DOCUMENT_DETAILS",
          labelDefault: "Document Details",
          form: [
            {
              order: 1,
              key: "TRADE_LICENSE_ID_PROOF",
              field: { code: "TRADE_LICENSE_ID_PROOF", name: "proofOfIdentity", type: "file", accept: ".jpg,.jpeg,.png,.pdf" },
              validation: { required: true, maxSizeMB: 2 },
              messages: { error: "Allowed formats: PDF, JPG, PNG up to 2MB", labelDefault: "Proof of Identity" },
            },
          ],
        },
        {
          key: "confirm",
          route: "check",
          order: 3,
          i18nKey: "TRADE_LICENSE_STEP_CONFIRM",
          labelDefault: "Confirm",
          isPreview: true,
          type: "review",
          reviewSectionsFromSteps: ["tradeDetails", "documentDetails"],
          declaration: {
            required: true,
            i18nKey: "TRADE_LICENSE_DECLARATION_CONFIRM",
            labelDefault: "I confirm that the information provided above is true and correct to the best of my knowledge.",
          },
          submitLabel: "COMMON_SUBMIT_APPLICATION",
          submitLabelDefault: "Submit Application",
          form: [],
        },
      ],
    },
  ],
};

const FormBuilder = () => {
  const { t } = useTranslation();
  const navigate = Digit.Hooks.useCustomNavigate();
  const [callRefetch, setCallRefetch] = useState(false);
  const [tableData, setTableData] = useState(FORM_BUILDER_DATA);
  const [showSampleModal, setShowSampleModal] = useState(false);
  const fileInputRef = useRef(null);

  const handleCopySampleJson = () => {
    navigator.clipboard.writeText(JSON.stringify(SAMPLE_UPYOG_JSON, null, 2));
    showToast(t("WBH_SAMPLE_JSON_COPIED") || "Sample JSON copied to clipboard!", false);
  };

  const handleDownloadSampleJson = () => {
    const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(SAMPLE_UPYOG_JSON, null, 2));
    const downloadAnchor = document.createElement("a");
    downloadAnchor.setAttribute("href", dataStr);
    downloadAnchor.setAttribute("download", "sample-upyog-form-schema.json");
    document.body.appendChild(downloadAnchor);
    downloadAnchor.click();
    downloadAnchor.remove();
  };

  // Fetch MDMS ModuleAccordions store data
  const stateCode = Digit.ULBService.getStateId ? Digit.ULBService.getStateId() : "pg";
  const { isLoading, data: ModuleData } = Digit.Hooks.useInitStore(stateCode, ["Workbench"]);

  // Extract array of module objects from MDMS ModuleAccordions
  const modulesList = useMemo(() => getModuleListFromMDMS(ModuleData?.ModuleAccordions), [ModuleData]);

  // 1. Dynamic Module dropdown options from MDMS
  const moduleOptions = useMemo(() => {
    if (modulesList && modulesList.length > 0) {
      return modulesList.map((m) => ({
        name: m?.i18nKey || m?.name || m?.code,
        code: m?.code,
      }));
    }
    return [];
  }, [modulesList]);

  // Reusable Popup Modal state hook
  const { isOpen: showCreateModal, openModal: handleAddNewField, closeModal: handleModalCloseRaw } = useModalState(false);

  // react-hook-form hook for managing modal form state & validation rules
  const {
    control,
    handleSubmit,
    setValue,
    reset,
    formState: { errors },
  } = useForm({
    defaultValues: EMPTY_CREATE_FORM,
  });

  // Watch selected moduleName & formType
  const selectedModuleName = useWatch({ control, name: "moduleName" });
  const selectedFormType = useWatch({ control, name: "formType" });

  // Selected module object matching selected module code
  const selectedModuleObj = useMemo(() => {
    if (!selectedModuleName) return null;
    return modulesList.find((m) => m?.code === selectedModuleName || m?.name === selectedModuleName);
  }, [modulesList, selectedModuleName]);

  // 2. Dependent Form Type (Accordion / Single) dropdown options based on selected module
  const formTypeOptions = useMemo(() => {
    return getFormTypesForModule(selectedModuleObj);
  }, [selectedModuleObj]);

  // 3a. Dependent Accordion section options based on selected module (Summary is auto-generated, so excluded)
  const accordionOptions = useMemo(() => {
    return getAccordionsForModule(selectedModuleObj, []).filter((option) => option.type !== "review");
  }, [selectedModuleObj]);

  // 3b. Dependent Single Form dropdown options based on selected module
  const singleFormOptions = useMemo(() => {
    return getSingleFormsForModule(selectedModuleObj, []);
  }, [selectedModuleObj]);

  // Section options for the selected form type
  const sectionOptions = selectedFormType === FORM_TYPES.SINGLE ? singleFormOptions : accordionOptions;

  // Reset dependent Form Type, Sections & Form Name whenever selected module changes
  useEffect(() => {
    if (selectedModuleName) {
      setValue("formType", "");
      setValue("sections", []);
      setValue("formName", "");
    }
  }, [selectedModuleName, setValue]);

  // Reset Section selection whenever selected form type changes
  useEffect(() => {
    setValue("sections", []);
  }, [selectedFormType, setValue]);

  const handleCreateFormSubmit = (data) => {
    // Selected sections in MDMS order, not in the order they were ticked
    const selectedSections = sectionOptions
      .filter((option) => data.sections.includes(option.code))
      .map((option) => ({ key: option.key, i18nKey: option.name, labelDefault: option.labelDefault }));

    const schema = buildFormSchemaSkeleton({
      tenantId: stateCode,
      moduleName: data.moduleName,
      formType: data.formType,
      formName: data.formName,
      sections: selectedSections,
    });

    const newItem = {
      id: `${Date.now()}`,
      moduleName: data.moduleName,
      formType: data.formType,
      accordionName: selectedSections.map((section) => section.labelDefault).join(", "),
      formName: data.formName,
    };

    setTableData((prev) => [newItem, ...prev]);
    handleModalCloseRaw();
    reset(EMPTY_CREATE_FORM);
    setCallRefetch((prev) => !prev);

    // Navigate to the Form Field Builder page with the new (all sections pending) form schema
    navigate(...buildFormCreateNavigation({ moduleName: data.moduleName, formType: data.formType, formName: data.formName, schema }));
  };

  // Builder navigation data for an existing list row
  const getRowNavigationData = (rowData, formName) => {
    const formType = rowData.formType || FORM_TYPES.ACCORDION;
    // A copied row keeps its saved schema under the new form name
    const { [rowData.formName]: savedForm, ...schemaMeta } = rowData.schema || {};
    return {
      moduleName: rowData.moduleName,
      formType,
      formName,
      schema: savedForm
        ? { ...schemaMeta, [formName]: savedForm }
        : buildFormSchemaSkeleton({
          tenantId: stateCode,
          moduleName: rowData.moduleName,
          formType,
          formName,
          sections: [sectionFromLabel(rowData.accordionName || formName)],
        }),
    };
  };

  const handleModalClose = () => {
    handleModalCloseRaw();
    reset(EMPTY_CREATE_FORM);
  };

  const [deleteModalRow, setDeleteModalRow] = useState(null);
  const [toast, setToast] = useState(null);

  const showToast = (label, error = false) => {
    setToast({ label, error });
    setTimeout(() => {
      setToast(null);
    }, 2500);
  };

  const handleEditForm = (row) => {
    const rowData = row?.original || row || {};
    console.log("Editing Form Configuration: ", rowData);
    navigate(...buildFormCreateNavigation(getRowNavigationData(rowData, rowData.formName), "&isEdit=true", { isEdit: true }));
  };

  const handleCopyForm = (row) => {
    const rowData = row?.original || row || {};
    console.log("Copying Form Configuration to New: ", rowData);
    const newFormName = `${rowData.formName || "FORM"}_COPY`;
    navigate(...buildFormCreateNavigation(getRowNavigationData(rowData, newFormName), "&isCopy=true", { isCopy: true }));
  };

  const confirmDeleteForm = () => {
    if (deleteModalRow) {
      const targetName = deleteModalRow?.formName || deleteModalRow?.id;
      console.log("Deleting Form Configuration: ", deleteModalRow);
      setTableData((prev) => prev.filter((item) => item.id !== deleteModalRow.id && item.formName !== deleteModalRow.formName));
      showToast(t("WBH_FORM_DELETED_SUCCESS") || `Form configuration '${targetName}' deleted successfully!`, false);
      setDeleteModalRow(null);
      setCallRefetch((prev) => !prev);
    }
  };

  // Global action handler hook for table action column buttons
  useEffect(() => {
    window.onFormBuilderAction = (actionType, row) => {
      const rowData = row?.original || row || {};
      if (actionType === "EDIT") {
        handleEditForm(rowData);
      } else if (actionType === "COPY") {
        handleCopyForm(rowData);
      } else if (actionType === "DELETE") {
        setDeleteModalRow(rowData);
      }
    };
    return () => {
      delete window.onFormBuilderAction;
    };
  }, [navigate]);

  const onClickSvg = (row) => {
    handleEditForm(row);
  };

  const handleJsonFileUpload = (e) => {
    const file = e.target.files && e.target.files[0];
    if (!file) return;

    if (!file.name.toLowerCase().endsWith(".json")) {
      showToast(t("WBH_INVALID_FILE_TYPE_JSON") || "Please upload a valid JSON file (.json)", true);
      e.target.value = "";
      return;
    }

    const reader = new FileReader();
    reader.onload = (event) => {
      try {
        const parsed = JSON.parse(event.target.result);
        const { error, formName, formType, moduleName } = parseFormSchema(parsed);
        if (error) {
          showToast(
            t("WBH_INVALID_FORM_SCHEMA_ERROR") || "JSON does not match the form schema template. Download the sample JSON to see the expected format.",
            true
          );
          return;
        }

        const sectionCount = parsed[formName][0].body.filter((section) => section?.type !== "review").length;
        showToast(t("WBH_JSON_UPLOAD_SUCCESS") || `JSON configuration uploaded! Loaded ${sectionCount} section(s).`, false);

        setTimeout(() => {
          navigate(...buildFormCreateNavigation({ moduleName, formType, formName, schema: parsed }, "&isUpload=true", { isUpload: true }));
        }, 600);
      } catch (err) {
        console.error("Failed to parse JSON file:", err);
        showToast(t("WBH_INVALID_JSON_ERROR") || "Failed to parse JSON file. Please check file format.", true);
      } finally {
        e.target.value = "";
      }
    };

    reader.onerror = () => {
      showToast(t("WBH_FILE_READ_ERROR") || "Error reading file", true);
      e.target.value = "";
    };

    reader.readAsText(file);
  };

  return (
    <React.Fragment>
      <div className="jk-header-btn-wrapper form-builder-header-wrapper">
        <Header className="works-header-search">{t(Config?.label || "WBH_FORM_BUILDER_HEADER")}</Header>
        <div style={{ display: "flex", gap: "10px", alignItems: "center" }}>
          <input type="file" ref={fileInputRef} style={{ display: "none" }} accept=".json" onChange={handleJsonFileUpload} />
          <Button
            type="button"
            variation="secondary"
            className="header-btn-back-to-forms"
            label={t("WBH_SHOW_SAMPLE_JSON") || "Display JSON"}
            onButtonClick={() => setShowSampleModal(true)}
          />
          <Button
            type="button"
            variation="secondary"
            className="header-btn-back-to-forms"
            label={t("WBH_UPLOAD_JSON") || "Upload JSON"}
            onButtonClick={() => fileInputRef.current && fileInputRef.current.click()}
          />
        </div>
      </div>

      {Config && (
        <div className="inbox-search-wrapper">
          <InboxSearchComposer
            configs={Config}
            additionalConfig={{
              resultsTable: {
                onClickSvg,
              },
              search: {
                callRefetch,
                setCallRefetch,
              },
            }}
          ></InboxSearchComposer>
        </div>
      )}

      {/* Create New Form Modal Popup */}
      {showCreateModal && (
        <PopUp>
          <div className="create-form-modal-overlay">
            <div className="create-form-modal-container">
              {/* Modal Header */}
              <div className="create-form-modal-header">
                <div className="modal-title-wrapper">
                  <div className="modal-icon-badge">
                    <SVG.Edit fill="#f97316" width="22" height="22" />
                  </div>
                  <div>
                    <h2 className="modal-title-text">{t("WBH_CREATE_NEW_FORM")}</h2>
                    <p className="modal-subtitle-text">{t("WBH_SET_UP_MODULE_SCOPE")}</p>
                  </div>
                </div>

                <button type="button" className="modal-close-btn" onClick={handleModalClose} aria-label="Close">
                  <CloseSvg fill="#64748b" width="18" height="18" />
                </button>
              </div>

              {/* Modal Content Grid */}
              <form onSubmit={handleSubmit(handleCreateFormSubmit)}>
                <div className="create-form-modal-grid">
                  {/* Left Form Column */}
                  <div className="create-form-modal-left">
                    {/* 1. Module Name Dropdown */}
                    <div className="form-group-item">
                      <CardLabel className="form-field-label">
                        {t("WBH_MODULE_NAME")} <span className="mandatory-asterisk">*</span>
                      </CardLabel>
                      <Controller
                        name="moduleName"
                        control={control}
                        rules={{ required: true }}
                        render={({ field: { onChange, value } }) => (
                          <Dropdown
                            option={moduleOptions}
                            optionKey="name"
                            selected={moduleOptions.find((m) => m.code === value)}
                            select={(val) => onChange(val?.code)}
                            t={t}
                            placeholder={t("WBH_SELECT_TARGET_MODULE")}
                          />
                        )}
                      />
                      {errors?.moduleName && (
                        <CardLabelError className="form-field-error">{t("WBH_MODULE_NAME_REQUIRED") || "Module Name is required"}</CardLabelError>
                      )}
                    </div>

                    {/* 2. Dependent Form Type (Accordion / Single) Dropdown */}
                    <div className="form-group-item">
                      <CardLabel className="form-field-label">
                        {t("WBH_FORM_TYPE")} <span className="mandatory-asterisk">*</span>
                      </CardLabel>
                      <Controller
                        name="formType"
                        control={control}
                        rules={{ required: true }}
                        render={({ field: { onChange, value } }) => (
                          <Dropdown
                            option={formTypeOptions}
                            optionKey="name"
                            selected={formTypeOptions.find((ft) => ft.code === value)}
                            select={(val) => onChange(val?.code)}
                            t={t}
                            placeholder={t("WBH_SELECT_FORM_TYPE")}
                            disable={!selectedModuleName}
                          />
                        )}
                      />
                      {errors?.formType && (
                        <CardLabelError className="form-field-error">{t("WBH_FORM_TYPE_REQUIRED") || "Form Type is required"}</CardLabelError>
                      )}
                    </div>

                    {/* 3. Dependent Sections: multi-select for Accordion, single select for Single form */}
                    {selectedFormType && (
                      <div className="form-group-item">
                        <CardLabel className="form-field-label">
                          {selectedFormType === FORM_TYPES.SINGLE ? t("WBH_SINGLE_FORM_NAME") : t("WBH_ACCORDION_NAME")}{" "}
                          <span className="mandatory-asterisk">*</span>
                        </CardLabel>
                        <Controller
                          key={selectedFormType}
                          name="sections"
                          control={control}
                          rules={{ validate: (value) => Array.isArray(value) && value.length > 0 }}
                          render={({ field: { onChange, value } }) =>
                            selectedFormType === FORM_TYPES.SINGLE ? (
                              <Dropdown
                                option={sectionOptions}
                                optionKey="name"
                                selected={sectionOptions.find((s) => (value || []).includes(s.code))}
                                select={(val) => onChange(val?.code ? [val.code] : [])}
                                t={t}
                                placeholder={t("WBH_SELECT_SINGLE_FORM")}
                              />
                            ) : (
                              <MultiSelectDropdown
                                options={sectionOptions}
                                optionsKey="name"
                                selected={sectionOptions.filter((s) => (value || []).includes(s.code))}
                                onSelect={(list) => onChange((list || []).map((item) => item?.[1]?.code).filter(Boolean))}
                                defaultLabel={t("WBH_SELECT_SECTIONS")}
                                defaultUnit={t("WBH_SECTIONS_SELECTED")}
                                config={{ isDropdownWithChip: true }}
                              />
                            )
                          }
                        />
                        {errors?.sections && (
                          <CardLabelError className="form-field-error">
                            {selectedFormType === FORM_TYPES.SINGLE
                              ? t("WBH_SINGLE_FORM_NAME_REQUIRED") || "Single Form Name is required"
                              : t("WBH_SECTIONS_REQUIRED") || "Select at least one section"}
                          </CardLabelError>
                        )}
                      </div>
                    )}

                    {/* 4. Form Name (typed; becomes the form's JSON key, e.g. NEW_REGISTRATION) */}
                    <div className="form-group-item">
                      <CardLabel className="form-field-label">
                        {t("WBH_FORM_NAME")} <span className="mandatory-asterisk">*</span>
                      </CardLabel>
                      <Controller
                        name="formName"
                        control={control}
                        rules={{
                          required: true,
                          validate: (value) =>
                            !tableData.some((row) => row.moduleName === selectedModuleName && row.formName === value) || "DUPLICATE",
                        }}
                        render={({ field: { onChange, value } }) => (
                          <TextInput
                            value={value}
                            onChange={(e) => onChange(toFormNameKey(e.target.value))}
                            placeholder={t("WBH_ENTER_FORM_NAME")}
                            disabled={!selectedModuleName}
                          />
                        )}
                      />
                      {errors?.formName && (
                        <CardLabelError className="form-field-error">
                          {errors.formName.message === "DUPLICATE"
                            ? t("WBH_FORM_NAME_DUPLICATE") || "A form with this name already exists for the selected module"
                            : t("WBH_FORM_NAME_REQUIRED") || "Form Name is required"}
                        </CardLabelError>
                      )}
                    </div>
                  </div>

                  {/* Right Info Column */}
                  <div className="create-form-modal-right">
                    <div>
                      <div className="schema-icon-badge">
                        <SVG.Description fill="#f97316" width="22" height="22" />
                      </div>

                      <h3 className="schema-title">{t("WBH_UPYOG_SCHEMA_HIERARCHY")}</h3>

                      <p className="schema-description">{t("WBH_SCHEMA_HIERARCHY_DESC")}</p>

                      <ul className="schema-bullets-list">
                        <li className="bullet-green">
                          <span>{t("WBH_STANDARDIZED_SCHEMAS")}</span>
                        </li>
                        <li className="bullet-orange">
                          <span>{t("WBH_MULTILINGUAL_I18N")}</span>
                        </li>
                        <li className="bullet-blue">
                          <span>{t("WBH_TIMELINE_SYNC")}</span>
                        </li>
                      </ul>
                    </div>
                  </div>
                </div>

                {/* Modal Footer Actions */}
                <div className="create-form-modal-footer">
                  <Button type="button" className="modal-btn-cancel" label={t("WBH_CANCEL")} variation="secondary" onButtonClick={handleModalClose} />

                  <SubmitBar
                    submit={true}
                    className="modal-btn-submit"
                    label={t("WBH_PROCEED_TO_FORM_BUILDER")}
                    onSubmit={handleSubmit(handleCreateFormSubmit)}
                  />
                </div>
              </form>
            </div>
          </div>
        </PopUp>
      )}

      {/* Delete Form Confirmation Popup Modal */}
      {deleteModalRow && (
        <PopUp>
          <div className="create-form-modal-overlay">
            <div className="create-form-modal-container" style={{ maxWidth: "480px" }}>
              <div className="create-form-modal-header">
                <div className="modal-title-wrapper">
                  <div className="modal-icon-badge" style={{ background: "#fef2f2", borderColor: "#fecaca" }}>
                    <span style={{ fontSize: "1.2rem" }}>🗑️</span>
                  </div>
                  <div>
                    <h2 className="modal-title-text">{t("WBH_DELETE_CONFIRM_TITLE") || "Delete Form Configuration"}</h2>
                    <p className="modal-subtitle-text">
                      {t("WBH_DELETE_CONFIRM_DESC") || "Are you sure you want to delete this form configuration?"}
                    </p>
                  </div>
                </div>
                <button type="button" className="modal-close-btn" onClick={() => setDeleteModalRow(null)} aria-label="Close">
                  <CloseSvg fill="#64748b" width="18" height="18" />
                </button>
              </div>

              <div style={{ padding: "1.25rem 1.5rem", background: "#f8fafc" }}>
                <p style={{ margin: 0, fontSize: "0.875rem", color: "#334155" }}>
                  <strong>{t("WBH_FORM_NAME") || "Form Name"}:</strong> {deleteModalRow.formName}
                </p>
                <p style={{ margin: "0.35rem 0 0 0", fontSize: "0.85rem", color: "#64748b" }}>
                  <strong>{t("WBH_MODULE_NAME") || "Module"}:</strong> {deleteModalRow.moduleName} |{" "}
                  <strong>{t("WBH_ACCORDION") || "Accordion"}:</strong> {deleteModalRow.accordionName}
                </p>
              </div>

              <div className="create-form-modal-footer">
                <Button
                  type="button"
                  className="modal-btn-cancel"
                  label={t("WBH_CANCEL") || "Cancel"}
                  variation="secondary"
                  onButtonClick={() => setDeleteModalRow(null)}
                />
                <Button
                  type="button"
                  className="modal-btn-submit"
                  style={{ background: "#dc2626" }}
                  label={t("WBH_CONFIRM_DELETE") || "Delete Configuration"}
                  onButtonClick={confirmDeleteForm}
                />
              </div>
            </div>
          </div>
        </PopUp>
      )}

      {/* Fixed Bottom Action Bar */}
      <ActionBar>
        <SubmitBar disabled={false} onSubmit={handleAddNewField} label={t(Config?.actionLabel || "WBH_ADD_NEW_FIELD")} />
      </ActionBar>

      {/* Sample JSON Schema Popup Modal */}
      {showSampleModal && (
        <PopUp>
          <div className="create-form-modal-overlay">
            <div className="create-form-modal-container" style={{ maxWidth: "780px" }}>
              <div className="create-form-modal-header">
                <div className="modal-title-wrapper">
                  <div className="modal-icon-badge" style={{ background: "#eff6ff", borderColor: "#bfdbfe" }}>
                    <span style={{ fontSize: "1.2rem" }}>📋</span>
                  </div>
                  <div>
                    <h2 className="modal-title-text">{t("WBH_SAMPLE_JSON_TITLE") || "Sample UPYOG Form JSON Schema"}</h2>
                    <p className="modal-subtitle-text">
                      {t("WBH_SAMPLE_JSON_DESC") || "Standard UPYOG MDMS single-component JSON schema format for uploading forms."}
                    </p>
                  </div>
                </div>
                <button type="button" className="modal-close-btn" onClick={() => setShowSampleModal(false)} aria-label="Close">
                  <CloseSvg fill="#64748b" width="18" height="18" />
                </button>
              </div>

              <div style={{ padding: "1.25rem 1.5rem", background: "#1e293b", maxHeight: "55vh", overflowY: "auto" }}>
                <pre
                  style={{
                    color: "#f8fafc",
                    fontFamily: "monospace",
                    fontSize: "0.8rem",
                    margin: 0,
                    whiteSpace: "pre-wrap",
                    wordBreak: "break-word",
                  }}
                >
                  {JSON.stringify(SAMPLE_UPYOG_JSON, null, 2)}
                </pre>
              </div>

              <div className="create-form-modal-footer">
                <Button
                  type="button"
                  className="modal-btn-cancel"
                  label={t("WBH_COPY_JSON") || "📋 Copy JSON"}
                  variation="secondary"
                  onButtonClick={handleCopySampleJson}
                />
                <Button
                  type="button"
                  className="modal-btn-cancel"
                  label={t("WBH_DOWNLOAD_SAMPLE") || "⬇️ Download Sample"}
                  variation="secondary"
                  onButtonClick={handleDownloadSampleJson}
                />
                <Button
                  type="button"
                  className="modal-btn-submit"
                  label={t("WBH_CLOSE") || "Close"}
                  onButtonClick={() => setShowSampleModal(false)}
                />
              </div>
            </div>
          </div>
        </PopUp>
      )}

      {toast && (
        <div className="form-builder-toast-wrapper">
          <Toast label={toast.label} error={toast.error} onClose={() => setToast(null)} isDleteBtn={true} />
        </div>
      )}
    </React.Fragment>
  );
};

export default FormBuilder;
