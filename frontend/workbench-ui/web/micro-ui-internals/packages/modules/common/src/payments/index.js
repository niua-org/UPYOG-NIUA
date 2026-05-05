import { Loader } from "@upyog/workbench-ui-react-components";
import React from "react";
import { useTranslation } from "react-i18next";
import { useLocation } from "react-router-dom";
import CitizenPayment from "./citizen";
import { getKeyNotesConfig } from "./citizen/keynotesConfig";
import EmployeePayment from "./employee";


export const PaymentModule = ({ deltaConfig = {}, stateCode, cityCode, moduleCode = "Payment", userType }) => {
  const location = useLocation(); // Changed from useRouteMatch
  const path = location.pathname; // Get current path
  const url = location.pathname;  // In v6, path and url are the same

  const store = { data: {} }; //Digit.Services.useStore({}, { deltaConfig, stateCode, cityCode, moduleCode, language });

  if (Object.keys(store).length === 0) {
    
    return <Loader />;
  }

  const getPaymentHome = () => {
    if (userType === "citizen") return <CitizenPayment {...{ stateCode, moduleCode, cityCode, path, url }} />;
    else return <EmployeePayment {...{ stateCode, cityCode, moduleCode }} />;
  };
  return <React.Fragment>{getPaymentHome()}</React.Fragment>;
};

export const PaymentLinks = ({ matchPath }) => {
  const { t } = useTranslation();
  return null;
};

export const paymentConfigs = {
  getBillDetailsConfigWithBusinessService: getKeyNotesConfig,
};



