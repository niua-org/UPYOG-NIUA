import React, { useState, useCallback, useMemo } from "react";

/**
 * Reusable Custom Hook for managing Modal / Popup open-close state.
 * 
 * @param {boolean} initialState - Initial visibility state of the popup (default: false)
 * @returns {object} { isOpen, openModal, closeModal, toggleModal, setIsOpen }
 */
export const useModalState = (initialState = false) => {
  const [isOpen, setIsOpen] = useState(initialState);

  const openModal = useCallback(() => setIsOpen(true), []);
  const closeModal = useCallback(() => setIsOpen(false), []);
  const toggleModal = useCallback(() => setIsOpen((prev) => !prev), []);

  return { isOpen, openModal, closeModal, toggleModal, setIsOpen };
};

/**
 * Helper function to retrieve the initialized MDMS store data from Digit services.
 * 
 * @param {string} stateCode - Optional state code (defaults to current tenant state)
 * @returns {object} { isLoading, initData }
 */
export const useMDMSData = (stateCode) => {
  const tenantState = stateCode || (Digit.ULBService.getStateId ? Digit.ULBService.getStateId() : "pg");
  const { isLoading, data: initData } = Digit.Hooks.useInitStore(tenantState);
  return { isLoading, initData };
};

/**
 * Generic Utility to transform any raw array into standard Dropdown options ({ name, code }).
 * 
 * @param {Array} rawList - Raw data array from API or MDMS
 * @param {string} labelKey - Object key to use for label/name (default: "code")
 * @param {string} valueKey - Object key to use for value/code (default: "code")
 * @param {string} prefix - Optional i18n key prefix (e.g. "WBH_MODULE")
 * @returns {Array<{ name: string, code: string }>}
 */
export const transformToDropdownOptions = (rawList = [], labelKey = "code", valueKey = "code", prefix = "") => {
  if (!Array.isArray(rawList) || rawList.length === 0) return [];

  return rawList
    .filter((item) => item?.enabled !== false && item?.active !== false)
    .map((item) => {
      const val = typeof item === "string" ? item : item?.[valueKey] || item?.[labelKey];
      const rawLabel = typeof item === "string" ? item : item?.[labelKey] || item?.[valueKey];
      const nameKey = prefix && val ? `${prefix}_${String(val).toUpperCase()}` : rawLabel;

      return {
        name: nameKey,
        code: val,
      };
    });
};

/**
 * Extract Module Dropdown options dynamically from MDMS initData (tenant.citymodule)
 * 
 * @param {object} initData - Store init data from useInitStore
 * @param {Array} defaultOptions - Optional fallback array
 * @returns {Array<{ name: string, code: string }>}
 */
export const extractModuleOptions = (initData, defaultOptions = []) => {
  if (initData?.modules && Array.isArray(initData.modules) && initData.modules.length > 0) {
    return initData.modules.map((m) => ({
      name: m?.code ? `WBH_MODULE_${m.code.toUpperCase()}` : m?.name || m?.code,
      code: m?.code || m?.name,
    }));
  }
  return defaultOptions;
};

/**
 * Extract Form Dropdown options dynamically from MDMS initData (ModuleAccordions.dropdownOptionsForForm)
 * 
 * @param {object} initData - Store init data from useInitStore
 * @param {Array} defaultOptions - Optional fallback array
 * @returns {Array<{ name: string, code: string }>}
 */
export const extractFormOptions = (initData, defaultOptions = []) => {
  const rawForms = initData?.ModuleAccordions?.dropdownOptionsForForm;
  if (rawForms && Array.isArray(rawForms) && rawForms.length > 0) {
    return rawForms
      .filter((item) => item?.enabled !== false)
      .map((item) => ({
        name: item?.code ? `WBH_FORM_${item.code.toUpperCase()}` : item?.code,
        code: item?.code,
      }));
  }
  return defaultOptions;
};

/**
 * Extract Accordion Dropdown options dynamically from MDMS initData (ModuleAccordions.dropdownOptionsForAccordion)
 * 
 * @param {object} initData - Store init data from useInitStore
 * @param {Array} defaultOptions - Optional fallback array
 * @returns {Array<{ name: string, code: string }>}
 */
export const extractAccordionOptions = (initData, defaultOptions = []) => {
  const rawAccordions = initData?.ModuleAccordions?.dropdownOptionsForAccordion;
  if (rawAccordions && Array.isArray(rawAccordions) && rawAccordions.length > 0) {
    return rawAccordions
      .filter((item) => item?.enabled !== false)
      .map((item) => ({
        name: item?.code ? `WBH_ACCORDION_${item.code.toUpperCase()}` : item?.code,
        code: item?.code,
      }));
  }
  return defaultOptions;
};

/**
 * Single dynamic input change handler for state objects.
 * 
 * @param {Function} setFormState - React setState function
 * @returns {Function} Event handler function
 */
export const handleDynamicInputChange = (setFormState) => (e) => {
  const { name, value } = e && e.target ? e.target : e;
  setFormState((prev) => ({
    ...prev,
    [name]: value,
  }));
};

/**
 * Clean fallback helper for localization keys to prevent displaying raw unformatted keys.
 * 
 * @param {Function} t - i18next translation function
 * @param {string} key - i18n translation key
 * @param {string} fallback - Fallback string if key is not found
 * @returns {string}
 */
export const getLocaleText = (t, key, fallback) => {
  if (!key) return fallback || "";
  const translated = t(key);
  return translated && translated !== key ? translated : fallback || key;
};

/**
 * Normalize ModuleAccordions data from MDMS store into an array of module objects.
 * 
 * @param {Array|object} moduleAccordionsData - MdmsRes ModuleAccordions
 * @returns {Array<object>} Array of module objects with code, dropdownOptionsForAccortion, dropdownOptionsForForm
 */
export const getModuleListFromMDMS = (moduleAccordionsData) => {
  if (!moduleAccordionsData) return [];
  if (Array.isArray(moduleAccordionsData)) {
    return moduleAccordionsData;
  }
  if (typeof moduleAccordionsData === "object") {
    if (moduleAccordionsData.code) {
      return [moduleAccordionsData];
    }
    return Object.values(moduleAccordionsData).filter((item) => typeof item === "object" && item !== null && item.code);
  }
  return [];
};

/**
 * Extract Accordion dropdown options for a specific selected module object.
 * 
 * @param {object} moduleObj - Selected module object from MDMS ModuleAccordions
 * @param {Array} fallback - Fallback options array if moduleObj has no accordions
 * @returns {Array<{ name: string, code: string }>}
 */
export const getAccordionsForModule = (moduleObj, fallback = []) => {
  if (!moduleObj) return fallback;
  const rawList = moduleObj.dropdownOptionsForAccortion || moduleObj.dropdownOptionsForAccordion || [];
  if (!Array.isArray(rawList) || rawList.length === 0) return fallback;

  return rawList
    .filter((item) => item?.enabled !== false)
    .map((item) => {
      const code = item?.code || item?.value || item?.name;
      return {
        name: item?.i18nKey || (code ? `WBH_ACCORDION_${String(code).toUpperCase()}` : code),
        code: code,
        i18nKey: item?.i18nKey || code,
      };
    });
};

/**
 * Extract Form dropdown options for a specific selected module object.
 * 
 * @param {object} moduleObj - Selected module object from MDMS ModuleAccordions
 * @param {Array} fallback - Fallback options array if moduleObj has no forms
 * @returns {Array<{ name: string, code: string }>}
 */
export const getFormsForModule = (moduleObj, fallback = []) => {
  if (!moduleObj) return fallback;
  const rawList = moduleObj.dropdownOptionsForForm || [];
  if (!Array.isArray(rawList) || rawList.length === 0) return fallback;

  return rawList
    .filter((item) => item?.enabled !== false)
    .map((item) => {
      const code = item?.code || item?.value || item?.name;
      return {
        name: item?.i18nKey || (code ? `WBH_FORM_${String(code).toUpperCase()}` : code),
        code: code,
        i18nKey: item?.i18nKey || code,
      };
    });
};

export default {
  useModalState,
  useMDMSData,
  transformToDropdownOptions,
  extractModuleOptions,
  extractFormOptions,
  extractAccordionOptions,
  getModuleListFromMDMS,
  getAccordionsForModule,
  getFormsForModule,
  handleDynamicInputChange,
  getLocaleText,
};
