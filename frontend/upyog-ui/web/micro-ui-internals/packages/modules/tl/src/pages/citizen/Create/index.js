/**
 * CreateTradeLicence — citizen new-application flow.
 *
 * Consumes workbench AccordionNewApplication.json (or MDMS NewApplication) via
 * shared resolveFormConfig + useFormWizard + FormFlowRoutes.
 * Accordion when navigation.pattern = "accordion"; otherwise stepped wizard.
 */
import React, { useMemo } from "react";
import { useTranslation } from "react-i18next";
import {
  FormFlowRoutes,
  resolveFormConfig,
} from "@nudmcdgnpm/digit-ui-react-components";
import localAccordion from "../../../config/AccordionNewApplication.json";
import TLCitizenNewApplication from "../../../pageComponents/TLCitizenNewApplication";
import TradeLicense from "../../../pageComponents/TradeLicense";
import useTlWizard from "../../../utils/useTlWizard";
import { mapDynamicTlToCheckSession } from "../../../utils/tlSessionAdapter";

/** Resolves a wizard step name to the citizen accordion or documents component. */
const resolveStepComponent = (component) => {
  if (typeof component !== "string") return component;
  const fromRegistry = Digit.ComponentRegistryService.getComponent(component);
  if (fromRegistry) return fromRegistry;
  if (component === "TLCitizenNewApplication") return TLCitizenNewApplication;
  if (component === "TradeLicense") return TradeLicense;
  return null;
};

/**
 * Citizen new-application wizard. Session key is TL_CREATE_TRADE; check and
 * acknowledgement sit after the accordion form.
 * Document rows for /info are read in TradeLicense from config/RequiredDocuments.json.
 */
const CreateTradeLicence = () => {
  const { t } = useTranslation();
  const navigate = Digit.Hooks.useCustomNavigate();
  const stateId = Digit.ULBService.getStateId();

  const { data: mdmsConfig, isLoading: isMdmsLoading } = Digit.Hooks.useEnabledMDMS(
    stateId,
    "TradeLicense",
    [{ name: "NewApplication" }],
    {
      select: (data) => data?.TradeLicense?.NewApplication || null,
    }
  );
  // Local accordion JSON, overlaid by MDMS TradeLicense.NewApplication when present.
  const initialConfig = useMemo(
    () =>
      resolveFormConfig({
        local: localAccordion,
        masterKey: "NewApplication",
        mdms: mdmsConfig,
        preferAccordion: true,
        preferFieldArray: true,
      }),
    [mdmsConfig]
  );

  const waitingForConfig = isMdmsLoading && !initialConfig;
  const indexRoute = initialConfig?.[0]?.navigation?.indexRoute || "info";

  const {
    config,
    params,
    match,
    handleSelect,
    onAckSuccess,
    onUpdateSuccess,
    isReady,
  } = useTlWizard({
    mdmsData: initialConfig,
    isLoading: waitingForConfig,
    indexRoute,
    sessionKey: "TL_CREATE_TRADE",
    sessionStepKey: "newApplication",
    terminalSegments: ["check", "acknowledgement", "info", "apply"],
    multiStepNavigation: initialConfig?.[0]?.navigation?.multiStepNavigation === true,
    invalidateQueryKey: "TL_CREATE_TRADE",
    transformSession: mapDynamicTlToCheckSession,
  });

  const checkValue = useMemo(() => mapDynamicTlToCheckSession(params), [params]);
  const timeline = initialConfig?.[0]?.navigation?.timeline;

  const goToAcknowledgement = async () => {
    sessionStorage.setItem("isCreateEnabled", "true");
    navigate("acknowledgement");
  };

  const CheckPage = Digit?.ComponentRegistryService?.getComponent("TLCheckPage");
  const TLAcknowledgement = Digit?.ComponentRegistryService?.getComponent("TLAcknowledgement");

  return (
    <FormFlowRoutes
      config={config}
      isReady={isReady}
      resolveComponent={resolveStepComponent}
      onSelect={handleSelect}
      formData={params}
      t={t}
      userType="citizen"
      parentRoute={match?.pathnameBase}
      indexRoute={config.indexRoute || indexRoute}
      onStepSelect={(routeObj, key, data, skipStep, index, isAddMultiple) => {
        if (routeObj.component === "TradeLicense") {
          handleSelect(routeObj.key || "info", {}, false);
          return;
        }
        try {
          localStorage.setItem("TLAppSubmitEnabled", "true");
        } catch (e) {
          /* ignore */
        }
        handleSelect(key, data, skipStep, index, isAddMultiple);
      }}
      checkRoute={{
        path: "check",
        element: (
          <CheckPage
            onSubmit={goToAcknowledgement}
            value={checkValue}
            timeline={timeline}
          />
        ),
      }}
      ackRoute={{
        path: "acknowledgement",
        element: (
          <TLAcknowledgement
            data={checkValue}
            onSuccess={onAckSuccess}
            onUpdateSuccess={onUpdateSuccess}
          />
        ),
      }}
    />
  );
};

export default CreateTradeLicence;
