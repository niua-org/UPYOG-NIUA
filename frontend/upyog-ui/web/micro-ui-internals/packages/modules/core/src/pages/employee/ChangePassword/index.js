import React, { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { AppContainer } from "@nudmcdgnpm/digit-ui-react-components";
import { Route, Routes } from "react-router-dom";
import { config } from "./config";
import ChangePasswordComponent from "./changePassword";

const EmployeeChangePassword = () => {
  const { t } = useTranslation();
  const { path } = Digit.Hooks.useModuleBasePath();

  const params = useMemo(() =>
    config.map(
      (step) => {
        const texts = {};
        for (const key in step.texts) {
          texts[key] = t(step.texts[key]);
        }
        return { ...step, texts };
      },
      [config]
    )
  );

  return (
    <Routes>
      <Route path={`*`}>
        <ChangePasswordComponent config={params[0]} t={t} />
      </Route>
    </Routes>
  );
};

export default EmployeeChangePassword;
