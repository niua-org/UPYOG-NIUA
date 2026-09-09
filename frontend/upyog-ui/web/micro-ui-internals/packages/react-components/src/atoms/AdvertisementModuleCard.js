import React, { useEffect } from 'react';
// this code shows the image and the detail of the advertisement

const AdvertisementModuleCard = ({ imageSrc, title, location, poleNo, price, path, light,adType,faceArea }) => {
  const [params, setParams,clearParams] = Digit.Hooks.useSessionStorage("ADS_CREATE", {});
  const handleViewAvailability = () => {
    setParams({
      faceArea:{code:faceArea,value:faceArea,i18nKey:faceArea},
      adType:{code:adType,value:adType,i18nKey:adType},
      location:{code:location,value:location,i18nKey:location},
      fromDate: new Date(new Date().setDate(new Date().getDate() + 1)).toISOString().split('T')[0],
      toDate: new Date(new Date().setMonth(new Date().getMonth() + 2)).toISOString().split("T")[0], // 3 months later
      nightLight:{
        i18nKey: "Yes",
        code: "Yes",
        value: "true",
      }
    });
    window.location.href = `${path}bookad/searchads`;
  };
  useEffect(() => {
    clearParams();
  }, []); 
  const handleBookNow = () => {
    setParams({
      faceArea:{code:faceArea,value:faceArea,i18nKey:faceArea},
      adType:{code:adType,value:adType,i18nKey:adType},
      location:{code:location,value:location,i18nKey:location},
      nightLight:{
        i18nKey: "Yes",
        code: "Yes",
        value: "true",
      }
    });
    window.location.href = `${path}bookad/searchads`;
  };
  return (
    <div
      className="bg-white rounded-md" style={{ border: "1px solid #ccc", overflow: "hidden", maxWidth: "30%", margin: "10px auto", minWidth: "24%" }}
    >
      <div style={{ width: "100%", height: "200px", position: "relative",padding: "10px"}}>
        <img
          src={imageSrc}
          alt="Advertisement"
          style={{
            width: "100%",
            height: "100%",
            backgroundSize: "cover",
            backgroundRepeat: "no-repeat",
            backgroundPosition: "center",
            minWidth: "0",
          }}
        />
      </div>
      <div className="p-sm">
        <p className="text-primary-main m-0">{light}</p>
        <h3 className="font-bold my-xs">{title}</h3>
        <p>
          {location} (
          <button type="button" className="text-primary-main ml-xs">
            View Map
          </button>
          )
        </p>
        <div className="flex justify-between">
          <p>Pole No: {poleNo}</p>
          <p>₹ {price}</p>
        </div>
        <div className="flex justify-between">
          <button
            type="button"
            onClick={handleViewAvailability}
            className="bg-success rounded-sm text-white border-sm border-solid border-border py-xs px-sm"
          >
            View Availability
          </button>
          <button
            type="button"
            onClick={handleBookNow}
            className="bg-primary-main rounded-sm text-white border-sm border-solid border-border py-xs px-sm"
          >
            Book Now
          </button>
        </div>
      </div>
    </div>
  );
};
export { AdvertisementModuleCard };