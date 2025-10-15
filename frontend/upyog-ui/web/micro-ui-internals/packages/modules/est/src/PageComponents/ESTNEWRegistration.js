import React, { useState } from "react";
import {
  Header,
  Card,
  CardLabel,
  TextInput,
  Dropdown,
  SubmitBar,
  Toast,
} from "@upyog/digit-ui-react-components";
import { useTranslation } from "react-i18next";

const NewRegistration = () => {
  const { t } = useTranslation();

  const [formData, setFormData] = useState({
    assetNo: "",
    buildingName: "",
    buildingNumber: "",
    buildingFloor: "",
    buildingBlock: "",
    serviceType: "",
    totalPlotArea: "",
    dimensionX: "",
    dimensionY: "",
    rate: "",
    assetRef: "",
    assetType: "",
  });

  const [errors, setErrors] = useState({});
  const [showToast, setShowToast] = useState(false);

  const serviceOptions = [
    { label: t("NEW_DELHI"), code: "newdelhi" },
    { label: t("UTTAR_PRADESH"), code: "uttarpradesh" },
    { label: t("MADHYA_PRADESH"), code: "madhyapradesh" },
  ];

  const assetTypeOptions = [
    { label: t("Residential"), code: "RESIDENTIAL" },
    { label: t("Commercial"), code: "COMMERCIAL" },
    { label: t("Industrial"), code: "INDUSTRIAL" },
  ];

  const handleChange = (field, value) => {
    setFormData({ ...formData, [field]: value });
    setErrors({ ...errors, [field]: "" });
  };

  const validateForm = () => {
    const newErrors = {};

    if (!formData.assetNo.trim()) newErrors.assetNo = "Asset No is required";
    if (!formData.buildingName.trim()) newErrors.buildingName = "Building Name is required";
    if (!formData.buildingNumber.trim()) newErrors.buildingNumber = "Building Number is required";
    if (!formData.buildingFloor.trim()) newErrors.buildingFloor = "Building Floor is required";
    if (!formData.buildingBlock.trim()) newErrors.buildingBlock = "Building Block is required";
    if (!formData.serviceType) newErrors.serviceType = "Locality is required";
    if (!formData.totalPlotArea.trim()) newErrors.totalPlotArea = "Total Plot Area is required";
    if (!formData.dimensionX.trim()) newErrors.dimensionX = "Length is required";
    if (!formData.dimensionY.trim()) newErrors.dimensionY = "Breadth is required";
    if (!formData.rate.trim()) newErrors.rate = "Rate is required";
    if (!formData.assetRef.trim()) newErrors.assetRef = "Asset Reference Number is required";
    if (!formData.assetType) newErrors.assetType = "Asset Type is required";

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e) => {
    e.preventDefault(); // Prevent default form reload

    if (validateForm()) {
      console.log("Form Data Submitted:", formData);
      setShowToast(true);

      // Redirect after successful form submission
      setTimeout(() => {
        window.location.href = "http://localhost:3000/upyog-ui/employee/est/my-applications";
      }, 2000);
    }
  };

  return (
    <React.Fragment>
      <Header>{t("EST_COMMMON_NEW_REGISTRATION")}</Header>

      <Card style={{ padding: "16px" }}>
        {/* ✅ Wrap inside form */}
        <form onSubmit={handleSubmit}>
          <div
            style={{
              display: "grid",
              gridTemplateColumns: "repeat(2, 1fr)",
              gap: "16px",
            }}
          >
            {/* Asset No */}
            <div>
              <CardLabel>{t("ASSETS_NO")}</CardLabel>
              <TextInput
                placeholder={t("Enter Asset No.")}
                value={formData.assetNo}
                onChange={(e) => handleChange("assetNo", e.target.value)}
                error={errors.assetNo}
              />
              {errors.assetNo && <p style={{ color: "red" }}>{errors.assetNo}</p>}
            </div>

            {/* Building Name */}
            <div>
              <CardLabel>{t("BUILDING_NAME")}</CardLabel>
              <TextInput
                placeholder={t("Enter Building Name")}
                value={formData.buildingName}
                onChange={(e) => handleChange("buildingName", e.target.value)}
                error={errors.buildingName}
              />
              {errors.buildingName && <p style={{ color: "red" }}>{errors.buildingName}</p>}
            </div>

            {/* Building Number */}
            <div>
              <CardLabel>{t("BUILDING_NUMBER")}</CardLabel>
              <TextInput
                placeholder={t("Enter Building Number")}
                value={formData.buildingNumber}
                onChange={(e) => handleChange("buildingNumber", e.target.value)}
                error={errors.buildingNumber}
              />
              {errors.buildingNumber && <p style={{ color: "red" }}>{errors.buildingNumber}</p>}
            </div>

            {/* Building Floor */}
            <div>
              <CardLabel>{t("BUILDING_FLOOR")}</CardLabel>
              <TextInput
                placeholder={t("Enter Building Floor")}
                value={formData.buildingFloor}
                onChange={(e) => handleChange("buildingFloor", e.target.value)}
                error={errors.buildingFloor}
              />
              {errors.buildingFloor && <p style={{ color: "red" }}>{errors.buildingFloor}</p>}
            </div>

            {/* Building Block */}
            <div>
              <CardLabel>{t("BUILDING_BLOCK")}</CardLabel>
              <TextInput
                placeholder={t("Enter Building Block")}
                value={formData.buildingBlock}
                onChange={(e) => handleChange("buildingBlock", e.target.value)}
                error={errors.buildingBlock}
              />
              {errors.buildingBlock && <p style={{ color: "red" }}>{errors.buildingBlock}</p>}
            </div>

            {/* Locality */}
            <div>
              <CardLabel>{t("LOCALITY")}</CardLabel>
              <Dropdown
                selected={serviceOptions.find((opt) => opt.code === formData.serviceType)}
                select={(opt) => handleChange("serviceType", opt.code)}
                option={serviceOptions}
                optionKey="label"
                placeholder={t("Select Locality")}
                t={t}
              />
              {errors.serviceType && <p style={{ color: "red" }}>{errors.serviceType}</p>}
            </div>

            {/* Total Plot Area */}
            <div>
              <CardLabel>{t("TOTAL PLOT AREA")}</CardLabel>
              <TextInput
                placeholder={t("Enter Total Plot Area")}
                value={formData.totalPlotArea}
                onChange={(e) => handleChange("totalPlotArea", e.target.value)}
                error={errors.totalPlotArea}
              />
              {errors.totalPlotArea && <p style={{ color: "red" }}>{errors.totalPlotArea}</p>}
            </div>

            {/* Dimensions */}
            <div>
              <CardLabel>{t("DIMENSION")}</CardLabel>
              <div style={{ display: "flex", gap: "8px" }}>
                <TextInput
                  placeholder={t("Length")}
                  value={formData.dimensionX}
                  onChange={(e) => handleChange("dimensionX", e.target.value)}
                />
                <span>X</span>
                <TextInput
                  placeholder={t("Breadth")}
                  value={formData.dimensionY}
                  onChange={(e) => handleChange("dimensionY", e.target.value)}
                />
              </div>
              {(errors.dimensionX || errors.dimensionY) && (
                <p style={{ color: "red" }}>{errors.dimensionX || errors.dimensionY}</p>
              )}
            </div>

            {/* Rate */}
            <div>
              <CardLabel>{t("RATES")}</CardLabel>
              <TextInput
                placeholder={t("Enter Rate")}
                value={formData.rate}
                onChange={(e) => handleChange("rate", e.target.value)}
                error={errors.rate}
              />
              {errors.rate && <p style={{ color: "red" }}>{errors.rate}</p>}
            </div>

            {/* Asset Reference Number */}
            <div>
              <CardLabel>{t("ASSET REFERENCE NUMBER")}</CardLabel>
              <TextInput
                placeholder={t("Enter Asset Reference Number")}
                value={formData.assetRef}
                onChange={(e) => handleChange("assetRef", e.target.value)}
                error={errors.assetRef}
              />
              {errors.assetRef && <p style={{ color: "red" }}>{errors.assetRef}</p>}
            </div>

            {/* Asset Type */}
            <div>
              <CardLabel>{t("ASSET TYPE")}</CardLabel>
              <Dropdown
                selected={assetTypeOptions.find((opt) => opt.code === formData.assetType)}
                select={(opt) => handleChange("assetType", opt.code)}
                option={assetTypeOptions}
                optionKey="label"
                placeholder={t("Select Asset Type")}
                t={t}
              />
              {errors.assetType && <p style={{ color: "red" }}>{errors.assetType}</p>}
            </div>
          </div>

          {/* Submit Button */}
          <div style={{ marginTop: "24px", textAlign: "center" }}>
            <SubmitBar label={t("Submit")} submit />
          </div>
        </form>
      </Card>

      {showToast && (
        <Toast
          label={t("Form submitted successfully!")}
          onClose={() => setShowToast(false)}
          type="success"
        />
      )}
    </React.Fragment>
  );
};

export default NewRegistration;
