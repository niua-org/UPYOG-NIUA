import { AppContainer, BackButton, PrivateRoute } from "@nudmcdgnpm/digit-ui-react-components";
import React from "react";
import { Route, Switch, useRouteMatch } from "react-router-dom";
import { shouldHideBackButton } from "../../utils";
import { useTranslation } from "react-i18next";

const hideBackButtonConfig = [];

const App = () => {
  const { path, url, ...match } = useRouteMatch();
  const { t } = useTranslation();
//  to show back button on top left of the page in order to go back to previous page
  return (
    <span className={"ads-citizen"}style={{width:"100%"}}>
      <Switch>
        <AppContainer>
          {!shouldHideBackButton(hideBackButtonConfig) ? <BackButton>Back</BackButton> : ""}

        </AppContainer>
      </Switch>
    </span>
  );
};

export default App;