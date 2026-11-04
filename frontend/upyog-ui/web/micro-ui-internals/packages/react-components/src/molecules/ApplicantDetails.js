/**
 * This component is the Common Component used for Applicant Details in both Citizen and Employee Side.
 * Key of this Component is "owner", use this in your config file inside Key
 * 
 * This Component have some additional Field such as Gender, Gaurdian as well as relatioshipType which will render 
 * according to the flag if true then render otherwise not.
 * 
 * To Set the Flag you need to make an object in your Config or you can dirctly copy and paste this object in your Config - 
 * 
 *              "additionaFields":{
                    "gender":true,
                    "dateofBirth":false,
                    "guardianName":false,
                    "relationShipType":false,
                }
 * 
 * When using this component, you can customize fields using the following props:
 * 
 * 1. `applicantData` (Type: Object):
 *    - Payload object to pre-fill fields. Use this exact structure:
 * 
 * 2. `labels` (Type: Object):
 *    - Key-value map of localization keys to dynamically change header or any CardLabel.
 *    - Available keys: header, applicantName, mobileNumber, alternateNumber, gender, dateOfBirth, guardianName, relationShipType, emailId
 *    - Example: labels={{ header: "ES_TITILE_OWNER_DETAILS", applicantName: "GC_OWNER_NAME", mobileNumber: "GC_OWNER_MOBILE" }}
 * 
 * 3. `disableFields` (Type: Object):
 *    - Key-value boolean map to disable specific fields.
 *    - Available keys: applicantName, mobileNumber, alternateNumber, gender, dateOfBirth, guardianName, relationShipType, emailId
 *    - Example: disableFields={{ applicantName: true, mobileNumber: true }}
 * 
 * <ApplicantDetails
 *   labels={{                              // (Optional) Object: Custom i18n keys for header and card labels
 *     header: "ES_TITILE_OWNER_DETAILS",
 *     applicantName: "GC_OWNER_NAME",
 *     mobileNumber: "GC_OWNER_MOBILE_NUMBER",
 *     gender: "GC_OWNER_GENDER",
 *     guardianName: "GC_OWNER_GUARDIAN",
 *     emailId: "GC_OWNER_EMAIL"
 *   }}
 *   disableFields={{                       // (Optional) Object: Pass true to disable specific fields
 *     applicantName: true,
 *     mobileNumber: true
 *   }}
 *   applicantData={{                       // (Optional) Object: Standardized payload structure
 *     applicantName: "...",                // Applicant full name
 *     mobileNumber: "...",                 // 10-digit mobile number
 *     emailId: "...",                      // Email address
 *     gender: "...",                       // Gender code (e.g. "MALE", "FEMALE") or object
 *     dateOfBirth: "...",                  // Date of birth (YYYY-MM-DD)
 *     guardianName: "...",                 // Father / Husband name
 *     relationShipType: "...",             // Relation (e.g. "FATHER", "HUSBAND") or object
 *     alternateNumber: "..."               // Alternate contact number
 *   }}
 *   ...otherProps
 * />
 *  
 * TODO:- 1. Need to remove extra OR (||) fallback conditions (e.g., renewApplication, formData?.infodetails?.existingDataSet) across state initializations. Calling modules across Edit, Renew & Draft should send data using the standardized `applicantData` prop payload documented above.
 */



import React, { useEffect, useState } from "react";
import { FormStep, TextInput, CardLabel, MobileNumber, RadioButtons, Dropdown } from "../index";

const ApplicantDetails = ({ t, config, onSelect, formData, renewApplication = {}, applicantData, labels = {}, disableFields = {} }) => {
  const user = Digit.UserService.getUser().info;
  const inputStyles = { width: user.type === "EMPLOYEE" ? "50%" : "100%" };
  let validation = {};
  const [applicantName, setName] = useState(applicantData?.applicantName || (user.type === "EMPLOYEE" ?"":user?.name) || formData?.owner?.applicantName || formData?.infodetails?.existingDataSet?.owner?.applicantName || renewApplication?.additionalDetail?.applicantDetails?.[0]?.applicantName || "");
  const [mobileNumber, setMobileNumber] = useState(applicantData?.mobileNumber || (user.type === "EMPLOYEE" ?"":user?.mobileNumber) ||formData?.owner?.mobileNumber || formData?.infodetails?.existingDataSet?.owner?.mobileNumber || renewApplication?.additionalDetail?.applicantDetails?.[0]?.mobileNumber || "");
  const [gender, setGender]=useState(applicantData?.gender || formData?.owner?.gender||"");
  const [dateOfBirth, setDateofBirth] = useState(applicantData?.dateOfBirth || formData?.owner?.dateOfBirth|| formData?.owner?.requestDetails?.applicantName || "");
  const [emailId, setEmail] = useState(applicantData?.emailId || (user.type === "EMPLOYEE" ?"":user?.emailId)||formData?.owner?.emailId ||  formData?.infodetails?.existingDataSet?.owner?.emailId || renewApplication?.additionalDetail?.applicantDetails?.[0]?.emailId || "");
  const [alternateNumber, setAltMobileNumber] = useState(applicantData?.alternateNumber || formData?.owner?.alternateNumber ||  formData?.infodetails?.existingDataSet?.owner?.alternateNumber || renewApplication?.additionalDetail?.applicantDetails?.[0]?.alternateNumber || "");
  const [guardianName, setGuardian] = useState(applicantData?.guardianName || formData?.owner?.guardianName || "");
  const [relationShipType, setRelationShipType] = useState(applicantData?.relationShipType || formData?.owner?.relationShipType || "");

  useEffect(() => {
    if (applicantData) {
      if (applicantData.applicantName) setName(applicantData.applicantName);
      if (applicantData.mobileNumber) setMobileNumber(applicantData.mobileNumber);
      if (applicantData.gender) setGender(applicantData.gender);
      if (applicantData.dateOfBirth) setDateofBirth(applicantData.dateOfBirth);
      if (applicantData.emailId) setEmail(applicantData.emailId);
      if (applicantData.alternateNumber) setAltMobileNumber(applicantData.alternateNumber);
      if (applicantData.guardianName) setGuardian(applicantData.guardianName);
      if (applicantData.relationShipType) setRelationShipType(applicantData.relationShipType);
    }
  }, [applicantData]);


  function setOwnerName(e) {
    setName(e.target.value);
  }
  function setOwnerEmail(e) {
    setEmail(e.target.value);
  }
  function setMobileNo(e) {
    setMobileNumber(e.target.value);
  }
  function setAltMobileNo(e) {
    setAltMobileNumber(e.target.value);
  }
  function setGuardiansName(e) {
    setGuardian(e.target.value);
  }

  function setBirthDate(e){
    setDateofBirth(e.target.value);
  }
  
  const { data: applicantGender } = Digit.Hooks.useEnabledMDMS(Digit.ULBService.getStateId(), "common-masters", [{ name: "GenderType" }],
    {
      select: (data) => {
        const formattedData = data?.["common-masters"]?.["GenderType"]
        return formattedData;
      },
    });

    let genderOptions = [];  

    applicantGender &&
      applicantGender.map((genderoption) => {
        if (genderoption.code !== "TRANSGENDER")
          genderOptions.push({ i18nKey: `${genderoption.code}`, code: `${genderoption.code}`, name: `${genderoption.code}` });
      });


  const GuardianOptions = [
    { name: "Husband", code: "HUSBAND", i18nKey: "COMMON_HUSBAND" },
    { name: "Father", code: "FATHER", i18nKey: "COMMON_FATHER" },
  ];
  
  const goNext = () => {
    let owner = formData.owner;
    let applicantDetails = { ...owner, applicantName, mobileNumber, gender,dateOfBirth,alternateNumber,relationShipType, guardianName, emailId};
    onSelect(config.key, applicantDetails, false);
  };

  return (
    <React.Fragment>
    <FormStep
      config={config}
      onSelect={goNext}
      t={t}
      isDisabled={!applicantName || 
        !mobileNumber || 
        (config?.additionaFields?.guardianName)?!guardianName:null  || 
        (config?.additionaFields?.dateofBirth) ? !dateOfBirth:null  || 
        (config?.additionaFields?.gender) ? !gender:null}
    >
      <div>
        <CardLabel>{`${t(labels?.applicantName || "COMMON_APPLICANT_NAME")}`} <span className="astericColor">*</span></CardLabel>
        <TextInput
          t={t}
          type={"text"}
          isMandatory={false}
          optionKey="i18nKey"
          name="applicantName"
          value={applicantName}
          style={inputStyles}
          onChange={setOwnerName}
          disable={disableFields?.applicantName}
          ValidationRequired = {true}
          {...(validation = {
            isRequired: true,
            pattern: "^[a-zA-Z ]+$",
            title: t("PT_NAME_ERROR_MESSAGE"),
          })}
        />
        {(config?.additionaFields?.gender) ? 
        <React.Fragment>       
        <CardLabel>{`${t(labels?.gender || "COMMON_GENDER")}`} <span className="astericColor">*</span></CardLabel>
        <RadioButtons
            t={t}
            options={genderOptions}
            additionalWrapperClass="rc-applicant-details-flex-container"
            style={{ display: "flex", flexWrap: "wrap", alignItems: "center", gap: "24px", marginBottom: "16px", maxHeight: "none" }}
            innerStyles={{ display: "flex", alignItems: "center", minWidth: "auto" }}
            optionsKey="i18nKey"
            name={`gender`}
            value={gender}
            selectedOption={gender}
            onSelect={setGender}
            disabled={disableFields?.gender}
            labelKey="i18nKey"
            isPTFlow={true}
        />
        </React.Fragment>:null}
      
        <CardLabel>{`${t(labels?.mobileNumber || "COMMON_MOBILE_NUMBER")}`} <span className="astericColor">*</span></CardLabel>
        <MobileNumber
          value={mobileNumber}
          name="mobileNumber"
          onChange={(value) => setMobileNo({ target: { value } })}
          disable={disableFields?.mobileNumber}
          style={inputStyles}
          {...{ required: true, pattern: "[6-9]{1}[0-9]{9}",  title: t("CORE_COMMON_APPLICANT_MOBILE_NUMBER_INVALID") }}
        />

        <CardLabel>{`${t(labels?.alternateNumber || "COMMON_ALT_MOBILE_NUMBER")}`}</CardLabel>
          <MobileNumber
            value={alternateNumber}
            name="alternateNumber"
            onChange={(value) => setAltMobileNo({ target: { value } })}
            disable={disableFields?.alternateNumber}
            style={inputStyles}
            {...{ required: false, pattern: "[6-9]{1}[0-9]{9}", title: t("CORE_COMMON_APPLICANT_MOBILE_NUMBER_INVALID") }}
          />
        {(config?.additionaFields?.dateofBirth)?
        <React.Fragment>
        <CardLabel>{`${t(labels?.dateOfBirth || "COMMON_BIRTH_DATE")}`} <span className="astericColor">*</span></CardLabel>
        <TextInput
            t={t}
            type={"date"}
            isMandatory={false}
            optionKey="i18nKey"
            name="dateOfBirth"
            value={dateOfBirth}
            onChange={setBirthDate}
            disable={disableFields?.dateOfBirth}
            style={inputStyles}
            max={new Date().toISOString().split('T')[0]}
            rules={{
            required: t("CORE_COMMON_REQUIRED_ERRMSG"),
            validDate: (val) => (/^\d{4}-\d{2}-\d{2}$/.test(val) ? true : t("ERR_DEFAULT_INPUT_FIELD_MSG")),
            }}
        />
        </React.Fragment>:null}  
        
        {(config?.additionaFields?.guardianName)?
        <React.Fragment>
        <CardLabel>{`${t(labels?.guardianName || "COMMON_GUARDIAN")}`} <span className="astericColor">*</span></CardLabel>
        <TextInput
          t={t}
          type={"text"}
          isMandatory={false}
          optionKey="i18nKey"
          name="guardianName"
          style={inputStyles}
          value={guardianName}
          onChange={setGuardiansName}
          disable={disableFields?.guardianName}
          ValidationRequired = {true}
          {...(validation = {
            isRequired: true,
            pattern: "^[a-zA-Z ]+$",
            title: t("PT_NAME_ERROR_MESSAGE"),
          })}
        />
        </React.Fragment>:null}
      
      {(config?.additionaFields?.guardianName)?
      <React.Fragment>
        <CardLabel>{`${t(labels?.relationShipType || "COMMON_RELATIONTYPE")}`} <span className="astericColor">*</span></CardLabel>
        <Dropdown
        className="form-field"
        selected={relationShipType}
        option={GuardianOptions}
        select={setRelationShipType}
        disable={disableFields?.relationShipType}
        optionKey="i18nKey"
        t={t}
        name="relationShipType"
        style={inputStyles}
        placeholder={"Select"}
        />
      </React.Fragment>:null }
        
      <CardLabel>{`${t(labels?.emailId || "COMMON_EMAIL_ID")}`} <span className="astericColor">*</span></CardLabel>
      <TextInput
        t={t}
        type={"email"}
        isMandatory={true}
        optionKey="i18nKey"
        name="emailId"
        value={emailId}
        style={inputStyles}
        onChange={setOwnerEmail}
        disable={disableFields?.emailId}
        ValidationRequired={true}
        {...(validation = {
          isRequired: true,
          pattern: "^[a-zA-Z0-9._%+-]+@[a-zA-Z]+\\.[a-zA-Z]{3,4}$",
          title: t("PT_NAME_ERROR_MESSAGE"),
        })}
      />
        
      </div>
    </FormStep>
    </React.Fragment>
  );
};

export default ApplicantDetails;