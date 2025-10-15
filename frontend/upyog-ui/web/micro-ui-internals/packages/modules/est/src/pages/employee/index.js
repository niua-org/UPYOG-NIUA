import { PrivateRoute, BreadCrumb } from "@upyog/digit-ui-react-components";
import React from "react";
import { useTranslation } from "react-i18next";
import { Switch, useLocation } from "react-router-dom";
import SearchApp from "./SearchApp";
import ManagePropertiesPage from "./ManagePropertiesPage";
import AllPropertiesPage from "./AllPropertiesPage";


const EmployeeApp = ({ path }) => {
  const { t } = useTranslation();
  const location = useLocation();
  const isMobile = window.Digit.Utils.browser.isMobile();

  const ESTBreadCrumbs = ({ location }) => {
    const { t } = useTranslation();
    
    const crumbs = [
      {
        path: "/upyog-ui/employee",
        content: t("ES_COMMON_HOME"),
        show: true,
      },
      {
        path: "/upyog-ui/employee/est/my-applications",
        content: t("ES_COMMON_APPLICATION_SEARCH"),
        show: location.pathname.includes("/est/my-applications"),
      },
    ];

    return (
      <BreadCrumb
        style={isMobile ? { display: "flex" } : {}}
        spanStyle={{ maxWidth: "min-content" }}
        crumbs={crumbs}
      />
    );
  };

  return (
    <Switch>
        <PrivateRoute 
  path={`${path}/manage-properties`} 
  component={() => <ManagePropertiesPage />} 
/>
 <PrivateRoute 
        path={`${path}/all-properties`} 
        component={() => <AllPropertiesPage />} 
        />

      <React.Fragment>
        <div className="ground-container">
          <div style={{ marginLeft: "12px" }}><ESTBreadCrumbs location={location} /></div>

          <PrivateRoute 
            path={`${path}/my-applications`} 
            component={(props) => <SearchApp {...props} parentRoute={path} />} 
          />
        </div>
      </React.Fragment>
    </Switch>
  );
};

export default EmployeeApp;
