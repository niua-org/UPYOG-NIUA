import React, { useEffect, useState } from "react";
import { CitizenInfoLabel, Loader, Dropdown, FormStep, CardLabel, RadioOrSelect } from "@upyog/digit-ui-react-components";
import Timeline from "../components/TLTimelineInFSM";
import { useLocation } from "react-router-dom";

const SelectPropertyType = ({ config, onSelect, t, userType, formData }) => {
  const { pathname: url } = useLocation();
  const tenantId = Digit.ULBService.getCurrentTenantId();
  const stateId = Digit.ULBService.getStateId();
  const select = (items) => items.map((item) => ({ ...item, i18nKey: t(item.i18nKey) }));

  const propertyTypesData = Digit.Hooks.fsm.useMDMS(stateId, "FSM", "PropertyType", { select });

  //const usageType=formData?.cpt!=="undefined"? (formData?.cpt?.details?.usageCategory==="RESIDENTIAL" ? formData?.cpt?.details?.usageCategory: formData?.cpt?.details?.usageCategory.split('.')[1]):""
  //const property = JSON.parse(sessionStorage?.getItem("Digit_FSM_PT")|| "{}")
  let property = sessionStorage?.getItem("Digit_FSM_PT")
if (property !== "undefined")
{
  property = JSON.parse(sessionStorage?.getItem("Digit_FSM_PT"))
}
const usageType = property?.propertyDetails?.usageCategory
? property?.propertyDetails?.usageCategory.includes("INSTITUTIONAL") || property?.propertyDetails?.usageCategory.includes("COMMERCIAL")
  ? property?.propertyDetails?.usageCategory.split('.')[1] 
  : property?.propertyDetails?.usageCategory 
: property?.usageCategory
? property?.usageCategory.includes("INSTITUTIONAL") || property?.usageCategory.includes("COMMERCIAL")
  ? property?.usageCategory.split('.')[1] 
  : property?.usageCategory 
: property?.propertyDetails?.usageCategory || property?.usageCategory;
  
  const [propertyType, setPropertyType] = useState(formData?.propertyType || "" );
  formData.propertyType = propertyType
  
useEffect(()=>{
 if(userType === "employee" && property && propertyTypesData.data)
    {
      
      let propertyType = []
      
      propertyType = propertyTypesData?.data.filter((city) => {
          return city.code == formData?.propertyType
        })

        if(propertyType.length >0)
        {
          onSelect(config.key, propertyType[0].code)
          setPropertyType(propertyType[0])
        }
     
    }
    if(property){
      
      if(property?.propertyDetails?.usageCategory == "COMMERCIAL" || property?.propertyDetails?.usageCategory == 
      "RESIDENTIAL")
      {
        setPropertyType(usageType)
      }
      // else if(property?.propertyDetails?.usageCategory == "INSTITUTIONAL" || property?.propertyDetails?.usageCategory == "COMMERCIAL"){
      //   let type = property?.propertyDetails?.usageCategory.split('.')[1];
      //   setPropertyType(type);
      // }
      // else{
      //   setPropertyType("")
      // }
     
    }
},[propertyTypesData.isLoading])
  useEffect(() => {
    
    if (!propertyTypesData.isLoading && propertyTypesData.data && usageType) {
      const preFilledPropertyType = propertyTypesData.data.filter(
        (propertyType) => propertyType.code === (usageType||formData?.propertyType?.code || formData?.propertyType)
      )[0];

      if(preFilledPropertyType !== undefined)
      {
        setPropertyType(preFilledPropertyType);
      }
      else if(usageType==="COMMERCIAL" || usageType==="INSTITUTIONAL"){
        setPropertyType(usageType)
      }
     
    }
  }, [property, formData?.propertyType, propertyTypesData.data]);

  const goNext = () => {
    sessionStorage.removeItem("Digit.total_amount");
    onSelect(config.key, propertyType);
  };
  function selectedValue(value) {

    setPropertyType(value);
  }
  function selectedType(value) {
    setPropertyType(value)
    onSelect(config.key, value.code);
  }

  const getInfoContent = () => {
    let content = t("CS_DEFAULT_INFO_TEXT");
    if (formData && formData.selectPaymentPreference && formData.selectPaymentPreference.code === "PRE_PAY") {
      content = t("CS_CHECK_INFO_PAY_NOW");
    } else {
      content = t("CS_CHECK_INFO_PAY_LATER");
    }
    return content;
  };

  if (propertyTypesData.isLoading) {
    return <Loader />;
  }
  if (userType === "employee") {
    return (
      <Dropdown
        option={propertyTypesData.data?.sort((a, b) => a.name.localeCompare(b.name))}
        optionKey="i18nKey"
        id="propertyType"
        selected={propertyType}
        select={selectedType}
        t={t}
        disable={url.includes("/modify-application/") || (url.includes("/new-application") && propertyType !== undefined) ? false : true}
      />
    );
  } else {
    return (
      <React.Fragment>
        <Timeline currentStep={1} flow="APPLY" />
        <FormStep config={config} onSelect={goNext} isDisabled={!propertyType} t={t}>
          <CardLabel>{`${t("CS_FILE_APPLICATION_PROPERTY_LABEL")} *`}</CardLabel>
          <RadioOrSelect
            options={propertyTypesData.data?.sort((a, b) => a.name.localeCompare(b.name))}
            selectedOption={propertyType}
            optionKey="i18nKey"
            onSelect={selectedValue}
            t={t}
          />
        </FormStep>
        {propertyType && (
          <CitizenInfoLabel
            info={t("CS_FILE_APPLICATION_INFO_LABEL")}
            text={t("CS_FILE_APPLICATION_INFO_TEXT", { content: t("CS_DEFAULT_INFO_TEXT"), ...propertyType })}
          />
        )}
      </React.Fragment>
    );
  }
};

export default SelectPropertyType;
