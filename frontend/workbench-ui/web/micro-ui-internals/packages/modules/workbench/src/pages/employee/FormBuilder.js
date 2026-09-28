import {
  Header,
  InboxSearchComposer,
  ActionBar,
  SubmitBar,
  PopUp,
  Dropdown,
  CardLabel,
  CardLabelError,
  Button,
  SVG,
  CloseSvg,
} from "@upyog/workbench-ui-react-components";
import React, { useState, useMemo, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { Controller, useForm, useWatch } from "react-hook-form";
import { Config } from "../../configs/FormBuilderConfig";
import { FORM_BUILDER_DATA } from "../../hooks/useFormBuilderSearch";
import {
  useModalState,
  getModuleListFromMDMS,
  getAccordionsForModule,
  getFormsForModule,
} from "../../utils/workbenchUtils";

const FormBuilder = () => {
  const { t } = useTranslation();
  const navigate = Digit.Hooks.useCustomNavigate();
  const [callRefetch, setCallRefetch] = useState(false);
  const [tableData, setTableData] = useState(FORM_BUILDER_DATA);

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
    defaultValues: {
      moduleName: "",
      accordionName: "",
      formName: "",
    },
  });

  // Watch selected moduleName
  const selectedModuleName = useWatch({ control, name: "moduleName" });

  // Selected module object matching selected module code
  const selectedModuleObj = useMemo(() => {
    if (!selectedModuleName) return null;
    return modulesList.find((m) => m?.code === selectedModuleName || m?.name === selectedModuleName);
  }, [modulesList, selectedModuleName]);

  // 2. Dependent Accordion dropdown options based on selected module
  const accordionOptions = useMemo(() => {
    return getAccordionsForModule(selectedModuleObj, []);
  }, [selectedModuleObj]);

  // 3. Dependent Form dropdown options based on selected module
  const formOptions = useMemo(() => {
    return getFormsForModule(selectedModuleObj, []);
  }, [selectedModuleObj]);

  // Reset dependent Accordion & Form fields whenever selected module changes
  useEffect(() => {
    if (selectedModuleName) {
      setValue("accordionName", "");
      setValue("formName", "");
    }
  }, [selectedModuleName, setValue]);

  const handleCreateFormSubmit = (data) => {
    const newItem = {
      id: `${Date.now()}`,
      moduleName: data.moduleName,
      accordionName: data.accordionName,
      formName: data.formName,
    };

    setTableData((prev) => [newItem, ...prev]);
    handleModalCloseRaw();
    reset({ moduleName: "", accordionName: "", formName: "" });
    setCallRefetch((prev) => !prev);

    // Navigate to the new Form Field Builder page with all 3 popup field values
    navigate(
      `/${window?.contextPath}/employee/workbench/form-create?moduleName=${encodeURIComponent(
        data.moduleName
      )}&accordionName=${encodeURIComponent(data.accordionName)}&formName=${encodeURIComponent(
        data.formName
      )}`,
      {
        state: {
          moduleName: data.moduleName,
          accordionName: data.accordionName,
          formName: data.formName,
        },
      }
    );
  };

  const handleModalClose = () => {
    handleModalCloseRaw();
    reset({ moduleName: "", accordionName: "", formName: "" });
  };

  const onClickSvg = (row) => {
    const rowData = row?.original || row || {};
    console.log("Selected row for configuration edit: ", rowData);
    navigate(
      `/${window?.contextPath}/employee/workbench/form-create?moduleName=${encodeURIComponent(
        rowData.moduleName || ""
      )}&accordionName=${encodeURIComponent(rowData.accordionName || "")}&formName=${encodeURIComponent(
        rowData.formName || ""
      )}&isEdit=true`,
      {
        state: {
          isEdit: true,
          moduleName: rowData.moduleName,
          accordionName: rowData.accordionName,
          formName: rowData.formName,
          existingFields: rowData.fields || rowData.schemaData || null,
        },
      }
    );
  };

  return (
    <React.Fragment>
      <div className="jk-header-btn-wrapper form-builder-header-wrapper">
        <Header className="works-header-search">{t(Config?.label || "WBH_FORM_BUILDER_HEADER")}</Header>
        <ActionBar>
          <SubmitBar disabled={false} onSubmit={handleAddNewField} label={t(Config?.actionLabel || "WBH_ADD_NEW_FIELD")} />
        </ActionBar>
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
                        <CardLabelError className="form-field-error">
                          {t("WBH_MODULE_NAME_REQUIRED") || "Module Name is required"}
                        </CardLabelError>
                      )}
                    </div>

                    {/* 2. Dependent Accordion Name Dropdown */}
                    <div className="form-group-item">
                      <CardLabel className="form-field-label">
                        {t("WBH_ACCORDION_NAME")} <span className="mandatory-asterisk">*</span>
                      </CardLabel>
                      <Controller
                        name="accordionName"
                        control={control}
                        rules={{ required: true }}
                        render={({ field: { onChange, value } }) => (
                          <Dropdown
                            option={accordionOptions}
                            optionKey="name"
                            selected={accordionOptions.find((a) => a.code === value)}
                            select={(val) => onChange(val?.code)}
                            t={t}
                            placeholder={t("WBH_SELECT_INITIAL_ACCORDION")}
                            disable={!selectedModuleName}
                          />
                        )}
                      />
                      {errors?.accordionName && (
                        <CardLabelError className="form-field-error">
                          {t("WBH_ACCORDION_NAME_REQUIRED") || "Accordion Name is required"}
                        </CardLabelError>
                      )}
                    </div>

                    {/* 3. Dependent Form Name Dropdown */}
                    <div className="form-group-item">
                      <CardLabel className="form-field-label">
                        {t("WBH_FORM_NAME")} <span className="mandatory-asterisk">*</span>
                      </CardLabel>
                      <Controller
                        name="formName"
                        control={control}
                        rules={{ required: true }}
                        render={({ field: { onChange, value } }) => (
                          <Dropdown
                            option={formOptions}
                            optionKey="name"
                            selected={formOptions.find((f) => f.code === value)}
                            select={(val) => onChange(val?.code)}
                            t={t}
                            placeholder={t("WBH_SELECT_TARGET_FORM")}
                            disable={!selectedModuleName}
                          />
                        )}
                      />
                      {errors?.formName && (
                        <CardLabelError className="form-field-error">
                          {t("WBH_FORM_NAME_REQUIRED") || "Form Name is required"}
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

                      <p className="schema-description">
                        {t("WBH_SCHEMA_HIERARCHY_DESC")}
                      </p>

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

                  <SubmitBar submit={true} className="modal-btn-submit" label={t("WBH_PROCEED_TO_FORM_BUILDER")} onSubmit={handleSubmit(handleCreateFormSubmit)} />
                </div>
              </form>
            </div>
          </div>
        </PopUp>
      )}
    </React.Fragment>
  );
};

export default FormBuilder;