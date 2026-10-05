/**
 * DynamicFormAccordion.js
 *
 * Layout-only accordion for DynamicForm. Does not own form state — parent
 * still hydrates, validates, and submits. Each JSON `body[]` (or sectionHeader
 * split) is one panel; fields inside are rendered via `renderFields`.
 *
 * JSON (`navigation.accordion`)
 * -----------------------------
 * - allowMultipleOpen   several panels open at once (default true)
 * - defaultOpenStep     panel id to expand on first paint
 * - continueOpensNext   Save & continue opens the following panel
 * - continueLabel       i18n key for the per-panel continue button
 * - continueLabelDefault fallback continue text
 * - showFilledHint      show filled/total on the header
 *
 * @see DynamicFormAccordionStep
 * @see buildAccordionSections
 */

import React, { useCallback, useEffect, useMemo, useState } from "react";
import ButtonSelector from "../atoms/ButtonSelector";
import { flattenFormConfig, isFieldVisible, sectionHasFieldError } from "../utilities/formUtils";

const ChevronIcon = () => (
  <svg className="dynamic-form-accordion__chevron" viewBox="0 0 20 20" aria-hidden="true">
    <path
      fill="currentColor"
      d="M5.3 7.3a1 1 0 0 1 1.4 0L10 10.6l3.3-3.3a1 1 0 1 1 1.4 1.4l-4 4a1 1 0 0 1-1.4 0l-4-4a1 1 0 0 1 0-1.4z"
    />
  </svg>
);

const isFilledValue = (value) => {
  if (value === undefined || value === null || value === "") return false;
  if (Array.isArray(value)) return value.length > 0;
  if (typeof value === "object") {
    return Boolean(value.code || value.filestoreId || value.fileStoreId || value.documentuuid);
  }
  return true;
};

const countFilledFields = (form, formData) => {
  const leaves = flattenFormConfig(form || []).filter(
    (fc) => fc?.field?.name && isFieldVisible(fc, formData)
  );
  const filled = leaves.filter((fc) => isFilledValue(formData?.[fc.field.name]));
  return { filled: filled.length, total: leaves.length };
};

const resolvePanelTitle = (section, t) => {
  const key = section.titleKey || section.id;
  const fallback = section.titleDefault;
  const translated = fallback ? t(key, { defaultValue: fallback }) : t(key);
  if (fallback && (!translated || translated === key)) return fallback;
  return translated || fallback || key;
};

/**
 * Accordion shell around DynamicFormField groups.
 *
 * @param {object}   props
 * @param {Array}    props.sections       From buildAccordionSections.
 * @param {object}   props.formData       Live DynamicForm state (filled hints).
 * @param {object}   props.errors         Field error map.
 * @param {object}   [props.accordion]    navigation.accordion JSON.
 * @param {Function} props.t
 * @param {boolean}  [props.isDisabled]
 * @param {string}   [props.focusSectionId] Force-open this panel (e.g. after submit errors).
 * @param {Function} props.renderFields   (formSlice) => React nodes.
 * @param {Function} [props.onContinue]   (section) => boolean; false keeps the panel open.
 * @param {Function} [props.onActiveSectionChange] (sectionId) => void. Latest panel the citizen opened.
 */
const DynamicFormAccordion = ({
  sections = [],
  formData = {},
  errors = {},
  accordion = {},
  t = (k) => k,
  isDisabled = false,
  focusSectionId,
  renderFields,
  onContinue,
  onActiveSectionChange,
}) => {
  const allowMultipleOpen = accordion.allowMultipleOpen !== false;
  const continueOpensNext = accordion.continueOpensNext !== false;
  const showFilledHint = accordion.showFilledHint === true;
  const continueLabel = accordion.continueLabel || "COMMON_SAVE_NEXT";
  const continueLabelDefault = accordion.continueLabelDefault || "Save & continue";

  const defaultOpenId = useMemo(() => {
    const requested = accordion.defaultOpenStep;
    if (requested && sections.some((s) => s.id === requested)) return requested;
    return sections[0]?.id || null;
  }, [accordion.defaultOpenStep, sections]);

  const [openIds, setOpenIds] = useState(() => new Set(defaultOpenId ? [defaultOpenId] : []));

  const reportSection = useCallback(
    (id) => {
      if (id) onActiveSectionChange?.(id);
    },
    [onActiveSectionChange]
  );

  const openSection = useCallback(
    (id, { exclusive } = {}) => {
      if (!id) return;
      setOpenIds((prev) => {
        if (exclusive || !allowMultipleOpen) return new Set([id]);
        const next = new Set(prev);
        next.add(id);
        return next;
      });
      reportSection(id);
    },
    [allowMultipleOpen, reportSection]
  );

  useEffect(() => {
    if (defaultOpenId) reportSection(defaultOpenId);
  }, [defaultOpenId, reportSection]);

  useEffect(() => {
    if (focusSectionId) openSection(focusSectionId);
  }, [focusSectionId, openSection]);

  const toggle = (id) => {
    setOpenIds((prev) => {
      if (allowMultipleOpen) {
        const next = new Set(prev);
        if (next.has(id)) next.delete(id);
        else next.add(id);
        return next;
      }
      if (prev.has(id)) return new Set();
      return new Set([id]);
    });
    reportSection(id);
  };

  const handleContinue = (section, index) => {
    if (onContinue && onContinue(section) === false) {
      openSection(section.id);
      return;
    }
    if (!continueOpensNext) return;
    const next = sections[index + 1];
    if (next) openSection(next.id, { exclusive: !allowMultipleOpen });
  };

  if (!sections.length) return null;

  const hintDefault =
    accordion.introDefault ||
    "Expand a section to fill details. Submit when every section is complete.";
  const hintKey = accordion.introKey || "COMMON_ACCORDION_HINT";
  const hintTranslated = t(hintKey, { defaultValue: hintDefault });
  const hintText =
    !hintTranslated || hintTranslated === hintKey ? hintDefault : hintTranslated;

  return (
    <>
      <p className="dynamic-form-accordion__intro">{hintText}</p>
      <div className="dynamic-form-accordion" role="list">
        {sections.map((section, index) => {
          const isOpen = openIds.has(section.id);
          const hasError = sectionHasFieldError(section.form, errors);
          const title = resolvePanelTitle(section, t);
          const hint = section.hintDefault;
          const counts = showFilledHint ? countFilledFields(section.form, formData) : null;
          const isLast = index === sections.length - 1;
          const itemClass = [
            "dynamic-form-accordion__item",
            isOpen ? "dynamic-form-accordion__item--open" : "",
            hasError ? "dynamic-form-accordion__item--error" : "",
          ]
            .filter(Boolean)
            .join(" ");

          return (
            <div key={section.id} className={itemClass} role="listitem">
              <button
                type="button"
                className="dynamic-form-accordion__head"
                aria-expanded={isOpen}
                onClick={() => toggle(section.id)}
              >
                <span className="dynamic-form-accordion__num">{index + 1}</span>
                <span className="dynamic-form-accordion__text">
                  <span className="dynamic-form-accordion__title">{title}</span>
                  {hint ? <span className="dynamic-form-accordion__sub">{hint}</span> : null}
                </span>
                <span className="dynamic-form-accordion__meta">
                  {counts && counts.total > 0 ? (
                    <span className="dynamic-form-accordion__hint">
                      {counts.filled}/{counts.total}
                    </span>
                  ) : null}
                  <ChevronIcon />
                </span>
              </button>
              <div className="dynamic-form-accordion__body" hidden={!isOpen}>
                {isOpen ? (
                  <>
                    {renderFields?.(section.form)}
                    {!isDisabled && !isLast && onContinue ? (
                      <div className="dynamic-form-accordion__footer">
                        <ButtonSelector
                          theme="border"
                          label={
                            t(continueLabel, { defaultValue: continueLabelDefault }) === continueLabel
                              ? continueLabelDefault
                              : t(continueLabel, { defaultValue: continueLabelDefault })
                          }
                          onSubmit={() => handleContinue(section, index)}
                        />
                      </div>
                    ) : null}
                  </>
                ) : null}
              </div>
            </div>
          );
        })}
      </div>
    </>
  );
};

export default DynamicFormAccordion;
