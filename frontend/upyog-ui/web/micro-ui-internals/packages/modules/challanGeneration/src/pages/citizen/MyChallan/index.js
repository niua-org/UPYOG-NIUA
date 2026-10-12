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

  const useSearch = Digit.Hooks?.challangeneration?.useChallanGenerationSearch || Digit.Hooks?.challanGeneration?.useChallanGenerationSearch;
  const searchResult = useSearch
    ? useSearch({ tenantId, filters }, { enabled: !!(tenantId && filters.mobileNumber) })
    : { isLoading: false, data: null };

  const { isLoading, data } = searchResult || {};
  const challans = data?.challans || [];

  const handlePay = (challan) => {
    navigate(`/upyog-ui/citizen/payment/my-bills/${challan?.businessService || "Challan_Generation"}/${challan?.challanNo}?workflow=challangeneration`);
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
                  text={challan?.citizen?.name || challan?.additionalDetail?.ownerName || "N/A"}
                />
                <Row
                  className="border-none"
                  label={t("CHALLAN_BUSINESS_SERVICE")}
                  text={t(challan?.businessService) || challan?.businessService || "N/A"}
                />
                <Row
                  className="border-none"
                  label={t("CHALLAN_TOTAL_DUE")}
                  text={`₹ ${challan?.totalAmount || challan?.amount || 0}`}
                />
                <Row
                  className="border-none"
                  label={t("CHALLAN_STATUS")}
                  text={t(`CHALLAN_STATUS_${challan?.applicationStatus}`) || challan?.applicationStatus || "ACTIVE"}
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
