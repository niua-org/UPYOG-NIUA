import React, { useState } from "react";
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

const ESTAssignAssets = () => {
  const { t } = useTranslation();

  const [formData, setFormData] = useState({
    assetNumber: "AS-123452BB",
    assetRefNumber: "AS-123452BB",
    propertyType: "",
    allotteeName: "",
    phoneNumber: "",
    altPhoneNumber: "",
    email: "",
    startDate: "",
    endDate: "",
    duration: "",
    rate: "",
    monthlyRent: "",
    advancePayment: "",
    advancePaymentDate: "",
    eOfficeFileNo: "",
    citizenLetter: null,
    allotmentLetter: null,
    signedDeed: null,
  });

  const [errors, setErrors] = useState({});
  const [showToast, setShowToast] = useState(false);

  const propertyTypeOptions = [
    { label: t("EST_PROPERTY_TYPE_RENT"), code: "RENT" },
    { label: t("EST_PROPERTY_TYPE_LEASE"), code: "LEASE" },
  ];

  const handleChange = (field, value) => {
    setFormData({ ...formData, [field]: value });
    setErrors({ ...errors, [field]: "" }); // clear error when user edits
  };

  const calculateDuration = (start, end) => {
    if (!start || !end) return "";
    const diff = new Date(end) - new Date(start);
    const years = diff / (1000 * 60 * 60 * 24 * 365);
    return years.toFixed(1);
  };

  // -------------------- VALIDATION --------------------
  const validate = () => {
    const newErrors = {};

    if (!formData.propertyType) newErrors.propertyType = t("EST_ERROR_PROPERTY_TYPE_REQUIRED");
    if (!formData.allotteeName.trim()) newErrors.allotteeName = t("EST_ERROR_ALLOTTEE_NAME_REQUIRED");

    if (!formData.phoneNumber.trim()) newErrors.phoneNumber = t("EST_ERROR_PHONE_REQUIRED");
    else if (!/^\d{10}$/.test(formData.phoneNumber)) newErrors.phoneNumber = t("EST_ERROR_PHONE_INVALID");

    if (formData.altPhoneNumber && !/^\d{10}$/.test(formData.altPhoneNumber))
      newErrors.altPhoneNumber = t("EST_ERROR_ALT_PHONE_INVALID");

    if (!formData.email.trim()) newErrors.email = t("EST_ERROR_EMAIL_REQUIRED");
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) newErrors.email = t("EST_ERROR_EMAIL_INVALID");

    if (!formData.startDate) newErrors.startDate = t("EST_ERROR_START_DATE_REQUIRED");
    if (!formData.endDate) newErrors.endDate = t("EST_ERROR_END_DATE_REQUIRED");

    if (!formData.rate.trim()) newErrors.rate = t("EST_ERROR_RATE_REQUIRED");
    else if (isNaN(formData.rate)) newErrors.rate = t("EST_ERROR_RATE_NUMERIC");

    if (!formData.monthlyRent.trim()) newErrors.monthlyRent = t("EST_ERROR_RENT_REQUIRED");
    else if (isNaN(formData.monthlyRent)) newErrors.monthlyRent = t("EST_ERROR_RENT_NUMERIC");

    if (!formData.advancePayment.trim()) newErrors.advancePayment = t("EST_ERROR_ADVANCE_REQUIRED");
    else if (isNaN(formData.advancePayment)) newErrors.advancePayment = t("EST_ERROR_ADVANCE_NUMERIC");

    if (!formData.advancePaymentDate) newErrors.advancePaymentDate = t("EST_ERROR_ADVANCE_DATE_REQUIRED");

    if (!formData.eOfficeFileNo.trim()) newErrors.eOfficeFileNo = t("EST_ERROR_EOFFICE_REQUIRED");
    else if (isNaN(formData.eOfficeFileNo)) newErrors.eOfficeFileNo = t("EST_ERROR_EOFFICE_NUMERIC");

    if (!formData.citizenLetter) newErrors.citizenLetter = t("EST_ERROR_CITIZEN_LETTER_REQUIRED");
    if (!formData.allotmentLetter) newErrors.allotmentLetter = t("EST_ERROR_ALLOTMENT_LETTER_REQUIRED");
    if (!formData.signedDeed) newErrors.signedDeed = t("EST_ERROR_SIGNED_DEED_REQUIRED");

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  // -------------------- SUBMIT --------------------
  const handleSubmit = (e) => {
    e.preventDefault();
    if (!validate()) return;
    console.log("✅ Form Submitted:", formData);
    setShowToast(true);
    setTimeout(() => setShowToast(false), 2000);
  };

  // -------------------- UI --------------------
  return (
    <React.Fragment>
      <Header>{t("EST_COMMMON_ASSIGN_ASSETS")}</Header>

      <Card style={{ padding: "24px 32px" }}>
        <form onSubmit={handleSubmit}>
          {/* -------- Asset Info -------- */}
          <div style={{ marginBottom: "16px" }}>
            <CardLabel>{t("EST_ASSET_NUMBER")}</CardLabel>
            <TextInput value={formData.assetNumber} readOnly />
          </div>

          {/* Auto Populated Info Box */}
          <div
            style={{
              border: "1px solid #ccc",
              borderRadius: "6px",
              padding: "12px",
              marginBottom: "16px",
              background: "#f8f8f8",
            }}
          >
            <p>
              <b>{t("EST_BUILDING_NAME")}:</b> ABC Bhawan
            </p>
            <p>
              <b>{t("EST_LOCALITY")}:</b> BCD Nagar{" "}
              <span style={{ color: "red" }}>*{t("EST_AUTO_POPULATED")}*</span>
            </p>
            <p>
              <b>{t("EST_TOTAL_AREA")}:</b> 500 sq. ft.
            </p>
            <p>
              <b>{t("EST_FLOOR")}:</b> 02
            </p>
            <p>
              <b>{t("EST_RATE")}:</b> 400/ sq. ft.
            </p>
          </div>

          {/* Asset Reference */}
          <div style={{ marginBottom: "16px" }}>
            <CardLabel>{t("EST_ASSET_REFERENCE_NUMBER")}</CardLabel>
            <TextInput
              value={formData.assetRefNumber}
              onChange={(e) => handleChange("assetRefNumber", e.target.value)}
            />
          </div>

          {/* -------- Personal Details -------- */}
          <h3 style={{ marginTop: "20px", color: "#333" }}>
            {t("EST_PERSONAL_DETAILS_OF_ALLOTTEE")}
          </h3>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" }}>
            <div>
              <CardLabel>{t("EST_PROPERTY_TYPE")}</CardLabel>
              <Dropdown
                option={propertyTypeOptions}
                optionKey="label"
                t={t}
                selected={propertyTypeOptions.find((opt) => opt.code === formData.propertyType)}
                select={(opt) => handleChange("propertyType", opt.code)}
                placeholder={t("EST_SELECT_PROPERTY_TYPE")}
                error={errors.propertyType}
              />
            </div>

            <div>
              <CardLabel>{t("EST_ALLOTTEE_NAME")}</CardLabel>
              <TextInput
                placeholder={t("EST_ENTER_ALLOTTEE_NAME")}
                value={formData.allotteeName}
                onChange={(e) => handleChange("allotteeName", e.target.value)}
                error={errors.allotteeName}
              />
            </div>

            <div>
              <CardLabel>{t("EST_PHONE_NUMBER")}</CardLabel>
              <TextInput
                placeholder={t("EST_ENTER_PHONE_NUMBER")}
                value={formData.phoneNumber}
                onChange={(e) => handleChange("phoneNumber", e.target.value)}
                error={errors.phoneNumber}
              />
            </div>

            <div>
              <CardLabel>{t("EST_ALTERNATE_PHONE_NUMBER")}</CardLabel>
              <TextInput
                placeholder={t("EST_ENTER_ALTERNATE_PHONE_NUMBER")}
                value={formData.altPhoneNumber}
                onChange={(e) => handleChange("altPhoneNumber", e.target.value)}
                error={errors.altPhoneNumber}
              />
            </div>

            <div style={{ gridColumn: "span 2" }}>
              <CardLabel>{t("EST_EMAIL_ID")}</CardLabel>
              <TextInput
                placeholder={t("EST_ENTER_EMAIL_ID")}
                value={formData.email}
                onChange={(e) => handleChange("email", e.target.value)}
                error={errors.email}
              />
            </div>
          </div>

          {/* -------- Allotment & Invoice Details -------- */}
          <h3 style={{ marginTop: "24px", color: "#333" }}>
            {t("EST_ALLOTMENT_INVOICE_DETAILS")}
          </h3>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" }}>
            <div>
              <CardLabel>{t("EST_AGREEMENT_START_DATE")}</CardLabel>
              <DatePicker
                date={formData.startDate}
                onChange={(date) => {
                  handleChange("startDate", date);
                  handleChange("duration", calculateDuration(date, formData.endDate));
                }}
                error={errors.startDate}
              />
            </div>

            <div>
              <CardLabel>{t("EST_AGREEMENT_END_DATE")}</CardLabel>
              <DatePicker
                date={formData.endDate}
                onChange={(date) => {
                  handleChange("endDate", date);
                  handleChange("duration", calculateDuration(formData.startDate, date));
                }}
                error={errors.endDate}
              />
            </div>

            <div>
              <CardLabel>{t("EST_DURATION_IN_YEARS")}</CardLabel>
              <TextInput
                value={formData.duration}
                placeholder={t("EST_AUTO_POPULATE_DATES")}
                readOnly
              />
            </div>

            <div>
              <CardLabel>{t("EST_RATE_PER_SQFT")}</CardLabel>
              <TextInput
                value={formData.rate}
                onChange={(e) => handleChange("rate", e.target.value)}
                error={errors.rate}
              />
            </div>

            <div>
              <CardLabel>{t("EST_MONTHLY_RENT_IN_INR")}</CardLabel>
              <TextInput
                value={formData.monthlyRent}
                onChange={(e) => handleChange("monthlyRent", e.target.value)}
                error={errors.monthlyRent}
              />
            </div>

            <div>
              <CardLabel>{t("EST_ADVANCE_PAYMENT_IN_INR")}</CardLabel>
              <TextInput
                value={formData.advancePayment}
                onChange={(e) => handleChange("advancePayment", e.target.value)}
                error={errors.advancePayment}
              />
            </div>

            <div style={{ gridColumn: "span 2" }}>
              <CardLabel>{t("EST_ADVANCE_PAYMENT_DATE")}</CardLabel>
              <DatePicker
                date={formData.advancePaymentDate}
                onChange={(date) => handleChange("advancePaymentDate", date)}
                error={errors.advancePaymentDate}
              />
            </div>
          </div>

          {/* -------- Document Upload -------- */}
          <h3 style={{ marginTop: "24px", color: "#333" }}>{t("EST_DOCUMENT_UPLOAD")}</h3>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" }}>
            <div>
              <CardLabel>{t("EST_EOFFICE_FILE_NO")}</CardLabel>
              <TextInput
                placeholder={t("EST_NUMERIC")}
                value={formData.eOfficeFileNo}
                onChange={(e) => handleChange("eOfficeFileNo", e.target.value)}
                error={errors.eOfficeFileNo}
              />
            </div>

            <div>
              <CardLabel>{t("EST_CITIZEN_REQUEST_LETTER")}</CardLabel>
              <UploadFile
                onUpload={(file) => handleChange("citizenLetter", file)}
                accept="application/pdf,image/*"
                error={errors.citizenLetter}
              />
            </div>

            <div>
              <CardLabel>{t("EST_ALLOTMENT_LETTER")}</CardLabel>
              <UploadFile
                onUpload={(file) => handleChange("allotmentLetter", file)}
                accept="application/pdf,image/*"
                error={errors.allotmentLetter}
              />
            </div>

            <div>
              <CardLabel>{t("EST_SIGNED_DEED")}</CardLabel>
              <UploadFile
                onUpload={(file) => handleChange("signedDeed", file)}
                accept="application/pdf,image/*"
                error={errors.signedDeed}
              />
            </div>
          </div>

          {/* Submit */}
          <div style={{ textAlign: "center", marginTop: "32px" }}>
            <SubmitBar label={t("Submit")} submit />
          </div>
        </form>
      </Card>

      {showToast && (
        <Toast
          label={t("EST_FORM_SUBMIT_SUCCESS")}
          onClose={() => setShowToast(false)}
          type="success"
        />
      )}
    </React.Fragment>
  );
};

export default ESTAssignAssets;
