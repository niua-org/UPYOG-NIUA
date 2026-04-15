import { Banner, Card, CardText, ActionBar, SubmitBar } from "@upyog/digit-ui-react-components";
import React from "react";
import { useTranslation } from "react-i18next";
import { useHistory } from "react-router-dom";
import { stringReplaceAll } from "../../utils";

//NDCResponseCitizen is a component that displays the response after a citizen submits an NDC application.
//  It shows a banner with the application status and provides options to go back to the home page, go to the NDC home page, or make a payment if the application is approved.
//  The content of the banner and the available actions are based on the application status.
const NDCResponseCitizen = (props) => {
  const { state } = props.location;
  const { t } = useTranslation();
  const history = useHistory();
  const nocData = state?.data?.Noc?.[0];
  const tenantId = Digit.ULBService.getCurrentTenantId();

  const pathname = history?.location?.pathname || "";
  const ndcCode = pathname.split("/").pop(); 
  const onSubmit = () => {
    history.push(`/upyog-ui/citizen`);
  };

  const onGoToNDC = () => {
    history.push(`/upyog-ui/citizen/ndc-home`);
  };

  const handlePayment = () => {
    history.push(`/upyog-ui/citizen/payment/collect/NDC/${ndcCode}/${tenantId}?tenantId=${tenantId}`);
    // pathname: `/digit-ui/citizen/payment/collect/${application?.businessService}/${application?.applicationNumber}`,
  };


  return (
    <div>
      <Card>
        <Banner
          // message={t(`NDC_${stringReplaceAll(nocData?.nocType, ".", "_")}_${stringReplaceAll(nocData?.applicationStatus, ".", "_")}_HEADER`)}
          message={"NDC Application Submitted Successfully"}
          applicationNumber={ndcCode}
          info={nocData?.applicationStatus == "REJECTED" ? "" : t(`NDC_APPROVAL_NUMBER`)}
          successful={nocData?.applicationStatus == "REJECTED" ? false : true}
          style={{ padding: "10px" }}
          headerStyles={{ fontSize: "32px", wordBreak: "break-word" }}
        />
        {/* {nocData?.applicationStatus !== "REJECTED" ? (
          <CardText>
            {t(`NDC_${stringReplaceAll(nocData?.nocType, ".", "_")}_${stringReplaceAll(nocData?.applicationStatus, ".", "_")}_SUB_HEADER`)}
          </CardText>
        ) : null} */}
        <ActionBar className="ndc-action-bar" >
          <SubmitBar label={t("CORE_COMMON_GO_TO_HOME")} onSubmit={onSubmit} />
          <SubmitBar label={t("CORE_COMMON_GO_TO_NDC")} onSubmit={onGoToNDC} />
          <SubmitBar label={t("CS_APPLICATION_DETAILS_MAKE_PAYMENT")} onSubmit={handlePayment} />
        </ActionBar>
      </Card>
    </div>
  );
};
export default NDCResponseCitizen;
