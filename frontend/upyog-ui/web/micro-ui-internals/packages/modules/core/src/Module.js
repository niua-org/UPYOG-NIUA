import React from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { QueryClient as TanstackQueryClient, QueryClientProvider as TanstackQueryClientProvider } from "@tanstack/react-query";
import { Provider } from "react-redux";
import { BrowserRouter as Router } from "react-router-dom";
import { getI18n } from "react-i18next";
import { Body, Loader } from "@nudmcdgnpm/digit-ui-react-components";
import { DigitApp } from "./App";
import SelectOtp from "./pages/citizen/Login/SelectOtp";
import AcknowledgementCF from "./components/AcknowledgementCF";
import CitizenFeedback from "./components/CitizenFeedback";

import getStore from "./redux/store";
import ErrorBoundary from "./components/ErrorBoundaries";
import EmployeeDashboard from "./components/EmployeeDashboard";
import { useState } from "react";
import EDCRAcknowledgement from "./pages/citizen/Home/EDCR/EDCRAcknowledgement"
import CreateAnonymousEDCR from "./pages/citizen/Home/EDCR";
// Receive the version flag at the initialized-store boundary so routing starts
// only after the same application configuration and module data are available.
const DigitUIWrapper = ({ stateCode, enabledModules, moduleReducers, isV2 }) => {
  const { isLoading, data: initData } = Digit.Hooks.useInitStore(stateCode, enabledModules);
  if (isLoading) {
    return <Loader page={true} />;
  }

  const i18n = getI18n();
  return (
    <Provider store={getStore(initData, moduleReducers(initData))}>
      {/*
       * Opt in to React Router v7 behavior while still using v6:
       * - v7_startTransition wraps router state updates in React.startTransition.
       * - v7_relativeSplatPath uses the v7 rules for resolving relative links
       *   inside splat ("*") routes.
       *
       * Without these flags, the app keeps the legacy v6 behavior and logs
       * future-flag warnings. Enabling them now also avoids an unexpected
       * routing behavior change when the application is upgraded to v7.
       */}
      <Router future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <Body>
          {/* Pass the selected auth-flow version into the top-level route
              switch; this does not change the initialized store itself. */}
          <DigitApp
            initData={initData}
            stateCode={stateCode}
            modules={initData?.modules || []}
            appTenants={initData?.tenants || []}
            logoUrl={initData?.stateInfo?.logoUrl || ""}
            isV2={isV2}
          />
        </Body>
      </Router>
    </Provider>
  );
};

// Default to V1 for backward compatibility with applications that consume
// DigitUI without explicitly opting in to the V2 authentication experience.
export const DigitUI = ({ stateCode, registry, enabledModules, moduleReducers, isV2 = false }) => {
  const userType = Digit.UserService.getType();
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 15 * 60 * 1000,
        cacheTime: 50 * 60 * 1000,
        retryDelay: (attemptIndex) => Infinity,
        retry: false,
      },
    },
  });
  const tanstackQueryClient = new TanstackQueryClient({
    defaultOptions: {
      queries: {
        staleTime: 15 * 60 * 1000,
        gcTime: 50 * 60 * 1000,
        retry: false,
      },
    },
  });
  const [privacy, setPrivacy] = useState(Digit.Utils.getPrivacyObject() || {});

  const ComponentProvider = Digit.Contexts.ComponentProvider;
  const PrivacyProvider = Digit.Contexts.PrivacyProvider;

  const DSO = Digit.UserService.hasAccess(["FSM_DSO"]);

  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <TanstackQueryClientProvider client={tanstackQueryClient}>
          <ComponentProvider.Provider value={registry}>
            <PrivacyProvider.Provider
              value={{
                privacy: privacy?.[window.location.pathname],
                resetPrivacy: (_data) => {
                  Digit.Utils.setPrivacyObject({});
                  setPrivacy({});
                },
                getPrivacy: () => {
                  const privacyObj = Digit.Utils.getPrivacyObject();
                  setPrivacy(privacyObj);
                  return privacyObj;
                },
                /*  Descoped method to update privacy object  */
                updatePrivacyDescoped: (_data) => {
                  const privacyObj = Digit.Utils.getAllPrivacyObject();
                  const newObj = { ...privacyObj, [window.location.pathname]: _data };
                  Digit.Utils.setPrivacyObject({ ...newObj });
                  setPrivacy(privacyObj?.[window.location.pathname] || {});
                },
                /**
                 * Main Method to update the privacy object anywhere in the application
                 *
                 * @author jagankumar-egov
                 *
                 * Feature :: Privacy
                 *
                 * @example
                 *    const { privacy , updatePrivacy } = Digit.Hooks.usePrivacyContext();
                 */
                updatePrivacy: (uuid, fieldName) => {
                  setPrivacy(Digit.Utils.updatePrivacy(uuid, fieldName) || {});
                },
              }}
            >
              {/* Forward the caller's version choice unchanged through the
                  providers so every downstream router uses one source of truth. */}
              <DigitUIWrapper isV2={isV2} stateCode={stateCode} enabledModules={enabledModules} moduleReducers={moduleReducers} />
            </PrivacyProvider.Provider>
          </ComponentProvider.Provider>
        </TanstackQueryClientProvider>
      </QueryClientProvider>
    </ErrorBoundary>
  );
};

const componentsToRegister = {
  SelectOtp,
  AcknowledgementCF,
  CitizenFeedback,
  EmployeeDashboard,
  EDCRAcknowledgement,
  CreateAnonymousEDCR,
};

export const initCoreComponents = () => {
  Object.entries(componentsToRegister).forEach(([key, value]) => {
    Digit.ComponentRegistryService.setComponent(key, value);
  });
};
