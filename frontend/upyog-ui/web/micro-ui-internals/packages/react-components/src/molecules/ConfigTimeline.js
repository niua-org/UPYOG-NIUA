/**
 * ConfigTimeline.js
 *
 * Stepper used on citizen create flows (Trade License units-details and the
 * same four steps on the config-driven application). Labels, order, and which
 * route or accordion section lights each step come from navigation.timeline.
 *
 * @see DynamicFormAccordionStep
 */

import React from "react";
import { useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { TickMark } from "../atoms/svgindex";

const routeSegment = (pathname = "") => {
  const parts = String(pathname).split("/").filter(Boolean);
  return parts[parts.length - 1] || "";
};

const hasValue = (value) => {
  if (value === undefined || value === null || value === "") return false;
  if (Array.isArray(value)) return value.length > 0;
  if (typeof value === "object") {
    return Boolean(
      value.filestoreId ||
        value.fileStoreId ||
        value.documentuuid ||
        value.code ||
        value.name
    );
  }
  return true;
};

/**
 * Pick the active step from navigation.timeline.
 * A route with no section list (summary / check) wins.
 * Otherwise the open accordion section, then a field list on that section
 * (document uploads advance Document Details).
 *
 * @param {object} timeline
 * @param {object} context
 * @param {string} [context.route]
 * @param {string} [context.activeSection]
 * @param {string} [context.activeField]
 * @param {object} [context.values]
 * @returns {number|null}
 */
export const resolveConfigTimelineStep = (
  timeline,
  { route, activeSection, activeField, values = {} } = {}
) => {
  const steps = Array.isArray(timeline?.steps) ? timeline.steps : [];
  if (!steps.length) return null;

  const routeSteps = route
    ? steps.filter((step) => Array.isArray(step.routes) && step.routes.includes(route))
    : [];
  const exclusiveRoute = routeSteps.filter((step) => !step.sections?.length);
  if (exclusiveRoute.length) {
    return Math.max(...exclusiveRoute.map((step) => Number(step.step) || 0));
  }

  const sectionSteps = activeSection
    ? steps.filter(
        (step) => Array.isArray(step.sections) && step.sections.includes(activeSection)
      )
    : [];

  if (sectionSteps.length) {
    if (activeField) {
      const byField = sectionSteps.find((step) => step.fields?.includes(activeField));
      if (byField) return byField.step;
      const plain = sectionSteps.find((step) => !step.fields?.length);
      if (plain) return plain.step;
    }
    const filled = [...sectionSteps]
      .reverse()
      .find((step) => step.fields?.some((name) => hasValue(values?.[name])));
    if (filled) return filled.step;
    const plain = sectionSteps.find((step) => !step.fields?.length);
    return (plain || sectionSteps[0]).step;
  }

  if (routeSteps.length) {
    return Math.max(...routeSteps.map((step) => Number(step.step) || 0));
  }

  const sorted = [...steps].sort((a, b) => a.step - b.step);
  return sorted[0]?.step ?? null;
};

const stepLabel = (step, t) => {
  const key = step.label || step.actions;
  const fallback = step.labelDefault || key;
  if (!key) return fallback || "";
  const translated = t(key, { defaultValue: fallback });
  if (!translated || translated === key) return fallback;
  return translated;
};

/**
 * @param {object} props
 * @param {object} props.timeline - navigation.timeline from the form master
 * @param {number} [props.currentStep] - When set, skips route/section resolution
 * @param {string} [props.activeSection]
 * @param {string} [props.activeField]
 * @param {object} [props.values]
 * @param {Function} [props.t]
 * @param {string} [props.className]
 * @param {boolean} [props.fullWidth] Skip the default 640–960px width lock.
 */
const ConfigTimeline = ({
  timeline,
  currentStep,
  activeSection,
  activeField,
  values,
  t: tProp,
  className,
  fullWidth = false,
}) => {
  const { t: tHook } = useTranslation();
  const t = tProp || tHook;
  const { pathname } = useLocation();
  const steps = Array.isArray(timeline?.steps) ? timeline.steps : [];
  if (!steps.length) return null;

  const citizenOnly = timeline.citizenOnly !== false;
  if (citizenOnly && !pathname.includes("/citizen")) return null;

  const route = routeSegment(pathname);
  const hidden = Array.isArray(timeline.hideOnRoutes) && timeline.hideOnRoutes.includes(route);
  if (hidden) return null;

  const active =
    typeof currentStep === "number"
      ? currentStep
      : resolveConfigTimelineStep(timeline, {
          route,
          activeSection,
          activeField,
          values,
        });
  if (active == null) return null;

  const unique = [];
  [...steps]
    .sort((a, b) => a.step - b.step)
    .forEach((step) => {
      if (!unique.some((item) => item.step === step.step)) unique.push(step);
    });

  const isMobile = window.Digit?.Utils?.browser?.isMobile?.();

  return (
    <div
      className={["timeline-container", className].filter(Boolean).join(" ")}
      style={
        isMobile || fullWidth
          ? {}
          : { maxWidth: "960px", minWidth: "640px", marginRight: "auto" }
      }
    >
      {unique.map((step, index, arr) => (
        <div className="timeline-checkpoint" key={step.step}>
          <div className="timeline-content">
            <span className={`circle ${step.step <= active ? "active" : ""}`}>
              {step.step < active ? <TickMark /> : index + 1}
            </span>
            <span className="secondary-color">{stepLabel(step, t)}</span>
          </div>
          {index < arr.length - 1 && (
            <span className={`line ${step.step < active ? "active" : ""}`}></span>
          )}
        </div>
      ))}
    </div>
  );
};

export default ConfigTimeline;
