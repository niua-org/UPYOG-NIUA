import { useMemo } from "react";
import { getDataSourceKey } from "./formUtils";

const LANGUAGE_SOURCES = new Set(["language", "languages"]);
const TENANT_SOURCES = new Set(["city", "cities", "tenant", "tenants"]);

/**
 * Resolves built-in platform sources. Future API/MDMS hooks should run in a
 * parent and be passed through externalSources to keep hooks deterministic.
 */
const useOnboardingOptionSources = (fields = [], externalSources = {}) => {
  const { data: initData, isLoading: isInitDataLoading } = Digit.Hooks.useStore.getInitData();
  // Direct URL loads can expose a brief null init-data state. Normalize it
  // before reading language/tenant sources so V2 routes render their loader
  // instead of falling through the application error boundary.
  const { languages = [], stateInfo } = initData || {};
  const { data: tenants = [], isLoading: isTenantsLoading } = Digit.Hooks.useTenants();
  const requestedSources = useMemo(() => new Set(fields.map(getDataSourceKey).filter(Boolean)), [fields]);
  const currentLanguage = Digit.StoreData.getCurrentLanguage();
  const selectedLanguage =
    languages.find((language) => language.value === currentLanguage)?.value ||
    languages.find((language) => language.value?.split("_")[0] === currentLanguage?.split("_")[0])?.value ||
    currentLanguage;
  const sources = useMemo(
    () => ({
      // Source metadata lets the generic renderer work without checking field names.
      language: { options: languages, optionKey: "label", valueKey: "value" },
      languages: { options: languages, optionKey: "label", valueKey: "value" },
      city: { options: tenants, optionKey: "i18nKey", valueKey: "code" },
      cities: { options: tenants, optionKey: "i18nKey", valueKey: "code" },
      tenant: { options: tenants, optionKey: "i18nKey", valueKey: "code" },
      tenants: { options: tenants, optionKey: "i18nKey", valueKey: "code" },
      // Parent-provided sources override built-ins with the same key.
      ...externalSources,
    }),
    [externalSources, languages, tenants]
  );
  const needsLanguages = [...requestedSources].some((source) => LANGUAGE_SOURCES.has(source));
  const needsTenants = [...requestedSources].some((source) => TENANT_SOURCES.has(source));
  const areExternalSourcesLoading = Object.values(externalSources).some((source) => source?.isLoading);

  return {
    sources,
    stateInfo,
    selectedLanguage,
    isLoading: (needsLanguages && isInitDataLoading) || (needsTenants && isTenantsLoading) || areExternalSourcesLoading,
  };
};

export default useOnboardingOptionSources;
