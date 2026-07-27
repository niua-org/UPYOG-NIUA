import { CustomButton, DatePicker, Dropdown, MobileNumber, OTPInput, TextInput } from "@nudmcdgnpm/digit-ui-react-components";
import { getFieldType, getOptionLabel, getOptionValue } from "./formUtils";

const FieldError = ({ id, message }) =>
  message ? (
    <p className="error-message" id={id} role="alert">
      {message}
    </p>
  ) : null;

/**
 * Standard renderer registry for supported MDMS field types. A caller can
 * override any type with customRenderer without changing this component.
 */
const OnboardingField = ({
  field,
  value,
  options = [],
  error,
  config,
  t,
  inputRef,
  onChange,
  onBlur,
  customRenderer,
  resendState,
}) => {
  const fieldType = getFieldType(field);
  const errorId = `${field.name}-error`;
  const accessibilityProps = {
    "aria-invalid": Boolean(error),
    "aria-describedby": error ? errorId : undefined,
  };

  // Custom renderers receive the same controlled-field contract as built-in controls.
  if (customRenderer) {
    return customRenderer({ field, value, options, error, inputRef, setValue: onChange, onBlur });
  }

  if (fieldType === "otp") {
    // OTP countdown/resend state stays at form level so it survives field rerenders.
    const resendAction = config?.footer?.resendAction ?? {};
    const resendLabel = resendAction.label || "Resend";
    const countdownText = (resendAction.countdownText || "in {seconds} seconds")
      .replace("{{seconds}}", resendState.seconds)
      .replace("{seconds}", resendState.seconds);

    return (
      <>
        <div id={field.name} className="otp-field" {...accessibilityProps}>
          <OTPInput placeholder={"X"} length={Number(field.otpLength) || 6} value={value} onChange={onChange} />
        </div>
        {config?.otpInfo?.enabled && (
          <p className="helper-text" aria-live="polite">
            {resendState.seconds > 0 ? (
              <>
                <button type="button" disabled>
                  {t(resendLabel)}
                </button>
                <span>{countdownText}</span>
              </>
            ) : (
              <>
                <span>{t(config.otpInfo.text)}</span>
                <button type="button" disabled={resendState.isResending} onClick={resendState.onResend}>
                  {resendState.isResending ? t("CORE_COMMON_SENDING") : t(resendLabel)}
                </button>
              </>
            )}
          </p>
        )}
        <FieldError id={errorId} message={error} />
      </>
    );
  }

  if (fieldType === "singleSelectButtonGroup") {
    // Store the configured primitive value rather than the full option object.
    return (
      <>
        <div className="languages" id={field.name} {...accessibilityProps}>
          <ul>
            {options.map((option) => {
              const optionValue = getOptionValue(option, field);
              const isSelected = optionValue === value;
              return (
                <li className={isSelected ? "selected" : ""} key={optionValue}>
                  <CustomButton selected={isSelected} text={t(getOptionLabel(option, field))} onClick={() => onChange(optionValue)} />
                </li>
              );
            })}
          </ul>
          {options.length > 3 && field.searchIcon && (
            <button type="button" aria-label={`Search ${t(field.label || field.name)}`}>
              <img src={field.searchIcon} alt="" />
            </button>
          )}
        </div>
        <FieldError id={errorId} message={error} />
      </>
    );
  }

  // Any field with a resolved option source is selectable, even when an older
  // MDMS config uses a domain-specific type such as "city" instead of "dropdown".
  if (["dropdown", "select", "autocomplete"].includes(fieldType)) {
    const selectedOption =
      typeof value === "object" ? value : options.find((option) => getOptionValue(option, field) === value) || null;
    return (
      <>
        <div className="form-field">
          {field.startIcon && <em><img src={field.startIcon} alt="" /></em>}
          <Dropdown
            id={field.name}
            option={options}
            optionKey={field.optionKey || field.labelKey || "label"}
            selected={selectedOption}
            select={onChange}
            placeholder={t(field.placeholder)}
            errorStyle={Boolean(error)}
            onBlur={onBlur}
            t={t}
          />
        </div>
        <FieldError id={errorId} message={error} />
      </>
    );
  }

  const commonInputProps = {
    id: field.name,
    name: field.name,
    value,
    placeholder: t(field.placeholder),
    maxlength: field.validation?.maxLength,
    minlength: field.validation?.minLength,
    errorStyle: Boolean(error),
    inputRef,
    onBlur,
  };
  const input =
    // DIGIT controls expose different change signatures, normalized here for React Hook Form.
    fieldType === "tel" ? (
      <MobileNumber {...commonInputProps} onChange={onChange} />
    ) : fieldType === "date" ? (
      <DatePicker date={value} min={field.validation?.min} max={field.validation?.max} onChange={onChange} />
    ) : (
      <TextInput {...commonInputProps} type={fieldType} onChange={(event) => onChange(event.target.value)} />
    );

  return (
    <>
      <div className="form-field">
        {field.startIcon && <em><img src={field.startIcon} alt="" /></em>}
        {field.prefix && fieldType !== "tel" && <span>{field.prefix}</span>}
        {input}
      </div>
      {field.helperText && (
        <p className="helper-text">
          {field.helperIcon && <img src={field.helperIcon} alt="" />}
          {t(field.helperText)}
        </p>
      )}
      <FieldError id={errorId} message={error} />
    </>
  );
};

export default OnboardingField;
