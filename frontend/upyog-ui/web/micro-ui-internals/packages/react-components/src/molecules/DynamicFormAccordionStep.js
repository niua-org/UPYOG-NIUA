/**
 * DynamicFormAccordionStep.js
 *
 * Accordion variation of DynamicFormStep. Same MDMS field engine (DynamicForm),
 * different layout: all `body[]` steps (or sectionHeader groups) on one page.
 *
 * Use when JSON has `navigation.pattern = "accordion"` (see AccordionNewApplication.json).
 * Wizard routes should keep using DynamicFormStep (one body item per route).
 *
 * Responsibilities
 * ----------------
 * 1. buildAccordionSections(config, localOverrides.form) → panels.
 * 2. Concatenate panel forms into one routeConfig.form so MDMS, compute,
 *    gates, and payload stay a single DynamicForm instance.
 * 3. Render Header from config.head, then DynamicForm with layout="accordion".
 * 4. Bridge onSelect / draft the same way DynamicFormStep does.
 *
 * Typical usage
 * -------------
 *   <DynamicFormAccordionStep
 *     config={fullMdmsEntry}
 *     localOverrides={tlFormConfig}
 *     onSelect={onSelect}
 *     formData={formData}
 *     t={t}
 *     onFieldSearch={handleFieldSearch}
 *   />
 *
 * @see DynamicFormStep
 * @see DynamicFormAccordion
 * @see buildAccordionSections
 */

import React, { useMemo, useCallback, useState } from "react";
import Header from "../atoms/Header";
import DynamicForm from "./DynamicForm";
import ConfigTimeline from "./ConfigTimeline";
import {
  attachRouteConfigToStepData,
  mergeRouteConfig,
} from "../utilities/checkPageUtils";
import { buildAccordionSections } from "../utilities/formUtils";

/**
 * One-page accordion shell around DynamicForm.
 *
 * @param {object} props — same as DynamicFormStep, but `config` is the full
 *   MDMS entry (`head`, `navigation`, `body[]`) rather than a single body step.
 *   `reviseFormData` is forwarded so a module can copy owner address and radios
 *   after each edit without the accordion owning that trade-license rule.
 * @returns {JSX.Element}
 */
const DynamicFormAccordionStep = ({
  config,
  localOverrides = {},
  onSelect,
  persistedData,
  formData,
  isEditMode = false,
  editData = {},
  resetBaseline,
  draft,
  t: tProp,
  tenantId: tenantIdProp,
  wrapperClassName = "employeeCard",
  defaultHeaderCode = "COMMON_FORM",
  onFieldSearch,
  onPrefillApplied,
  confirmCancel = false,
  enrichFieldArrayRow = null,
  transformDropdownData = null,
  onFieldChange = null,
  // Module hook forwarded to DynamicForm. Null for modules that do not copy fields.
  reviseFormData = null,
  pageClassName,
}) => {
  const t = tProp || ((key) => key);
  const sessionData = formData ?? persistedData ?? {};
  const [timelineStep, setTimelineStep] = useState(null);
  const handleTimelineStep = useCallback((step) => {
    setTimelineStep((prev) => (prev === step ? prev : step));
  }, []);

  const sections = useMemo(
    () => buildAccordionSections(config, localOverrides.form || []),
    [config, localOverrides.form]
  );

  const combinedForm = useMemo(
    () => sections.flatMap((section) => section.form || []),
    [sections]
  );

  const firstStep = config?.body?.[0] || config || {};

  const routeConfig = useMemo(() => {
    const merged = mergeRouteConfig(
      {
        ...firstStep,
        form: combinedForm,
        key: config?.key || config?.sessionKey || "newApplication",
        navigation: config?.navigation,
        pageHeading: firstStep?.pageHeading || {
          create: config?.head,
        },
        actionButton: {
          text: {
            create:
              config?.navigation?.accordion?.submitLabel ||
              firstStep?.texts?.submitBarLabel ||
              "COMMON_SUBMIT_APPLICATION",
          },
        },
      },
      { ...localOverrides, form: undefined }
    );
    merged.form = combinedForm;
    merged.navigation = config?.navigation || merged.navigation;
    return merged;
  }, [firstStep, combinedForm, localOverrides, config]);

  const tenantId = useMemo(
    () => tenantIdProp || Digit.ULBService.getCurrentTenantId(),
    [tenantIdProp]
  );

  const handleSubmit = useCallback(({ error }) => {
    if (error) console.error("Submit error:", error);
  }, []);

  const handleSelect = useCallback(
    (key, data, skipStep, index, isAddMultiple) => {
      onSelect?.(
        key,
        attachRouteConfigToStepData(data, routeConfig),
        skipStep,
        index,
        isAddMultiple
      );
    },
    [onSelect, routeConfig]
  );

  const handleDraftSelect = useCallback(
    (key, data, skipStep, index, isAddMultiple) => {
      if (!draft?.buildStepData) {
        handleSelect(key, data, skipStep, index, isAddMultiple);
        return;
      }
      const saved = data?.[routeConfig.payloadKey]?.[0] || {};
      onSelect?.(
        key,
        attachRouteConfigToStepData(draft.buildStepData(saved), routeConfig),
        skipStep,
        index,
        isAddMultiple
      );
    },
    [draft, handleSelect, onSelect, routeConfig]
  );
// this function is used ot save the draft
  const onSaveDraft = draft?.onPersist
    ? (flat) => draft.onPersist(routeConfig.key, draft.buildStepData?.(flat) || flat)
    : undefined;

  const headerCode =
    routeConfig.pageHeading?.create ||
    config?.head ||
    config?.sectionHeading ||
    config?.texts?.header ||
    defaultHeaderCode;

  const translatedHeader = t(headerCode);
  const headerFallback =
    routeConfig.pageHeading?.fallback ||
    config?.headDefault ||
    "Application";
  const headerText =
    translatedHeader && translatedHeader !== headerCode
      ? translatedHeader
      : headerFallback;
  const subtitleDefault =
    config?.navigation?.accordion?.subtitleDefault || "All sections on one page";
  const subtitleKey =
    config?.navigation?.accordion?.subtitleKey || "COMMON_ACCORDION_SUBTITLE";
  const subtitleTranslated = t(subtitleKey, { defaultValue: subtitleDefault });
  const subtitleText =
    !subtitleTranslated || subtitleTranslated === subtitleKey
      ? subtitleDefault
      : subtitleTranslated;

  const handleCancel = () => draft?.onClear?.(routeConfig.key);

  const timeline = config?.navigation?.timeline;

  const page = (
    <>
      {timeline?.steps?.length ? (
        <ConfigTimeline
          timeline={timeline}
          currentStep={timelineStep}
          t={t}
          className={pageClassName ? `${pageClassName}__timeline` : undefined}
          fullWidth={Boolean(pageClassName)}
        />
      ) : null}
      <div className={wrapperClassName}>
      <div className="dynamic-form-accordion-pagehead">
        <Header>{headerText}</Header>
        <p className="dynamic-form-accordion-pagehead__sub">{subtitleText}</p>
      </div>
      <DynamicForm
        layout="accordion"
        sections={sections}
        routeConfig={routeConfig}
        onSubmit={handleSubmit}
        onSelect={draft?.buildStepData ? handleDraftSelect : handleSelect}
        config={{ key: routeConfig.key }}
        persistedData={sessionData}
        isEditMode={isEditMode}
        editData={editData || {}}
        resetBaseline={resetBaseline}
        tenantId={tenantId}
        t={t}
        showCancel
        confirmCancel={confirmCancel}
        onCancel={handleCancel}
        showDraftButton={Boolean(draft?.onPersist)}
        draftLabel={routeConfig.draftButton?.label || draft?.label || "CS_COMMON_SAVE_DRAFT"}
        draftSuccessLabel={
          routeConfig.draftButton?.successMessage || draft?.successLabel || "CS_COMMON_SAVED"
        }
        onSaveDraft={onSaveDraft}
        onFieldSearch={onFieldSearch}
        onPrefillApplied={onPrefillApplied}
        enrichFieldArrayRow={enrichFieldArrayRow}
        transformDropdownData={transformDropdownData}
        onFieldChange={onFieldChange}
        reviseFormData={reviseFormData}
        onTimelineStep={timeline?.steps?.length ? handleTimelineStep : undefined}
      />
    </div>
    </>
  );

  if (!pageClassName) return page;
  return <div className={pageClassName}>{page}</div>;
};

export default DynamicFormAccordionStep;
