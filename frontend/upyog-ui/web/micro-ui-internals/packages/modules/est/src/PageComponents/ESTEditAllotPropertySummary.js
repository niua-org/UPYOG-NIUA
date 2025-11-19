import React, { useState } from "react";
import {
  Card,
  CardHeader,
  CardSubHeader,
  TextInput,
  DatePicker,
  UploadFile,
  SubmitBar,
} from "@upyog/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import { useHistory } from "react-router-dom";

const ESTEditAllotPropertySummary = () => {
  const { t } = useTranslation();
  const history = useHistory();

  // 🔹 Load saved form data from session storage
  const savedData = JSON.parse(sessionStorage.getItem("estAssignAssetsForm")) || {};

  const [form, setForm] = useState({
    propertyType: savedData.propertyType || "",
    allotteeName: savedData.allotteeName || "",
    phone: savedData.phone || "",
    altPhone: savedData.altPhone || "",
    email: savedData.email || "",
    startDate: savedData.startDate || "",
    endDate: savedData.endDate || "",
    rate: savedData.rate || "",
    rent: savedData.rent || "",
    advance: savedData.advance || "",
    advanceDate: savedData.advanceDate || "",
    eoffice: savedData.eoffice || "",
    citizenLetter: savedData.citizenLetter || null,
    allotmentLetter: savedData.allotmentLetter || null,
    signedDeed: savedData.signedDeed || null,
  });

  const handleChange = (key, value) => {
    setForm((prev) => ({ ...prev, [key]: value }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    sessionStorage.setItem("estAssignAssetsForm", JSON.stringify(form));
    alert(t("EST_DETAILS_UPDATED_SUCCESSFULLY", "Details Updated Successfully"));
    history.push("/upyog-ui/employee/est/allot-property-summary");
  };

  // 🔹 Styling
  const labelStyle = { marginBottom: "4px", fontWeight: "500", color: "#333" };
  const grid = { display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" };
  const sectionTitle = {
    margin: "20px 0 10px",
    fontSize: "18px",
    fontWeight: "600",
    color: "#333",
  };

  return (
    <div className="digit-card" style={{ padding: "10px" }}>
      <Card>
        <CardHeader>
          {t("EST_EDIT_ALLOT_PROPERTY_SUMMARY", "Edit Allot Property Summary")}
        </CardHeader>

        {/* ✅ Asset Details (Read Only) */}
        <CardSubHeader>{t("EST_ASSET_DETAILS")}</CardSubHeader>
        <div
          style={{
            background: "#f7f7f7",
            padding: "12px",
            borderRadius: "6px",
            marginBottom: "16px",
          }}
        >
          <p>
            <strong>{t("EST_ASSET_NUMBER")}: </strong> AS-1234528B
          </p>
          <p>
            <strong>{t("EST_BUILDING_NAME")}: </strong> ABC Bhawan
          </p>
          <p>
            <strong>{t("EST_LOCALITY")}: </strong> BCD Nagar
          </p>
          <p>
            <strong>{t("EST_TOTAL_AREA")}: </strong> 500 sq.ft.
          </p>
          <p>
            <strong>{t("EST_FLOOR")}: </strong> 02
          </p>
          <p>
            <strong>{t("EST_RATE")}: </strong> 400 sq.ft.
          </p>
        </div>

        {/* ✅ Editable Allottee Details */}
        <CardSubHeader>{t("EST_ALLOTTEE_DETAILS")}</CardSubHeader>
        <div style={grid}>
          <div>
            <label style={labelStyle}>{t("EST_NAME_OF_ALLOTTEE")}</label>
            <TextInput
              value={form.allotteeName}
              onChange={(e) => handleChange("allotteeName", e.target.value)}
              placeholder={t("EST_ENTER_NAME_OF_ALLOTTEE")}
            />
          </div>

          <div>
            <label style={labelStyle}>{t("EST_PHONE_NUMBER")}</label>
            <TextInput
              value={form.phone}
              onChange={(e) => handleChange("phone", e.target.value)}
              placeholder={t("EST_ENTER_PHONE_NUMBER")}
            />
          </div>

          <div>
            <label style={labelStyle}>{t("EST_ALTERNATE_PHONE_NUMBER")}</label>
            <TextInput
              value={form.altPhone}
              onChange={(e) => handleChange("altPhone", e.target.value)}
              placeholder={t("EST_ENTER_ALTERNATE_PHONE")}
            />
          </div>

          <div>
            <label style={labelStyle}>{t("EST_EMAIL_ID")}</label>
            <TextInput
              value={form.email}
              onChange={(e) => handleChange("email", e.target.value)}
              placeholder={t("EST_ENTER_EMAIL_ID")}
            />
          </div>
        </div>

        {/* ✅ Editable Agreement Details */}
        <CardSubHeader>{t("EST_AGREEMENT_DETAILS")}</CardSubHeader>
        <div style={grid}>
          <div>
            <label style={labelStyle}>{t("EST_AGREEMENT_START_DATE")}</label>
            <DatePicker
              date={form.startDate}
              onChange={(d) => handleChange("startDate", d)}
            />
          </div>

          <div>
            <label style={labelStyle}>{t("EST_AGREEMENT_END_DATE")}</label>
            <DatePicker
              date={form.endDate}
              onChange={(d) => handleChange("endDate", d)}
            />
          </div>

          <div>
            <label style={labelStyle}>{t("EST_RATE_PER_SQFT")}</label>
            <TextInput
              type="number"
              value={form.rate}
              onChange={(e) => handleChange("rate", e.target.value)}
              placeholder={t("EST_ENTER_RATE")}
            />
          </div>

          <div>
            <label style={labelStyle}>{t("EST_MONTHLY_RENT_INR")}</label>
            <TextInput
              type="number"
              value={form.rent}
              onChange={(e) => handleChange("rent", e.target.value)}
              placeholder={t("EST_ENTER_MONTHLY_RENT")}
            />
          </div>

          <div>
            <label style={labelStyle}>{t("EST_ADVANCE_PAYMENT_INR")}</label>
            <TextInput
              type="number"
              value={form.advance}
              onChange={(e) => handleChange("advance", e.target.value)}
              placeholder={t("EST_ENTER_ADVANCE_PAYMENT")}
            />
          </div>

          <div>
            <label style={labelStyle}>{t("EST_ADVANCE_PAYMENT_DATE")}</label>
            <DatePicker
              date={form.advanceDate}
              onChange={(d) => handleChange("advanceDate", d)}
            />
          </div>
        </div>

        {/* ✅ Editable Document Uploads */}
        <CardSubHeader>{t("EST_DOCUMENT_UPLOAD")}</CardSubHeader>
        <div style={grid}>
          <div>
            <label style={labelStyle}>{t("EST_E_OFFICE_FILE_NO")}</label>
            <TextInput
              value={form.eoffice}
              onChange={(e) => handleChange("eoffice", e.target.value)}
              placeholder={t("EST_ENTER_E_OFFICE_FILE_NO")}
            />
          </div>

          <div>
            <label style={labelStyle}>{t("EST_CITIZEN_REQUEST_LETTER")}</label>
            <UploadFile
              id="citizenLetter"
              t={t}
              onUpload={(file) => handleChange("citizenLetter", file)}
            />
            {form.citizenLetter && (
              <p style={{ color: "#555", marginTop: "4px" }}>
                {t("EST_UPLOADED_FILE")}: {form.citizenLetter.name || "File selected"}
              </p>
            )}
          </div>

          <div>
            <label style={labelStyle}>{t("EST_ALLOTMENT_LETTER")}</label>
            <UploadFile
              id="allotmentLetter"
              t={t}
              onUpload={(file) => handleChange("allotmentLetter", file)}
            />
            {form.allotmentLetter && (
              <p style={{ color: "#555", marginTop: "4px" }}>
                {t("EST_UPLOADED_FILE")}: {form.allotmentLetter.name || "File selected"}
              </p>
            )}
          </div>

          <div>
            <label style={labelStyle}>{t("EST_SIGNED_DEED")}</label>
            <UploadFile
              id="signedDeed"
              t={t}
              onUpload={(file) => handleChange("signedDeed", file)}
            />
            {form.signedDeed && (
              <p style={{ color: "#555", marginTop: "4px" }}>
                {t("EST_UPLOADED_FILE")}: {form.signedDeed.name || "File selected"}
              </p>
            )}
          </div>
        </div>

        {/* ✅ Submit */}
        <div style={{ marginTop: "20px" }}>
          <SubmitBar
            label={t("EST_SAVE_CHANGES", "Save Changes")}
            onSubmit={handleSubmit}
          />
        </div>
      </Card>
    </div>
  );
};

export default ESTEditAllotPropertySummary;
