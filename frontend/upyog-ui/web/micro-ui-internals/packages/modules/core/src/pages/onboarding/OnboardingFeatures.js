const OnboardingFeatures = ({ config, t }) => {
  if (!config?.isVisible || !config?.items?.length) return null;

  return (
    <ul className="onboarding__features">
      {config.items.map((feature, index) => (
        <li className="onboarding__feature" key={`${feature.title}-${index}`}>
          <div className="onboarding__feature-icon">
            <img src={feature.icon} alt={t(feature.title)} />
          </div>
          <div className="onboarding__feature-info">
            <h3 className="onboarding__feature-title">{t(feature.title)}</h3>
            <p className="onboarding__feature-text">{t(feature.description)}</p>
          </div>
        </li>
      ))}
    </ul>
  );
};

export default OnboardingFeatures;
