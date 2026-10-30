/**
 * formWizardUtils.js
 *
 * Shared helpers for workbench / MDMS form wizards across modules.
 * Layout is driven by `navigation.pattern` (accordion | oneStep | wizard),
 * not by the master array name.
 *
 * @see resolveFormConfig
 * @see useFormWizard
 * @see ConfigDrivenFormStep
 */

/** Default steps skipped because the flow uses a dedicated /check route. */
const DEFAULT_SKIP_COMPONENTS = new Set(["ReviewDetails"]);

/**
 * @param {object} entry - Single master entry (`head`, `navigation`, `body[]`)
 * @returns {"accordion"|"oneStep"|"wizard"}
 */
export const getNavigationPattern = (entry) => {
  const pattern = entry?.navigation?.pattern;
  if (pattern === "accordion" || pattern === "oneStep" || pattern === "wizard") {
    return pattern;
  }
  return "wizard";
};

/**
 * @param {Array|null|undefined} initialConfig - Master array from MDMS / local JSON
 * @returns {boolean}
 */
export const isAccordionWizard = (initialConfig) =>
  Array.isArray(initialConfig) &&
  initialConfig.some((entry) => getNavigationPattern(entry) === "accordion");

/**
 * Normalize the second argument of buildWizardSteps to an options object.
 * Supports legacy `(config, indexRoute)` call sites.
 *
 * @param {string|object|null|undefined} indexRouteOrOptions
 * @returns {object}
 */
const normalizeBuildOptions = (indexRouteOrOptions) => {
  if (
    typeof indexRouteOrOptions === "string" ||
    indexRouteOrOptions == null
  ) {
    return { indexRoute: indexRouteOrOptions || undefined };
  }
  return indexRouteOrOptions;
};

/**
 * @param {object} step
 * @param {"citizen"|"employee"} userType
 * @returns {boolean}
 */
const isHiddenForUserType = (step, userType) => {
  if (userType === "citizen" && step?.hideInCitizen) return true;
  if (userType === "employee" && step?.hideInEmployee) return true;
  return false;
};

/**
 * Flatten MDMS / local wizard entries into routable steps.
 *
 * When `navigation.pattern === "accordion"`, collapses form body items into a
 * single route (optional docs landing + form page) so ConfigDrivenFormStep /
 * DynamicFormAccordionStep can render every section on one page.
 *
 * @param {Array} initialConfig
 * @param {string|object} [indexRouteOrOptions]
 * @param {string} [indexRouteOrOptions.indexRoute]
 * @param {"citizen"|"employee"} [indexRouteOrOptions.userType="employee"]
 * @param {Set<string>|string[]} [indexRouteOrOptions.skipComponents]
 * @param {string} [indexRouteOrOptions.docsComponent] - Accordion docs landing component name
 * @param {string} [indexRouteOrOptions.formComponent] - Default accordion form component name
 * @param {string} [indexRouteOrOptions.sessionKey] - Fallback key for accordion session step
 * @returns {Array}
 */
export const buildWizardSteps = (initialConfig, indexRouteOrOptions) => {
  if (!initialConfig || !Array.isArray(initialConfig)) return [];

  const {
    indexRoute,
    userType = "employee",
    skipComponents: skipOption,
    docsComponent,
    formComponent,
    sessionKey = "newApplication",
  } = normalizeBuildOptions(indexRouteOrOptions);

  const skipComponents =
    skipOption instanceof Set
      ? skipOption
      : new Set(
          Array.isArray(skipOption) && skipOption.length
            ? skipOption
            : [...DEFAULT_SKIP_COMPONENTS]
        );

  const steps = initialConfig.reduce((acc, entry) => {
    if (!entry?.body) return acc;

    if (getNavigationPattern(entry) === "accordion") {
      const formSteps = entry.body.filter((step) => {
        if (isHiddenForUserType(step, userType)) return false;
        if (skipComponents.has(step.component)) return false;
        if (step.isPreview || step.type === "review") return false;
        return Array.isArray(step.form) && step.form.length > 0;
      });
      const first = formSteps[0];
      if (!first) return acc;

      const docsRoute = entry.navigation?.indexRoute || indexRoute || "info";
      const formRoute = entry.navigation?.formRoute || "apply";
      // Headings for the docs landing. Structural step fields are set after the
      // spread so infoPage.key cannot replace the wizard step key.
      const infoPage =
        entry.navigation?.infoPage || entry.infoPage || {};

      if (docsComponent) {
        acc.push({
          ...infoPage,
          key: "info",
          route: docsRoute,
          component: docsComponent,
          nextStep: formRoute,
          hideInEmployee: userType === "citizen" ? true : undefined,
          withoutLabel: true,
          type: "component",
          documents:
            infoPage.documents ||
            infoPage.Documents ||
            entry.Documents ||
            entry.documents,
          requiredDocuments:
            infoPage.requiredDocuments ||
            entry.requiredDocuments ||
            entry.RequiredDocuments,
        });
      }

      acc.push({
        ...first,
        key: entry.key || entry.sessionKey || sessionKey,
        route: formRoute,
        nextStep: null,
        component: first.component || formComponent,
        accordionConfig: entry,
      });
      return acc;
    }

    return acc.concat(
      entry.body.filter((step) => {
        if (isHiddenForUserType(step, userType)) return false;
        if (skipComponents.has(step.component)) return false;
        if (step.isPreview && (!Array.isArray(step.form) || step.form.length === 0)) {
          return false;
        }
        return true;
      })
    );
  }, []);

  steps.indexRoute =
    initialConfig.find((entry) => entry?.navigation?.indexRoute)?.navigation
      ?.indexRoute || indexRoute;
  return steps;
};

/**
 * @param {string} pathname
 * @param {string} [pathnameBase]
 * @param {string[]} terminalSegments
 * @returns {string}
 */
export const getWizardBasePath = (pathname, pathnameBase, terminalSegments) => {
  if (pathnameBase) return pathnameBase;

  const parts = pathname.split("/");
  const terminalIndex = parts.findIndex((p) => terminalSegments.includes(p));
  if (terminalIndex > 0) {
    return parts.slice(0, terminalIndex).join("/");
  }
  return parts.slice(0, -1).join("/");
};

/**
 * Shared goNext for dynamic-form wizards.
 * `nextStep === null` (or non-string) → navigate to check.
 *
 * @param {object} options
 * @param {string} options.pathname
 * @param {Array} options.config
 * @param {Function} options.navigate
 * @param {boolean} [options.multiStep=true]
 * @returns {Function}
 */
export const createWizardGoNext = ({
  pathname,
  config,
  navigate,
  multiStep = true,
}) => {
  return (skipStep, index, isAddMultiple, key) => {
    let currentPath = pathname.split("/").pop();
    let isMultiple = false;

    if (multiStep) {
      const lastchar = currentPath.charAt(currentPath.length - 1);
      if (Number(parseInt(currentPath)) || currentPath === "0" || currentPath === "-1") {
        if (currentPath === "-1" || currentPath === "-2") {
          currentPath = pathname.slice(0, -3).split("/").pop();
        } else {
          currentPath = pathname.slice(0, -2).split("/").pop();
        }
        isMultiple = true;
      } else {
        isMultiple = false;
      }
      if (!Number.isNaN(Number(lastchar))) isMultiple = true;
    }

    let { nextStep = {} } =
      config.find((routeObj) => routeObj.route === currentPath) || {};

    let redirectWithHistory = (to, state) =>
      navigate(to, state != null ? { state } : undefined);

    if (skipStep) {
      redirectWithHistory = (to, state) =>
        navigate(
          to,
          state != null ? { replace: true, state } : { replace: true }
        );
    }

    if (isAddMultiple) nextStep = key;
    if (nextStep === null) return redirectWithHistory("check");
    if (typeof nextStep !== "string") return redirectWithHistory("check");

    const nextPage =
      multiStep &&
      !Number.isNaN(Number(nextStep.split("/").pop())) &&
      nextStep !== "map"
        ? `${nextStep}`
        : isMultiple && nextStep !== "map"
          ? `${nextStep}/${index}`
          : `${nextStep}`;

    redirectWithHistory(nextPage);
  };
};
