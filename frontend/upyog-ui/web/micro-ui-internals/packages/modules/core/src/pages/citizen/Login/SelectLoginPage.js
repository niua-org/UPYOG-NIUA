import React, { useState } from "react";
import { Dropdown, Loader,Modal,Card} from "@nudmcdgnpm/digit-ui-react-components";

const FAMILY_ID_LENGTH = 12;
const getFamilyIdLength = (value) => value.replace(/-/g, "").length;

const SelectLoginPage = ({ t, onSelect, mobileNumber = "",aadharNumber="", onMobileChange, onAadharChange, onAadharSelect, canSubmit, familyId = "", onFamilyIdChange, onFamilyIdSelect }) => {
  const [error, setError] = useState({ field: "", message: "" });
  const showError = (field, message) => setError({ field, message });
  const clearError = () => setError({ field: "", message: "" });
  const { isLoading, data } = Digit.Hooks.useCustomMDMS(Digit.ULBService.getStateId(), "common-masters", [{ name: "CitizenConsentForm" }]);

  const { data: cities } = Digit.Hooks.useTenants();
  const { data: { languages, stateInfo } = {} } = Digit.Hooks.useStore.getInitData();
  const [selectedLanguage, setSelectedLanguage] = useState(Digit.StoreData.getCurrentLanguage() || "en_IN");
  const [selectedCity, setSelectedCity] = useState(() => {
    const homeCity = Digit.ULBService.getCitizenCurrentTenant(true);
    return homeCity ? { code: homeCity } : null;
  });
  const [showToast, setShowToast] = useState(null);

  // const checkDisbaled = () => !(mobileNumber.length === 10 || aadharNumber.length === 12) && canSubmit
  const checkDisbaled = () => {
    const validCity = !!selectedCity?.code;
    const validMobile = mobileNumber.length === 10;
    const validAadhar = aadharNumber.length === 12;
    const validFamilyId = getFamilyIdLength(familyId) === FAMILY_ID_LENGTH;
    return !(validCity && (validMobile || validAadhar || validFamilyId) && canSubmit);
  };

  const handleMobileChange = (e) => {
    const value = e.target.value;
    if (/^\d{0,10}$/.test(value)) {
      onMobileChange(e);
      clearError();
    } else {
      showError("mobile", t("CORE_COMMON_PROFILE_MOBILE_NUMBER_INVALID"));
    }
  };

  const handleAadharChange = (e) => {
    const value = e.target.value;
    if (/^\d{0,12}$/.test(value)) {
      onAadharChange(e);
      clearError();
    } else {
      showError("aadhar", t("ERR_HRMS_INVALID_AADHAR_NO"));
    }
  };

  const handleFamilyIdChange = (e) => {
    const value = e.target.value.toUpperCase();
    const isAllowedChars = /^[A-Z0-9-]*$/.test(value);
    if (isAllowedChars && getFamilyIdLength(value) <= FAMILY_ID_LENGTH) {
      onFamilyIdChange({ target: { value } });
      clearError();
    } else {
      showError("familyId", t("ERR_INVALID_FAMILY_ID"));
    }
  };

  const handleContinue = () => {
    if (!selectedCity?.code) return;

    if (mobileNumber.length === 10) {
      onSelect({ mobileNumber });
    } else if (aadharNumber.length === 12) {
      onAadharSelect({ aadharNumber });
    } else if (getFamilyIdLength(familyId) === FAMILY_ID_LENGTH) {
      onFamilyIdSelect({ familyId });
    }
  };

  if (isLoading) return <Loader />
  const register = async (e) => {
    const data = await Digit.DigiLockerService.register({ module: "SSO" });
    e.preventDefault()
    const redirectUrl = data.redirectURL
    console.log("data", data)
    localStorage.setItem("code_verfier_register", data?.dlReqRef)
    window.location.href = redirectUrl
  }

  const Heading = (props) => {
    return <h1 className="heading-m">{props.label}</h1>;
  };
  const Close = () => (
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="#FFFFFF">
      <path d="M0 0h24v24H0V0z" fill="none" />
      <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12 19 6.41z" />
    </svg>
  );
  const CloseBtn = (props) => {
    return (
      <div className="icon-bg-secondary" onClick={props.onClick}>
        <Close />
      </div>
    );
  };

  const closeModal =() =>{
    setShowToast(false)
  }
  const setModal=(e)=>{
    setShowToast(false)   
    register(e)
  }

  const handleLanguageSelection = (language) => {
    Digit.LocalizationService.changeLanguage(language.value, stateInfo.code)
    setSelectedLanguage(language.value)
  }

  const handleCitySelection = (city) => {
    Digit.SessionStorage.set("CITIZEN.COMMON.HOME.CITY", city);
    setSelectedCity(city)
  }

  return (
    <div className="login-mobile-step">
      <div className="login-form-header">
        <h2>{"Login to UPYOG"}</h2>
        <p>
          {"All the communications regarding the application will be sent to this mobile number."}
        </p>
      </div>

      <div className="login-form-body">
        <div className="login-field-group">
          <label className="login-label">{"Select Language"}</label>
          <div className="login-language">
            <ul className="login-language-list">
              {languages?.map((language) => (
                <li
                  key={language.label}
                  className={selectedLanguage === language.value ? "is-selected" : ""}
                  onClick={() => handleLanguageSelection(language)}
                >
                 {selectedLanguage === language.value ? (
                    <img src={"/upyog-ui/images/check.svg"} alt="check icon"/>
                 ) : null}&nbsp;{language.label}
                </li>
              ))}
                <li><img src={"/upyog-ui/images/search.svg"} alt="search icon"/></li>
            </ul>
          </div>
        </div>

        <div className="login-field-group">
            <label className="login-label">{"Select City"}</label>
          <div className="city-selection">
            <span className="login-input-icon" aria-hidden="true">
                <img src={"/upyog-ui/images/search.svg"} alt="search icon"/>
            </span>
            <Dropdown
              className="city-selection-dropdown"
              selected={selectedCity}
              select={handleCitySelection}
              option={cities}
              optionKey="i18nKey"
              t={t}
              placeholder={"Search your city"}
            />
          </div>
        </div>

        <div className="login-field-group">
          <label className="login-label">{"Mobile Number"}</label>
          <div className="login-input-wrap">
            <span className="login-prefix">+91</span>
            <input
              type="tel"
              inputMode="numeric"
              pattern="[0-9]*"
              maxLength={10}
              value={mobileNumber || ""}
              placeholder={"Enter 10 digit mobile number"}
              onChange={handleMobileChange}
              disabled={!!aadharNumber || !!familyId}
            />
          </div>
          <p className="login-helper">
            <span className="login-helper-icon" aria-hidden="true">
              <img src={"/upyog-ui/images/secure.svg"} alt="secure"/>
            </span>
            {"We will send an OTP for verification"}
          </p>
          {error.field === "mobile" && <p className="login-error">{error.message}</p>}
        </div>
        <span className="login-divider">{t("OR")}</span>
        <div className="login-field-group">
          <label className="login-label">{t("CORE_COMMON_AADHAR_NUMBER")}</label>
          <div className="login-input-wrap">
            <input
              type="tel"
              inputMode="numeric"
              pattern="[0-9]*"
              maxLength={12}
              value={aadharNumber}
              placeholder={"Enter 12 digit aadhar number"}
              onChange={handleAadharChange}
              disabled={!!mobileNumber || !!familyId}
            />
          </div>
          <p className="login-helper">
            <span className="login-helper-icon" aria-hidden="true">
              <img src={"/upyog-ui/images/secure.svg"} alt="secure"/>
            </span>
            {"We will send an OTP for verification"}
          </p>
          {error.field === "aadhar" && <p className="login-error">{error.message}</p>}
        </div>
        <span className="login-divider">{t("OR")}</span>
        <div className="login-field-group">
          <label className="login-label">{t("CORE_COMMON_FAMILY_ID")}</label>
          <div className="login-input-wrap">
            <input
              type="text"
              maxLength={16}
              value={familyId}
              disabled={!!mobileNumber || !!aadharNumber}
              placeholder={"Enter 12 character family ID"}
              onChange={handleFamilyIdChange}
            />
          </div>
          <p className="login-helper">
            <span className="login-helper-icon" aria-hidden="true">
              <img src={"/upyog-ui/images/secure.svg"} alt="secure"/>
            </span>
            {"We will send an OTP for verification"}
          </p>
          {error.field === "familyId" && <p className="login-error">{error.message}</p>}
        </div>
         <button
          type="button"
          className="login-primary-button"
          disabled={checkDisbaled()}
          onClick={handleContinue}
        >
          {t("CORE_COMMON_CONTINUE")}
        </button>
      </div>

      <div className="login-actions">
        <button
          className="login-secondary-button"
          type="button"
          onClick={(e) => setShowToast(true)}
        >
          <span className="login-secondary-button__content">
            <img src={"/upyog-ui/images/digilocker.svg"} alt="digilocker" />
            {t("CORE_COMMON_DGILOCKER_REGISTER")}
          </span>
        </button>
      </div>

      <p className="login-security">
        <span className="login-security-icon" aria-hidden="true">
          <img src={"/upyog-ui/images/secure.svg"} alt="secure"/>
        </span>
        {"Your information is safe and secure with us."}
      </p>

      {showToast && <Modal
        headerBarMain={<Heading label={"Consent"} />}
        headerBarEnd={<CloseBtn onClick={closeModal} />}
        actionCancelLabel={"Cancel"}
        actionCancelOnSubmit={closeModal}
        actionSaveLabel={"Ok"}
        actionSaveOnSubmit={(e)=>setModal(e)}
        formId="modal-action"
      >
        <div className="w-full">
          <Card>
            <p>{"By selecting this option, I am providing my consent to associate my Upyog account with my DigiLocker ID"}</p>
          </Card>
        </div>
      </Modal>}
    </div>
  );
};

export default SelectLoginPage;
