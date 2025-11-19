import React, { useState, useEffect } from "react";
import {
  Header,
  Card,
  CardLabel,
  TextInput,
  Dropdown,
  SubmitBar,
  Toast,
  UploadFile,
  DatePicker,
} from "@upyog/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import { useHistory } from "react-router-dom";

const ESTAssignAssets = ({ t: propT, onSelect, formData = {}, config, payload }) => {
  const history = useHistory();
  const { t } = useTranslation();
  const tenantId = Digit.ULBService.getStateId();

  

const [data, setFormData] = useState({
  assetNo: formData?.assetData?.estateNo || "",
  assetRefNumber: formData?.assetData?.refAssetNo || "",
  propertyType: formData?.AllotmentData?.propertyType || "",
  allotteeName: formData?.AllotmentData?.allotteeName || "",
  phoneNumber: formData?.AllotmentData?.phoneNumber || "",
  altPhoneNumber: formData?.AllotmentData?.altPhoneNumber || "",
  email: formData?.AllotmentData?.email || "",
  startDate: formData?.AllotmentData?.startDate ? new Date(formData.AllotmentData.startDate) : null,
  endDate: formData?.AllotmentData?.endDate ? new Date(formData.AllotmentData.endDate) : null,
  duration: formData?.AllotmentData?.duration || "",
  rate: formData?.AllotmentData?.rate || "",
  monthlyRent: formData?.AllotmentData?.monthlyRent || "",
  advancePayment: formData?.AllotmentData?.advancePayment || "",
  advancePaymentDate: formData?.AllotmentData?.advancePaymentDate ? new Date(formData.AllotmentData.advancePaymentDate) : null,
  eOfficeFileNo: formData?.AllotmentData?.eOfficeFileNo || "",
  citizenLetter: formData?.AllotmentData?.citizenLetter || null,
  allotmentLetter: formData?.AllotmentData?.allotmentLetter || null,
  signedDeed: formData?.AllotmentData?.signedDeed || null,
});

console.log("Form state data:", data);
console.log("assetNo in form:", data.assetNo);

  const [errors, setErrors] = useState({});
  const [showToast, setShowToast] = useState(false);
  const [toastMessage, setToastMessage] = useState("");
  const [toastError, setToastError] = useState(false);

  const propertyTypeOptions = [
    { label: t("EST_PROPERTY_TYPE_RENT"), code: "RENT" },
    { label: t("EST_PROPERTY_TYPE_LEASE"), code: "LEASE" },
  ];

  const handleChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    setErrors((prev) => ({ ...prev, [field]: "" }));
  };

  // ✅ Duration calculation between startDate and endDate
  const calculateDuration = (start, end) => {
    if (!start || !end) return "";
    const startDate = new Date(start);
    const endDate = new Date(end);
    let years = endDate.getFullYear() - startDate.getFullYear();
    let months = endDate.getMonth() - startDate.getMonth();
    if (months < 0) {
      years--;
      months += 12;
    }
    let result = "";
    if (years > 0) result += `${years} year${years > 1 ? "s" : ""}`;
    if (months > 0) result += `${years > 0 ? " " : ""}${months} month${months > 1 ? "s" : ""}`;
    return result || "0 months";
  };

  // ✅ File Upload
  const handleFileUpload = async (file, fieldName) => {
    if (!file) return;
    if (file.size >= 5242880) {
      setToastError(true);
      setToastMessage(t("CS_MAXIMUM_UPLOAD_SIZE_EXCEEDED"));
      setShowToast(true);
      return;
    }
    try {
      const response = await Digit.UploadServices.Filestorage("ESTATE", file, tenantId);
      if (response?.data?.files?.length > 0) {
        const fileStoreId = response.data.files[0].fileStoreId;
        const uploadedDoc = {
          filestoreId: fileStoreId,
          documentuuid: fileStoreId,
          documentType: fieldName,
        };
        handleChange(fieldName, uploadedDoc);
      } else throw new Error("Upload failed");
    } catch (err) {
      setToastError(true);
      setToastMessage(t("CS_FILE_UPLOAD_ERROR"));
      setShowToast(true);
    }
  };

  const handleFileDelete = (fieldName) => handleChange(fieldName, null);

  // ✅ Validation
  const validate = () => {
    const newErrors = {};
    if (!data.propertyType) newErrors.propertyType = t("EST_ERROR_PROPERTY_TYPE_REQUIRED");
    if (!data.allotteeName.trim()) newErrors.allotteeName = t("EST_ERROR_ALLOTTEE_NAME_REQUIRED");
    if (!data.phoneNumber.trim()) newErrors.phoneNumber = t("EST_ERROR_PHONE_REQUIRED");
    else if (!/^\d{10}$/.test(data.phoneNumber)) newErrors.phoneNumber = t("EST_ERROR_PHONE_INVALID");
    if (data.altPhoneNumber && !/^\d{10}$/.test(data.altPhoneNumber))
      newErrors.altPhoneNumber = t("EST_ERROR_ALT_PHONE_INVALID");
    if (!data.email.trim()) newErrors.email = t("EST_ERROR_EMAIL_REQUIRED");
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(data.email)) newErrors.email = t("EST_ERROR_EMAIL_INVALID");
    if (!data.startDate) newErrors.startDate = t("EST_ERROR_START_DATE_REQUIRED");
    if (!data.endDate) newErrors.endDate = t("EST_ERROR_END_DATE_REQUIRED");
    if (!data.rate.trim()) newErrors.rate = t("EST_ERROR_RATE_REQUIRED");
    if (!data.monthlyRent.trim()) newErrors.monthlyRent = t("EST_ERROR_RENT_REQUIRED");
    if (!data.advancePayment.trim()) newErrors.advancePayment = t("EST_ERROR_ADVANCE_REQUIRED");
    if (!data.advancePaymentDate) newErrors.advancePaymentDate = t("EST_ERROR_ADVANCE_DATE_REQUIRED");
    if (!data.eOfficeFileNo.trim()) newErrors.eOfficeFileNo = t("EST_ERROR_EOFFICE_REQUIRED");
    if (!data.citizenLetter) newErrors.citizenLetter = t("EST_ERROR_CITIZEN_LETTER_REQUIRED");
    if (!data.allotmentLetter) newErrors.allotmentLetter = t("EST_ERROR_ALLOTMENT_LETTER_REQUIRED");
    if (!data.signedDeed) newErrors.signedDeed = t("EST_ERROR_SIGNED_DEED_REQUIRED");
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // ✅ Submit (Save & Next)
  const handleSaveNext = (e) => {
    e.preventDefault();
    if (validate()) {
      console.log("config?.key:", config?.key);
      onSelect(config?.key, { AllotmentData: data }, false);
      setToastMessage(t("EST_FORM_SUBMIT_SUCCESS"));
      setToastError(false);
      setShowToast(true);
    } else {
      setToastMessage(t("EST_PLEASE_FILL_REQUIRED_FIELDS"));
      setToastError(true);
      setShowToast(true);
    }
  };

  return (
    <React.Fragment>
      <div>
        <Header>{t("EST_COMMMON_ASSIGN_ASSETS")}</Header>
        <Card style={{ padding: "24px 32px" }}>
          <form onSubmit={handleSaveNext}>
            {/* ---------- Asset Info ---------- */}
            <h1 style={{color: "#333", marginBottom: "16px" }}>{t("EST_ASSET_DETAILS_")}</h1>
            <div style={{ display:"grid", gridTemplateColumns: "1fr 1fr", gap: "16px", marginBottom: "24px" }}>
              <div>
              <CardLabel>{t("EST_ASSET_NUMBER")}</CardLabel>
              <TextInput value={data.assetNo} readOnly />
            </div>

            <div>
              <CardLabel>{t("EST_ASSET_REFERENCE_NUMBER")}</CardLabel>
              <TextInput value={data.assetRefNumber} readOnly />
            </div>

            <div>
              <CardLabel>{t("EST_BUILDING_NAME")}</CardLabel>
              <TextInput value={formData?.assetData?.buildingName || ""} readOnly />
            </div>

            <div>
              <CardLabel>{t("EST_LOCALITY")}</CardLabel>
              <TextInput value={formData?.assetData?.locality || ""} readOnly />
            </div>

            <div>
              <CardLabel>{t("EST_TOTAL_AREA")}</CardLabel>
              <TextInput value={`${formData?.assetData?.totalFloorArea || ""} sq. ft.`} readOnly />
            </div>

            <div>
              <CardLabel>{t("EST_FLOOR")}</CardLabel>
              <TextInput value={formData?.assetData?.floor || ""} readOnly />
            </div>

            <div>
              <CardLabel>{t("EST_RATE")}</CardLabel>
              <TextInput value={`${formData?.assetData?.rate || ""}/ sq. ft.`} readOnly />
            </div>
            </div>

            {/* ---------- Personal Details ---------- */}
            <h1 style={{ marginTop: "20px", color: "#333", marginBottom: "16px" }}>{t("EST_PERSONAL_DETAILS_OF_ALLOTTEE")}</h1>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" }}>
              <div>
                <CardLabel>{t("EST_PROPERTY_TYPE")}</CardLabel>
                <Dropdown
                  option={propertyTypeOptions}
                  optionKey="label"
                  t={t}
                  selected={propertyTypeOptions.find((opt) => opt.code === data.propertyType)}
                  select={(opt) => handleChange("propertyType", opt.code)}
                  placeholder={t("EST_SELECT_PROPERTY_TYPE")}
                  error={errors.propertyType}
                />
              </div>

              <div>
                <CardLabel>{t("EST_ALLOTTEE_NAME")}</CardLabel>
                <TextInput
                  placeholder={t("EST_ENTER_ALLOTTEE_NAME")}
                  value={data.allotteeName}
                  onChange={(e) => handleChange("allotteeName", e.target.value)}
                  error={errors.allotteeName}
                />
              </div>

              <div>
                <CardLabel>{t("EST_PHONE_NUMBER")}</CardLabel>
                <TextInput
                  placeholder={t("EST_ENTER_PHONE_NUMBER")}
                  value={data.phoneNumber}
                  onChange={(e) => handleChange("phoneNumber", e.target.value)}
                  error={errors.phoneNumber}
                />
              </div>

              <div>
                <CardLabel>{t("EST_ALTERNATE_PHONE_NUMBER")}</CardLabel>
                <TextInput
                  placeholder={t("EST_ENTER_ALTERNATE_PHONE_NUMBER")}
                  value={data.altPhoneNumber}
                  onChange={(e) => handleChange("altPhoneNumber", e.target.value)}
                  error={errors.altPhoneNumber}
                />
              </div>

              <div style={{ gridColumn: "span 2" }}>
                <CardLabel>{t("EST_EMAIL_ID")}</CardLabel>
                <TextInput
                  placeholder={t("EST_ENTER_EMAIL_ID")}
                  value={data.email}
                  onChange={(e) => handleChange("email", e.target.value)}
                  error={errors.email}
                />
              </div>
            </div>

            {/* ---------- Allotment Details ---------- */}
            <h1 style={{ marginTop: "24px", color: "#333", marginBottom: "16px" }}>{t("EST_ALLOTMENT_INVOICE_DETAILS")}</h1>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" }}>
              <div>
                <CardLabel>{t("EST_AGREEMENT_START_DATE")}</CardLabel>
                <DatePicker
                  date={data.startDate}
                  onChange={(date) => {
                    handleChange("startDate", date);
                    handleChange("duration", calculateDuration(date, data.endDate));
                  }}
                  error={errors.startDate}
                />
              </div>

              <div>
                <CardLabel>{t("EST_AGREEMENT_END_DATE")}</CardLabel>
                <DatePicker
                  date={data.endDate}
                  onChange={(date) => {
                    handleChange("endDate", date);
                    handleChange("duration", calculateDuration(data.startDate, date));
                  }}
                  error={errors.endDate}
                />
              </div>

              <div>
                <CardLabel>{t("EST_DURATION_IN_YEARS")}</CardLabel>
                <TextInput value={data.duration} readOnly />
              </div>

              <div>
                <CardLabel>{t("EST_RATE_PER_SQFT")}</CardLabel>
                <TextInput
                  value={data.rate}
                  onChange={(e) => handleChange("rate", e.target.value)}
                  error={errors.rate}
                />
              </div>

              <div>
                <CardLabel>{t("EST_MONTHLY_RENT_IN_INR")}</CardLabel>
                <TextInput
                  value={data.monthlyRent}
                  onChange={(e) => handleChange("monthlyRent", e.target.value)}
                  error={errors.monthlyRent}
                />
              </div>

              <div>
                <CardLabel>{t("EST_ADVANCE_PAYMENT_IN_INR")}</CardLabel>
                <TextInput
                  value={data.advancePayment}
                  onChange={(e) => handleChange("advancePayment", e.target.value)}
                  error={errors.advancePayment}
                />
              </div>

              <div style={{ gridColumn: "span 2" }}>
                <CardLabel>{t("EST_ADVANCE_PAYMENT_DATE")}</CardLabel>
                <DatePicker
                  date={data.advancePaymentDate}
                  onChange={(date) => handleChange("advancePaymentDate", date)}
                  error={errors.advancePaymentDate}
                />
              </div>
            </div>

            {/* ---------- Document Upload ---------- */}
            <h1 style={{ marginTop: "24px", color: "#333", marginBottom: "16px"}}>{t("EST_DOCUMENT_UPLOAD")}</h1>
            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" }}>
              <div>
                <CardLabel>{t("EST_EOFFICE_FILE_NO")}</CardLabel>
                <TextInput
                  placeholder={t("EST_ENTER_EOFFICE_FILE_NO")}
                  value={data.eOfficeFileNo}
                  onChange={(e) => handleChange("eOfficeFileNo", e.target.value)}
                  error={errors.eOfficeFileNo}
                />
              </div>

              <div>
                <CardLabel>{t("EST_CITIZEN_REQUEST_LETTER")}</CardLabel>
                <UploadFile
                  onUpload={(e) => handleFileUpload(e.target.files[0], "citizenLetter")}
                  onDelete={() => handleFileDelete("citizenLetter")}
                  id="citizenLetter"
                  message={data.citizenLetter ? t("CS_ACTION_FILEUPLOADED") : t("CS_ACTION_NO_FILEUPLOADED")}
                  accept=".png,.jpg,.jpeg,.pdf"
                  error={errors.citizenLetter}
                />
              </div>

              <div>
                <CardLabel>{t("EST_ALLOTMENT_LETTER")}</CardLabel>
                <UploadFile
                  onUpload={(e) => handleFileUpload(e.target.files[0], "allotmentLetter")}
                  onDelete={() => handleFileDelete("allotmentLetter")}
                  id="allotmentLetter"
                  message={data.allotmentLetter ? t("CS_ACTION_FILEUPLOADED") : t("CS_ACTION_NO_FILEUPLOADED")}
                  accept=".png,.jpg,.jpeg,.pdf"
                  error={errors.allotmentLetter}
                />
              </div>

              <div>
                <CardLabel>{t("EST_SIGNED_DEED")}</CardLabel>
                <UploadFile
                  onUpload={(e) => handleFileUpload(e.target.files[0], "signedDeed")}
                  onDelete={() => handleFileDelete("signedDeed")}
                  id="signedDeed"
                  message={data.signedDeed ? t("CS_ACTION_FILEUPLOADED") : t("CS_ACTION_NO_FILEUPLOADED")}
                  accept=".png,.jpg,.jpeg,.pdf"
                  error={errors.signedDeed}
                />
              </div>
            </div>

            {/* ---------- Save & Next ---------- */}
            <div style={{ display: "flex", justifyContent: "center", marginTop: "32px" }}>
              <SubmitBar label={t("SAVE_&_NEXT")} submit />
            </div>
          </form>
        </Card>

        {showToast && (
          <Toast label={t(toastMessage)} onClose={() => setShowToast(false)} error={toastError} />
        )}
      </div>
    </React.Fragment>
  );
};

export default ESTAssignAssets;