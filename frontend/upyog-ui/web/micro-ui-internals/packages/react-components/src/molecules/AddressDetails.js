import React, { useState, useEffect, useMemo } from "react";
import { useForm, Controller } from "react-hook-form";
import { CardLabel, TextInput,TextArea,Dropdown,FormStep, UploadFile } from "../index";  //imported all from our common library
import { useLocation } from "react-router-dom";


/**
 * Common Address Details component so that developer can use it just by importing it accross the UPYOG.
 * all cities data is fetching from common Tenants hook and locality data is fetched in the same way.
 * 
 * Steps to Use it in your Module/Application  :-
 *    1. Import it in your Module.js file of your Application/Module from  @upyog/upyog-ui-react-components-lts library.
 *    2. Insert it inside your componet registry Function in the same Module.js file.
 *    3. Then simply In your Config.js file which is present in your comfig folder inside src, add the component name "AddressDetails" in components and key should be "address"
 * 
 *     example :- 
 *              {
                "route": "add url route here",
                "component": "AddressDetails",
                "withoutLabel": true,
                "key": "address",
                "type": "component",
                "nextStep": "add your next to be url route here",
                "isMandatory": true,
                "texts": {
                    "submitBarLabel": "COMMON_SAVE_NEXT",
                    "header": "ADDRESS_DETAILS",
                }
            },

      4. Test it, it will work.
      

      TODO: Need to check how to use Timeline functioality 
 */

const AddressDetails = ({t, config, onSelect, formData, isEdit}) => {
  const { data: allCities, isLoading } = Digit.Hooks.useTenants();
  let validation = {};
  const convertToObject = (String) => String ? { i18nKey: String, code: String, value: String } : null;
  const user = Digit.UserService.getUser().info;
  const [pincode, setPincode] = useState(formData?.pincode||formData?.address?.pincode || formData?.infodetails?.existingDataSet?.address?.pincode ||  "");
  const [city, setCity] = useState( convertToObject(formData?.city) ||formData?.address?.city ||formData?.infodetails?.existingDataSet?.address?.cityValue || "");
  const [locality, setLocality] = useState( convertToObject(formData?.locality) || formData?.address?.locality || formData?.infodetails?.existingDataSet?.address?.locality || "");
  const [houseNo, setHouseNo] = useState(formData?.houseNo ||formData?.address?.houseNo || formData?.infodetails?.existingDataSet?.address?.houseNo ||"");
  const [streetName, setstreetName] = useState(formData?.streetName ||formData?.address?.streetName ||formData?.infodetails?.existingDataSet?.address?.streetName ||"");
  const [landmark, setLandmark] = useState(formData?.landmark ||formData?.address?.landmark ||formData?.infodetails?.existingDataSet?.address?.landmark || "");
  const [addressLine1, setAddressLine1] = useState(formData?.addressLine1 ||formData?.address?.addressLine1 || formData?.infodetails?.existingDataSet?.address?.addressline1 || "");
  const [addressLine2, setAddressLine2] = useState(formData?.addressLine2 ||formData?.address?.addressLine2 || formData?.infodetails?.existingDataSet?.address?.addressline2 || "");
  const [addressType, setAddressType] = useState( convertToObject(formData?.addressType) ||formData?.address?.addressType || formData?.infodetails?.existingDataSet?.address?.addressType || "");
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

  const [addressProof, setAddressProof] = useState(formData?.addressProof || null);
  const [fileError, setFileError] = useState("");

  const { data: fetchedLocalities, isLoading: isLoadingLocalities } = Digit.Hooks.useBoundaryLocalities(
    city?.code,
    "revenue",
    {
      enabled: !!city,
    },
    t
  );

  // Handle the uploading of Address Details
  const handleProofUpload = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    const allowed = ["image/jpeg", "image/png", "image/jpg", "application/pdf"];
    if (!allowed.includes(file.type)) {
      setFileError(t("PROFILE_ADDRESS_PROOF_INVALID_TYPE"));
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      setFileError(t("PROFILE_ADDRESS_PROOF_SIZE_EXCEEDED"));
      return;
    }
    setFileError("");
    setAddressProof(file);
    onSelect((prev) => ({ ...prev, addressProof: file }));
  };

  let structuredLocality = [];
  fetchedLocalities &&
    fetchedLocalities.map((local, index) => {
      structuredLocality.push({ i18nKey: local.i18nkey, code: local.code, label: local.label, area: local.area, boundaryNum: local.boundaryNum });
    });
  
  const goNext = () => {
    let ownerAddress = formData.address;
    let addressStep = { ...ownerAddress, pincode, city, locality, houseNo, landmark, addressLine1, addressLine2, streetName, addressType };
    onSelect(config.key, { ...formData[config.key], ...addressStep }, false);
  };
  /* If `config` is undefined and all required address fields are filled, it creates an `addressStep` object
    containing the address details and calls the `onSelect` function with it.
   **/
  useEffect(() => {
    onSelect((prev) => ({...prev, addressType, houseNo, streetName, addressLine1, addressLine2, landmark, city, locality, pincode}));
  }, [addressType, houseNo, streetName, addressLine1, addressLine2, landmark, city, locality, pincode]);

  return (
    <React.Fragment>
        <FormStep
        config={config}
        onSelect={goNext}
        t={t}
        isDisabled={!houseNo || !city || !locality || !pincode || pincode.length !== 6 || !addressLine1||!streetName||!addressLine2}
        >
    <div>
    <CardLabel>{`${t("COMMON_ADDRESS_TYPE")}`} <span className="check-page-link-button">*</span></CardLabel>
    <Dropdown
      className="form-field"
      selected={addressType}
      select={setAddressType}
      disable={isEdit}
      option={availableAddressTypeOptions}
      optionCardStyles={{ overflowY: "auto", maxHeight: "300px" }}
      optionKey="i18nKey"
      t={t}
      style={inputStyles} 
      placeholder={"Select Address Type"}
    />
    <CardLabel>{`${t("HOUSE_NO")}`} <span className="check-page-link-button">*</span></CardLabel>
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
        ValidationRequired={true}
        validation={{
          isRequired: true,
          pattern: "^[a-zA-Z0-9 ,\\-]+$",
          title: t("HOUSE_NO_ERROR_MESSAGE"),
        }}
      />

      <CardLabel>{`${t("STREET_NAME")}`} <span className="check-page-link-button">*</span></CardLabel>
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
        ValidationRequired={true}
        validation={{
          pattern: "^[a-zA-Z0-9 ,\\-]+$",
          title: t("STREET_NAME_ERROR_MESSAGE"),
        }}
      />

      <CardLabel>{`${t("ADDRESS_LINE1")}`} <span className="check-page-link-button">*</span></CardLabel>
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
        ValidationRequired={false}
        {...(validation = {
          isRequired: false,
          pattern: "^[a-zA-Z,-/ ]*$",
          title: t("ADDRESS_ERROR_MESSAGE"),
        })}
      />

      <CardLabel>{`${t("ADDRESS_LINE2")}`} <span className="check-page-link-button">*</span></CardLabel>
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
        ValidationRequired={false}
        {...(validation = {
          isRequired: false,
          pattern: "^[a-zA-Z,-/ ]*$",
          title: t("ADDRESS_ERROR_MESSAGE"),
        })}
      />

      <CardLabel>{`${t("LANDMARK")}`}</CardLabel>
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
        ValidationRequired={true}
        validation={{
          isRequired: false,
          pattern: "^[a-zA-Z0-9 ]+$",
          title: t("LANDMARK_ERROR_MESSAGE"),
        }}
      />

      <CardLabel>{`${t("CITY")}`} <span className="check-page-link-button">*</span></CardLabel>
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
            optionCardStyles={{ overflowY: "auto", maxHeight: "300px" }}
            optionKey="i18nKey"
            t={t}
            style={inputStyles}
            placeholder={"Select"}
          />
        )}
      />

      <CardLabel>{`${t("LOCALITY")}`} <span className="check-page-link-button">*</span></CardLabel>
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
            optionCardStyles={{ overflowY: "auto", maxHeight: "300px" }}
            optionKey="i18nKey"
            t={t}
            style={inputStyles}
            placeholder={"Select"}
          />
        )}
      />

      <CardLabel>{`${t("PINCODE")}`} <span className="check-page-link-button">*</span></CardLabel>
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
      <CardLabel>{t("PROFILE_ADDRESS_PROOF")}</CardLabel>
      <UploadFile
        accept=".jpg,.jpeg,.png,.pdf"
        onUpload={handleProofUpload}
        onDelete={() => { setAddressProof(null); onSelect((prev) => ({ ...prev, addressProof: null })); }}
        file={addressProof}
        message={t("PROFILE_NO_FILE_SELECTED")}
        iserror={fileError}
      />
    </div>
    </FormStep>
    </React.Fragment>
  );
};

export default AddressDetails;