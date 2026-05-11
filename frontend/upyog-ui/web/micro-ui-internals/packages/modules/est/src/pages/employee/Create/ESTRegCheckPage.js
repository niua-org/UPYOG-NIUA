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
} from "@nudmcdgnpm/digit-ui-react-components";
import { useTranslation } from "react-i18next";

/**
 * ActionButton
 * -------------
 * Displays an edit icon aligned to the right.
 * On click, it navigates the user to the provided route.
 */

const ActionButton = ({ jumpTo }) => {
  const navigate = Digit.Hooks.useCustomNavigate();
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

/**
 * getLocalityText
 * ----------------
 * Safely resolves the locality value from asset data.
 * - Tries multiple possible fields
 * - Ignores empty, null, or invalid values like "Nill"
 * - Translates the value if it looks like an i18n key
 */
const getLocalityText = (asset, t) => {
  if (!asset) return t("NA");

  const candidates = [
    asset.locality,       // NewRegistration: label e.g. "Main Road Abadpura"
    asset.localityName,   // if we stored name separately
    asset.serviceType,    // earlier we used this as locality code
  ];

  // Pick the first valid value

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

/**
 * ESTRegCheckPage
 * ----------------
 * Final review page for EST asset registration.
 * Displays all entered asset details in read-only mode.
 * Submission is allowed only after user agrees to declaration.
 */

const ESTRegCheckPage = ({ onSubmit, value = {} }) => {
  const { t } = useTranslation();
    // Tracks whether the user has accepted the declaration
  const [agree, setAgree] = useState(false);

   // Normalize asset data structure (supports multiple shapes)

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
