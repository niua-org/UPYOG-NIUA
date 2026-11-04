import React, { useState, useEffect, useMemo } from "react";
import { useForm, Controller } from "react-hook-form";
import { CardLabel, TextInput,TextArea,Dropdown,FormStep } from "../index";  //imported all from our common library
import { useLocation } from "react-router-dom";


/**
 * Common Address Details component so that developer can use it just by importing it accross the UPYOG.
 * all cities data is fetching from common Tenants hook and locality data is fetched in the same way.
 * 
 * When using this component, you can customize fields using the following props:
 * 
 * 1. `addressData` (Type: Object):
 *    - Payload object to pre-fill address fields. Standardized structure:
 *      {
 *        houseNo: "...",          // House / Door No
 *        streetName: "...",       // Street Name
 *        addressLine1: "...",     // Address Line 1
 *        addressLine2: "...",     // Address Line 2
 *        landmark: "...",         // Landmark
 *        city: "...",             // City code or object { code, name, i18nKey }
 *        locality: "...",         // Locality code or object { code, name, label, i18nKey }
 *        pincode: "...",          // 6-digit postal pincode
 *        addressType: "..."       // Address type code or object { code, name, i18nKey }
 *      }
 * 
 * 2. `labels` (Type: Object):
 *    - Key-value map of localization keys to dynamically change header or any CardLabel.
 *    - Available keys: header, addressType, houseNo, streetName, addressLine1, addressLine2, landmark, city, locality, pincode
 *    - Example: labels={{ header: "ES_ADDRESS_DETAILS", houseNo: "GC_HOUSE_NO" }}
 * 
 * 3. `disableFields` (Type: Object):
 *    - Key-value boolean map to disable specific fields.
 *    - Available keys: addressType, houseNo, streetName, addressLine1, addressLine2, landmark, city, locality, pincode
 *    - Example: disableFields={{ city: true, locality: true, pincode: true }}
 * 
 * Usage Example:
 * <AddressDetails
 *   labels={{
 *     header: "ES_ADDRESS_DETAILS",
 *     houseNo: "GC_HOUSE_NO"
 *   }}
 *   disableFields={{
 *     pincode: true,
 *     city: true
 *   }}
 *   addressData={{
 *     houseNo: "123",
 *     streetName: "Main Street",
 *     city: "pb.amritsar",
 *     pincode: "143001"
 *   }}
 * />
 * 
 * TODO:- 1. Need to remove extra OR (||) fallback conditions (such as formData?.infodetails?.existingDataSet) across state initializations.
 *        2. Calling modules across Edit, Renew & Draft flows must pass prefilled data using the standardized `addressData` prop payload documented above.
 */

const AddressDetails = ({ t, config, onSelect, formData, isEdit, addressData, labels = {}, disableFields = {} }) => {
  const { data: allCities, isLoading } = Digit.Hooks.useTenants();
  let validation = {};
  const convertToObject = (String) => String ? { i18nKey: String, code: String, value: String } : null;
  const user = Digit.UserService.getUser().info;
  const [pincode, setPincode] = useState(addressData?.pincode ? String(addressData.pincode) : (formData?.pincode || formData?.address?.pincode || formData?.infodetails?.existingDataSet?.address?.pincode || ""));
  const [city, setCity] = useState(addressData?.city ? (typeof addressData.city === "object" ? addressData.city : convertToObject(addressData.city)) : (convertToObject(formData?.city) || formData?.address?.city || formData?.infodetails?.existingDataSet?.address?.cityValue || ""));
  const [locality, setLocality] = useState(addressData?.locality ? (typeof addressData.locality === "object" ? addressData.locality : convertToObject(addressData.locality)) : (convertToObject(formData?.locality) || formData?.address?.locality || formData?.infodetails?.existingDataSet?.address?.locality || ""));
  const [houseNo, setHouseNo] = useState(addressData?.houseNo || formData?.houseNo || formData?.address?.houseNo || formData?.infodetails?.existingDataSet?.address?.houseNo || "");
  const [streetName, setstreetName] = useState(addressData?.streetName || formData?.streetName || formData?.address?.streetName || formData?.infodetails?.existingDataSet?.address?.streetName || "");
  const [landmark, setLandmark] = useState(addressData?.landmark || formData?.landmark || formData?.address?.landmark || formData?.infodetails?.existingDataSet?.address?.landmark || "");
  const [addressLine1, setAddressLine1] = useState(addressData?.addressLine1 || formData?.addressLine1 || formData?.address?.addressLine1 || formData?.infodetails?.existingDataSet?.address?.addressline1 || "");
  const [addressLine2, setAddressLine2] = useState(addressData?.addressLine2 || formData?.addressLine2 || formData?.address?.addressLine2 || formData?.infodetails?.existingDataSet?.address?.addressline2 || "");
  const [addressType, setAddressType] = useState(addressData?.addressType ? (typeof addressData.addressType === "object" ? addressData.addressType : convertToObject(addressData.addressType)) : (convertToObject(formData?.addressType) || formData?.address?.addressType || formData?.infodetails?.existingDataSet?.address?.addressType || ""));

  useEffect(() => {
    if (addressData) {
      if (addressData.pincode) setPincode(String(addressData.pincode));
      if (addressData.city) setCity(typeof addressData.city === "object" ? addressData.city : convertToObject(addressData.city));
      if (addressData.locality) setLocality(typeof addressData.locality === "object" ? addressData.locality : convertToObject(addressData.locality));
      if (addressData.houseNo) setHouseNo(addressData.houseNo);
      if (addressData.streetName) setstreetName(addressData.streetName);
      if (addressData.landmark) setLandmark(addressData.landmark);
      if (addressData.addressLine1) setAddressLine1(addressData.addressLine1);
      if (addressData.addressLine2) setAddressLine2(addressData.addressLine2);
      if (addressData.addressType) setAddressType(typeof addressData.addressType === "object" ? addressData.addressType : convertToObject(addressData.addressType));
    }
  }, [addressData]);
  const { control } = useForm();
  const location = useLocation();
  const usedAddressTypes = location.state?.usedAddressTypes || [];

  const inputStyles = {width:user.type === "EMPLOYEE" ? "50%" : "100%"};
  
  const availableAddressTypeOptions = useMemo(() => {
    const allOptions = [
      { name: "Correspondence", code: "CORRESPONDENCE", i18nKey: "COMMON_ADDRESS_TYPE_CORRESPONDENCE" },
      { name: "Permanent", code: "PERMANENT", i18nKey: "COMMON_ADDRESS_TYPE_PERMANENT" },
      { name: "Other", code: "OTHER", i18nKey: "COMMON_ADDRESS_TYPE_OTHER" },
    ];
    if (usedAddressTypes.length === 3) {
      // If all are available → show only "Other"
      return allOptions.filter(opt => opt.code === "OTHER");
    }
    // Otherwise, show whatever is not used
    return allOptions.filter(opt => !usedAddressTypes.includes(opt.code));
  }, [usedAddressTypes]);

  const { data: fetchedLocalities, isLoading: isLoadingLocalities } = Digit.Hooks.useBoundaryLocalities(
    city?.code,
    "revenue",
    {
      enabled: !!city,
    },
    t
  );

  let structuredLocality = [];
  fetchedLocalities &&
    fetchedLocalities.map((local, index) => {
      structuredLocality.push({ i18nKey: local.i18nkey, code: local.code, label: local.label, area: local.area, boundaryNum: local.boundaryNum });
    });
  
  const goNext = () => {
    let ownerAddress = formData.address;
    let addressStep = { ...ownerAddress, pincode, city, locality, houseNo, landmark, addressLine1, addressLine2, streetName, addressType };
    onSelect(config.key, { ...formData[config.key], ...addressStep }, false);
    // Checks if the `config` is undefined, and if so, calls the `onSelect` function with the `addressStep` object.
   // This ensures that the address step is selected when no specific configuration is provided.
    if(config===undefined){
      onSelect(addressStep);
    }
  };
  /* If `config` is undefined and all required address fields are filled, it creates an `addressStep` object
    containing the address details and calls the `onSelect` function with it.
   **/
  useEffect(() => {
    if (config === undefined && houseNo && city && locality && pincode && pincode.length === 6 && addressLine1 && streetName && addressLine2) {
      let addressStep = { pincode, city, locality, houseNo, landmark, addressLine1, addressLine2, streetName, addressType };
      onSelect(addressStep);
    }
  }, [pincode, city, locality, houseNo, landmark, addressLine1, addressLine2, streetName, addressType]);
  return (
    <React.Fragment>
        <FormStep
        config={config}
        onSelect={goNext}
        t={t}
        isDisabled={!houseNo || !city || !locality || !pincode || pincode.length !== 6 || !addressLine1||!streetName||!addressLine2}
        >
    <div>
    <CardLabel>{`${t(labels?.addressType || "COMMON_ADDRESS_TYPE")}`} <span className="check-page-link-button">*</span></CardLabel>
    <Dropdown
      className="form-field"
      selected={addressType}
      select={setAddressType}
      disable={isEdit || disableFields?.addressType}
      option={availableAddressTypeOptions}
      optionCardStyles={{ overflowY: "auto", maxHeight: "300px" }}
      optionKey="i18nKey"
      t={t}
      style={inputStyles} 
      placeholder={"Select Address Type"}
    />
    <CardLabel>{`${t(labels?.houseNo || "HOUSE_NO")}`} <span className="check-page-link-button">*</span></CardLabel>
      <TextInput
        t={t}
        type={"text"}
        isMandatory={false}
        optionKey="i18nKey"
        name="houseNo"
        value={houseNo}
        style={inputStyles}
        placeholder={"Enter House No"}
        onChange={(e) => {
            setHouseNo(e.target.value);
        }}
        disable={disableFields?.houseNo}
        ValidationRequired={true}
        validation={{
          isRequired: true,
          pattern: "^[a-zA-Z0-9 ,\\-]+$",
          title: t("HOUSE_NO_ERROR_MESSAGE"),
        }}
      />

      <CardLabel>{`${t(labels?.streetName || "STREET_NAME")}`} <span className="check-page-link-button">*</span></CardLabel>
      <TextInput
        t={t}
        type={"text"}
        isMandatory={false}
        optionKey="i18nKey"
        name="streetName"
        value={streetName}
        style={inputStyles}
        placeholder={"Enter Street Name"}
        onChange={(e) => {
          setstreetName(e.target.value);
        }}
        disable={disableFields?.streetName}
        ValidationRequired={true}
        validation={{
          pattern: "^[a-zA-Z0-9 ,\\-]+$",
          title: t("STREET_NAME_ERROR_MESSAGE"),
        }}
      />

      <CardLabel>{`${t(labels?.addressLine1 || "ADDRESS_LINE1")}`} <span className="check-page-link-button">*</span></CardLabel>
      <TextInput
        t={t}
        type={"text"}
        isMandatory={false}
        optionKey="i18nKey"
        name="addressLine1"
        value={addressLine1}
        style={inputStyles}
        placeholder={"Enter Address"}
        onChange={(e) => {
          setAddressLine1(e.target.value);
        }}
        disable={disableFields?.addressLine1}
        ValidationRequired={false}
        {...(validation = {
          isRequired: false,
          pattern: "^[a-zA-Z,-/ ]*$",
          title: t("ADDRESS_ERROR_MESSAGE"),
        })}
      />

      <CardLabel>{`${t(labels?.addressLine2 || "ADDRESS_LINE2")}`} <span className="check-page-link-button">*</span></CardLabel>
      <TextInput
        t={t}
        type={"text"}
        isMandatory={false}
        optionKey="i18nKey"
        name="addressLine2"
        value={addressLine2}
        style={inputStyles}
        placeholder={"Enter Address"}
        onChange={(e) => {
         setAddressLine2(e.target.value);
        }}
        disable={disableFields?.addressLine2}
        ValidationRequired={false}
        {...(validation = {
          isRequired: false,
          pattern: "^[a-zA-Z,-/ ]*$",
          title: t("ADDRESS_ERROR_MESSAGE"),
        })}
      />

      <CardLabel>{`${t(labels?.landmark || "LANDMARK")}`}</CardLabel>
      <TextInput
        t={t}
        type={"textarea"}
        isMandatory={false}
        optionKey="i18nKey"
        name="landmark"
        value={landmark}
        style={inputStyles}
        placeholder={"Enter Landmark"}
        onChange={(e) => {
          setLandmark(e.target.value);
        }}
        disable={disableFields?.landmark}
        ValidationRequired={true}
        validation={{
          isRequired: false,
          pattern: "^[a-zA-Z0-9 ]+$",
          title: t("LANDMARK_ERROR_MESSAGE"),
        }}
      />

      <CardLabel>{`${t(labels?.city || "CITY")}`} <span className="check-page-link-button">*</span></CardLabel>
      <Controller
        control={control}
        name={"city"}
        defaultValue={city}
        rules={{ required: t("CORE_COMMON_REQUIRED_ERRMSG") }}
        render={({field}) => (
          <Dropdown
            className="form-field"
            selected={city}
            select={setCity}
            option={allCities}
            disable={disableFields?.city}
            optionCardStyles={{ overflowY: "auto", maxHeight: "300px" }}
            optionKey="i18nKey"
            t={t}
            style={inputStyles}
            placeholder={"Select"}
          />
        )}
      />

      <CardLabel>{`${t(labels?.locality || "LOCALITY")}`} <span className="check-page-link-button">*</span></CardLabel>
      <Controller
        control={control}
        name={"locality"}
        defaultValue={locality}
        rules={{ required: t("CORE_COMMON_REQUIRED_ERRMSG") }}
        render={({field}) => (
          <Dropdown
            className="form-field"
            selected={locality}
            select={setLocality}
            option={structuredLocality}
            disable={disableFields?.locality}
            optionCardStyles={{ overflowY: "auto", maxHeight: "300px" }}
            optionKey="i18nKey"
            t={t}
            style={inputStyles}
            placeholder={"Select"}
          />
        )}
      />

      <CardLabel>{`${t(labels?.pincode || "PINCODE")}`} <span className="check-page-link-button">*</span></CardLabel>
      <TextInput
        t={t}
        type="text"
        isMandatory={false}
        optionKey="i18nKey"
        name="pincode"
        value={pincode}
        onChange={(e) => {
          const val = e.target.value.replace(/\D/g, "").slice(0, 6);
          setPincode(val);
        }}
        disable={disableFields?.pincode}
        style={inputStyles}
        placeholder="Enter Pincode"
        ValidationRequired={true}
        validation={{
            required: true,
            pattern: "^[0-9]{6}$",
            title: t("SV_ADDRESS_PINCODE_INVALID"),
          }}
          minlength={6}
          maxLength={6}
      />
    </div>
    </FormStep>
    </React.Fragment>
  );
};

export default AddressDetails;