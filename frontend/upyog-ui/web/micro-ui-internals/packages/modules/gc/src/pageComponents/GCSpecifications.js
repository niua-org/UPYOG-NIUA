import React, { useState, useMemo } from "react";
import {
  FormStep,
  CardLabel,
  TextInput,
  Dropdown,
  CheckBox,
  CardSubHeader,
  Toast,
} from "@nudmcdgnpm/digit-ui-react-components";
import { multiUnits } from "../utils";
import GCTenantDetails from "./GCTenantDetails";

/**
 * GCSpecifications Component
 * 
 * Step in the GC Application Wizard:
 * - Owner Specification Card:
 *   - Collection Type (from MDMS CollectionType)
 *   - Category (from MDMS Categories)
 *   - Sub-Category (from MDMS SubCategories)
 *   - Sub-Category Type (from SubCategories subcategorytype)
 *   - Special Category (Dropdown: NONE, FREEDOM_FIGHTER, BPL, SENIOR_CITIZEN, DEFENCE)
 *   - Old Garbage ID (optional) & Inheritance Transfer Checkbox
 * - Tenant Add-On Section (Encapsulated in GCTenantDetails)
 */
const GCSpecifications = ({ t, config, onSelect, formData }) => {
  // 1. Context & Form Data Variables at the top
  const user = Digit.UserService.getUser().info;
  const stateId = Digit.ULBService.getStateId();
  const specsData = formData?.[config?.key] || {};
  const propertyLoc = formData?.gcpropertylocdetails;
  const detectedFloorCount = Number(propertyLoc?.detectedFloorCount);
  const inputStyles = { width: user?.type === "EMPLOYEE" ? "50%" : "100%" };

  const {
    oldGarbageId: initOldGarbageId = "", typeOfCollection: initTypeOfCollection = "",
    category: initCategory = "", subCategory: initSubCategory = "",
    subCategoryType: initSubCategoryType = "", specialCategory: initSpecialCategory = "",
    no_of_units: initNoOfUnits = 1, isInheritance: initIsInheritance = false, tenants: initTenants = [],
  } = specsData;

  // 2. React State Declarations at the top
  const [oldGarbageId, setOldGarbageId] = useState(initOldGarbageId);
  const [typeOfCollection, setTypeOfCollection] = useState(initTypeOfCollection);
  const [category, setCategory] = useState(initCategory);
  const [subCategory, setSubCategory] = useState(initSubCategory);
  const [subCategoryType, setSubCategoryType] = useState(initSubCategoryType);
  const [specialCategory, setSpecialCategory] = useState(initSpecialCategory);
  const [no_of_units, setNoOfUnits] = useState(initNoOfUnits);
  const [isInheritance, setIsInheritance] = useState(initIsInheritance);
  const [tenants, setTenants] = useState(initTenants);
  const [toast, setToast] = useState(null);

  // 3. MDMS Data Fetch (Direct destructuring utilizing right-side line space)
  const { data: { CollectionType = [], Categories = [], SubCategories = [], SpeacialCategories = [] } = {} } = Digit.Hooks.useEnabledMDMS(
    stateId,
    "Garbage",
    [{ name: "CollectionType" }, { name: "Categories" }, { name: "SubCategories" }, { name: "SpeacialCategories" }],
    { select: (data) => data?.Garbage }
  );

  // 4. Memoized Dropdown Options
  const collectionTypeOptions = useMemo(() => CollectionType.map((item) => ({ i18nKey: item.code, code: item.code, name: item.name })), [CollectionType]);
  const categoryOptions = useMemo(() => Categories.map((item) => ({ i18nKey: item.code, code: item.code, name: item.name })), [Categories]);
  const subCategoryOptions = useMemo(() => SubCategories.filter((item) => item.category === category?.code).map((item) => ({ i18nKey: item.subcategory, code: item.subcategory, name: item.name })), [SubCategories, category]);
  const matchedSubCategory = useMemo(() => SubCategories.find((item) => item.category === category?.code && item.subcategory === subCategory?.code), [SubCategories, category, subCategory]);
  const subCategoryTypeOptions = useMemo(() => matchedSubCategory?.subcategorytype?.map((typeItem) => ({ i18nKey: typeItem.code, code: typeItem.code, name: typeItem.name })), [matchedSubCategory]);
  const specialCategoryOptions = useMemo(() => SpeacialCategories.map((opt) => ({ code: opt.code, i18nKey: opt.code, name: opt.name, value: opt.code })), [SpeacialCategories]);

  const floorOptions = useMemo(() => {
    const maxFloors = Math.max(1, detectedFloorCount);
    const options = [];
    for (let i = 0; i < maxFloors; i++) {
      options.push({
        code: String(i),
        i18nKey: `GC_FLOOR_${i}`,
        name: i === 0 ? t("GC_FLOOR_GROUND") : `${i}${i === 1 ? "st" : i === 2 ? "nd" : i === 3 ? "rd" : "th"} Floor`,
        value: String(i),
      });
    }
    return options;
  }, [detectedFloorCount, t]);

  const goNext = () => {
    // Check duplicate mobiles or floors among tenants
    const tenantMobiles = tenants.map((tn) => tn.mobileNumber?.trim()).filter(Boolean);
    if (new Set(tenantMobiles).size !== tenantMobiles.length) {
      setToast({ error: true, label: t("GC_DUPLICATE_TENANT_MOBILE_ERR") });
      return;
    }

    const tenantFloors = tenants.map((tn) => String(tn.floorNo)).filter(Boolean);
    if (new Set(tenantFloors).size !== tenantFloors.length) {
      setToast({ error: true, label: t("GC_DUPLICATE_TENANT_FLOOR_ERR") });
      return;
    }

    const data = {
      oldGarbageId,
      typeOfCollection,
      category,
      subCategory,
      subCategoryType,
      specialCategory,
      no_of_units: multiUnits.includes(typeOfCollection?.code) ? no_of_units : 1,
      isInheritance,
      detectedFloorCount,
      tenants,
    };

    onSelect(config.key, data, false);
  };

  const hasValidUnits = !multiUnits.includes(typeOfCollection?.code) ? true : Boolean(no_of_units);
  const isFormValid =
    typeOfCollection &&
    category &&
    subCategory &&
    subCategoryType &&
    hasValidUnits &&
    tenants.every((tUnit) => tUnit.floorNo && tUnit.name?.trim().length >= 2 && /^[5-9]\d{9}$/.test(tUnit.mobileNumber));

  return (
    <React.Fragment>
      <FormStep config={config} onSelect={goNext} t={t} isDisabled={!isFormValid}>
        <div>
          {/* Card 1: Owner Collection Specifications */}
          <CardSubHeader className="gc-spec-subheader">{t("GC_OWNER_SPECIFICATIONS_HEADER")}</CardSubHeader>

          <CardLabel>{t("GC_OLD_GARBAGE_ID")}</CardLabel>
          <TextInput
            value={oldGarbageId}
            placeholder="e.g. 342"
            style={inputStyles}
            onChange={(e) => {
              const value = e.target.value.replace(/\D/g, "");
              setOldGarbageId(value);
              if (!value) setIsInheritance(false);
            }}
          />

          <CardLabel>{t("GC_TYPE_OF_COLLECTION")} <span className="astericColor">*</span></CardLabel>
          <Dropdown
            option={collectionTypeOptions}
            optionKey="i18nKey"
            selected={typeOfCollection}
            select={(val) => { setTypeOfCollection(val); if (!multiUnits.includes(val?.code)) setNoOfUnits(1); }}
            placeholder={t("GC_SELECT_TYPE")}
            style={inputStyles}
            t={t}
          />

          {multiUnits.includes(typeOfCollection?.code) && (
            <>
              <CardLabel>{t("GC_NO_OF_UNITS")} <span className="astericColor">*</span></CardLabel>
              <TextInput value={no_of_units} style={inputStyles} onChange={(e) => setNoOfUnits(e.target.value.replace(/\D/g, ""))} />
            </>
          )}

          <CardLabel>{t("GC_CATEGORY")} <span className="astericColor">*</span></CardLabel>
          <Dropdown
            option={categoryOptions}
            optionKey="i18nKey"
            selected={category}
            select={(val) => { setCategory(val); setSubCategory(""); setSubCategoryType(""); }}
            style={inputStyles}
            t={t}
          />

          <CardLabel>{t("GC_SUB_CATEGORY")} <span className="astericColor">*</span></CardLabel>
          <Dropdown
            option={subCategoryOptions}
            optionKey="i18nKey"
            selected={subCategory}
            select={(val) => { setSubCategory(val); setSubCategoryType(""); }}
            style={inputStyles}
            t={t}
          />

          <CardLabel>{t("GC_SUB_CATEGORY_TYPE")} <span className="astericColor">*</span></CardLabel>
          <Dropdown
            option={subCategoryTypeOptions}
            optionKey="i18nKey"
            selected={subCategoryType}
            select={(val) => setSubCategoryType(val)}
            style={inputStyles}
            t={t}
          />

          <CardLabel>{t("GC_SPECIAL_CATEGORY")} <span className="astericColor">*</span></CardLabel>
          <Dropdown option={specialCategoryOptions} optionKey="i18nKey" selected={specialCategory} select={(val) => setSpecialCategory(val)} style={inputStyles} t={t} />

          {oldGarbageId && oldGarbageId.trim().length > 0 && (
            <div className="gc-inheritance-wrapper">
              <CheckBox label={t("GC_IS_INHERITANCE")} checked={isInheritance} onChange={(e) => setIsInheritance(e.target.checked)} />
            </div>
          )}

          {/* Card 2: Dynamic Floor-Detected Tenant Add-on */}
          <GCTenantDetails
            t={t}
            user={user}
            tenants={tenants}
            setTenants={setTenants}
            detectedFloorCount={detectedFloorCount}
            floorOptions={floorOptions}
            setToast={setToast}
          />
        </div>
      </FormStep>

      {toast && (
        <Toast error={toast.error} warning={toast.warning} label={toast.label} onClose={() => setToast(null)} />
      )}
    </React.Fragment>
  );
};

export default GCSpecifications;
