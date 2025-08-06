import { Banner, Card, LinkButton, Loader, Row, StatusTable, SubmitBar } from "@upyog/digit-ui-react-components";
import React, { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { Link, useRouteMatch } from "react-router-dom";
import getPetAcknowledgementData from "../../../getPetAcknowledgementData";
import { PetDataConvert } from "../../../utils";

const GetActionMessage = (props) => {
  const { t } = useTranslation();
  if (props.isSuccess) {
    return !window.location.href.includes("revised-application") ? t("ES_PTR_RESPONSE_CREATE_ACTION") : t("PTR_REVISED_SUCCESSFULLY");
  } else if (props.isLoading) {
    return  t("CS_PTR_APPLICATION_PENDING");
  } else if (!props.isSuccess) {
    return t("CS_PTR_APPLICATION_FAILED");
  }
};

const rowContainerStyle = {
  padding: "4px 0px",
  justifyContent: "space-between",
};

const BannerPicker = (props) => {
  return (
    <Banner
      message={GetActionMessage(props)}
      applicationNumber={props.data?.PetRegistrationApplications[0].applicationNumber}
      info={props.isSuccess ? props.t("PTR_APPLICATION_NO") : ""}
      successful={props.isSuccess}
      style={{width: "100%"}}
    />
  );
};

const PTRAcknowledgement = ({ data, onSuccess }) => {
  const { t } = useTranslation();
  const tenantId = Digit.ULBService.getCitizenCurrentTenant(true);
  const user = Digit.UserService.getUser().info;
  const mutation = Digit.Hooks.ptr.usePTRCreateAPI(data.address?.city?.code); 
  const { data: storeData } = Digit.Hooks.useStore.getInitData();
  const { tenants } = storeData || {};

  useEffect(() => {
    try {
      data.tenantId = tenantId;
      let formdata = PetDataConvert(data)
      mutation.mutate(formdata, {onSuccess});
    } catch (err) {
    }
  }, []);

  

  const handleDownloadPdf = async () => {
    const { PetRegistrationApplications = [] } = mutation.data;
    let Pet = (PetRegistrationApplications && PetRegistrationApplications[0]) || {};
    const tenantInfo = tenants.find((tenant) => tenant.code === Pet.tenantId);
    let tenantId = Pet.tenantId || tenantId;
    const data = await getPetAcknowledgementData({ ...Pet }, tenantInfo, t);
    Digit.Utils.pdf.generate(data);
  };

  return mutation.isLoading || mutation.isIdle ? (
    <Loader />
  ) : (
    <Card>
      <BannerPicker t={t} data={mutation.data} isSuccess={mutation.isSuccess} isLoading={mutation.isIdle || mutation.isLoading} />
      <StatusTable>
        {mutation.isSuccess && (
          <Row
            rowContainerStyle={rowContainerStyle}
            last       
            textStyle={{ whiteSpace: "pre", width: "60%" }}
          />
        )}
      </StatusTable>
      {mutation.isSuccess && <SubmitBar label={t("PTR_PET_DOWNLOAD_ACK_FORM")} onSubmit={handleDownloadPdf} />}
      {user?.type==="CITIZEN"?
      <Link to={`/upyog-ui/citizen`}>
        <LinkButton label={t("CORE_COMMON_GO_TO_HOME")} />
      </Link>
      :
      <Link to={`/upyog-ui/employee`}>
        <LinkButton label={t("CORE_COMMON_GO_TO_HOME")} />
      </Link>}
    </Card>
  );
};

export default PTRAcknowledgement;