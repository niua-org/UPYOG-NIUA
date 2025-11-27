import { PrivateRoute, BreadCrumb, AppContainer, BackButton } from "@upyog/digit-ui-react-components";
import React from "react";
import { useTranslation } from "react-i18next";
import { Switch, useLocation } from "react-router-dom";
import { ESTLinks } from "../../Module";
import SearchApp from "./SearchApp";
import ManageProperties from "../../components/ManageProperties";
import ESTPropertyAllotteeDetails from "../../PageComponents/ESTPropertyAllotteeDetails"; 
import ESTRegCreate from "./Create";
import ESTAssignAssetCreate from "./Create/AssignAssetIndex";
import AllProperties  from "../../components/AllProperties";
import ESTInbox from "./Inbox";
import ESTManageProperties from "../../PageComponents/ESTManageProperties";

const EmployeeApp = ({ path, url, userType }) => {
  const { t } = useTranslation();
  const location = useLocation();
  const isMobile = window.Digit.Utils.browser.isMobile();

  const inboxInitialState = {
    pageOffset: 0,
    pageSize: 10,
    sortParams: [{ id: "createdTime", desc: true }],
    searchParams: {}
  };

  const ESTBreadCrumbs = ({ location }) => {
    const { t } = useTranslation();
    
    const crumbs = [
      {
        path: "/upyog-ui/employee",
        content: t("ES_COMMON_HOME"),
        show: true,
      },
      {
        path: "/upyog-ui/employee/est/inbox",
        content: t("INBOX"),
        show: location.pathname.includes("est/inbox"),
      },
      {
        path: "/upyog-ui/employee/est/search-applications",
        content: t("ES_COMMON_APPLICATION_SEARCH"),
        show: location.pathname.includes("est/search-applications"),
      },
      {
        path: "/upyog-ui/employee/est/create-asset",
        content: t("EST_CREATE_ASSET"),
        show: location.pathname.includes("est/create-asset"),
      },
      {
        path: "/upyog-ui/employee/est/manage-properties-table",
        content: t("EST_MANAGE_PROPERTY"),
        show: location.pathname.includes("est/manage-properties-table"),
      },
    ];

    return (
      <BreadCrumb
        style={isMobile ? { display: "flex" } : { margin: "0 0 4px", color: "#000000" }}
        spanStyle={{ maxWidth: "min-content" }}
        crumbs={crumbs}
      />
    );
  };

  return (
    <Switch>
      <AppContainer>
        <React.Fragment>
          <div className="ground-container">
            <div style={{ marginLeft: "-4px", display: "flex", alignItems: "center" }}>
              <ESTBreadCrumbs location={location} />
            </div>

            <PrivateRoute exact path={`${path}/`} component={() => <ESTLinks matchPath={path} userType={userType} />} />
            <PrivateRoute path={`${path}/property-allottee-details`} component={(props) => <ESTPropertyAllotteeDetails {...props} t={t} parentRoute={path} />} />
            <PrivateRoute path={`${path}/assignassets`} component={(props) => <ESTAssignAssetCreate {...props} parentRoute={path} />} />
            <PrivateRoute path={`${path}/inbox`} component={() => (
              <ESTInbox
                parentRoute={path}
                businessService="EST"
                initialStates={inboxInitialState}
                isInbox={true}
                filterComponent="EST_INBOX_FILTER"
                useNewInboxAPI={true}
              />
            )} />
            <PrivateRoute path={`${path}/search-applications`} component={(props) => <SearchApp {...props} parentRoute={path} />} />
            <PrivateRoute path={`${path}/create-asset`} component={(props) => <ESTRegCreate {...props} parentRoute={path} />} />
            <PrivateRoute path={`${path}/manage-properties`} component={(props) => <ESTManageProperties {...props} parentRoute={path} />} />
            <PrivateRoute path={`${path}/all-properties`} component={(props) => <AllProperties {...props}  t={t}  parentRoute={path} />} />
            <PrivateRoute path={`${path}/manage-properties-table`} component={(props) => <ManageProperties {...props} t={t} parentRoute={path} />} />
          </div>
        </React.Fragment>
      </AppContainer>
    </Switch>
  );
};

export default EmployeeApp;
