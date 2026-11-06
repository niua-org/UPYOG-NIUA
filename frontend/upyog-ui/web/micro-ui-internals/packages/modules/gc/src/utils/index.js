import { CloseSvg } from "@nudmcdgnpm/digit-ui-react-components";

/**
 * Determines if the back button should be hidden based on the current URL
 * matching screen paths in the config array.
 */
export const shouldHideBackButton = (config = []) => {
  return config.filter((key) => window.location.href.includes(key.screenPath)).length > 0;
};

export const checkForNotNull = (value = "") => {
  return value && value !== null && value !== undefined && value !== "";
};

export const checkForNA = (value = "") => {
  return checkForNotNull(value) ? value : "NA";
};

export const stringReplaceAll = (str = "", searcher = "", replaceWith = "") => {
  if (searcher === "") return str;
  while (str.includes(searcher)) {
    str = str.replace(searcher, replaceWith);
  }
  return str;
};

export const pdfDownloadLink = (documents = {}, fileStoreId = "", format = "") => {
  let downloadLink = documents[fileStoreId] || "";
  let differentFormats = downloadLink?.split(",") || [];
  let fileURL = "";

  differentFormats.length > 0 &&
    differentFormats.forEach((link) => {
      if (!link.includes("large") && !link.includes("medium") && !link.includes("small")) {
        fileURL = link;
      }
    });

  return fileURL;
};

/**
 * Main Multi-Application Payload Converter as defined in UPYOG Garbage Service HLD Section 2.5 & 9.2:
 * Converts wizard form session data into canonical `GarbageRequest` containing:
 * - 1 Owner Garbage Application
 * - N Individual Tenant Garbage Applications (one per added tenant with floorNo and tenant mobile number)
 */
export const GCDataConvert = (data) => {
  const user = Digit.UserService.getUser()?.info || {};
  const tenantId =
    data?.tenantId ||
    data?.gcpropertylocdetails?.city?.code ||
    Digit.ULBService.getCitizenCurrentTenant(true) ||
    Digit.ULBService.getCurrentTenantId();

  const propertyLoc = data?.gcpropertylocdetails || {};
  const addressLoc = data?.address || propertyLoc;
  const specs = data?.gcspecifications || data?.GCSpecifications || {};
  const docData = data?.gcdocuments || {};

  // Structured Property Details JSONB snapshot as per HLD 2.1 & 9.1
  const propertyLocation = {
    propertyId: propertyLoc?.propertyId || null,
    houseNo: addressLoc?.houseNo || propertyLoc?.houseNo || "",
    houseName: addressLoc?.houseName || propertyLoc?.houseName || "",
    streetName: addressLoc?.streetName || propertyLoc?.streetName || "",
    addressline1: addressLoc?.addressLine1 || addressLoc?.addressline1 || propertyLoc?.addressline1 || "",
    addressline2: addressLoc?.addressLine2 || addressLoc?.addressline2 || propertyLoc?.addressline2 || "",
    landmark: addressLoc?.landmark || propertyLoc?.landmark || "",
    city: addressLoc?.city?.code || addressLoc?.city || propertyLoc?.city?.code || propertyLoc?.city || tenantId,
    locality: addressLoc?.locality?.code || addressLoc?.locality || propertyLoc?.locality?.code || propertyLoc?.locality || "",
    pincode: addressLoc?.pincode || propertyLoc?.pincode || "",
  };

  // Documents
  const ownerDocs = (docData?.ownerDocuments || docData?.documents || []).map((doc) => ({
    documentType: doc?.documentType,
    fileStoreId: doc?.fileStoreId,
    documentUid: doc?.documentUid || doc?.fileStoreId,
  }));

  const tenantDocsMap = docData?.tenantDocuments || {};

  const ownerDetails = data?.owner || {};
  const ownerSource = propertyLoc?.ownerInfo || propertyLoc?.owners?.[0] || {};
  const ownerName = ownerDetails?.applicantName || ownerSource?.name || user?.name || "";
  const ownerMobile = ownerDetails?.mobileNumber || ownerSource?.mobileNumber || user?.mobileNumber || "";
  const ownerEmail = ownerDetails?.emailId || ownerSource?.emailId || user?.emailId || "";
  const ownerGender = (typeof ownerDetails?.gender === "object" ? ownerDetails?.gender?.code : ownerDetails?.gender) || ownerSource?.gender || "";
  const ownerGuardian = ownerDetails?.guardianName || ownerSource?.fatherOrHusbandName || ownerSource?.guardianName || "";
  const ownerRelationship = (typeof ownerDetails?.relationShipType === "object" ? ownerDetails?.relationShipType?.code || ownerDetails?.relationShipType?.name : ownerDetails?.relationShipType) || ownerSource?.relationship || "";
  const ownerAltNumber = ownerDetails?.alternateNumber || ownerSource?.altContactNumber || ownerSource?.alternatemobilenumber || "";

  // 1. Owner Application Object
  const ownerApp = {
    tenantId: tenantId,
    propertyId: propertyLocation.propertyId,
    ownerUuid: user?.uuid || ownerSource?.uuid || null,
    applicantDetails: [
      {
        name: ownerName,
        mobileNumber: ownerMobile,
        emailId: ownerEmail,
        gender: ownerGender,
        guardianName: ownerGuardian,
        relationship: ownerRelationship,
        alternateNumber: ownerAltNumber,
      },
    ],
    applicationNo: null,
    oldGarbageNo: specs?.oldGarbageId || null,
    garbageNo: null,
    status: "APPLICATION_CREATED",
    businessService: "garbage-service",
    channel: user?.type === "EMPLOYEE" ? "EMPLOYEE" : "CITIZEN",
    propertyLocation: propertyLocation,
    garbageSpecification: {
      oldGarbageNo: specs?.oldGarbageId || null,
      collectionType: specs?.typeOfCollection?.code || specs?.typeOfCollection || "HOUSEHOLD_SINGLE_COLLECTION",
      propertyType: "OWNER",
      category: specs?.category?.code || specs?.category || "RESIDENTIAL",
      subCategory: specs?.subCategory?.code || specs?.subCategory || "HOUSEHOLD",
      subCategoryType: specs?.subCategoryType?.code || specs?.subCategoryType || "AREA_LESS_THAN_2000_SQ_FEET",
      no_of_units: Number(specs?.no_of_units) || 1,
      specialCategory: specs?.specialCategory?.code || specs?.specialCategory || "NONE",
      floorNo: "0",
      isInheritance: specs?.isInheritance || false,
    },
    documents: ownerDocs,
    workflow: { action: "APPLY" },
  };

  // 2. Individual Applications per Tenant
  const tenants = specs?.tenants || [];
  const tenantApps = tenants.map((tenant, idx) => {
    const rawTDocs = tenantDocsMap[tenant.id] || [];
    const tDocs = rawTDocs.map((doc) => ({
      documentType: doc?.documentType,
      fileStoreId: doc?.fileStoreId,
      documentUid: doc?.documentUid || doc?.fileStoreId,
    }));

    return {
      tenantId: tenantId,
      propertyId: propertyLocation.propertyId,
      floorNo: String(tenant.floorNo || idx + 1),
      ownerUuid: user?.uuid || propertyLoc?.ownerInfo?.uuid || null,
      tenantUuid: tenant?.tenantUuid || null,
      tenantDetails: {
        name: tenant.name,
        mobileNumber: tenant.mobileNumber,
        emailId: tenant.email,
        floorNo: String(tenant.floorNo || idx + 1),
      },
      applicantDetails: [
        {
          name: tenant.name,
          mobileNumber: tenant.mobileNumber,
          emailId: tenant.email,
        },
      ],
      applicationNo: null,
      status: "APPLICATION_CREATED",
      connectionStatus: "ACTIVE",
      businessService: "garbage-service",
      channel: user?.type === "EMPLOYEE" ? "EMPLOYEE" : "CITIZEN",
      propertyLocation: propertyLocation,
      garbageSpecification: {
        collectionType: specs?.typeOfCollection?.code || specs?.typeOfCollection || "HOUSEHOLD_SINGLE_COLLECTION",
        propertyType: "TENANT",
        category: specs?.category?.code || specs?.category || "RESIDENTIAL",
        subCategory: specs?.subCategory?.code || specs?.subCategory || "HOUSEHOLD",
        subCategoryType: specs?.subCategoryType?.code || specs?.subCategoryType || "AREA_LESS_THAN_2000_SQ_FEET",
        no_of_units: 1,
        specialCategory: "NONE",
        floorNo: String(tenant.floorNo || idx + 1),
        isInheritance: false,
      },
      documents: tDocs,
      workflow: { action: "APPLY" },
    };
  });

  // Attach nested child garbage applications to owner parent application
  ownerApp.childGarbageApplications = tenantApps;

  // Canonical payload sends parent application with nested children
  return {
    garbageApplications: [ownerApp],
  };
};

export const getGCStatusOptions = (t) => [
  { i18nKey: t("GC_STATUS_INITIATED"), value: t("GC_STATUS_INITIATED"), code: "INITIATED" },
  { i18nKey: t("GC_STATUS_PENDING_FOR_VERIFICATION"), value: t("GC_STATUS_PENDING_FOR_VERIFICATION"), code: "PENDING_FOR_VERIFICATION" },
  { i18nKey: t("GC_STATUS_PENDING_FOR_APPROVAL"), value: t("GC_STATUS_PENDING_FOR_APPROVAL"), code: "PENDING_FOR_APPROVAL" },
  { i18nKey: t("GC_STATUS_EDIT_APPLICATION"), value: t("GC_STATUS_EDIT_APPLICATION"), code: "EDIT_APPLICATION" },
  { i18nKey: t("GC_STATUS_APPROVED"), value: t("GC_STATUS_APPROVED"), code: "APPROVED" },
  { i18nKey: t("GC_STATUS_ACTIVE"), value: t("GC_STATUS_ACTIVE"), code: "ACTIVE" },
  { i18nKey: t("GC_STATUS_DISCONNECTED"), value: t("GC_STATUS_DISCONNECTED"), code: "DISCONNECTED" },
  { i18nKey: t("GC_STATUS_PERMANENTLY_DISCONNECTED"), value: t("GC_STATUS_PERMANENTLY_DISCONNECTED"), code: "PERMANENTLY_DISCONNECTED" },
  { i18nKey: t("GC_STATUS_REJECTED"), value: t("GC_STATUS_REJECTED"), code: "REJECTED" },
  { i18nKey: t("GC_STATUS_PENDING_FOR_PAYMENT"), value: t("GC_STATUS_PENDING_FOR_PAYMENT"), code: "PENDING_FOR_PAYMENT" },
  { i18nKey: t("GC_STATUS_PAID"), value: t("GC_STATUS_PAID"), code: "PAID" },
];

export const downloadGCReceipt = async (tenantId, payments) => {
  const paymentList = Array.isArray(payments) ? payments : [payments];
  let response;
  if (paymentList[0]?.fileStoreId) {
    response = { filestoreIds: [paymentList[0].fileStoreId] };
  } else {
    response = await Digit.PaymentService.generatePdf(tenantId, { Payments: paymentList }, "garbage-service-receipt");
  }
  const fileStore = await Digit.PaymentService.printReciept(tenantId, { fileStoreIds: response.filestoreIds[0] });
  window.open(fileStore[response.filestoreIds[0]], "_blank");
};

export const downloadGCAcknowledgement = async (application, tenants, t) => {
  const tenantInfo = tenants?.find((tenant) => tenant.code === application?.tenantId);
  const getGcAcknowledgementData = (await import("../getGcAcknowledgementData")).default;
  const ackData = await getGcAcknowledgementData({ garbageApplications: [application] }, tenantInfo, t);
  Digit.Utils.pdf.generate(ackData);
};

export const GCAPIToFormData = (application, params) => {
  const updatedApplication = { ...application };

  const address = application?.addresses?.[0] || application?.propertyLocation || {};
  const addressAdditional = address?.additionalDetail || {};
  const specs = application?.grbgCollectionUnits?.[0] || application?.garbageSpecification || {};

  updatedApplication.gcpropertylocdetails = {
    propertyId: application?.propertyId,
    city: address?.city,
    pincode: address?.pincode,
    addressline1: address?.address1 || address?.addressline1,
    addressline2: address?.address2 || address?.addressline2,
    locality: addressAdditional?.locality || address?.locality,
    houseNo: addressAdditional?.houseNo || address?.houseNo,
    houseName: addressAdditional?.houseName || address?.houseName,
    streetName: addressAdditional?.streetName || address?.streetName,
    landmark: addressAdditional?.landmark || address?.landmark,
  };

  updatedApplication.gcspecifications = {
    typeOfCollection: specs?.unitType || specs?.collectionType,
    category: specs?.category,
    subCategory: specs?.subCategory,
    subCategoryType: specs?.subCategoryType,
    specialCategory: specs?.specialCategory,
    oldGarbageId: specs?.oldGarbageId || application?.oldGarbageNo,
    no_of_units: Number(specs?.no_of_units || 1),
    isInheritance: specs?.isInheritance || false,
    tenants: application?.tenants || [],
  };

  updatedApplication.gcdocuments = {
    documents: application?.documents || [],
  };

  return updatedApplication;
};

export const multiUnits = ["HOUSEHOLD_MULTI_COLLECTION", "COMMERCIAL_MULTI_COLLECTION", "MIX_PROPERTY"];