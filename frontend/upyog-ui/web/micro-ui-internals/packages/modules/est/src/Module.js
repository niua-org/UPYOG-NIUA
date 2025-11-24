import { CitizenHomeCard, PropertyHouse } from "@upyog/digit-ui-react-components";
import React, { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useRouteMatch } from "react-router-dom";
import CitizenApp from "./pages/citizen";
import ESTCard from "./components/ESTCard";
import EmployeeApp from "./pages/employee";
import MyApplications from "./pages/citizen/MyApplications";
import { ESTPaymentHistory } from "./pages/citizen/PaymentHistory";
import ESTManageProperties from "./PageComponents/ESTManageProperties";
import ESTDashboard from "./PageComponents/ESTDashboard";
import ESTAllotPropertySummary  from "./PageComponents/ESTAllotPropertySummary";
import ESTEditAllotPropertySummary  from "./PageComponents/ESTEditAllotPropertySummary";
import NewRegistration from "./PageComponents/ESTNEWRegistration";
import ESTRequiredDoc  from "./PageComponents/ESTRequiredDoc";
import ESTRegCheckPage from "./pages/employee/Create/ESTRegCheckPage";
import ESTRegCreate from "./pages/employee/Create";
import ESTAcknowledgement from "./pages/employee/Create/ESTAcknowledgement";
import ESTAllotmentAcknowledgement from "./pages/employee/Create/ESTAllotmentAcknowledgement";
import ESTAssignAssetCreate from "./pages/employee/Create/AssignAssetIndex";
import ESTAssignAstRequiredDoc from "./PageComponents/ESTAssignAstRequiredDoc";
import ESTAssignAssets from "./PageComponents/ESTAssignAssets";
import ESTAssignAssetsCheckPage from "./pages/employee/Create/ESTAssignAssetsCheckPage";

const componentsToRegister = {
  MyApplications,
  ESTPaymentHistory,
  NewRegistration,
  ESTManageProperties,
  ESTDashboard,
  ESTAllotPropertySummary,
  ESTEditAllotPropertySummary,
  ESTRegCreate,
  ESTRegCheckPage,
  ESTRequiredDoc,
  ESTAcknowledgement,
  ESTAllotmentAcknowledgement,
  ESTAssignAssetCreate,
  ESTAssignAstRequiredDoc,
  ESTAssignAssets,
  ESTAssignAssetsCheckPage,
};

const addComponentsToRegistry = () => {
  Object.entries(componentsToRegister).forEach(([key, value]) => {
    Digit.ComponentRegistryService.setComponent(key, value);
  });
};

export const ESTModule = ({ stateCode, userType, tenants }) => {
  const { path, url } = useRouteMatch();
  addComponentsToRegistry();
  Digit.SessionStorage.set("EST_TENANTS", tenants);

  if (userType === "employee") {
    return <EmployeeApp path={path} url={url} userType={userType} />;
  } else {
    return <CitizenApp path={path} />;
  }
};

export const ESTLinks = ({ matchPath, userType }) => {
  const { t } = useTranslation();

  const links = [
    {
      link: `${matchPath}/my-applications`,
      i18nKey: t("EST_MY_APPLICATIONS"),
    },
    {
      link: `${matchPath}/payment-history`,
      i18nKey: t("EST_PAYMENT_HISTORY"),
    },
  ];

  return (
    <CitizenHomeCard
      header={t("ESTATE_MANAGEMENT")}
      links={links}
      Icon={() => <PropertyHouse />}
    />
  );
};

export const ESTComponents = {
  ESTCard,
  ESTModule,
  ESTLinks,
};