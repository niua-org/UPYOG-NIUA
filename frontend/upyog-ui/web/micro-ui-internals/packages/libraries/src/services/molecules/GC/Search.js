import { GCServices } from "../../elements/GC";

/**
 * GCSearch
 *
 * Provides helper methods to search and format Garbage Collection (GC) application data
 * similar to the CHB module's Search.js implementation.
 */
export const GCSearch = {
  all: async (tenantId, filters = {}) => {
    const response = await GCServices.search({ tenantId, filters });
    return response?.garbageApplications || [];
  },

  application: async (tenantId, filters = {}) => {
    const response = await GCServices.search({ tenantId, filters });
    return response?.garbageApplications?.[0] || null;
  },

  BookingDetails: ({ application, garbageApplications, garbageAccounts, t }) => {
    const app = application || garbageApplications || garbageAccounts || {};
    const address = app?.propertyLocation || {};
    const collectionUnit = app?.garbageSpecification || {};
    const applicant = app?.applicantDetails?.[0] || {};

    const appNo = app?.applicationNo || t("CS_NA");
    const appStatus = app?.status || t("CS_NA");
    const connStatus = app?.connectionStatus || null;
    const propertyType = collectionUnit?.propertyType || "OWNER";
    const floorNo = collectionUnit?.floorNo;

    return [
      {
        title: "GC_APPLICATION_DETAILS",
        asSectionHeader: true,
        values: [
          { title: "GC_APPLICATION_NUMBER_LABEL", value: appNo },
          ...(app?.garbageNo ? [{ title: "GC_GARBAGE_NUMBER_LABEL", value: app.garbageNo }] : []),
          { title: "GC_APPLICATION_STATUS_LABEL", value: appStatus ? t(`GC_STATUS_${appStatus}`) : t("CS_NA") },
          ...(connStatus ? [{ title: "GC_CONNECTION_STATUS", value: t(`GC_STATUS_${connStatus}`) || connStatus }] : []),
          { title: "GC_PROPERTY_TYPE", value: propertyType },
          ...(floorNo ? [{ title: "GC_FLOOR_UNIT", value: floorNo === "0" ? t("GC_FLOOR_GROUND") || "Ground Floor" : `${floorNo} Floor` }] : []),
          ...(app?.dueDate ? [{ title: "GC_DUE_DATE", value: app.dueDate }] : []),
          ...(app?.paymentStatus ? [{ title: "GC_PAYMENT_STATUS", value: app.paymentStatus }] : []),
        ],
      },
      {
        title: "ES_APPLICANT_DETAILS",
        asSectionHeader: true,
        values: [
          { title: "GC_APPLICANT_NAME", value: applicant?.name || t("CS_NA") },
          { title: "GC_MOBILE_NUMBER", value: applicant?.mobileNumber || t("CS_NA") },
          { title: "GC_ALT_MOBILE_NUMBER", value: applicant?.alternateNumber || t("CS_NA") },
          { title: "GC_EMAIL_ID", value: applicant?.emailId || t("CS_NA") },
        ],
      },
      {
        title: "GC_PROPERTY_LOCATION_DETAILS",
        asSectionHeader: true,
        values: [
          { title: "GC_PROPERTY_ID", value: app?.propertyId || address?.propertyId || t("CS_NA") },
          { title: "GC_PINCODE", value: address?.pincode || t("CS_NA") },
          { title: "GC_CITY", value: address?.city ? t(address.city) : t("CS_NA") },
          { title: "GC_LOCALITY", value: address?.locality ? t(address.locality) : t("CS_NA") },
          { title: "GC_STREET_NAME", value: address?.streetName || t("CS_NA") },
          { title: "GC_HOUSE_NO", value: address?.houseNo || t("CS_NA") },
          { title: "GC_BUILDING_NAME", value: address?.houseName || t("CS_NA") },
          { title: "GC_ADDRESS_LINE1", value: address?.addressline1 || t("CS_NA") },
          { title: "GC_ADDRESS_LINE2", value: address?.addressline2 || t("CS_NA") },
          { title: "GC_LANDMARK", value: address?.landmark || t("CS_NA") },
        ],
      },
      {
        title: "GC_GARBAGE_SPECIFICATIONS",
        asSectionHeader: true,
        values: [
          { title: "GC_OLD_GARBAGE_ID", value: app?.oldGarbageNo || collectionUnit?.oldGarbageNo || t("CS_NA") },
          { title: "GC_TYPE_OF_COLLECTION", value: collectionUnit?.collectionType ? t(collectionUnit.collectionType) : t("CS_NA") },
          { title: "GC_OWNER_OR_TENANT", value: propertyType },
          { title: "GC_CATEGORY", value: collectionUnit?.category ? t(collectionUnit.category) : t("CS_NA") },
          { title: "GC_SUB_CATEGORY", value: collectionUnit?.subCategory ? t(collectionUnit.subCategory) : t("CS_NA") },
          { title: "GC_SUB_CATEGORY_TYPE", value: collectionUnit?.subCategoryType ? t(collectionUnit.subCategoryType) : t("CS_NA") },
          ...["HOUSEHOLD_MULTI_COLLECTION", "COMMERCIAL_MULTI_COLLECTION", "MIX_PROPERTY"].includes(collectionUnit?.collectionType)
            ? [{ title: "GC_NO_OF_UNITS", value: collectionUnit?.no_of_units || t("CS_NA") }]
            : [],
          { title: "GC_SPECIAL_CATEGORY", value: collectionUnit?.specialCategory ? t(collectionUnit.specialCategory) : t("CS_NA") },
          { title: "GC_IS_INHERITANCE", value: collectionUnit?.isInheritance ? t("YES") : t("NO") },
        ],
      },
      {
        title: "GC_GARBAGE_DOCUMENTS",
        additionalDetails: {
          documents: [
            {
              values: app?.documents?.map((document) => ({
                title: `GC_${document?.documentType?.split(".").slice(0, 5).join("_")}`,
                documentType: document?.documentType,
                documentUid: document?.documentUid,
                fileStoreId: document?.fileStoreId,
                status: document?.status,
              })) || [],
            },
          ],
        },
      },
    ];
  },

  applicationDetails: async (t, tenantId, applicationNo, userType, args) => {
    const response = await GCSearch.application(tenantId, { applicationNumber: [applicationNo], ...args });

    return {
      tenantId: response?.tenantId || tenantId,
      applicationDetails: GCSearch.BookingDetails({ application: response, t }),
      applicationData: response,
      transformToAppDetailsForEmployee: GCSearch.BookingDetails,
    };
  },
};

export default GCSearch;
