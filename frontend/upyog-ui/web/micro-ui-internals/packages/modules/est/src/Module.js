import { CitizenHomeCard, PTIcon } from "@upyog/digit-ui-react-components";
import React, { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useRouteMatch } from "react-router-dom";

import CitizenApp from "./pages/citizen";
import ESTCard from "./components/ESTCard";
import EmployeeApp from "./pages/employee";
import MyApplications from "./pages/citizen/MyApplications";
import PaymentHistory from "./pages/citizen/PaymentHistory";

const componentsToRegister = {
  MyApplications,
  PaymentHistory,
};

const addComponentsToRegistry = () => {
  Object.entries(componentsToRegister).forEach(([key, value]) => {
    Digit.ComponentRegistryService.setComponent(key, value);
  });
};

export const ESTModule = ({ stateCode, userType, tenants }) => {
  const { path, url } = useRouteMatch();
  addComponentsToRegistry();

  if (userType === "employee") {
    return <EmployeeApp path={path} url={url} userType={userType} />;
  } else {
    return <CitizenApp path={path} />;
  }
};

export const ESTLinks = null;
export const ESTComponents = {
  ESTCard,
  ESTModule,
  ESTLinks,
};
