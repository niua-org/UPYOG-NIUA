import React from "react";
import { initLibraries } from "@upyog/workbench-ui-libraries";
import {
  paymentConfigs,
  PaymentLinks,
  PaymentModule,
} from "@upyog/workbench-ui-module-common";
import { DigitUI } from "@upyog/workbench-ui-module-core";
import { initDSSComponents } from "@upyog/workbench-ui-module-dss";
import { initEngagementComponents } from "@upyog/workbench-ui-module-engagement";
import { initHRMSComponents } from "@upyog/workbench-ui-module-hrms";
import { initUtilitiesComponents } from "@upyog/workbench-ui-module-utilities";
import { UICustomizations } from "./Customisations/UICustomizations";
import { initWorkbenchComponents } from "@nudmcdgnpm/digit-ui-module-workbench";
import {
  initPGRComponents,
  PGRReducers,
} from "@upyog/workbench-ui-module-pgr";

window.contextPath = window?.globalConfigs?.getConfig("CONTEXT_PATH");

const enabledModules = [
  "DSS",
  "NDSS",
  "Utilities",
  // "HRMS",
  // "Engagement",
  "Workbench",
  "PGR"

];

const moduleReducers = (initData) => ({
  initData, pgr: PGRReducers(initData),
});

const initDigitUI = () => {
  window.Digit.ComponentRegistryService.setupRegistry({
    PaymentModule,
    ...paymentConfigs,
    PaymentLinks,
  });

  initPGRComponents();
  initDSSComponents();
  initHRMSComponents();
  initEngagementComponents();
  initUtilitiesComponents();
  initWorkbenchComponents();

  window.Digit.Customizations = {
    PGR: {},
    commonUiConfig: UICustomizations,
  };
};

initLibraries().then(() => {
  initDigitUI();
});

function App() {
  window.contextPath = window?.globalConfigs?.getConfig("CONTEXT_PATH");
  const stateCode =
    window.globalConfigs?.getConfig("STATE_LEVEL_TENANT_ID") ||
    process.env.REACT_APP_STATE_LEVEL_TENANT_ID;
  if (!stateCode) {
    return <h1>stateCode is not defined</h1>;
  }
  return (
    <DigitUI
      stateCode={stateCode}
      enabledModules={enabledModules}
      moduleReducers={moduleReducers}
      // defaultLanding="employee"
    />
  );
}

export default App;
