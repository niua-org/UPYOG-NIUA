import { useMemo } from "react";
import { getDataSourceKey } from "./formUtils";

const LANGUAGE_SOURCES = new Set(["language", "languages"]);
const TENANT_SOURCES = new Set(["city", "cities", "tenant", "tenants"]);

const getBuiltInSourceType = (field) => {
  const source = field.dataSource ?? field.optionsSource;
  const identifiers =
    source && typeof source === "object"
      ? [source.type, source.name, source.key]
      : [source, field.name];

  const normalizedIdentifiers = identifiers
    .filter(Boolean)
    .map((identifier) => String(identifier).toLowerCase());

  if (normalizedIdentifiers.some((identifier) => LANGUAGE_SOURCES.has(identifier))) return "language";
  if (normalizedIdentifiers.some((identifier) => TENANT_SOURCES.has(identifier))) return "tenant";
  return null;
};

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
  const requestedSources = useMemo(
    () =>
      fields
        .map((field) => ({
          key: getDataSourceKey(field),
          type: getBuiltInSourceType(field),
        }))
        .filter(({ key }) => Boolean(key)),
    [fields]
  );
  const currentLanguage = Digit.StoreData.getCurrentLanguage();
  const selectedLanguage =
    languages.find((language) => language.value === currentLanguage)?.value ||
    languages.find((language) => language.value?.split("_")[0] === currentLanguage?.split("_")[0])?.value ||
    currentLanguage;
  const sources = useMemo(() => {
    // Keep configuration keys intact while resolving each key to its platform source.
    const configuredSources = requestedSources.reduce((resolvedSources, source) => {
      if (source.type === "language") {
        resolvedSources[source.key] = { options: languages, optionKey: "label", valueKey: "value" };
      } else if (source.type === "tenant") {
        resolvedSources[source.key] = { options: tenants, optionKey: "i18nKey", valueKey: "code" };
      }
      return resolvedSources;
    }, {});

    return {
      ...configuredSources,
      // Parent-provided sources override built-ins with the same configured key.
      ...externalSources,
    };
  }, [externalSources, languages, requestedSources, tenants]);
  const needsLanguages = requestedSources.some((source) => source.type === "language");
  const needsTenants = requestedSources.some((source) => source.type === "tenant");
  const areExternalSourcesLoading = Object.values(externalSources).some((source) => source?.isLoading);

  return {
    sources,
    stateInfo,
    selectedLanguage,
    isLoading: (needsLanguages && isInitDataLoading) || (needsTenants && isTenantsLoading) || areExternalSourcesLoading,
  };
};

export default useOnboardingOptionSources;
