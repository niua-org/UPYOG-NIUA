import React from "react";
import { Route, Routes, useResolvedPath } from "react-router-dom";
import { BillList } from "./my-bills/my-bills";
import BillDetails from "./bill-details/bill-details";
import { BackButton } from "@upyog/workbench-ui-react-components";

const BillRoutes = ({ billsList, paymentRules, businessService }) => {
  const { pathname: currentPath } = useResolvedPath(); //  useRouteMatch → useResolvedPath

  return (
    <React.Fragment>
      <BackButton />
      <Routes>                                              /* Switch → Routes */
        <Route
          index                                             /* exact path → index route */
          element={<BillList {...{ billsList, currentPath, paymentRules, businessService }} />}
        />
        <Route
          path=":consumerCode"                             /* relative path */
          element={<BillDetails {...{ paymentRules, businessService }} />}
        />
      </Routes>
    </React.Fragment>
  );
};

export default BillRoutes;
