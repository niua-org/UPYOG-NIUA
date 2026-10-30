/**
 * ConfigDrivenFormStep.js
 *
 * Layout switch for workbench form configs:
 * - navigation.pattern === "accordion" (or accordionConfig) → DynamicFormAccordionStep
 * - otherwise → DynamicFormStep (oneStep / wizard)
 *
 * Modules pass localOverrides, search handlers, and labels — not layout choice.
 *
 * @see DynamicFormStep
 * @see DynamicFormAccordionStep
 */

import React from "react";
import DynamicFormAccordionStep from "./DynamicFormAccordionStep";
import DynamicFormStep from "./DynamicFormStep";

/**
 * @param {object} props — same surface as DynamicFormStep / DynamicFormAccordionStep
 * @returns {JSX.Element}
 */
const ConfigDrivenFormStep = (props) => {
  const { config, wrapperClassName, accordionWrapperClassName, ...rest } = props;

  const accordionConfig =
    config?.accordionConfig ||
    (config?.navigation?.pattern === "accordion" && Array.isArray(config?.body)
      ? config
      : null);

  if (accordionConfig) {
    return (
      <DynamicFormAccordionStep
        {...rest}
        config={accordionConfig}
        wrapperClassName={
          accordionWrapperClassName ||
          wrapperClassName ||
          "employeeCard dynamic-form-step--accordion"
        }
      />
    );
  }

  return (
    <DynamicFormStep
      {...rest}
      config={config}
      wrapperClassName={wrapperClassName || "employeeCard"}
    />
  );
};

export default ConfigDrivenFormStep;
