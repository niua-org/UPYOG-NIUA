/**
 * =============================================================================
 * CitizenChallanDetails Component - Challan Generation Module
 * =============================================================================
 * Purpose:
 *   Displays a comprehensive, read-only summary of a specific Challan for citizens.
 *   Provides clear violation details, offender information, incident location,
 *   supporting evidence documents, workflow audit history, and direct payment actions.
 *
 * Key Capabilities:
 *   1. Challan Summary       : Displays Challan Number, Status, Amount, Issue Date, Due Date.
 *   2. Offender Details      : Shows name, primary mobile number, email, and alternate contact.
 *   3. Location / Address    : Formats address lines, landmark, locality, and city.
 *   4. Offence Information   : Details violation type, category, vehicle number, GPS coordinates, waiver.
 *   5. Document Viewer       : Previews uploaded photo/document proofs via CHBDocument.
 *   6. Workflow Timeline     : Tracks real-time lifecycle events (creation, notices, payment status).
 *   7. Action Bar Integration: Contextual action bar:
 *                              - If unpaid (ACTIVE): Prompts citizen to "Pay Now".
 *                              - If paid (PAID)    : Allows citizen to "Download Receipt".
 * =============================================================================
 */

import {
  Card,
  CardSubHeader,
  CardSectionHeader,
  Header,
  Loader,
  Row,
  StatusTable,
  SubmitBar,
  ActionBar,
  Modal,
  Toast,
  TextArea,
  CardText,
  CloseSvg,
  MultiLink,
} from "@nudmcdgnpm/digit-ui-react-components";
import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useParams, useLocation } from "react-router-dom";
import { downloadAndPrintChallan, printReciept, convertEpochToDate } from "../../../utils";
import CHBDocument from "../../../components/ChallanDocument";
import NewApplicationTimeline from "../../../../../templates/ApplicationDetails/components/NewApplicationTimeline";

/**
 * CitizenChallanDetails Component
 *
 * Displays detailed information about a specific Challan for citizens,
 * including applicant/offender details, location, offence specifications,
 * documents, timeline, and payment actions.
 * Structured to match the GCApplicationDetails layout.
 */
const getChallanAmount = (challan) => {
  if (challan?.totalAmount !== undefined && challan?.totalAmount !== null && typeof challan?.totalAmount === "number") {
    return challan.totalAmount;
  }
  if (challan?.totalDue !== undefined && challan?.totalDue !== null) {
    return challan.totalDue;
  }
  if (challan?.calculation?.totalAmount !== undefined && challan?.calculation?.totalAmount !== null) {
    return challan.calculation.totalAmount;
  }
  if (Array.isArray(challan?.amount)) {
    return challan.amount.reduce((acc, curr) => acc + (Number(curr?.amount) || 0), 0);
  }
  if (typeof challan?.amount === "object" && challan?.amount !== null) {
    return challan.amount.amount || 0;
  }
  if (challan?.amount !== undefined && challan?.amount !== null && typeof challan?.amount === "number") {
    return challan.amount;
  }
  return 0;
};

const getChallanAddress = (challan, defaultText = "N/A") => {
  const addr = challan?.address;
  if (!addr) {
    return challan?.additionalDetail?.location || challan?.additionalDetail?.address || defaultText;
  }
  if (typeof addr === "string") return addr;

  const parts = [
    addr?.doorNo,
    addr?.buildingName,
    addr?.addressLine1,
    addr?.addressLine2,
    addr?.street,
    addr?.landmark,
    addr?.locality?.name || addr?.locality?.code,
    addr?.city,
    addr?.pincode,
    addr?.detail,
    addr?.address,
  ].filter(Boolean);

  if (parts.length > 0) {
    return [...new Set(parts)].join(", ");
  }

  return challan?.additionalDetail?.location || challan?.additionalDetail?.address || defaultText;
};

const CitizenChallanDetails = () => {
  const { t } = useTranslation();
  const navigate = Digit.Hooks.useCustomNavigate();
  const params = useParams();
  const location = useLocation();

  // Extract challan number from URL params
  const challanNo = decodeURIComponent(params.challanNo || params.applicationNo || location.pathname.split("/").pop());
  const tenantId = Digit.ULBService.getCitizenCurrentTenant(true) || Digit.ULBService.getCurrentTenantId();

  const [showOptions, setShowOptions] = useState(false);
  const [challan, setChallan] = useState(location?.state?.challan || null);
  const [loading, setLoading] = useState(!location?.state?.challan);
  const [showToast, setShowToast] = useState(null);

  useEffect(() => {
    if (showToast) {
      const timer = setTimeout(() => setShowToast(null), 10000);
      return () => clearTimeout(timer);
    }
  }, [showToast]);

  const { data: storeData } = Digit.Hooks.useStore.getInitData();
  const { tenants } = storeData || {};

  // Fetch challan details if not passed from previous screen
  useEffect(() => {
    if (!challan && challanNo) {
      const fetchDetails = async () => {
        setLoading(true);
        try {
          const response = await Digit.ChallanGenerationService.search({
            tenantId,
            filters: { challanNo },
          });
          if (response?.challans?.length > 0) {
            setChallan(response.challans[0]);
          } else {
            setShowToast({ key: "error", label: t("CHALLAN_NOT_FOUND") });
          }
        } catch (err) {
          setShowToast({ key: "error", label: t("ERR_LOAD_CHALLAN_FAILED") });
        } finally {
          setLoading(false);
        }
      };
      fetchDetails();
    }
  }, [challanNo]);

  const businessService = challan?.businessService || "Challan_Generation";

  const { data: reciept_data, isLoading: recieptDataLoading } = Digit.Hooks.useRecieptSearch(
    { tenantId, businessService: "Challan_Generation", consumerCodes: challanNo },
    { enabled: !!challanNo }
  );

  const { isLoading: isWorkflowLoading, data: workflowDetails } = Digit.Hooks.useWorkflowDetails({
    tenantId: tenantId,
    id: challanNo,
    moduleCode: "challan-generation",
    config: { staleTime: 0 },
  });

  const isPaid =
    challan?.applicationStatus === "PAID" ||
    challan?.status === "PAID" ||
    (reciept_data?.Payments && reciept_data?.Payments?.length > 0);

  const status = isPaid ? "PAID" : challan?.applicationStatus || challan?.status || "ACTIVE";
  const amount = getChallanAmount(challan);

  const handleMakePayment = () => {
    navigate(
      `/upyog-ui/citizen/payment/my-bills/${businessService}/${challanNo}?workflow=challangeneration&tenantId=${tenantId}`,
      {
        state: {
          tenantId: tenantId,
          fromSearchResults: true,
        },
      }
    );
  };

  const downloadOptions = [];
  downloadOptions.push({
    label: t("UC_CHALLAN") || t("CHALLAN_DOWNLOAD_NOTICE"),
    onClick: () => downloadAndPrintChallan(challanNo, "download"),
  });

  if (isPaid && !recieptDataLoading) {
    downloadOptions.push({
      label: t("UC_RECEIPT") || t("CHALLAN_FEE_RECEIPT"),
      onClick: () => printReciept(businessService, challanNo),
    });
  }

  if (loading || isWorkflowLoading) {
    return <Loader />;
  }

  if (!challan) {
    return (
      <Card>
        <CardText>{t("CHALLAN_NOT_FOUND")}</CardText>
      </Card>
    );
  }

  // ---------- Additional Details ----------
  let additionalDetails = challan?.additionalDetail || challan?.additionalDetails || {};
  if (typeof additionalDetails === "string") {
    try {
      additionalDetails = JSON.parse(additionalDetails);
    } catch (e) {
      additionalDetails = {};
    }
  }

  // ---------- Applicant / Offender Details ----------
  const offenderName =
    challan?.citizen?.name ||
    additionalDetails?.ownerName ||
    additionalDetails?.applicantName ||
    additionalDetails?.name ||
    t("CS_NA");

  const mobileNumber =
    challan?.citizen?.mobileNumber ||
    additionalDetails?.mobileNumber ||
    t("CS_NA");

  const emailId =
    challan?.citizen?.emailId ||
    additionalDetails?.emailId ||
    additionalDetails?.email ||
    null;

  const altMobileNumber =
    additionalDetails?.altMobileNumber ||
    additionalDetails?.alternateNumber ||
    additionalDetails?.altMobileNo ||
    null;

  // ---------- Address ----------
  const address = challan?.address || {};
  const doorNo = address?.doorNo || address?.houseNo || additionalDetails?.doorNo;
  const buildingName = address?.buildingName || address?.houseName || additionalDetails?.buildingName;
  const street = address?.street || address?.streetName || additionalDetails?.street;
  const localityRaw = address?.locality || additionalDetails?.locality;
  const localityText =
    typeof localityRaw === "string"
      ? t(localityRaw)
      : localityRaw?.name
      ? t(localityRaw.name)
      : localityRaw?.code
      ? t(localityRaw.code)
      : null;
  const landmark = address?.landmark || additionalDetails?.landmark;
  const city = address?.city || additionalDetails?.city || challan?.tenantId;
  const pincode = address?.pincode || additionalDetails?.pincode;
  const fullAddress = getChallanAddress(challan, t("CS_NA"));

  // ---------- Offence Details ----------
  const offenceDetails = additionalDetails?.offenceDetails || {};
  const offenceType =
    challan?.offenceTypeName ||
    offenceDetails?.offenceType ||
    additionalDetails?.offenceType ||
    t("CS_NA");
  const offenceCategory =
    challan?.offenceCategoryName ||
    offenceDetails?.category ||
    additionalDetails?.offenceCategory ||
    null;
  const vehicleNumber =
    offenceDetails?.vehicleNumber ||
    additionalDetails?.vehicleNumber ||
    null;
  const description =
    challan?.description ||
    offenceDetails?.description ||
    additionalDetails?.description ||
    t("CS_NA");
  const latitude = additionalDetails?.latitude || offenceDetails?.latitude;
  const longitude = additionalDetails?.longitude || offenceDetails?.longitude;

  // ---------- Documents ----------
  const docs = challan?.documents || additionalDetails?.documents || [];

  return (
    <React.Fragment>
      <div>
        <Card>
          {/* Challan Summary */}
          <CardSubHeader style={{ fontSize: "24px" }}>{t("CHALLAN_SUMMARY")}</CardSubHeader>
          <StatusTable>
            <Row className="border-none" label={t("CHALLAN_CHALLAN_NO")} text={challanNo || t("CS_NA")} />
            <Row
              className="border-none"
              label={t("CHALLAN_STATUS")}
              text={status ? t(`CHALLAN_STATUS_${status}`, status) : t("CS_NA")}
            />
            <Row className="border-none" label={t("CHALLAN_TOTAL_AMOUNT")} text={`₹ ${amount}`} />
            <Row
              className="border-none"
              label={t("CHALLAN_ISSUE_DATE")}
              text={
                challan?.auditDetails?.createdTime
                  ? convertEpochToDate(challan.auditDetails.createdTime)
                  : challan?.taxPeriodFrom
                  ? convertEpochToDate(challan.taxPeriodFrom)
                  : t("CS_NA")
              }
            />
            {challan?.taxPeriodTo && (
              <Row
                className="border-none"
                label={t("CHALLAN_DUE_DATE")}
                text={convertEpochToDate(challan.taxPeriodTo)}
              />
            )}
            <Row
              className="border-none"
              label={t("UC_SERVICE_CATEGORY_LABEL")}
              text={t(`BILLINGSERVICE_BUSINESSSERVICE_${businessService?.toUpperCase()?.replace(/\./g, "_")}`, businessService)}
            />
          </StatusTable>

          {/* Offender / Applicant Details */}
          <CardSubHeader style={{ fontSize: "24px" }}>{t("CHALLAN_OFFENDER_DETAILS") || t("ES_APPLICANT_DETAILS")}</CardSubHeader>
          <StatusTable>
            <Row className="border-none" label={t("CHALLAN_NAME") || t("CORE_COMMON_NAME")} text={offenderName} />
            <Row className="border-none" label={t("CORE_COMMON_MOBILE_NUMBER")} text={mobileNumber} />
            {altMobileNumber && <Row className="border-none" label={t("CHALLAN_ALT_MOBILE_NUMBER")} text={altMobileNumber} />}
            {emailId && <Row className="border-none" label={t("UC_EMAIL_ID")} text={emailId} />}
          </StatusTable>

          {/* Property Location / Address Details */}
          <CardSubHeader style={{ fontSize: "24px" }}>{t("GC_PROPERTY_LOCATION_DETAILS") || t("CHALLAN_LOCATION_DETAILS")}</CardSubHeader>
          <StatusTable>
            {doorNo && <Row className="border-none" label={t("UC_DOOR_NO_LABEL")} text={doorNo} />}
            {buildingName && <Row className="border-none" label={t("UC_BUILDING_NAME_LABEL")} text={buildingName} />}
            {street && <Row className="border-none" label={t("UC_STREET_NAME_LABEL")} text={street} />}
            {localityText && <Row className="border-none" label={t("GC_LOCALITY") || t("UC_MOHALLA_LABEL")} text={localityText} />}
            {landmark && <Row className="border-none" label={t("GC_LANDMARK")} text={landmark} />}
            <Row className="border-none" label={t("GC_CITY")} text={city ? t(city) : t("CS_NA")} />
            {pincode && <Row className="border-none" label={t("GC_PINCODE")} text={pincode} />}
            <Row className="border-none" label={t("CHALLAN_ADDRESS")} text={fullAddress} />
          </StatusTable>

          {/* Offence Specifications */}
          <CardSubHeader style={{ fontSize: "24px" }}>{t("CHALLAN_OFFENCE_DETAILS")}</CardSubHeader>
          <StatusTable>
            <Row className="border-none" label={t("CHALLAN_OFFENCE_TYPE")} text={offenceType} />
            {offenceCategory && <Row className="border-none" label={t("CHALLAN_OFFENCE_CATEGORY")} text={offenceCategory} />}
            {vehicleNumber && <Row className="border-none" label={t("CHALLAN_VEHICLE_NO")} text={vehicleNumber} />}
            <Row className="border-none" label={t("CHALLAN_DESCRIPTION")} text={description} />
            {latitude && longitude && (
              <Row
                className="border-none"
                label={t("CHALLAN_GPS_COORDINATES")}
                text={`${latitude}, ${longitude}`}
              />
            )}
            {challan?.feeWaiver && (
              <Row className="border-none" label={t("FEE_WAIVER_AMOUNT")} text={`₹ ${challan.feeWaiver}`} />
            )}
          </StatusTable>

          {/* Documents */}
          {docs.length > 0 && (
            <>
              <CardSubHeader style={{ fontSize: "24px" }}>{t("CS_COMMON_DOCUMENTS") || t("GC_GARBAGE_DOCUMENTS")}</CardSubHeader>
              <StatusTable>
                <Card className="challan-custom-card">
                  {docs.map((doc, index) => (
                    <div key={`doc-${index}`} className="challan-doc-item" style={{ marginBottom: "16px" }}>
                      <CardSectionHeader>{t(doc?.documentType?.replace(/\./g, "_") || doc?.documentType)}</CardSectionHeader>
                      <CHBDocument value={docs} Code={doc?.documentType} index={index} />
                    </div>
                  ))}
                </Card>
              </StatusTable>
            </>
          )}

          {/* Workflow Timeline */}
          {workflowDetails?.data && (
            <NewApplicationTimeline workflowDetails={workflowDetails} t={t} />
          )}
        </Card>

        {/* Action Bar */}
        {!isPaid && status === "ACTIVE" && (
          <ActionBar>
            <SubmitBar
              label={t("CS_APPLICATION_DETAILS_MAKE_PAYMENT") || t("CHALLAN_PAY_NOW")}
              onSubmit={handleMakePayment}
            />
          </ActionBar>
        )}

        {isPaid && (
          <ActionBar>
            <SubmitBar
              label={t("CS_COMMON_DOWNLOAD_RECEIPT") || t("CHALLAN_PRINT_RECEIPT")}
              onSubmit={() => printReciept(businessService, challanNo)}
            />
          </ActionBar>
        )}

        {showToast && (
          <Toast
            error={showToast.key === "error"}
            label={showToast.label}
            onClose={() => setShowToast(null)}
          />
        )}
      </div>
    </React.Fragment>
  );
};

export default CitizenChallanDetails;
