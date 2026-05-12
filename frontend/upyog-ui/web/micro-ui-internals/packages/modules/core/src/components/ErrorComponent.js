import React from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";

const ErrorConfig = {
  error: {
    imgUrl: `https://s3.ap-south-1.amazonaws.com/egov-qa-assets/error-image.png`,
    infoMessage: "CORE_SOMETHING_WENT_WRONG",
    buttonInfo: "ACTION_TEST_HOME",
  },
  maintenance: {
    imgUrl: `https://s3.ap-south-1.amazonaws.com/egov-qa-assets/maintainence-image.png`,
    infoMessage: "CORE_UNDER_MAINTENANCE",
    buttonInfo: "ACTION_TEST_HOME",
  },
  notfound: {
    imgUrl: `https://s3.ap-south-1.amazonaws.com/egov-qa-assets/PageNotFound.png`,
    infoMessage: "CORE_NOT_FOUND",
    buttonInfo: "ACTION_TEST_HOME",
  },
};

const ErrorComponent = (props) => {
  const { type = "error" } = Digit.Hooks.useQueryParams();
  const config = ErrorConfig[type];
  const { t } = useTranslation();
  
  let navigate = null;
  try {
    navigate = useNavigate();
  } catch (e) {
    // Not in Router context, will use window.location instead
  }

  const stateInfo = props.stateInfo;

  const handleGoHome = () => {
    if (props.goToHome && typeof props.goToHome === 'function') {
      props.goToHome();
    } else if (navigate) {
      navigate("/upyog-ui/employee");
    } else {
      window.location.href = "/upyog-ui/employee";
    }
  };

  return (
    <div className="error-boundary">
      <div className="error-container">
        <img src={config.imgUrl} alt="error" />
        <h1>{t(config.infoMessage)}</h1>
        <button onClick={handleGoHome}>
          {t(config.buttonInfo)}
        </button>
      </div>
    </div>
  );
};

export default ErrorComponent;
