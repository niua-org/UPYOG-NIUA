import { Header, CitizenHomeCard, PTIcon } from "@nudmcdgnpm/digit-ui-react-components";
import React, { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useRouteMatch } from "react-router-dom";
// import EmployeeApp from "./pages/employee";
import CitizenApp from "./pages/citizen";

import CMSearchCertificate from "./pageComponents/CMSearchCertificate";


const componentsToRegister = {
  CMSearchCertificate
};

const addComponentsToRegistry = () => {
  Object.entries(componentsToRegister).forEach(([key, value]) => {
    Digit.ComponentRegistryService.setComponent(key, value);
  });
};


export const COMMONMODULEModule = ({ stateCode, userType, tenants }) => {
  const { path, url } = useRouteMatch();
  const moduleCode = "COMMONMODULE";
  const language = Digit.StoreData.getCurrentLanguage();
  const { isLoading, data: store } = Digit.Services.useStore({ stateCode, moduleCode, language });

  addComponentsToRegistry();

  Digit.SessionStorage.set("CM_TENANTS", tenants);

  useEffect(
    () =>
      userType === "employee" &&
      Digit.LocalizationService.getLocale({
        modules: [`rainmaker-${Digit.ULBService.getCurrentTenantId()}`],
        locale: Digit.StoreData.getCurrentLanguage(),
        tenantId: Digit.ULBService.getCurrentTenantId(),
      }),
    []
  );

  // if (userType === "employee") {
  //   return <EmployeeApp path={path} url={url} userType={userType} />;
  // } else 
  return <CitizenApp />;
};

export const COMMONMODULELinks = ({ matchPath, userType }) => {
  const { t } = useTranslation();
  const [params, setParams, clearParams] = Digit.Hooks.useSessionStorage("COMMONMODULE", {});

  useEffect(() => {
    clearParams();
  }, []);

  const links = [
    
  ];

  return <CitizenHomeCard header={t("ACTION_TEST_EW")} links={links} Icon={() => <PTIcon className="fill-path-primary-main" />} />;
  
};


export const COMMONMODULEComponents = {
  // COMMONMODULECard,
  COMMONMODULEModule,
  COMMONMODULELinks,
  // AST_INBOX_FILTER: (props) => <InboxFilter {...props} />,
  // ASTInboxTableConfig: TableConfig,

};