/**
 * resolveFormConfig.js
 *
 * Pick the master array a module should run (local JSON vs MDMS), with optional
 * preference for accordion configs that have a real form body.
 */

import { isAccordionWizard } from "./formWizardUtils";

/**
 * @param {Array|null|undefined} config
 * @returns {boolean}
 */
export const hasFormBody = (config) =>
  Array.isArray(config) &&
  config.some((entry) =>
    entry?.body?.some((step) => Array.isArray(step?.form) && step.form.length > 0)
  );

/**
 * @param {Array|null|undefined} config
 * @returns {boolean}
 */
export const hasFieldArrayBody = (config) =>
  hasFormBody(config) &&
  config.some((entry) =>
    entry?.body?.some((step) =>
      step?.form?.some((f) => f?.type === "fieldArray" && f?.field?.name)
    )
  );

/**
 * Resolve which master array to feed useFormWizard / FormFlowRoutes.
 *
 * Typical options:
 * - local + mdms + masterKey: read `local[masterKey]` / treat mdms as already selected
 * - preferAccordion: use local/mdms when isAccordionWizard && hasFormBody
 * - preferFieldArray: when choosing non-accordion, prefer mdms if it has fieldArray
 *
 * @param {object} options
 * @param {object|Array|null} [options.local] - Full local JSON or already-extracted array
 * @param {Array|null} [options.mdms] - MDMS master array (already selected)
 * @param {string} [options.masterKey] - Key inside local JSON (e.g. AccordionNewApplication)
 * @param {boolean} [options.preferAccordion=true]
 * @param {boolean} [options.preferFieldArray=false]
 * @param {Array|null} [options.localFallback] - Optional non-accordion local array (e.g. NewApplication)
 * @returns {Array|null}
 */
export const resolveFormConfig = ({
  local = null,
  mdms = null,
  masterKey,
  preferAccordion = true,
  preferFieldArray = false,
  localFallback = null,
} = {}) => {
  const localMaster =
    Array.isArray(local)
      ? local
      : masterKey && local && typeof local === "object"
        ? local[masterKey]
        : null;

  if (preferAccordion) {
    if (isAccordionWizard(localMaster) && hasFormBody(localMaster)) {
      return localMaster;
    }
    if (isAccordionWizard(mdms) && hasFormBody(mdms)) {
      return mdms;
    }
  }

  const fallback =
    Array.isArray(localFallback)
      ? localFallback
      : masterKey && localFallback && typeof localFallback === "object"
        ? localFallback[masterKey]
        : localFallback;

  if (preferFieldArray && hasFieldArrayBody(mdms)) {
    return mdms;
  }

  if (hasFormBody(fallback)) return fallback;
  if (hasFormBody(localMaster)) return localMaster;
  if (hasFormBody(mdms)) return mdms;

  return fallback || localMaster || mdms || null;
};

export default resolveFormConfig;
