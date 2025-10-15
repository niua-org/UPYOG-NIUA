import React from "react";
import { Switch, Route } from "react-router-dom";
import { AppContainer, BackButton, PrivateRoute } from "@upyog/digit-ui-react-components";
import MyApplications from "./MyApplications";
import PaymentHistory from "./PaymentHistory";

const CitizenApp = ({ path }) => {
  return (
    <span className="citizen" style={{ width: "100%" }}>
      <Switch>
        <AppContainer>
          <BackButton>Back</BackButton>
          <PrivateRoute path={`${path}/myApplications`} component={MyApplications} />
          <PrivateRoute path={`${path}/paymentHistory`} component={PaymentHistory} />
        </AppContainer>
      </Switch>
    </span>
  );
};

export default CitizenApp;
