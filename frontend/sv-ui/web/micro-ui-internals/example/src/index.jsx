import React from "react";
import ReactDOM from "react-dom/client";

import { initLibraries } from "@nudmcdgnpm/digit-ui-libraries";
import { PaymentModule } from "@upyog/digit-ui-module-common";
import { StreetVendingUI } from "@upyog/digit-ui-module-core";
import "@nudmcdgnpm/cnd-css";
import { SVComponents, SVLinks, SVModule } from "@nudmcdgnpm/upyog-ui-module-sv";
import { initEngagementComponents } from "@upyog/digit-ui-module-engagement";

import { SVConstants } from "./SVConstants";

/**
 * This file initializes and renders the Street Vending UI application.
 * 
 * - Loads required libraries and components for the application.
 * - Sets up the `ComponentRegistryService` with modules and components.
 * - Initializes user tokens and session storage based on the user type (citizen or employee).
 * - Configures the application with the state code and enabled modules.
 * - renders the `StreetVendingUI` component into the root DOM element.
 */

var Digit = window.Digit || {};

const enabledModules = [
  "Payment",
  "QuickPayLinks",
  "Engagement",
  "SV"
];

const initTokens = (stateCode) => {
  const userType = window.sessionStorage.getItem("userType") || import.meta.env.VITE_USER_TYPE || "CITIZEN";

  const token = window.localStorage.getItem("token") || (userType === "CITIZEN" ? import.meta.env.VITE_CITIZEN_TOKEN : import.meta.env.VITE_EMPLOYEE_TOKEN);
 
  const citizenInfo = window.localStorage.getItem("Citizen.user-info")
 
  const citizenTenantId = window.localStorage.getItem("Citizen.tenant-id") || stateCode;

  const employeeInfo = window.localStorage.getItem("Employee.user-info");
  const employeeTenantId = window.localStorage.getItem("Employee.tenant-id");

  const userTypeInfo = userType === "CITIZEN" || userType === "QACT" ? "citizen" : "employee";
  window.Digit.SessionStorage.set("user_type", userTypeInfo);
  window.Digit.SessionStorage.set("userType", userTypeInfo);

  if (userType !== "CITIZEN") {
    window.Digit.SessionStorage.set("User", { access_token: token, info: userType !== "CITIZEN" ? JSON.parse(employeeInfo) : citizenInfo });
  } else {
    // if (!window.Digit.SessionStorage.get("User")?.extraRoleInfo) window.Digit.SessionStorage.set("User", { access_token: token, info: citizenInfo });
  }

  window.Digit.SessionStorage.set("Citizen.tenantId", citizenTenantId);

  if (employeeTenantId && employeeTenantId.length) window.Digit.SessionStorage.set("Employee.tenantId", employeeTenantId);
};

const initSVUI = () => {
  window?.Digit.ComponentRegistryService.setupRegistry({
  PaymentModule,
  SVModule,
  SVLinks,
  ...SVComponents,
  });

  initEngagementComponents();
  const moduleReducers = (initData) => ({
    // pgr: PGRReducers(initData),
  });

  window.Digit.Customizations = {
  };

  const stateCode = window?.globalConfigs?.getConfig("STATE_LEVEL_TENANT_ID") || SVConstants.Tenant;
  initTokens(stateCode);

  // const registry = window?.Digit.ComponentRegistryService.getRegistry();
  const root = ReactDOM.createRoot(document.getElementById("root"));
  root.render(
  <StreetVendingUI 
  stateCode={stateCode} 
  enabledModules={enabledModules} 
  moduleReducers={moduleReducers} 
  />);
};

initLibraries().then(() => {
  initSVUI();
});
