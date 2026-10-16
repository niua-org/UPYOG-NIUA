/**
 * CitizenApp Component - Challan Generation Module
 * -----------------------------------------------------------------------------
 * Acts as the top-level citizen router for the Challan Generation module.
 * Responsible for handling citizen-facing routes such as:
 *  - /my-payments       : Lists all active and paid challans associated with the logged-in citizen.
 *  - /details/:challanNo: Displays detailed summary of a specific challan including offender info,
 *                         offence description, evidence docs, workflow timeline, and payment actions.
 *  - /response/:id      : Displays receipt/acknowledgement after online payment completion.
 * -----------------------------------------------------------------------------
 */

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
