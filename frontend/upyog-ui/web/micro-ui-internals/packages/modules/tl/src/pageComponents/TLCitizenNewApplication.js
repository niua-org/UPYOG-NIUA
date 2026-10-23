/**
 * TLCitizenNewApplication.js
 *
 * Citizen Trade License create step — thin wrapper around ConfigDrivenFormStep.
 * Accordion vs stepped layout comes from navigation.pattern / accordionConfig.
 * Billing slabs + enrichFieldArrayRow stay in the TL module (not workbench JSON).
 */
import React, { useCallback, useEffect } from "react";
import {
  ConfigDrivenFormStep,
  coerceRadioFieldValue,
  optionCode,
} from "@nudmcdgnpm/digit-ui-react-components";
import tlFormConfig from "../config/tlFormConfig";
import "../styles/TLCitizenNewApplication.scss";
import {
  clearTlPropertySession,
  mapPropertyToSearchMatch,
  persistPropertySession,
  propertyLinkedFields,
  readStoredProperty,
  searchPropertyById,
} from "../utils/tlPropertySearch";
import {
  enrichTlBillingSlabRow,
  ensureTlBillingSlabFieldRule,
  filterTradeUnitDropdownByBillingSlabs,
  selectCitizenNewBillingSlabs,
} from "../utils/tlBillingSlab";

/** Plain label from a dropdown value, code object, or string. */
const textOf = (value) => {
  if (value == null || value === "") return "";
  if (typeof value === "object") {
    return value.name || value.i18nkey || value.i18nKey || value.code || "";
  }
  return String(value);
};

/** Same join as TLSelectOwnerAddress: parts with a comma, pincode at the end. */
const joinAddressParts = (parts, t) => {
  const translate = typeof t === "function" ? t : (key) => key;
  let addressDetails = "";
  Object.keys(parts).forEach((key) => {
    const raw = parts[key];
    // Pincode is appended with no comma. If there is no pincode, city is the last part.
    if (key === "pincode" || (!parts.pincode && key === "city")) {
      if (raw) addressDetails += String(raw);
      return;
    }
    if (!raw) return;
    const source = String(raw);
    const translated = translate(source);
    const text = !translated || translated === source ? source : translated;
    addressDetails += `${text}, `;
  });
  // Owner address on the license is stored with a 300 character limit.
  return addressDetails.length > 300 ? addressDetails.slice(0, 300) : addressDetails;
};

/**
 * Owner address filled when "same as trade address" is Yes.
 * A linked property uses its address. Otherwise the trade address on the form.
 */
const tradeAddressAsOwnerAddress = (formData = {}, t) => {
  const property = formData.__property;
  const propertyAddress = property?.address;
  if (property && propertyAddress && Object.keys(property).length > 0) {
    return joinAddressParts(
      {
        doorNo: propertyAddress.doorNo,
        street: propertyAddress.street || propertyAddress.buildingName,
        landmark: propertyAddress.landmark,
        locality: propertyAddress.locality?.name || textOf(propertyAddress.locality),
        city: textOf(propertyAddress.city || propertyAddress.tenantId || property.tenantId),
        pincode: formData.pincode || propertyAddress.pincode,
      },
      t
    );
  }
  const locality = formData.locality;
  const city = formData.city;
  return joinAddressParts(
    {
      doorNo: formData.doorNo,
      street: formData.street,
      landmark: formData.landmark,
      locality:
        locality && typeof locality === "object"
          ? locality.i18nkey || locality.i18nKey || locality.name || locality.code
          : locality,
      city: city && typeof city === "object" ? city.name || city.code : city,
      pincode: formData.pincode,
    },
    t
  );
};
/** Trade-address inputs. Editing any of these refreshes a copied owner address. */
const TRADE_ADDRESS_FIELDS = new Set([
  "doorNo",
  "street",
  "landmark",
  "locality",
  "city",
  "pincode",
]);

/**
 * When the owner address matches the trade address, copy that address onto the row.
 * Choosing No clears the copied permanent address.
 */
const applyCorrespondenceChoice = (ctx, t) => {
  const { childName, value, row = {}, resetFields = [] } = ctx;
  const updated = { ...row, [childName]: value };
  (resetFields || []).forEach((name) => {
    updated[name] = null;
  });
  updated.permanentAddress =
    optionCode(value) === "YES" ? tradeAddressAsOwnerAddress(ctx.formData, t) : "";
  return updated;
};

/** Linked property on the form, or the CPT session saved when it was selected. */
const propertyForOwnerRestore = (data) => {
  const stashed = data?.__property;
  if (stashed && typeof stashed === "object") {
    const details = stashed.details;
    if (
      details &&
      typeof details === "object" &&
      !stashed.owners &&
      !stashed.propertyId &&
      (details.owners || details.propertyId)
    ) {
      return details;
    }
    if (stashed.owners || stashed.propertyId) return stashed;
  }
  return readStoredProperty() || {};
};

/**
 * Gender and relationship captured when the property was selected, then the
 * live property, so a later edit of the visible row cannot wipe the codes.
 */
const ownerRadiosForRestore = (data) => {
  const saved = Array.isArray(data?.__ownerRadios) ? data.__ownerRadios : [];
  const linked = propertyLinkedFields(propertyForOwnerRestore(data));
  const live = Array.isArray(linked.owners) ? linked.owners : [];
  const length = Math.max(saved.length, live.length);
  return Array.from({ length }, (_, index) => ({
    gender: live[index]?.gender || saved[index]?.gender || "",
    relationship: live[index]?.relationship || saved[index]?.relationship || "",
  }));
};

const ownerRadioCode = (value) => coerceRadioFieldValue(value);

const sameOwnerText = (left, right) => String(left ?? "") === String(right ?? "");

/**
 * Owner fields that Yes copies back from the property.
 * Correspondence stays as the citizen left it, and its copied address is kept.
 */
const OWNER_FIELDS_FROM_PROPERTY = [
  "name",
  "mobilenumber",
  "fatherOrHusbandName",
  "emailId",
  "gender",
  "relationship",
  "permanentAddress",
];

const ownerRowFromProperty = (source, previous = {}) => {
  const keepTradeAddress = optionCode(previous.isCorrespondenceAddress) === "YES";
  return {
    ...source,
    isCorrespondenceAddress: previous.isCorrespondenceAddress ?? "",
    permanentAddress: keepTradeAddress
      ? previous.permanentAddress || ""
      : source.permanentAddress || "",
  };
};

const ownerRowMatchesProperty = (row, source) => {
  const next = ownerRowFromProperty(source, row);
  return OWNER_FIELDS_FROM_PROPERTY.every((field) => {
    if (field === "gender" || field === "relationship") {
      return ownerRadioCode(row?.[field]) === ownerRadioCode(next[field]);
    }
    return sameOwnerText(row?.[field], next[field]);
  }) && sameOwnerText(row?.isCorrespondenceAddress, next.isCorrespondenceAddress);
};

/**
 * Yes puts the linked property's owners and category back,
 * including after the citizen chose No and edited the name.
 * ID proof and the ownership document stay as the citizen uploaded them.
 * No clears gender and relationship and leaves the edited name in place.
 * A prefill can leave Yes selected with blank radios; those are filled too.
 */
const applySameAsPropertyOwner = (data, changedField) => {
  if (!Array.isArray(data?.owners) || !data.owners.length) return data;
  const same = optionCode(data.isSameAsPropertyOwner) === "YES";
  const choseThis =
    changedField === "isSameAsPropertyOwner" || changedField === "TL_SAME_AS_PROPERTY_OWNER";
  const radios = ownerRadiosForRestore(data);
  const blankRadios = data.owners.some(
    (row, index) =>
      (radios[index]?.gender && !ownerRadioCode(row?.gender)) ||
      (radios[index]?.relationship && !ownerRadioCode(row?.relationship))
  );
  // Fill on Yes, on the prefill that already has Yes, and whenever a code is
  // still blank after that choice. Other edits must not overwrite a choice.
  if (!choseThis && !(same && blankRadios) && !(changedField == null && same)) return data;

  if (!same) {
    let changed = false;
    const owners = data.owners.map((row) => {
      if (!ownerRadioCode(row?.gender) && !ownerRadioCode(row?.relationship)) return row;
      changed = true;
      return { ...row, gender: "", relationship: "" };
    });
    return changed ? { ...data, owners } : data;
  }

  const linked = propertyLinkedFields(propertyForOwnerRestore(data));
  const sourceRows = Array.isArray(linked.owners) ? linked.owners : [];
  if (!sourceRows.length && !radios.some((row) => row.gender || row.relationship)) return data;

  const ownersChanged =
    (sourceRows.length > 0 && data.owners.length !== sourceRows.length) ||
    data.owners.some((row, index) => {
      const source = sourceRows[index];
      if (source && !ownerRowMatchesProperty(row, source)) return true;
      const radio = radios[index] || {};
      return (
        (radio.gender && ownerRadioCode(row?.gender) !== radio.gender) ||
        (radio.relationship && ownerRadioCode(row?.relationship) !== radio.relationship)
      );
    });
  const baseRows =
    choseThis && sourceRows.length ? sourceRows : data.owners;
  const owners = ownersChanged
    ? baseRows.map((row, index) => {
        const source = sourceRows[index];
        const previous = data.owners[index] || row;
        const next = source ? ownerRowFromProperty(source, previous) : { ...previous };
        const radio = radios[index] || {};
        if (radio.gender) next.gender = radio.gender;
        if (radio.relationship) next.relationship = radio.relationship;
        return next;
      })
    : data.owners;

  const next = { ...data, owners };
  if (
    linked.ownershipCategory &&
    optionCode(data.ownershipCategory) !== optionCode(linked.ownershipCategory)
  ) {
    next.ownershipCategory = linked.ownershipCategory;
  }
  if (linked.ownerPhotoProof) next.ownerPhotoProof = linked.ownerPhotoProof;
  const documentsChanged = ["ownershipCategory", "ownerPhotoProof"].some(
    (field) => next[field] !== data[field]
  );
  if (!ownersChanged && !documentsChanged) return data;
  return next;
};

/**
 * Keep a copied owner address in step with the trade address.
 * A field name limits the refresh to address edits; a prefill passes none.
 */
const refreshCopiedOwnerAddresses = (data, changedField, t) => {
  if (changedField && !TRADE_ADDRESS_FIELDS.has(changedField)) return data;
  if (!Array.isArray(data?.owners) || !data.owners.length) return data;
  const needsCopy = data.owners.some(
    (row) => optionCode(row?.isCorrespondenceAddress) === "YES"
  );
  if (!needsCopy) return data;
  const translate = typeof t === "function" ? t : (key) => key;
  // Build the address once. Every "Yes" row shares the same trade address.
  const permanentAddress = tradeAddressAsOwnerAddress(data, translate);
  let changed = false;
  const owners = data.owners.map((row) => {
    if (optionCode(row?.isCorrespondenceAddress) !== "YES") return row;
    if (row.permanentAddress === permanentAddress) return row;
    changed = true;
    return { ...row, permanentAddress };
  });
  return changed ? { ...data, owners } : data;
};

/**
 * Citizen new-application accordion. Wires property search, billing-slab
 * pricing, and the same-as-trade-address owner choice into the shared form.
 */
const TLCitizenNewApplication = ({
  onSelect,
  config,
  persistedData,
  formData,
  isEditMode,
  editData,
  t,
}) => {
  const tenantId =
    Digit.ULBService.getCitizenCurrentTenant?.(true) ||
    Digit.ULBService.getCurrentTenantId();

  useEffect(() => {
    ensureTlBillingSlabFieldRule();
  }, []);

  const { data: billingSlabs = [] } = Digit.Hooks.tl.useTradeLicenseBillingslab(
    { tenantId, filters: {} },
    {
      enabled: !!tenantId,
      select: selectCitizenNewBillingSlabs,
    }
  );

  const enrichFieldArrayRow = useCallback(
    (ctx) => {
      if (ctx?.childName === "isCorrespondenceAddress") {
        return applyCorrespondenceChoice(ctx, t || ((key) => key));
      }
      return enrichTlBillingSlabRow({
        ...ctx,
        billingSlabs,
      });
    },
    [billingSlabs, t]
  );
  const transformDropdownData = useCallback(
    (dropdownData, values) =>
      filterTradeUnitDropdownByBillingSlabs(dropdownData, values, billingSlabs),
    [billingSlabs]
  );
  const reviseFormData = useCallback(
    (data, changedField) =>
      refreshCopiedOwnerAddresses(
        applySameAsPropertyOwner(data, changedField),
        changedField,
        t
      ),
    [t]
  );
  const handleFieldSearch = useCallback(
    async (fieldName, values) => {
      if (fieldName !== "searchPropertyId") return null;

      const propertyId = String(values?.searchPropertyId || "").trim();
      if (!propertyId) {
        return { error: "TL_PROPERTY_SEARCH_REQUIRED" };
      }

      try {
        const properties = await searchPropertyById(propertyId, tenantId);
        if (!properties.length) {
          return { notFound: true, estateNo: propertyId };
        }

        return {
          matches: properties.map(mapPropertyToSearchMatch),
        };
      } catch (err) {
        console.error("TL property search failed:", err);
        return { error: "CS_SOMETHING_WENT_WRONG" };
      }
    },
    [tenantId]
  );

  const handlePrefillApplied = useCallback((prefill = {}) => {
    if (prefill.__property) {
      persistPropertySession(prefill.__property);
    }
  }, []);

  const handleFieldChange = useCallback((fieldName, value) => {
    const code = optionCode(value);
    // A new structure choice (movable or immovable) drops any linked property.
    if (fieldName === "structureType") {
      clearTlPropertySession();
      return;
    }
    if (fieldName === "hasPropertyId" && code !== "YES") {
      clearTlPropertySession();
    }
  }, []);

  return (
    <ConfigDrivenFormStep
      config={config}
      localOverrides={tlFormConfig}
      onSelect={onSelect}
      persistedData={persistedData}
      formData={formData}
      isEditMode={isEditMode}
      editData={editData}
      t={t}
      tenantId={tenantId}
      defaultHeaderCode="TL_COMMON_NEW_TRADE_LICENSE"
      onFieldSearch={handleFieldSearch}
      onPrefillApplied={handlePrefillApplied}
      onFieldChange={handleFieldChange}
      enrichFieldArrayRow={enrichFieldArrayRow}
      reviseFormData={reviseFormData}
      transformDropdownData={transformDropdownData}
      confirmCancel
      pageClassName="tl-citizen-new-application"
      wrapperClassName="tl-citizen-new-application__form-step"
      accordionWrapperClassName="tl-citizen-new-application__form-step tl-citizen-new-application__form-step--accordion"
    />
  );
};

export default TLCitizenNewApplication;
