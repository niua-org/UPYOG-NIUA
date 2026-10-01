import { Card, CardSubHeader, CardSectionHeader, Header, Loader, Row, StatusTable, SubmitBar, ActionBar, Modal, Toast, TextArea, CardText, CloseSvg, MultiLink } from "@nudmcdgnpm/digit-ui-react-components";
import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import GCWFApplicationTimeline from "../../pageComponents/GCWFApplicationTimeline";
import { downloadGCReceipt, downloadGCAcknowledgement, multiUnits } from "../../utils";

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

  const { isLoading, data: gcData } = Digit.Hooks.gc.useGCSearch(
    { tenantId, data: searchCriteria, filters: { applicationNumber: [applicationNo] } },
    { enabled: !!applicationNo, cacheTime: 0, staleTime: 0 }
  );

  // The API should now return a single application in the array
  const applicationList = gcData?.garbageAccounts || gcData?.GarbageApplications || gcData?.data || [];
  const application = applicationList.length > 0 ? applicationList[0] : null;

  const businessService = application?.businessService || "garbage-service";

  const { data: storeData } = Digit.Hooks.useStore.getInitData();
  const { tenants } = storeData || {};

  const { data: reciept_data, isLoading: recieptDataLoading } = Digit.Hooks.useRecieptSearch(
    { tenantId, businessService: "garbage-service", consumerCodes: applicationNo},
    { enabled: !!applicationNo }
  );

  const GCDocuments = Digit?.ComponentRegistryService?.getComponent("GCDocuments");

  const { isLoading: isWorkflowLoading, data: workflowDetails } = Digit.Hooks.useWorkflowDetails({
    tenantId: tenantId,
    id: applicationNo, // Use the application number from the URL directly
    moduleCode: businessService,
    config: { staleTime: 0 }
  });

  const queryClient = useQueryClient();

  const { mutateAsync } = useMutation({ mutationFn: (data) => Digit.GCServices.update(data, tenantId) });

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

      queryClient.invalidateQueries({ queryKey: ["GC_SEARCH_APPLICATIONS"] });
      queryClient.invalidateQueries({ queryKey: ["workFlowDetails"] });
      setShowToast({ key: "success", label: t("GC_ACTION_SUCCESS") });
      setSelectedAction(null);
      setComments("");
      setTimeout(() => window.location.reload(), 1500);
    } catch (error) {
      setShowToast({ key: "error", label: t("GC_ACTION_FAILED") });
    }
  };

  // Step 2: flatten any wrapper (must come before applicationDetails)
  let appData = application?.garbageAccount || application || {};
  if (typeof appData === "string") {
    try {
      appData = JSON.parse(appData);
    } catch (e) {
      appData = {};
    }
  }

  // Step 3: derive nested application details (grbgApplication lives inside the account object)
  const applicationDetails = appData?.grbgApplication || appData?.GarbageApplication || {};

  // Application number — prefer nested applicationNo, fall back to top-level grbgApplicationNumber
  const appNo =
    applicationDetails?.applicationNo ||
    appData?.grbgApplicationNumber ||
    appData?.applicationNo ||
    t("CS_NA");

  // Status — prefer nested status, fall back to top-level status
  const appStatus =
    applicationDetails?.status ||
    appData?.status ||
    appData?.applicationStatus ||
    t("CS_NA");

  const dueDate = appData?.dueDate || null;

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
    return <div>{t("GC_APPLICATION_NOT_FOUND")}</div>;
  }

  // ---------- Additional Details ----------
  let additionalDetails = appData?.additionalDetails || appData?.additionalDetail || {};
  if (typeof additionalDetails === "string") {
    try {
      additionalDetails = JSON.parse(additionalDetails);
    } catch (e) {
      additionalDetails = {};
    }
  }

  // ---------- Applicant / Owner Details ----------
  const rawOwners =
    appData?.additionalDetail?.applicantDetails ||
    appData?.additionalDetails?.applicantDetails ||
    appData?.applicantDetails ||
    [];
  const owners = Array.isArray(rawOwners) ? rawOwners : rawOwners ? [rawOwners] : [];

  const ownerNames =
    owners
      ?.map((o) => o?.name || o?.applicantName || o?.ownerName)
      ?.filter(Boolean)
      ?.join(", ") ||
    appData?.name ||
    t("CS_NA");


  const mobileNumbers =
    owners?.map((o) => o?.mobileNumber)?.filter(Boolean)?.join(", ") ||
    appData?.mobileNumber ||
    t("CS_NA");

  const emails =
    owners
      ?.map((o) => o?.emailId || o?.email || o?.emailAddress)
      ?.filter(Boolean)
      ?.join(", ") ||
    appData?.emailId ||
    appData?.email ||
    additionalDetails?.emailId ||
    appData?.user?.emailId;

  const altMobileNumbers =
    owners
      ?.map((o) => o?.alternateNumber || o?.altMobileNumber || o?.altMobileNo || o?.alternateMobileNumber)
      ?.filter(Boolean)
      ?.join(", ") ||
    appData?.alternateNumber ||
    appData?.altMobileNumber ||
    additionalDetails?.alternateNumber;

  // ---------- Address ----------
  const address = appData?.addresses?.[0] || {};
  const addressAdditional = address?.additionalDetail || {};
  const propertyLocation = appData?.propertyLocation || {};

  const propertyId = appData?.propertyId || propertyLocation?.propertyId;
  const pincode = address?.pincode || propertyLocation?.pincode;
  const city = address?.city || propertyLocation?.city || appData?.tenantId;
  const localityRaw = addressAdditional?.locality || propertyLocation?.locality;
  const localityText = typeof localityRaw === "string" ? t(localityRaw) : localityRaw?.name ? t(localityRaw.name) : null;
  const street = addressAdditional?.streetName || propertyLocation?.streetName;
  const houseNo = addressAdditional?.houseNo || propertyLocation?.houseNo;
  const buildingName = addressAdditional?.houseName || propertyLocation?.houseName;
  const addressLine1 = address?.address1 || propertyLocation?.addressline1;
  const addressLine2 = address?.address2 || propertyLocation?.addressline2;
  const landmark = addressAdditional?.landmark || propertyLocation?.landmark;

  // ---------- Garbage Specs ----------
  const specs = appData?.grbgCollectionUnits?.[0] || {};
  const garbageSpec = appData?.garbageSpecification || {};
  const oldGarbageId = appData?.grbgOldDetails?.oldGarbageId || garbageSpec?.oldGarbageId;
  const typeOfCollection = specs?.unitType || garbageSpec?.typeOfCollection;
  const ownerOrTenant = specs?.ownerType || garbageSpec?.propertyOwnerType;
  const category = specs?.category || garbageSpec?.category;
  const subCategory = specs?.subCategory || garbageSpec?.subCategory;
  const subCategoryType = specs?.subCategoryType || garbageSpec?.subCategoryType;
  const no_of_units = specs?.no_of_units || garbageSpec?.no_of_units;
  const specialCategory = specs?.specialCategory || garbageSpec?.specialCategory;
  const isInheritance = specs?.isInheritance || garbageSpec?.isInheritance;
  const specName = garbageSpec?.name || appData?.name;
  const specPhone = garbageSpec?.phoneNumber || appData?.mobileNumber;
  const specGender = garbageSpec?.gender || appData?.gender;
  const specEmail = garbageSpec?.email || appData?.emailId;
  // ---------- Render ----------
  return (
    <React.Fragment>
      <div style={{ maxWidth: "960px", marginLeft: 0, marginRight: "auto" }}>
        {/* Top Header with Title & Interactive Download Button */}
        <div className="cardHeaderWithOptions mb-4 flex flex-row items-center justify-between gap-3 w-full">
          <Header styles={{ fontSize: "28px", margin: 0 }}>{t("GC_APPLICATION_DETAILS")}</Header>
          
          {downloadOptions.length > 0 && (
            <div className="gc-download-dropdown-wrap">
              <button
                type="button"
                onClick={() => setShowOptions(!showOptions)}
                className="gc-download-trigger-btn"
              >
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                </svg>
                <span>{t("CS_COMMON_DOWNLOAD") || "Download"}</span>
                <svg
                  className={`w-3.5 h-3.5 transition-transform duration-200 ${showOptions ? "rotate-180" : ""}`}
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                </svg>
              </button>

              {showOptions && (
                <div className="gc-download-menu">
                  {downloadOptions.map((opt, idx) => (
                    <button
                      key={`download-opt-${idx}`}
                      type="button"
                      onClick={() => {
                        opt.onClick();
                        setShowOptions(false);
                      }}
                      className="gc-download-menu-item"
                    >
                      <span className="gc-download-menu-item-icon">
                        <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
                        </svg>
                      </span>
                      <span>{opt.label}</span>
                    </button>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>

        <Card className="gc-details-card">
          {/* Section 1: Application Summary */}
          <div className="gc-details-section">
            <div className="gc-section-title">
              <span className="gc-section-title-icon">
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                </svg>
              </span>
              <span>{t("GC_APPLICATION_SUMMARY")}</span>
            </div>
            <div className="gc-details-grid">
              <div className="gc-detail-item">
                <span className="gc-detail-label">{t("GC_APPLICATION_NUMBER_LABEL")}</span>
                <span className="gc-detail-value">{appNo || t("CS_NA")}</span>
              </div>
              <div className="gc-detail-item">
                <span className="gc-detail-label">{t("GC_APPLICATION_STATUS_LABEL")}</span>
                <span className="gc-detail-value font-bold text-emerald-700">
                  {appStatus ? t(`GC_STATUS_${appStatus}`) : t("CS_NA")}
                </span>
              </div>
              {dueDate && (
                <div className="gc-detail-item">
                  <span className="gc-detail-label">{t("GC_DUE_DATE")}</span>
                  <span className="gc-detail-value text-amber-700">{dueDate}</span>
                </div>
              )}
            </div>
          </div>

          {/* Section 2: Applicant Details */}
          <div className="gc-details-section">
            <div className="gc-section-title">
              <span className="gc-section-title-icon">
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                </svg>
              </span>
              <span>{t("ES_APPLICANT_DETAILS")}</span>
            </div>
            <div className="gc-details-grid">
              <div className="gc-detail-item">
                <span className="gc-detail-label">{t("GC_APPLICANT_NAME")}</span>
                <span className="gc-detail-value">{ownerNames || t("CS_NA")}</span>
              </div>
              <div className="gc-detail-item">
                <span className="gc-detail-label">{t("GC_MOBILE_NUMBER")}</span>
                <span className="gc-detail-value">{mobileNumbers || t("CS_NA")}</span>
              </div>
              {altMobileNumbers && altMobileNumbers !== t("CS_NA") && (
                <div className="gc-detail-item">
                  <span className="gc-detail-label">{t("GC_ALT_MOBILE_NUMBER")}</span>
                  <span className="gc-detail-value">{altMobileNumbers}</span>
                </div>
              )}
              {emails && emails !== t("CS_NA") && (
                <div className="gc-detail-item">
                  <span className="gc-detail-label">{t("GC_EMAIL_ID")}</span>
                  <span className="gc-detail-value">{emails}</span>
                </div>
              )}
            </div>
          </div>

          {/* Section 3: Property Location Details */}
          <div className="gc-details-section">
            <div className="gc-section-title">
              <span className="gc-section-title-icon">
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                </svg>
              </span>
              <span>{t("GC_PROPERTY_LOCATION_DETAILS")}</span>
            </div>
            <div className="gc-details-grid">
              {propertyId && (
                <div className="gc-detail-item">
                  <span className="gc-detail-label">{t("GC_PROPERTY_ID")}</span>
                  <span className="gc-detail-value">{propertyId}</span>
                </div>
              )}
              {pincode && (
                <div className="gc-detail-item">
                  <span className="gc-detail-label">{t("GC_PINCODE")}</span>
                  <span className="gc-detail-value">{pincode}</span>
                </div>
              )}
              {city && (
                <div className="gc-detail-item">
                  <span className="gc-detail-label">{t("GC_CITY")}</span>
                  <span className="gc-detail-value">{t(city)}</span>
                </div>
              )}
              {localityText && (
                <div className="gc-detail-item">
                  <span className="gc-detail-label">{t("GC_LOCALITY")}</span>
                  <span className="gc-detail-value">{localityText}</span>
                </div>
              )}
              {street && (
                <div className="gc-detail-item">
                  <span className="gc-detail-label">{t("GC_STREET_NAME")}</span>
                  <span className="gc-detail-value">{street}</span>
                </div>
              )}
              {houseNo && (
                <div className="gc-detail-item">
                  <span className="gc-detail-label">{t("GC_HOUSE_NO")}</span>
                  <span className="gc-detail-value">{houseNo}</span>
                </div>
              )}
              {buildingName && (
                <div className="gc-detail-item">
                  <span className="gc-detail-label">{t("GC_BUILDING_NAME")}</span>
                  <span className="gc-detail-value">{buildingName}</span>
                </div>
              )}
              {addressLine1 && (
                <div className="gc-detail-item">
                  <span className="gc-detail-label">{t("GC_ADDRESS_LINE1")}</span>
                  <span className="gc-detail-value">{addressLine1}</span>
                </div>
              )}
              {addressLine2 && (
                <div className="gc-detail-item">
                  <span className="gc-detail-label">{t("GC_ADDRESS_LINE2")}</span>
                  <span className="gc-detail-value">{addressLine2}</span>
                </div>
              )}
              {landmark && (
                <div className="gc-detail-item">
                  <span className="gc-detail-label">{t("GC_LANDMARK")}</span>
                  <span className="gc-detail-value">{landmark}</span>
                </div>
              )}
            </div>
          </div>

          {/* Section 4: Garbage Specifications */}
          <div className="gc-details-section">
            <div className="gc-section-title">
              <span className="gc-section-title-icon">
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                </svg>
              </span>
              <span>{t("GC_GARBAGE_SPECIFICATIONS")}</span>
            </div>
            <div className="gc-details-grid">
              {oldGarbageId && (
                <div className="gc-detail-item">
                  <span className="gc-detail-label">{t("GC_OLD_GARBAGE_ID")}</span>
                  <span className="gc-detail-value">{oldGarbageId}</span>
                </div>
              )}
              {typeOfCollection && (
                <div className="gc-detail-item">
                  <span className="gc-detail-label">{t("GC_TYPE_OF_COLLECTION")}</span>
                  <span className="gc-detail-value">{t(typeOfCollection)}</span>
                </div>
              )}
              {ownerOrTenant && (
                <div className="gc-detail-item">
                  <span className="gc-detail-label">{t("GC_OWNER_OR_TENANT")}</span>
                  <span className="gc-detail-value">{t(ownerOrTenant)}</span>
                </div>
              )}
              {category && (
                <div className="gc-detail-item">
                  <span className="gc-detail-label">{t("GC_CATEGORY")}</span>
                  <span className="gc-detail-value">{t(category)}</span>
                </div>
              )}
              {subCategory && (
                <div className="gc-detail-item">
                  <span className="gc-detail-label">{t("GC_SUB_CATEGORY")}</span>
                  <span className="gc-detail-value">{t(subCategory)}</span>
                </div>
              )}
              {subCategoryType && (
                <div className="gc-detail-item">
                  <span className="gc-detail-label">{t("GC_SUB_CATEGORY_TYPE")}</span>
                  <span className="gc-detail-value">{t(subCategoryType)}</span>
                </div>
              )}
              {multiUnits.includes(typeOfCollection) && no_of_units && (
                <div className="gc-detail-item">
                  <span className="gc-detail-label">{t("GC_NO_OF_UNITS")}</span>
                  <span className="gc-detail-value">{no_of_units}</span>
                </div>
              )}
              {specialCategory && (
                <div className="gc-detail-item">
                  <span className="gc-detail-label">{t("GC_SPECIAL_CATEGORY")}</span>
                  <span className="gc-detail-value">{t(specialCategory)}</span>
                </div>
              )}
              <div className="gc-detail-item">
                <span className="gc-detail-label">{t("GC_IS_INHERITANCE")}</span>
                <span className="gc-detail-value">{isInheritance ? t("YES") : t("NO")}</span>
              </div>
            </div>
          </div>

          {/* Section 5: Documents */}
          {docs.length > 0 && (
            <div className="gc-details-section">
              <div className="gc-section-title">
                <span className="gc-section-title-icon">
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13" />
                  </svg>
                </span>
                <span>{t("GC_GARBAGE_DOCUMENTS")}</span>
              </div>
              <div className="chb-doc-card">
                {docs.map((doc, index) => (
                  <div key={`doc-${index}`} className="chb-doc-item">
                    <div>
                      <CardSectionHeader>{t("GC_" + (doc?.documentType?.split(".").slice(0, 2).join("_")))}</CardSectionHeader>
                      <GCDocuments value={docs} Code={doc?.documentType} index={index} />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Section 6: Workflow Timeline */}
          <div className="pt-2">
            <GCWFApplicationTimeline application={appData} />
          </div>
        </Card>

        {appStatus === "EDIT_APPLICATION" && (
          <ActionBar>
            <SubmitBar
              label={t("GC_EDIT_APPLICATION")}
              onSubmit={() =>
                navigate(`/upyog-ui/citizen/gc/edit/${encodeURIComponent(appNo)}`)
              }
            />
          </ActionBar>
        )}

        {appStatus === "PENDING_FOR_PAYMENT" && (
          <ActionBar>
            <SubmitBar label={t("CS_APPLICATION_DETAILS_MAKE_PAYMENT")} onSubmit={handleMakePayment} />
          </ActionBar>
        )}

        {selectedAction && (
          <Modal
            headerBarMain={<h1 className="heading-m">{t(`WF_EMPLOYEE_GC_${selectedAction.action}`)}</h1>}
            headerBarEnd={<CloseBtn onClick={() => setSelectedAction(null)} />}
            actionCancelLabel={t("CS_COMMON_CANCEL")}
            actionCancelOnSubmit={() => setSelectedAction(null)}
            actionSaveLabel={t("CS_COMMON_SUBMIT")}
            actionSaveOnSubmit={submitWorkflowAction}
          >
            <Card style={{ padding: "0px", margin: "0px", boxShadow: "none" }}>
              <CardText>{t("WF_COMMON_COMMENTS")}</CardText>
              <TextArea value={comments} onChange={(e) => setComments(e.target.value)} />
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