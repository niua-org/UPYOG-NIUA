import { BreadCrumb, AppContainer, BackButton, PrivateRoute } from "@upyog/digit-ui-react-components";
import React from "react";
import { Route, Switch, useRouteMatch, useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";

const NDCBreadCrumbs = ({ location }) => {
  const { t } = useTranslation();
  const crumbs = [
    {
      path: "/upyog-ui/citizen",
      content: t("ES_COMMON_HOME"),
      show: true,
    },
    {
      path: "/upyog-ui/citizen/ndc-home",
      content: `${t("NDC")} Home`,
      show: location.pathname.includes("ndc/") ? true : false,
    },

    {
      path: "/upyog-ui/citizen/ptr-home",
      content: t("PET_NDCSERVICE"),
      show: location.pathname.includes("ptr/petservice/test") ? true : false,
    },
  ];
  return <BreadCrumb crumbs={crumbs} />;
};

const App = () => {
  const { path, url, ...match } = useRouteMatch();
  const { t } = useTranslation();
  const location = useLocation();
  const NewNDCStepForm = Digit.ComponentRegistryService.getComponent("NewNDCStepFormCitizen");
  const MyApplications = Digit.ComponentRegistryService.getComponent("MyApplications");
  const NDCResponseCitizen = Digit.ComponentRegistryService.getComponent("NDCResponseCitizen");
  const ApplicationOverview = Digit?.ComponentRegistryService?.getComponent("CitizenApplicationOverview");
  const isResponse = window.location.href.includes("/response");
  const isMobile = window.Digit.Utils.browser.isMobile();
  return (
    <span className={"chb-citizen"} style={{ width: "100%", paddingRight: "25px", paddingLeft: "25px" }}>
      <Switch>
        <AppContainer>
          {!isResponse ? (
            <div style={window.location.href.includes("application-overview") || isMobile ? { marginLeft: "10px" } : {}}>
              <NDCBreadCrumbs location={location} />
            </div>
          ) : null}
          <PrivateRoute path={`${path}/new-application`} component={NewNDCStepForm} />
          <PrivateRoute path={`${path}/my-application`} component={MyApplications} />
          <PrivateRoute path={`${path}/response/:id`} component={NDCResponseCitizen} />
          <PrivateRoute path={`${path}/search/application-overview/:id`} component={ApplicationOverview} />
        </AppContainer>
      </Switch>
    </span>
  );
};

export default App;
