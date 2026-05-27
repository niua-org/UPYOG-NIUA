import React from "react";
import { createRoot } from "react-dom/client";

import { initLibraries } from "@upyog/workbench-ui-libraries";
import { DigitUI } from "@upyog/workbench-ui-module-core";
import { initWorkbenchComponents } from "@nudmcdgnpm/digit-ui-module-workbench";
import { pgrCustomizations } from "./pgr";
import { UICustomizations } from "./UICustomizations";

var Digit = window.Digit || {};

const container = document.getElementById("root");
  const root = createRoot(container);
const enabledModules = [ 
    "DSS", 
    "Workbench",
    "PGR"
  ];

const initTokens = (stateCode) => {
  const userType = window.sessionStorage.getItem("userType") || process.env.REACT_APP_USER_TYPE || "CITIZEN";
  const token = window.localStorage.getItem("token") || process.env[`REACT_APP_${userType}_TOKEN`];

  const citizenInfo = window.localStorage.getItem("Citizen.user-info");

  const citizenTenantId = window.localStorage.getItem("Citizen.tenant-id") || stateCode;

  const employeeInfo = window.localStorage.getItem("Employee.user-info");
  const employeeTenantId = window.localStorage.getItem("Employee.tenant-id");

  const userTypeInfo = userType === "CITIZEN" || userType === "QACT" ? "citizen" : "employee";
  window.Digit.SessionStorage.set("user_type", userTypeInfo);
  window.Digit.SessionStorage.set("userType", userTypeInfo);

  if (userType !== "CITIZEN") {
    window.Digit.SessionStorage.set("User", { access_token: token, info: userType !== "CITIZEN" ? JSON.parse(employeeInfo) : citizenInfo });
  } else {
  }

  window.Digit.SessionStorage.set("Citizen.tenantId", citizenTenantId);

  if (employeeTenantId && employeeTenantId.length) window.Digit.SessionStorage.set("Employee.tenantId", employeeTenantId);
};

const initDigitUI = () => {
  window.contextPath = window?.globalConfigs?.getConfig("CONTEXT_PATH") || "workbench-ui";
  window.Digit.Customizations = {
    PGR: pgrCustomizations,
    commonUiConfig: UICustomizations
  };
  window?.Digit.ComponentRegistryService.setupRegistry({
  });

  initWorkbenchComponents();
 
  const moduleReducers = (initData) =>  ({
    pgr: PGRReducers(initData),
  });

  const stateCode = window?.globalConfigs?.getConfig("STATE_LEVEL_TENANT_ID") || "pb";
  initTokens(stateCode);

  createRoot(document.getElementById("root")).render(
    <DigitUI stateCode={stateCode} enabledModules={enabledModules} defaultLanding="employee" moduleReducers={moduleReducers} />
  );
};

initLibraries().then(() => {
  initDigitUI();
});
