import React, { useState } from "react";
import {
  Card,
  CardHeader,
  CardSubHeader,
  StatusTable,
  Row,
  LinkButton,
  SubmitBar,
  CheckBox,
  EditIcon,
} from "@upyog/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import { useHistory } from "react-router-dom";

const ActionButton = ({ jumpTo }) => {
  const history = useHistory();
  return (
    <LinkButton
      label={
        <EditIcon
          style={{
            marginTop: "-30px",
            float: "right",
            position: "relative",
            bottom: "32px",
          }}
        />
      }
      className="check-page-link-button"
      onClick={() => history.push(jumpTo)}
    />
  );
};

// very small, predictable helper
const getLocalityText = (asset, t) => {
  if (!asset) return t("NA");

  const candidates = [
    asset.locality,       // NewRegistration: label e.g. "Main Road Abadpura"
    asset.localityName,   // if we stored name separately
    asset.serviceType,    // earlier we used this as locality code
  ];

  const raw = candidates.find(
    (v) => v !== undefined && v !== null && v !== "" && v !== "Nill"
  );

  // if it's clearly an i18n key, translate; else show as plain text
  if (
    typeof raw === "string" &&
    (raw.startsWith("TENANT_") || raw.startsWith("EST_"))
  ) {
    return t(raw);
  }

  return raw || t("NA");
};

const ESTRegCheckPage = ({ onSubmit, value = {} }) => {
  const { t } = useTranslation();
  const [agree, setAgree] = useState(false);

  const Assetdata = value?.Assetdata?.Assetdata || value?.Assetdata || {};

  return (
    <Card>
      <CardHeader>{t("EST_REGISTRATION_SUMMARY")}</CardHeader>

      <CardSubHeader>{t("EST_ASSET_DETAILS")}</CardSubHeader>
      <StatusTable>
        <Row
          label={t("BUILDING_NAME")}
          text={Assetdata.buildingName || t("NA")}
          actionButton={
            <ActionButton jumpTo={`/upyog-ui/employee/est/create/newRegistration`} />
          }
        />

        <Row label={t("BUILDING_NUMBER")} text={Assetdata.buildingNo || t("NA")} />

        <Row label={t("BUILDING_FLOOR")} text={Assetdata.buildingFloor || t("NA")} />

        <Row label={t("BUILDING_BLOCK")} text={Assetdata.buildingBlock || t("NA")} />

        {/* Locality */}
        <Row label={t("LOCALITY")} text={getLocalityText(Assetdata, t)} />

        <Row label={t("TOTAL_PLOT_AREA")} text={Assetdata.totalFloorArea || t("NA")} />

        <Row
          label={t("DIMENSION")}
          text={`${Assetdata.dimensionLength || t("NA")} X ${
            Assetdata.dimensionWidth || t("NA")
          }`}
        />

        <Row label={t("RATE")} text={Assetdata.rate || t("NA")} />

        <Row
          label={t("ASSET_REFERENCE_NUMBER")}
          text={Assetdata.assetRef || t("NA")}
        />

        <Row label={t("ASSET_TYPE")} text={Assetdata.assetType || t("NA")} />
      </StatusTable>

      <CheckBox
        label={t("EST_FINAL_DECLARATION_MESSAGE")}
        onChange={() => setAgree(!agree)}
      />

      <SubmitBar
        label={t("EST_COMMON_SUBMIT")}
        onSubmit={onSubmit}
        disabled={!agree}
      />
    </Card>
  );
};

export default ESTRegCheckPage;