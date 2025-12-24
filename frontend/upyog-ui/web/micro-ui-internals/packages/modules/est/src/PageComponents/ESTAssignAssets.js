import React, { useState } from "react";
import {
  Header,
  Card,
  CardLabel,
  TextInput,
  Dropdown,
  Toast,
  UploadFile,
  DatePicker,
  FormStep,
  SubmitBar,
} from "@upyog/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import { calculateDuration } from "../utils";

const ESTAssignAssets = ({ t: propT, onSelect, onSkip, formData = {}, config }) => {
  const { t: hookT } = useTranslation();
  const t = propT || hookT;
  const tenantId = Digit.ULBService.getStateId();

  const toEpoch = (value) => {
    if (!value) return null;
    const d = value instanceof Date ? value : new Date(value);
    return isNaN(d.getTime()) ? null : d.getTime();
  };

  // helper to sanitize input (digits-only or general regex removal) and enforce max length
  const sanitizeAndSet = (field, value, { maxLength = null, regex = null } = {}) => {
    let v = value ?? "";
    if (typeof v !== "string") v = String(v);
    if (regex) v = v.replace(regex, "");
    if (maxLength && v.length > maxLength) v = v.slice(0, maxLength);
    setData((prev) => ({ ...prev, [field]: v }));
  };

  // state (kept key names similar to original)
  const [data, setData] = useState({
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
    advancePaymentDate: formData?.AllotmentData?.advancePaymentDate
      ? new Date(formData.AllotmentData.advancePaymentDate)
      : null,
    eOfficeFileNo: formData?.AllotmentData?.eOfficeFileNo || "",
    citizenLetter: formData?.AllotmentData?.citizenLetter || null,
    allotmentLetter: formData?.AllotmentData?.allotmentLetter || null,
    signedDeed: formData?.AllotmentData?.signedDeed || null,
  });

  const { data: Asset_Type } = Digit.Hooks.useEnabledMDMS(
    tenantId,
    "ASSET",
    [{ name: "assetParentCategory" }],
    {
      select: (data) => {
        const formatted = data?.ASSET?.assetParentCategory || [];
        return formatted
          .filter((item) => item.active)
          .map((i) => ({
            i18nKey: `ASSET_TYPE_${i.code}`,
            code: i.code,
            label: i.name,
          }));
      },
    }
  );

  const [showToast, setShowToast] = useState(false);
  const [toastMessage, setToastMessage] = useState("");
  const [toastError, setToastError] = useState(false);

  const handleChange = (field, value) => {
    setData((prev) => ({ ...prev, [field]: value }));
  };

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
      const id = response?.data?.files?.[0]?.fileStoreId;

      if (id) {
        handleChange(fieldName, {
          filestoreId: id,
          documentuuid: id,
          documentType: fieldName,
        });
      }
    } catch {
      setToastError(true);
      setToastMessage(t("CS_FILE_UPLOAD_ERROR"));
      setShowToast(true);
    }
  };

  const handleFileDelete = (fieldName) => handleChange(fieldName, null);

  // ---------- Validations ----------
  const phoneValidation = {
    required: true,
    pattern: "^[0-9]{10}$",
    title: t("EST_INVALID_PHONE_NUMBER"),
  };

  const emailValidation = {
    required: true,
    pattern: "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
    title: t("EST_INVALID_EMAIL_ID"),
  };

  const numberValidation = {
    required: true,
    pattern: "^[0-9]+(\\.[0-9]{1,2})?$",
    title: t("EST_INVALID_AMOUNT"),
  };

  const isFormInvalid =
    !data.propertyType ||
    !data.allotteeName ||
    !data.phoneNumber ||
    !data.email ||
    !data.startDate ||
    !data.endDate ||
    !data.rate ||
    !data.monthlyRent ||
    !data.advancePayment;

  const goNext = () => {
    const prepared = {
      ...data,
      startDate: toEpoch(data.startDate),
      endDate: toEpoch(data.endDate),
      advancePaymentDate: toEpoch(data.advancePaymentDate),
      rate: Number(data.rate),
      monthlyRent: Number(data.monthlyRent),
      advancePayment: Number(data.advancePayment),
    };

    onSelect(config?.key, { AllotmentData: prepared }, false);

    setToastMessage(t("EST_FORM_SUBMIT_SUCCESS"));
    setToastError(false);
    setShowToast(true);
  };

  // Reusable required label like in NewRegistration
  const RequiredLabel = ({ label, unit }) => (
    <CardLabel>
      {t(label)} {unit && <span style={{ fontSize: "0.9em", marginLeft: "6px" }}>{unit}</span>}{" "}
      <span style={{ color: "red" }}>*</span>
    </CardLabel>
  );

  // inputs now 70% width (to match NewRegistration)
  const fullWidthStyle = { width: "70%", marginBottom: "16px" };

  return (
    <FormStep t={t} config={config} onSelect={goNext} onSkip={onSkip} isDisabled={isFormInvalid}>
      <Header>{t("EST_COMMMON_ASSIGN_ASSETS")}</Header>

      <Card style={{ padding: "16px" }}>
        {/* ---------- Asset Info ---------- */}
        <h2 style={{ color: "#333", marginBottom: "16px", fontSize: "20px", fontWeight: "bold" }}>
          {t("EST_ASSET_DETAILS")}
        </h2>

        {/* Asset Number */}
        <CardLabel>{t("EST_ASSET_NUMBER")}</CardLabel>
        <TextInput t={t} value={data.assetNo} readOnly style={fullWidthStyle} />

        {/* Asset Reference Number */}
        <CardLabel>{t("EST_ASSET_REFERENCE_NUMBER")}</CardLabel>
        <TextInput t={t} value={data.assetRefNumber} readOnly style={fullWidthStyle} />

        {/* Building Name */}
        <CardLabel>{t("EST_BUILDING_NAME")}</CardLabel>
        <TextInput t={t} value={formData?.assetData?.buildingName} readOnly style={fullWidthStyle} />

        {/* Locality */}
        <CardLabel>{t("EST_LOCALITY")}</CardLabel>
        <TextInput t={t} value={formData?.assetData?.locality} readOnly style={fullWidthStyle} />

        {/* Total Area */}
        <CardLabel>{t("EST_TOTAL_AREA")}</CardLabel>
        <TextInput
          t={t}
          value={`${formData?.assetData?.totalFloorArea || ""} sq. ft.`}
          readOnly
          style={fullWidthStyle}
        />

        {/* Floor */}
        <CardLabel>{t("EST_FLOOR")}</CardLabel>
        <TextInput t={t} value={formData?.assetData?.floor} readOnly style={fullWidthStyle} />

        {/* Rate */}
        <CardLabel>{t("EST_RATE")}</CardLabel>
        <TextInput
          t={t}
          value={`${formData?.assetData?.rate || ""}/ sq. ft.`}
          readOnly
          style={fullWidthStyle}
        />

        {/* ---------- Personal Details ---------- */}
        <h2 style={{ marginTop: "20px", color: "#333", marginBottom: "16px", fontSize: "20px", fontWeight: "bold" }}>
          {t("EST_PERSONAL_DETAILS_OF_ALLOTTEE")}
        </h2>

        {/* Property Type */}
        <RequiredLabel label="EST_PROPERTY_TYPE" />
        <Dropdown
          option={Asset_Type || []}
          optionKey="label"
          selected={Asset_Type?.find((o) => o.code === data.propertyType) || null}
          select={(o) => handleChange("propertyType", o.code)}
          placeholder={t("EST_SELECT_PROPERTY_TYPE")}
          t={t}
          style={fullWidthStyle}
        />

        {/* Allottee Name */}
        <RequiredLabel label="EST_ALLOTTEE_NAME" />
      <TextInput
        t={t}
        placeholder={t("EST_ENTER_ALLOTTEE_NAME")}
        value={data.allotteeName}
        onChange={(e) =>
          handleChange(
            "allotteeName",
            e.target.value.replace(/[^A-Za-z ]/g, "")   // 🔥 removes numbers & special chars live
          )
        }
        required
        minLength={3}
        title={t("EST_INVALID_ALLOTTEE_NAME")}
        style={fullWidthStyle}
      />

        {/* Phone (primary) - digits only, max 10 */}
        <RequiredLabel label="EST_PHONE_NUMBER" />
        <TextInput
          t={t}
          placeholder={t("EST_ENTER_PHONE_NUMBER")}
          value={data.phoneNumber}
          onChange={(e) =>
            sanitizeAndSet("phoneNumber", e.target.value, { maxLength: 10, regex: /[^0-9]/g })
          }
          maxLength={10}
          {...phoneValidation}
          style={fullWidthStyle}
        />

        {/* Alternate Phone - digits only, max 10 */}
        <CardLabel>{t("EST_ALTERNATE_PHONE_NUMBER")}</CardLabel>
        <TextInput
          t={t}
          placeholder={t("EST_ENTER_ALTERNATE_PHONE_NUMBER")}
          value={data.altPhoneNumber}
          onChange={(e) =>
            sanitizeAndSet("altPhoneNumber", e.target.value, { maxLength: 10, regex: /[^0-9]/g })
          }
          maxLength={10}
          style={fullWidthStyle}
        />

        {/* Email */}
        <RequiredLabel label="EST_EMAIL_ID" />
        <TextInput
          t={t}
          placeholder={t("EST_ENTER_EMAIL_ID")}
          value={data.email}
          onChange={(e) => handleChange("email", e.target.value)}
          {...emailValidation}
          style={fullWidthStyle}
        />

        {/* ---------- Allotment Details ---------- */}
        <h2 style={{ marginTop: "20px", color: "#333", marginBottom: "16px", fontSize: "20px", fontWeight: "bold" }}>
          {t("EST_ALLOTMENT_INVOICE_DETAILS")}
        </h2>

        {/* Agreement Start Date */}
        <RequiredLabel label="EST_AGREEMENT_START_DATE" />
        <DatePicker
          date={data.startDate}
          onChange={(d) => {
            handleChange("startDate", d);
            handleChange("duration", calculateDuration(d, data.endDate));
            // make sure duration string stored as well
            const newDuration = calculateDuration(d, data.endDate);
            handleChange("duration", newDuration);
          }}
          style={fullWidthStyle}
        />

        {/* Agreement End Date */}
        <RequiredLabel label="EST_AGREEMENT_END_DATE" />
        <DatePicker
          date={data.endDate}
          onChange={(d) => {
            handleChange("endDate", d);
            handleChange("duration", calculateDuration(data.startDate, d));
            const newDuration = calculateDuration(data.startDate, d);
            handleChange("duration", newDuration);
          }}
          style={fullWidthStyle}
        />

        {/* Duration - frozen, disabled, no cursor */}
        <CardLabel>{t("EST_DURATION_IN_MONTHS")}</CardLabel>
        <TextInput
          t={t}
          value={data.duration}
          readOnly
          disabled
          style={{ ...fullWidthStyle, pointerEvents: "none", cursor: "default", backgroundColor: "transparent" }}
        />

        {/* Rate per sqft */}
        <RequiredLabel label="EST_RATE_PER_SQFT" unit="(Per sq ft)" />
        <TextInput
          t={t}
          value={data.rate}
          onChange={(e) => sanitizeAndSet("rate", e.target.value, { maxLength: 12, regex: /[^0-9.]/g })}
          {...numberValidation}
          style={fullWidthStyle}
        />

        {/* Monthly Rent */}
        <RequiredLabel label="EST_MONTHLY_RENT_IN_INR" />
        <TextInput
          t={t}
          value={data.monthlyRent}
          onChange={(e) => sanitizeAndSet("monthlyRent", e.target.value, { maxLength: 12, regex: /[^0-9.]/g })}
          {...numberValidation}
          style={fullWidthStyle}
        />

        {/* Advance Payment */}
        <RequiredLabel label="EST_ADVANCE_PAYMENT_IN_INR" />
        <TextInput
          t={t}
          value={data.advancePayment}
          onChange={(e) => sanitizeAndSet("advancePayment", e.target.value, { maxLength: 12, regex: /[^0-9.]/g })}
          {...numberValidation}
          style={fullWidthStyle}
        />

        {/* Advance Payment Date */}
        <CardLabel>{t("EST_ADVANCE_PAYMENT_DATE")}</CardLabel>
        <DatePicker
          date={data.advancePaymentDate}
          onChange={(d) => handleChange("advancePaymentDate", d)}
          style={fullWidthStyle}
        />

        {/* ---------- Document Upload ---------- */}
        <h2 style={{ marginTop: "20px", color: "#333", marginBottom: "16px", fontSize: "20px", fontWeight: "bold" }}>
          {t("EST_DOCUMENT_UPLOAD")}
        </h2>

        <CardLabel>{t("EST_EOFFICE_FILE_NO")}</CardLabel>
        <TextInput
          t={t}
          placeholder={t("EST_ENTER_EOFFICE_FILE_NO")}
          value={data.eOfficeFileNo}
          onChange={(e) => handleChange("eOfficeFileNo", e.target.value)}
          style={fullWidthStyle}
        />

        <CardLabel>{t("EST_CITIZEN_REQUEST_LETTER")}</CardLabel>
        <div style={fullWidthStyle}>
          <UploadFile
            onUpload={(e) => handleFileUpload(e.target.files[0], "citizenLetter")}
            onDelete={() => handleFileDelete("citizenLetter")}
            id="citizenLetter"
            message={data.citizenLetter ? t("CS_ACTION_FILEUPLOADED") : t("CS_ACTION_NO_FILEUPLOADED")}
            accept=".png,.jpg,.jpeg,.pdf"
          />
        </div>

        <CardLabel>{t("EST_ALLOTMENT_LETTER")}</CardLabel>
        <div style={fullWidthStyle}>
          <UploadFile
            onUpload={(e) => handleFileUpload(e.target.files[0], "allotmentLetter")}
            onDelete={() => handleFileDelete("allotmentLetter")}
            id="allotmentLetter"
            message={data.allotmentLetter ? t("CS_ACTION_FILEUPLOADED") : t("CS_ACTION_NO_FILEUPLOADED")}
            accept=".png,.jpg,.jpeg,.pdf"
          />
        </div>

        <CardLabel>{t("EST_SIGNED_DEED")}</CardLabel>
        <div style={fullWidthStyle}>
          <UploadFile
            onUpload={(e) => handleFileUpload(e.target.files[0], "signedDeed")}
            onDelete={() => handleFileDelete("signedDeed")}
            id="signedDeed"
            message={data.signedDeed ? t("CS_ACTION_FILEUPLOADED") : t("CS_ACTION_NO_FILEUPLOADED")}
            accept=".png,.jpg,.jpeg,.pdf"
          />
        </div>

       
      </Card>

      {showToast && (
        <Toast label={toastMessage} error={toastError} onClose={() => setShowToast(false)} />
      )}
    </FormStep>
  );
};

export default ESTAssignAssets;
