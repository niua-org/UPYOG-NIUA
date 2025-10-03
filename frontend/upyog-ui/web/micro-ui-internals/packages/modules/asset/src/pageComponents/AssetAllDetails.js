import React, { useEffect, useState } from "react";
// import { FormStep, TextInput, LocationIcon, InfoBannerIcon, Dropdown, CardHeader } from "@upyog/digit-ui-react-components";
import { 
  FormStep, TextInput, LocationIcon, InfoBannerIcon, Dropdown, CardHeader,
  CardLabel, UploadFile, Toast, LabelFieldPair
} from "@upyog/digit-ui-react-components";
import Timeline from "../components/ASTTimeline";
import { Controller, useForm } from "react-hook-form";
import EXIF from 'exif-js';

const AssetAllDetails = ({ t, config, onSelect, userType, formData }) => {
    console.log("formData in AssetAllDetails",formData);
  const { control } = useForm();
  const allCities = Digit.Hooks.asset.useTenants();
  const tenantId = Digit.ULBService.getCurrentTenantId();
  const stateTenantId = Digit.ULBService.getStateId();
  let index = 0;
  let validation = {};
  const convertToObject = (String) => String ? { i18nKey: String, code: String, value: String } : null;
  const calculateCurrentFinancialYear = () => {
    const currentDate = new Date();
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth() + 1; // getMonth() is zero-based
    if (month >= 4) {
      return `${year}-${year + 1}`;
    } else {
      return `${year - 1}-${year}`;
    }
  };

  const initialFinancialYear = calculateCurrentFinancialYear();

  // data set priveis
  const [assetclassification, setassetclassification] = useState(
    (formData.asset && formData.asset[index] && formData.asset[index].assetclassification) || formData?.asset?.assetclassification || ""
  );
  const [assettype, setassettype] = useState(
    (formData.asset && formData.asset[index] && formData.asset[index].assettype) || formData?.asset?.assettype || ""
  );
  const [assetsubtype, setassetsubtype] = useState(
    (formData.asset && formData.asset[index] && formData.asset[index].assetsubtype) || formData?.asset?.assetsubtype || ""
  );

  const [assetparentsubCategory, setassetparentsubCategory] = useState(
    (formData.asset && formData.asset[index] && formData.asset[index].assetparentsubCategory) || formData?.asset?.assetparentsubCategory || ""
  );

  const [BookPagereference, setBookPagereference] = useState(
    (formData.asset && formData.asset[index] && formData.asset[index].BookPagereference) || formData?.asset?.BookPagereference || ""
  );
  const [AssetName, setAssetName] = useState(
    (formData.asset && formData.asset[index] && formData.asset[index].AssetName) || formData?.asset?.AssetName || ""
  );
  const [Assetdescription, setAssetdescription] = useState(
    (formData.asset && formData.asset[index] && formData.asset[index].Assetdescription) || formData?.asset?.Assetdescription || ""
  );
  const [Department, setDepartment] = useState(
    (formData.asset && formData.asset[index] && formData.asset[index].Department) || formData?.asset?.Department || ""
  );

  const [assetsOfType, setAssetsOfType] = useState(
    (formData.asset && formData.asset[index] && formData.asset[index].assetsOfType) || formData?.asset?.assetsOfType || ""
  );
  const [assetsUsage, setAssetsUsage] = useState(
    (formData.asset && formData.asset[index] && formData.asset[index].assetsUsage) || formData?.asset?.assetsUsage || ""
  );
  const [assetAssignable, setAssetAssignable] = useState(
    (formData.asset && formData.asset[index] && formData.asset[index].assetAssignable) || formData?.asset?.assetAssignable || ""
  );

  const [financialYear, setfinancialYear] = useState(
    (formData.asset && formData.asset[index] && formData.asset[index].financialYear) || formData?.asset?.financialYear || initialFinancialYear
  );
  const [sourceOfFinance, setsourceOfFinance] = useState(
    (formData.asset && formData.asset[index] && formData.asset[index].sourceOfFinance) || formData?.asset?.sourceOfFinance || ""
  );
  const [address, setAddress] = useState(formData.address);

  const [city, setCity] = useState(convertToObject(formData?.city) || "");
  const [locality, setLocality] = useState(convertToObject(formData?.locality) || "");
      // Document State - NEW
  const [documents, setDocuments] = useState(formData?.documents?.documents || []);
  const [documentError, setDocumentError] = useState(null);
  const [enableSubmit, setEnableSubmit] = useState(true);
  const [checkRequiredFields, setCheckRequiredFields] = useState(true);
   

  
    const [assetDetails, setAssetDetails] = useState(
      formData.assetDetails && formData.assetDetails.assetParentCategory === formData?.asset?.assettype?.code
        ? formData.assetDetails
        : { assetParentCategory: formData?.asset?.assettype?.code,
          geometry: formData.assetDetails?.geometry || null,
          area: formData.assetDetails?.area || null
         }
    );

    const { data: fetchedLocalities } = Digit.Hooks.useBoundaryLocalities(
    tenantId,
    "revenue",
    {
      enabled: !!tenantId,
    },
    t
  );

  let structuredLocality = [];
  fetchedLocalities && fetchedLocalities.map((localityData, index) => {
    structuredLocality.push({ i18nKey: localityData?.i18nkey, code: localityData?.code, label: localityData?.label, area: localityData?.area, boundaryNum: localityData?.boundaryNum })
  })


    const [currentLocation, setCurrentLocation] = useState(null);
  
  
    const [categoriesWiseData, setCategoriesWiseData] = useState();

  

  // const { data: Menu_Asset } = Digit.Hooks.asset.useAssetClassification(stateTenantId, "ASSET", "assetClassification"); // hook for asset classification Type
  const { data: Menu_Asset } = Digit.Hooks.useEnabledMDMS(Digit.ULBService.getStateId(), "ASSET", [{ name: "assetClassification" }], {
    select: (data) => {
      const formattedData = data?.["ASSET"]?.["assetClassification"];
      const activeData = formattedData?.filter((item) => item.active === true);
      return activeData;
    },
  });

  // const { data: Asset_Type } = Digit.Hooks.asset.useAssetType(stateTenantId, "ASSET", "assetParentCategory");
  const { data: Asset_Type } = Digit.Hooks.useEnabledMDMS(Digit.ULBService.getStateId(), "ASSET", [{ name: "assetParentCategory" }], {
    select: (data) => {
      const formattedData = data?.["ASSET"]?.["assetParentCategory"];
      const activeData = formattedData?.filter((item) => item.active === true);
      return activeData;
    },
  });

  const { data: Asset_Sub_Type } = Digit.Hooks.asset.useAssetSubType(stateTenantId, "ASSET", "assetCategory"); // hooks for Asset Parent Category

  // const { data: Asset_Parent_Sub_Type } = Digit.Hooks.asset.useAssetparentSubType(stateTenantId, "ASSET", "assetSubCategory");

  // For Sub Catagories
  const { data: Asset_Parent_Sub_Type } = Digit.Hooks.useEnabledMDMS(Digit.ULBService.getStateId(), "ASSET", [{ name: "assetSubCategory" }], {
    select: (data) => {
      const formattedData = data?.["ASSET"]?.["assetSubCategory"];
      const activeData = formattedData?.filter((item) => item.active === true);
      return activeData;
    },
  });

  const { data: sourceofFinanceMDMS } = Digit.Hooks.useEnabledMDMS(Digit.ULBService.getStateId(), "ASSET", [{ name: "SourceFinance" }], {
    select: (data) => {
      const formattedData = data?.["ASSET"]?.["SourceFinance"];
      const activeData = formattedData?.filter((item) => item.active === true);
      return activeData;
    },
  }); // Note : used direct custom MDMS to get the Data ,Do not copy and paste without understanding the Context

  let sourcefinance = [];

  sourceofFinanceMDMS &&
    sourceofFinanceMDMS.map((finance) => {
      sourcefinance.push({ i18nKey: `AST_${finance.code}`, code: `${finance.code}`, value: `${finance.name}` });
    });

    const { data: assetCurrentUsageData } = Digit.Hooks.useEnabledMDMS(Digit.ULBService.getStateId(), "ASSET", [{ name: "AssetUsage" }], {
    select: (data) => {
      const formattedData = data?.["ASSET"]?.["AssetUsage"];
      return formattedData;
    },
  });
  let assetCurrentUsage = [];

  assetCurrentUsageData &&
    assetCurrentUsageData.map((assT) => {
      assetCurrentUsage.push({ i18nKey: `${assT.code}`, code: `${assT.code}`, value: `${assT.name}` });
    });

  // This is use for Asset Assigned / Not Assigned menu
  let assetAssignableMenu = [
    { i18nKey: "YES", code: "YES", value: "YES" },
    { i18nKey: "NO", code: "NO", value: "NO" },
  ];


  const { data: currentFinancialYear } = Digit.Hooks.useEnabledMDMS(Digit.ULBService.getStateId(), "ASSET", [{ name: "FinancialYear" }], {
    select: (data) => {
      const formattedData = data?.["ASSET"]?.["FinancialYear"];
      return formattedData;
    },
  });

  let financal = [];

  currentFinancialYear &&
    currentFinancialYear.map((financialyear) => {
      financal.push({ i18nKey: `${financialyear.code}`, code: `${financialyear.code}`, value: `${financialyear.name}` });
    });

  const { data: departmentName } = Digit.Hooks.useEnabledMDMS(Digit.ULBService.getStateId(), "common-masters", [{ name: "Department" }], {
    select: (data) => {
      const formattedData = data?.["common-masters"]?.["Department"];
      const activeData = formattedData?.filter((item) => item.active === true);
      return activeData;
    },
  });

  //  This call with tenantId (Get city-level data)
    const cityResponseObject = Digit.Hooks.useCustomMDMS(tenantId, "ASSET", [{ name: "AssetParentCategoryFields" }], {
      select: (data) => {
        const formattedData = data?.["ASSET"]?.["AssetParentCategoryFields"];
        return formattedData;
      },
    });
  
    // This call with stateTenantId (Get state-level data)
    const stateResponseObject = Digit.Hooks.useEnabledMDMS(stateTenantId, "ASSET", [{ name: "AssetParentCategoryFields" }], {
      select: (data) => {
        const formattedData = data?.["ASSET"]?.["AssetParentCategoryFields"];
        return formattedData;
      },
    });

    const { isLoading: isDocumentLoading, data: documentData } = Digit.Hooks.asset.useAssetDocumentsMDMS(stateTenantId, "ASSET", "Documents");

  
    useEffect(() => {
      let combinedData;
      // if city level master is not available then fetch  from state-level
      if (cityResponseObject?.data) {
        combinedData = cityResponseObject.data;
      } else if (stateResponseObject?.data) {
        combinedData = stateResponseObject.data;
      } else {
        combinedData = []; // Or an appropriate default value for empty data
        console.log("Both cityResponseObject and stateResponseObject data are unavailable.");
      }
      setCategoriesWiseData(combinedData);
    }, [cityResponseObject, stateResponseObject]);

    let formJson = [];
  if (Array.isArray(categoriesWiseData)) {
    // Filter categories based on the selected assetParentCategory
    formJson = categoriesWiseData
      .filter((category) => {
        const isMatch = category.assetParentCategory === assettype?.code || category.assetParentCategory === "COMMON";
        return isMatch;
      })
      .map((category) => category.fields) // Extract the fields array
      .flat() // Flatten the fields array
      .filter((field) => field.active === true && field.isNeeded !== false); // Filter by active status
    };

    useEffect(() => {
    if (documentData?.ASSET?.Documents) {
      let count = 0;
      documentData.ASSET.Documents.forEach((doc) => {
        doc.hasDropdown = true;
        let isRequired = documents.some((data) => doc.required && data?.documentType.includes(doc.code));
        if (!isRequired && doc.required) count += 1;
      });
      setEnableSubmit(!(count === 0 && documents.length > 0));
    }
  }, [documents, checkRequiredFields, documentData]);


  let departNamefromMDMS = [];

  departmentName &&
    departmentName.map((departmentname) => {
      departNamefromMDMS.push({
        i18nKey: `COMMON_MASTERS_DEPARTMENT_${departmentname.code}`,
        code: `${departmentname.code}`,
        value: `COMMON_MASTERS_DEPARTMENT_${departmentname.code}`,
      });
    });
  let menu_Asset = []; //variable name for assetCalssification
  let asset_type = []; //variable name for asset type
  let asset_sub_type = []; //variable name for asset sub  parent caregory
  let asset_parent_sub_category = [];

  Menu_Asset &&
    Menu_Asset.map((asset_mdms) => {
      if (asset_mdms?.code === assettype?.assetClassification) {
        menu_Asset.push({ i18nKey: `${asset_mdms.name}`, code: `${asset_mdms.code}`, value: `${asset_mdms.name}` });
      }
    });

  Asset_Type &&
    Asset_Type.map((asset_type_mdms) => {
      asset_type.push({
        i18nKey: `${asset_type_mdms.name}`,
        code: `${asset_type_mdms.code}`,
        value: `${asset_type_mdms.name}`,
        assetClassification: `${asset_type_mdms.assetClassification}`,
      });
    });

  Asset_Sub_Type &&
    Asset_Sub_Type.map((asset_sub_type_mdms) => {
      if (asset_sub_type_mdms.assetParentCategory == assettype?.code) {
        asset_sub_type.push({
          i18nKey: `${asset_sub_type_mdms.name}`,
          code: `${asset_sub_type_mdms.code}`,
          value: `${asset_sub_type_mdms.name}`,
        });
      }
    });

  Asset_Parent_Sub_Type &&
    Asset_Parent_Sub_Type.map((asset_parent_mdms) => {
      if (asset_parent_mdms.assetCategory == assetsubtype?.code) {
        asset_parent_sub_category.push({
          i18nKey: `${asset_parent_mdms.name}`,
          code: `${asset_parent_mdms.code}`,
          value: `${asset_parent_mdms.name}`,
        });
      }
    });


    const regexPattern = (columnType) => {
    if (!columnType) {
      return "^[-+]?([1-8]?\\d(\\.\\d+)?|90(\\.0+)?),\\s*[-+]?(180(\\.0+)?|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d+)?)$";
    } else if (columnType === "number") {
      return "^[0-9]+(\\.[0-9]+)?$";
    } else if (columnType === "text") {
      return "^[-+]?([1-8]?\\d(\\.\\d+)?|90(\\.0+)?),\\s*[-+]?(180(\\.0+)?|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d+)?)$";
    } else {
      return "^[-+]?([1-8]?\\d(\\.\\d+)?|90(\\.0+)?),\\s*[-+]?(180(\\.0+)?|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d+)?)$";
    }
  };

  console.log("assteyhwedwef",assetDetails);
    

  function setbookpagereference(e) {
    setBookPagereference(e.target.value);
  }
  function setassetname(e) {
    setAssetName(e.target.value);
  }
  function setassetDescription(e) {
    setAssetdescription(e.target.value);
  }

  // Set State Dynamically!
  const handleInputChange = (e) => {
    // Get the name & value from the input and select field
    const { name, value } = e.target ? e.target : { name: e.name, value: e.code };

    if (name === "lifeOfAsset" && value.length > 3) {
      // Validation for life of Asset
      alert("Maximum limit is 3 digits only!");
      return false;
    }
    setAssetDetails((prevData) => {
      // Update the current field
      const updatedData = {
        ...prevData,
        [name]: value,
      };

      // Check if both acquisitionCost and purchaseCost are set and calculate bookValue
      const acquisitionCost = parseFloat(updatedData.acquisitionCost) || 0;
      const purchaseCost = parseFloat(updatedData.purchaseCost) || 0;

      if (acquisitionCost >= 0 || purchaseCost >= 0) {
        updatedData.bookValue = acquisitionCost + purchaseCost;
      }

      return updatedData;
    });
  };



   //  Get location
  const fetchCurrentLocation = (name) => {
    if ("geolocation" in navigator) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          const { latitude, longitude } = position.coords;
          const locationString = `${latitude}, ${longitude}`;
          setAssetDetails((prevDetails) => ({
            ...prevDetails,
            [name]: locationString, // Update the specific field
          }));
          // Set the location for the map component
          setCurrentLocation({ lat: latitude, lng: longitude });
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

//   const goNext = () => {
//     let owner = formData.asset
//     let ownerStep = { ...owner, financialYear, sourceOfFinance, assetclassification, assetparentsubCategory, assetsubtype, assettype };

//     onSelect(config.key, { ...formData[config.key], ...ownerStep, assetDetails: assetDetails, address:address, documents: { documents }  }, false);
//   };

    const goNext = () => {
  // Create separate objects for each section
  const assetData = { 
    financialYear, 
    sourceOfFinance, 
    assetclassification, 
    assetparentsubCategory, 
    assetsubtype, 
    assettype,
    BookPagereference,
    AssetName,
    Assetdescription,
    Department,
    assetsUsage,
    assetAssignable
  };

  const assetDetailsData = { ...assetDetails };
  const addressData = { ...address };
  const documentsData = { documents };

  // Pass them as a single object that contains all sections
  const allData = { 
    asset: assetData,
    assetDetails: assetDetailsData, 
    address: addressData, 
    documents: documentsData  
  };

  onSelect(config.key, allData, false);
};


  const onSkip = () => onSelect();


  return (
    <React.Fragment>
      {<Timeline currentStep={1} />}

      <FormStep config={config} onSelect={goNext} onSkip={onSkip} t={t} isDisabled={!assettype || !assetsubtype || !BookPagereference || !assetDetails["marketRate"] || !assetDetails["purchaseCost"] || !assetDetails["acquisitionCost"] ||!assetDetails["bookValue"]|| !assetDetails["purchaseDate"] || !assetDetails["modeOfPossessionOrAcquisition"] || !assetDetails["purchaseOrderNumber"]}>
        <React.Fragment>
          <CardHeader>{t("ASSET_GENERAL_DETAILS")}</CardHeader>
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "1fr 1fr",
              gap: "20px",
              marginBottom: "30px",
              border: "1px solid rgb(101 43 43)",
              borderRadius: "8px",
              padding: "16px",
            }}
          >


            {/* Field 3 - Parent Category */}
            <div>
              <div>
                {`${t("AST_PARENT_CATEGORY")}`} <span style={{ color: "red" }}>*</span>
              </div>
              <Controller
                control={control}
                name={"assettype"}
                defaultValue={assettype}
                rules={{ required: t("CORE_COMMON_REQUIRED_ERRMSG") }}
                render={(props) => (
                  <Dropdown selected={assettype} select={setassettype} option={asset_type} optionKey="i18nKey" placeholder={"Select"} t={t} />
                )}
              />
            </div>

            {/* Field 4 - Asset Classification */}
            <div>
              <div>
                {t("AST_CATEGORY")} <span style={{ color: "red" }}>*</span>
                <div className="tooltip" style={{ width: "12px", height: "5px", marginLeft: "10px", display: "inline-flex", alignItems: "center" }}>
                  <InfoBannerIcon />
                  <span
                    className="tooltiptext"
                    style={{
                      whiteSpace: "pre-wrap",
                      fontSize: "small",
                      wordWrap: "break-word",
                      width: "300px",
                      marginLeft: "15px",
                      marginBottom: "-10px",
                    }}
                  >
                    {`${t(`AST_CLASSIFICATION_ASSET`)}`}
                  </span>
                </div>
              </div>
              <Controller
                control={control}
                name={"assetclassification"}
                defaultValue={assetclassification}
                rules={{ required: t("CORE_COMMON_REQUIRED_ERRMSG") }}
                render={(props) => (
                  <Dropdown
                    selected={menu_Asset[0]}
                    select={(e) => {setassetclassification(e)}}
                    option={menu_Asset}
                    optionKey="i18nKey"
                    placeholder={"Select"}
                    t={t}
                    disable={true}
                  />
                )}
              />
            </div>

            {/* Field 5 - Sub Category */}
            <div>
              <div>
                {`${t("AST_SUB_CATEGORY")}`} <span style={{ color: "red" }}>*</span>
              </div>
              <Controller
                control={control}
                name={"assetsubtype"}
                defaultValue={assetsubtype}
                rules={{ required: t("CORE_COMMON_REQUIRED_ERRMSG") }}
                render={(props) => (
                  <Dropdown
                    selected={assetsubtype}
                    select={setassetsubtype}
                    option={asset_sub_type}
                    optionKey="i18nKey"
                    placeholder={"Select"}
                    t={t}
                  />
                )}
              />
            </div>

            {/* Field 6 - Parent Sub Category */}
            <div>
              <div>{`${t("AST_CATEGORY_SUB_CATEGORY")}`}</div>
              <Controller
                control={control}
                name={"assetparentsubCategory"}
                defaultValue={assetparentsubCategory}
                rules={{ required: t("CORE_COMMON_REQUIRED_ERRMSG") }}
                render={(props) => (
                  <Dropdown
                    selected={assetparentsubCategory}
                    select={setassetparentsubCategory}
                    option={asset_parent_sub_category}
                    optionKey="i18nKey"
                    placeholder={"Select"}
                    t={t}
                  />
                )}
              />
            </div>

            {/* Field 10 - Department */}
            <div>
              <div>
                {t("AST_DEPARTMENT")} <span style={{ color: "red" }}>*</span>
                <div className="tooltip" style={{ width: "12px", height: "5px", marginLeft: "10px", display: "inline-flex", alignItems: "center" }}>
                  <InfoBannerIcon />
                  <span
                    className="tooltiptext"
                    style={{
                      whiteSpace: "pre-wrap",
                      fontSize: "small",
                      wordWrap: "break-word",
                      width: "300px",
                      marginLeft: "15px",
                      marginBottom: "-10px",
                    }}
                  >
                    {`${t(`AST_PROCURED_DEPARTMENT`)}`}
                  </span>
                </div>
              </div>
              <Controller
                control={control}
                name={"Department"}
                defaultValue={Department}
                rules={{ required: t("CORE_COMMON_REQUIRED_ERRMSG") }}
                render={(props) => (
                  <Dropdown
                    selected={Department}
                    select={setDepartment}
                    option={departNamefromMDMS}
                    optionKey="i18nKey"
                    placeholder={"Select"}
                    t={t}
                  />
                )}
              />
            </div>


            {/* Field 8 - Asset Name */}
            <div>
              <div>
                {`${t("AST_NAME")}`} <span style={{ color: "red" }}>*</span>
              </div>
              <TextInput
                t={t}
                type={"text"}
                isMandatory={false}
                optionKey="i18nKey"
                name="AssetName"
                value={AssetName}
                onChange={setassetname}
                style={{ width: "100%" }}
                ValidationRequired={false}
                {...(validation = {
                  isRequired: true,
                  pattern: "^[a-zA-Z0-9/-]*$",
                  type: "text",
                  title: t("PT_NAME_ERROR_MESSAGE"),
                })}
              />
            </div>

            {assettype?.code && formJson.filter((e) => e.group === "generalDetails")
      .map((row, index) => {
        if (row.conditionalField) {
          const { dependsOn, showWhen } = row.conditionalField;
          if (assetDetails[dependsOn] !== showWhen) {
            return null;
          }
        } 

        return (
          <div key={index}>
            <div>
              {`${t(row.code)}`} {row.isMandatory ? <span style={{ color: "red" }}>*</span> : null}
              <div className="tooltip" style={{ width: "12px", height: "5px", marginLeft: "10px", display: "inline-flex", alignItems: "center" }}>
                <InfoBannerIcon />
                <span className="tooltiptext" style={{ whiteSpace: "pre-wrap", fontSize: "small", wordWrap: "break-word", width: "300px", marginLeft: "15px", marginBottom: "-10px" }}>
                  {`${t(row.code + "_INFO")} `}
                </span>
              </div>
            </div>

            { row.type === "num" ? (
              <TextInput
                t={t}
                type={"number"}
                isMandatory={row.isMandatory}
                optionKey="i18nKey"
                name={row.name}
                value={assetDetails[row.name] || ""}
                onChange={handleInputChange}
                {...(validation = {
                  isRequired: row.isMandatory,
                  pattern: regexPattern(row.columnType),
                  type: row.columnType,
                  title: t("PT_NAME_ERROR_MESSAGE"),
                })}
                style={{ width: "100%" }}
                readOnly={row.isReadOnly}
              />
            ) : null}
          </div>
        );
        })}
            <div>
                <div>
                {`${t("AST_MARKET_RATE")}`} <span style={{ color: "red" }}>*</span>
                <div className="tooltip" style={{ width: "12px", height: "5px", marginLeft: "10px", display: "inline-flex", alignItems: "center" }}>
                    <InfoBannerIcon />
                    <span className="tooltiptext" style={{ whiteSpace: "pre-wrap", fontSize: "small", wordWrap: "break-word", width: "300px", marginLeft: "15px", marginBottom: "-10px" }}>
                    {`${t("ASSET_MARKET_VALUE")} `}
                    </span>
                </div>
                </div>
                <TextInput
                t={t}
                type={"number"}
                isMandatory={false}
                optionKey="i18nKey"
                name="marketRate"
                value={assetDetails["marketRate"]}
                onChange={handleInputChange}
                ValidationRequired={true}
                {...(validation = {
                    isRequired: true,
                    pattern: regexPattern("number"),
                    type: "number",
                    title: t("PT_NAME_ERROR_MESSAGE"),
                })}
                style={{ width: "100%" }}
                />
            </div>

            <div>
                <div>
                {`${t("AST_PURCHASE_COST")}`} <span style={{ color: "red" }}>*</span>
                <div className="tooltip" style={{ width: "12px", height: "5px", marginLeft: "10px", display: "inline-flex", alignItems: "center" }}>
                    <InfoBannerIcon />
                    <span className="tooltiptext" style={{ whiteSpace: "pre-wrap", fontSize: "small", wordWrap: "break-word", width: "300px", marginLeft: "15px", marginBottom: "-10px" }}>
                    {`${t("ASSET_PURCHASE_COST")} `}
                    </span>
                </div>
                </div>
                <TextInput
                t={t}
                type={"number"}
                isMandatory={false}
                optionKey="i18nKey"
                name="purchaseCost"
                value={assetDetails["purchaseCost"]}
                onChange={handleInputChange}
                ValidationRequired={true}
                {...(validation = {
                    isRequired: true,
                    pattern: regexPattern("number"),
                    type: "number",
                    title: t("PT_NAME_ERROR_MESSAGE"),
                })}
                style={{ width: "100%" }}
                />
            </div>

            <div>
                <div>
                {`${t("AST_BOOK_VALUE")}`} <span style={{ color: "red" }}>*</span>
                <div className="tooltip" style={{ width: "12px", height: "5px", marginLeft: "10px", display: "inline-flex", alignItems: "center" }}>
                    <InfoBannerIcon />
                    <span className="tooltiptext" style={{ whiteSpace: "pre-wrap", fontSize: "small", wordWrap: "break-word", width: "300px", marginLeft: "15px", marginBottom: "-10px" }}>
                    {`${t("ASSET_BOOK_VALUE")} `}
                    </span>
                </div>
                </div>
                <TextInput
                t={t}
                type={"text"}
                isMandatory={false}
                optionKey="i18nKey"
                name="bookValue"
                value={assetDetails["bookValue"]}
                onChange={handleInputChange}
                {...(validation = {
                    isRequired: true,
                    pattern: "^[a-zA-Z0-9/-]*$",
                    type: "text",
                    title: t("PT_NAME_ERROR_MESSAGE"),
                })}
                style={{ width: "100%" }}
                />
            </div>

            <div>
        <div>
            {`${t("AST_LOCATION_DETAILS")}`} <span style={{ color: "red" }}>*</span>
            <div className="tooltip" style={{ width: "12px", height: "5px", marginLeft: "10px", display: "inline-flex", alignItems: "center" }}>
            <InfoBannerIcon />
            <span className="tooltiptext" style={{ whiteSpace: "pre-wrap", fontSize: "small", wordWrap: "break-word", width: "300px", marginLeft: "15px", marginBottom: "-10px" }}>
                {`${t("ASSET_LOCATION_DETAILS")} `}
            </span>
            </div>
        </div>
        <div style={{ position: "relative" }}>
            <TextInput
            t={t}
            type={"text"}
            isMandatory={false}
            optionKey="i18nKey"
            name={"location"}
            value={assetDetails["location"] || ""}
            onChange={handleInputChange}
            style={{ flex: 1 }}
            ValidationRequired={false}
            {...(validation = {
                isRequired: true,
                pattern: "^[-+]?([1-8]?\\d(\\.\\d+)?|90(\\.0+)?),\\s*[-+]?(180(\\.0+)?|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d+)?)$",
                type: "text",
                title: t("VALID_LAT_LONG"),
            })}
            />
            <div
            className="butt-icon"
            onClick={() => fetchCurrentLocation("location")}
            style={{
                position: "absolute",
                right: "0",
                top: "50%",
                transform: "translateY(-50%)",
                cursor: "pointer",
                display: "flex",
                alignItems: "center",
                padding: "2px 5px",
            }}
            >
            <LocationIcon styles={{ width: "16px", border: "none" }} className="fill-path-primary-main" />
            </div>
        </div>

        {/* {assetDetails?.location && (
            <div>
            <button style={{ color: "#a82227" }} onClick={() => setShowMap(true)}>
                Mark Asset on Map
            </button>
            </div>
        )}

        {showMap && (
            <MarkPropertyMap
            onGeometrySave={(geoJson) => {
                setGeometry(geoJson);
                setAssetDetails((prevDetails) => ({
                ...prevDetails,
                geometry: geoJson,
                }));
            }}
            onAreaSave={(polygonArea) => {
                setArea(polygonArea);
                setAssetDetails((prevDetails) => ({
                ...prevDetails,
                area: polygonArea,
                }));
            }}
            closeModal={() => setShowMap(false)}
            location={currentLocation}
            />
        )} */}
            </div>

             <div>
        <div>
          {`${t("AST_LIFE")}`} <span style={{ color: "red" }}>*</span>
          <div className="tooltip" style={{ width: "12px", height: "5px", marginLeft: "10px", display: "inline-flex", alignItems: "center" }}>
            <InfoBannerIcon />
            <span className="tooltiptext" style={{ whiteSpace: "pre-wrap", fontSize: "small", wordWrap: "break-word", width: "300px", marginLeft: "15px", marginBottom: "-10px" }}>
              {`${t("ASSET_USEFUL_LIFECYCLE")} `}
            </span>
          </div>
        </div>
        <TextInput
          t={t}
          type={"text"}
          isMandatory={false}
          optionKey="i18nKey"
          name="lifeOfAsset"
          value={assetDetails["lifeOfAsset"]}
          onChange={handleInputChange}
          {...(validation = {
            isRequired: true,
            pattern: regexPattern("number"),
            type: "number",
            title: t("PT_NAME_ERROR_MESSAGE"),
          })}
          style={{ width: "100%" }}
        />
             </div>

            {/* Field 12 - Assignable remove */}
            {/* <div>
              <div>
                {t("AST_STATUS_ASSIGNABLE")} <span style={{ color: "red" }}>*</span>
              </div>
              <Controller
                control={control}
                name={"assetAssignable"}
                defaultValue={assetAssignable}
                rules={{ required: t("CORE_COMMON_REQUIRED_ERRMSG") }}
                render={(props) => (
                  <Dropdown
                    selected={assetAssignable}
                    select={setAssetAssignable}
                    option={assetAssignableMenu}
                    optionKey="i18nKey"
                    placeholder={"Select"}
                    t={t}
                  />
                )}
              />
            </div> */}
          </div>
        </React.Fragment>
        <React.Fragment>
        <CardHeader>{t("AST_ACQUSTION_DETAILS")}</CardHeader>
            {/* Group 1: Basic Purchase Information */}
        {/* <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '30px',  border: "1px solid rgb(101 43 43)", borderRadius: '8px', padding: '16px' }}>
        
               
        </div> */}


        {assettype?.code && assettype?.code !== "LAND" && (
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '30px', border: "1px solid rgb(101 43 43)", borderRadius: '8px', padding: '16px' }}>
            <div>
            <div>
                {`${t("AST_INVOICE_DATE")}`} <span style={{ color: "red" }}>*</span>
                <div className="tooltip" style={{ width: "12px", height: "5px", marginLeft: "10px", display: "inline-flex", alignItems: "center" }}>
                <InfoBannerIcon />
                <span className="tooltiptext" style={{ whiteSpace: "pre-wrap", fontSize: "small", wordWrap: "break-word", width: "300px", marginLeft: "15px", marginBottom: "-10px" }}>
                    {`${t("ASSET_INVOICE_ISSUE_DATE")} `}
                </span>
                </div>
            </div>
            <TextInput
                t={t}
                key={assetDetails["purchaseDate"] || "no-purchase"}
                type={"date"}
                isMandatory={false}
                optionKey="i18nKey"
                name={"invoiceDate"}
                value={assetDetails["invoiceDate"]}
                onChange={handleInputChange}
                style={{ width: "100%" }}
                min={assetDetails["purchaseDate"] || ""}
                disabled={!assetDetails["purchaseDate"]}
                rules={{
                required: t("CORE_COMMON_REQUIRED_ERRMSG"),
                validDate: (val) => (/^\d{4}-\d{2}-\d{2}$/.test(val) ? true : t("ERR_DEFAULT_INPUT_FIELD_MSG")),
                validate: (val) => {
                    if (!assetDetails["purchaseDate"]) return t("INVOICE_DATE_REQUIRES_PURCHASE_DATE");
                    return true;
                },
                }}
            />
            </div>

            <div>
            <div>
                {`${t("AST_INVOICE_NUMBER")}`} <span style={{ color: "red" }}>*</span>
                <div className="tooltip" style={{ width: "12px", height: "5px", marginLeft: "10px", display: "inline-flex", alignItems: "center" }}>
                <InfoBannerIcon />
                <span className="tooltiptext" style={{ whiteSpace: "pre-wrap", fontSize: "small", wordWrap: "break-word", width: "300px", marginLeft: "15px", marginBottom: "-10px" }}>
                    {`${t("ASSET_INVOICE_ISSUE_DATE")} `}
                </span>
                </div>
            </div>
            <TextInput
                t={t}
                type={"text"}
                isMandatory={false}
                optionKey="i18nKey"
                name="invoiceNumber"
                value={assetDetails["invoiceNumber"]}
                onChange={handleInputChange}
                {...(validation = {
                isRequired: true,
                pattern: "^[a-zA-Z0-9/-]*$",
                type: "text",
                title: t("PT_NAME_ERROR_MESSAGE"),
                })}
                style={{ width: "100%" }}
            />
            </div>
        </div>
        )}
     
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '30px',  border: "1px solid rgb(101 43 43)", borderRadius: '8px', padding: '16px' }}>
       <div>
                    <div>
                    {`${t("AST_ACQUISITION_COST")}`} <span style={{ color: "red" }}>*</span>
                    <div className="tooltip" style={{ width: "12px", height: "5px", marginLeft: "10px", display: "inline-flex", alignItems: "center" }}>
                        <InfoBannerIcon />
                        <span className="tooltiptext" style={{ whiteSpace: "pre-wrap", fontSize: "small", wordWrap: "break-word", width: "300px", marginLeft: "15px", marginBottom: "-10px" }}>
                        {`${t("ASSET_ACQUISITION_COST")} `}
                        </span>
                    </div>
                    </div>
                    <TextInput
                    t={t}
                    type={"text"}
                    isMandatory={false}
                    optionKey="i18nKey"
                    name="acquisitionCost"
                    value={assetDetails["acquisitionCost"]}
                    onChange={handleInputChange}
                    ValidationRequired={true}
                    {...(validation = {
                        isRequired: true,
                        pattern: regexPattern("number"),
                        type: "number",
                        title: t("PT_NAME_ERROR_MESSAGE"),
                    })}
                    style={{ width: "100%" }}
                    />
            </div>


            <div>
                <div>
                {`${t("AST_PURCHASE_DATE")}`} <span style={{ color: "red" }}>*</span>
                <div className="tooltip" style={{ width: "12px", height: "5px", marginLeft: "10px", display: "inline-flex", alignItems: "center" }}>
                    <InfoBannerIcon />
                    <span className="tooltiptext" style={{ whiteSpace: "pre-wrap", fontSize: "small", wordWrap: "break-word", width: "300px", marginLeft: "15px", marginBottom: "-10px" }}>
                    {`${t("ASSET_PURCHASE_DATE")}`}
                    </span>
                </div>
                </div>
                <TextInput
                t={t}
                type={"date"}
                isMandatory={false}
                optionKey="i18nKey"
                name={"purchaseDate"}
                value={assetDetails["purchaseDate"]}
                onChange={handleInputChange}
                style={{ width: "100%" }}
                max={new Date().toISOString().split("T")[0]}
                rules={{
                    required: t("CORE_COMMON_REQUIRED_ERRMSG"),
                    validDate: (val) => (/^\d{4}-\d{2}-\d{2}$/.test(val) ? true : t("ERR_DEFAULT_INPUT_FIELD_MSG")),
                }}
                />
            </div>

            <div>
                <div>
                {`${t("AST_PURCHASE_ORDER")}`} <span style={{ color: "red" }}>*</span>
                </div>
                <TextInput
                t={t}
                type={"text"}
                isMandatory={false}
                optionKey="i18nKey"
                name="purchaseOrderNumber"
                value={assetDetails["purchaseOrderNumber"]}
                onChange={handleInputChange}
                {...(validation = {
                    isRequired: true,
                    pattern: "^[a-zA-Z0-9/-]*$",
                    type: "text",
                    title: t("PT_NAME_ERROR_MESSAGE"),
                })}
                style={{ width: "100%" }}
                />
            </div>


            <div>
                <div>
                {t("AST_SOURCE_FINANCE")} <span style={{ color: "red" }}>*</span>
                <div className="tooltip" style={{ width: "12px", height: "5px", marginLeft: "10px", display: "inline-flex", alignItems: "center" }}>
                    <InfoBannerIcon />
                    <span
                    className="tooltiptext"
                    style={{
                        whiteSpace: "pre-wrap",
                        fontSize: "small",
                        wordWrap: "break-word",
                        width: "300px",
                        marginLeft: "15px",
                        marginBottom: "-10px",
                    }}
                    >
                    {`${t(`AST_SOURCE_OF_FUNDING`)}`}
                    </span>
                </div>
                </div>
                <Controller
                control={control}
                name={"sourceOfFinance"}
                defaultValue={sourceOfFinance}
                rules={{ required: t("CORE_COMMON_REQUIRED_ERRMSG") }}
                render={(props) => (
                    <Dropdown
                    selected={sourceOfFinance}
                    select={setsourceOfFinance}
                    option={sourcefinance}
                    optionKey="i18nKey"
                    placeholder={"Select"}
                    t={t}
                    />
                )}
                />
            </div> 
      {assettype?.code && formJson.filter((e) => e.group === "acquistionDetails")
      .map((row, index) => {
        if (row.conditionalField) {
          const { dependsOn, showWhen } = row.conditionalField;
          if (assetDetails[dependsOn] !== showWhen) {
            return null;
          }
        } 

        return (
          <div key={index}>
            <div>
              {`${t(row.code)}`} {row.isMandatory ? <span style={{ color: "red" }}>*</span> : null}
              <div className="tooltip" style={{ width: "12px", height: "5px", marginLeft: "10px", display: "inline-flex", alignItems: "center" }}>
                <InfoBannerIcon />
                <span className="tooltiptext" style={{ whiteSpace: "pre-wrap", fontSize: "small", wordWrap: "break-word", width: "300px", marginLeft: "15px", marginBottom: "-10px" }}>
                  {`${t(row.code + "_INFO")} `}
                </span>
              </div>
            </div>

            {row.type === "date" ? (
              <TextInput
                t={t}
                type={"date"}
                isMandatory={false}
                optionKey="i18nKey"
                name={row.name}
                value={assetDetails[row.name]}
                onChange={handleInputChange}
                style={{ width: "100%" }}
                // max={new Date().toISOString().split("T")[0]}
                rules={{
                  required: t("CORE_COMMON_REQUIRED_ERRMSG"),
                  validDate: (val) => (/^\d{4}-\d{2}-\d{2}$/.test(val) ? true : t("ERR_DEFAULT_INPUT_FIELD_MSG")),
                }}
              />
            ) : row.type === "dropdown" ? (
              <Controller
                control={control}
                name={row.name}
                isMandatory={row.isMandatory}
                defaultValue={assetDetails[row.name] ? assetDetails[row.name] : ""}
                rules={{ required: t("CORE_COMMON_REQUIRED_ERRMSG") }}
                render={(props) => (
                  <Dropdown
                    // className="form-field"
                    selected={assetDetails[row.code]}
                    select={(value) => handleInputChange({ name: row.name, ...value })}
                    option={row.options}
                    optionKey="i18nKey"
                    placeholder={"Select"}
                    isMandatory={row.isMandatory}
                    t={t}
                  />
                )}
              />
            ) : row.type === "num" ? (
              <TextInput
                t={t}
                type={"number"}
                isMandatory={row.isMandatory}
                optionKey="i18nKey"
                name={row.name}
                value={assetDetails[row.name] || ""}
                onChange={handleInputChange}
                {...(validation = {
                  isRequired: row.isMandatory,
                  pattern: regexPattern(row.columnType),
                  type: row.columnType,
                  title: t("PT_NAME_ERROR_MESSAGE"),
                })}
                style={{ width: "100%" }}
                readOnly={row.isReadOnly}
              />
            ) : row.addCurrentLocationButton === true ? (
              <div style={{ position: "relative" }}>
                <TextInput
                  t={t}
                  type={row.type}
                  isMandatory={row.isMandatory}
                  optionKey="i18nKey"
                  name={row.name}
                  value={assetDetails[row.name] || ""}
                  onChange={handleInputChange}
                  style={{ flex: 1 }}
                  ValidationRequired={false}
                  {...(validation = {
                    isRequired: true,
                    pattern: regexPattern(row.columnType),
                    type: row.columnType,
                    title: t("VALID_LAT_LONG"),
                  })}
                />
                <div
                  className="butt-icon"
                  onClick={() => fetchCurrentLocation(row.name)}
                  style={{
                    position: "absolute",
                    right: "0",
                    top: "50%",
                    transform: "translateY(-50%)",
                    cursor: "pointer",
                    display: "flex",
                    alignItems: "center",
                    padding: "2px 5px",
                  }}
                >
                  <LocationIcon styles={{ width: "16px", border: "none" }} className="fill-path-primary-main" />
                </div>
              </div>
            ) : (
              <TextInput
                t={t}
                type={row.type}
                isMandatory={row.isMandatory}
                optionKey="i18nKey"
                name={row.name}
                value={assetDetails[row.name] || ""}
                onChange={handleInputChange}
                {...(validation = {
                  isRequired: row.isMandatory,
                  pattern: regexPattern(row.columnType),
                  type: row.columnType,
                  title: t("PT_NAME_ERROR_MESSAGE"),
                })}
                style={{ width: "100%" }}
                readOnly={row.isReadOnly}
              />
            )}
          </div>
        );
      })}
    </div>
        </React.Fragment>

        <React.Fragment>
        <CardHeader>{t("AST_IDENTIFICATION_LOCATION")}</CardHeader>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '30px',  border: "1px solid rgb(101 43 43)", borderRadius: '8px', padding: '16px' }}>
        {/* Field 9 - Description */}
            <div>
              <div>
                {t("ASSET_DESCRIPTION")}
                <div className="tooltip" style={{ width: "12px", height: "5px", marginLeft: "10px", display: "inline-flex", alignItems: "center" }}>
                  <InfoBannerIcon />
                  <span
                    className="tooltiptext"
                    style={{
                      whiteSpace: "pre-wrap",
                      fontSize: "small",
                      wordWrap: "break-word",
                      width: "300px",
                      marginLeft: "15px",
                      marginBottom: "-10px",
                    }}
                  >
                    {`${t(`AST_ANY_DESCRIPTION`)}`}
                  </span>
                </div>
              </div>
              <TextInput
                t={t}
                type={"text"}
                isMandatory={false}
                optionKey="i18nKey"
                name="Assetdescription"
                value={Assetdescription}
                onChange={setassetDescription}
                style={{ width: "100%" }}
                ValidationRequired={false}
                {...(validation = {
                  isRequired: true,
                  pattern: "^[a-zA-Z0-9/-]*$",
                  type: "text",
                  title: t("PT_NAME_ERROR_MESSAGE"),
                })}
              />
            </div>


            {/* Field 11 - Usage */}
            <div>
              <div>
                {t("AST_USAGE")} <span style={{ color: "red" }}>*</span>
              </div>
              <Controller
                control={control}
                name={"assetsUsage"}
                defaultValue={assetsUsage}
                rules={{ required: t("CORE_COMMON_REQUIRED_ERRMSG") }}
                render={(props) => (
                  <Dropdown
                    selected={assetsUsage}
                    select={setAssetsUsage}
                    option={assetCurrentUsage}
                    optionKey="i18nKey"
                    placeholder={"Select"}
                    t={t}
                  />
                )}
              />
            </div>
        {assettype?.code === "LAND" && (
                <div>
                <div>
                    {`${t("AST_SURVEY_NUMBER")}`}
                    <div className="tooltip" style={{ width: "12px", height: "5px", marginLeft: "10px", display: "inline-flex", alignItems: "center" }}>
                    <InfoBannerIcon />
                    <span className="tooltiptext" style={{ whiteSpace: "pre-wrap", fontSize: "small", wordWrap: "break-word", width: "300px", marginLeft: "15px", marginBottom: "-10px" }}>
                        {`${t("ASSET_SURVEY_NUMBER")}`}
                    </span>
                    </div>
                </div>
                <TextInput
                    t={t}
                    type={"text"}
                    isMandatory={false}
                    optionKey="i18nKey"
                    name="surveyNumber"
                    value={assetDetails["surveyNumber"]}
                    onChange={handleInputChange}
                    {...(validation = {
                    isRequired: true,
                    pattern: "^[a-zA-Z0-9/-]*$",
                    type: "text",
                    title: t("PT_NAME_ERROR_MESSAGE"),
                    })}
                    style={{ width: "100%" }}
                />
                </div>
            )}
        <div>
          <div>
            {`${t("AST_PINCODE")}`}
          </div>
          <TextInput
            t={t}
            type={"tel"}
            isMandatory={false}
            optionKey="i18nKey"
            name="pincode"
            value={address?.pincode || ""}
            onChange={(e) => {
                setAddress({ ...address, pincode: e.target.value });
            }}
            validation={{
              required: true,
              pattern: "^[0-9]{6}$",
              type: "tel",
              title: t("PINCODE_INVALID"),
            }}
            style={{ width: "100%" }}
            maxLength={6}
          />
        </div>
        <div>
        <div>
        {`${t("SV_CITY")}`} <span className="astericColor">*</span>
        </div>
          <Controller
            control={control}
            name={"city"}
            rules={{ required: t("CORE_COMMON_REQUIRED_ERRMSG") }}
            render={(props) => (
              <Dropdown
                // className="form-field"
                selected={props.value}
                select={(value) => {
                  props.onChange(value);
                  setAddress({ ...address, city: value });
                }}
                option={allCities}
                optionKey="code"
                t={t}
                placeholder={"Select"}
              />
            )}
          />
          </div>
          <div>
          <div>
          {`${t("SV_LOCALITY")}`} <span className="astericColor">*</span>
          </div>

          <Controller
            control={control}
            name={"locality"}
            rules={{ required: t("CORE_COMMON_REQUIRED_ERRMSG") }}
            render={(props) => (
              <Dropdown
                // className="form-field"
                selected={props.value}
                select={(value) => {
                  props.onChange(value);
                  setAddress({ ...address, locality: value });
                }}
                option={structuredLocality}
                optionCardStyles={{ overflowY: "auto", maxHeight: "300px" }}
                optionKey="i18nKey"
                t={t}
                placeholder={"Select"}
              />
            )}
          />
          </div>

        </div>
        </React.Fragment>

        <React.Fragment>
        <CardHeader>{t("AST_PHYSICAL_CHARACTERISTICS_DETAILS")}</CardHeader>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '30px',  border: "1px solid rgb(101 43 43)", borderRadius: '8px', padding: '16px' }}>
      {assettype?.code && formJson.filter((e) => e.group === "physicialCharacteristics")
      .map((row, index) => {
        if (row.conditionalField) {
          const { dependsOn, showWhen } = row.conditionalField;
          if (assetDetails[dependsOn] !== showWhen) {
            return null;
          }
        } 

        return (
          <div key={index}>
            <div>
              {`${t(row.code)}`} {row.isMandatory ? <span style={{ color: "red" }}>*</span> : null}
              <div className="tooltip" style={{ width: "12px", height: "5px", marginLeft: "10px", display: "inline-flex", alignItems: "center" }}>
                <InfoBannerIcon />
                <span className="tooltiptext" style={{ whiteSpace: "pre-wrap", fontSize: "small", wordWrap: "break-word", width: "300px", marginLeft: "15px", marginBottom: "-10px" }}>
                  {`${t(row.code + "_INFO")} `}
                </span>
              </div>
            </div>

            {row.type === "date" ? (
              <TextInput
                t={t}
                type={"date"}
                isMandatory={false}
                optionKey="i18nKey"
                name={row.name}
                value={assetDetails[row.name]}
                onChange={handleInputChange}
                style={{ width: "100%" }}
                // max={new Date().toISOString().split("T")[0]}
                rules={{
                  required: t("CORE_COMMON_REQUIRED_ERRMSG"),
                  validDate: (val) => (/^\d{4}-\d{2}-\d{2}$/.test(val) ? true : t("ERR_DEFAULT_INPUT_FIELD_MSG")),
                }}
              />
            ) : row.type === "dropdown" 
            ? (
              <Controller
                control={control}
                name={row.name}
                isMandatory={row.isMandatory}
                defaultValue={assetDetails[row.name] ? assetDetails[row.name] : ""}
                rules={{ required: t("CORE_COMMON_REQUIRED_ERRMSG") }}
                render={(props) => (
                  <Dropdown
                    // className="form-field"
                    selected={assetDetails[row.code]}
                    select={(value) => handleInputChange({ name: row.name, ...value })}
                    option={row.options}
                    optionKey="i18nKey"
                    placeholder={"Select"}
                    isMandatory={row.isMandatory}
                    t={t}
                  />
                )}
              />
            )  : (
              <TextInput
                t={t}
                type={row.type}
                isMandatory={row.isMandatory}
                optionKey="i18nKey"
                name={row.name}
                value={assetDetails[row.name] || ""}
                onChange={handleInputChange}
                {...(validation = {
                  isRequired: row.isMandatory,
                  pattern: regexPattern(row.columnType),
                  type: row.columnType,
                  title: t("PT_NAME_ERROR_MESSAGE"),
                })}
                style={{ width: "100%" }}
                readOnly={row.isReadOnly}
              />
            )
            }
          </div>
        );
        })}
        </div>
        </React.Fragment>

        <React.Fragment>
        <CardHeader>{t("AST_IMPROVEMENTS_O&M_DETAILS")}</CardHeader>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '30px',  border: "1px solid rgb(101 43 43)", borderRadius: '8px', padding: '16px' }}>
      {assettype?.code && formJson.filter((e) => e.group === "improvementsDetails")
      .map((row, index) => {
        if (row.conditionalField) {
          const { dependsOn, showWhen } = row.conditionalField;
          if (assetDetails[dependsOn] !== showWhen) {
            return null;
          }
        } 

        return (
          <div key={index}>
            <div>
              {`${t(row.code)}`} {row.isMandatory ? <span style={{ color: "red" }}>*</span> : null}
              <div className="tooltip" style={{ width: "12px", height: "5px", marginLeft: "10px", display: "inline-flex", alignItems: "center" }}>
                <InfoBannerIcon />
                <span className="tooltiptext" style={{ whiteSpace: "pre-wrap", fontSize: "small", wordWrap: "break-word", width: "300px", marginLeft: "15px", marginBottom: "-10px" }}>
                  {`${t(row.code + "_INFO")} `}
                </span>
              </div>
            </div>

            {row.type === "date" ? (
              <TextInput
                t={t}
                type={"date"}
                isMandatory={false}
                optionKey="i18nKey"
                name={row.name}
                value={assetDetails[row.name]}
                onChange={handleInputChange}
                style={{ width: "100%" }}
                // max={new Date().toISOString().split("T")[0]}
                rules={{
                  required: t("CORE_COMMON_REQUIRED_ERRMSG"),
                  validDate: (val) => (/^\d{4}-\d{2}-\d{2}$/.test(val) ? true : t("ERR_DEFAULT_INPUT_FIELD_MSG")),
                }}
              />
            ) : row.type === "dropdown" ? (
              <Controller
                control={control}
                name={row.name}
                isMandatory={row.isMandatory}
                defaultValue={assetDetails[row.name] ? assetDetails[row.name] : ""}
                rules={{ required: t("CORE_COMMON_REQUIRED_ERRMSG") }}
                render={(props) => (
                  <Dropdown
                    // className="form-field"
                    selected={assetDetails[row.code]}
                    select={(value) => handleInputChange({ name: row.name, ...value })}
                    option={row.options}
                    optionKey="i18nKey"
                    placeholder={"Select"}
                    isMandatory={row.isMandatory}
                    t={t}
                  />
                )}
              />
            ) : row.type === "num" ? (
              <TextInput
                t={t}
                type={"number"}
                isMandatory={row.isMandatory}
                optionKey="i18nKey"
                name={row.name}
                value={assetDetails[row.name] || ""}
                onChange={handleInputChange}
                {...(validation = {
                  isRequired: row.isMandatory,
                  pattern: regexPattern(row.columnType),
                  type: row.columnType,
                  title: t("PT_NAME_ERROR_MESSAGE"),
                })}
                style={{ width: "100%" }}
                readOnly={row.isReadOnly}
              />
            ) :  (
              <TextInput
                t={t}
                type={row.type}
                isMandatory={row.isMandatory}
                optionKey="i18nKey"
                name={row.name}
                value={assetDetails[row.name] || ""}
                onChange={handleInputChange}
                {...(validation = {
                  isRequired: row.isMandatory,
                  pattern: regexPattern(row.columnType),
                  type: row.columnType,
                  title: t("PT_NAME_ERROR_MESSAGE"),
                })}
                style={{ width: "100%" }}
                readOnly={row.isReadOnly}
              />
            )}
          </div>
        );
        })}
        </div>
        </React.Fragment>
        <React.Fragment>
        <CardHeader>{t("AST_REFERNCE_DETAILS")}</CardHeader>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '30px',  border: "1px solid rgb(101 43 43)", borderRadius: '8px', padding: '16px' }}>
        <div>
              <div>
                {t("AST_FINANCIAL_YEAR")} <span style={{ color: "red" }}>*</span>
                <div className="tooltip" style={{ width: "12px", height: "5px", marginLeft: "10px", display: "inline-flex", alignItems: "center" }}>
                  <InfoBannerIcon />
                  <span
                    className="tooltiptext"
                    style={{
                      whiteSpace: "pre-wrap",
                      fontSize: "small",
                      wordWrap: "break-word",
                      width: "300px",
                      marginLeft: "15px",
                      marginBottom: "-10px",
                    }}
                  >
                    {`${t(`AST_WHICH_FINANCIAL_YEAR`)}`}
                  </span>
                </div>
              </div>
              <Controller
                control={control}
                name={"financialYear"}
                defaultValue={financialYear}
                rules={{ required: t("CORE_COMMON_REQUIRED_ERRMSG") }}
                render={({ field }) => (
                  <Dropdown selected={financialYear} select={setfinancialYear} option={financal} optionKey="i18nKey" placeholder={"Select"} t={t} />
                )}
              />
            </div>

            <div>
              <div>
                {t("AST_BOOK_REF_SERIAL_NUM")} <span style={{ color: "red" }}>*</span>
                <div className="tooltip" style={{ width: "12px", height: "5px", marginLeft: "10px", display: "inline-flex", alignItems: "center" }}>
                  <InfoBannerIcon />
                  <span
                    className="tooltiptext"
                    style={{
                      whiteSpace: "pre-wrap",
                      fontSize: "small",
                      wordWrap: "break-word",
                      width: "300px",
                      marginLeft: "15px",
                      marginBottom: "-10px",
                    }}
                  >
                    {`${t(`AST_BOOK_REF_NUMBER`)}`}
                  </span>
                </div>
              </div>
              <TextInput
                t={t}
                type={"text"}
                isMandatory={false}
                optionKey="i18nKey"
                name="BookPagereference"
                value={BookPagereference}
                onChange={setbookpagereference}
                style={{ width: "100%" }}
                ValidationRequired={false}
                {...(validation = {
                  isRequired: true,
                  pattern: "^[a-zA-Z0-9/-]*$",
                  type: "text",
                  title: t("PT_NAME_ERROR_MESSAGE"),
                })}
              />
            </div>
      {assettype?.code && formJson.filter((e) => e.group === "others")
      .map((row, index) => {
        if (row.conditionalField) {
          const { dependsOn, showWhen } = row.conditionalField;
          if (assetDetails[dependsOn] !== showWhen) {
            return null;
          }
        } 

        return (
          <div key={index}>
            <div>
              {`${t(row.code)}`} {row.isMandatory ? <span style={{ color: "red" }}>*</span> : null}
              <div className="tooltip" style={{ width: "12px", height: "5px", marginLeft: "10px", display: "inline-flex", alignItems: "center" }}>
                <InfoBannerIcon />
                <span className="tooltiptext" style={{ whiteSpace: "pre-wrap", fontSize: "small", wordWrap: "break-word", width: "300px", marginLeft: "15px", marginBottom: "-10px" }}>
                  {`${t(row.code + "_INFO")} `}
                </span>
              </div>
            </div>
              <TextInput
                t={t}
                type={row.type}
                isMandatory={row.isMandatory}
                optionKey="i18nKey"
                name={row.name}
                value={assetDetails[row.name] || ""}
                onChange={handleInputChange}
                {...(validation = {
                  isRequired: row.isMandatory,
                  pattern: regexPattern(row.columnType),
                  type: row.columnType,
                  title: t("PT_NAME_ERROR_MESSAGE"),
                })}
                style={{ width: "100%" }}
                readOnly={row.isReadOnly}
              />
          </div>
        );
        })}
        </div>
        </React.Fragment>
{/* NEW DOCUMENT UPLOAD SECTION */}
        <React.Fragment>
          <CardHeader>{t("AST_DOCUMENT_DETAILS")}</CardHeader>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '30px',  border: "1px solid rgb(101 43 43)", borderRadius: '8px', padding: '16px' }}>

            {documentData?.ASSET?.Documents?.map((document, index) => (
              <DocumentUploadField
                key={index}
                document={document}
                t={t}
                error={documentError}
                setError={setDocumentError}
                setDocuments={setDocuments}
                documents={documents}
                setCheckRequiredFields={setCheckRequiredFields}
                formData={formData}
              />
            ))}
            {documentError && <Toast label={documentError} onClose={() => setDocumentError(null)} error />}
          </div>
        </React.Fragment>
      </FormStep>
    </React.Fragment>

    
  );
};


// NEW COMPONENT: Extracted from NewDocument
function DocumentUploadField({
  t,
  document: doc,
  setDocuments,
  setError,
  documents,
  formData,
}) {
  const filteredDocument = documents?.find((item) => item?.documentType?.includes(doc?.code));

  const [selectedDocument, setSelectedDocument] = useState(
    filteredDocument
      ? { ...filteredDocument, active: doc?.active === true, code: filteredDocument?.documentType }
      : doc?.dropdownData?.length === 1
      ? doc?.dropdownData[0]
      : {}
  );

  const [file, setFile] = useState(null);
  const [uploadedFile, setUploadedFile] = useState(filteredDocument?.fileStoreId || null);
  const [latitude, setLatitude] = useState(formData?.latitude || null);
  const [longitude, setLongitude] = useState(formData?.longitude || null);
  const [isUploading, setIsUploading] = useState(false);

  const LoadingSpinner = () => (
    <div className="loading-spinner" style={{
      border: '2px solid #f3f3f3',
      borderTop: '2px solid #a82227',
      borderRadius: '50%',
      width: '16px',
      height: '16px',
      animation: 'spin 1s linear infinite',
      display: 'inline-block'
    }} />
  );

  const extractGeoLocation = (file) => {
    return new Promise((resolve) => {
      EXIF.getData(file, function () {
        const lat = EXIF.getTag(this, 'GPSLatitude');
        const lon = EXIF.getTag(this, 'GPSLongitude');
        if (lat && lon) {
          const latDecimal = convertToDecimal(lat);
          const lonDecimal = convertToDecimal(lon);
          resolve({ latitude: latDecimal, longitude: lonDecimal });
        } else {
          resolve({ latitude: null, longitude: null });
        }
      });
    });
  };

  const convertToDecimal = (coordinate) => {
    const degrees = coordinate[0];
    const minutes = coordinate[1];
    const seconds = coordinate[2];
    return degrees + minutes / 60 + seconds / 3600;
  };

  const handleFileUpload = (e) => {
    const file = e.target.files[0];
    setFile(file);
    extractGeoLocation(file).then(({ latitude, longitude }) => {
      setLatitude(latitude);
      setLongitude(longitude);
      if (doc?.code === "OWNER.ASSETPHOTO" && (!latitude || !longitude)) {
        setError("Please upload a photo with location details");
      }
    });
  };

  useEffect(() => {
    (async () => {
      setError(null);
      if (file) {
        if (file.size >= 5242880) {
          setError(t("CS_MAXIMUM_UPLOAD_SIZE_EXCEEDED"));
        } else {
          try {
            setUploadedFile(null);
            setIsUploading(true);
            const response = await Digit.UploadServices.Filestorage("ASSET", file, Digit.ULBService.getStateId());
            if (response?.data?.files?.length > 0) {
              setUploadedFile(response?.data?.files[0]?.fileStoreId);
            } else {
              setError(t("CS_FILE_UPLOAD_ERROR"));
            }
          } catch (err) {
            setError(t("CS_FILE_UPLOAD_ERROR"));
          } finally {
            setIsUploading(false);
          }
        }
      }
    })();
  }, [file]);

  useEffect(() => {
    if (selectedDocument?.code) {
      setDocuments((prev) => {
        const filteredDocumentsByDocumentType = prev?.filter((item) => item?.documentType !== selectedDocument?.code);
        if (!uploadedFile) {
          return filteredDocumentsByDocumentType;
        }
        const filteredDocumentsByFileStoreId = filteredDocumentsByDocumentType?.filter((item) => item?.fileStoreId !== uploadedFile);
        return [
          ...filteredDocumentsByFileStoreId,
          {
            documentType: selectedDocument?.code,
            fileStoreId: uploadedFile,
            documentUid: uploadedFile,
            latitude,
            longitude,
          },
        ];
      });
    }
  }, [uploadedFile, selectedDocument, latitude, longitude, setDocuments]);

  return (
    <div style={{ marginBottom: "24px" }}>
      {doc?.hasDropdown && (
        <LabelFieldPair>
          {doc?.code === "OWNER.ASSETPHOTO" ? (
            <div>
              {`${t(doc.code.replaceAll(".", "_"))}`} <span style={{ color: "red" }}>*</span>
              <div
                className="tooltip"
                style={{
                  width: "12px",
                  height: "5px",
                  marginLeft: "10px",
                  display: "inline-flex",
                  alignItems: "center",
                }}
              >
                <InfoBannerIcon />
                <span
                  className="tooltiptext"
                  style={{
                    whiteSpace: "pre-wrap",
                    fontSize: "small",
                    wordWrap: "break-word",
                    width: "300px",
                    marginLeft: "15px",
                    marginBottom: "-10px",
                  }}
                >
                  {`${t(doc.code.replaceAll(".", "_") + "_INFO")}`}
                </span>
              </div>
            </div>
          ) : (
            <CardLabel className="card-label-smaller">
              {t(doc.code.replaceAll(".", "_"))} <span style={{ color: "red" }}>*</span>
            </CardLabel>
          )}
        </LabelFieldPair>
      )}
      <LabelFieldPair>
        {/* <div className="field"> */}
          <UploadFile
            onUpload={handleFileUpload}
            onDelete={() => {
              setUploadedFile(null);
              setLatitude(null);
              setLongitude(null);
            }}
            message={isUploading ? (
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <LoadingSpinner />
                <span>Uploading...</span>
              </div>
            ) : uploadedFile ? "1 File Uploaded" : "No File Uploaded"}
            textStyles={{ width: "100%" }}
            inputStyles={{ width: "280px" }}
            accept=".pdf, .jpeg, .jpg, .png"
            buttonType="button"
            error={!uploadedFile}
          />
        {/* </div> */}
      </LabelFieldPair>
      {doc?.code === "OWNER.ASSETPHOTO" && latitude && longitude && (
        <div style={{ marginTop: '10px', textAlign: 'center' }}>
          <p><strong>{t("Location Details")}:</strong></p>
          <p>{t("Latitude")}: {latitude}</p>
          <p>{t("Longitude")}: {longitude}</p>
        </div>
      )}
    </div>
  );
}

export default AssetAllDetails;
