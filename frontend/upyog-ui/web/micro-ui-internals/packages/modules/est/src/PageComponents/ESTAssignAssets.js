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
    setFormData((prev) => ({ ...prev, [field]: value }));
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
  
   //  numberValidation is used for validating fields that require numeric input such as rate, monthlyRent, and advancePayment.

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

  return (
    <FormStep t={t} config={config} onSelect={goNext} onSkip={onSkip} isDisabled={isFormInvalid}>
      <Header>{t("EST_COMMMON_ASSIGN_ASSETS")}</Header>

      <Card style={{ padding: "24px 32px" }}>

        {/* ---------- Asset Info ---------- */}
        <h1 style={{ color: "#333", marginBottom: "16px" }}>{t("EST_ASSET_DETAILS_")}</h1>

        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" }}>
          <div>
            <CardLabel>{t("EST_ASSET_NUMBER")}</CardLabel>
            <TextInput t={t} value={data.assetNo} readOnly />
          </div>

          <div>
            <CardLabel>{t("EST_ASSET_REFERENCE_NUMBER")}</CardLabel>
            <TextInput t={t} value={data.assetRefNumber} readOnly />
          </div>

          <div>
            <CardLabel>{t("EST_BUILDING_NAME")}</CardLabel>
            <TextInput t={t} value={formData?.assetData?.buildingName} readOnly />
          </div>

          <div>
            <CardLabel>{t("EST_LOCALITY")}</CardLabel>
            <TextInput t={t} value={formData?.assetData?.locality} readOnly />
          </div>

          <div>
            <CardLabel>{t("EST_TOTAL_AREA")}</CardLabel>
            <TextInput
              t={t}
              value={`${formData?.assetData?.totalFloorArea || ""} sq. ft.`}
              readOnly
            />
          </div>

          <div>
            <CardLabel>{t("EST_FLOOR")}</CardLabel>
            <TextInput t={t} value={formData?.assetData?.floor} readOnly />
          </div>

          <div>
            <CardLabel>{t("EST_RATE")}</CardLabel>
            <TextInput
              t={t}
              value={`${formData?.assetData?.rate || ""}/ sq. ft.`}
              readOnly
            />
          </div>
        </div>

        {/* ---------- Personal Details ---------- */}
        <h1 style={{ marginTop: "20px", color: "#333", marginBottom: "16px" }}>
          {t("EST_PERSONAL_DETAILS_OF_ALLOTTEE")}
        </h1>

        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" }}>

          {/* Property Type */}
          <div>
            <CardLabel>
              {t("EST_PROPERTY_TYPE")}
              <span style={{ color: "red", marginLeft: 4 }}>*</span>
            </CardLabel>
            <Dropdown
              option={Asset_Type || []}
              optionKey="label"
              selected={Asset_Type?.find((o) => o.code === data.propertyType) || null}
              select={(o) => handleChange("propertyType", o.code)}
              placeholder={t("EST_SELECT_PROPERTY_TYPE")}
              t={t}
            />
          </div>

          {/* Allottee Name */}
          <div>
            <CardLabel>
              {t("EST_ALLOTTEE_NAME")}
              <span style={{ color: "red", marginLeft: 4 }}>*</span>
            </CardLabel>
            <TextInput
              t={t}
              placeholder={t("EST_ENTER_ALLOTTEE_NAME")}
              value={data.allotteeName}
              onChange={(e) => handleChange("allotteeName", e.target.value)}
              required
              minLength={3}
              pattern="^[a-zA-Z ]+$"
              title={t("EST_INVALID_ALLOTTEE_NAME")}
            />
          </div>

          {/* Phone */}
          <div>
            <CardLabel>
              {t("EST_PHONE_NUMBER")}
              <span style={{ color: "red", marginLeft: 4 }}>*</span>
            </CardLabel>
            <TextInput
              t={t}
              placeholder={t("EST_ENTER_PHONE_NUMBER")}
              value={data.phoneNumber}
              onChange={(e) => handleChange("phoneNumber", e.target.value)}
              {...phoneValidation}
            />
          </div>

          {/* Alternate Phone – NO * */}
          <div>
            <CardLabel>{t("EST_ALTERNATE_PHONE_NUMBER")}</CardLabel>
            <TextInput
              t={t}
              placeholder={t("EST_ENTER_ALTERNATE_PHONE_NUMBER")}
              value={data.altPhoneNumber}
              onChange={(e) => handleChange("altPhoneNumber", e.target.value)}
            />
          </div>

          {/* Email */}
          <div style={{ gridColumn: "span 2" }}>
            <CardLabel>
              {t("EST_EMAIL_ID")}
              <span style={{ color: "red", marginLeft: 4 }}>*</span>
            </CardLabel>
            <TextInput
              t={t}
              placeholder={t("EST_ENTER_EMAIL_ID")}
              value={data.email}
              onChange={(e) => handleChange("email", e.target.value)}
              {...emailValidation}
            />
          </div>
        </div>

        {/* ---------- Allotment Details ---------- */}
        <h1 style={{ marginTop: "24px", color: "#333", marginBottom: "16px" }}>
          {t("EST_ALLOTMENT_INVOICE_DETAILS")}
        </h1>

        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" }}>

          <div>
            <CardLabel>
              {t("EST_AGREEMENT_START_DATE")}
              <span style={{ color: "red", marginLeft: 4 }}>*</span>
            </CardLabel>
            <DatePicker
              date={data.startDate}
              onChange={(d) => {
                handleChange("startDate", d);
                handleChange("duration", calculateDuration(d, data.endDate));
              }}
            />
          </div>

          <div>
            <CardLabel>
              {t("EST_AGREEMENT_END_DATE")}
              <span style={{ color: "red", marginLeft: 4 }}>*</span>
            </CardLabel>
            <DatePicker
              date={data.endDate}
              onChange={(d) => {
                handleChange("endDate", d);
                handleChange("duration", calculateDuration(data.startDate, d));
              }}
            />
          </div>

          <div>
            <CardLabel>{t("EST_DURATION_IN_YEARS")}</CardLabel>
            <TextInput t={t} value={data.duration} readOnly />
          </div>

          <div>
            <CardLabel>
              {t("EST_RATE_PER_SQFT")}
              <span style={{ color: "red", marginLeft: 4 }}>*</span>
            </CardLabel>
            <TextInput
              t={t}
              value={data.rate}
              onChange={(e) => handleChange("rate", e.target.value)}
              {...numberValidation}
            />
          </div>

          <div>
            <CardLabel>
              {t("EST_MONTHLY_RENT_IN_INR")}
              <span style={{ color: "red", marginLeft: 4 }}>*</span>
            </CardLabel>
            <TextInput
              t={t}
              value={data.monthlyRent}
              onChange={(e) => handleChange("monthlyRent", e.target.value)}
              {...numberValidation}
            />
          </div>

          <div>
            <CardLabel>
              {t("EST_ADVANCE_PAYMENT_IN_INR")}
              <span style={{ color: "red", marginLeft: 4 }}>*</span>
            </CardLabel>
            <TextInput
              t={t}
              value={data.advancePayment}
              onChange={(e) => handleChange("advancePayment", e.target.value)}
              {...numberValidation}
            />
          </div>

          <div style={{ gridColumn: "span 2" }}>
            <CardLabel>{t("EST_ADVANCE_PAYMENT_DATE")}</CardLabel>
            <DatePicker
              date={data.advancePaymentDate}
              onChange={(d) => handleChange("advancePaymentDate", d)}
            />
          </div>
        </div>

        {/* ---------- Document Upload ---------- */}
        <h1 style={{ marginTop: "24px", color: "#333", marginBottom: "16px" }}>
          {t("EST_DOCUMENT_UPLOAD")}
        </h1>

        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" }}>

          <div>
            <CardLabel>{t("EST_EOFFICE_FILE_NO")}</CardLabel>
            <TextInput
              t={t}
              placeholder={t("EST_ENTER_EOFFICE_FILE_NO")}
              value={data.eOfficeFileNo}
              onChange={(e) => handleChange("eOfficeFileNo", e.target.value)}
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
            />
          </div>
        </div>
      </Card>

      {showToast && (
        <Toast label={toastMessage} error={toastError} onClose={() => setShowToast(false)} />
      )}
    </FormStep>
  );
};

export default ESTAssignAssets;
