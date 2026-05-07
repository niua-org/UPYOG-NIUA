import React, { Fragment } from "react";
import { useTranslation } from "react-i18next";
import { BackButton, Loader, PrivateRoute, BreadCrumb } from "@upyog/workbench-ui-react-components";
import DashBoard from "./pages";
import Home from "./pages/Home";
import { Route, Routes, useResolvedPath, useLocation } from "react-router-dom";
import Overview from "./pages/Overview";
import {checkCurrentScreen, DSSCard,NDSSCard} from "./components/DSSCard";
import DrillDown from "./pages/DrillDown";
import FAQsSection from "./pages/FAQs/FAQs"
import About from "./pages/About";
const DssBreadCrumb = ({ location }) => {
  const { t } = useTranslation();
  const {fromModule=false,title}= Digit.Hooks.useQueryParams();
  const moduleName=Digit.Utils.dss.getCurrentModuleName();
  const landingPageHiddenIn=["works-ui","sanitation-ui"];
  const crumbs = [
    {
      path: `/workbench-ui/employee`,
      content: t("ES_COMMON_HOME"),
      show: true,
    },
    {
      path: checkCurrentScreen() || window.location.href.includes("NURT_DASHBOARD") ? `/workbench-ui/employee/dss/landing/NURT_DASHBOARD` : `/workbench-ui/employee/dss/landing/home`,
      content: t("ES_LANDING_PAGE"),
      show: landingPageHiddenIn?.includes(window?.contextPath)?false:true,
    },
    {
      path: fromModule?`/workbench-ui/employee/dss/dashboard/${fromModule}`:`/workbench-ui/employee/dss/dashboard/${Digit.Utils.dss.getCurrentModuleName()}`,
      content: t(`ES_COMMON_DSS_${Digit.Utils.locale.getTransformedLocale(fromModule?fromModule:moduleName)}`),
      show: location.pathname.includes("dashboard") ? true : false,
    },
    {
      path: `/workbench-ui/employee/dss/drilldown`,
      content:location.pathname.includes("drilldown")?t(title): t("ES_COMMON_DSS_DRILL"),
      show: location.pathname.includes("drilldown") ? true : false,
    },
    {
      path: `/workbench-ui/employee/dss/national-faqs`,
      content: t("ES_COMMON_DSS_FAQS"),
      show: location.pathname.includes("national-faqs") ? true : false,
    } ,
    {
      path: `/workbench-ui/employee/dss/national-about`,
      content: t("ES_COMMON_DSS_ABOUT"),
      show: location.pathname.includes("national-about") ? true : false,
    } 
  ];

  return <BreadCrumb crumbs={crumbs?.filter(ele=>ele.show)} />;
};
// Renamed Routes → DSSRoutes (For conflict avoid with react-router-dom v6 Routes)

const DSSRoutes = ({ path, stateCode }) => {
  const location = useLocation();
  const isMobile = window.Digit.Utils.browser.isMobile();
  return (
    <div className="chart-wrapper" style={isMobile ? {marginTop:"unset"} : {}}>
      <DssBreadCrumb location={location} />
      //Switch → Routes
      <Routes>                                         
        // rivateRoute element prop + relative paths 
        <Route path="landing/:moduleCode" element={<PrivateRoute element={<Home stateCode={stateCode} />} />} />
        <Route path="dashboard/:moduleCode" element={<PrivateRoute element={<DashBoard stateCode={stateCode} />} />} />
        <Route path="drilldown" element={<PrivateRoute element={<DrillDown stateCode={stateCode} />} />} />
        // children → element prop 
        <Route path="national-faqs" element={<FAQsSection />} />
        <Route path="national-about" element={<About />} />
      </Routes>
    </div>
  );
};

const DSSModule = ({ stateCode, userType, tenants }) => {
  const { pathname: path } = useResolvedPath(".");        //useRouteMatch → useResolvedPath
  const language = Digit.StoreData.getCurrentLanguage();
  const tenantId = Digit.ULBService.getCurrentTenantId();
  const moduleCode = ["DSS","common-masters",tenantId];
  const { isLoading, data: store } = Digit.Services.useStore({
      stateCode,
      moduleCode,
      language,
  });

  if (isLoading) {
    return <Loader />;
  }

  Digit.SessionStorage.set("DSS_TENANTS", tenants);

  if (userType !== "citizen") {
    return <DSSRoutes stateCode={stateCode} />;      // path prop removed from DSSRoutes as useResolvedPath is used inside it to get the path
  }
};

const componentsToRegister = {
  DSSModule,
  DSSCard,
  NDSSCard
};

export const initDSSComponents = () => {
  Object.entries(componentsToRegister).forEach(([key, value]) => {
    Digit.ComponentRegistryService.setComponent(key, value);
  });
};
