import OnboardingForm from "./OnboardingForm";

/**
 * Registration-step adapter. The owning route supplies the registration
 * business flow so this shared component never guesses an OTP destination.
 */
const RegisterV2 = ({ onSubmit, optionSources, fieldRenderers, isOptionSourcesLoading }) => {
  return (
    <OnboardingForm
      step="register"
      optionSources={optionSources}
      fieldRenderers={fieldRenderers}
      isOptionSourcesLoading={isOptionSourcesLoading}
      onSubmit={onSubmit}
    />
  );
};

export default RegisterV2;
