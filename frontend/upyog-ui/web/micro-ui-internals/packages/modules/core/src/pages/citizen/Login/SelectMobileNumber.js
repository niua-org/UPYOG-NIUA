import React, { useState, useEffect } from "react";
import { FormStep, CitizenConsentForm, Loader, CheckBox } from "@nudmcdgnpm/digit-ui-react-components";
import { startCitizenDigiLockerLogin } from "./citizenAuth";
import DigiLockerConsentModal from "./DigiLockerConsentModal";

const SelectMobileNumber = ({ t, onSelect, showRegisterLink, mobileNumber, onMobileChange, config, canSubmit }) => {
  const [isCheckBox, setIsCheckBox] = useState(false);
  const [isCCFEnabled, setisCCFEnabled] = useState(false);
  const [mdmsConfig, setMdmsConfig] = useState("");
  const [error, setError] = useState("");
  const { isLoading, data } = Digit.Hooks.useCustomMDMS(Digit.ULBService.getStateId(), "common-masters", [{ name: "CitizenConsentForm" }]);
  const [showToast, setShowToast] = useState(null);
  function setTermsAndPolicyDetails(e) {
    setIsCheckBox(e.target.checked);
  }

  const checkDisbaled = () => {
    if (isCCFEnabled?.isCitizenConsentFormEnabled) {
      return !(mobileNumber.length === 10 && canSubmit && isCheckBox);
    } else {
      return !(mobileNumber.length === 10 && canSubmit);
    }
  };

  useEffect(() => {
    if (data?.["common-masters"]?.CitizenConsentForm?.[0]?.isCitizenConsentFormEnabled) {
      setisCCFEnabled(data?.["common-masters"]?.CitizenConsentForm?.[0]);
    }
  }, [data]);

  const onLinkClick = (e) => {
    setMdmsConfig(e.target.id);
  };

  const checkLabels = () => {
    return (
      <span>
        {isCCFEnabled?.checkBoxLabels?.map((data, index) => {
          return (
            <span key={data?.linkId || index}>
              {/* {index == 0 && "CCF"} */}
              {data?.linkPrefix && <span>{t(`${data?.linkPrefix}_`)}</span>}
              {data?.link && (
                <span
                  id={data?.linkId}
                  onClick={(e) => {
                    onLinkClick(e);
                  }}
                  style={{ color: "#a82227", cursor: "pointer" }}
                >
                  {t(`${data?.link}_`)}
                </span>
              )}
              {data?.linkPostfix && <span>{t(`${data?.linkPostfix}_`)}</span>}
              {index == isCCFEnabled?.checkBoxLabels?.length - 1 && t("LABEL")}
            </span>
          );
        })}
      </span>
    );
  };
  const validateMobileNumber = () => {
    if (/^\d{0,10}$/.test(mobileNumber)) {
      setError("");
    }
  };
  const handleMobileChange = (e) => {
    const value = e.target.value;
    if (/^\d{0,10}$/.test(value) || value === "") {
      onMobileChange(e);
      validateMobileNumber();
    } else {
      setError(t("CORE_COMMON_PROFILE_MOBILE_NUMBER_INVALID"));
    }
  };
  if (isLoading) return <Loader />;
  const register = async (e) => {
    e?.preventDefault();
    // Both onboarding versions use the same DigiLocker authorization request.
    await startCitizenDigiLockerLogin();
  };

  const closeModal = () => {
    setShowToast(false);
  };
  const setModal = (e) => {
    setShowToast(false);
    register(e);
  };

  return (
    <FormStep
      isDisabled={checkDisbaled()}
      onSelect={onSelect}
      config={config}
      t={t}
      componentInFront="+91"
      onChange={handleMobileChange}
      value={mobileNumber}
    >
      {error && <p style={{ color: "red" }}>{error}</p>}
      {isCCFEnabled?.isCitizenConsentFormEnabled && (
        <div>
          <CheckBox
            className="form-field"
            label={checkLabels()}
            value={isCheckBox}
            checked={isCheckBox}
            style={{ marginTop: "5px", marginLeft: "55px" }}
            styles={{ marginBottom: "30px" }}
            onChange={setTermsAndPolicyDetails}
          />

          <CitizenConsentForm
            styles={{}}
            t={t}
            isCheckBoxChecked={setTermsAndPolicyDetails}
            labels={isCCFEnabled?.checkBoxLabels}
            mdmsConfig={mdmsConfig}
            setMdmsConfig={setMdmsConfig}
          />
        </div>
      )}
      <div className="col col-md-4  text-md-center p-0" style={{ width: "40%", marginTop: "5px" }}>
        <button className="digilocker-btn" type="button" onClick={() => setShowToast(true)}>
          <img src="https://meripehchaan.gov.in/assets/img/icon/digi.png" className="mr-2" style={{ width: "12%" }} />
          {t("CORE_COMMON_DGILOCKER_REGISTER")}
        </button>
        <DigiLockerConsentModal isOpen={showToast} onCancel={closeModal} onConfirm={setModal} t={t} />
      </div>
    </FormStep>
  );
};

export default SelectMobileNumber;
