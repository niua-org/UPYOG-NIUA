import { Banner, Card, LinkButton, Row, StatusTable, SubmitBar } from "@upyog/digit-ui-react-components";
import React, { useEffect, useRef } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import getESTAllotmentAcknowledgementData from "../../../utils/getESTAllotmentAcknowledgementData";
import { createAllotmentData } from "../../../utils";

const GetActionMessage = (props) => {
  const { t } = useTranslation();
  if (props.isSuccess) {
    return t("EST_ALLOTED_SUCCESSFULL");
  } else if (props.isLoading) {
    return t("EST_APPLICATION_PENDING");
  } else return t("EST_APPLICATION_FAILED");
};

const rowContainerStyle = {
  padding: "4px 0px",
  justifyContent: "space-between",
};

const BannerPicker = (props) => {
  return (
    <Banner
      message={GetActionMessage(props)}
      applicationNumber={props.data?.Allotments[0]?.assetNo || ""}
      info={props.isSuccess ? props.t("EST_APPLICATION_NO") : ""}
      successful={props.isSuccess}
      style={{ width: "100%" }} />
  );
};

const ESTAllotmentAcknowledgement = ({ data, onSuccess }) => {
  const hasRun = useRef(false);
  const { t } = useTranslation();
  const tenantId = Digit.ULBService.getCitizenCurrentTenant(true) || Digit.ULBService.getCurrentTenantId();
  const mutation = Digit.Hooks.estate.useESTAssetsAllotment(tenantId);
  const user = Digit.UserService.getUser().info;
  const { data: storeData } = Digit.Hooks.useStore.getInitData();
  const { tenants } = storeData || {};

  useEffect(() => {
    if (hasRun.current || !data?.AssignAssetsData) return;
    hasRun.current = true;
    
    try {
      const allotmentPayload = {
        RequestInfo: {
          apiId: "Rainmaker",
          authToken: Digit.UserService.getUser()?.access_token,
          userInfo: Digit.UserService.getUser()?.info
        },
        ...createAllotmentData(data)
      };
      
      mutation.mutate(allotmentPayload, {
        onSuccess: (responseData) => {
          console.log("Allotment API Success:", responseData);
          if (onSuccess) onSuccess();
        },
        onError: (error) => {
          console.error("Allotment API Error:", error);
        }
      });
    } catch (err) {
      console.error(err);
    }
  }, []);

  const handleDownloadPdf = async () => {
    try {
      const { Allotments = [] } = mutation.data || {};
      const allotmentInfo = Allotments[0] || {};
      const tenantInfo = tenants.find((tenant) => tenant.code === allotmentInfo.tenantId);
      const pdfData = await getESTAllotmentAcknowledgementData({ ...allotmentInfo }, tenantInfo, t);
      Digit.Utils.pdf.generate(pdfData);
    } catch (error) {
      console.error("PDF generation error:", error);
    }
  };

  return (
    <Card>
      <BannerPicker 
        t={t} 
        data={mutation.data || { Allotments: [{ assetNo: data?.AssignAssetsData?.assetNo }] }}
        isSuccess={mutation.isSuccess} 
        isLoading={mutation.isLoading} 
      />
      <StatusTable>
        <Row
          rowContainerStyle={rowContainerStyle}
          last
          textStyle={{ whiteSpace: "pre", width: "60%" }}
        />
      </StatusTable>
      {mutation.isSuccess && <SubmitBar label={t("EST_ALLOTMENT_ACKNOWLEDGEMENT")} onSubmit={handleDownloadPdf} />}
      <Link to={user?.type === "CITIZEN" ? "/upyog-ui/citizen" : "/upyog-ui/employee"}>
        <LinkButton label={t("CORE_COMMON_GO_TO_HOME")} />
      </Link>
    </Card>
  );
};

export default ESTAllotmentAcknowledgement;