import { Card, KeyNote, SubmitBar } from "@upyog/digit-ui-react-components";
import React from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";

const EwasteApplication = ({ application, tenantId, buttonLabel }) => {
  
  
  const { t } = useTranslation();
  return (
    <Card>
      <KeyNote keyValue={t("EWASTE_APPLICATION_REQUEST_ID")} note={application?.requestId} />
      <KeyNote keyValue={t("EWASTE_APPLICANT_NAME")} note={application?.applicant?.applicantName} />
      <KeyNote keyValue={t("EWASTE_APPLICATION_STATUS")} note={application?.requestStatus} />
      {/* <KeyNote keyValue={t("EWASTE_SEARCH_TYPE")} note={application?.petDetails?.petType} /> */}
      {/* {console.log("ewaste application ::", application)} */}
      {/* <KeyNote keyValue={t("PT_COMMON_TABLE_COL_STATUS_LABEL")} note={t(`EWASTE_COMMON_${application?.status}`)} /> */}

      {/* <Link to={`/digit-ui/citizen/ew/application/${application?.applicationNumber}/${application?.tenantId}`}> */}
      <Link to={`/digit-ui/citizen/ew/application/${application?.requestId}/${application?.tenantId}`}>
        <SubmitBar label={buttonLabel} />
      </Link>
    </Card>
  );
};

export default EwasteApplication;
