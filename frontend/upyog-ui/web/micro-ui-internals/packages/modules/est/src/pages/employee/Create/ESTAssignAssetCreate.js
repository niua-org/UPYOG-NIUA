import React, { Children, Fragment } from "react";
import { useTranslation } from "react-i18next";
import { useQueryClient } from "@tanstack/react-query";
import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { Config } from "../../../config/Create/AssignAssetConfig";

/**
 * ESTAssignAssetCreate
 * ---------------------------------------------------------
 * This component controls the complete Assign Asset flow:
 * - Multi-step form navigation
 * - Session storage handling
 * - Route-based rendering
 * - Final acknowledgement handling
 */
const ESTAssignAssetCreate = ({ parentRoute }) => {
  // React Query client for cache invalidation
  const queryClient = useQueryClient();
  const match = Digit.Hooks.useModuleBasePath();
  const { t } = useTranslation();
  const { pathname } = useLocation();
  
  // Navigation handler
  const navigate = Digit.Hooks.useCustomNavigate();
    // State ID (currently unused but available)
  const stateId = Digit.ULBService.getStateId();
  
  // Dynamic route configuration holder
  let config = [];
  /**
   * Session storage to persist form data across steps
   * Key: EST_ASSIGN_ASSETS
   */
  const [params, setParams, clearParams] = Digit.Hooks.useSessionStorage("EST_ASSIGN_ASSETS", {});

  /**
   * goNext
   * -----------------------------------------------------
   * Handles navigation logic between form steps
   * Supports:
   * - Normal flow
   * - Skip flow
   * - Multiple-entry steps (owners, units, etc.)
   */

  const goNext = (skipStep, index, isAddMultiple, key) => {
    let currentPath = pathname.split("/").pop(),
      lastchar = currentPath.charAt(currentPath.length - 1),
      isMultiple = false,
      nextPage;
        // Detect numeric routes for multiple entries
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
      // Check if route ends with a number
    if (!isNaN(lastchar)) {
      isMultiple = true;
    }
      // Get next step from config
    let { nextStep = {} } = config.find((routeObj) => routeObj.route === currentPath);

      // Decide navigation method
    let redirectWithHistory = navigate;
    if (skipStep) {
      redirectWithHistory = (to, state) => navigate(to, state != null ? { replace: true, state } : { replace: true });
    }
    // Override next step for multiple add flow
    if (isAddMultiple) {
      nextStep = key;
    }

    // Redirect to check page if flow ends
    if (nextStep === null) {
      return redirectWithHistory(`check`);
    }
        // Build next page URL
    if (!isNaN(nextStep.split("/").pop())) {
      nextPage = `${nextStep}`;
    } else {
      nextPage = isMultiple && nextStep !== "map" ? `${nextStep}/${index}` : `${nextStep}`;
    }

    redirectWithHistory(nextPage);
  };

   /**
   * Clear stored form data if user revisits
   * the first step without back navigation
   */
  if(params && Object.keys(params).length>0 && window.location.href.includes("/info") && sessionStorage.getItem("docReqScreenByBack") !== "true")
    {
    clearParams();
    queryClient.invalidateQueries("EST_ASSIGN_ASSETS");
  }

  /**
   * Triggered after final check page submission
   */
  const estcreate = async () => {
    navigate(`acknowledgement`);
  };

    /**
   * handleSelect
   * -----------------------------------------------------
   * Stores step data into session storage
   * Handles special cases:
   * - owners (array-based)
   * - units (list replacement)
   * - general form sections
   */

  function handleSelect(key, data, skipStep, index, isAddMultiple = false) {
    if (key === "owners") {
      let owners = params.owners || [];
      owners[index] = data;
      setParams({ ...params, ...{ [key]: [...owners] } });
    } else if (key === "units") {
      let units = params.units || [];
      // if(index){units[index] = data;}else{
      units = data;
      setParams({ ...params, units });
    } else {
      setParams({ ...params, ...{ [key]: { ...params[key], ...data } } });
    }
    goNext(skipStep, index, isAddMultiple, key);
  }

  const handleSkip = () => {};
  const handleMultiple = () => {};

  const onSuccess = () => {
    clearParams();
    queryClient.invalidateQueries("EST_ASSIGN_ASSETS");
  };

    /**
   * Build route configuration from AssignAssetConfig
   * Filters out citizen-hidden screens
   */
  let commonFields = Config;
  commonFields.forEach((obj) => {
    config = config.concat(obj.body.filter((a) => !a.hideInCitizen));
  });

    // Default route
  config.indexRoute = "info";

  // Load dynamic components
  const ESTAssignAssetsCheckPage = Digit?.ComponentRegistryService?.getComponent("ESTAssignAssetsCheckPage");
  const ESTAllotmentAcknowledgement = Digit?.ComponentRegistryService?.getComponent("ESTAllotmentAcknowledgement");


    /**
   * Route Rendering
   * -----------------------------------------------------
   * Dynamically renders each step using configuration
   */
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
            element={<Component config={{ texts, inputs, key }} onSelect={handleSelect} onSkip={handleSkip} t={t} formData={params} onAdd={handleMultiple} userType={user} />}
          />
        );
      })}
      <Route path="check/*" element={<ESTAssignAssetsCheckPage onSubmit={estcreate} value={params} />} />
      <Route path="acknowledgement/*" element={<ESTAllotmentAcknowledgement data={params} onSuccess={onSuccess} />} />
      <Route path="/*" element={<Navigate to={config.indexRoute} replace />} />
    </Routes>
  );
};

export default ESTAssignAssetCreate;