/**
 * =============================================================================
 * MyChallanResult Component - Challan Generation Module (Citizen Side)
 * =============================================================================
 * Purpose:
 *   Displays a list of all challans issued against the logged-in citizen's
 *   registered mobile number.
 *
 * Key Functionality:
 *   1. Identity Resolution: Retrieves logged-in citizen's mobile number and tenant context.
 *   2. Search Execution   : Calls Challan Generation search API filtered by mobileNumber.
 *   3. Responsive Listing : Renders each challan as a distinct Card with key attributes:
 *                           - Challan Number
 *                           - Offender / Citizen Name
 *                           - Business Service
 *                           - Total Due Amount
 *                           - Current Lifecycle Status
 *                           - Issue Date (formatted to en-IN)
 *   4. Contextual Actions :
 *       - Unpaid: Shows "Pay Now" button redirecting to bill payment checkout.
 *       - Paid  : Shows "View Details" button navigating to Challan Details screen.
 * =============================================================================
 */

import React from "react";
import { Header, Loader, Card, CardText, SubmitBar, StatusTable, Row } from "@nudmcdgnpm/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";

const MyChallanResult = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const userInfo = Digit.UserService.getUser();
  const tenantId = Digit.ULBService.getCitizenCurrentTenant(true) || Digit.ULBService.getCurrentTenantId();

  const filters = {
    mobileNumber: userInfo?.info?.mobileNumber,
  };

  const { isLoading, data } = Digit.Hooks.challangeneration.useChallanGenerationSearch(
    { tenantId, filters },
    { enabled: Boolean(tenantId && filters?.mobileNumber) }
  );

  const challans = data?.challans || [];

    const handlePay = (challan) => {
    navigate(`/upyog-ui/citizen/payment/my-bills/${challan?.businessService}/${challan?.challanNo}?workflow=challangeneration`);
  };


  const handleViewDetails = (challan) => {
    navigate(`/upyog-ui/citizen/challangeneration/details/${challan?.challanNo}`, { state: { challan } });
  };


  if (isLoading) {
    return <Loader />;
  }

  return (
    <React.Fragment>
      <Header>{`${t("CHALLAN_MY_CHALLANS_HEADER")} (${challans.length})`}</Header>
      {challans.length === 0 ? (
        <Card>
          <CardText>{t("CS_NO_CHALLANS_FOUND")}</CardText>
        </Card>
      ) : (
        challans.map((challan, index) => {
          const isPaid = challan?.applicationStatus === "PAID";
          const status = challan?.applicationStatus || "ACTIVE";
          return (
            <Card key={challan?.challanNo || index}>
              <StatusTable>
                <Row
                  className="border-none"
                  label={t("CHALLAN_CHALLAN_NO")}
                  text={challan?.challanNo || "N/A"}
                />
                <Row
                  className="border-none"
                  label={t("CHALLAN_NAME")}
                  text={challan?.citizen?.name || "N/A"}
                />
                <Row
                  className="border-none"
                  label={t("CHALLAN_BUSINESS_SERVICE")}
                  text={challan?.businessService ? t(challan.businessService) : t("CS_NA")}
                />
                <Row
                  className="border-none"
                  label={t("CHALLAN_TOTAL_DUE")}
                  text={`₹ ${challan?.challanAmount || 0}`}
                />
                <Row
                  className="border-none"
                  label={t("CHALLAN_STATUS")}
                  text={t(`CHALLAN_STATUS_${status}`, status)}
                />
                <Row
                  className="border-none"
                  label={t("CHALLAN_ISSUE_DATE")}
                  text={
                    challan?.auditDetails?.createdTime
                      ? new Date(challan.auditDetails.createdTime).toLocaleDateString("en-IN")
                      : "N/A"
                  }
                />
              </StatusTable>
              <SubmitBar
                label={!isPaid ? (t("CHALLAN_PAY_NOW")) : (t("CHALLAN_VIEW_DETAILS"))}
                onSubmit={!isPaid ? () => handlePay(challan) : () => handleViewDetails(challan)}
              />
            </Card>
          );
        })
      )}
    </React.Fragment>
  );
};

export default MyChallanResult;
