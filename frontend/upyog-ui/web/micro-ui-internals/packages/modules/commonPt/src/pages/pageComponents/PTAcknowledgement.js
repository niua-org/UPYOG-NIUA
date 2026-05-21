import { Banner, Card, CardText, LinkButton, Loader, Row, StatusTable, SubmitBar } from "@nudmcdgnpm/digit-ui-react-components";
import React, { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { Link, useLocation,  } from "react-router-dom";
// import getPTAcknowledgementData from "../../../getPTAcknowledgementData";
import { convertToPropertyLightWeight, convertToUpdatePropertyLightWeight } from "../utils";

const GetActionMessage = (props) => {
  const { t } = useTranslation();
  if (props.isSuccess) {
    return !window.location.href.includes("edit-application") ? (window.location.href.includes("employee") ?  t("CS_NEW_PROPERTY_APPLICATION_CREATED_SUCCESS") : t("CS_NEW_PROPERTY_APPLICATION_SUBMITTED_SUCCESS")) : t("CS_PROPERTY_UPDATE_APPLICATION_SUCCESS");
  } else if (props.isLoading) {
    return !window.location.href.includes("edit-application") ? t("CS_PROPERTY_APPLICATION_PENDING") : t("CS_PROPERTY_UPDATE_APPLICATION_PENDING");
  } else if (!props.isSuccess) {
    return !window.location.href.includes("edit-application") ? t("CS_PROPERTY_APPLICATION_FAILED") : t("CS_PROPERTY_UPDATE_APPLICATION_FAILED");
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
      applicationNumber={props.data?.Properties?.[0]?.acknowldgementNumber}
      info={props.isSuccess ? props.t("PT_APPLICATION_NO") : ""}
      successful={props.isSuccess}
    />
  );
};

const PTAcknowledgement = ({ onSuccess, onSelect, formData, redirectUrl, userType }) => {
  const { t } = useTranslation();
  const location = useLocation();
  const stateId = Digit.ULBService.getStateId();
  const tenantId = Digit.ULBService.getCurrentTenantId();
  const navigate = Digit.Hooks.useCustomNavigate();

  let data = location?.state?.data;
  if (onSelect) {
    data = formData?.cptNewProperty?.property;
  }

  let createNUpdate = false;
  let { data: mdmsConfig, isLoading: isMdmsLoading } = Digit.Hooks.pt.useMDMS(stateId, "PropertyTax", "PTWorkflow");
  (mdmsConfig?.PropertyTax?.PTWorkfow || []).forEach((item) => {
    if (item.enable) {
      if (item.businessService.includes("WNS")) {
        createNUpdate = true;
      }
    }
  });

  const mutation = Digit.Hooks.pt.usePropertyAPI(
    onSelect ? (data?.locationDet?.city?.code || data?.locationDet?.cityCode?.code || tenantId) : tenantId,
    true // create
  );

  const mutationForUpdate = Digit.Hooks.pt.usePropertyAPI(
    onSelect ? (data?.locationDet?.city?.code || data?.locationDet?.cityCode?.code || tenantId) : tenantId,
    false // update
  );

  const { data: storeData } = Digit.Hooks.useStore.getInitData();
  const { tenants } = storeData || {};

  useEffect(() => {
    if (onSelect && (!data || formData?.isSkip || formData?.cptNewProperty?.isSkip)) {
      onSelect("cpt", { details: null });
    }
  }, [onSelect, data, formData]);

  useEffect(() => {
    if (onSelect && data && mutation.status === "idle") {
      try {
        let tenant = userType === "employee" ? tenantId : (data?.locationDet?.cityCode?.code || data?.locationDet?.city?.code);
        data.tenantId = tenant;

        let formdata = convertToPropertyLightWeight(data);
        formdata.Property.tenantId = formdata?.Property?.tenantId || tenant;

        mutation.mutate(formdata, {
          onSuccess,
        });
      } catch (err) {}
    }
  }, [onSelect, data, mutation.status]);

  useEffect(() => {
    if (onSelect && mutation.isSuccess && createNUpdate && mutationForUpdate.status === "idle") {
      try {
        let tenant = userType === "employee" ? tenantId : (data?.locationDet?.city?.code || data?.locationDet?.cityCode?.code);
        data.tenantId = tenant;

        let formdata = convertToUpdatePropertyLightWeight(data);
        formdata.Property.tenantId = formdata?.Property?.tenantId || tenant;

        mutationForUpdate.mutate(formdata, {
          onSuccess,
        });
      } catch (er) {}
    }
  }, [onSelect, mutation.isSuccess, createNUpdate, mutationForUpdate.status, data]);

  const isSuccess = onSelect 
    ? (createNUpdate ? mutationForUpdate.isSuccess : mutation.isSuccess) 
    : location?.state?.isSuccess;
  const isError = onSelect 
    ? (mutation.isError || mutationForUpdate.isError) 
    : (location?.state?.isSuccess === false);
  const responseData = onSelect 
    ? (createNUpdate ? mutationForUpdate.data : mutation.data) 
    : location?.state?.data;
  const isPending = onSelect ? (!isSuccess && !isError) : false;

  useEffect(() => {
    if (!onSelect && isSuccess) {
      const timer = setTimeout(() => {
        if (redirectUrl) {
          const prop = responseData?.Properties?.[0];
          navigate(
            `${redirectUrl}?propertyId=${prop?.propertyId}&tenantId=${prop?.tenantId}`,
            { ...location?.state?.prevState }
          );
          const scrollConst = redirectUrl?.includes("employee/tl") ? 1600 : 300;
          setTimeout(() => window.scrollTo(0, scrollConst), 400);
        }
      }, 3000);
      return () => clearTimeout(timer);
    }
  }, [onSelect, isSuccess, redirectUrl]);

  const onNext = () => {
    if (onSelect && isSuccess) {
      sessionStorage.setItem("Digit_OBPS_PT", JSON.stringify(responseData?.Properties?.[0]));
      sessionStorage.setItem("Digit_FSM_PT", JSON.stringify(responseData?.Properties?.[0]));
      onSelect("cpt", { details: responseData?.Properties?.[0] });
    }
  };

  if (isPending || isMdmsLoading) {
    return <Loader />;
  }

  return (
    <Card>
      <BannerPicker t={t} data={responseData} isSuccess={isSuccess} isLoading={isPending} />
      {isSuccess && <CardText>{window.location.href.includes("employee") ? t("CS_CREATE_PROPERTY_SUCCESS_EMP_RESPONSE") : t("CS_CREATE_PROPERTY_SUCCESS_CITIZEN_RESPONSE")}</CardText>}
      {!isSuccess && <CardText>{t("CS_FILE_PROPERTY_FAILED_RESPONSE")}</CardText>}

      <StatusTable>
        {isSuccess && (
          <Row
            rowContainerStyle={rowContainerStyle}
            last
            label={t("PT_COMMON_TABLE_COL_PT_ID")}
            text={responseData?.Properties?.[0]?.propertyId}
            textStyle={{ whiteSpace: "pre", width: "200%" }}
          />
        )}
      </StatusTable>
      
      {isSuccess &&
        window.location.href.includes("/citizen/") &&
        (onSelect ? (
          <SubmitBar label={t("CS_COMMON_PROCEED")} onSubmit={onNext} />
        ) : (
          <SubmitBar
            label={t("CS_COMMON_PROCEED")}
            onSubmit={() => {
              if (redirectUrl) {
                const prop = responseData?.Properties?.[0];
                navigate(
                  `${redirectUrl}?propertyId=${prop?.propertyId}&tenantId=${prop?.tenantId}`,
                  { ...location?.state?.prevState }
                );
              }
            }}
          />
        ))}
    </Card>
  );
};

export default PTAcknowledgement;
