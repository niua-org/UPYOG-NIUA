export const checkForNotNull = (value = "") => {
  return value && value != null && value != undefined && value != "" ? true : false;
};

export const convertDotValues = (value = "") => {
  return (
    (checkForNotNull(value) && ((value.replaceAll && value.replaceAll(".", "_")) || (value.replace && stringReplaceAll(value, ".", "_")))) || "NA"
  );
};



export const getFixedFilename = (filename = "", size = 5) => {
  if (filename.length <= size) {
    return filename;
  }
  return `${filename.substr(0, size)}...`;
};

export const shouldHideBackButton = (config = []) => {
  return config.filter((key) => window.location.href.includes(key.screenPath)).length > 0 ? true : false;
};


export const CompareTwoObjects = (ob1, ob2) => {
  let comp = 0;
Object.keys(ob1).map((key) =>{
  if(typeof ob1[key] == "object")
  {
    if(key == "institution")
    {
      if((ob1[key].name || ob2[key].name) && ob1[key]?.name !== ob2[key]?.name)
      comp=1
      else if(ob1[key]?.type?.code !== ob2[key]?.type?.code)
      comp=1
      
    }
    else if(ob1[key]?.code !== ob2[key]?.code)
    comp=1
  }
  else
  {
    if((ob1[key] || ob2[key]) && ob1[key] !== ob2[key])
    comp=1
  }
});
if(comp==1)
return false
else
return true;
}

/*   method to check value  if not returns NA*/
export const checkForNA = (value = "") => {
  return checkForNotNull(value) ? value : "NA";
};

/*   method to get required format from fielstore url*/
export const pdfDownloadLink = (documents = {}, fileStoreId = "", format = "") => {
  /* Need to enhance this util to return required format*/

  let downloadLink = documents[fileStoreId] || "";
  let differentFormats = downloadLink?.split(",") || [];
  let fileURL = "";
  differentFormats.length > 0 &&
    differentFormats.map((link) => {
      if (!link.includes("large") && !link.includes("medium") && !link.includes("small")) {
        fileURL = link;
      }
    });
  return fileURL;
};

/*   method to get filename  from fielstore url*/
export const pdfDocumentName = (documentLink = "", index = 0) => {
  let documentName = decodeURIComponent(documentLink.split("?")[0].split("/").pop().slice(13)) || `Document - ${index + 1}`;
  return documentName;
};

/* methid to get date from epoch */
export const convertDateToEpoch = (dateString) => {
  // Parse the date string into a Date object
  const date = new Date(dateString);

  // Check if the date is valid
  if (!isNaN(date)) {
    // Return the epoch time in seconds (divide by 1000 to convert from milliseconds to seconds)
    return Math.floor(date.getTime() / 1000);
  } else {
    return null; // Return null if the input date is invalid
  }
};

export const convertStringToFloat = (amountString) => {
  // Remove commas if present and convert to float
  const cleanedString = amountString.replace(/,/g, '');
  
  // Convert to float and return
  const floatValue = parseFloat(cleanedString);
  
  // Return the float value, or NaN if conversion fails
  return isNaN(floatValue) ? null : floatValue;
};


export const Assetdata = (data) => {
  // Define the keys to exclude from additionalDetails
  const keysToRemove = [
    "acquisitionCost",
    "assetAge",
    "bookValue",
    "invoiceDate",
    "invoiceNumber",
    "purchaseDate",
    "purchaseOrderNumber",
    "purchaseCost",
    "location",
    "lifeOfAsset" 
  ];

  // Ensure data?.assetDetails exists before attempting to filter
  const assetDetails = data?.asset?.assetDetails || {}; // Default to empty object if undefined or null
  

  // Filter out the keys to remove from assetDetails
  const filteredAdditionalDetails = Object.keys(assetDetails)
    .filter(key => !keysToRemove.includes(key)) // Remove unwanted keys
    .reduce((acc, key) => {
      acc[key] = assetDetails[key]; // Add remaining keys to the accumulator
      return acc;
    }, {});
  const formdata = {
    Asset: {
      accountId: "",
      tenantId: data?.asset?.address?.city?.code,
      assetBookRefNo: data?.asset?.asset?.BookPagereference,
      assetName: data?.asset?.asset?.AssetName,
      description: data?.asset?.asset?.Assetdescription,
      assetClassification:data?.asset?.asset?.assetclassification?.code,
      assetParentCategory: data?.asset?.asset?.assettype?.code,
      assetCategory:data?.asset?.asset?.assetsubtype?.code,
      assetSubCategory:data?.asset?.asset?.assetparentsubCategory?.code,
      department: data?.asset?.asset?.Department?.code,
      // assetType: data?.asset?.assetsOfType?.code,
      assetUsage: data?.asset?.asset?.assetsUsage?.code,
      assetIsAssigned: data?.asset?.asset?.assetAssignable?.code,
      financialYear: data?.asset?.asset?.financialYear?.code,
      sourceOfFinance: data?.asset?.asset?.sourceOfFinance?.code,
      applicationNo: "",
      approvalDate: "",
      applicationDate: "",
      status: "",
      modeOfPossessionOrAcquisition: data?.asset?.assetDetails?.modeOfPossessionOrAcquisition?.code,
      invoiceDate: convertDateToEpoch(data?.asset?.assetDetails?.invoiceDate),
      invoiceNumber: data?.asset?.assetDetails?.invoiceNumber,
      purchaseDate: convertDateToEpoch(data?.asset?.assetDetails?.purchaseDate),
      purchaseOrderNumber: data?.asset?.assetDetails?.purchaseOrderNumber,
      assetAge: data?.asset?.assetDetails?.assetAge,
      location: data?.asset?.assetDetails?.location,
      lifeOfAsset: data?.asset?.assetDetails?.lifeOfAsset,
      purchaseCost: convertStringToFloat(data?.asset?.assetDetails?.purchaseCost),
      acquisitionCost: convertStringToFloat(data?.asset?.assetDetails?.acquisitionCost),
      bookValue: data?.asset?.assetDetails?.bookValue,
      originalBookValue: data?.asset?.assetDetails?.bookValue,
      islegacyData: false,
      minimumValue: 0,
      assetStatus: "1",
      action: "",
      businessService: "asset-create",

      addressDetails: {
        addressLine1:data?.asset?.address?.addressLine1,
        addressLine2:data?.asset?.address?.addressLine2,
        buildingName:data?.asset?.address?.buildingName,
        doorNo:data?.asset?.address?.doorNo,
        street:data?.asset?.address?.street,
        pincode:data?.asset?.address?.pincode,
        city:data?.asset?.address?.city?.name,
        locality: { code: data?.asset?.address?.locality?.code,
                    area: data?.asset?.address?.locality?.area,
                    latitude: data?.asset?.address?.latitude,
                    longitude: data?.asset?.address?.longitude },

      },
      documents: data?.asset?.documents?.documents,
      workflow : {
        action: "INITIATE",
        businessService: "asset-create",
        moduleName: "asset-services"
      },

      additionalDetails: filteredAdditionalDetails,

    },
  };

  return formdata;
};
export const stringReplaceAll = (str = "", searcher = "", replaceWith = "") => {
  if (searcher == "") return str;
  while (str.includes(searcher)) {
    str = str.replace(searcher, replaceWith);
  }
  return str;
};

export const DownloadReceipt = async (consumerCode, tenantId, businessService, pdfKey = "consolidatedreceipt") => {
  tenantId = tenantId ? tenantId : Digit.ULBService.getCurrentTenantId();
  await Digit.Utils.downloadReceipt(consumerCode, businessService, "consolidatedreceipt", tenantId);
};

export const checkIsAnArray = (obj = []) => {
  return obj && Array.isArray(obj) ? true : false;
};
export const checkArrayLength = (obj = [], length = 0) => {
  return checkIsAnArray(obj) && obj.length > length ? true : false;
};

export const getWorkflow = (data = {}) => {
  return {

    businessService: `asset-create`,
    moduleName: "asset-services",
  };
};

