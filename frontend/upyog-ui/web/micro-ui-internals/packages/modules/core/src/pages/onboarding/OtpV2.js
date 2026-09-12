import { Navigate, useLocation, useParams } from "react-router-dom";
import OnboardingForm from "./OnboardingForm";
import { maskMobileNumber } from "./formUtils";

/**
 * OTP-step adapter. Route state provides the mobile number while parent
 * callbacks own verification and resend APIs for the active flow.
 */
const OtpV2 = ({
  onSubmit,
  onResend,
  optionSources,
  fieldRenderers,
  isOptionSourcesLoading,
  isFlowValid,
  invalidFlowFallback,
  mobileNumber: mobileNumberProp,
}) => {
  const { flow } = useParams();
  const location = useLocation();
  // Route adapters may provide persisted context for refresh; navigation state
  // remains supported for other onboarding consumers.
  const mobileNumber = mobileNumberProp || location.state?.mobileNumber;
  const maskedMobileNumber = maskMobileNumber(mobileNumber);

  // Reject unsupported flow parameters with a replace redirect. Valid login
  // and register OTP URLs still render on refresh even when route state is absent.
  if (isFlowValid && !isFlowValid(flow)) {
    return <Navigate to={invalidFlowFallback} replace />;
  }

  // Verification and resend APIs remain owned by the route parent.
  const handleSubmit = (data, config) => onSubmit?.(data, config, flow);
  const handleResend = () => onResend?.(flow);

  return (
    <OnboardingForm
      step="otp"
      optionSources={optionSources}
      fieldRenderers={fieldRenderers}
      isOptionSourcesLoading={isOptionSourcesLoading}
      descriptionSuffix={maskedMobileNumber}
      onSubmit={handleSubmit}
      onResend={handleResend}
    />
  );
};

export default OtpV2;
