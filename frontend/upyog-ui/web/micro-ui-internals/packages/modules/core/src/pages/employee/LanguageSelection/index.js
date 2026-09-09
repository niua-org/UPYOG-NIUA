import { Card, CustomButton, SubmitBar } from "@nudmcdgnpm/digit-ui-react-components";
import React, { useState } from "react";
import { useTranslation } from "react-i18next";

import Background from "../../../components/Background";

const LanguageSelection = () => {
  const { data: storeData, isLoading } = Digit.Hooks.useStore.getInitData();
  const { t } = useTranslation();
  const navigate = Digit.Hooks.useCustomNavigate();
  const { languages, stateInfo } = storeData || {};
  const selectedLanguage = Digit.StoreData.getCurrentLanguage();
  const [selected, setselected] = useState(selectedLanguage);
  const handleChangeLanguage = (language) => {
    setselected(language.value);
    Digit.LocalizationService.changeLanguage(language.value, stateInfo.code);
  };
  let sourceUrl = "https://s3.ap-south-1.amazonaws.com/egov-qa-assets";
  const pdfUrl = "https://pg-egov-assets.s3.ap-south-1.amazonaws.com/Upyog+Code+and+Copyright+License_v1.pdf";

  const handleSubmit = (event) => {
    navigate("/upyog-ui/employee/user/login");
  };

  if (isLoading) return null;

  return (
    <Background>
      <Card className="bannerCard removeBottomMargin">
       
        <div className="language-selector mb-lg" style={{ justifyContent: "space-around", padding: "0 5%" }}>
          {languages.map((language, index) => (
            <div className="language-button-container" key={index}>
              <CustomButton
                selected={language.value === selected}
                text={language.label}
                onClick={() => handleChangeLanguage(language)}
              ></CustomButton>
            </div>
          ))}
        </div>
        <SubmitBar style={{ width: "100%" }} label={t(`CORE_COMMON_CONTINUE`)} onSubmit={handleSubmit} />
      </Card>

      <div className="bg-white text-center" style={{ width: '100%', position: 'fixed', bottom: 0 }}>
        <div className="flex justify-center" style={{ color:"black" }}>
          {/* <span className="cursor-pointer font-regular" style={{ fontSize: window.Digit.Utils.browser.isMobile()?"12px":"12px" }} onClick={() => { window.open('https://www.digit.org/', '_blank').focus();}} >Powered by DIGIT</span>
          <span style={{ margin: "0 10px" ,fontSize: window.Digit.Utils.browser.isMobile()?"12px":"12px"}}>|</span> */}
          <a className="cursor-pointer font-regular" style={{ fontSize: window.Digit.Utils.browser.isMobile()?"12px":"12px" }} href="#" target='_blank'>UPYOG License</a>

          <span  className="upyog-copyright-footer text-xs" style={{ margin: "0 10px" }} >|</span>
          <span  className="upyog-copyright-footer cursor-pointer font-regular" style={{ fontSize: window.Digit.Utils.browser.isMobile()?"12px":"12px" }} onClick={() => { window.open('https://niua.in/', '_blank').focus();}} >Copyright © 2022 National Institute of Urban Affairs</span>
          
          {/* <a className="cursor-pointer text-md font-regular" href="#" target='_blank'>UPYOG License</a> */}

        </div>
        <div className="upyog-copyright-footer-web">
          <span className=" cursor-pointer font-regular" style={{ fontSize:  window.Digit.Utils.browser.isMobile()?"14px":"16px" }} onClick={() => { window.open('https://niua.in/', '_blank').focus();}} >Copyright © 2022 National Institute of Urban Affairs</span>
          </div>
      </div>
    </Background>
  );
};

export default LanguageSelection;
