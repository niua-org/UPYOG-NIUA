import { Card, CardSubHeader, CardSectionHeader, Header, Loader, Row, StatusTable, SubmitBar, ActionBar, Modal, Toast, TextArea, CardText, CloseSvg, MultiLink } from "@nudmcdgnpm/digit-ui-react-components";
import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";
import GCWFApplicationTimeline from "../../pageComponents/GCWFApplicationTimeline";
import GCPaymentHistoryModal from "../../components/GCPaymentHistoryModal";
import GCPauseServiceModal from "../../components/GCPauseServiceModal";
import GCPauseHistoryModal from "../../components/GCPauseHistoryModal";
import { downloadGCReceipt, downloadGCAcknowledgement, multiUnits, formatDateValue, createDisconnectPayload, createReconnectPayload } from "../../utils";
import "../../css/gc-inline-auto.css";

/**
 * GCApplicationDetails Component (Citizen)
 * 
 * Displays detailed information about a specific GC application for citizens,
 * including applicant details, property location, garbage specifications, and payment status.
 * 
 * Features:
 * - Fetches application and workflow details by application number from URL params
 * - Handles payment: fetches bill data and provides "Make Payment" button when pending
 * - Supports edit flow: shows "Edit Application" button when status is EDIT_APPLICATION
 * - Supports disconnection & reconnection flow: allows citizens to disconnect active services and resume via edit flow
 * - Displays all details in organized sections with StatusTable rows
 * - Handles multiple data formats from the API (nested garbageAccount, flat structure, etc.)
 */
const GCApplicationDetails = () => {
  const { t } = useTranslation();
  const navigate = Digit.Hooks.useCustomNavigate();
  const params = useParams();

  // Reconstruct application number from URL params
  let reconstructedAppNo = params.applicationNo;
  if (params["*"]) {
    reconstructedAppNo = `${params.applicationNo}/${params["*"]}`;
  }
  const applicationNo = decodeURIComponent(reconstructedAppNo);
  const tenantId = Digit.ULBService.getCitizenCurrentTenant(true) || Digit.ULBService.getCurrentTenantId();

  const [showOptions, setShowOptions] = useState(false);
  const [showPaymentHistoryModal, setShowPaymentHistoryModal] = useState(false);
  const [showDisconnectionHistoryModal, setShowDisconnectionHistoryModal] = useState(false);
  const [showDisconnectModal, setShowDisconnectModal] = useState(false);
  const [selectedAction, setSelectedAction] = useState(null);
  const [comments, setComments] = useState("");
  const [showToast, setShowToast] = useState(null);

  useEffect(() => {
    if (showToast) {
      const timer = setTimeout(() => setShowToast(null), 10000);
      return () => clearTimeout(timer);
    }
  }, [showToast]);

  const searchCriteria = {
    searchCriteriaGarbageAccount: {
      applicationNumber: [applicationNo],
    },
  };

  const { isLoading, data: gcData, refetch: refetchApplication } = Digit.Hooks.gc.useGCSearch(
    { tenantId, data: searchCriteria, filters: { applicationNumber: [applicationNo] } },
    { enabled: !!applicationNo, cacheTime: 0, staleTime: 0 }
  );

  const application = gcData?.garbageAccounts?.[0] || null;

  const businessService = application?.businessService || "garbage-service";

  const { data: storeData } = Digit.Hooks.useStore.getInitData();
  const { tenants } = storeData || {};

  const { data: reciept_data, isLoading: recieptDataLoading } = Digit.Hooks.useRecieptSearch(
    { tenantId, businessService: "garbage-service", consumerCodes: applicationNo},
    { enabled: !!applicationNo }
  );

  const GCDocuments = Digit?.ComponentRegistryService?.getComponent("GCDocuments");

  const { isLoading: isWorkflowLoading, data: workflowDetails, revalidate: revalidateWorkflow } = Digit.Hooks.useWorkflowDetails({
    tenantId: tenantId,
    id: applicationNo, // Use the application number from the URL directly
    moduleCode: businessService,
    config: { staleTime: 0 }
  });

  const { mutateAsync } = Digit.Hooks.gc.useGCApplicationAction(tenantId);

  const CloseBtn = (props) => {
    return (
      <div onClick={props.onClick} style={{ cursor: "pointer", padding: "5px" }}>
        <CloseSvg />
      </div>
    );
  };

  const submitWorkflowAction = async () => {
    try {
      const payload = {
        garbageAccounts: [
          {
            ...application,
            workflow: {
              action: selectedAction.action,
              comments: comments,
              assignes: []
            }
          }
        ]
      };

      await mutateAsync(payload);

      setShowToast({ key: "success", label: t("GC_ACTION_SUCCESS") });
      setSelectedAction(null);
      setComments("");
      refetchApplication();
      revalidateWorkflow();
    } catch (error) {
      setShowToast({ key: "error", label: t("GC_ACTION_FAILED") });
    }
  };

  const handleDisconnectService = async (params) => {
    try {
      const payload = createDisconnectPayload(application, params);

      await mutateAsync(payload);
      setShowDisconnectModal(false);

      if (params.hasPendingPayment) {
        setShowToast({ key: "info", label: t("GC_REDIRECTING_TO_PAYMENT") });
        setTimeout(() => {
          navigate(`/upyog-ui/citizen/payment/my-bills/garbage-service/${encodeURIComponent(appNo)}`);
        }, 1500);
      } else {
        setShowToast({ key: "success", label: t("GC_DISCONNECT_REQUEST_SUCCESS") });
        refetchApplication();
        revalidateWorkflow();
      }
    } catch (error) {
      setShowToast({ key: "error", label: t("GC_DISCONNECT_REQUEST_FAILED") });
    }
  };

  const handleContinueService = async () => {
    try {
      const payload = createReconnectPayload(application, "GC_SERVICE_RECONNECTION_REQUESTED");

      await mutateAsync(payload);
      navigate(`/upyog-ui/citizen/gc/edit/${encodeURIComponent(appNo)}`);
    } catch (error) {
      setShowToast({ key: "error", label: t("GC_ACTION_FAILED") });
    }
  };

  const appNo = application?.grbgApplicationNumber;
  const rawStatus = application?.status || "";
  const appStatus = rawStatus.toUpperCase();
  const dueDate = application?.dueDate;
  const paymentStatus = application?.paymentStatus;
  const paymentAmount = application?.paymentAmount;

  const nextActions = workflowDetails?.data?.nextActions || [];
  const canDisconnect = nextActions.some((a) => a.action === "DISCONNECT");
  const canReconnect = nextActions.some((a) => a.action === "RECONNECT");
  const canEdit = nextActions.some((a) => a.action === "EDIT");
  const isPendingPayment = paymentStatus === "PENDING_FOR_PAYMENT" || (Number(paymentAmount) > 0 && paymentStatus !== "PAID");


  const downloadOptions = [];
  downloadOptions.push({
    label: t("GC_DOWNLOAD_ACKNOWLEDGEMENT"),
    onClick: () => downloadGCAcknowledgement(application, tenants, t),
  });
  if (reciept_data?.Payments?.length > 0 && !recieptDataLoading) {
    downloadOptions.push({
      label: t("GC_FEE_RECEIPT"),
      onClick: () => downloadGCReceipt(reciept_data.Payments[0].tenantId, reciept_data.Payments[0]),
    });
  }
  downloadOptions.push({
    label: t("GC_VIEW_DISCONNECTION_HISTORY"),
    onClick: () => setShowDisconnectionHistoryModal(true),
  });

  const docs = application?.documents || [];

  const formatDisplayDate = (dVal) => formatDateValue(dVal, t("CS_NA"));

  const handleMakePayment = () => {
    navigate(`/upyog-ui/citizen/payment/my-bills/garbage-service/${appNo}`);
  };

  if (isLoading || isWorkflowLoading) {
    return <Loader />;
  }

  if (!application) {
    return <div>{t("GC_APPLICATION_NOT_FOUND")}</div>;
  }

  // ---------- Applicant Details ----------
  const applicant = application?.additionalDetail?.applicantDetails?.[0];
  const ownerNames = applicant?.applicantName || application?.name || t("CS_NA");
  const mobileNumbers = applicant?.mobileNumber || application?.mobileNumber || t("CS_NA");
  const altMobileNumbers = applicant?.alternateNumber;
  const emails = applicant?.emailId || application?.emailId;

  // ---------- Address ----------
  const address = application?.addresses?.[0] || {};
  const addressAdditional = address?.additionalDetail || {};

  const propertyId = application?.propertyId;
  const pincode = address?.pincode;
  const city = address?.city;
  const localityText = addressAdditional?.locality ? t(addressAdditional.locality) : null;
  const street = addressAdditional?.streetName;
  const houseNo = addressAdditional?.houseNo;
  const buildingName = addressAdditional?.houseName;
  const addressLine1 = address?.address1;
  const addressLine2 = address?.address2;
  const landmark = addressAdditional?.landmark;

  // ---------- Garbage Specs ----------
  const collectionUnit = application?.grbgCollectionUnits?.[0] || {};
  const oldGarbageId = application?.grbgOldDetails?.oldGarbageId;
  const typeOfCollection = collectionUnit?.unitType;
  const ownerOrTenant = collectionUnit?.ownerType;
  const category = collectionUnit?.category;
  const subCategory = collectionUnit?.subCategory;
  const subCategoryType = collectionUnit?.subCategoryType;
  const no_of_units = collectionUnit?.no_of_units;
  const specialCategory = collectionUnit?.specialCategory;
  const isInheritance = collectionUnit?.isInheritance;
  const specName = application?.name;
  const specPhone = application?.mobileNumber;
  const specGender = application?.gender;
  const specEmail = application?.emailId;
  // ---------- Render ----------
  return (
    <React.Fragment>
      <div>
        <div className="cardHeaderWithOptions" style={{ marginRight: "auto", maxWidth: "960px" }}>
          <Header styles={{ fontSize: "32px" }}>{t("GC_APPLICATION_DETAILS")}</Header>
          {downloadOptions.length > 0 && (
            <MultiLink
              className="multilinkWrapper"
              onHeadClick={() => setShowOptions(!showOptions)}
              displayOptions={showOptions}
              options={downloadOptions}
            />
          )}
        </div>

        <Card>
          {/* Application Summary */}
          <CardSubHeader style={{ fontSize: "24px" }}>{t("GC_APPLICATION_SUMMARY")}</CardSubHeader>
          <StatusTable>
            <Row className="border-none" label={t("GC_APPLICATION_NUMBER_LABEL")} text={appNo || t("CS_NA")} />
            <Row
              className="border-none"
              label={t("GC_APPLICATION_STATUS_LABEL")}
              text={
                appStatus ? (
                  <span className={appStatus === "DISCONNECTED" ? "gc-status-disconnected" : ""}>
                    {t(`GC_STATUS_${appStatus}`)}
                  </span>
                ) : (
                  t("CS_NA")
                )
              }
            />
            {appStatus === "DISCONNECTED" && application?.additionalDetail?.disconnectionDate && (
              <Row
                className="border-none"
                label={t("GC_DISCONNECTION_DATE_LABEL")}
                text={formatDisplayDate(application.additionalDetail.disconnectionDate)}
              />
            )}
            {appStatus === "DISCONNECTED" && application?.additionalDetail?.disconnectionReason && (
              <Row
                className="border-none"
                label={t("GC_REASON_FOR_DISCONNECTION")}
                text={application.additionalDetail.disconnectionReason}
              />
            )}
            {paymentStatus && (
              <Row
                className="border-none"
                label={t("GC_PAYMENT_STATUS_LABEL")}
                text={
                  <span className={paymentStatus === "PAID" ? "gc-status-paid" : "gc-status-pending"}>
                    {t(`GC_STATUS_${paymentStatus}`)}
                  </span>
                }
              />
            )}
            {paymentAmount !== null && (
              <Row
                className="border-none"
                label={t("GC_PAYMENT_AMOUNT_LABEL")}
                text={`₹ ${paymentAmount}`}
              />
            )}
            {dueDate && <Row className="border-none" label={t("GC_DUE_DATE")} text={formatDisplayDate(dueDate)} />}
            <Row
              className="border-none"
              label={t("GC_PAYMENT_HISTORY_LABEL")}
              text={
                <span
                  className="gc-payment-history-link"
                  onClick={() => setShowPaymentHistoryModal(true)}
                >
                  {t("GC_VIEW_DETAILS")}
                </span>
              }
            />
            <Row
              className="border-none"
              label={t("GC_DISCONNECTION_HISTORY_LABEL")}
              text={
                <span
                  className="gc-payment-history-link"
                  onClick={() => setShowDisconnectionHistoryModal(true)}
                >
                  {t("GC_VIEW_DETAILS")}
                </span>
              }
            />
          </StatusTable>

          {/* Applicant Details */}
          <CardSubHeader style={{ fontSize: "24px" }}>{t("ES_APPLICANT_DETAILS")}</CardSubHeader>
          <StatusTable>
            <Row className="border-none" label={t("GC_APPLICANT_NAME")} text={ownerNames || t("CS_NA")} />
            <Row className="border-none" label={t("GC_MOBILE_NUMBER")} text={mobileNumbers || t("CS_NA")} />
            <Row className="border-none" label={t("GC_ALT_MOBILE_NUMBER")} text={altMobileNumbers || t("CS_NA")} />
            <Row className="border-none" label={t("GC_EMAIL_ID")} text={emails || t("CS_NA")} />
          </StatusTable>

          {/* Property Location */}
          <CardSubHeader style={{ fontSize: "24px" }}>{t("GC_PROPERTY_LOCATION_DETAILS")}</CardSubHeader>
          <StatusTable>
            <Row className="border-none" label={t("GC_PROPERTY_ID")} text={propertyId || t("CS_NA")} />
            <Row className="border-none" label={t("GC_PINCODE")} text={pincode || t("CS_NA")} />
            <Row className="border-none" label={t("GC_CITY")} text={city ? t(city) : t("CS_NA")} />
            <Row className="border-none" label={t("GC_LOCALITY")} text={localityText || t("CS_NA")} />
            <Row className="border-none" label={t("GC_STREET_NAME")} text={street || t("CS_NA")} />
            <Row className="border-none" label={t("GC_HOUSE_NO")} text={houseNo || t("CS_NA")} />
            <Row className="border-none" label={t("GC_BUILDING_NAME")} text={buildingName || t("CS_NA")} />
            <Row className="border-none" label={t("GC_ADDRESS_LINE1")} text={addressLine1 || t("CS_NA")} />
            <Row className="border-none" label={t("GC_ADDRESS_LINE2")} text={addressLine2 || t("CS_NA")} />
            <Row className="border-none" label={t("GC_LANDMARK")} text={landmark || t("CS_NA")} />
          </StatusTable>

          {/* Garbage Specifications */}
          <CardSubHeader style={{ fontSize: "24px" }}>{t("GC_GARBAGE_SPECIFICATIONS")}</CardSubHeader>
          <StatusTable>
            <Row className="border-none" label={t("GC_OLD_GARBAGE_ID")} text={oldGarbageId || t("CS_NA")} />
            <Row className="border-none" label={t("GC_TYPE_OF_COLLECTION")} text={typeOfCollection ? t(typeOfCollection) : t("CS_NA")} />
            <Row className="border-none" label={t("GC_OWNER_OR_TENANT")} text={ownerOrTenant ? t(ownerOrTenant) : t("CS_NA")} />
            <Row className="border-none" label={t("GC_NAME")} text={specName || t("CS_NA")} />
            <Row className="border-none" label={t("GC_PHONE_NUMBER")} text={specPhone || t("CS_NA")} />
            <Row className="border-none" label={t("GC_GENDER")} text={specGender ? t(specGender) : t("CS_NA")} />
            <Row className="border-none" label={t("GC_EMAIL")} text={specEmail || t("CS_NA")} />
            <Row className="border-none" label={t("GC_CATEGORY")} text={category ? t(category) : t("CS_NA")} />
            <Row className="border-none" label={t("GC_SUB_CATEGORY")} text={subCategory ? t(subCategory) : t("CS_NA")} />
            <Row className="border-none" label={t("GC_SUB_CATEGORY_TYPE")} text={subCategoryType ? t(subCategoryType) : t("CS_NA")} />
            {multiUnits.includes(typeOfCollection) && (
              <Row className="border-none" label={t("GC_NO_OF_UNITS")} text={no_of_units || t("CS_NA")} />
            )}
            <Row className="border-none" label={t("GC_SPECIAL_CATEGORY")} text={specialCategory ? t(specialCategory) : t("CS_NA")} />
            <Row className="border-none" label={t("GC_IS_INHERITANCE")} text={isInheritance ? t("YES") : t("NO")} />
          </StatusTable>

          {/* Documents */}
          {docs.length > 0 && (
            <>
              <CardSubHeader style={{ fontSize: "24px" }}>{t("GC_GARBAGE_DOCUMENTS")}</CardSubHeader>
              <StatusTable>
                <Card className="chb-doc-card">
                  {docs.map((doc, index) => (
                    <div key={`doc-${index}`} className="chb-doc-item">
                      <div>
                        <CardSectionHeader>{t("GC_" + (doc?.documentType?.split(".").slice(0, 2).join("_")))}</CardSectionHeader>
                        <GCDocuments value={docs} Code={doc?.documentType} index={index} />
                      </div>
                    </div>
                  ))}
                </Card>
              </StatusTable>
            </>
          )}

          <GCWFApplicationTimeline application={application} />


        </Card>

        {(canEdit || canDisconnect || canReconnect || isPendingPayment) && (
          <ActionBar>
            <div className="gc-btn-row">
              {canDisconnect && (
                <SubmitBar
                  label={t("GC_DISCONNECT_SERVICE")}
                  onSubmit={() => setShowDisconnectModal(true)}
                />
              )}
              {canReconnect && (
                <SubmitBar
                  label={t("GC_RECONNECT_SERVICE")}
                  onSubmit={handleContinueService}
                />
              )}
              {canEdit && (
                <SubmitBar
                  label={t("GC_EDIT_APPLICATION", "Edit Application")}
                  onSubmit={() =>
                    navigate(`/upyog-ui/citizen/gc/edit/${encodeURIComponent(appNo)}`)
                  }
                />
              )}
              {isPendingPayment && (
                <SubmitBar
                  label={t("CS_APPLICATION_DETAILS_MAKE_PAYMENT", "Make Payment")}
                  onSubmit={handleMakePayment}
                />
              )}
            </div>
          </ActionBar>
        )}

        {selectedAction && (
          <Modal
            headerBarMain={<h1 className="heading-m">{t(`WF_EMPLOYEE_GC_${selectedAction.action}`)}</h1>}
            headerBarEnd={<CloseBtn onClick={() => setSelectedAction(null)} />}
            actionCancelLabel={t("CS_COMMON_CANCEL", "Cancel")}
            actionCancelOnSubmit={() => setSelectedAction(null)}
            actionSaveLabel={t("CS_COMMON_SUBMIT", "Submit")}
            actionSaveOnSubmit={submitWorkflowAction}
          >
            <Card style={{ padding: "0px", margin: "0px", boxShadow: "none" }}>
              <CardText>{t("WF_COMMON_COMMENTS", "Comments")}</CardText>
              <TextArea value={comments} onChange={(e) => setComments(e.target.value)} />
            </Card>
          </Modal>
        )}

        <GCPauseServiceModal
          isOpen={showDisconnectModal}
          onClose={() => setShowDisconnectModal(false)}
          onConfirm={handleDisconnectService}
          applicationNo={appNo}
          paymentAmount={paymentAmount}
          paymentStatus={paymentStatus}
        />

        <GCPaymentHistoryModal
          isOpen={showPaymentHistoryModal}
          onClose={() => setShowPaymentHistoryModal(false)}
          payments={reciept_data?.Payments}
          tenantId={tenantId}
        />

        <GCPauseHistoryModal
          isOpen={showDisconnectionHistoryModal}
          onClose={() => setShowDisconnectionHistoryModal(false)}
          application={application}
        />

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

export default GCApplicationDetails;