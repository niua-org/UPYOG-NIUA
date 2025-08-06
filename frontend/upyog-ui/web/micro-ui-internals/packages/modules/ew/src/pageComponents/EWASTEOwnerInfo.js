import React, { useEffect, useState } from "react";
import { FormStep, TextInput, CardLabel, RadioButtons, LabelFieldPair, Dropdown, Menu, MobileNumber } from "@upyog/digit-ui-react-components";
import { useLocation, useRouteMatch } from "react-router-dom";
import { Controller, useForm } from "react-hook-form";
import Timeline from "../components/EWASTETimeline";

const EWOwnerDetails = ({ t, config, onSelect, userType, formData, ownerIndex }) => {
  const { pathname: url } = useLocation();

  let index = 0;
  let validation = {};

  const user = Digit.UserService.getUser().info;
  const [applicantName, setName] = useState(
    (formData.ownerKey && formData.ownerKey[index] && formData.ownerKey[index].applicantName) || formData?.ownerKey?.applicantName || ""
  );
  const [emailId, setEmail] = useState(
    (formData.ownerKey && formData.ownerKey[index] && formData.ownerKey[index].emailId) || formData?.ownerKey?.emailId || ""
  );
  const [mobileNumber, setMobileNumber] = useState(
    (formData.ownerKey && formData.ownerKey[index] && formData.ownerKey[index].mobileNumber) || formData?.ownerKey?.mobileNumber || user?.mobileNumber
  );
  const [altMobileNumber, setAltMobileNumber] = useState(
    (formData.ownerKey && formData.ownerKey[index] && formData.ownerKey[index].altMobileNumber) || formData?.ownerKey?.altmobileNumber || ""
  );

  const tenantId = Digit.ULBService.getCurrentTenantId();
  const stateId = Digit.ULBService.getStateId();
  const { control } = useForm();

  function setOwnerName(e) {
    setName(e.target.value);
  }

  function setMobileNo(e) {
    setMobileNumber(e.target.value);
  }

  function setAltMobileNo(e) {
    setAltMobileNumber(e.target.value);
  }

  function setOwnerEmail(e) {
    setEmail(e.target.value);
  }



  const goNext = () => {
    let owner = formData.ownerKey && formData.ownerKey[index];
    let ownerStep;
    if (userType === "citizen") {
      ownerStep = { ...owner, applicantName, mobileNumber, altMobileNumber, emailId };
      onSelect(config.key, { ...formData[config.key], ...ownerStep }, false, index);
    } else {
      ownerStep = { ...owner, applicantName, mobileNumber, altMobileNumber, emailId };
      onSelect(config.key, ownerStep, false, index);
    }
  };

  const onSkip = () => onSelect();

  useEffect(() => {
    if (userType === "citizen") {
      goNext();
    }
  }, [applicantName, mobileNumber, emailId]);

  return (
    <React.Fragment>
      {window.location.href.includes("/citizen") ? <Timeline currentStep={3} /> : null}

      <FormStep
        config={config}
        onSelect={goNext}
        onSkip={onSkip}
        t={t}
        isDisabled={!applicantName || !mobileNumber || !emailId}
      >
        <div>
          <CardLabel>{`${t("EWASTE_APPLICANT_NAME")}`}</CardLabel>
          <TextInput
            t={t}
            type={"text"}
            isMandatory={false}
            optionKey="i18nKey"
            name="applicantName"
            value={applicantName}
            onChange={setOwnerName}
            ValidationRequired={true}
            {...(validation = {
              isRequired: true,
              pattern: "^[a-zA-Z ]+$",
              type: "text",
              title: t("EW_ENTER_CORRECT_NAME"),
            })}
          />

          <CardLabel>{`${t("EWASTE_MOBILE_NUMBER")}`}</CardLabel>
          <MobileNumber
            value={mobileNumber}
            name="mobileNumber"
            onChange={(value) => setMobileNo({ target: { value } })}
            {...{ required: true, pattern: "[6-9]{1}[0-9]{9}", type: "tel",}}
          />

          <CardLabel>{`${t("EWASTE_ALT_MOBILE_NUMBER")}`}</CardLabel>
          <MobileNumber
            value={altMobileNumber}
            name="altmobileNumber"
            onChange={(value) => setAltMobileNo({ target: { value } })}
            {...{ required: true, pattern: "[6-9]{1}[0-9]{9}", type: "tel", }}
          />

          <CardLabel>{`${t("EWASTE_EMAIL_ID")}`}</CardLabel>
          <TextInput
            t={t}
            type={"email"}
            isMandatory={true}
            optionKey="i18nKey"
            name="emailId"
            value={emailId}
            onChange={setOwnerEmail}
            ValidationRequired={true}
            {...(validation = {
              isRequired: true,
              pattern: "[A-Za-z]{i}\.[A-Za-z]{i}\.[A-Za-z]{i}",
              type: "email",
            })}
          />
        </div>
      </FormStep>
    </React.Fragment>
  );
};

export default EWOwnerDetails;
