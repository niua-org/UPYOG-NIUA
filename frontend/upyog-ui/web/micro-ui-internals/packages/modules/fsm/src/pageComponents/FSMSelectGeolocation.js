import React, { useState, useEffect } from "react";
import { LocationSearchCard } from "@nudmcdgnpm/digit-ui-react-components";
import Timeline from "../components/TLTimelineInFSM";
import { getDigiPin } from "../../../../libraries/src/utils/digipin";

const FSMSelectGeolocation = ({ t, config, onSelect, formData = {} }) => {
  const [pincode, setPincode] = useState(formData?.address?.pincode || "");
  const [geoLocation, setGeoLocation] = useState(formData?.address?.geoLocation || {});
  const tenants = Digit.Hooks.fsm.useTenants();
  const [pincodeServicability, setPincodeServicability] = useState(null);
  const [digipin, setDigipin] = useState("");

  const onSkip = () => onSelect();
  const onChange = (code, location) => {
    setPincodeServicability(null);
    const foundValue = tenants?.find((obj) => obj.pincode?.find((item) => item == code));
    if (!foundValue) {
      setPincodeServicability("CS_COMMON_PINCODE_NOT_SERVICABLE");
      setPincode("");
      setGeoLocation({});
    } else {
      setPincode(code);
      setGeoLocation(location);
    }
  };
  // On component mount, fetch user's current location and generate Digipin
  useEffect(() => {
    if ("geolocation" in navigator) {
      navigator.geolocation.getCurrentPosition(
        async (position) => {
          const { latitude, longitude } = position.coords;
          setGeoLocation({ latitude, longitude });
          try {
            const pin = await getDigiPin(latitude, longitude);
            setDigipin(pin);
          } catch (err) {
            console.error("Error generating digipin:", err);
          }
        },
        (error) => console.error("Error getting location:", error)
      );
    }
  }, []);

  return (
    <React.Fragment>
      <Timeline currentStep={1} flow="APPLY" />
      {digipin && (
        <div style={{ 
          marginTop: "10px", 
          padding: "19px 78px",
          backgroundColor: "#f0f0f0",
          borderRadius: "37px",
          border: "1px solid #d4d4d4",
          width: "50%",
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center"
        }}>
          <div>
            <strong>Digipin:</strong> {digipin}
          </div>
        </div>
      )}
      <LocationSearchCard
        header={t("CS_ADDCOMPLAINT_SELECT_GEOLOCATION_HEADER")}
        cardText={t("CS_ADDCOMPLAINT_SELECT_GEOLOCATION_TEXT")}
        nextText={t("CS_COMMON_NEXT")}
        skipAndContinueText={t("CORE_COMMON_SKIP_CONTINUE")}
        skip={onSkip}
        t={t}
        position={geoLocation}
        onSave={() => onSelect(config.key, { geoLocation, pincode })}
        onChange={(code, location) => onChange(code, location)}
        disabled={pincode === ""}
        forcedError={t(pincodeServicability)}
      />
    </React.Fragment>
  );
};

export default FSMSelectGeolocation;
