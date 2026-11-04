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
  Dropdown,
} from "@nudmcdgnpm/digit-ui-react-components";
import React, { useEffect, useState, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import GCWFApplicationTimeline from "../../pageComponents/GCWFApplicationTimeline";
import { downloadGCReceipt, downloadGCAcknowledgement, multiUnits } from "../../utils";

/**
 * GCApplicationDetails Component (Citizen)
 * 
 * Displays detailed information about a GC application for Citizens (Owners and Tenants)
 * as defined in UPYOG Garbage Service HLD Section 3, Section 5, and Section 18:
 * - Displays Property ID, Floor Number, Property Type (Owner/Tenant), and Billed Amounts
 * - Enables bill payment via "Make Payment"
 * - Owner-Only Connection Lifecycle Actions (HLD Section 5.2):
 *   - DISCONNECT (Temporary Disconnection)
 *   - RECONNECT (Reconnection)
 *   - PERMANENT_DISCONNECT (Terminal Disconnection)
 *   with modal capturing reason and reasonDetails.
 */
const GCApplicationDetails = () => {
  const { t } = useTranslation();
  const navigate = Digit.Hooks.useCustomNavigate();
  const params = useParams();

  let reconstructedAppNo = params.applicationNo;
  if (params["*"]) {
    reconstructedAppNo = `${params.applicationNo}/${params["*"]}`;
  }
  const applicationNo = decodeURIComponent(reconstructedAppNo);
  const tenantId = Digit.ULBService.getCitizenCurrentTenant(true) || Digit.ULBService.getCurrentTenantId();
  const user = Digit.UserService.getUser()?.info || {};

  const [showOptions, setShowOptions] = useState(false);
  const [selectedAction, setSelectedAction] = useState(null);
  const [selectedReason, setSelectedReason] = useState(null);
  const [comments, setComments] = useState("");
  const [showToast, setShowToast] = useState(null);

  useEffect(() => {
    if (showToast) {
      const timer = setTimeout(() => setShowToast(null), 6000);
      return () => clearTimeout(timer);
    }
  }, [showToast]);

  const searchCriteria = {
    searchCriteriaGarbageAccount: {
      applicationNumber: [applicationNo],
    },
  };

  const { isLoading, data: gcData } = Digit.Hooks.gc.useGCSearch(
    { tenantId, data: searchCriteria, filters: { applicationNumber: [applicationNo] } },
    { enabled: !!applicationNo, cacheTime: 0, staleTime: 0 }
  );

  const applicationList = gcData?.garbageApplications || gcData?.garbageAccounts || gcData?.GarbageApplications || gcData?.data || [];
  const application = applicationList.length > 0 ? applicationList[0] : null;

  const businessService = application?.businessService || "garbage-service";

  const { data: storeData } = Digit.Hooks.useStore.getInitData();
  const { tenants } = storeData || {};

  const { data: reciept_data, isLoading: recieptDataLoading } = Digit.Hooks.useRecieptSearch(
    { tenantId, businessService: "garbage-service", consumerCodes: applicationNo },
    { enabled: !!applicationNo }
  );

  const GCDocuments = Digit?.ComponentRegistryService?.getComponent("GCDocuments");

  const { isLoading: isWorkflowLoading, data: workflowDetails } = Digit.Hooks.useWorkflowDetails({
    tenantId: tenantId,
    id: applicationNo,
    moduleCode: businessService,
    config: { staleTime: 0 },
  });

  const queryClient = useQueryClient();
  const { mutateAsync } = useMutation({ mutationFn: (data) => Digit.GCServices.update(data, tenantId) });

  const CloseBtn = (props) => (
    <div onClick={props.onClick} className="gc-gcapplication-details-clickable" style={{ cursor: "pointer" }}>
      <CloseSvg />
    </div>
  );

  let appData = application?.garbageAccount || application || {};
  if (typeof appData === "string") {
    try {
      appData = JSON.parse(appData);
    } catch (e) {
      appData = {};
    }
  }

  const applicationDetails = appData?.grbgApplication || appData?.GarbageApplication || {};
  const appNo =
    applicationDetails?.applicationNo ||
    appData?.grbgApplicationNumber ||
    appData?.applicationNo ||
    appData?.garbageNo ||
    t("CS_NA");

  const appStatus =
    applicationDetails?.status ||
    appData?.status ||
    appData?.applicationStatus ||
    t("CS_NA");

  const dueDate = appData?.dueDate || null;
  const paymentStatus = appData?.paymentStatus || null;

  // Determine Owner vs Tenant Persona
  const specs = appData?.grbgCollectionUnits?.[0] || appData?.garbageSpecification || {};
  const propertyType = specs?.propertyType || specs?.ownerType || "OWNER";
  const isOwner = propertyType === "OWNER" || user?.uuid === appData?.ownerUuid;
  const floorNo = specs?.floorNo || null;

  // Reason Dropdowns for Lifecycle Actions
  const disconnectReasons = useMemo(
    () => [
      { code: "PROPERTY_VACANT", i18nKey: "GC_REASON_VACANT", name: "Property Vacant" },
      { code: "TENANT_VACATED", i18nKey: "GC_REASON_TENANT_VACATED", name: "Tenant Vacated" },
      { code: "MAJOR_RENOVATION", i18nKey: "GC_REASON_RENOVATION", name: "Major Renovation" },
      { code: "OTHER", i18nKey: "GC_REASON_OTHER", name: "Other" },
    ],
    []
  );

  const reconnectReasons = useMemo(
    () => [
      { code: "REOCCUPIED", i18nKey: "GC_REASON_REOCCUPIED", name: "Property Reoccupied" },
      { code: "NEW_TENANT_MOVED_IN", i18nKey: "GC_REASON_NEW_TENANT", name: "New Tenant Moved In" },
      { code: "RENOVATION_COMPLETE", i18nKey: "GC_REASON_RENOVATION_DONE", name: "Renovation Completed" },
      { code: "OTHER", i18nKey: "GC_REASON_OTHER", name: "Other" },
    ],
    []
  );

  const permDisconnectReasons = useMemo(
    () => [
      { code: "BUILDING_DEMOLITION", i18nKey: "GC_REASON_DEMOLITION", name: "Building Demolition" },
      { code: "PROPERTY_RECONSTRUCTION", i18nKey: "GC_REASON_RECONSTRUCTION", name: "Property Reconstruction" },
      { code: "LAND_CONSOLIDATION", i18nKey: "GC_REASON_LAND", name: "Land Consolidation" },
      { code: "OTHER", i18nKey: "GC_REASON_OTHER", name: "Other" },
    ],
    []
  );

  const submitLifecycleAction = async () => {
    if (!selectedAction) return;

    if (!selectedReason) {
      setShowToast({ key: "error", label: t("GC_PLEASE_SELECT_REASON") || "Please select a valid reason." });
      return;
    }

    try {
      const payloadApp = {
        ...appData,
        workflow: {
          action: selectedAction,
          comments: comments || `${selectedAction} initiated by owner.`,
          assignes: [],
        },
        connectionLog: {
          requestType:
            selectedAction === "DISCONNECT"
              ? "DISCONNECTION"
              : selectedAction === "RECONNECT"
              ? "RECONNECTION"
              : "PERMANENT_DISCONNECTION",
          reason: selectedReason?.code || selectedReason,
          reasonDetails: comments,
        },
      };

      const payload = {
        garbageApplications: [payloadApp],
        garbageAccounts: [payloadApp],
      };

      await mutateAsync(payload);

      queryClient.invalidateQueries({ queryKey: ["GC_SEARCH_APPLICATIONS"] });
      queryClient.invalidateQueries({ queryKey: ["workFlowDetails"] });
      setShowToast({ key: "success", label: t(`GC_ACTION_SUCCESS_${selectedAction}`) || `${selectedAction} submitted successfully.` });
      setSelectedAction(null);
      setSelectedReason(null);
      setComments("");
      setTimeout(() => window.location.reload(), 1500);
    } catch (error) {
      console.error("Workflow action error:", error);
      setShowToast({
        key: "error",
        label: error?.response?.data?.Errors?.[0]?.message || t("GC_ACTION_FAILED") || "Action failed to execute.",
      });
    }
  };

  const downloadOptions = [];
  downloadOptions.push({
    label: t("GC_DOWNLOAD_ACKNOWLEDGEMENT"),
    onClick: () => downloadGCAcknowledgement(appData, tenants, t),
  });
  if (reciept_data?.Payments?.length > 0 && !recieptDataLoading) {
    downloadOptions.push({
      label: t("GC_FEE_RECEIPT"),
      onClick: () => downloadGCReceipt(reciept_data.Payments[0].tenantId, reciept_data.Payments[0]),
    });
  }

  const docs = appData?.documents || [];

  const handleMakePayment = () => {
    navigate(`/upyog-ui/citizen/payment/my-bills/garbage-service/${appNo}`);
  };

  if (isLoading || isWorkflowLoading) {
    return <Loader />;
  }

  if (!application) {
    return <div>{t("GC_APPLICATION_NOT_FOUND") || "Application not found"}</div>;
  }

  // Address Details
  const address = appData?.addresses?.[0] || appData?.propertyLocation || {};
  const addressAdditional = address?.additionalDetail || {};
  const propertyId = appData?.propertyId || address?.propertyId;
  const pincode = address?.pincode;
  const city = address?.city || appData?.tenantId;
  const localityRaw = addressAdditional?.locality || address?.locality;
  const localityText = typeof localityRaw === "string" ? t(localityRaw) : localityRaw?.name ? t(localityRaw.name) : null;
  const street = addressAdditional?.streetName || address?.streetName;
  const houseNo = addressAdditional?.houseNo || address?.houseNo;
  const buildingName = addressAdditional?.houseName || address?.houseName;
  const addressLine1 = address?.address1 || address?.addressline1;
  const addressLine2 = address?.address2 || address?.addressline2;
  const landmark = addressAdditional?.landmark || address?.landmark;

  // Applicant/Contact Details
  const applicantInfo = appData?.additionalDetail?.applicantDetails?.[0] || appData?.applicantDetails?.[0] || appData?.tenantDetails || {};
  const applicantName = applicantInfo?.name || appData?.name || t("CS_NA");
  const applicantMobile = applicantInfo?.mobileNumber || appData?.mobileNumber || t("CS_NA");
  const applicantEmail = applicantInfo?.emailId || appData?.emailId || t("CS_NA");

  // Specifications
  const oldGarbageId = appData?.grbgOldDetails?.oldGarbageId || specs?.oldGarbageId || appData?.oldGarbageNo;
  const typeOfCollection = specs?.unitType || specs?.collectionType;
  const category = specs?.category;
  const subCategory = specs?.subCategory;
  const subCategoryType = specs?.subCategoryType;
  const no_of_units = specs?.no_of_units;
  const specialCategory = specs?.specialCategory;
  const isInheritance = specs?.isInheritance;

  return (
    <React.Fragment>
      <div>
        <div className="cardHeaderWithOptions gc-gcapplication-details-spacing">
          <Header styles={{ fontSize: "32px" }}>{t("GC_APPLICATION_DETAILS") || "Garbage Service Details"}</Header>
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
          <CardSubHeader className="gc-check-page-header">{t("GC_APPLICATION_SUMMARY") || "Application Summary"}</CardSubHeader>
          <StatusTable>
            <Row className="border-none" label={t("GC_APPLICATION_NUMBER_LABEL")} text={appNo} />
            <Row
              className="border-none"
              label={t("GC_APPLICATION_STATUS_LABEL")}
              text={
                <span
                  style={{
                    color:
                      appStatus === "APPROVED" || appStatus === "ACTIVE"
                        ? "#00703c"
                        : appStatus === "DISCONNECTED"
                        ? "#a82227"
                        : appStatus === "PERMANENTLY_DISCONNECTED"
                        ? "#d4351c"
                        : "#1d70b8",
                    fontWeight: "bold",
                  }}
                >
                  {t(`GC_STATUS_${appStatus}`) || appStatus}
                </span>
              }
            />
            <Row
              className="border-none"
              label={t("GC_PROPERTY_TYPE") || "Application Type"}
              text={
                <span
                  style={{
                    backgroundColor: propertyType === "OWNER" ? "#1d70b8" : "#00703c",
                    color: "#fff",
                    padding: "2px 8px",
                    borderRadius: "4px",
                    fontSize: "12px",
                    fontWeight: "bold",
                  }}
                >
                  {propertyType}
                </span>
              }
            />
            {floorNo && (
              <Row
                className="border-none"
                label={t("GC_FLOOR_UNIT") || "Assigned Floor"}
                text={floorNo === "0" ? t("GC_FLOOR_GROUND") || "Ground Floor" : `${floorNo} Floor`}
              />
            )}
            {dueDate && <Row className="border-none" label={t("GC_DUE_DATE")} text={dueDate} />}
            {paymentStatus && (
              <Row className="border-none" label={t("GC_PAYMENT_STATUS") || "Payment Status"} text={paymentStatus} />
            )}
          </StatusTable>

          {/* Applicant Details */}
          <CardSubHeader className="gc-check-page-header">{t("ES_APPLICANT_DETAILS")}</CardSubHeader>
          <StatusTable>
            <Row className="border-none" label={t("GC_APPLICANT_NAME")} text={applicantName} />
            <Row className="border-none" label={t("GC_MOBILE_NUMBER")} text={applicantMobile} />
            <Row className="border-none" label={t("GC_EMAIL_ID")} text={applicantEmail} />
          </StatusTable>

          {/* Property Location */}
          <CardSubHeader className="gc-check-page-header">{t("GC_PROPERTY_LOCATION_DETAILS")}</CardSubHeader>
          <StatusTable>
            <Row className="border-none" label={t("GC_PROPERTY_ID")} text={propertyId || t("CS_NA")} />
            <Row className="border-none" label={t("GC_PINCODE")} text={pincode || t("CS_NA")} />
            <Row className="border-none" label={t("GC_CITY")} text={city ? t(city) : t("CS_NA")} />
            <Row className="border-none" label={t("GC_LOCALITY")} text={localityText || t("CS_NA")} />
            <Row className="border-none" label={t("GC_STREET_NAME")} text={street || t("CS_NA")} />
            <Row className="border-none" label={t("GC_HOUSE_NO")} text={houseNo || t("CS_NA")} />
            <Row className="border-none" label={t("GC_BUILDING_NAME")} text={buildingName || t("CS_NA")} />
            <Row className="border-none" label={t("GC_ADDRESS_LINE1")} text={addressLine1 || t("CS_NA")} />
            {addressLine2 && <Row className="border-none" label={t("GC_ADDRESS_LINE2")} text={addressLine2} />}
            <Row className="border-none" label={t("GC_LANDMARK")} text={landmark || t("CS_NA")} />
          </StatusTable>

          {/* Garbage Specifications */}
          <CardSubHeader className="gc-check-page-header">{t("GC_GARBAGE_SPECIFICATIONS")}</CardSubHeader>
          <StatusTable>
            {oldGarbageId && <Row className="border-none" label={t("GC_OLD_GARBAGE_ID")} text={oldGarbageId} />}
            <Row className="border-none" label={t("GC_TYPE_OF_COLLECTION")} text={typeOfCollection ? t(typeOfCollection) : t("CS_NA")} />
            <Row className="border-none" label={t("GC_CATEGORY")} text={category ? t(category) : t("CS_NA")} />
            <Row className="border-none" label={t("GC_SUB_CATEGORY")} text={subCategory ? t(subCategory) : t("CS_NA")} />
            <Row className="border-none" label={t("GC_SUB_CATEGORY_TYPE")} text={subCategoryType ? t(subCategoryType) : t("CS_NA")} />
            {multiUnits.includes(typeOfCollection) && (
              <Row className="border-none" label={t("GC_NO_OF_UNITS")} text={no_of_units || "1"} />
            )}
            <Row className="border-none" label={t("GC_SPECIAL_CATEGORY")} text={specialCategory ? t(specialCategory) : "NONE"} />
            <Row className="border-none" label={t("GC_IS_INHERITANCE")} text={isInheritance ? t("YES") : t("NO")} />
          </StatusTable>

          {/* Documents */}
          {docs.length > 0 && (
            <>
              <CardSubHeader className="gc-check-page-header">{t("GC_GARBAGE_DOCUMENTS")}</CardSubHeader>
              <StatusTable>
                <Card className="chb-doc-card">
                  {docs.map((doc, index) => (
                    <div key={`doc-${index}`} className="chb-doc-item">
                      <div>
                        <CardSectionHeader>{t("GC_" + doc?.documentType?.replaceAll(".", "_"))}</CardSectionHeader>
                        {GCDocuments ? (
                          <GCDocuments value={docs} Code={doc?.documentType} index={index} />
                        ) : (
                          <span>{doc?.documentType}</span>
                        )}
                      </div>
                    </div>
                  ))}
                </Card>
              </StatusTable>
            </>
          )}

          <GCWFApplicationTimeline application={appData} />
        </Card>

        {/* Action Bar for Payments, Edits, and Owner-Only Lifecycle actions */}
        <ActionBar style={{ display: "flex", gap: "10px", justifyContent: "flex-end" }}>
          {/* Edit Button */}
          {appStatus === "EDIT_APPLICATION" && (
            <SubmitBar
              label={t("GC_EDIT_APPLICATION") || "Edit Application"}
              onSubmit={() => navigate(`/upyog-ui/citizen/gc/edit/${encodeURIComponent(appNo)}`)}
            />
          )}

          {/* Make Payment Button */}
          {(appStatus === "PENDING_FOR_PAYMENT" || paymentStatus === "PENDING") && (
            <SubmitBar
              label={t("CS_APPLICATION_DETAILS_MAKE_PAYMENT") || "Make Payment"}
              onSubmit={handleMakePayment}
            />
          )}

          {/* Owner-Only Lifecycle Actions as per HLD Section 5.1 & 5.2 */}
          {isOwner && (appStatus === "APPROVED" || appStatus === "ACTIVE") && (
            <>
              <button
                type="button"
                className="btn-secondary"
                style={{
                  backgroundColor: "#a82227",
                  color: "#fff",
                  border: "none",
                  padding: "10px 18px",
                  fontWeight: "bold",
                  cursor: "pointer",
                  borderRadius: "2px",
                }}
                onClick={() => {
                  setSelectedAction("DISCONNECT");
                  setSelectedReason(disconnectReasons[0]);
                }}
              >
                {t("GC_ACTION_DISCONNECT") || "Disconnect Service"}
              </button>

              <button
                type="button"
                className="btn-danger"
                style={{
                  backgroundColor: "#d4351c",
                  color: "#fff",
                  border: "none",
                  padding: "10px 18px",
                  fontWeight: "bold",
                  cursor: "pointer",
                  borderRadius: "2px",
                }}
                onClick={() => {
                  setSelectedAction("PERMANENT_DISCONNECT");
                  setSelectedReason(permDisconnectReasons[0]);
                }}
              >
                {t("GC_ACTION_PERMANENT_DISCONNECT") || "Permanent Disconnection"}
              </button>
            </>
          )}

          {isOwner && appStatus === "DISCONNECTED" && (
            <>
              <button
                type="button"
                className="btn-primary"
                style={{
                  backgroundColor: "#00703c",
                  color: "#fff",
                  border: "none",
                  padding: "10px 18px",
                  fontWeight: "bold",
                  cursor: "pointer",
                  borderRadius: "2px",
                }}
                onClick={() => {
                  setSelectedAction("RECONNECT");
                  setSelectedReason(reconnectReasons[0]);
                }}
              >
                {t("GC_ACTION_RECONNECT") || "Reconnect Service"}
              </button>

              <button
                type="button"
                className="btn-danger"
                style={{
                  backgroundColor: "#d4351c",
                  color: "#fff",
                  border: "none",
                  padding: "10px 18px",
                  fontWeight: "bold",
                  cursor: "pointer",
                  borderRadius: "2px",
                }}
                onClick={() => {
                  setSelectedAction("PERMANENT_DISCONNECT");
                  setSelectedReason(permDisconnectReasons[0]);
                }}
              >
                {t("GC_ACTION_PERMANENT_DISCONNECT") || "Permanent Disconnection"}
              </button>
            </>
          )}
        </ActionBar>

        {/* Modal for Lifecycle Transitions (Disconnect, Reconnect, Permanent Disconnect) */}
        {selectedAction && (
          <Modal
            headerBarMain={
              <h1 className="heading-m">
                {selectedAction === "DISCONNECT"
                  ? t("GC_MODAL_TITLE_DISCONNECT") || "Temporary Service Disconnection"
                  : selectedAction === "RECONNECT"
                  ? t("GC_MODAL_TITLE_RECONNECT") || "Service Reconnection Request"
                  : t("GC_MODAL_TITLE_PERM_DISCONNECT") || "Permanent Service Disconnection"}
              </h1>
            }
            headerBarEnd={<CloseBtn onClick={() => setSelectedAction(null)} />}
            actionCancelLabel={t("CS_COMMON_CANCEL")}
            actionCancelOnSubmit={() => setSelectedAction(null)}
            actionSaveLabel={t("CS_COMMON_CONFIRM_SUBMIT") || "Confirm & Submit"}
            actionSaveOnSubmit={submitLifecycleAction}
          >
            <Card className="gc-gcapplication-details-card" style={{ padding: "8px 0" }}>
              {selectedAction === "PERMANENT_DISCONNECT" && (
                <div
                  style={{
                    backgroundColor: "#fff5f5",
                    border: "1px solid #d4351c",
                    padding: "12px",
                    borderRadius: "4px",
                    marginBottom: "16px",
                    color: "#d4351c",
                    fontSize: "13px",
                    fontWeight: "500",
                  }}
                >
                  ⚠️ {t("GC_PERMANENT_DISCONNECT_WARNING") || "Caution: Permanent disconnection terminates this garbage collection account permanently. No future reconnections will be permitted."}
                </div>
              )}

              {selectedAction === "DISCONNECT" && (
                <div
                  style={{
                    backgroundColor: "#fff8e6",
                    border: "1px solid #f3b200",
                    padding: "12px",
                    borderRadius: "4px",
                    marginBottom: "16px",
                    color: "#505a5f",
                    fontSize: "13px",
                  }}
                >
                  ℹ️ {t("GC_DISCONNECT_INFO") || "Monthly demand generation will immediately halt while disconnected. Only the property owner can reconnect."}
                </div>
              )}

              {selectedAction === "RECONNECT" && (
                <div
                  style={{
                    backgroundColor: "#f3fdf5",
                    border: "1px solid #00703c",
                    padding: "12px",
                    borderRadius: "4px",
                    marginBottom: "16px",
                    color: "#00703c",
                    fontSize: "13px",
                  }}
                >
                  ℹ️ {t("GC_RECONNECT_INFO") || "Monthly demand generation will resume upon approved reconnection. Zero arrears are required."}
                </div>
              )}

              <CardText style={{ fontWeight: "600", marginBottom: "6px" }}>
                {t("GC_SELECT_REASON") || "Select Reason"} <span style={{ color: "#d4351c" }}>*</span>
              </CardText>
              <Dropdown
                option={
                  selectedAction === "DISCONNECT"
                    ? disconnectReasons
                    : selectedAction === "RECONNECT"
                    ? reconnectReasons
                    : permDisconnectReasons
                }
                optionKey="name"
                selected={selectedReason}
                select={(val) => setSelectedReason(val)}
                placeholder={t("GC_CHOOSE_REASON") || "Choose a reason"}
                t={t}
              />

              <div style={{ marginTop: "16px" }}>
                <CardText style={{ fontWeight: "600", marginBottom: "6px" }}>
                  {t("GC_REASON_DETAILS_REMARKS") || "Remarks / Additional Details"}
                </CardText>
                <TextArea
                  value={comments}
                  onChange={(e) => setComments(e.target.value)}
                  placeholder={t("GC_ENTER_REMARKS_PLACEHOLDER") || "Enter remarks or inspection details..."}
                />
              </div>
            </Card>
          </Modal>
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

export default GCApplicationDetails;