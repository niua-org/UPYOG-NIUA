import React, { useEffect, useState } from "react";
import { FormStep, TextInput, CardLabel, RadioButtons, Dropdown, RadioOrSelect, Toast } from "@upyog/digit-ui-react-components";
import { cardBodyStyle } from "../utils";
import { useLocation, useRouteMatch } from "react-router-dom";
import Timeline from "../components/EWASTETimeline";
import { Controller, useForm } from "react-hook-form";
import { SubmitBar } from "@upyog/digit-ui-react-components";
import ProductList from "../components/EWASTEProductList";

const EWProductDetails = ({ t, config, onSelect, userType, formData, ownerIndex }) => {
  const { pathname: url } = useLocation();
  let index = window.location.href.charAt(window.location.href.length - 1);
  let validation = {};

  const [productName, setProductName] = useState(
    (formData.ewdet && formData.ewdet[index] && formData.ewdet[index]?.productName) || formData?.ewdet?.productName || ""
  );
  const [productQuantity, setProductQuantity] = useState(
    (formData.ewdet && formData.ewdet[index] && formData.ewdet[index]?.productQuantity) || formData?.ewdet?.productQuantity || "1"
  );
  // const productPrice = (formData.ewdet && formData.ewdet[index] && formData.ewdet[index]?.productPrice) || formData?.ewdet?.productPrice || "0.0";
  const [calculatedAmount, setCalculatedAmount] = useState((formData.ewdet && formData.ewdet[index] && formData.ewdet[index]?.calculatedAmount) || formData?.ewdet?.calculatedAmount || "");

  const [prlistName, setPrlistName] = useState((formData.ewdet && formData.ewdet[index] && formData.ewdet[index]?.prlistName) || formData?.ewdet?.prlistName || []);
  const [prlistQuantity, setPrlistQuantity] = useState((formData.ewdet && formData.ewdet[index] && formData.ewdet[index]?.prlistQuantity) || formData?.ewdet?.prlistQuantity || []);

  const [showToast, setShowToast] = useState(null);

  const tenantId = Digit.ULBService.getCurrentTenantId();
  const stateId = Digit.ULBService.getStateId();

  const { data: Menu } = Digit.Hooks.ew.useProductPriceMDMS(stateId, "Ewaste", "ProductName");

  let menu = [];

  Menu?.Ewaste?.ProductName &&
    Menu?.Ewaste?.ProductName.map((ewasteDetails) => {
      menu.push({ i18nKey: `EWASTE_${ewasteDetails.code}`, code: `${ewasteDetails.name}`, value: `${ewasteDetails.name}`, price: `${ewasteDetails.price}` });
    });

  const { control, setError, clearErrors } = useForm();

  function setproductQuantity(e) {
    setShowToast(null);
    setProductQuantity(e.target.value);
  }

  // const [prlistTotalprice, setPrlistTotalprice] = useState([]);

  const handleAddProduct = () => {
    if (!/^[1-9][0-9]*$/.test(productQuantity)) {
      // alert(t("EWASTE_NUMBER_ERROR_MESSAGE"));
      setShowToast({
        label: t("EWASTE_ZERO_ERROR_MESSAGE")
      });
      return;
    }

    const productExists = prlistName.some((item) => item.code === productName.code);

    if (!productExists) {
      setPrlistName([...prlistName, { code: productName.code, i18nKey: productName.i18nKey, price: productName.price }]);
      setPrlistQuantity([...prlistQuantity, { code: productQuantity }]);
    } else {
      setShowToast({
        label: t("EWASTE_DUPLICATE_PRODUCT_ERROR_MESSAGE")
      });
    }


    // setPrlistName([...prlistName, { code: productName.code, i18nKey: productName.i18nKey, price: productName.price }]);
    // setPrlistQuantity([...prlistQuantity, { code: productQuantity }]);
    // setPrlistTotalprice([...prlistTotalprice, { code: productPrice }]);
    // prlist.map((pr) => {
    //   console.log("product is" + pr.code);
    // })
  };

  const goNext = () => {
    let owner = formData.ewdet && formData.ewdet[index];
    let ownerStep;
    if (userType === "citizen") {
      ownerStep = { ...owner, prlistName, prlistQuantity, calculatedAmount };
      onSelect(config.key, { ...formData[config.key], ...ownerStep }, false, index);
    } else {
      ownerStep = { ...owner, prlistName, prlistQuantity, calculatedAmount };
      onSelect(config.key, ownerStep, false, index);
    }
  };

  const onSkip = () => onSelect();

  useEffect(() => {
    if (userType === "citizen") {
      goNext();
    }
  }, [prlistName, prlistQuantity, calculatedAmount]);

  return (
    <React.Fragment>
      {window.location.href.includes("/citizen") ? <Timeline currentStep={1} /> : null}

      <FormStep
        config={config}
        onSelect={goNext}
        onSkip={onSkip}
        t={t}
        isDisabled={!productName || !productQuantity}
      >
        <div>
          <CardLabel>{`${t("EWASTE_SEARCH_PRODUCT")}`}</CardLabel>
          <Controller
            control={control}
            name={"productName"}
            defaultValue={productName}
            rules={{ required: t("CORE_COMMON_REQUIRED_ERRMSG") }}
            render={(props) => (
              <Dropdown
              className="form-field" 
              selected={productName} 
              select={setProductName} 
              option={menu} 
              optionCardStyles={{overflowY: "auto", maxHeight: "300px"}}
              optionKey="i18nKey" 
              t={t} 
              />
            )}
          />

          <CardLabel>{`${t("EWASTE_QUANTITY")}`}</CardLabel>
          <TextInput
            t={t}
            type={"text"}
            isMandatory={false}
            optionKey="i18nKey"
            name="productQuantity"
            value={productQuantity}
            onChange={setproductQuantity}
            style={{ width: "86%" }}
            ValidationRequired={true}
            {...(validation = {
              isRequired: true,
              pattern: "^[1-9][0-9]*$",
              type: "text",
              title: t("EWASTE_NUMBER_ERROR_MESSAGE"),
            })}
          />


          <CardLabel>{`${t("EWASTE_UNIT_PRICE")}`}</CardLabel>
          <TextInput
            t={t}
            type={"text"}
            isMandatory={false}
            optionKey="i18nKey"
            name="productPrice"
            value={productName.price}
            disable={true}
            //  onChange={setproductPrice}
            style={{ width: "86%" }}
            ValidationRequired={false}
          />


        </div>
        <SubmitBar label="Add Product" style={{ marginBottom: "10px" }} onSubmit={handleAddProduct} />
      </FormStep>

      <div>
        <ProductList
          t={t}
          prlistName={prlistName}
          setPrlistName={setPrlistName}
          prlistQuantity={prlistQuantity}
          setPrlistQuantity={setPrlistQuantity}
          setCalculatedAmount={setCalculatedAmount}
          // prlistTotalprice={prlistTotalprice}
        />
      </div>

      {showToast?.label && <Toast
      label={showToast.label}
      error={true}
      isDleteBtn={true}
      onClose={() => {
        setShowToast(null)
      }}
      />}
    </React.Fragment>
  );
};

export default EWProductDetails;
