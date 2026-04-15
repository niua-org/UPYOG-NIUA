import React from "react";
import { ChallanGenerationModule } from "../../Module";
import Inbox from "./Inbox";
import { Switch, useLocation, Link } from "react-router-dom";
import { PrivateRoute } from "@upyog/digit-ui-react-components";
import { useTranslation } from "react-i18next";

/**
 * EmployeeApp component:
 * - Main routing container for challan module (employee side)
 * - Handles navigation, routes, and workflow pages
 */

const EmployeeApp = ({ path, url, userType }) => {
  const { t } = useTranslation();
  const location = useLocation();
  const mobileView = innerWidth <= 640;
  const inboxInitialState = {
    searchParams: {
      status: [],
      businessService: [],
    },
  };

  const combineTaxDueInSearchData = async (searchData, _break, _next) => {
    let returnData;
    const tenantId = Digit.ULBService.getCurrentTenantId();
    let businessService = ["PT"].join();
    let consumerCode = searchData.map((e) => e.propertyId).join();
    try {
      const res = await Digit.PaymentService.fetchBill(tenantId, { consumerCode, businessService });
      let obj = {};
      res.Bill.forEach((e) => {
        obj[e.consumerCode] = e.totalAmount;
      });
      returnData = searchData.map((e) => ({ ...e, due_tax: "₹ " + (obj[e.propertyId] || 0) }));
    } catch (er) {
      const err = er?.response?.data;
      if (["EG_BS_BILL_NO_DEMANDS_FOUND", "EMPTY_DEMANDS"].includes(err?.Errors?.[0].code)) {
        returnData = searchData.map((e) => ({ ...e, due_tax: "₹ " + 0 }));
      }
    }
    return _next(returnData);
  };

  const searchMW = [{ combineTaxDueInSearchData }];

  const EmployeeChallan = Digit?.ComponentRegistryService?.getComponent("MCollectEmployeeChallan");
  const ChallanAcknowledgement = Digit?.ComponentRegistryService?.getComponent("ChallanAcknowledgement");
  const SearchReceiptPage = Digit?.ComponentRegistryService?.getComponent("SearchReceipt");
  const SearchChallanPage = Digit?.ComponentRegistryService?.getComponent("SearchChallan");
  const ChallanSearch = Digit?.ComponentRegistryService?.getComponent("ChallanStepperForm");
  const ChallanResponseCitizen = Digit?.ComponentRegistryService?.getComponent("ChallanResponseCitizen");
  const ChallanApplicationDetails = Digit?.ComponentRegistryService?.getComponent("ChallanApplicationDetails");

  return (
    <Switch>
      <React.Fragment>
        <div className="ground-container">
          <p className="breadcrumb employee-main-application-details" style={{ marginLeft: mobileView ? "2vw" : "revert" }}>
            <Link to="/upyog-ui/employee" className="challan-link-href" >
              {t("ES_COMMON_HOME")}
            </Link>{" "}
            / <span>{t("CHALLAN_MODULE")}</span>
          </p>
          <PrivateRoute exact path={`${path}/`} component={() => <ChallanGenerationModule matchPath={path} userType={userType} />} />
          <PrivateRoute
            path={`${path}/inbox`}
            component={() => (
              <Inbox
                parentRoute={path}
                businessService="PT"
                filterComponent="MCOLLECT_INBOX_FILTER"
                initialStates={inboxInitialState}
                isInbox={true}
              />
            )}
          />{" "}
          <PrivateRoute
            path={`${path}/search`}
            component={() => (
              <Inbox parentRoute={path} businessService="PT" middlewareSearch={searchMW} initialStates={inboxInitialState} isInbox={false} />
            )}
          />
          <PrivateRoute path={`${path}/acknowledgement`} component={() => <ChallanAcknowledgement />} />
          <PrivateRoute path={`${path}/challansearch/:challanno`} component={() => <EmployeeChallan />} />
          <PrivateRoute path={`${path}/search-receipt`} component={() => <SearchReceiptPage />} />{" "}
          <PrivateRoute path={`${path}/search-challan`} component={() => <SearchChallanPage parentRoute={path} />} />{" "}
          <PrivateRoute path={`${path}/generate-challan`} component={() => <ChallanSearch />} />
          <PrivateRoute path={`${path}/response/:id`} component={ChallanResponseCitizen} />
          <PrivateRoute path={`${path}/application/:acknowledgementIds/:tenantId`} component={ChallanApplicationDetails} />
        </div>
      </React.Fragment>
    </Switch>
  );
};

export default EmployeeApp;
