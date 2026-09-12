import { createContext, useCallback, useContext, useMemo, useState } from "react";

const OnboardingContext = createContext(null);
// Route continuity needs identity/location fields and flow markers, never OTPs
// or passwords. Sensitive values remain only in the live React form state.
const isSensitiveFormField = (key) => {
  const normalizedKey = key.toLowerCase();
  return normalizedKey.includes("password") || (!key.startsWith("_") && normalizedKey.includes("otp"));
};
const getPersistableFormData = (data) =>
  Object.fromEntries(Object.entries(data).filter(([key]) => !isSensitiveFormField(key)));

/**
 * Loads the onboarding and logo MDMS masters and owns values shared between
 * nested onboarding steps. moduleName/masterName make this provider reusable
 * for citizen and employee configurations.
 */
export const OnboardingProvider = ({ stateCode, moduleName, masterName, storageKey, children }) => {
  // Citizen and Employee V2 supply distinct keys so non-sensitive continuation
  // context can survive refresh without crossing user types.
  const [formData, setFormData] = useState(() => (storageKey ? Digit.SessionStorage.get(storageKey) || {} : {}));
  const {
    data: config,
    isLoading,
    error,
    refetch,
  } = Digit.Hooks.useEnabledMDMS(stateCode, moduleName, [{ name: masterName }], {
    // Normalize API presentation text once; all child components receive clean config.
    select: (data) => data?.[moduleName]?.[masterName]?.[0],
    useCache: false,
  });
  const {
    data: logo,
  } = Digit.Hooks.useEnabledMDMS(stateCode, "LOGO", [{ name: "LogoConfig" }], {
    select: (data) => data?.LOGO?.LogoConfig?.[0]?.default,
    useCache: false,
  });

  // Step components request only their own slice of the complete MDMS config.
  const getStepConfig = useCallback((step) => config?.steps?.[step] ?? null, [config]);

  // Merge one field at a time so values can be carried from login/register to OTP.
  const updateField = useCallback((name, value) => {
    setFormData((currentData) => {
      const nextData = { ...currentData, [name]: value };
      if (storageKey) Digit.SessionStorage.set(storageKey, getPersistableFormData(nextData));
      return nextData;
    });
  }, [storageKey]);

  // Merge a submitted step atomically so route guards see defaults and values
  // that may not have emitted an individual change event.
  const updateFormData = useCallback((values) => {
    setFormData((currentData) => {
      const nextData = { ...currentData, ...values };
      if (storageKey) Digit.SessionStorage.set(storageKey, getPersistableFormData(nextData));
      return nextData;
    });
  }, [storageKey]);

  const clearFormData = useCallback(() => {
    setFormData({});
    if (storageKey) Digit.SessionStorage.del(storageKey);
  }, [storageKey]);

  const value = useMemo(
    () => ({
      content: config?.content ?? {},
      common: config?.common ?? {},
      getStepConfig,
      formData,
      updateField,
      updateFormData,
      clearFormData,
      logo,
      isLoading,
      error: error || null,
      refetch,
    }),
    [
      config,
      getStepConfig,
      formData,
      updateField,
      updateFormData,
      clearFormData,
      logo,
      isLoading,
      error,
      refetch,
    ]
  );

  return <OnboardingContext.Provider value={value}>{children}</OnboardingContext.Provider>;
};

export const useOnboarding = () => {
  const context = useContext(OnboardingContext);

  if (!context) {
    // Fail early when a step is mounted outside OnboardingLayout/Provider.
    throw new Error("useOnboarding must be used inside OnboardingProvider");
  }

  return context;
};
