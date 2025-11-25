import {
  Banner,
  Card,
  LinkButton,
  Row,
  StatusTable,
} from "@upyog/digit-ui-react-components";
import React, { useEffect, useRef } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { estPayloadData } from "../../../utils";

const GetActionMessage = ({ t, isSuccess, isLoading, isError }) => {
  if (isSuccess) {
    return window?.location?.href?.includes("edit")
      ? t("EST_UPDATE_SUCCESSFULL")
      : t("EST_SUBMIT_SUCCESSFULL");
  }
  if (isError) {
    return t("EST_APPLICATION_FAILED");
  }
  return "";
};

const rowContainerStyle = {
  padding: "4px 0px",
  justifyContent: "space-between",
};

const BannerPicker = (props) => {
  const { t } = useTranslation();
  return (
    <Banner
      message={
        <GetActionMessage
          t={t}
          isSuccess={props.isSuccess}
          isLoading={props.isLoading}
          isError={props.isError}
        />
      }
      applicationNumber={props.data?.Assets?.[0]?.estateNo || ""}
      info={props.isSuccess ? t("EST_APPLICATION_NO") : ""}
      successful={props.isSuccess}
      style={{ width: "100%" }}
    />
  );
};

const ESTAcknowledgement = ({ data, onSuccess }) => {
  const hasRun = useRef(false);
  const { t } = useTranslation();

  const tenantId =
    Digit.ULBService.getCitizenCurrentTenant(true) ||
    Digit.ULBService.getCurrentTenantId();

  const mutation = Digit.Hooks.estate.useESTCreateAPI(tenantId);
  const user = Digit.UserService.getUser().info;

  const { data: storeData } = Digit.Hooks.useStore.getInitData();
  const { tenants } = storeData || {};
  const assetData = data?.Assetdata?.Assetdata || data?.Assetdata || {};

  useEffect(() => {
    if (hasRun.current || !data?.Assetdata) return;
    hasRun.current = true;

    try {
      const payloadFromSteps = estPayloadData(data);

      const payload = {
        RequestInfo: {
          apiId: "Rainmaker",
          authToken: Digit.UserService.getUser()?.access_token,
          userInfo: Digit.UserService.getUser()?.info,
        },
        ...payloadFromSteps,
      };

      console.log("EST create payload ->", payload);

      mutation.mutate(payload, {
        onSuccess: (responseData) => {
          console.log("EST Allotment API Success:", responseData);
          if (onSuccess) onSuccess(responseData);
        },
        onError: (error) => {
          console.error("EST Allotment API Error:", error);
          if (error?.response?.data) {
            console.error("Backend error body:", error.response.data);
          }
        },
      });
    } catch (err) {
      console.error("EST payload build error:", err);
    }
  }, [data, mutation, onSuccess]);

  const handleDownloadPdf = async () => {
    try {
      const { Assets = [] } = mutation.data || {};
      const assetInfo = Assets[0] || {};
      const tenantInfo = tenants?.find(
        (tenant) => tenant.code === assetInfo.tenantId
      );
      // Remove this function call as it's not defined
      // const pdfData = await getESTAcknowledgementData(
      //   { ...assetInfo },
      //   tenantInfo,
      //   t
      // );
      // Digit.Utils.pdf.generate(pdfData);
      console.log("PDF download functionality to be implemented");
    } catch (error) {
      console.error("PDF generation error:", error);
    }
  };

  return (
    <Card>
      <BannerPicker
        data={mutation.data || { Assets: [{ estateNo: assetData?.estateNo }] }}
        isSuccess={mutation.isSuccess}
        isLoading={mutation.isLoading}
        isError={mutation.isError}
      />

      <StatusTable>
        <Row
          rowContainerStyle={rowContainerStyle}
          last
          textStyle={{ whiteSpace: "pre", width: "60%" }}
        />
      </StatusTable>

      {user?.type === "CITIZEN" ? (
        <Link to={`/upyog-ui/citizen`}>
          <LinkButton label={t("CORE_COMMON_GO_TO_HOME")} />
        </Link>
      ) : (
        <Link to={`/upyog-ui/employee`}>
          <LinkButton label={t("CORE_COMMON_GO_TO_HOME")} />
        </Link>
      )}
    </Card>
  );
};

export default ESTAcknowledgement;
