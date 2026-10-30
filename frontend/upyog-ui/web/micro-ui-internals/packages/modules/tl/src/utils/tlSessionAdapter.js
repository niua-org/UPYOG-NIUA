/**
 * tlSessionAdapter.js
 *
 * Bridges DynamicForm flat session (newApplication.Licenses[0]) into the nested
 * shape expected by legacy TL CheckPage + convertToTrade, without rewriting those.
 *
 * Dynamic form remains the source of truth under `newApplication`; nested keys are
 * derived views for check / acknowledgement.
 */
import { coerceRadioFieldValue } from "@nudmcdgnpm/digit-ui-react-components";
import { activePropertyOwners } from "./tlPropertySearch";

const asCodeObj = (value, i18nPrefix) => {
  if (value == null || value === "") return undefined;
  if (typeof value === "object" && value.code != null) {
    const code = String(value.code);
    const key = i18nPrefix
      ? `${i18nPrefix}_${code.replace(/\./g, "_").replace(/-/g, "_")}`
      : code;
    if (
      !i18nPrefix ||
      (value.i18nKey && value.i18nKey !== code && !String(value.i18nKey).includes("."))
    ) {
      return { ...value };
    }
    return { ...value, i18nKey: key };
  }
  const code = String(value);
  return {
    code,
    i18nKey: i18nPrefix
      ? `${i18nPrefix}_${code.replace(/\./g, "_").replace(/-/g, "_")}`
      : code,
  };
};

const TRADE_TYPE_PREFIX = "TRADELICENSE_TRADETYPE";
const STRUCTURE_PREFIX = "COMMON_MASTERS_STRUCTURETYPE";

const pickFirstArrayItem = (value) => {
  if (Array.isArray(value)) return value;
  if (value && typeof value === "object") return [value];
  return [];
};

const isSameAsPropertyOwnerYes = (value) =>
  value === "YES" || value === true || value === "true";

/** User service rejects create when name or mobile is blank (InvalidUserCreateException). */
const ownerDisplayName = (owner) => {
  const name = owner?.name;
  if (name && typeof name === "object") return String(name.name || name.code || "").trim();
  return String(name || "").trim();
};

const ownerMobile = (owner) =>
  String(owner?.mobilenumber || owner?.mobileNumber || "").trim();

/** Privacy masks (asterisks) are not a real name or mobile. */
const isPlainOwnerText = (value) => {
  const text = String(value || "").trim();
  return Boolean(text) && !text.includes("*");
};

const isFilledOwner = (owner) =>
  isPlainOwnerText(ownerDisplayName(owner)) && isPlainOwnerText(ownerMobile(owner));

/** Radio code first, then the check-page `{ code, i18nKey }` shape. */
const radioCode = (value, i18nPrefix) => {
  const code = coerceRadioFieldValue(value);
  if (!code) return undefined;
  return asCodeObj(code, i18nPrefix);
};

const normalizeOwnerRow = (owner) => ({
  ...owner,
  name: ownerDisplayName(owner),
  mobilenumber: ownerMobile(owner),
  gender: radioCode(owner?.gender, "TL_GENDER"),
  relationship: radioCode(owner?.relationship, "TL_RELATIONSHIP"),
});

/** Accordion file fields are { filestoreId, fileName } or a bare id string. */
const asUploadDoc = (file, documentType) => {
  if (file == null || file === "") return undefined;
  if (typeof file === "string") {
    return { fileStoreId: file, filestoreId: file, documentType, fileName: documentType };
  }
  const fileStoreId = file.fileStoreId || file.filestoreId || file.documentuuid || "";
  if (!fileStoreId) return undefined;
  return {
    ...file,
    fileStoreId,
    filestoreId: fileStoreId,
    documentType,
    fileName: file.fileName || file.name || documentType,
  };
};

/**
 * Build ownershipCategory from a PT property when citizen picks same-as-property-owner.
 * Mirrors SelectOwnerShipDetails auto-select from cpt.details.ownershipCategory.
 */
const codeText = (value) => {
  if (value == null || value === "") return "";
  if (typeof value === "object") return String(value.code ?? value.value ?? value.name ?? "");
  return String(value);
};

const ownershipFromProperty = (property) => {
  const rawStr = codeText(property?.ownershipCategory);
  if (!rawStr) {
    return {
      code: "INDIVIDUAL.SINGLEOWNER",
      i18nKey: "COMMON_MASTERS_OWNERSHIPCATEGORY_INDIVIDUAL_SINGLEOWNER",
      isSameAsPropertyOwner: true,
    };
  }
  if (rawStr.includes("INSTITUTIONAL")) {
    const instType = codeText(property?.institution?.type);
    const code =
      instType && !rawStr.endsWith(`.${instType}`) && !rawStr.endsWith(`_${instType}`)
        ? `${rawStr}.${instType}`
        : rawStr;
    return {
      code,
      i18nKey: `COMMON_MASTERS_OWNERSHIPCATEGORY_${code.replace(/\./g, "_")}`,
      isSameAsPropertyOwner: true,
    };
  }
  return {
    code: rawStr,
    i18nKey: `COMMON_MASTERS_OWNERSHIPCATEGORY_${rawStr.replace(/\./g, "_")}`,
    isSameAsPropertyOwner: true,
  };
};

/**
 * Map PT owners → TL CheckPage / convertToTrade owner shape (mobilenumber lowercase).
 */
const ownersFromProperty = (property, ownershipCategory) => {
  const code = ownershipCategory?.code || "";
  const src = activePropertyOwners(property);

  if (code.includes("INSTITUTIONAL")) {
    const first = src[0] || {};
    return [
      {
        name: property?.institution?.nameOfAuthorizedPerson || first.name,
        designation: property?.institution?.designation,
        mobilenumber: first.mobileNumber,
        altContactNumber: first.altContactNumber,
        institutionName: property?.institution?.name,
        fatherOrHusbandName: first.fatherOrHusbandName || first.guardian,
        relationship: radioCode(first.relationship ?? first.relation, "TL_RELATIONSHIP"),
        emailId: first.emailId,
        permanentAddress: first.permanentAddress || first.correspondenceAddress,
        gender: radioCode(first.gender ?? first.user?.gender, "TL_GENDER"),
        subOwnerShipCategory: ownershipCategory,
      },
    ];
  }

  return src.map((ow) => ({
    name: ow?.name,
    mobilenumber: ow?.mobileNumber,
    fatherOrHusbandName: ow?.fatherOrHusbandName || ow?.guardian,
    relationship: radioCode(ow?.relationship ?? ow?.relation, "TL_RELATIONSHIP"),
    gender: radioCode(ow?.gender ?? ow?.user?.gender, "TL_GENDER"),
    emailId: ow?.emailId,
    permanentAddress: ow?.permanentAddress || ow?.correspondenceAddress,
  }));
};

/** Ensure trade-unit / accessory nested dropdown codes survive flatten + display. */
const normalizeUnitRow = (row = {}) => ({
  ...row,
  tradecategory: asCodeObj(row.tradecategory, TRADE_TYPE_PREFIX),
  tradetype: asCodeObj(row.tradetype, TRADE_TYPE_PREFIX),
  tradesubtype: asCodeObj(row.tradesubtype, TRADE_TYPE_PREFIX),
  accessory: asCodeObj(row.accessory, "TRADELICENSE_ACCESSORIESCATEGORY"),
});

const padDatePart = (value) => String(value).padStart(2, "0");

/** License commencement epoch → yyyy-MM-dd for the accordion date field. */
const epochToInputDate = (value) => {
  if (value == null || value === "") return "";
  const numeric = Number(value);
  const date = new Date(Number.isFinite(numeric) ? numeric : value);
  if (Number.isNaN(date.getTime())) return "";
  return `${date.getFullYear()}-${padDatePart(date.getMonth() + 1)}-${padDatePart(date.getDate())}`;
};

/** Application document whose type contains the token, in the file-field shape. */
const documentByType = (documents, token) => {
  const found = (documents || []).find((doc) =>
    String(doc?.documentType || "").toUpperCase().includes(token)
  );
  const fileStoreId = found?.fileStoreId || found?.filestoreId;
  if (!fileStoreId) return null;
  return {
    fileStoreId,
    filestoreId: fileStoreId,
    fileName: found.fileName || found.name || token,
    documentType: found.documentType,
  };
};

/**
 * Existing trade license → flat accordion values (newApplication.Licenses[0]).
 * The renewal edit pencil opens this shape in the same form as a new application.
 *
 * @param {object} license Trade-license search record
 * @returns {object}
 */
export const mapLicenseToAccordionForm = (license = {}) => {
  const detail = license.tradeLicenseDetail || {};
  const structure = String(detail.structureType || "");
  const structureRoot = structure.split(".")[0] || "";
  const additional = detail.additionalDetail || {};
  const propertyId =
    additional.propertyId && additional.propertyId !== "null" ? additional.propertyId : "";
  const sameOwner =
    additional.isSameAsPropertyOwner === true || additional.isSameAsPropertyOwner === "true";
  const accessories = Array.isArray(detail.accessories) ? detail.accessories : [];
  const address = detail.address || {};
  const documents = detail.applicationDocuments || [];

  return {
    structureType: structureRoot,
    buildingType: structureRoot === "IMMOVABLE" ? structure : "",
    vehicleType: structureRoot === "MOVABLE" ? structure : "",
    hasPropertyId: propertyId ? "YES" : "",
    propertyId,
    showTradeDetails: "YES",
    tradeName: license.tradeName || "",
    commencementDate: epochToInputDate(license.commencementDate),
    tradeUnits: (detail.tradeUnits || []).map((unit) => {
      const parts = String(unit?.tradeType || "").split(".");
      return {
        tradecategory: parts[0] || "",
        tradetype: parts[1] || "",
        tradesubtype: unit?.tradeType || "",
        unit: unit?.uom || "",
        uom: unit?.uomValue != null ? String(unit.uomValue) : "",
        id: unit?.id,
      };
    }),
    isAccessories: accessories.length > 0 ? "ACCESSORY" : "NONACCESSORY",
    accessories: accessories.map((item) => ({
      accessory: item.accessoryCategory,
      accessorycount: item.count != null ? String(item.count) : "",
      unit: item.uom || "",
      uom: item.uomValue != null ? String(item.uomValue) : "",
      id: item.id,
    })),
    tradeGSTNumber: additional.tradeGstNo || additional.gstNo || "",
    operationalSqFtArea: detail.operationalArea != null ? String(detail.operationalArea) : "",
    numberOfEmployees: detail.noOfEmployees != null ? String(detail.noOfEmployees) : "",
    pincode: address.pincode || "",
    city: address.tenantId || license.tenantId || "",
    locality: address.locality || "",
    street: address.street || "",
    doorNo: address.doorNo || "",
    landmark: address.landmark || "",
    isSameAsPropertyOwner: sameOwner ? "YES" : "NO",
    ownershipCategory: detail.subOwnerShipCategory || "",
    owners: (detail.owners || []).map((owner) => ({
      name: owner.name || "",
      gender: owner.gender || "",
      mobilenumber: owner.mobileNumber || "",
      fatherOrHusbandName: owner.fatherOrHusbandName || "",
      relationship: owner.relationship || "",
      emailId: owner.emailId || "",
      permanentAddress: owner.permanentAddress || "",
      id: owner.id,
      uuid: owner.uuid,
    })),
    proofOfIdentity: documentByType(documents, "OWNERIDPROOF"),
    proofOfOwnership: documentByType(documents, "OWNERSHIPPROOF"),
    ownerPhotoProof: documentByType(documents, "OWNERPHOTO"),
  };
};

/**
 * Extract flat form row from wizard session.
 * @param {object} session
 * @returns {object}
 */
export const getTlDynamicFlatData = (session = {}) => {
  const payloadKey = "Licenses";
  const step =
    session?.newApplication ||
    session?.info ||
    session?.structure ||
    session?.accordionForm;
  const saved = step?.[payloadKey] || session?.newApplication?.[payloadKey];
  if (Array.isArray(saved)) return saved[0] || {};
  return saved || {};
};

/**
 * Map flat DynamicForm values → nested TL_CREATE_TRADE keys for CheckPage.
 * Preserves dynamic step data + routeConfigs for edit round-trip.
 *
 * @param {object} session - Wizard session after mergeSessionStepWithRouteConfig
 * @returns {object}
 */
export const mapDynamicTlToCheckSession = (session = {}) => {
  const flat = getTlDynamicFlatData(session);
  if (!flat || Object.keys(flat).length === 0) return session;

  const property = flat.__property || null;
  const propertyId = flat.propertyId || property?.propertyId || "";
  const sameAsProperty = isSameAsPropertyOwnerYes(flat.isSameAsPropertyOwner);

  const structureType = asCodeObj(flat.structureType, "TL");
  const isAccessories = asCodeObj(
    flat.isAccessories === "ACCESSORY"
      ? "ACCESSORY"
      : flat.isAccessories === "NONACCESSORY"
        ? "NONACCESSORY"
        : flat.isAccessories,
    "TL"
  );

  // Soft-normalize accessories yes/no for convertToTrade (looks for YES in i18nKey).
  if (isAccessories) {
    if (String(flat.isAccessories).toUpperCase() === "ACCESSORY") {
      isAccessories.i18nKey = "TL_COMMON_YES";
      isAccessories.code = "ACCESSORY";
    } else if (String(flat.isAccessories).toUpperCase() === "NONACCESSORY") {
      isAccessories.i18nKey = "TL_COMMON_NO";
      isAccessories.code = "NONACCESSORY";
    }
  }

  let ownershipCategory = asCodeObj(
    flat.ownershipCategory,
    "COMMON_MASTERS_OWNERSHIPCATEGORY"
  );

  // Drop blank placeholder rows. The owners fieldArray is seeded with an empty
  // row while same-as-property-owner is hidden, and that empty row was sent to
  // user create (name/mobile missing → InvalidUserCreateException).
  let ownersList = pickFirstArrayItem(flat.owners)
    .map(normalizeOwnerRow)
    .filter(isFilledOwner);

  // When owners are still flat single fields (fieldArray not yet rendered), synthesize one.
  if (
    ownersList.length === 0 &&
    (flat.name || flat.mobilenumber || flat.mobileNumber || flat.fatherOrHusbandName)
  ) {
    const synthesized = normalizeOwnerRow({
      name: flat.name,
      gender: flat.gender,
      mobilenumber: flat.mobilenumber || flat.mobileNumber,
      fatherOrHusbandName: flat.fatherOrHusbandName,
      relationship: flat.relationship,
      emailId: flat.emailId,
      permanentAddress: flat.permanentAddress,
      isCorrespondenceAddress: flat.isCorrespondenceAddress,
    });
    if (isFilledOwner(synthesized)) ownersList.push(synthesized);
  }

  // Same-as-property-owner: use the property owners when they are complete.
  // Incomplete property owners stay on the form so the citizen can correct them.
  if (sameAsProperty && property) {
    const fromProperty = ownersFromProperty(property, ownershipFromProperty(property))
      .map(normalizeOwnerRow)
      .filter(isFilledOwner);
    if (fromProperty.length) {
      ownershipCategory = {
        ...ownershipFromProperty(property),
        isSameAsPropertyOwner: true,
      };
      ownersList = fromProperty;
    } else if (ownershipCategory) {
      ownershipCategory = {
        ...ownershipCategory,
        isSameAsPropertyOwner: true,
      };
    }
  }

  const units = pickFirstArrayItem(flat.tradeUnits || flat.units).map(normalizeUnitRow);
  if (
    units.length === 0 &&
    (flat.tradecategory || flat.tradetype || flat.tradesubtype)
  ) {
    units.push(
      normalizeUnitRow({
        tradecategory: flat.tradecategory,
        tradetype: flat.tradetype,
        tradesubtype: flat.tradesubtype,
        unit: flat.unit,
        uom: flat.uom,
      })
    );
  }

  const accessories = pickFirstArrayItem(flat.accessories).map(normalizeUnitRow);
  if (
    accessories.length === 0 &&
    flat.isAccessories === "ACCESSORY" &&
    (flat.accessory || flat.accessorycount)
  ) {
    accessories.push(
      normalizeUnitRow({
        accessory: flat.accessory,
        accessorycount: flat.accessorycount,
        unit: flat.unit,
        uom: flat.uom,
      })
    );
  }

  const cityCode =
    (typeof flat.city === "object" ? flat.city?.code : flat.city) ||
    property?.tenantId ||
    Digit.ULBService.getCurrentTenantId();

  const cityValue =
    typeof flat.city === "object" && flat.city?.code
      ? flat.city
      : { code: cityCode, name: cityCode };

  const localityValue =
    typeof flat.locality === "object" && flat.locality
      ? flat.locality
      : flat.locality
        ? { code: flat.locality, i18nkey: flat.locality }
        : property?.address?.locality;

  const permanentAddress =
    flat.permanentAddress ||
    ownersList[0]?.permanentAddress ||
    (sameAsProperty
      ? property?.owners?.[0]?.permanentAddress ||
        property?.owners?.[0]?.correspondenceAddress
      : undefined);

  const nested = {
    tenantId: cityCode,
    TradeDetails: {
      TradeName: flat.tradeName,
      StructureType: structureType,
      BuildingType: asCodeObj(flat.buildingType, STRUCTURE_PREFIX),
      VehicleType: asCodeObj(flat.vehicleType, STRUCTURE_PREFIX),
      CommencementDate: flat.commencementDate,
      TradeGSTNumber: flat.tradeGSTNumber,
      OperationalSqFtArea: flat.operationalSqFtArea,
      NumberOfEmployees: flat.numberOfEmployees,
      units,
      isAccessories,
      accessories,
    },
    financialYear:
      (typeof flat.financialYear === "object"
        ? flat.financialYear?.code || flat.financialYear?.name
        : flat.financialYear) || undefined,
    // Citizen accordion create is PERMANENT-only (matches billing slabs).
    licenseType: "PERMANENT",
    address: {
      pincode: flat.pincode || property?.address?.pincode,
      city: cityValue,
      locality: localityValue,
      street: flat.street || property?.address?.street,
      doorNo: flat.doorNo || property?.address?.doorNo,
      landmark: flat.landmark || property?.address?.landmark,
    },
    ownershipCategory: ownershipCategory || {},
    owners: {
      owners: ownersList,
      permanentAddress,
      isCorrespondenceAddress: flat.isCorrespondenceAddress,
      documents: {
        ProofOfIdentity: asUploadDoc(flat.proofOfIdentity, "OWNERIDPROOF"),
        ProofOfOwnership: asUploadDoc(flat.proofOfOwnership, "OWNERSHIPPROOF"),
        OwnerPhotoProof: asUploadDoc(flat.ownerPhotoProof, "OWNERPHOTO"),
      },
    },
    cpt: propertyId
      ? {
          details: property || {
            propertyId,
            tenantId: cityCode,
            address: {
              pincode: flat.pincode,
              city: cityCode,
              locality: localityValue,
              street: flat.street,
              doorNo: flat.doorNo,
              landmark: flat.landmark,
            },
          },
        }
      : undefined,
    cptId: propertyId ? { id: propertyId } : undefined,
    knowyourproperty: propertyId
      ? { KnowProperty: { code: "YES", i18nKey: "TL_COMMON_YES" } }
      : undefined,
    __dynamicForm: true,
  };

  if (!propertyId) {
    try {
      sessionStorage.removeItem("cpt");
      sessionStorage.removeItem("KnowProperty");
    } catch (e) {
      /* ignore */
    }
    try {
      if (Digit?.SessionStorage?.set) Digit.SessionStorage.set("cpt", "");
    } catch (e) {
      /* ignore */
    }
  }

  if (property) {
    try {
      sessionStorage.setItem("KnowProperty", "TL_COMMON_YES");
      sessionStorage.setItem("cpt", JSON.stringify(property));
      if (Digit?.SessionStorage?.set) {
        Digit.SessionStorage.set("cpt", property);
      }
    } catch (e) {
      /* ignore */
    }
  }

  if (flat.isSameAsPropertyOwner != null) {
    try {
      sessionStorage.setItem(
        "isSameAsPropertyOwner",
        String(sameAsProperty)
      );
    } catch (e) {
      /* ignore */
    }
  }

  const fyCode =
    typeof flat.financialYear === "object"
      ? flat.financialYear?.code || flat.financialYear?.name
      : flat.financialYear;
  if (fyCode) {
    try {
      sessionStorage.setItem("CurrentFinancialYear", String(fyCode));
    } catch (e) {
      /* ignore */
    }
  }

  return {
    ...session,
    ...nested,
  };
};
