import { Loader } from "@upyog/digit-ui-react-components";
import React from "react";
import { useRouteMatch } from "react-router-dom";
import EmployeeApp from "./pages/employee";
import CitizenApp from "./pages/citizen";
import ApplicationOverview from "./pages/employee/ApplicationOverview";
import CitizenApplicationOverview from "./pages/citizen/ApplicationOverview";
import NOCCard from "./pages/employee/EmployeeCard";
import Inbox from "./pages/employee/Inbox";
import NOCSearchApplication from "./pages/employee/SearchApplication/Search";
import { NewNDCStepForm as NewNDCStepFormCitizen } from "./pages/citizen/createNCDApplication/NewNDCStepForm";
import { NewNDCStepFormOne as NewNDCStepFormOneCitizen } from "./pages/citizen/createNCDApplication/NewNDCStepFormOne";
import SelectNDCReason from "./pageComponents/SelectNDCReason";
import { PropertyDetailsForm as PropertyDetailsFormCitizen } from "./pageComponents/PropertyDetailsForm";
import { PropertySearchNSummary as NDCPropertySearch } from "./components/NDCPropertySearch";
import SelectNDCDocuments from "./pageComponents/SelectNDCDocuments";
import { NewNDCStepFormTwo as NewNDCStepFormTwoCitizen } from "./pages/citizen/createNCDApplication/NewNDCStepFormTwo";
import NDCSummary from "./pageComponents/NDCSummary";
import { NDCNewFormSummaryStepThreeCitizen } from "./pages/citizen/createNCDApplication/NDCNewFormSummaryStepThreeCitizen";
import { PayWSBillModal } from "./pageComponents/PayWSBillModal";
import { NewNDCStepForm as NewNDCStepFormEmployee } from "./pages/employee/createNDCApplication/createNDCApplicationStepperForm/NewNDCStepForm";
import { NewNDCStepFormOne as NewNDCStepFormOneEmployee } from "./pages/employee/createNDCApplication/createNDCApplicationStepperForm/NewNDCStepFormOne";
import { NewNDCStepFormTwo as NewNDCStepFormTwoEmployee } from "./pages/employee/createNDCApplication/createNDCApplicationStepperForm/NewNDCStepFormTwo";
import { NDCNewFormSummaryStepThreeEmployee } from "./pages/employee/createNDCApplication/createNDCApplicationStepperForm/NDCNewFormSummaryStepThreeEmployee";
import MyApplications from "./pages/citizen/Applications/Application";
import NDCResponseCitizen from "./pages/citizen/NDCResponseCitizen";
import Timeline from "./components/NDCTimeline";

import getRootReducer from "./redux/reducers";

export const NDCReducers = getRootReducer;
// NDCModule is the entry point for the NDC module. It checks the user type and renders the appropriate app (EmployeeApp or CitizenApp) based on that. It also fetches necessary data from the store and handles loading state.
const NDCModule = ({ stateCode, userType, tenants }) => {
  const moduleCode = "NDC";
  const { path, url } = useRouteMatch();
  const language = Digit.StoreData.getCurrentLanguage();
  const { isLoading, data: store } = Digit.Services.useStore({ stateCode, moduleCode, language });

  Digit.SessionStorage.set("NDC_TENANTS", tenants);

  if (isLoading) {
    return <Loader />;
  }

  if (userType === "citizen") {
    return <CitizenApp />;
  }

  return <EmployeeApp path={path} stateCode={stateCode} />;
};

const componentsToRegister = {
  NDCModule,
  NOCCard,
  NDCApplicationOverview: ApplicationOverview,
  NDCInbox: Inbox,
  NOCSearchApplication,
  NewNDCStepFormCitizen,
  SelectNDCReason,
  NewNDCStepFormOneCitizen,
  PropertyDetailsFormCitizen,
  NDCPropertySearch,
  SelectNDCDocuments,
  NewNDCStepFormTwoCitizen,
  NDCSummary,
  NDCNewFormSummaryStepThreeCitizen,
  PayWSBillModal,
  NewNDCStepFormEmployee,
  NewNDCStepFormOneEmployee,
  NewNDCStepFormTwoEmployee,
  NDCNewFormSummaryStepThreeEmployee,
  MyApplications,
  NDCResponseCitizen,
  CitizenApplicationOverview,
  Timeline
};

export const initNDCComponents = () => {
  Object.entries(componentsToRegister).forEach(([key, value]) => {
    Digit.ComponentRegistryService.setComponent(key, value);
  });
};
