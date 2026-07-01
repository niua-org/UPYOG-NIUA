import { CardLabel, CardLabelError, FormStep, LabelFieldPair, TextInput, LocationIcon } from "@nudmcdgnpm/digit-ui-react-components";
import React, { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";
import Timeline from "../components/TLTimeline";
import { getDigiPin, DigipinDisplay } from "../../../../libraries/src/utils/digipin";

const PTSelectPincode = ({ t, config, onSelect, formData = {}, userType, register, errors, setError, formState, clearErrors }) => {
  const tenants = Digit.Hooks.pt.useTenants();
  const {
    pathname
  } = useLocation();
  const presentInModifyApplication = pathname.includes("modify");
  const [pincode, setPincode] = useState(() => {
    if (presentInModifyApplication && userType === "employee") return formData?.originalData?.address?.pincode || "";
    return formData?.address?.pincode || "";
  });
  // location and geolocation states
  const [locationText, setLocationText] = useState("");
  const [geoLocation, setGeoLocation] = useState(formData?.address?.geoLocation || {});
  const [digipin, setDigipin] = useState(formData?.address?.digipin || "");

  let isEditProperty = formData?.isEditProperty || false;
  if (formData?.isUpdateProperty) isEditProperty = true;
  const inputs = [{
    label: "PT_PROPERTY_ADDRESS_PINCODE",
    type: "text",
    name: "pincode",
    disable: isEditProperty,
    validation: {
      minlength: 6,
      maxlength: 7,
      pattern: "[0-9]+",
      max: "9999999",
      title: t("PT_PROPERTY_ADDRESS_PINCODE_INVALID")
    }
  }];
  const [pincodeServicability, setPincodeServicability] = useState(null);
  const [error, setLocalError] = useState("");

  // Generate Digipin from coordinates using client-side function
  const generateDigipin = (latitude, longitude) => {
    try {
      const pin = getDigiPin(latitude, longitude);
      setDigipin(pin);
    } catch (error) {
      console.error("Error generating Digipin:", error);
    }
  };

  // Fetch user's current location (latitude & longitude), update state, and generate Digipin
  const fetchCurrentLocation = () => {
    if ("geolocation" in navigator) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          const { latitude, longitude } = position.coords;
          const location = { latitude, longitude };
          const locationString = `${latitude}, ${longitude}`;
          setGeoLocation(location);
          setLocationText(locationString);
          generateDigipin(latitude, longitude);
        },
        (error) => {
          console.error("Error getting location:", error);
          alert("Unable to retrieve your location. Please check your browser settings.");
        }
      );
    } else {
      alert("Geolocation is not supported by your browser.");
    }
  };

  // Parse manually entered coordinates
  const handleLocationTextChange = (e) => {
    const value = e.target.value;
    setLocationText(value);
    
    // Try to parse lat,long format
    const coords = value.split(',');
    if (coords.length === 2) {
      const lat = parseFloat(coords[0].trim());
      const lng = parseFloat(coords[1].trim());
      if (!isNaN(lat) && !isNaN(lng)) {
        setGeoLocation({ latitude: lat, longitude: lng });
        generateDigipin(lat, lng);
      }
    }
  };

  useEffect(() => {
    if (formData?.address?.pincode) {
      setPincode(formData.address.pincode);
    }
  }, [formData?.address?.pincode]);
  function onChange(e) {
    setPincode(e.target.value);
    setPincodeServicability(null);
    setLocalError("");
    let validPincode = Digit.Utils.getPattern("Pincode").test(e.target.value);
    if (userType === "employee") {
      if (e.target.value && !validPincode) setLocalError(t("ERR_DEFAULT_INPUT_FIELD_MSG"));
      if (validPincode) {
        const foundValue = tenants?.find(obj => obj.pincode?.find(item => item.toString() === e.target.value));
        if (!foundValue) setLocalError(t("PT_COMMON_PINCODE_NOT_SERVICABLE"));
      }
      onSelect(config.key, {
        ...formData.address,
        pincode: e.target.value
      });
    }
  }

  const goNext = async (data) => {
    const foundValue = tenants?.find((obj) => obj.pincode?.find((item) => item == data?.pincode));
    if (foundValue || locationText) {
      onSelect(config.key, { pincode, geoLocation, digipin });
    } else {
      setPincodeServicability("PT_COMMON_PINCODE_NOT_SERVICABLE");
    }
  };
  if (userType === "employee") {
    return inputs?.map((input, index) => {
      return <React.Fragment>
          <LabelFieldPair key={index}>
            <CardLabel className="card-label-smaller">{t(input.label)}</CardLabel>
            <div className="field">
              <TextInput key={input.name} value={pincode} onChange={onChange} {...input.validation} disable={presentInModifyApplication} autoFocus={presentInModifyApplication} />
            </div>
          </LabelFieldPair>
          {error ? <CardLabelError className="pt-auto-60">{error}</CardLabelError> : null}
        </React.Fragment>;
    });
  }
  const onSkip = () => onSelect();
  return (
    <React.Fragment>
    {window.location.href.includes("/citizen") ? <Timeline currentStep={1}/> : null}
    
    <FormStep
      t={t}
      config={{ ...config, inputs }}
      onSelect={goNext}
      _defaultValues={{ pincode }}
      onChange={onChange}
      onSkip={onSkip}
      forcedError={t(pincodeServicability)}
      isDisabled={!pincode && !locationText || isEditProperty}
    >
      {/* Location Input with Fetch Button */}
      <div style={{ marginBottom: "20px" }}>
        <div style={{ display: "flex", alignItems: "stretch", gap: "8px" }}>
          <TextInput
            t={t}
            type="text"
            name="currentLocation"
            value={locationText}
            onChange={handleLocationTextChange}
            placeholder="Click location icon to fetch current location"
            style={{ flex: 1 }}
          />
          <div
            className="butt-icon"
            onClick={fetchCurrentLocation}
            style={{ cursor: "pointer", display: "flex", alignItems: "center", padding: "5px" }}
          >
            <LocationIcon styles={{ width: "16px", border: "none" }} className="fill-path-primary-main" />
          </div>
        </div>
        <DigipinDisplay t={t} digipin={digipin} />
      </div>
    </FormStep>
            </React.Fragment>
  );
};
export default PTSelectPincode;
