import React, { useState } from "react";
import { useSearchParams } from "react-router-dom";
import { FormStep, CardLabel, Dropdown, } from "@nudmcdgnpm/digit-ui-react-components";
import { profileStyles } from "../../utils/profileStyles";
import FamilyCreateTabs from "../../config/FamilyCreateTabs";
import RuralPropertyForm from "./RuralForm";
import UrbanPropertyForm from "./UrbanForm";

const LOCALITY_TYPE_OPTIONS = [
  {
    name: "Urban",
    code: "URBAN",
  },
  {
    name: "Rural",
    code: "RURAL",
  },
];

/**
 * Property Details Page Developed to get the Property details of the family Members
 * These Details Will be filled by the Family Head
 * Head should have to provide below details :-
 *  1. Property Proof
 *  2. Locality Type
 *  3. Rural / Urban Property Details
 *  5. Owner Name
 *  6. Property Address
 */
const PropertyDetails = ({ t, config, onSelect, formData }) => {
  const styles = profileStyles.propertyDetails;
  const [searchParams] = useSearchParams();
  const isViewMode = searchParams.get("mode") === "view" || !!config?.isViewMode;
  const saved = formData?.[config?.key] || {};
  const [localityType, setLocalityType] = useState(
    saved.localityType ? LOCALITY_TYPE_OPTIONS.find((item) => item.code === saved.localityType) : null);
  const [propertyData, setPropertyData] = useState(saved);
  const updatePropertyData = (data) => {
    if (isViewMode) return;
    setPropertyData((prev) => ({ ...prev, ...data, }));
  };

  /*
   * Locality Type change.
   */
  const handleLocalityTypeChange = (value) => {
    if (isViewMode) return;
    setLocalityType(value);
    setPropertyData((prev) => {
      if (value?.code === "RURAL") {
        return {
          ...prev,
          localityType: value.code,
          urbanPropertyAddress:
            localityType?.code === "URBAN" ? prev.propertyAddress : prev.urbanPropertyAddress,
          propertyAddress: "",
        };
      }

      if (value?.code === "URBAN") {
        const primaryProperty = prev.properties?.find(
          (property) => property.propertyId === (prev.primaryPropertyId || prev.propertyId)
        ) || prev.properties?.[0];

        return {
          ...prev,
          localityType: value.code,
          propertyAddress:
            prev.urbanPropertyAddress ||
            primaryProperty?.propertyAddress ||
            (localityType?.code === "URBAN" ? prev.propertyAddress : ""),
        };
      }

      return { ...prev, localityType: value?.code };
    });
  };

  const goNext = () => {
    onSelect(config.key, { ...propertyData, localityType: localityType?.code }, false);
  };
  const onSkip = () => onSelect();

  const hasUrbanProperty =
    propertyData.properties?.some((property) => property.propertyId) ||
    (propertyData.propertyId && propertyData.ownerName);
  const isDisabled =
    !localityType ||
    (localityType.code === "URBAN" && !hasUrbanProperty) ||
    (localityType.code === "RURAL" &&
      (!propertyData.gramPanchayat || !propertyData.village || !propertyData.ward || !propertyData.propertyAddress || !propertyData.propertyType ||
        !propertyData.propertyUsage || !propertyData.surveyNumber || !propertyData.ownershipType || !propertyData.documentType || !propertyData.propertyDocument ||
        (propertyData.ownershipType === "JOINT" && (!propertyData.ownerName || !propertyData.ownerMobile))
      ));

  return (
    <React.Fragment>
      {/* Profile Tabs */}
      <div> <FamilyCreateTabs t={t} /></div>
      <FormStep
        config={{
          ...config,
          texts: {
            ...config?.texts,
            submitBarLabel: isViewMode ? "COMMON_NEXT" : (config?.texts?.submitBarLabel || "COMMON_SAVE_NEXT"),
          },
        }}
        onSelect={goNext}
        onSkip={onSkip}
        t={t}
        isDisabled={isViewMode ? false : isDisabled}
      >
        <div style={styles.container}>
          <div style={profileStyles.componentBottomMargin}>
            <CardLabel> Locality Type{" "}
              <span className="astericColor">*</span>
            </CardLabel>
            <Dropdown
              className="form-field"
              selected={localityType}
              select={handleLocalityTypeChange}
              option={LOCALITY_TYPE_OPTIONS}
              optionKey="name"
              placeholder="Urban/Rural"
              disable={isViewMode}
            />
          </div>
          {localityType?.code === "URBAN" && (<UrbanPropertyForm t={t} data={propertyData} onChange={updatePropertyData} disabled={isViewMode} />)}
          {localityType?.code === "RURAL" && (
            <RuralPropertyForm t={t} data={propertyData} onChange={updatePropertyData} disabled={isViewMode} />
          )}
        </div>
      </FormStep>
    </React.Fragment>
  );
};

export default PropertyDetails;