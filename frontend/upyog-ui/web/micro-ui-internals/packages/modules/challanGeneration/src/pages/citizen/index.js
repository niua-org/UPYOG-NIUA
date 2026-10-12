import React from "react";
import { Routes, Route, Link } from "react-router-dom";
import { AppContainer, BackButton, PrivateRoute } from "@nudmcdgnpm/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import ChallanMyPayments from "./MyPayments";
import CitizenChallanDetails from "./ChallanDetails";

const CitizenApp = () => {
  const { t } = useTranslation();
  const ChallanResponseCitizen = Digit?.ComponentRegistryService?.getComponent("ChallanResponseCitizen");

  return (
    <span className={"challan-citizen pt-citizen pt-auto-149"}>
      <AppContainer>
        <BackButton>Back</BackButton>
        <Routes>
          <Route path="my-payments" element={<PrivateRoute><ChallanMyPayments /></PrivateRoute>} />
          <Route path="details/:challanNo" element={<CitizenChallanDetails />} />
          <Route path="response/:id" element={<PrivateRoute><ChallanResponseCitizen /></PrivateRoute>} />
          <Route path="*" element={<PrivateRoute><ChallanMyPayments /></PrivateRoute>} />
        </Routes>
      </AppContainer>
    </span>
  );
};

export default CitizenApp;
