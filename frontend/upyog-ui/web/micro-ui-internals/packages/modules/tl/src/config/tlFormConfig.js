/**
 * tlFormConfig.js
 *
 * Local behavior overrides for TradeLicense.NewApplication /
 * AccordionNewApplication (DynamicFormStep — Estate NewRegistration pattern).
 *
 * AccordionNewApplication.json drives the citizen create accordion
 * (`navigation.pattern = "accordion"`) via DynamicFormAccordionStep.
 *
 * Field structure / labels / options live in JSON (or MDMS); this file supplies
 * what JSON cannot own (paths, payload keys, JS hooks) — including TL billing-slab
 * validation overlays (kept out of the workbench schema).
 *
 * @see mergeRouteConfig
 * @see DynamicFormAccordionStep
 * @see DynamicFormStep
 * @see ./tlBillingSlab.js
 * @see ./AccordionNewApplication.json
 */

const CREATE_PROPERTY_PATH =
  "/upyog-ui/citizen/pt/property/new-application/info";

/**
 * Ensure createNewPath works even when remote MDMS is stale.
 * Billing-slab rules live here (not in AccordionNewApplication.json).
 */
const tlFormFieldOverlays = [
  {
    key: "TL_HAS_PROPERTY_ID",
    field: {
      code: "TL_DO_YOU_HAVE_PROPERTY_ID_OR_MOBILE",
      name: "hasPropertyId",
    },
    messages: {
      labelDefault: "Do you have a property ID or mobile number?",
    },
  },
  {
    key: "TL_PROPERTY_ID",
    field: {
      code: "TL_SEARCH_PROPERTY_OR_MOBILE",
      name: "searchPropertyId",
      type: "text",
      searchCard: true,
      searchButton: true,
      createNewPath: CREATE_PROPERTY_PATH,
      placeholder: "TL_ENTER_PROPERTY_ID_OR_MOBILE",
      placeholderDefault: "Property ID or mobile number",
      resultLabel: "TL_PROPERTY_ID",
      searchButtonLabel: "CS_COMMON_SEARCH",
      selectLabel: "CS_COMMON_SELECT",
      notFoundLabel: "TL_PROPERTY_NOT_FOUND",
      notFoundLabelDefault: "No property found for this property ID or mobile number",
      createNewLabel: "TL_CREATE_NEW_PROPERTY",
      // Shown when the localization key has no entry yet.
      createNewLabelDefault: "Create new property",
    },
    messages: {
      error: "TL_PROPERTY_SEARCH_REQUIRED",
      errorDefault: "Enter a property ID or mobile number",
      labelDefault: "Property ID or mobile number",
    },
  },
  {
    key: "TL_CREATE_PROPERTY_REDIRECT",
    field: {
      name: "createPropertyRedirect",
      createNewPath: CREATE_PROPERTY_PATH,
    },
  },
  {
    // Local copy so an MDMS NewApplication cannot drop the financial-year limit.
    key: "TL_COMMENCEMENT_DATE",
    field: {
      name: "commencementDate",
      withinFinancialYear: true,
    },
    validation: {
      financialYear: true,
    },
    messages: {
      financialYear: "TL_COMMENCEMENT_DATE_FINANCIAL_YEAR",
      financialYearDefault:
        "Commencement date must be in the current financial year and cannot be after today",
    },
  },
  {
    key: "TL_TRADE_CATEGORY",
    field: {
      name: "tradecategory",
      dataSource: { i18nKeyPrefix: "TRADELICENSE_TRADETYPE" },
    },
  },
  {
    key: "TL_TRADE_TYPE",
    field: {
      name: "tradetype",
      dataSource: {
        i18nKeyPrefix: "TRADELICENSE_TRADETYPE",
        sortOptions: "label",
      },
    },
  },
  {
    key: "TL_TRADE_SUBTYPE",
    field: {
      name: "tradesubtype",
      dataSource: { leafI18nPrefix: "TL", sortOptions: "label" },
    },
    validation: {
      billingSlabFound: true,
    },
    messages: {
      billingSlabFound: "TL_BILLING_SLAB_NOT_FOUND_FOR_COMB",
    },
  },
  {
    key: "TL_TRADE_UOM_VALUE",
    field: { name: "uom" },
    validation: {
      min: { fromField: "fromUom" },
      max: { fromField: "toUom" },
    },
    messages: {
      min: "TL_FILL_CORRECT_UOM_VALUE",
      max: "TL_FILL_CORRECT_UOM_VALUE",
      minDefault: "Enter a UOM value within the allowed range",
      maxDefault: "Enter a UOM value within the allowed range",
    },
  },
  {
    key: "TL_ACCESSORY_CATEGORY",
    field: {
      name: "accessory",
      dataSource: {
        i18nKeyPrefix: "TRADELICENSE_ACCESSORIESCATEGORY",
        sortOptions: "label",
      },
    },
    validation: {
      billingSlabFound: true,
    },
    messages: {
      billingSlabFound: "TL_BILLING_SLAB_NOT_FOUND_FOR_COMB",
    },
  },
  {
    key: "TL_ACCESSORY_UOM_VALUE",
    field: { name: "uom" },
    validation: {
      min: { fromField: "fromUom" },
      max: { fromField: "toUom" },
    },
    messages: {
      min: "TL_FILL_CORRECT_UOM_VALUE",
      max: "TL_FILL_CORRECT_UOM_VALUE",
      minDefault: "Enter a UOM value within the allowed range",
      maxDefault: "Enter a UOM value within the allowed range",
    },
  },
];

/**
 * Trade License property gate. Merged over the JSON formGate (local keys win).
 * YES → property ID search; NO → popup then redirect to create property.
 */
const tlFormGate = {
  typeField: "hasPropertyId",
  detailsField: "showTradeDetails",
  searchValue: "YES",
  openValue: null,
  searchField: "searchPropertyId",
  redirectOnValue: "NO",
  redirectPath: CREATE_PROPERTY_PATH,
  redirectModalHeading: "TL_CREATE_NEW_PROPERTY",
  redirectModalMessage: "TL_PROCEED_POST_APPROVAL",
  redirectModalMessageDefault:
    "You will be able to proceed post approval.",
  unlockWhenFields: ["tradeName", "propertyId"],
  preserveOnPrefill: ["structureType", "buildingType", "vehicleType"],
  structureReset: {
    field: "structureType",
    unlockValue: "MOVABLE",
    clearFields: [
      "hasPropertyId",
      "searchPropertyId",
      "propertyId",
      "createPropertyRedirect",
      "buildingType",
      "vehicleType",
      "propertyOwnersReady",
      "isSameAsPropertyOwner",
    ],
  },
};

export default {
  form: tlFormFieldOverlays,
  formGate: tlFormGate,
  payloadKey: "Licenses",
  apiId: "Rainmaker",
  uploadModule: "TL",
  crossFieldValidations: [],
  staticFields: () => ({
    licenseType: "PERMANENT",
  }),
  computedFields: [],
};
