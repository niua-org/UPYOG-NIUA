import React from "react";
import { initLibraries } from "@upyog/workbench-ui-libraries";
import { DigitUI } from "@upyog/workbench-ui-module-core";
import { initEngagementComponents } from "@upyog/workbench-ui-module-engagement";
import { initUtilitiesComponents } from "@upyog/workbench-ui-module-utilities";
import { UICustomizations } from "./Customisations/UICustomizations";
import { initWorkbenchComponents } from "@nudmcdgnpm/digit-ui-module-workbench";

window.contextPath = window?.globalConfigs?.getConfig("CONTEXT_PATH");

const enabledModules = [
  "Utilities",
  // "Engagement",
  "Workbench"
];

const moduleReducers = (initData) => ({});

const initDigitUI = () => {
  initEngagementComponents();
  initUtilitiesComponents();
  initWorkbenchComponents();

  window.Digit.Customizations = {
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
