import React, { useEffect, useState } from "react";
// import { useTranslation } from "react-i18next";
import { useResolvedPath, Routes, Route, Link } from "react-router-dom";
import { CollectPayment } from "./payment-collect";
import { SuccessfulPayment, FailedPayment } from "./response";
// import { SubformComposer } from "../../hoc";
// import { subFormRegistry } from "../../hoc/subFormClass";
import { testForm } from "../../hoc/testForm-config";
import { subFormRegistry } from "@upyog/workbench-ui-libraries";
import { useTranslation } from "react-i18next";
import IFrameInterface from "./IFrameInterface";

subFormRegistry.addSubForm("testForm", testForm);

const EmployeePayment = ({ stateCode, cityCode, moduleCode }) => {
  const userType = "employee";
  const { pathname: currentPath } = useResolvedPath("."); // useRouteMatch → useResolvedPath


  const { t } = useTranslation();

  const [link, setLink] = useState(null);

  const commonProps = { stateCode, cityCode, moduleCode, setLink };

  const isFsm = location?.pathname?.includes("fsm") || location?.pathname?.includes("FSM");

  return (
    <React.Fragment>
      <p className="breadcrumb" style={{ marginLeft: "15px" }}>
        <Link to={`/workbench-ui/employee`}>{t("ES_COMMON_HOME")}</Link>
        {isFsm ? <Link to={`/workbench-ui/employee/fsm/home`}>/ {t("ES_TITLE_FSM")} </Link> : null}
        {isFsm ? <Link to={`/workbench-ui/employee/fsm/inbox`}>/ {t("ES_TITLE_INBOX")}</Link> : null}/ {link}
      </p>
      <Routes>
        <Route
          path="collect/:businessService/:consumerCode"
          element={<CollectPayment {...commonProps} basePath={currentPath} />}
        />
       <Route
          path="success/:businessService/:receiptNumber/:consumerCode"
          element={<SuccessfulPayment {...commonProps} />}
        />
        <Route
          path="integration/:moduleName/:pageName"
          element={<IFrameInterface {...commonProps} />}
        />
        <Route
          path="failure"
          element={<FailedPayment {...commonProps} />}
        />

      </Routes>
    </React.Fragment>
  );
};

export default EmployeePayment;
