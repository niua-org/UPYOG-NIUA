import { Card, CardHeader, CardSubHeader, CardText, CheckBox, LinkButton, Row, StatusTable, SubmitBar } from "@upyog/digit-ui-react-components";
import React, { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useHistory } from "react-router-dom";

import { checkForNA } from "../../../../utils";
import Timeline from "../../../../components/ASTTimeline";

const ActionButton = ({ jumpTo }) => {
  const { t } = useTranslation();
  const history = useHistory();
  function routeTo() {
    history.push(jumpTo);
  }

  return <LinkButton label={t("CS_COMMON_CHANGE")} className="check-page-link-button" onClick={routeTo} />;
};

const CheckPage = ({ onSubmit, value = {} }) => {
  const { t } = useTranslation();
  const history = useHistory();
  const [agree, setAgree] = useState(false);
  const [categoriesWiseData, setCategoriesWiseData] = useState();
  
   //  * get @param city & state id
   const tenantId = Digit.ULBService.getCurrentTenantId();
   const stateTenantId = Digit.ULBService.getStateId();
 
   //  This call with tenantId (Get city-level data)
   const cityResponseObject = Digit.Hooks.useCustomMDMS(tenantId, "ASSET", [{ name: "AssetParentCategoryFields" }], {
     select: (data) => {
       const formattedData = data?.["ASSET"]?.["AssetParentCategoryFields"];
       return formattedData;
     },
   });
 
   // This call with stateTenantId (Get state-level data)
   const stateResponseObject = Digit.Hooks.useCustomMDMS(stateTenantId, "ASSET", [{ name: "AssetParentCategoryFields" }], {
     select: (data) => {
       const formattedData = data?.["ASSET"]?.["AssetParentCategoryFields"];
       return formattedData;
     },
   });

  const { address, assetDetails, asset } = value;


  
  const setdeclarationhandler = () => {
    setAgree(!agree);
  };
  useEffect(() => {
    let combinedData;
    // if city level master is not available then fetch  from state-level
    if (cityResponseObject?.data) {
      combinedData = cityResponseObject.data;
    } else if (stateResponseObject?.data) {
      combinedData = stateResponseObject.data;
    } else {
      combinedData = [];
    }
    setCategoriesWiseData(combinedData);
  }, [cityResponseObject, stateResponseObject]);
  
  let formJson = [];
  if (Array.isArray(categoriesWiseData)) {

    formJson = categoriesWiseData
      .filter((category) => {
        const isMatch = category.assetParentCategory === asset?.asset?.assettype?.code || category.assetParentCategory === "COMMON";
        return isMatch;
      })
      .map((category) => category.fields) // Extract the fields array
      .flat() // Flatten the fields array
      .filter((field) => field.active === true); // Filter by active status
  }

  function extractValue(key){
      var vl = asset?.assetDetails[key]
      if(typeof vl === 'object'){
          return vl.code
      }
      return vl
  }

   console.log("Asset:", asset);
  console.log("Asset Details:", assetDetails);
  console.log("Address:", address);

  return (
    <React.Fragment>
      { <Timeline currentStep={2} />}
      <Card>
        <CardHeader>{t("AST_CHECK_DETAILS")}</CardHeader>
        <div>
          <br></br>

          <CardSubHeader>{t("ASSET_GENERAL_DETAILS")}</CardSubHeader>
          <StatusTable>
            <Row
              label={t("AST_FINANCIAL_YEAR")}
              text={`${t(checkForNA(asset?.asset?.financialYear?.code))}`}
              actionButton={<ActionButton jumpTo={`/upyog-ui/employee/asset/assetservice/new-assets/asset-deatils`} />}
            />
            <Row
              label={t("AST_SOURCE_FINANCE")}
              text={`${t(checkForNA(asset?.asset?.sourceOfFinance?.code))}`}
              actionButton={<ActionButton jumpTo={`/upyog-ui/employee/asset/assetservice/new-assets/asset-deatils`} />}
            />
            <Row
              label={t("AST_ASSET_CATEGORY_LABEL")}
              text={`${t(checkForNA(asset?.asset?.assetclassification?.code))}`}
              actionButton={<ActionButton jumpTo={`/upyog-ui/employee/asset/assetservice/new-assets/asset-deatils`} />}
            />

            <Row
              label={t("AST_PARENT_CATEGORY_LABEL")}
              text={`${t(checkForNA(asset?.asset?.assettype?.code))}`}
              actionButton={<ActionButton jumpTo={`/upyog-ui/employee/asset/assetservice/new-assets/asset-deatils`} />}
            />

            <Row
              label={t("AST_SUB_CATEGORY")}
              text={`${t(checkForNA(asset?.asset?.assetsubtype?.code))}`}
              actionButton={<ActionButton jumpTo={`/upyog-ui/employee/asset/assetservice/new-assets/asset-deatils`} />}
            />
            <Row
              label={t("AST_CATEGORY_SUB_CATEGORY")}
              text={`${t(checkForNA(asset?.asset?.assetparentsubCategory?.code))}`}
              actionButton={<ActionButton jumpTo={`/upyog-ui/employee/asset/assetservice/new-assets/asset-deatils`} />}
            />

            <Row
              label={t("AST_BOOK_REF_SERIAL_NUM")}
              text={`${t(checkForNA(asset?.asset?.BookPagereference))}`}
              actionButton={<ActionButton jumpTo={`/upyog-ui/employee/asset/assetservice/new-assets/asset-deatils`} />}
            />
            <Row
              label={t("AST_NAME")}
              text={`${t(checkForNA(asset?.asset?.AssetName))}`}
              actionButton={<ActionButton jumpTo={`/upyog-ui/employee/asset/assetservice/new-assets/asset-deatils`} />}
            />
            <Row
              label={t("ASSET_DESCRIPTION")}
              text={`${t(checkForNA(asset?.asset?.Assetdescription))}`}
              actionButton={<ActionButton jumpTo={`/upyog-ui/employee/asset/assetservice/new-assets/asset-deatils`} />}
            />

            <Row
              label={t("AST_DEPARTMENT")}
              text={`${t(checkForNA(asset?.sset?.Department?.value))}`}
              actionButton={<ActionButton jumpTo={`/upyog-ui/employee/asset/assetservice/new-assets/asset-deatils`} />}
            />

<Row
            label={t("AST_TYPE")}
            text={`${t(checkForNA(asset?.asset?.assetsOfType?.value))}`}
            actionButton={<ActionButton jumpTo={`/upyog-ui/employee/asset/assetservice/new-assets/asset-deatils`} />}
          />

          <Row
            label={t("AST_USAGE")}
            text={`${t(checkForNA(asset?.asset?.assetsUsage?.code))}`}
            actionButton={<ActionButton jumpTo={`/upyog-ui/employee/asset/assetservice/new-assets/asset-deatils`} />}
          />

          <Row
            label={t("AST_STATUS_ASSIGNABLE")}
            text={`${t(checkForNA(asset?.asset?.assetAssignable?.code))}`}
            actionButton={<ActionButton jumpTo={`/upyog-ui/employee/asset/assetservice/new-assets/asset-deatils`} />}
          />

          </StatusTable>
          <br></br>

          <CardSubHeader>{t("AST_DETAILS")}</CardSubHeader>

          <StatusTable>
              <React.Fragment>
             {asset?.asset?.assettype?.code!== "LAND" &&(
              <React.Fragment>
            <Row
              label={t("AST_INVOICE_DATE")}
              text={`${t(checkForNA(asset?.assetDetails?.invoiceDate))}`}
              actionButton={<ActionButton jumpTo={`/upyog-ui/employee/asset/assetservice/new-assets/assets`} />}
            />
            <Row
              label={t("AST_INVOICE_NUMBER")}
              text={`${t(checkForNA(asset?.assetDetails?.invoiceNumber))}`}
              actionButton={<ActionButton jumpTo={`/upyog-ui/employee/asset/assetservice/new-assets/assets`} />}
            />
            </React.Fragment>
             )}   
            
            <Row
              label={t("AST_PURCHASE_DATE")}
              text={`${t(checkForNA(asset?.assetDetails?.purchaseDate))}`}
              actionButton={<ActionButton jumpTo={`/upyog-ui/employee/asset/assetservice/new-assets/assets`} />}
            />
            <Row
              label={t("AST_PURCHASE_ORDER")}
              text={`${t(checkForNA(asset?.assetDetails?.purchaseOrderNumber))}`}
              actionButton={<ActionButton jumpTo={`/upyog-ui/employee/asset/assetservice/new-assets/assets`} />}
            />
            <Row
              label={t("AST_LIFE")}
              text={`${t(checkForNA(asset?.assetDetails?.lifeOfAsset))}`}
              actionButton={<ActionButton jumpTo={`/upyog-ui/employee/asset/assetservice/new-assets/assets`} />}
            />
            <Row
              label={t("AST_LOCATION_DETAILS")}
              text={`${t(checkForNA(asset?.ssetDetails?.location))}`}
              actionButton={<ActionButton jumpTo={`/upyog-ui/employee/asset/assetservice/new-assets/assets`} />}
            />
            <Row
              label={t("AST_MARKET_RATE")}
              text={`${t(checkForNA(asset?.assetDetails?.marketRate))}`}
              actionButton={<ActionButton jumpTo={`/upyog-ui/employee/asset/assetservice/new-assets/assets`} />}
            />
            <Row
              label={t("AST_PURCHASE_COST")}
              text={`${t(checkForNA(asset?.assetDetails?.purchaseCost))}`}
              actionButton={<ActionButton jumpTo={`/upyog-ui/employee/asset/assetservice/new-assets/assets`} />}
            />
            <Row
              label={t("AST_ACQUISITION_COST")}
              text={`${t(checkForNA(asset?.assetDetails?.acquisitionCost))}`}
              actionButton={<ActionButton jumpTo={`/upyog-ui/employee/asset/assetservice/new-assets/assets`} />}
            />
            <Row
              label={t("AST_BOOK_VALUE")}
              text={`${t(checkForNA(asset?.assetDetails?.bookValue))}`}
              actionButton={<ActionButton jumpTo={`/upyog-ui/employee/asset/assetservice/new-assets/assets`} />}
            />
              {formJson.filter(row => row.isNeeded !== false)
              .map((row, index) => (
                <Row key= {index}
                  label={t(row.code)}
                  text={`${extractValue(row.name)}`}
                  actionButton={<ActionButton jumpTo={`/upyog-ui/employee/asset/assetservice/new-assets/assets`} />}
                />
                
              ))}
              </React.Fragment>
         
          </StatusTable>

          <br></br>
          <CardSubHeader>{t("AST_ADDRESS_DETAILS")}</CardSubHeader>

          <StatusTable>
             <Row
              label={t("AST_PINCODE")}
              text={`${t(checkForNA(asset?.address?.pincode))}`}
              actionButton={<ActionButton jumpTo={`/upyog-ui/employee/asset/assetservice/new-assets/address`} />}
            />
            <Row
              label={t("MYCITY_CODE_LABEL")}
              text={`${t(checkForNA(asset?.address?.city?.name))}`}
              actionButton={<ActionButton jumpTo={`/upyog-ui/employee/asset/assetservice/new-assets/address`} />}
            />
            <Row
              label={t("AST_LOCALITY")}
              text={`${t(checkForNA(asset?.address?.locality?.i18nKey))}`}
              actionButton={<ActionButton jumpTo={`/upyog-ui/employee/asset/assetservice/new-assets/address`} />}
            />
          </StatusTable>
          <br></br>

          <CheckBox
            label={t("AST_FINAL_DECLARATION_MESSAGE")}
            onChange={setdeclarationhandler}
            styles={{ height: "auto" }}
            //disabled={!agree}
          />
        </div>
        <br></br>
        <SubmitBar label={t("COMMON_BUTTON_SUBMIT")} onSubmit={onSubmit} disabled={!agree} />
      </Card>
    </React.Fragment>
  );
};

export default CheckPage;
