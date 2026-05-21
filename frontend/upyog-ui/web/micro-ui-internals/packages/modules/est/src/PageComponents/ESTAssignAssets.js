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
} from "@nudmcdgnpm/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import { calculateDuration } from "../utils";

const getLocalityText = (asset, t) => {
  if (!asset) return "";
  const locObj = asset.locality || asset.address?.locality;
  if (typeof locObj === "string") {
    if (locObj.startsWith("TENANT_") || locObj.startsWith("EST_")) return t(locObj);
    return locObj;
  }
  if (locObj && typeof locObj === "object") {
    if (locObj.i18nKey) return t(locObj.i18nKey);
    return locObj.label || locObj.name || locObj.code || "";
  }
  const candidates = [asset.localityName, asset.localityCode, asset.serviceType];
  const raw = candidates.find((v) => v !== undefined && v !== null && v !== "");
  if (!raw) return "";
  if (typeof raw === "string" && (raw.startsWith("TENANT_") || raw.startsWith("EST_"))) {
    return t(raw);
  }
  return raw;
};

const ESTAssignAssets = ({ t: propT, onSelect, onSkip, formData = {}, config }) => {

  // Translation hook (fallback support)
  const { t: hookT } = useTranslation();
  const t = propT || hookT;
  const tenantId = Digit.ULBService.getStateId();

  /**
   * Date ko epoch (milliseconds) mein convert karta hai
   * Invalid date ke case mein null return karta hai
   */
  const toEpoch = (value) => {
    if (!value) return null;
    const d = value instanceof Date ? value : new Date(value);
    return isNaN(d.getTime()) ? null : d.getTime();
  };

  const sanitizeAndSet = (field, value, { maxLength = null, regex = null } = {}) => {
    let v = value ?? "";
    if (typeof v !== "string") v = String(v);
    if (regex) v = v.replace(regex, "");
    if (maxLength && v.length > maxLength) v = v.slice(0, maxLength);
    setData((prev) => ({ ...prev, [field]: v }));
  };

 
  /**
   * Main form state
   * Existing formData se values prefill hoti hain
   */
  const [data, setData] = useState({
    assetNo: formData?.assetData?.estateNo || "",
    assetRefNumber: formData?.assetData?.refAssetNo || "",
    allotmentType: formData?.AllotmentData?.allotmentType || "",
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

   /**
   * MDMS se Asset Property Types fetch karta hai
   * Sirf active categories dropdown mein dikhengi
   */

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
  // Toast (success / error) state
  const [showToast, setShowToast] = useState(false);
  const [toastMessage, setToastMessage] = useState("");
  const [toastError, setToastError] = useState(false);
 /**
   * Generic state updater
   */
  const handleChange = (field, value) => {
    setData((prev) => ({ ...prev, [field]: value }));
  };

   /**
   * File upload handler
   * - size validation (5MB)
   * - filestore upload
   * - response se fileStoreId save
   */
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
/**
   * Uploaded document remove karne ke liye
   */
  const handleFileDelete = (fieldName) => handleChange(fieldName, null);

 // ---------- Validation Rules ----------

   // 10 digit phone number
  const phoneValidation = {
    required: true,
    pattern: "^[0-9]{10}$",
    title: t("EST_INVALID_PHONE_NUMBER"),
  };
  // Valid email format
  const emailValidation = {
    required: true,
    pattern: "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
    title: t("EST_INVALID_EMAIL_ID"),
  };
 // Numeric amount with optional decimals
  const numberValidation = {
    required: true,
    pattern: "^[0-9]+(\\.[0-9]{1,2})?$",
    title: t("EST_INVALID_AMOUNT"),
  };
/**
   * Form disable condition
   * Agar mandatory fields empty hain
   */
  const { data: allotmentTypeMdms } = Digit.Hooks.useCustomMDMS(
    Digit.ULBService.getStateId(),
    "Estate",
    [{ name: "AllotmentType" }],
    {
      select: (data) => {
        const list = data?.Estate?.AllotmentType || [];
        return list
          .filter((item) => item.active)
          .map((item) => ({
            code: item.code,
            label: item.name,
            i18nKey: item.name || item.code,
          }));
      },
    }
  );

  const fallbackAllotmentTypes = [
    { code: "RENT", label: "Rent", i18nKey: "Rent" },
    { code: "LEASE", label: "Lease", i18nKey: "Lease" },
  ];

  const allotmentTypeOptions =
    (Array.isArray(allotmentTypeMdms) && allotmentTypeMdms.length > 0)
      ? allotmentTypeMdms
      : fallbackAllotmentTypes;

  const isFormInvalid =
    !data.allotmentType ||
    !data.allotteeName ||
    !data.phoneNumber ||
    !data.email ||
    !data.startDate ||
    !data.endDate ||
    !data.rate ||
    !data.monthlyRent ||
    !data.advancePayment;

     /**
   * Next step submit handler
   * - Dates ko epoch mein convert
   * - Amount fields ko number mein convert
   * - Parent ko data bhejna
   */
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

  /**
   * Reusable label component
   * Required (*) indicator ke saath
   */
  const RequiredLabel = ({ label, unit }) => (
    <CardLabel>
      {t(label)} {unit && <span style={{ fontSize: "0.9em", marginLeft: "6px" }}>{unit}</span>}{" "}
      <span style={{ color: "red" }}>*</span>
    </CardLabel>
  );

  const fullWidthStyle = { width: "70%", marginBottom: "16px" };

  const asset = formData?.assetData || {};
  const localityText = getLocalityText(asset, t);

  return (
    <FormStep t={t} config={config} onSelect={goNext} onSkip={onSkip} isDisabled={isFormInvalid}>
      <Header>{t("EST_COMMMON_ASSIGN_ASSETS")}</Header>

      <Card style={{ padding: "16px" }}>
        <h2 style={{ color: "#333", marginBottom: "16px", fontSize: "20px", fontWeight: "bold" }}>
          {t("EST_ASSET_DETAILS")}
        </h2>

        <CardLabel>{t("EST_ASSET_NUMBER")}</CardLabel>
        <TextInput t={t} value={data.assetNo} readOnly style={fullWidthStyle} />

        <CardLabel>{t("EST_ASSET_REFERENCE_NUMBER")}</CardLabel>
        <TextInput t={t} value={data.assetRefNumber} readOnly style={fullWidthStyle} />

        <CardLabel>{t("EST_BUILDING_NAME")}</CardLabel>
        <TextInput t={t} value={formData?.assetData?.buildingName} readOnly style={fullWidthStyle} />

        <CardLabel>{t("EST_LOCALITY")}</CardLabel>
        <TextInput t={t} value={localityText} readOnly style={fullWidthStyle} />

        <CardLabel>{t("EST_TOTAL_AREA")}</CardLabel>
        <TextInput
          t={t}
          value={`${formData?.assetData?.totalFloorArea || ""} sq. ft.`}
          readOnly
          style={fullWidthStyle}
        />

        <CardLabel>{t("EST_FLOOR")}</CardLabel>
        <TextInput t={t} value={formData?.assetData?.floor} readOnly style={fullWidthStyle} />

        <CardLabel>{t("EST_RATE")}</CardLabel>
        <TextInput
          t={t}
          value={`${formData?.assetData?.rate || ""}/ sq. ft.`}
          readOnly
          style={fullWidthStyle}
        />

        <h2 style={{ marginTop: "20px", color: "#333", marginBottom: "16px", fontSize: "20px", fontWeight: "bold" }}>
          {t("EST_PERSONAL_DETAILS_OF_ALLOTTEE")}
        </h2>

       <RequiredLabel label="EST_ALLOTMENT_TYPE" />
<div style={{ display: "flex", gap: "20px", marginBottom: "16px" }}>
  <label style={{ display: "flex", alignItems: "center", cursor: "pointer" }}>
    <input
      type="radio"
      name="allotmentType"
      value="RENT"
      checked={data.allotmentType === "RENT"}
      onChange={() => handleChange("allotmentType", "RENT")}
      style={{ marginRight: "8px" }}
    />
    Rent
  </label>
  <label style={{ display: "flex", alignItems: "center", cursor: "pointer" }}>
    <input
      type="radio"
      name="allotmentType"
      value="LEASE"
      checked={data.allotmentType === "LEASE"}
      onChange={() => handleChange("allotmentType", "LEASE")}
      style={{ marginRight: "8px" }}
    />
    Lease
  </label>
</div>


        <RequiredLabel label="EST_ALLOTTEE_NAME" />
        <TextInput
          t={t}
          placeholder={t("EST_ENTER_ALLOTTEE_NAME")}
          value={data.allotteeName}
          onChange={(e) =>
            handleChange("allotteeName", e.target.value.replace(/[^A-Za-z ]/g, ""))
          }
          required
          minLength={3}
          title={t("EST_INVALID_ALLOTTEE_NAME")}
          style={fullWidthStyle}
        />

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

        <RequiredLabel label="EST_EMAIL_ID" />
        <TextInput
          t={t}
          placeholder={t("EST_ENTER_EMAIL_ID")}
          value={data.email}
          onChange={(e) => handleChange("email", e.target.value)}
          {...emailValidation}
          style={fullWidthStyle}
        />

        <h2 style={{ marginTop: "20px", color: "#333", marginBottom: "16px", fontSize: "20px", fontWeight: "bold" }}>
          {t("EST_ALLOTMENT_INVOICE_DETAILS")}
        </h2>

        <RequiredLabel label="EST_AGREEMENT_START_DATE" />
        <DatePicker
          date={data.startDate}
          onChange={(d) => {
            handleChange("startDate", d);
            const newDuration = calculateDuration(d, data.endDate);
            handleChange("duration", newDuration);
          }}
          style={fullWidthStyle}
        />

        <RequiredLabel label="EST_AGREEMENT_END_DATE" />
        <DatePicker
          date={data.endDate}
          onChange={(d) => {
            handleChange("endDate", d);
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
          style={{
            ...fullWidthStyle,
            pointerEvents: "none",
            cursor: "default",
            backgroundColor: "transparent",
          }}
        />

        <RequiredLabel label="EST_RATE_PER_SQFT" unit="(Per sq ft)" />
        <TextInput
          t={t}
          value={data.rate}
          onChange={(e) =>
            sanitizeAndSet("rate", e.target.value, { maxLength: 12, regex: /[^0-9.]/g })
          }
          {...numberValidation}
          style={fullWidthStyle}
        />

        <RequiredLabel label="EST_MONTHLY_RENT_IN_INR" />
        <TextInput
          t={t}
          value={data.monthlyRent}
          onChange={(e) =>
            sanitizeAndSet("monthlyRent", e.target.value, { maxLength: 12, regex: /[^0-9.]/g })
          }
          {...numberValidation}
          style={fullWidthStyle}
        />

        <RequiredLabel label="EST_ADVANCE_PAYMENT_IN_INR" />
        <TextInput
          t={t}
          value={data.advancePayment}
          onChange={(e) =>
            sanitizeAndSet("advancePayment", e.target.value, { maxLength: 12, regex: /[^0-9.]/g })
          }
          {...numberValidation}
          style={fullWidthStyle}
        />

        <CardLabel>{t("EST_ADVANCE_PAYMENT_DATE")}</CardLabel>
        <DatePicker
          date={data.advancePaymentDate}
          onChange={(d) => handleChange("advancePaymentDate", d)}
          style={fullWidthStyle}
        />

        <h2 style={{ marginTop: "20px", color: "#333", marginBottom: "16px", fontSize: "20px", fontWeight: "bold" }}>
          {t("EST_DOCUMENT_UPLOAD")}
        </h2>

        <CardLabel>{t("FILE_REFERENCE_NUMBER")}</CardLabel>
        <TextInput
          t={t}
          placeholder={t("EST_ENTER_FILE_NO")}
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
        
      </Card>

      {showToast && (
        <Toast label={toastMessage} error={toastError} onClose={() => setShowToast(false)} />
      )}
    </FormStep>
  );
};
export default ESTAssignAssets;