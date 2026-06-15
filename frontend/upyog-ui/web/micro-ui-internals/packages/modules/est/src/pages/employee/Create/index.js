import React, { Children, Fragment } from "react";
import { useTranslation } from "react-i18next";
import { useQueryClient } from "@tanstack/react-query";
import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { Config } from "../../../config/Create/config";

/**
 * ESTRegCreate
 * -------------
 * Main container component responsible for:
 * - Rendering multi-step EST registration form
 * - Handling navigation between steps
 * - Managing form data in session storage
 * - Rendering check & acknowledgement screens
 */
const ESTRegCreate = ({ parentRoute }) => {
  const queryClient = useQueryClient();
  const match = Digit.Hooks.useModuleBasePath();
  const { t } = useTranslation();
  const { pathname } = useLocation();
  const navigate = Digit.Hooks.useCustomNavigate();
  const stateId = Digit.ULBService.getStateId();
  let config = [];
  const [params, setParams, clearParams] = Digit.Hooks.useSessionStorage("EST_NEW_REGISTRATION_CREATES", {});

  /**
   * goNext
   * -------
   * Handles navigation logic between form steps.
   */
  const goNext = (skipStep, index, isAddMultiple, key) => {
    let currentPath = pathname.split("/").pop(),
      lastchar = currentPath.charAt(currentPath.length - 1),
      isMultiple = false,
      nextPage;

    if (Number(parseInt(currentPath)) || currentPath == "0" || currentPath == "-1") {
      if (currentPath == "-1" || currentPath == "-2") {
        currentPath = pathname.slice(0, -3);
        currentPath = currentPath.split("/").pop();
        isMultiple = true;
      } else {
        currentPath = pathname.slice(0, -2);
        currentPath = currentPath.split("/").pop();
        isMultiple = true;
      }
    } else {
      isMultiple = false;
    }

    if (!isNaN(lastchar)) {
      isMultiple = true;
    }

    let { nextStep = {} } = config.find((routeObj) => routeObj.route === currentPath);

    let redirectWithHistory = (to, state) => navigate(to, state != null ? { state } : undefined);
    if (skipStep) {
      redirectWithHistory = (to, state) => navigate(to, state != null ? { replace: true, state } : { replace: true });
    }

    if (isAddMultiple) {
      nextStep = key;
    }

    if (nextStep === null) {
      return redirectWithHistory(`check`);
    }

    if (!isNaN(nextStep.split("/").pop())) {
      nextPage = `${nextStep}`;
    } else {
      nextPage = isMultiple && nextStep !== "map" ? `${nextStep}/${index}` : `${nextStep}`;
    }

    redirectWithHistory(nextPage);
  };

  /**
   * Clear old form data when user enters the first screen again,
   * unless user navigated using browser back button
   */
  if (params && Object.keys(params).length > 0 && window.location.href.includes("/info") && sessionStorage.getItem("docReqScreenByBack") !== "true") {
    clearParams();
    queryClient.invalidateQueries("EST_NEW_REGISTRATION_CREATES");
  }

  const estcreate = async () => {
    navigate(`acknowledgement`);
  };

  /**
   * handleSelect
   * ------------
   * Saves form data into session storage
   */
  function handleSelect(key, data, skipStep, index, isAddMultiple = false) {
    setParams({ ...params, [key]: data });
    goNext(skipStep, index, isAddMultiple, key);
  }

  const handleSkip = () => {};
  const handleMultiple = () => {};

  /**
   * onSuccess
   * ----------
   * Called after successful submission
   */
  const onSuccess = () => {
    clearParams();
    queryClient.invalidateQueries("EST_NEW_REGISTRATION_CREATES");
  };

  let commonFields = Config;
  commonFields.forEach((obj) => {
    config = config.concat(obj.body.filter((a) => !a.hideInCitizen));
  });

  config.indexRoute = "newRegistration";

  const ESTRegCheckPage = Digit?.ComponentRegistryService?.getComponent("ESTRegCheckPage");
  const ESTAcknowledgement = Digit?.ComponentRegistryService?.getComponent("ESTAcknowledgement");

  return (
    <Routes>
      {config.map((routeObj, index) => {
        const { component, texts, inputs, key } = routeObj;
        const Component = typeof component === "string" ? Digit.ComponentRegistryService.getComponent(component) : component;
        const user = Digit.UserService.getUser().info.type;
        return (
          <Route
            path={`${routeObj.route}/*`}
            key={index}
            element={<Component config={routeObj} onSelect={handleSelect} onSkip={handleSkip} t={t} formData={params} onAdd={handleMultiple} userType={user} parentRoute={match.pathnameBase} />}
          />
        );
      })}
      <Route path="check/*" element={<ESTRegCheckPage onSubmit={estcreate} value={params} />} />
      <Route path="acknowledgement/*" element={<ESTAcknowledgement data={params} onSuccess={onSuccess} />} />
      <Route path="/*" element={<Navigate to={config.indexRoute} replace />} />
    </Routes>
  );
};

export default ESTRegCreate;