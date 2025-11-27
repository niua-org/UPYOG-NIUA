import React from "react";
import { Switch, Route } from "react-router-dom";
import { AppContainer, BackButton, PrivateRoute } from "@upyog/digit-ui-react-components";
// import { ESTMyApplications } from "./MyApplications";
import ESTApplicationDetails from "./ESTApplicationDetails";
import {ESTPaymentHistory} from "./PaymentHistory";
import ESTMyApplications from "./MyApplications";

const CitizenApp = ({ path }) => {
  return (
    <span className="citizen" style={{ width: "100%" }}>
      <Switch>
        <AppContainer>
          <BackButton>Back</BackButton>
          <PrivateRoute path={`${path}/application/:assetNo/:tenantId`} component={ESTApplicationDetails} />
          <PrivateRoute path={`${path}/my-applications`} component={ESTMyApplications} />
          <PrivateRoute path={`${path}/payment-history`} component={ESTPaymentHistory} />

        </AppContainer>
      </Switch>
    </span>
  );
};

export default CitizenApp;