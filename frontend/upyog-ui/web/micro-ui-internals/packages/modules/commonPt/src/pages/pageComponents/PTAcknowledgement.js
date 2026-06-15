import {
  Banner,
  Card,
  CardText,
  Loader,
  Row,
  StatusTable,
  SubmitBar
} from "@nudmcdgnpm/digit-ui-react-components";
import React, { useEffect, useRef } from "react";
import { useTranslation } from "react-i18next";
import { useLocation } from "react-router-dom";
import {
  convertToPropertyLightWeight,
  convertToUpdatePropertyLightWeight
} from "../utils";

const GetActionMessage = ({ isSuccess, isLoading }) => {
  const { t } = useTranslation();
  if (isSuccess) {
    return !window.location.href.includes("edit-application")
      ? (window.location.href.includes("employee")
        ? t("CS_NEW_PROPERTY_APPLICATION_CREATED_SUCCESS")
        : t("CS_NEW_PROPERTY_APPLICATION_SUBMITTED_SUCCESS"))
      : t("CS_PROPERTY_UPDATE_APPLICATION_SUCCESS");
  } else if (isLoading) {
    return !window.location.href.includes("edit-application")
      ? t("CS_PROPERTY_APPLICATION_PENDING")
      : t("CS_PROPERTY_UPDATE_APPLICATION_PENDING");
  } else {
    return !window.location.href.includes("edit-application")
      ? t("CS_PROPERTY_APPLICATION_FAILED")
      : t("CS_PROPERTY_UPDATE_APPLICATION_FAILED");
  }
};

const rowContainerStyle = {
  padding: "4px 0px",
  justifyContent: "space-between"
};

const BannerPicker = (props) => (
  <Banner
    message={GetActionMessage(props)}
    applicationNumber={props.data?.Properties?.[0]?.acknowldgementNumber}
    info={props.isSuccess ? props.t("PT_APPLICATION_NO") : ""}
    successful={props.isSuccess}
  />
);

const PTAcknowledgement = ({ onSuccess, onSelect, formData, redirectUrl, userType }) => {
  const { t } = useTranslation();
  const location = useLocation();
  const stateId = Digit.ULBService.getStateId();
  const tenantId = Digit.ULBService.getCurrentTenantId();
  const navigate = Digit.Hooks.useCustomNavigate();
  const [localMutationSuccess, setLocalMutationSuccess] = React.useState(false);
  const [localResponseData, setLocalResponseData] = React.useState(null);

  // Create mutation with callbacks in config
  const mutation = Digit.Hooks.pt.usePropertyAPI(tenantId, true, {
    onSuccess: (responseData) => {
      setLocalMutationSuccess(true);
      setLocalResponseData(responseData); // Store response in state
      if (onSuccess) onSuccess(responseData);
    },
    onError: (err) => {
      setLocalMutationSuccess(false);
      setLocalResponseData(null);
    }
  });
  const calledRef = useRef(false);
  let data = location?.state?.data;
  if (onSelect) {
    data = formData?.cptNewProperty?.property;
  }

  let createNUpdate = false;
  let { data: mdmsConfig } = Digit.Hooks.pt.useMDMS(stateId, "PropertyTax", "PTWorkflow");
  (mdmsConfig?.PropertyTax?.PTWorkfow || []).forEach((item) => {
    if (item.enable && item.businessService.includes("WNS")) {
      createNUpdate = true;
    }
  });

  const mutationForUpdate = Digit.Hooks.pt.usePropertyAPI(
    data?.locationDet?.city?.code || data?.locationDet?.cityCode?.code || tenantId,
    false, // update
    {
      onSuccess: (responseData) => {
        if (onSuccess) onSuccess(responseData);
      },
      onError: (err) => {
      }
    }
  );

  // Run create mutation once on mount
  useEffect(() => {
    if (!calledRef.current && data && mutation.status === "idle") {
      calledRef.current = true; // prevent second call

      const tenant =
        userType === "employee"
          ? tenantId
          : data?.locationDet?.cityCode?.code || data?.locationDet?.city?.code;
      data.tenantId = tenant;

      const formdata = convertToPropertyLightWeight(data);
      formdata.Property.tenantId = formdata?.Property?.tenantId || tenant;
      if (mutation.isPending) return;

      // Use mutate - callbacks are now in hook config
      mutation.mutate(formdata);
    }
  }, [data, mutation.status, onSuccess, tenantId, userType]);

  // Optionally run update after create if workflow requires
  useEffect(() => {
    if (mutation.isSuccess && createNUpdate && mutationForUpdate.status === "idle") {
      const tenant =
        userType === "employee"
          ? tenantId
          : data?.locationDet?.city?.code || data?.locationDet?.cityCode?.code;
      data.tenantId = tenant;

      const formdata = convertToUpdatePropertyLightWeight(data);
      formdata.Property.tenantId = formdata?.Property?.tenantId || tenant;

      mutationForUpdate.mutate(formdata);
    }
  }, [mutation.isSuccess, createNUpdate, mutationForUpdate.status, data, tenantId, userType, onSuccess]);

  const onNext = () => {
    const data = localMutationSuccess ? localResponseData : mutation.data;
    if (onSelect && (mutation.isSuccess || localMutationSuccess) && data) {
      sessionStorage.setItem("Digit_OBPS_PT", JSON.stringify(data?.Properties[0]));
      sessionStorage.setItem("Digit_FSM_PT", JSON.stringify(data?.Properties[0]));
      onSelect("cpt", { details: data?.Properties[0] });
    }
  };

  // Use localResponseData when localMutationSuccess is true, otherwise use mutation.data
  const responseData = localMutationSuccess ? localResponseData : mutation.data;

  // Check local success FIRST before showing loader
  if ((mutation.isSuccess || localMutationSuccess) && responseData) {
    return (
      <Card>
        <BannerPicker t={t} data={responseData} isSuccess={true} isLoading={false} />
        <CardText>
          {window.location.href.includes("employee")
            ? t("CS_CREATE_PROPERTY_SUCCESS_EMP_RESPONSE")
            : t("CS_CREATE_PROPERTY_SUCCESS_CITIZEN_RESPONSE")}
        </CardText>
        <StatusTable>
          <Row
            rowContainerStyle={rowContainerStyle}
            last
            label={t("PT_COMMON_TABLE_COL_PT_ID")}
            text={responseData?.Properties[0]?.propertyId}
            textStyle={{ whiteSpace: "pre", width: "200%" }}
          />
        </StatusTable>
        {window.location.href.includes("/citizen/") &&
          (onSelect ? (
            <SubmitBar label={t("CS_COMMON_PROCEED")} onSubmit={onNext} />
          ) : (
            <SubmitBar
              label={t("CS_COMMON_PROCEED")}
              onSubmit={() => {
                if (redirectUrl) {
                  const prop = responseData?.Properties[0];
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
  }

  // Show loader only if not successful and still pending
  if (mutation.isIdle || mutation.isLoading || mutation.isPending) {
    return <Loader />;
  }

  if (mutation.isError) {
    return (
      <Card>
        <BannerPicker t={t} data={mutation.data} isSuccess={false} isLoading={false} />
        <CardText>{t("CS_FILE_PROPERTY_FAILED_RESPONSE")}</CardText>
      </Card>
    );
  }

  return null;
};

export default PTAcknowledgement;
