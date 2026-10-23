/**
 * FormFlowRoutes.js
 *
 * Maps useFormWizard `config` steps to React Router routes, plus optional
 * check / acknowledgement slots. Passes `accordionConfig || routeObj` into
 * each step component so accordion layouts receive the full master entry.
 *
 * @see useFormWizard
 * @see ConfigDrivenFormStep
 */

import React from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { Loader } from "../atoms/Loader";

/**
 * @param {object} props
 * @param {Array} props.config - From useFormWizard
 * @param {boolean} [props.isReady=true]
 * @param {Function} props.resolveComponent - (componentName, routeObj) => Component | null
 * @param {Function} props.onSelect
 * @param {object} props.formData - Wizard session params
 * @param {object} [props.persistedData]
 * @param {Function} [props.t]
 * @param {string} [props.userType]
 * @param {string} [props.parentRoute]
 * @param {object} [props.checkRoute] - { path, element }
 * @param {object} [props.ackRoute] - { path, element }
 * @param {string} [props.indexRoute]
 * @param {boolean} [props.passAccordionConfig=true]
 * @param {Function} [props.getStepProps] - (routeObj) => extra props for the step
 * @param {Function} [props.onStepSelect] - Optional override: (routeObj, ...args) => void
 * @returns {JSX.Element}
 */
const FormFlowRoutes = ({
  config,
  isReady = true,
  resolveComponent,
  onSelect,
  formData,
  persistedData,
  t,
  userType,
  parentRoute,
  checkRoute,
  ackRoute,
  indexRoute,
  passAccordionConfig = true,
  getStepProps,
  onStepSelect,
}) => {
  if (!isReady) {
    return <Loader />;
  }

  const defaultIndex =
    config?.indexRoute || indexRoute || config?.[0]?.route || "info";

  return (
    <Routes>
      {(config || []).map((routeObj, index) => {
        const Component =
          typeof resolveComponent === "function"
            ? resolveComponent(routeObj.component, routeObj)
            : null;

        if (!Component) {
          console.error(
            `FormFlowRoutes: component "${routeObj.component}" is not registered`
          );
          return null;
        }

        const handleStepSelect = (key, data, skipStep, idx, isAddMultiple) => {
          if (typeof onStepSelect === "function") {
            onStepSelect(routeObj, key, data, skipStep, idx, isAddMultiple);
            return;
          }
          onSelect?.(key, data, skipStep, idx, isAddMultiple);
        };

        const stepConfig = passAccordionConfig
          ? routeObj.accordionConfig || routeObj
          : routeObj;

        const extraProps =
          typeof getStepProps === "function" ? getStepProps(routeObj) : {};

        return (
          <Route
            path={`${routeObj.route}/*`}
            key={routeObj.key || routeObj.route || index}
            element={
              <Component
                config={stepConfig}
                onSelect={handleStepSelect}
                t={t}
                persistedData={persistedData ?? formData}
                formData={formData}
                userType={userType}
                parentRoute={parentRoute}
                {...extraProps}
              />
            }
          />
        );
      })}

      {checkRoute?.element ? (
        <Route
          path={`${checkRoute.path || "check"}/*`}
          element={checkRoute.element}
        />
      ) : null}

      {ackRoute?.element ? (
        <Route
          path={`${ackRoute.path || "acknowledgement"}/*`}
          element={ackRoute.element}
        />
      ) : null}

      <Route
        path="/*"
        element={<Navigate to={defaultIndex} replace />}
      />
    </Routes>
  );
};

export default FormFlowRoutes;
