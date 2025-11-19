const capitalize = (text) => text.substr(0, 1).toUpperCase() + text.substr(1);
const ulbCamel = (ulb) => ulb.toLowerCase().split(" ").map(capitalize).join(" ");

const getESTAcknowledgementData = async (application, tenantInfo, t) => {
  const filesArray = application?.documents?.map((value) => value?.fileStoreId);
  const res = filesArray?.length > 0 && (await Digit.UploadServices.Filefetch(filesArray, Digit.ULBService.getStateId()));

  // function to filter out the fields which have values
  const filterEmptyValues = (values) => values.filter(item => item.value);

  return {
    t: t,
    tenantId: tenantInfo?.code,
    name: `${t(tenantInfo?.i18nKey)} ${ulbCamel(t(`ULBGRADE_${tenantInfo?.city?.ulbGrade.toUpperCase().replace(" ", "_").replace(".", "_")}`))}`,
    email: tenantInfo?.emailId,
    phoneNumber: tenantInfo?.contactNumber,
    applicationNumber: application?.applicationNo,
    isTOCRequired: false,
    heading: t("EST_ACKNOWLEDGEMENT"),
    details: [

       {
        title: t("EST_ASSET_NEW_REGISTRATION_DETAILS"),
        asSectionHeader: true,
        values: filterEmptyValues([

          { title: t("EST_ADDRESS_LINE1"),value: application?.addressDetails[0]?.addressLine1 },
          { title: t("EST_ADDRESS_LINE2"), value: application?.addressDetails[0]?.addressLine1 },
          { title: t("EST_LOCALITY"), value: application?.addressDetails[0]?.locality?.name },
          { title: t("EST_PINCODE"), value: application?.addressDetails[0]?.pincode },
          { title: t("EST_ASSET_TYPE"), value: t(`EST_ASSETTYPE_${application?.assetType?.toUpperCase().replace(" ", "_")}`) },
          { title: t("EST_ASSET_SUBTYPE"), value: t(`EST_ASSETSUBTYPE_${application?.assetSubType?.toUpperCase().replace(" ", "_")}`) },
          { title: t("EST_NO_OF_UNITS"), value: application?.noOfUnits },
          { title: t("EST_TOTAL_AREA_SQFT"), value: application?.totalAreaSqFt },
          { title: t("EST_PREFERRED_ALLOTMENT_TYPE"), value: t(`EST_ALLOTMENTTYPE_${application?.preferredAllotmentType?.toUpperCase().replace(" ", "_")}`) },          
          {title: t("EST_ADDITIONAL_DETAILS"), value: application?.additionalDetails },
       
        ]),
      },


    ],
  };
};

export default getESTAcknowledgementData;