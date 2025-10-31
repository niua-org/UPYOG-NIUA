import { AppContainer, BackButton, PrivateRoute } from "@upyog/digit-ui-react-components";
import React from "react";
import { Route, Switch, useRouteMatch } from "react-router-dom";
import { useTranslation } from "react-i18next";


const App = () => {
  const { path, url, ...match } = useRouteMatch();
  const { t } = useTranslation();
  

  const MapView = Digit?.ComponentRegistryService?.getComponent("MapView");
  const ServiceType = Digit?.ComponentRegistryService?.getComponent("ServiceTypes");
 
  return (
    <span className={"chb-citizen"}style={{width:"100%"}}>
      <Switch>
        <AppContainer>         
          <PrivateRoute path={`${path}/map`} component={ServiceType}></PrivateRoute>
          <PrivateRoute path={`${path}/mapview`} component={MapView}></PrivateRoute>
        </AppContainer>
      </Switch>
    </span>
  );
};

export default App;