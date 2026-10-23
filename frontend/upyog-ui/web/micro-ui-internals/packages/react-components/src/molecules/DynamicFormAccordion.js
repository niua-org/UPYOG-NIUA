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
 * - showContinue       per-panel continue button (only when true, or when continueLabel is set)
 * - continueOpensNext   continue button opens the following panel
 * - continueLabel       i18n key for the per-panel continue button
 * - continueLabelDefault fallback continue text
 * - showFilledHint      header shows required-field status (pending or completed)
 * - openNextOnComplete when a section's mandatory fields become valid, close
 *   that section and open the next one
 *
 * @see DynamicFormAccordionStep
 * @see buildAccordionSections
 */

import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import ButtonSelector from "../atoms/ButtonSelector";
import { evaluateFormRule, getFieldArrayName, isFieldVisible, sectionHasFieldError } from "../utilities/formUtils";
import { validateFields } from "../utilities/validators";

const ChevronIcon = () => (
  <svg className="dynamic-form-accordion__chevron" viewBox="0 0 20 20" aria-hidden="true">
    <path
      fill="currentColor"
      d="M5.3 7.3a1 1 0 0 1 1.4 0L10 10.6l3.3-3.3a1 1 0 1 1 1.4 1.4l-4 4a1 1 0 0 1-1.4 0l-4-4a1 1 0 0 1 0-1.4z"
    />
  </svg>
);

/** True when every visible field in the panel is filled and valid. */
const sectionCanContinue = (form, formData) =>
  Object.keys(validateFields(form || [], formData)).length === 0;

const isRequiredNow = (fieldConfig, data) => {
  const validation = fieldConfig?.validation || {};
  if (validation.required) return true;
  return Boolean(
    validation.requiredWhen && evaluateFormRule(validation.requiredWhen, data || {})
  );
};

/**
 * Visible fields that are required right now.
 * Field-array children use the same row scope as validateFields.
 * @returns {"none"|"pending"|"complete"}
 */
const getRequiredSectionStatus = (form, formData) => {
  const data = formData || {};
  let requiredCount = 0;

  const countLeaf = (fieldConfig, scope) => {
    if (!fieldConfig?.field?.name) return;
    if (!isFieldVisible(fieldConfig, scope)) return;
    if (!isRequiredNow(fieldConfig, scope)) return;
    requiredCount += 1;
  };

  (form || []).forEach((item) => {
    if (!item || item.type === "sectionHeader" || item.hidden === true) return;
    if (!isFieldVisible(item, data)) return;

    if (item.type === "fieldArray") {
      const name = getFieldArrayName(item);
      const rows = Array.isArray(data[name]) ? data[name] : [];
      const minItems = Math.max(1, Number(item.minItems) || 1);
      const effectiveRows =
        rows.length > 0 ? rows : Array.from({ length: minItems }, () => ({}));
      effectiveRows.forEach((row) => {
        (item.children || []).forEach((child) => countLeaf(child, row || {}));
      });
      return;
    }

    if (item.type === "group") {
      (item.children || []).forEach((child) => countLeaf(child, data));
      return;
    }

    countLeaf(item, data);
  });

  if (requiredCount === 0) return "none";
  return sectionCanContinue(form, data) ? "complete" : "pending";
};

/** Smooth unless the user asked the browser to reduce motion. */
const scrollBehavior = () =>
  window.matchMedia?.("(prefers-reduced-motion: reduce)")?.matches ? "auto" : "smooth";

/**
 * Bring a newly opened section heading into view with the smallest scroll.
 * The heading is the target, not the whole panel, so a tall section is not
 * pulled to the top of the page. Focus stays where it is.
 */
const revealSectionHeader = (sectionId) => {
  const item = document.getElementById(`dynamic-form-accordion-${sectionId}`);
  const head = item?.querySelector(".dynamic-form-accordion__head");
  if (!head) return;
  head.scrollIntoView({
    behavior: scrollBehavior(),
    block: "nearest",
    inline: "nearest",
  });
};

const translateWithFallback = (t, key, fallback) => {
  const translated = t(key, { defaultValue: fallback });
  if (!translated || translated === key) return fallback;
  return translated;
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
 * @param {Function} [props.onSectionBlocked] (section) => void. Earlier panel that still has errors.
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
  onSectionBlocked,
  onActiveSectionChange,
}) => {
  const allowMultipleOpen = accordion.allowMultipleOpen !== false;
  const continueOpensNext = accordion.continueOpensNext !== false;
  const showFilledHint = accordion.showFilledHint === true;
  const openNextOnComplete = accordion.openNextOnComplete === true;
  const showContinue =
    accordion.showContinue === true ||
    Boolean(accordion.continueLabel || accordion.continueLabelDefault);
  const continueLabel = accordion.continueLabel || "COMMON_SAVE_NEXT";
  const continueLabelDefault = accordion.continueLabelDefault || "Save & continue";

  const defaultOpenId = useMemo(() => {
    const requested = accordion.defaultOpenStep;
    if (requested && sections.some((s) => s.id === requested)) return requested;
    return sections[0]?.id || null;
  }, [accordion.defaultOpenStep, sections]);

  const [openIds, setOpenIds] = useState(() => new Set(defaultOpenId ? [defaultOpenId] : []));
  /** Section the citizen tried to open before its required fields were ready. */
  const [blockedId, setBlockedId] = useState(null);

  const pendingHint = translateWithFallback(
    t,
    "COMMON_ACCORDION_INCOMPLETE",
    "Incomplete - Please fill all the mandatory fields"
  );
  const completeHint = translateWithFallback(
    t,
    "COMMON_ACCORDION_COMPLETE",
    "Completed"
  );
  const blockedMessage = translateWithFallback(
    t,
    "COMMON_ACCORDION_SECTION_LOCKED",
    "Please fill mandatory fields before opening this section."
  );

  // One validation pass per form change. The joined string stays stable when
  // the pending/complete result did not change, so the auto-open effect skips.
  const statusSnapshot = useMemo(
    () => sections.map((section) => getRequiredSectionStatus(section?.form, formData)).join("|"),
    [sections, formData]
  );
  const sectionStatuses = useMemo(
    () => (statusSnapshot ? statusSnapshot.split("|") : []),
    [statusSnapshot]
  );

  const canOpenAt = useCallback(
    (index) => {
      if (!showFilledHint) return true;
      if (sectionStatuses[index] === "none") return false;
      for (let i = 0; i < index; i += 1) {
        if (sectionStatuses[i] === "pending") return false;
      }
      return true;
    },
    [showFilledHint, sectionStatuses]
  );

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

  useEffect(() => {
    if (!blockedId) return;
    const index = sections.findIndex((section) => section.id === blockedId);
    if (index < 0 || canOpenAt(index)) setBlockedId(null);
  }, [blockedId, canOpenAt, sections]);

  const prevStatusRef = useRef(null);

  // When a panel's required fields become valid, close it and open the next one.
  // A panel that had no required fields and then gains some opens once the one above is complete.
  useEffect(() => {
    const statuses = sectionStatuses;
    const previous = prevStatusRef.current;
    prevStatusRef.current = statuses;
    // First paint has no previous snapshot, so nothing auto-opens on load.
    if (!openNextOnComplete || !previous || previous.length !== statuses.length) return;

    let openIndex = -1;
    let completedIndex = -1;
    statuses.forEach((status, index) => {
      if (previous[index] !== "complete" && status === "complete") completedIndex = index;
    });
    if (completedIndex >= 0) {
      const nextIndex = completedIndex + 1;
      const nextStatus = statuses[nextIndex];
      const earlierStillPending = statuses
        .slice(0, nextIndex)
        .some((status) => status === "pending");
      if (!earlierStillPending && nextStatus && nextStatus !== "none") openIndex = nextIndex;
    }
    if (openIndex < 0) {
      statuses.forEach((status, index) => {
        if (index === 0 || openIndex >= 0) return;
        const previousReady = statuses[index - 1] === "complete";
        if (previous[index] === "none" && status === "pending" && previousReady) openIndex = index;
      });
    }
    if (openIndex < 0) return;

    const next = sections[openIndex];
    const finished = sections[openIndex - 1];
    if (!next || !finished) return;

    setBlockedId(null);
    setOpenIds((prev) => {
      const nextIds = allowMultipleOpen ? new Set(prev) : new Set();
      nextIds.delete(finished.id);
      nextIds.add(next.id);
      return nextIds;
    });
    reportSection(next.id);
    const nextId = next.id;
    requestAnimationFrame(() => {
      revealSectionHeader(nextId);
    });
  }, [sectionStatuses, openNextOnComplete, sections, allowMultipleOpen, reportSection]);

  const toggle = (id) => {
    const index = sections.findIndex((section) => section.id === id);
    if (index < 0) return;
    const isOpen = openIds.has(id);

    if (!isOpen && !canOpenAt(index)) {
      setBlockedId(id);
      const earlier = sections
        .slice(0, index)
        .find((_, earlierIndex) => sectionStatuses[earlierIndex] === "pending");
      if (earlier) {
        openSection(earlier.id);
        onSectionBlocked?.(earlier);
      }
      return;
    }

    setBlockedId(null);
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
    if (!next) return;
    if (!canOpenAt(index + 1)) {
      setBlockedId(next.id);
      return;
    }
    setBlockedId(null);
    openSection(next.id, { exclusive: !allowMultipleOpen });
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
          const hint = section.hintKey
            ? translateWithFallback(t, section.hintKey, section.hintDefault)
            : section.hintDefault;
          const status = showFilledHint ? sectionStatuses[index] : null;
          const earlierHasPending = sectionStatuses
            .slice(0, index)
            .some((earlier) => earlier === "pending");
          // Incomplete hint belongs only on the section the citizen can fill now.
          // A later section blocked by unfinished fields above stays quiet.
          const hintState =
            status === "complete"
              ? "complete"
              : status === "pending" && !earlierHasPending
                ? "pending"
                : null;
          const statusLabel =
            hintState === "complete" ? completeHint : hintState === "pending" ? pendingHint : null;
          const isLast = index === sections.length - 1;
          const canContinue = sectionCanContinue(section.form, formData);
          const isBlocked = blockedId === section.id;
          const itemClass = [
            "dynamic-form-accordion__item",
            isOpen ? "dynamic-form-accordion__item--open" : "",
            hasError ? "dynamic-form-accordion__item--error" : "",
            isBlocked ? "dynamic-form-accordion__item--locked" : "",
          ]
            .filter(Boolean)
            .join(" ");

          return (
            <div
              key={section.id}
              id={`dynamic-form-accordion-${section.id}`}
              className={itemClass}
              role="listitem"
            >
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
                  {statusLabel ? (
                    <span className={`dynamic-form-accordion__hint dynamic-form-accordion__hint--${hintState}`}>
                      {statusLabel}
                    </span>
                  ) : null}
                  <ChevronIcon />
                </span>
              </button>
              {isBlocked ? (
                <p className="dynamic-form-accordion__notice" role="alert">
                  {blockedMessage}
                </p>
              ) : null}
              <div className="dynamic-form-accordion__body" hidden={!isOpen}>
                {isOpen ? (
                  <>
                    {renderFields?.(section.form)}
                    {!isDisabled && !isLast && showContinue && onContinue ? (
                      <div className="dynamic-form-accordion__footer">
                        <ButtonSelector
                          theme="border"
                          className="dynamic-form-field__lookup-btn"
                          isDisabled={!canContinue}
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
