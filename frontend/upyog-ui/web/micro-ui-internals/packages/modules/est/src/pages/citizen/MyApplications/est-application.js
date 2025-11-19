import { Card, KeyNote, SubmitBar } from "@upyog/digit-ui-react-components";
import React from "react";
import { useTranslation } from "react-i18next";
import { Link, useHistory } from "react-router-dom";

const EstateApplication = ({ application, tenantId, buttonLabel }) => {
  const { t } = useTranslation();
  const history = useHistory();

  const handleMakePayment = () => {
    history.push({
      pathname: `/upyog-ui/citizen/payment/my-bills/est-services/${application?.allotmentId}`,
      state: { 
        tenantId: tenantId, 
        allotmentId: application?.allotmentId,
        consumerCode: application?.allotmentId
      },
    });
  };

  const handleViewDetails = () => {
    history.push({
      pathname: `/upyog-ui/citizen/est/application/${application?.allotmentId}/${tenantId}`,
      state: { applicationData: application }
    });
  };

  return (
    <Card style={{ marginBottom: "10px" }}>
      <KeyNote keyValue={t("EST_ALLOTMENT_ID")} note={application?.allotmentId} />
      <KeyNote keyValue={t("EST_ASSET_NUMBER")} note={application?.assetNo} />
      <KeyNote keyValue={t("EST_ALLOTTEE_NAME")} note={application?.alloteeName} />
      <KeyNote keyValue={t("EST_MOBILE_NUMBER")} note={application?.mobileNo} />
      <KeyNote keyValue={t("EST_MONTHLY_RENT")} note={`₹${application?.monthlyRent || 0}`} />
      <KeyNote keyValue={t("EST_STATUS")} note={application?.status || "PENDING"} />
      
      <div style={{ marginTop: "10px" }}>
        <SubmitBar label={buttonLabel || t("EST_VIEW_DETAILS")} onSubmit={handleViewDetails} />
        <SubmitBar 
          label={t("CS_APPLICATION_DETAILS_MAKE_PAYMENT")} 
          onSubmit={handleMakePayment} 
          style={{ margin: "10px" }} 
        />
      </div>
    </Card>
  );
};

export default EstateApplication;