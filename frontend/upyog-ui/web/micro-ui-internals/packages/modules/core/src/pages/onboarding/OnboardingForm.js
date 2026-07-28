import { useEffect, useMemo } from "react";
import ConfigurableForm from "./ConfigurableForm";
import { getDataSourceKey } from "./formUtils";
import { useOnboarding } from "./OnboardingContext";
import useOnboardingOptionSources from "./useOnboardingOptionSources";

const LANGUAGE_SOURCES = new Set(["language", "languages"]);

/**
 * Onboarding adapter: selects one MDMS step, resolves its option sources and
 * connects the generic form to shared onboarding state and platform effects.
 */
const OnboardingForm = ({
  step,
  optionSources: externalOptionSources = {},
  fieldRenderers = {},
  isOptionSourcesLoading = false,
  onSubmit,
  onSecondaryAction,
  onRegister,
  onResend,
  descriptionSuffix,
}) => {
  const { common, formData, getStepConfig, isLoading: isConfigLoading, updateField } = useOnboarding();
  const config = getStepConfig(step);
  const fields = useMemo(() => config?.fields ?? [], [config?.fields]);
  const { sources, stateInfo, selectedLanguage, isLoading: arePlatformSourcesLoading } =
    useOnboardingOptionSources(fields, externalOptionSources);

  // Initialize a language field from the user's stored locale only when it has no cross-step value.
  useEffect(() => {
    const languageField = fields.find((field) => LANGUAGE_SOURCES.has(getDataSourceKey(field)));
    if (!languageField || Object.prototype.hasOwnProperty.call(formData, languageField.name)) return;
    const initialValue = selectedLanguage ?? languageField.defaultValue;
    if (initialValue) updateField(languageField.name, initialValue);
  }, [fields, formData, selectedLanguage, updateField]);

  const handleChange = (name, value, field) => {
    updateField(name, value);
    // Source metadata, rather than the field name, determines language side effects.
    if (LANGUAGE_SOURCES.has(getDataSourceKey(field)) && value) {
      // The shared service loads locale resources before changing i18next, so
      // every translated V2 label rerenders immediately after runtime selection.
      void Digit.LocalizationService.changeLanguage(value, stateInfo?.code);
    }
  };

  if (isConfigLoading || arePlatformSourcesLoading || isOptionSourcesLoading) {
    return (
      <div className="onboarding__status">
        Loading <strong>{step}</strong> configuration…
      </div>
    );
  }

  if (!config) {
    // Missing MDMS is a valid temporary V2 state. Keep the requested route
    // mounted instead of falling back to a legacy component.
    return (
      <div className="onboarding__status">
        Configuration missing for <strong>{step}</strong> step.
      </div>
    );
  }

  return (
    <ConfigurableForm
      step={step}
      config={config}
      common={common}
      values={formData}
      optionSources={sources}
      fieldRenderers={fieldRenderers}
      onChange={handleChange}
      // Keep MDMS config as the second argument for existing adapters and pass
      // generic form actions separately for cross-field inline validation.
      onSubmit={(data, formActions) => onSubmit?.(data, config, formActions)}
      onSecondaryAction={onSecondaryAction}
      onRegister={onRegister}
      onResend={onResend}
      descriptionSuffix={descriptionSuffix}
    />
  );
};

export default OnboardingForm;
