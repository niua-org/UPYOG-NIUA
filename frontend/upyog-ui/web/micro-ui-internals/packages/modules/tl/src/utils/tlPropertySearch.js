/**
 * tlPropertySearch.js
 * Property lookup helpers for TL DynamicForm searchCard.
 * Accepts a property ID or a 10-digit mobile number.
 */
import { coerceRadioFieldValue } from "@nudmcdgnpm/digit-ui-react-components";

/** PT citizen search treats a 10-digit number starting at 5 as a mobile. */
const MOBILE_QUERY = /^[5-9]\d{9}$/;

/** Owner PII the property search should return in plain text for this form. */
const OWNER_PLAIN_FIELDS = [
  "name",
  "mobileNumber",
  "fatherOrHusbandName",
  "guardian",
  "gender",
  "relationship",
  "emailId",
  "permanentAddress",
  "correspondenceAddress",
  "userName",
];

const normalizeTenant = (value) => String(value || "").trim().toLowerCase();

/**
 * Keep properties that belong to the citizen's ULB.
 * A state-level row (tenant `pg` for ULB `pg.citya`) is kept only when it
 * does not name a different city.
 *
 * @param {object} property
 * @param {string} ulbTenant
 * @returns {boolean}
 */
export const propertyMatchesUlb = (property = {}, ulbTenant) => {
  const ulb = normalizeTenant(ulbTenant);
  if (!ulb) return false;
  const tid = normalizeTenant(property?.tenantId || property?.address?.tenantId);
  const cityRaw = property?.address?.city;
  const city = normalizeTenant(
    cityRaw && typeof cityRaw === "object" ? cityRaw.code || cityRaw.name : cityRaw
  );
  if (tid === ulb || city === ulb) return true;
  if (tid && ulb.startsWith(`${tid}.`) && !city) return true;
  // State-level rows often store only the city segment (`amritsar` for `pg.amritsar`).
  if (tid && ulb.startsWith(`${tid}.`)) {
    const ulbCity = ulb.slice(tid.length + 1);
    if (city === ulbCity || city.endsWith(`.${ulbCity}`)) return true;
  }
  return false;
};

/** Non-empty value that is not a privacy mask (asterisks). */
const usableText = (value) => {
  const text = String(value || "").trim();
  return Boolean(text) && !text.includes("*");
};

/** Keep plain text; drop privacy masks so the form does not submit asterisks. */
const plainText = (value) => (usableText(value) ? String(value).trim() : "");

/**
 * True when the property can supply at least one owner with a real name and mobile.
 * Masked values (asterisks) do not count. Institutional records use the authorized
 * person plus the first owner mobile.
 *
 * @param {object} property
 * @returns {boolean}
 */
export const propertyOwnersAreComplete = (property = {}) => {
  const owners = activePropertyOwners(property);
  const category = codeOf(property.ownershipCategory);
  if (category.includes("INSTITUTIONAL")) {
    const name = property?.institution?.nameOfAuthorizedPerson || owners[0]?.name;
    const mobile = owners[0]?.mobileNumber;
    return usableText(name) && usableText(mobile);
  }
  return owners.some(
    (owner) => usableText(owner?.name) && usableText(owner?.mobileNumber)
  );
};

/**
 * Current owners only. Cancelled / mutation rows stay out of the trade form.
 * @param {object} property
 * @returns {object[]}
 */
export const activePropertyOwners = (property = {}) => {
  const owners = Array.isArray(property.owners) ? property.owners : [];
  const active = owners.filter((owner) => {
    const status = String(owner?.status || "ACTIVE").toUpperCase();
    return status === "ACTIVE";
  });
  const list = active.length ? active : owners;
  if (list.some((owner) => owner?.additionalDetails?.ownerSequence != null)) {
    return [...list].sort(
      (a, b) =>
        (a?.additionalDetails?.ownerSequence ?? 0) -
        (b?.additionalDetails?.ownerSequence ?? 0)
    );
  }
  return list;
};

/**
 * Drop property linkage written by an earlier immovable selection.
 * Safe to call when the citizen switches to a movable trade.
 */
export const clearTlPropertySession = () => {
  try {
    sessionStorage.removeItem("cpt");
    sessionStorage.removeItem("KnowProperty");
    sessionStorage.removeItem("isSameAsPropertyOwner");
  } catch (e) {
    /* ignore */
  }
  try {
    if (Digit?.SessionStorage?.set) Digit.SessionStorage.set("cpt", "");
  } catch (e) {
    /* ignore */
  }
};

/**
 * Search PT properties by property ID or owner mobile.
 * Queries the citizen ULB first, then the state tenant (PG ids are often
 * stored there) and keeps only rows for this ULB.
 * @param {string} query property id or mobile number
 * @param {string} tenantId
 * @returns {Promise<object[]>}
 */
export const searchPropertyById = async (query, tenantId) => {
  const value = String(query || "").trim();
  if (!value) return [];

  const ulbTenant =
    tenantId ||
    Digit.ULBService.getCitizenCurrentTenant?.(true) ||
    Digit.ULBService.getCurrentTenantId?.();
  if (!ulbTenant) return [];

  const filters = MOBILE_QUERY.test(value)
    ? { mobileNumber: value }
    : { propertyIds: value };

  const tenants = [
    ulbTenant,
    Digit.ULBService.getCurrentTenantId?.(),
    Digit.ULBService.getStateId?.(),
  ].filter((t, i, arr) => t && arr.indexOf(t) === i);

  for (const tid of tenants) {
    try {
      const response = await Digit.PTService.search({
        tenantId: tid,
        filters,
        auth: true,
        plainAccessRequest: { plainRequestFields: OWNER_PLAIN_FIELDS },
      });
      const list = (Array.isArray(response?.Properties) ? response.Properties : []).filter(
        (property) => propertyMatchesUlb(property, ulbTenant)
      );
      if (list.length) return list;
    } catch (err) {
      console.warn("TL property search failed for tenant", tid, err);
    }
  }
  return [];
};

/**
 * Format a short address line for search-result / property cards.
 * @param {object} property
 * @returns {string}
 */
const formatPropertyAddress = (property = {}) => {
  const address = property.address || {};
  const parts = [
    address.doorNo,
    address.street || address.buildingName,
    address.landmark,
    address.locality?.name || address.locality?.code,
    (address.city && typeof address.city === "object"
      ? address.city.name || address.city.code
      : address.city) || property.tenantId,
    address.pincode,
  ].filter(Boolean);
  return parts.join(", ");
};

/**
 * Latest owner first, same order as CPTPropertyDetails.
 * @param {object} property
 * @returns {object}
 */
const getPrimaryOwner = (property = {}) => {
  const owners = Array.isArray(property.owners) ? property.owners : [];
  const active = owners.filter(
    (owner) => String(owner?.status || "ACTIVE").toUpperCase() === "ACTIVE"
  );
  const list = (active.length ? active : owners).slice().reverse();
  return list[0] || {};
};

/**
 * Persist CPT session keys the way tl_old PropertyDetails.goNext does,
 * so CheckPage / convertToTrade / same-as-property-owner keep working.
 * @param {object} property
 */
export const persistPropertySession = (property = {}) => {
  if (!property?.propertyId) return;
  try {
    sessionStorage.setItem("KnowProperty", "TL_COMMON_YES");
    sessionStorage.setItem("cpt", JSON.stringify(property));
    if (Digit?.SessionStorage?.set) {
      Digit.SessionStorage.set("cpt", property);
    }
  } catch (e) {
    console.warn("TL persistPropertySession failed", e);
  }
};

/**
 * Prefill DynamicForm fields + stash full property for CheckPage / convertToTrade.
 * @param {object} property
 * @returns {object}
 */
const codeOf = (value) => {
  if (value == null || value === "") return "";
  if (typeof value === "object") {
    const nested = value.code ?? value.value ?? value.name ?? value.i18nKey ?? "";
    if (nested && typeof nested === "object") return codeOf(nested);
    return String(nested || "");
  }
  return String(value);
};

/** additionalDetails is sometimes a JSON string rather than an object. */
const ownerExtra = (owner = {}) => {
  const extra = owner.additionalDetails;
  if (!extra) return {};
  if (typeof extra === "string") {
    try {
      const parsed = JSON.parse(extra);
      return parsed && typeof parsed === "object" ? parsed : {};
    } catch (e) {
      return {};
    }
  }
  return typeof extra === "object" ? extra : {};
};

/** User gender enum order, used when a property sends the ordinal instead of the name. */
const GENDER_BY_ORDINAL = ["FEMALE", "MALE", "OTHERS", "TRANSGENDER"];

const asGenderCode = (value) => {
  if (typeof value === "number" && GENDER_BY_ORDINAL[value]) return GENDER_BY_ORDINAL[value];
  if (typeof value === "string" && /^[0-3]$/.test(value.trim())) {
    return GENDER_BY_ORDINAL[Number(value.trim())];
  }
  return coerceRadioFieldValue(value);
};

const ownerGender = (owner = {}) => {
  const extra = ownerExtra(owner);
  const user = owner.user && typeof owner.user === "object" ? owner.user : {};
  for (const value of [
    owner.gender,
    user.gender,
    extra.gender,
    extra.ownerGender,
    owner.genderType,
  ]) {
    const code = asGenderCode(value);
    if (code) return code;
  }
  return "";
};

/**
 * Guardian phrases on property records.
 * Checked before the shared radio aliases, where "F" means female.
 */
const relationshipPhrase = (value) => {
  if (value == null || value === "" || typeof value === "object") return "";
  const token = String(value).trim().toUpperCase();
  if (!token || token.includes("*")) return "";
  if (token === "F" || token === "FATHER") return "FATHER";
  if (token === "H" || token === "HUSBAND") return "HUSBAND";
  if (/\b(FATHER|S\/O|D\/O|C\/O|SON|DAUGHTER)\b/.test(token)) return "FATHER";
  if (/\b(HUSBAND|W\/O|H\/O|WIFE)\b/.test(token)) return "HUSBAND";
  return "";
};

const relationshipCode = (value) => {
  const phrase = relationshipPhrase(value);
  if (phrase) return phrase;
  if (value && typeof value === "object") {
    const nested = value.code ?? value.value ?? value.name ?? value.i18nKey ?? value.i18nkey;
    const nestedPhrase = relationshipPhrase(nested);
    if (nestedPhrase) return nestedPhrase;
  }
  const code = coerceRadioFieldValue(value);
  return relationshipPhrase(code) || (code === "FATHER" || code === "HUSBAND" ? code : "");
};

const ownerRelationship = (owner = {}) => {
  const extra = ownerExtra(owner);
  const candidates = [
    owner.relationship,
    owner.relation,
    owner.guardianRelation,
    extra.relationship,
    extra.relation,
    extra.guardianRelation,
  ];
  for (const value of candidates) {
    const code = relationshipCode(value);
    if (code) return code;
  }
  return "";
};

const mapPropertyOwnersToForm = (property = {}) => {
  const owners = activePropertyOwners(property);
  const category = codeOf(property.ownershipCategory);
  if (category.includes("INSTITUTIONAL")) {
    const first = owners[0] || {};
    return [
      {
        name: plainText(property?.institution?.nameOfAuthorizedPerson || first.name),
        gender: ownerGender(first),
        mobilenumber: plainText(first.mobileNumber),
        fatherOrHusbandName: plainText(first.fatherOrHusbandName || first.guardian),
        relationship: ownerRelationship(first),
        emailId: plainText(first.emailId),
        permanentAddress: plainText(first.permanentAddress || first.correspondenceAddress),
      },
    ];
  }
  return owners.map((owner) => ({
    name: plainText(owner?.name),
    gender: ownerGender(owner),
    mobilenumber: plainText(owner?.mobileNumber),
    fatherOrHusbandName: plainText(owner?.fatherOrHusbandName || owner?.guardian),
    relationship: ownerRelationship(owner),
    emailId: plainText(owner?.emailId),
    permanentAddress: plainText(owner?.permanentAddress || owner?.correspondenceAddress),
  }));
};

/** Same owner rows the form prefill keeps, so radio indexes stay aligned. */
const formPropertyOwners = (property = {}) =>
  mapPropertyOwnersToForm(property).filter((owner) => owner.name || owner.mobilenumber);

/**
 * Gender and relationship for each property owner.
 * The form leaves these radios blank until same-as-property-owner is Yes.
 * @param {object} property
 * @returns {{ gender: string, relationship: string }[]}
 */
export const propertyOwnerRadios = (property = {}) =>
  formPropertyOwners(property).map((owner) => ({
    gender: owner.gender || "",
    relationship: owner.relationship || "",
  }));

/** Older CPT sessions wrap the property as `{ details }`. */
const unwrapProperty = (value) => {
  if (!value || typeof value !== "object") return null;
  const details = value.details;
  if (
    details &&
    typeof details === "object" &&
    !value.owners &&
    !value.propertyId &&
    (details.owners || details.propertyId)
  ) {
    return details;
  }
  return value.owners || value.propertyId ? value : null;
};

/**
 * Property stashed in the CPT session when it was selected.
 * A refresh can drop `__property` from form state while `cpt` is still there.
 * @returns {object|null}
 */
export const readStoredProperty = () => {
  try {
    const fromDigit = Digit?.SessionStorage?.get?.("cpt");
    if (fromDigit && typeof fromDigit === "object") return unwrapProperty(fromDigit);
    if (typeof fromDigit === "string" && fromDigit) {
      const parsed = JSON.parse(fromDigit);
      const property = unwrapProperty(parsed);
      if (property) return property;
    }
  } catch (e) {
    /* ignore */
  }
  try {
    const raw = sessionStorage.getItem("cpt");
    if (!raw) return null;
    return unwrapProperty(JSON.parse(raw));
  } catch (e) {
    return null;
  }
};

const ownershipCategoryCode = (property = {}) => {
  const raw = property?.ownershipCategory;
  if (raw == null || raw === "") return "";
  const code = codeOf(raw);
  if (!code.includes("INSTITUTIONAL")) return code;
  const instType = codeOf(property?.institution?.type);
  return instType && !code.endsWith(`.${instType}`) && !code.endsWith(`_${instType}`)
    ? `${code}.${instType}`
    : code;
};

const asUploadedFile = (doc, documentType) => {
  const fileStoreId = doc?.fileStoreId || doc?.filestoreId || "";
  if (!fileStoreId) return null;
  return {
    fileStoreId,
    filestoreId: fileStoreId,
    fileName: doc.fileName || doc.documentType || documentType,
    documentType,
  };
};

/**
 * First matching document from the property and its owners.
 * PT types look like OWNER.IDENTITYPROOF.AADHAAR.
 * @param {object} property
 * @param {string[]} needles
 * @param {string} documentType TL document type
 * @returns {object|null}
 */
const documentMatches = (doc, needles) => {
  const type = String(doc?.documentType || "").toUpperCase();
  return needles.some((needle) => type.includes(needle));
};

const ownerDocuments = (owners) =>
  owners.flatMap((owner) => (Array.isArray(owner?.documents) ? owner.documents : []));

const findPropertyDocument = (property, needles, documentType) => {
  const propertyDocs = Array.isArray(property?.documents) ? property.documents : [];
  const allOwners = Array.isArray(property?.owners) ? property.owners : [];
  const pools = [
    propertyDocs,
    ownerDocuments(activePropertyOwners(property)),
    ownerDocuments(allOwners),
  ];
  for (const docs of pools) {
    for (const doc of docs) {
      if (!documentMatches(doc, needles)) continue;
      const file = asUploadedFile(doc, documentType);
      if (file) return file;
    }
  }
  return null;
};

const textOrCode = (value) => {
  if (value == null || value === "") return "";
  if (typeof value === "object") return value.code || value.name || "";
  return value;
};

/**
 * Owner rows, ownership category, and documents taken from the linked property.
 * Same-as-property-owner Yes writes this back over any edits made while No was selected.
 * @param {object} property
 * @returns {object}
 */
export const propertyLinkedFields = (property = {}) => {
  const ownershipCategory = ownershipCategoryCode(property);
  const proofOfIdentity = findPropertyDocument(
    property,
    ["IDENTITYPROOF", "OWNERIDPROOF"],
    "OWNERIDPROOF"
  );
  const proofOfOwnership = findPropertyDocument(
    property,
    ["OWNERSHIPPROOF", "ADDRESSPROOF"],
    "OWNERSHIPPROOF"
  );
  const ownerPhotoProof = findPropertyDocument(
    property,
    ["OWNERPHOTO", "PHOTOGRAPH"],
    "OWNERPHOTO"
  );
  return {
    owners: formPropertyOwners(property),
    ...(ownershipCategory ? { ownershipCategory } : {}),
    ...(proofOfIdentity ? { proofOfIdentity } : {}),
    ...(proofOfOwnership ? { proofOfOwnership } : {}),
    ...(ownerPhotoProof ? { ownerPhotoProof } : {}),
  };
};

export const mapPropertyToFormPrefill = (property = {}) => {
  const address = property.address || {};
  const linked = propertyLinkedFields(property);
  // Gender and relationship stay blank until the citizen chooses same-as-property-owner.
  const owners = (linked.owners || []).map((owner) => ({
    ...owner,
    gender: "",
    relationship: "",
  }));
  return {
    propertyId: property.propertyId || "",
    searchPropertyId: property.propertyId || "",
    hasPropertyId: "YES",
    showTradeDetails: "YES",
    propertyOwnersReady: propertyOwnersAreComplete(property) ? "YES" : "NO",
    ...(linked.ownershipCategory ? { ownershipCategory: linked.ownershipCategory } : {}),
    pincode: address.pincode || "",
    city: property.tenantId || textOrCode(address.city),
    locality: textOrCode(address.locality),
    street: address.street || "",
    doorNo: address.doorNo || "",
    landmark: address.landmark || "",
    ...(owners.length ? { owners } : {}),
    // ID proof and ownership document are uploaded for this trade application.
    // Property files must not appear as already attached.
    proofOfIdentity: null,
    proofOfOwnership: null,
    ...(linked.ownerPhotoProof ? { ownerPhotoProof: linked.ownerPhotoProof } : {}),
    // Captured before gender/relationship are cleared on the visible rows.
    __ownerRadios: propertyOwnerRadios(property),
    __property: property,
  };
};

/**
 * Map a PT Property into DynamicForm search-panel match shape.
 * Uses `estateNo` as the panel's generic result id (DynamicForm convention).
 *
 * @param {object} property
 * @returns {object}
 */
export const mapPropertyToSearchMatch = (property = {}) => {
  const propertyId = property.propertyId || "";
  const owner = getPrimaryOwner(property);
  const addressLine = formatPropertyAddress(property);
  return {
    estateNo: propertyId,
    label: propertyId,
    subtitle: [owner?.name, addressLine].filter(Boolean).join(" — "),
    prefill: mapPropertyToFormPrefill(property),
  };
};
