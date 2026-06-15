import React, { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useQueryClient } from "@tanstack/react-query";
import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { Config } from "../../../config/Create/AssignAssetConfig";

const ESTAssignAssetCreate = ({ parentRoute }) => {
  const location = useLocation();
  const assetData = location.state?.assetData || {};
  const queryClient = useQueryClient();
  const match = Digit.Hooks.useModuleBasePath();
  const { t } = useTranslation();
  const { pathname } = useLocation();
  const navigate = Digit.Hooks.useCustomNavigate();
  const stateId = Digit.ULBService.getStateId();
  let config = [];
  const [params, setParams, clearParams] = Digit.Hooks.useSessionStorage("EST_ASSIGN_ASSETS", {});

  useEffect(() => {
    if (assetData && Object.keys(params).length === 0) {
      setParams({ assetData });
    }
  }, [assetData]);

  const goNext = (skipStep, index, isAddMultiple, key) => {
    console.log("goNext called with:", { skipStep, index, isAddMultiple, key });
    let currentPath = pathname.split("/").pop();
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

    let nextPage = `${nextStep}`;
    redirectWithHistory(nextPage);
  };

  function handleSelect(key, data, skipStep, index, isAddMultiple = false) {
    console.log("handleSelect called with:", { key, data, skipStep, index, isAddMultiple });
    if (key === "Documents") {
      setParams({ ...params, assetData, [key]: data });
    } else {
      setParams({ ...params, [key]: data });
    }
    goNext(skipStep, index, isAddMultiple, key);
  }

  const handleSkip = () => {};
  const handleMultiple = () => {};

  const onSuccess = () => {
    clearParams();
    queryClient.invalidateQueries("EST_ASSIGN_ASSETS");
  };

  const estcreate = async () => {
    console.log("Final params before acknowlgement:", params);
    navigate(`acknowledgement`);
  };

  let commonFields = Config;
  commonFields.forEach((obj) => {
    config = config.concat(obj.body.filter((a) => !a.hideInCitizen));
  });

  config.indexRoute = "info";

  const ESTAssignAssetsCheckPage = Digit?.ComponentRegistryService?.getComponent("ESTAssignAssetsCheckPage");
  const ESTAllotmentAcknowledgement = Digit?.ComponentRegistryService?.getComponent("ESTAllotmentAcknowledgement");

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