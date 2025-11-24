
import React, { useState } from "react";
import { Card, CardHeader, CardSubHeader, StatusTable, Row, LinkButton, SubmitBar, CheckBox, EditIcon } from "@upyog/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import { useHistory } from "react-router-dom";
import { checkForNA } from "../../../utils";
import { APPLICATION_PATH } from "../../../../../est/src/utils";

const ActionButton = ({ jumpTo }) => {
  const { t } = useTranslation();
  const history = useHistory();
  function routeTo() {
    history.push(jumpTo);
  }
  return (
    <LinkButton
      label={<EditIcon style={{ marginTop: "-30px", float: "right", position: "relative", bottom: "32px" }} />}
      className="check-page-link-button"
      onClick={routeTo}
    />
  );
};
const ESTRegCheckPage =  ({ onSubmit, editdata, value = {} }) => {
   const { t } = useTranslation();
   const [agree, setAgree] = useState(false);
  
   const Assetdata = value?.Assetdata?.Assetdata || value?.Assetdata || {};
 

  return (
    <Card>
      <CardHeader>{t("EST_REGISTRATION_SUMMARY")}</CardHeader>

      <CardSubHeader>{t("EST_ASSET_DETAILS")}</CardSubHeader>
      <StatusTable>
        {/* <Row label={t("ASSETS_NO")} text={Assetdata.estateNo || t("NA")} */}
         
        <Row label={t("BUILDING_NAME")} text={Assetdata.buildingName || t("NA")}
        actionButton={<ActionButton jumpTo={`/upyog-ui/employee/est/create/newRegistration`}/>} />
        <Row label={t("BUILDING_NUMBER")} text={Assetdata.buildingNo || t("NA")} />
        <Row label={t("BUILDING_FLOOR")} text={Assetdata.buildingFloor || t("NA")} />
        <Row label={t("BUILDING_BLOCK")} text={Assetdata.buildingBlock || t("NA")} />
        <Row label={t("LOCALITY")} text={Assetdata.serviceType || t("NA")} />
        <Row label={t("TOTAL_PLOT_AREA")} text={Assetdata.totalFloorArea || t("NA")} />
        <Row label={t("DIMENSION")} text={`${Assetdata.dimensionLength || t("NA")} X ${Assetdata.dimensionWidth || t("NA")}`} />
        <Row label={t("RATE")} text={Assetdata.rate || t("NA")} />
        <Row label={t("ASSET_REFERENCE_NUMBER")} text={Assetdata.assetRef || t("NA")} />
        <Row label={t("ASSET_TYPE")} text={Assetdata.assetType || t("NA")} />
      </StatusTable> 

      <CheckBox label={t("EST_FINAL_DECLARATION_MESSAGE")} onChange={() => setAgree(!agree)} />

      <SubmitBar label={t("EST_COMMON_SUBMIT")} onSubmit={onSubmit} disabled={!agree} />
    </Card>
  );
};

export default ESTRegCheckPage;