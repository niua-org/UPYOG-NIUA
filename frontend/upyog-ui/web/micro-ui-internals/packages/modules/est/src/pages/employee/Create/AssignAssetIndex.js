import React, { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useQueryClient } from "react-query";
import { Redirect, Route, Switch, useHistory, useLocation, useRouteMatch } from "react-router-dom";
import { Config } from "../../../config/Create/AssignAssetConfig";

const ESTAssignAssetCreate = ({ parentRoute }) => {
  const location = useLocation();
  const assetData = location.state?.assetData || {};
  const queryClient = useQueryClient();
  const match = useRouteMatch();
  const { t } = useTranslation();
  const { pathname } = useLocation();
  const history = useHistory();
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
    
    let redirectWithHistory = history.push;
    if (skipStep) {
      redirectWithHistory = history.replace;
    }
    if (nextStep === null) {
      return redirectWithHistory(`${match.path}/check`);
    }
    
    let nextPage = `${match.path}/${nextStep}`;
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
  history.replace(`${match.path}/acknowledgement`);
};
  
  let commonFields = Config;
  commonFields.forEach((obj) => {
    config = config.concat(obj.body.filter((a) => !a.hideInCitizen));
  });
  
  config.indexRoute = "info";

  const ESTAssignAssetsCheckPage = Digit?.ComponentRegistryService?.getComponent("ESTAssignAssetsCheckPage");
  const ESTAllotmentAcknowledgement = Digit?.ComponentRegistryService?.getComponent("ESTAllotmentAcknowledgement");
  
  return (
    <Switch>
      {config.map((routeObj, index) => {
        const { component, texts, inputs, key } = routeObj;
        const Component = typeof component === "string" ? Digit.ComponentRegistryService.getComponent(component) : component;
        const user = Digit.UserService.getUser().info.type;
        return (
          <Route path={`${match.path}/${routeObj.route}`} key={index}>
           <Component config={{ texts, inputs, key }} onSelect={handleSelect} onSkip={handleSkip} t={t} formData={params} onAdd={handleMultiple} userType={user} />
          </Route>
        );
      })}

      {/* {console.log("params in create1111", params)} */}
      <Route path={`${match.path}/check`}>
       <ESTAssignAssetsCheckPage onSubmit={estcreate} value={params} />
      </Route>
      <Route path={`${match.path}/acknowledgement`}>
       <ESTAllotmentAcknowledgement data={params} onSuccess={onSuccess}/>
      </Route>
      <Route>
        <Redirect to={`${match.path}/${config.indexRoute}`} />  
      </Route>
      
    </Switch>
  );
};

export default ESTAssignAssetCreate;
