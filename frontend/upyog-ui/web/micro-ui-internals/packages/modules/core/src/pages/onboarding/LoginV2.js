import OnboardingForm from "./OnboardingForm";

/**
 * Login-step adapter shared by citizen and employee routes. The route parent
 * supplies onSubmit so each user type can keep its own navigation/API flow.
 */
const LoginV2 = ({ onSubmit, onSecondaryAction, onRegister, optionSources, fieldRenderers, isOptionSourcesLoading }) => {
  // Registration has its own callback so onSecondaryAction remains reserved
  // for configured alternate login providers such as DigiLocker.
  return (
    <OnboardingForm
      step="login"
      optionSources={optionSources}
      fieldRenderers={fieldRenderers}
      isOptionSourcesLoading={isOptionSourcesLoading}
      onSubmit={onSubmit}
      onSecondaryAction={onSecondaryAction}
      onRegister={onRegister}
    />
  );
};

export default LoginV2;
