import {
  Card,
  CardSectionHeader,
  Header,
  Loader,
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

  const formatFloor = (floor) => {
    if (floor === null || floor === undefined || floor === "") return t("CS_NA");
    const floorStr = String(floor).trim();
    if (floorStr === "0") {
      return t("GC_FLOOR_GROUND");
    }
    const fNum = parseInt(floorStr, 10);
    if (!isNaN(fNum)) {
      const suffix = fNum === 1 ? "st" : fNum === 2 ? "nd" : fNum === 3 ? "rd" : "th";
      return `${fNum}${suffix} Floor`;
    }
    return `${floorStr} Floor`;
  };

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

  const applicationList = gcData?.garbageApplications || [];
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
    <div onClick={props.onClick} style={{ cursor: "pointer" }}>
      <CloseSvg />
    </div>
  );

  const appData = application || {};
  const appNo = appData?.applicationNo || t("CS_NA");
  const appStatus = appData?.status || t("CS_NA");
  const connStatus = appData?.connectionStatus || "ACTIVE";

  const dueDate = appData?.dueDate || null;
  const paymentStatus = appData?.paymentStatus || null;

  // Determine Owner vs Tenant Persona
  const specs = appData?.garbageSpecification || {};
  const propertyType = specs?.propertyType || "OWNER";
  const isOwner = propertyType === "OWNER" || user?.uuid === appData?.ownerUuid;
  const floorNo = specs?.floorNo || null;
  const childApps = appData?.childGarbageApplications || [];

  const getStatusColor = (status) => {
    switch (status) {
      case "APPROVED":
      case "ACTIVE":
        return "#00703c";
      case "DISCONNECTED":
        return "#a82227";
      case "PERMANENTLY_DISCONNECTED":
      case "REJECTED":
        return "#d4351c";
      default:
        return "#1d70b8";
    }
  };

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
    navigate(`/upyog-ui/citizen/payment/my-bills/garbage-service/${encodeURIComponent(appNo)}`);
  };

  if (isLoading || isWorkflowLoading) {
    return <Loader />;
  }

  if (!application) {
    return (
      <div className="cardHeaderWithOptions">
        <Header>{t("GC_APPLICATION_NOT_FOUND") || "Application not found"}</Header>
      </div>
    );
  }

  // Address Details
  const address = appData?.propertyLocation || {};
  const addressAdditional = address?.additionalDetail || {};
  const propertyId = appData?.propertyId;
  const pincode = address?.pincode;
  const city = address?.city;
  const localityText = addressAdditional?.locality ? t(addressAdditional.locality) : address?.locality ? t(address.locality) : "";
  const street = addressAdditional?.streetName || address?.street;
  const houseNo = addressAdditional?.houseNo || address?.doorNo;
  const buildingName = addressAdditional?.houseName || address?.buildingName;
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
        {/* Header with Title and MultiLink download dropdown (matching Employee view structure) */}
        <div className="cardHeaderWithOptions" style={{ marginBottom: "20px" }}>
          <Header styles={{ marginLeft: "0px", paddingTop: "10px", fontSize: "32px" }}>
            {t("GC_APPLICATION_DETAILS") || "Garbage Service Details"}
          </Header>
          {downloadOptions.length > 0 && (
            <MultiLink
              className="multilinkWrapper employee-mulitlink-main-div"
              onHeadClick={() => setShowOptions(!showOptions)}
              displayOptions={showOptions}
              options={downloadOptions}
              downloadBtnClassName={"employee-download-btn-className"}
              optionsClassName={"employee-options-btn-className"}
            />
          )}
        </div>

        {/* Section 1: Application Summary */}
        <div className="app-details-section-card">
          <div className="app-details-section-header-wrap">
            <div className="app-details-section-title-wrap">
              <span className="app-details-section-accent-bar" />
              <CardSectionHeader className="app-details-section-title">
                {t("GC_APPLICATION_SUMMARY") || "Application Summary"}
              </CardSectionHeader>
            </div>
          </div>
          <div className="app-details-kv-grid">
            <div className="app-details-kv-cell">
              <p className="app-details-kv-label">{t("GC_APPLICATION_NUMBER_LABEL")}</p>
              <p className="app-details-kv-value">{appNo}</p>
            </div>
            {appData?.garbageNo && (
              <div className="app-details-kv-cell">
                <p className="app-details-kv-label">{t("GC_GARBAGE_NUMBER_LABEL")}</p>
                <p className="app-details-kv-value">{appData.garbageNo}</p>
              </div>
            )}
            <div className="app-details-kv-cell">
              <p className="app-details-kv-label">{t("GC_APPLICATION_STATUS_LABEL")}</p>
              <p className="app-details-kv-value" style={{ color: getStatusColor(appStatus), fontWeight: "bold" }}>
                {t(`GC_STATUS_${appStatus}`) || appStatus}
              </p>
            </div>
            <div className="app-details-kv-cell">
              <p className="app-details-kv-label">{t("GC_PROPERTY_TYPE") || "Application Type"}</p>
              <p className="app-details-kv-value">
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
              </p>
            </div>
            {floorNo && (
              <div className="app-details-kv-cell">
                <p className="app-details-kv-label">{t("GC_FLOOR_UNIT")}</p>
                <p className="app-details-kv-value">
                  {formatFloor(floorNo)}
                </p>
              </div>
            )}
            {dueDate && (
              <div className="app-details-kv-cell">
                <p className="app-details-kv-label">{t("GC_DUE_DATE")}</p>
                <p className="app-details-kv-value">{dueDate}</p>
              </div>
            )}
            {paymentStatus && (
              <div className="app-details-kv-cell">
                <p className="app-details-kv-label">{t("GC_PAYMENT_STATUS") || "Payment Status"}</p>
                <p className="app-details-kv-value">{paymentStatus}</p>
              </div>
            )}
          </div>
        </div>

        {/* Section 2: Applicant Details */}
        <div className="app-details-section-card">
          <div className="app-details-section-header-wrap">
            <div className="app-details-section-title-wrap">
              <span className="app-details-section-accent-bar" />
              <CardSectionHeader className="app-details-section-title">
                {t("ES_APPLICANT_DETAILS")}
              </CardSectionHeader>
            </div>
          </div>
          <div className="app-details-kv-grid">
            <div className="app-details-kv-cell">
              <p className="app-details-kv-label">{t("GC_APPLICANT_NAME")}</p>
              <p className="app-details-kv-value">{applicantName}</p>
            </div>
            <div className="app-details-kv-cell">
              <p className="app-details-kv-label">{t("GC_MOBILE_NUMBER")}</p>
              <p className="app-details-kv-value">{applicantMobile}</p>
            </div>
            <div className="app-details-kv-cell">
              <p className="app-details-kv-label">{t("GC_EMAIL_ID")}</p>
              <p className="app-details-kv-value">{applicantEmail}</p>
            </div>
          </div>
        </div>

        {/* Section 3: Property & Location Details */}
        <div className="app-details-section-card">
          <div className="app-details-section-header-wrap">
            <div className="app-details-section-title-wrap">
              <span className="app-details-section-accent-bar" />
              <CardSectionHeader className="app-details-section-title">
                {t("GC_PROPERTY_LOCATION_DETAILS")}
              </CardSectionHeader>
            </div>
          </div>
          <div className="app-details-kv-grid">
            <div className="app-details-kv-cell">
              <p className="app-details-kv-label">{t("GC_PROPERTY_ID")}</p>
              <p className="app-details-kv-value">{propertyId || t("CS_NA")}</p>
            </div>
            <div className="app-details-kv-cell">
              <p className="app-details-kv-label">{t("GC_PINCODE")}</p>
              <p className="app-details-kv-value">{pincode || t("CS_NA")}</p>
            </div>
            <div className="app-details-kv-cell">
              <p className="app-details-kv-label">{t("GC_CITY")}</p>
              <p className="app-details-kv-value">{city ? t(city) : t("CS_NA")}</p>
            </div>
            <div className="app-details-kv-cell">
              <p className="app-details-kv-label">{t("GC_LOCALITY")}</p>
              <p className="app-details-kv-value">{localityText || t("CS_NA")}</p>
            </div>
            <div className="app-details-kv-cell">
              <p className="app-details-kv-label">{t("GC_STREET_NAME")}</p>
              <p className="app-details-kv-value">{street || t("CS_NA")}</p>
            </div>
            <div className="app-details-kv-cell">
              <p className="app-details-kv-label">{t("GC_HOUSE_NO")}</p>
              <p className="app-details-kv-value">{houseNo || t("CS_NA")}</p>
            </div>
            <div className="app-details-kv-cell">
              <p className="app-details-kv-label">{t("GC_BUILDING_NAME")}</p>
              <p className="app-details-kv-value">{buildingName || t("CS_NA")}</p>
            </div>
            <div className="app-details-kv-cell">
              <p className="app-details-kv-label">{t("GC_ADDRESS_LINE1")}</p>
              <p className="app-details-kv-value">{addressLine1 || t("CS_NA")}</p>
            </div>
            {addressLine2 && (
              <div className="app-details-kv-cell">
                <p className="app-details-kv-label">{t("GC_ADDRESS_LINE2")}</p>
                <p className="app-details-kv-value">{addressLine2}</p>
              </div>
            )}
            <div className="app-details-kv-cell">
              <p className="app-details-kv-label">{t("GC_LANDMARK")}</p>
              <p className="app-details-kv-value">{landmark || t("CS_NA")}</p>
            </div>
          </div>
        </div>

        {/* Section 4: Garbage Specifications */}
        <div className="app-details-section-card">
          <div className="app-details-section-header-wrap">
            <div className="app-details-section-title-wrap">
              <span className="app-details-section-accent-bar" />
              <CardSectionHeader className="app-details-section-title">
                {t("GC_GARBAGE_SPECIFICATIONS")}
              </CardSectionHeader>
            </div>
          </div>
          <div className="app-details-kv-grid">
            {oldGarbageId && (
              <div className="app-details-kv-cell">
                <p className="app-details-kv-label">{t("GC_OLD_GARBAGE_ID")}</p>
                <p className="app-details-kv-value">{oldGarbageId}</p>
              </div>
            )}
            <div className="app-details-kv-cell">
              <p className="app-details-kv-label">{t("GC_TYPE_OF_COLLECTION")}</p>
              <p className="app-details-kv-value">{typeOfCollection ? t(typeOfCollection) : t("CS_NA")}</p>
            </div>
            <div className="app-details-kv-cell">
              <p className="app-details-kv-label">{t("GC_CATEGORY")}</p>
              <p className="app-details-kv-value">{category ? t(category) : t("CS_NA")}</p>
            </div>
            <div className="app-details-kv-cell">
              <p className="app-details-kv-label">{t("GC_SUB_CATEGORY")}</p>
              <p className="app-details-kv-value">{subCategory ? t(subCategory) : t("CS_NA")}</p>
            </div>
            <div className="app-details-kv-cell">
              <p className="app-details-kv-label">{t("GC_SUB_CATEGORY_TYPE")}</p>
              <p className="app-details-kv-value">{subCategoryType ? t(subCategoryType) : t("CS_NA")}</p>
            </div>
            {multiUnits.includes(typeOfCollection) && (
              <div className="app-details-kv-cell">
                <p className="app-details-kv-label">{t("GC_NO_OF_UNITS")}</p>
                <p className="app-details-kv-value">{no_of_units || "1"}</p>
              </div>
            )}
            <div className="app-details-kv-cell">
              <p className="app-details-kv-label">{t("GC_SPECIAL_CATEGORY")}</p>
              <p className="app-details-kv-value">{specialCategory ? t(specialCategory) : "NONE"}</p>
            </div>
            <div className="app-details-kv-cell">
              <p className="app-details-kv-label">{t("GC_IS_INHERITANCE")}</p>
              <p className="app-details-kv-value">{isInheritance ? t("YES") : t("NO")}</p>
            </div>
          </div>
        </div>

        {/* Section 5: Documents */}
        {docs.length > 0 && (
          <div className="app-details-section-card">
            <div className="app-details-section-header-wrap">
              <div className="app-details-section-title-wrap">
                <span className="app-details-section-accent-bar" />
                <CardSectionHeader className="app-details-section-title">
                  {t("GC_GARBAGE_DOCUMENTS")}
                </CardSectionHeader>
              </div>
            </div>
            <div style={{ display: "flex", flexWrap: "wrap", gap: "16px" }}>
              {docs.map((doc, index) => (
                <div key={`doc-${index}`} style={{ minWidth: "220px", flex: "1 1 220px", marginBottom: "8px" }}>
                  <CardSectionHeader style={{ fontSize: "14px", fontWeight: "600", marginBottom: "4px" }}>
                    {t("GC_" + doc?.documentType?.replaceAll(".", "_"))}
                  </CardSectionHeader>
                  {GCDocuments ? (
                    <GCDocuments value={docs} Code={doc?.documentType} index={index} />
                  ) : (
                    <span>{doc?.documentType}</span>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Section 6: Owner Cross-View: Tenant Floor Connections */}
        {isOwner && childApps.length > 0 && (
          <div className="app-details-section-card">
            <div className="app-details-section-header-wrap">
              <div className="app-details-section-title-wrap">
                <span className="app-details-section-accent-bar" />
                <CardSectionHeader className="app-details-section-title">
                  {t("GC_TENANT_APPLICATIONS") || "Tenant Floor Connections"}
                </CardSectionHeader>
              </div>
            </div>
            <div style={{ display: "flex", flexDirection: "column", gap: "12px", marginTop: "12px" }}>
              {childApps.map((child, cIdx) => {
                const childAppNo = child?.applicationNo || child?.garbageNo || `Tenant ${cIdx + 1}`;
                const childFloor = child?.garbageSpecification?.floorNo || child?.floorNo || `${cIdx + 1}`;
                const childName = child?.applicantDetails?.[0]?.name || child?.tenantDetails?.name || t("CS_NA");
                const childMobile = child?.applicantDetails?.[0]?.mobileNumber || child?.tenantDetails?.mobileNumber || t("CS_NA");
                const childStatus = child?.status || "APPLICATION_CREATED";
                const childConnStatus = child?.connectionStatus || childStatus;
                return (
                  <Card key={childAppNo} style={{ padding: "12px 16px", border: "1px solid #d6d5d4", borderRadius: "4px" }}>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "8px" }}>
                      <span style={{ fontWeight: "bold", fontSize: "16px" }}>
                        {formatFloor(childFloor)}: {childName}
                      </span>
                      <span
                        style={{
                          padding: "2px 8px",
                          borderRadius: "4px",
                          fontSize: "12px",
                          fontWeight: "bold",
                          color: "#fff",
                          backgroundColor:
                            childConnStatus === "ACTIVE" || childStatus === "APPROVED"
                              ? "#00703c"
                              : childConnStatus === "DISCONNECTED"
                              ? "#a82227"
                              : "#1d70b8",
                        }}
                      >
                        {childConnStatus}
                      </span>
                    </div>
                    <div className="app-details-kv-grid">
                      <div className="app-details-kv-cell">
                        <p className="app-details-kv-label">{t("GC_APPLICATION_NUMBER_LABEL")}</p>
                        <p className="app-details-kv-value">
                          <span className="link">
                            <a href={`/upyog-ui/citizen/gc/application-details/${encodeURIComponent(childAppNo)}`}>
                              {childAppNo}
                            </a>
                          </span>
                        </p>
                      </div>
                      <div className="app-details-kv-cell">
                        <p className="app-details-kv-label">{t("GC_MOBILE_NUMBER")}</p>
                        <p className="app-details-kv-value">{childMobile}</p>
                      </div>
                    </div>
                  </Card>
                );
              })}
            </div>
          </div>
        )}

        {/* Tenant notice */}
        {!isOwner && (
          <div style={{ padding: "12px", backgroundColor: "#f3f2f1", borderLeft: "4px solid #1d70b8", marginBottom: "24px", fontSize: "14px", color: "#505a5f" }}>
            {t("GC_TENANT_NOTICE_OWNER_AUTHORITY") || "Only the property owner has the authority to request connection disconnection or reconnection for this property."}
          </div>
        )}

        {/* Section 7: Workflow Timeline */}
        <div className="app-details-section-card">
          <div className="app-details-section-header-wrap">
            <div className="app-details-section-title-wrap">
              <span className="app-details-section-accent-bar" />
              <CardSectionHeader className="app-details-section-title">
                {t("CS_APPLICATION_DETAILS_APPLICATION_TIMELINE") || "Application Timeline"}
              </CardSectionHeader>
            </div>
          </div>
          <GCWFApplicationTimeline application={appData} />
        </div>

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
          {isOwner && connStatus === "ACTIVE" && (
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
                  borderRadius: "4px",
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
                  borderRadius: "4px",
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

          {isOwner && connStatus === "DISCONNECTED" && (
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
                  borderRadius: "4px",
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
                  borderRadius: "4px",
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
            <Card style={{ padding: "8px 0", boxShadow: "none", margin: 0 }}>
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
