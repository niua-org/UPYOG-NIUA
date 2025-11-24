import { Banner, Card, LinkButton, Row, StatusTable, SubmitBar } from "@upyog/digit-ui-react-components";
import React, { useEffect, useRef } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import getESTAcknowledgementData from "../../../utils/getESTAcknowledgementData";
import { estPayloadData } from "../../../utils";

const GetActionMessage = (props) => {
  const { t } = useTranslation();
  if (props.isSuccess) {
    return window?.location?.href?.includes("edit") ? t("EST_UPDATE_SUCCESSFULL") : t("EST_SUBMIT_SUCCESSFULL");
  }
  // else if (props.isLoading) {
  //   return t("EST_APPLICATION_PENDING");
  // }
  else (!props.isSuccess)
    return t("EST_APPLICATION_FAILED");
};

const rowContainerStyle = {
  padding: "4px 0px",
  justifyContent: "space-between",
};

const BannerPicker = (props) => {
  return (
    <Banner
      message={GetActionMessage(props)}
      applicationNumber={props.data?.Assets[0]?.estateNo || ""}
      info={props.isSuccess ? props.t("EST_APPLICATION_NO") : ""}
      successful={props.isSuccess}
      style={{ width: "100%" }}
    />
  );
};

const ESTAcknowledgement = ({ data, onSuccess }) => {
  const hasRun = useRef(false);
  const { t } = useTranslation();
  const tenantId = Digit.ULBService.getCitizenCurrentTenant(true) || Digit.ULBService.getCurrentTenantId();
  const mutation = Digit.Hooks.estate.useESTCreateAPI(tenantId);
  const user = Digit.UserService.getUser().info;
  const { data: storeData } = Digit.Hooks.useStore.getInitData();
  const { tenants } = storeData || {};
  const assetData = data?.Assetdata?.Assetdata || data?.Assetdata || {};

  useEffect(() => {
    if (hasRun.current || !data?.Assetdata) return;
    hasRun.current = true;
    
    try {
      // if (!assetData?.estateNo) return;
      const payload = {
  RequestInfo: {
    apiId: "Rainmaker",
    authToken: Digit.UserService.getUser()?.access_token,
    userInfo: Digit.UserService.getUser()?.info
  },
  ...estPayloadData(data)
};
      
      mutation.mutate(payload, {
        onSuccess: (responseData) => {
          console.log("Asset API Success:", responseData);
          if (onSuccess) onSuccess();
        },
        onError: (error) => {
          console.error("Asset API Error:", error);
        }
      });
    } catch (err) {
      console.error(err);
    }
  }, []);

  const handleDownloadPdf = async () => {
    try {
      const { Assets = [] } = mutation.data || {};
      const assetInfo = Assets[0] || {};
      const tenantInfo = tenants.find((tenant) => tenant.code === assetInfo.tenantId);
      const pdfData = await getESTAcknowledgementData({ ...assetInfo }, tenantInfo, t);
      Digit.Utils.pdf.generate(pdfData);
    } catch (error) {
      console.error("PDF generation error:", error);
    }
  };

  return (
    <Card>
      <BannerPicker 
        t={t} 
        data={mutation.data || { Assets: [{ estateNo: assetData?.estateNo }] }} 
        isSuccess={true} 
        isLoading={false} 
      />
      <StatusTable>
        <Row
          rowContainerStyle={rowContainerStyle}
          last
          textStyle={{ whiteSpace: "pre", width: "60%" }}
        />
      </StatusTable>
      {/* {mutation.isSuccess && <SubmitBar label={t("EST_ACKNOWLEDGEMENT")} onSubmit={handleDownloadPdf} />} */}
      {user?.type === "CITIZEN" ?
        <Link to={`/upyog-ui/citizen`}>
          <LinkButton label={t("CORE_COMMON_GO_TO_HOME")} />
        </Link>
        :
        <Link to={`/upyog-ui/employee`}>
          <LinkButton label={t("CORE_COMMON_GO_TO_HOME")} />
        </Link>
      }
    </Card>
  );
};

export default ESTAcknowledgement;