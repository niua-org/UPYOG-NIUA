/**
 * TLCitizenNewApplication.js
 *
 * Citizen Trade License create step — thin wrapper around ConfigDrivenFormStep.
 * Accordion vs stepped layout comes from navigation.pattern / accordionConfig.
 * Billing slabs + enrichFieldArrayRow stay in the TL module (not workbench JSON).
 */
import React, { useCallback, useEffect } from "react";
import { ConfigDrivenFormStep, optionCode } from "@nudmcdgnpm/digit-ui-react-components";
import tlFormConfig from "../config/tlFormConfig";
import "../styles/TLCitizenNewApplication.scss";
import {
  clearTlPropertySession,
  mapPropertyToSearchMatch,
  persistPropertySession,
  searchPropertyById,
} from "../utils/tlPropertySearch";
import {
  enrichTlBillingSlabRow,
  ensureTlBillingSlabFieldRule,
  filterTradeUnitDropdownByBillingSlabs,
  selectCitizenNewBillingSlabs,
} from "../utils/tlBillingSlab";

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

  // this is use to enrich the billing slab data in the dropdown
  const enrichFieldArrayRow = useCallback(
    (ctx) =>
      enrichTlBillingSlabRow({
        ...ctx,
        billingSlabs,
      }),
    [billingSlabs]
  );
// this is use to filter the dropdown data based on the billing slabs
  const transformDropdownData = useCallback(
    (dropdownData, values) =>
      filterTradeUnitDropdownByBillingSlabs(dropdownData, values, billingSlabs),
    [billingSlabs]
  );
// this is use to search the property id and return the matches
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
      transformDropdownData={transformDropdownData}
      confirmCancel
      pageClassName="tl-citizen-new-application"
      wrapperClassName="tl-citizen-new-application__form-step"
      accordionWrapperClassName="tl-citizen-new-application__form-step tl-citizen-new-application__form-step--accordion"
    />
  );
};

export default TLCitizenNewApplication;
