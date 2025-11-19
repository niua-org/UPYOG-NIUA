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
import { checkForNA } from "../../../utils";
import { APPLICATION_PATH } from "../../../../../est/src/utils";

const ActionButton = ({ jumpTo }) => {
  const history = useHistory();
  return <LinkButton label={<EditIcon />} onClick={() => history.push(jumpTo)} />;
};
const ESTAssignAssetsCheckPage =  ({ onSubmit, value = {} }) => {
   const { t } = useTranslation();
  const [agree, setAgree] = useState(false);
 const AssignAssetsData = value?.AssignAssetsData?.AllotmentData || {};
 console.log("✅ AssignAssets CheckPage AssignAssetsData:", AssignAssetsData);
 console.log("✅ Full asset data:", value?.assetData);
console.log("✅ Available fields:", Object.keys(value?.assetData || {}));


  const handleFileOpen = (fileId) => {
  if (!fileId) return;

  Digit.UploadServices.Filefetch([fileId], Digit.ULBService.getStateId())
    .then((res) => {
      const concatenatedUrls = res?.data?.fileStoreIds?.[0]?.url;
      if (concatenatedUrls) {
        const urlArray = concatenatedUrls.split(",");
        const fileUrl = urlArray[0];
        if (fileUrl) {
          window.open(fileUrl, "_blank"); // open in new tab
        } else {
          console.error("No valid URL found!");
        }
      } else {
        console.error("No URL in response!");
      }
    })
    .catch((err) => console.error("Error fetching file:", err));
};


  return (
    <Card>
      <CardHeader>{t("EST_ASSIGN_ASSETS_SUMMARY")}</CardHeader>

      {/* ----------------- ASSET DETAILS ----------------- */}
      <CardSubHeader>{t("EST_ASSET_DETAILS")}</CardSubHeader>
      <StatusTable>
        <Row
          label={t("EST_ASSET_NUMBER")}
          text={checkForNA(value?.assetData?.estateNo)}
          actionButton={<ActionButton jumpTo={`/upyog-ui/employee/est/assignassets/assign-assets`} />}
        />
       <Row label={t("EST_BUILDING_NAME")} text={checkForNA(value?.assetData?.buildingName)} />
<Row label={t("EST_LOCALITY")} text={checkForNA(value?.assetData?.locality)} />
<Row label={t("EST_TOTAL_AREA")} text={checkForNA(value?.assetData?.totalFloorArea)} />
<Row label={t("EST_FLOOR")} text={checkForNA(value?.assetData?.floorNo)} />
<Row label={t("EST_RATE")} text={checkForNA(value?.assetData?.rate)} />
      </StatusTable>

      {/* ----------------- PERSONAL DETAILS ----------------- */}
      <CardSubHeader>{t("EST_PERSONAL_DETAILS_OF_ALLOTTEE")}</CardSubHeader>
      <StatusTable>
        <Row label={t("EST_PROPERTY_TYPE")} text={checkForNA(AssignAssetsData?.propertyType)} />
        <Row label={t("EST_ALLOTTEE_NAME")} text={checkForNA(AssignAssetsData?.allotteeName)} />
        <Row label={t("EST_PHONE_NUMBER")} text={checkForNA(AssignAssetsData?.phoneNumber)} />
        <Row label={t("EST_ALTERNATE_PHONE_NUMBER")} text={checkForNA(AssignAssetsData?.altPhoneNumber)} />
        <Row label={t("EST_EMAIL_ID")} text={checkForNA(AssignAssetsData?.email)} />
      </StatusTable>

      {/* ----------------- AGREEMENT DETAILS ----------------- */}
      <CardSubHeader>{t("EST_AGREEMENT_DETAILS")}</CardSubHeader>
      <StatusTable>
        <Row label={t("EST_AGREEMENT_START_DATE")} text={checkForNA(AssignAssetsData?.startDate)} />
        <Row label={t("EST_AGREEMENT_END_DATE")} text={checkForNA(AssignAssetsData?.endDate)} />
        <Row label={t("EST_DURATION_IN_YEARS")} text={checkForNA(AssignAssetsData?.duration)} />
        <Row label={t("EST_RATE_PER_SQFT")} text={checkForNA(AssignAssetsData?.rate)} />
        <Row label={t("EST_MONTHLY_RENT_IN_INR")} text={checkForNA(AssignAssetsData?.monthlyRent)} />
        <Row label={t("EST_ADVANCE_PAYMENT_IN_INR")} text={checkForNA(AssignAssetsData?.advancePayment)} />
        <Row label={t("EST_ADVANCE_PAYMENT_DATE")} text={checkForNA(AssignAssetsData?.advancePaymentDate)} />
      </StatusTable>

      {/* ----------------- DOCUMENT UPLOAD ----------------- */}
      <CardSubHeader>{t("EST_DOCUMENT_UPLOAD")}</CardSubHeader>
      <StatusTable>
  <Row
    label={t("EST_EOFFICE_FILE_NO")}
    text={checkForNA(AssignAssetsData?.eOfficeFileNo)}
  />

  {/* CITIZEN REQUEST LETTER */}
  <Row
    label={t("EST_CITIZEN_REQUEST_LETTER")}
    text={
      AssignAssetsData?.citizenLetter?.filestoreId ? (
        <span
          onClick={() =>
            handleFileOpen(AssignAssetsData?.citizenLetter?.filestoreId)
          }
          style={{
            color: "blue",
            textDecoration: "underline",
            cursor: "pointer",
          }}
        >
          {t("Click to View File")}
        </span>
      ) : (
        t("CS_FILE_NOT_UPLOADED")
      )
    }
  />

  {/* ALLOTMENT LETTER */}
  <Row
    label={t("EST_ALLOTMENT_LETTER")}
    text={
      AssignAssetsData?.allotmentLetter?.filestoreId ? (
        <span
          onClick={() =>
            handleFileOpen(AssignAssetsData?.allotmentLetter?.filestoreId)
          }
          style={{
            color: "blue",
            textDecoration: "underline",
            cursor: "pointer",
          }}
        >
          {t("Click to View File")}
        </span>
      ) : (
        t("CS_FILE_NOT_UPLOADED")
      )
    }
  />

  {/* SIGNED DEED */}
  <Row
    label={t("EST_SIGNED_DEED")}
    text={
      AssignAssetsData?.signedDeed?.filestoreId ? (
        <span
          onClick={() =>
            handleFileOpen(AssignAssetsData?.signedDeed?.filestoreId)
          }
          style={{
            color: "blue",
            textDecoration: "underline",
            cursor: "pointer",
          }}
        >
          {t("Click to View File")}
        </span>
      ) : (
        t("CS_FILE_NOT_UPLOADED")
      )
    }
  />
</StatusTable>


      {/* ----------------- DECLARATION + SUBMIT ----------------- */}
      <CheckBox
        label={t("EST_FINAL_DECLARATION_MESSAGE")}
        onChange={() => setAgree(!agree)}
        value={agree}
      />
      <SubmitBar label={t("EST_COMMON_SUBMIT")} onSubmit={onSubmit} disabled={!agree} />
    </Card>
  );
};

export default ESTAssignAssetsCheckPage;