import { memo, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Controller, useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import OnboardingField from "./OnboardingField";
import { buildSubmittedData, getDataSourceKey, getFieldType, validateField } from "./formUtils";

const getFieldError = (validationErrors, fieldName) =>
  fieldName.split(".").reduce((currentValue, key) => currentValue?.[key], validationErrors);

/**
 * Footer rendering is isolated from watched field values. React.memo keeps the
 * action controls stable while the user types, while isSubmitting still
 * updates the primary button during an active submission.
 */
const ConfigurableFormFooter = memo(
  ({ footer = {}, common, isSubmitting, showPrimaryAction, onConfiguredAction, t }) => {
    const secondaryActions = footer.secondaryActions ?? [];
    const showSeparator = showPrimaryAction && secondaryActions.length > 0 && footer.showSeparator;
    const showSecureInfo = footer.showSecureInfo && common.secureInfo?.text;
    const hasActions = showPrimaryAction || secondaryActions.length > 0;

    // Avoid an empty footer and button group when MDMS supplies neither actions
    // nor secure-information content for the current step.
    if (!hasActions && !showSecureInfo) return null;

    return (
      <div className="onboarding__form-footer">
        {hasActions && (
          <div className={`button-group ${showSeparator ? "gap-lg" : "gap-sm"}`}>
            {showPrimaryAction && (
              <button type="submit" className="button primary" disabled={isSubmitting}>
                {t(footer.primaryAction.label)}
              </button>
            )}
            {/* A separator is meaningful only between primary and secondary actions. */}
            {showSeparator && <div className="separator">{t("OR")}</div>}
            {secondaryActions.map((action) => (
              <button
                type="button"
                key={action.type || action.label}
                className={`button ${action.actionType === "link" ? "text" : "secondary"}`}
                onClick={() => onConfiguredAction(action)}
              >
                <span>
                  {action.icon && <img src={action.icon} alt="" />}
                  {t(action.label)}
                </span>
              </button>
            ))}
          </div>
        )}
        {showSecureInfo && (
          <p>
            {common.secureInfo.icon && <img src={common.secureInfo.icon} alt="" />}
            {t(common.secureInfo.text)}
          </p>
        )}
      </div>
    );
  }
);

ConfigurableFormFooter.displayName = "ConfigurableFormFooter";

/**
 * Config-driven form engine. React Hook Form owns validation and field state;
 * the onboarding container owns cross-step values and side effects.
 */
const ConfigurableForm = ({
  step,
  config,
  common = {},
  values = {},
  optionSources = {},
  fieldRenderers = {},
  onChange,
  onSubmit,
  onSecondaryAction,
  onRegister,
  onResend,
  descriptionSuffix,
}) => {
  const [resendSeconds, setResendSeconds] = useState(0);
  const [isResending, setIsResending] = useState(false);
  const { t } = useTranslation();
  const fields = useMemo(() => config.fields ?? [], [config.fields]);
  const hasConfiguredFields = fields.length > 0;
  // A primary submission without fields has no meaningful payload. Secondary
  // actions remain available because they may start an independent auth flow.
  const showPrimaryAction = hasConfiguredFields && Boolean(config.footer?.primaryAction?.label);
  const defaultValues = useMemo(() => buildSubmittedData(fields, values), [fields, values]);
  const {
    control,
    getValues,
    handleSubmit,
    setError,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm({
    defaultValues,
    mode: "onBlur",
    reValidateMode: "onChange",
    // Custom UPYOG controls expose different ref structures. Disable React
    // Hook Form's automatic focus so it cannot override the ordered handler
    // below (for example, focusing mobile after Select city was chosen).
    shouldFocusError: false,
  });
  const resendDelay = config.footer?.resendAction?.disabledTime ?? 0;

  // Context values can arrive after MDMS/platform queries; hydrate them without marking fields dirty.
  useEffect(() => {
    Object.entries(defaultValues).forEach(([name, value]) => {
      if (getValues(name) !== value) setValue(name, value, { shouldDirty: false, shouldTouch: false });
    });
  }, [defaultValues, getValues, setValue]);

  useEffect(() => {
    // Only OTP-backed steps start a resend countdown.
    setResendSeconds(fields.some((field) => getFieldType(field) === "otp") ? resendDelay : 0);
  }, [fields, resendDelay]);

  useEffect(() => {
    if (resendSeconds <= 0) return undefined;
    const timer = window.setTimeout(() => setResendSeconds((seconds) => seconds - 1), 1000);
    return () => window.clearTimeout(timer);
  }, [resendSeconds]);

  // Keep the latest route callbacks available without changing the footer's
  // click handler identity whenever its parent recreates an adapter function.
  const configuredActionContext = useRef({ fields, onRegister, onSecondaryAction });
  configuredActionContext.current = { fields, onRegister, onSecondaryAction };

  const handleResend = async () => {
    // Guard against duplicate requests while the timer or API call is active.
    if (resendSeconds > 0 || isResending) return;
    setIsResending(true);
    try {
      await onResend?.();
      setResendSeconds(resendDelay);
    } catch {
      // The route adapter owns API feedback (normally Toast). Keep the timer at
      // zero after failure so the user can retry without an unhandled rejection.
    } finally {
      setIsResending(false);
    }
  };

  const focusFirstInvalidField = (validationErrors) => {
    // Follow configured field order instead of object-key order so multiple
    // validation failures always focus the first input shown to the user.
    const firstInvalidField = fields.find((field) => getFieldError(validationErrors, field.name));
    if (!firstInvalidField) return;

    window.requestAnimationFrame(() => {
      const fieldGroup = Array.from(document.querySelectorAll(".onboarding__step .form-group"))
        .find((element) => element.dataset.fieldName === firstInvalidField.name);
      if (!fieldGroup) return;

      const focusTarget = fieldGroup.querySelector(
        'input:not([type="hidden"]):not([disabled]), select:not([disabled]), textarea:not([disabled]), button:not([disabled]), [tabindex]:not([tabindex="-1"])'
      );
      fieldGroup.scrollIntoView({ behavior: "smooth", block: "center" });
      (focusTarget || fieldGroup).focus({ preventScroll: true });
    });
  };

  const submitForm = (data) =>
    onSubmit?.(buildSubmittedData(fields, data), {
      // Route adapters can report cross-field validation (for example,
      // mismatched passwords) through the same inline error/focus treatment as
      // MDMS field rules. Toast remains reserved for API failures.
      setFieldError: (fieldName, message) => {
        const fieldError = { type: "manual", message };
        setError(fieldName, fieldError);
        focusFirstInvalidField({ [fieldName]: fieldError });
      },
    });

  const handleConfiguredAction = useCallback(
    (action) => {
      // Build the payload on demand so ordinary field changes do not perform
      // work that is needed only when a secondary action is selected.
      const {
        fields: configuredFields,
        onRegister: registerAction,
        onSecondaryAction: secondaryAction,
      } = configuredActionContext.current;
      const formData = buildSubmittedData(configuredFields, getValues());

      // Registration is a separate onboarding transition, not an alternate login
      // method. The configuration's existing type discriminator keeps DigiLocker
      // and other secondary authentication actions on onSecondaryAction.
      if (String(action?.type).toLowerCase() === "register") {
        registerAction?.(action, formData);
        return;
      }
      secondaryAction?.(action, formData);
    },
    [getValues]
  );

  return (
    <form className="onboarding__step" noValidate onSubmit={handleSubmit(submitForm, focusFirstInvalidField)}>
      <div className="onboarding__form-header">
        {config.heading && <h2>{t(config.heading)}</h2>}
        {config.description && (
          <p>
            {t(config.description)}
            {descriptionSuffix ? ` ${descriptionSuffix}` : ""}
          </p>
        )}
      </div>

      <div className="onboarding__form-body">
        {!hasConfiguredFields && (
          <div className="onboarding__status" role="status">
            <strong>No fields</strong> are configured for the <strong>{step?.toUpperCase() || "REQUESTED"}</strong> step in MDMS.
          </div>
        )}
        {fields.map((fieldConfig) => {
          // A source may be a plain options array or include renderer metadata such as optionKey/valueKey.
          const source = optionSources[getDataSourceKey(fieldConfig)];
          const sourceConfig = Array.isArray(source) ? { options: source } : (source ?? {});
          const field = { ...sourceConfig, ...fieldConfig };

          return (
            <div
              className={`form-group${getFieldError(errors, field.name) ? " error" : ""}`}
              data-field-name={field.name}
              key={field.name}
              tabIndex={-1}
            >
              {field.label && <label htmlFor={field.name}>{t(field.label)}</label>}
              <Controller
                name={field.name}
                control={control}
                defaultValue={field.defaultValue ?? ""}
                rules={{ validate: (value) => validateField(field, value) || true }}
                render={({ field: formField, fieldState }) => (
                  <OnboardingField
                    field={field}
                    value={formField.value ?? ""}
                    options={field.options ?? sourceConfig.options ?? []}
                    error={fieldState.error?.message}
                    config={config}
                    t={t}
                    inputRef={formField.ref}
                    onChange={(value) => {
                      // Explicitly revalidate an already-invalid field on its
                      // first value change. This is important for controlled
                      // UPYOG dropdowns whose value is also mirrored to context;
                      // untouched fields still follow the form's onBlur mode.
                      setValue(field.name, value, {
                        shouldDirty: true,
                        shouldValidate: Boolean(fieldState.error),
                      });
                      // Mirror values to onboarding context so another step can reuse them.
                      onChange?.(field.name, value, fieldConfig);
                    }}
                    onBlur={formField.onBlur}
                    customRenderer={fieldRenderers[getFieldType(field)]}
                    resendState={{ seconds: resendSeconds, isResending, onResend: handleResend }}
                  />
                )}
              />
            </div>
          );
        })}
      </div>

      <ConfigurableFormFooter
        footer={config.footer}
        common={common}
        isSubmitting={isSubmitting}
        showPrimaryAction={showPrimaryAction}
        onConfiguredAction={handleConfiguredAction}
        t={t}
      />
    </form>
  );
};

export default ConfigurableForm;
