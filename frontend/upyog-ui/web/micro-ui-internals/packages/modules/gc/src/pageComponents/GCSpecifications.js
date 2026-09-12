import React, { useState, useEffect } from "react";
import {
    FormStep,
    CardLabel,
    TextInput,
    Dropdown,
    CheckBox,
    CloseSvg,
} from "@nudmcdgnpm/digit-ui-react-components";
import i18next from "i18next";
import {multiUnits} from "../utils";


/**
 * GCSpecifications Component
 * 
 * Renders the garbage specification details step in the GC application wizard.
 * Collects information about collection type, property owner/tenant status, 
 * applicant details (name, phone, gender, email), waste category/subcategory,
 * and additional billing configuration (fixed/variable calculation, units).
 * Supports inheritance flag when an old garbage ID is provided.
 * 
 * Props:
 * - `t`: i18n translation function
 * - `config`: Step configuration from the wizard config
 * - `onSelect`: Callback when the form is submitted
 * - `formData`: Current accumulated form data
 * - `renewApplication`: Existing application for pre-filling during edit
 */
const GCSpecifications = ({ t, config, onSelect, formData, renewApplication }) => {

    const convertToObject = (params) => ({ i18nKey: params, code: params, value: params });

    const user = Digit.UserService.getUser().info;
    const stateId = Digit.ULBService.getStateId();

    // --- MDMS fetches: master names now match your MDMS files exactly ---
    const { data: collectionTypesData } = Digit.Hooks.useCustomMDMS(stateId, "Garbage", [{ name: "CollectionType" }], {
        select: (data) => data?.["Garbage"]?.["CollectionType"] || [],
    });

    const { data: ownerTypesData } = Digit.Hooks.useCustomMDMS(stateId, "Garbage", [{ name: "OwnerType" }], {
        select: (data) => data?.["Garbage"]?.["OwnerType"] || [],
    });

    const { data: genderData } = Digit.Hooks.useCustomMDMS(stateId, "Garbage", [{ name: "Gender" }], {
        select: (data) => data?.["Garbage"]?.["Gender"] || [],
    });

    const { data: categoriesData } = Digit.Hooks.useCustomMDMS(stateId, "Garbage", [{ name: "Categories" }], {
        select: (data) => data?.["Garbage"]?.["Categories"] || [],
    });

    // SubCategories is its own master, separate from Categories, and carries the nested subcategorytype array
    const { data: subCategoriesData } = Digit.Hooks.useCustomMDMS(stateId, "Garbage", [{ name: "SubCategories" }], {
        select: (data) => data?.["Garbage"]?.["SubCategories"] || [],
    });

    // --- Build dropdown option lists straight from the MDMS shape ---
    const CollectionTypes = collectionTypesData?.map((item) => ({
        i18nKey: item.code,
        code: item.code,
        name: item.name,
    })) || [];

    const OwnerTypes = ownerTypesData?.map((item) => ({
        i18nKey: item.code,
        code: item.code,
        name: item.name,
    })) || [];

    const Genders = genderData?.map((item) => ({
        i18nKey: item.code,
        code: item.code,
        name: item.name,
    })) || [];

    const UniqueCategories = categoriesData?.map((item) => ({
        i18nKey: item.code,
        code: item.code,
        name: item.name,
    })) || [];

    const specsData = formData?.[config?.key] || formData?.gcspecifications || formData?.GCSpecifications || {};
    const applicantPhoneNumber = formData?.owner?.mobileNumber || formData?.owners?.[0]?.mobileNumber || "";

    const isOwner = (ownerType) => String(ownerType?.code || ownerType?.i18nKey || "").toUpperCase() === "OWNER";

    const [oldGarbageId, setOldGarbageId] = useState(specsData.oldGarbageId || renewApplication?.grbgOldDetails?.oldGarbageId || "");
    const [typeOfCollection, setTypeOfCollection] = useState(specsData.typeOfCollection || convertToObject(renewApplication?.grbgCollectionUnits?.[0]?.unitType) || "");
    const [propertyOwnerType, setPropertyOwnerType] = useState(specsData.propertyOwnerType || convertToObject(renewApplication?.grbgCollectionUnits?.[0]?.ownerType) || "");
    const [name, setName] = useState(specsData.name || renewApplication?.name || "");
    // This field intentionally starts blank. It is populated from Applicant Details
    // only when the user explicitly selects Owner.
    const [phoneNumber, setPhoneNumber] = useState("");
    const [gender, setGender] = useState(specsData.gender || convertToObject(renewApplication?.gender) || "");
    const [email, setEmail] = useState(specsData.email || renewApplication?.emailId || "");
    const [category, setCategory] = useState(specsData.category || convertToObject(renewApplication?.grbgCollectionUnits?.[0]?.category) || "");
    const [subCategory, setSubCategory] = useState(specsData.subCategory || convertToObject(renewApplication?.grbgCollectionUnits?.[0]?.subCategory) || "");
    const [subCategoryType, setSubCategoryType] = useState(specsData.subCategoryType ||convertToObject(renewApplication?.grbgCollectionUnits?.[0]?.subCategoryType) || "");
    const [isVariableCalculation, setIsvariablecalculation] = useState(specsData.isVariableCalculation || renewApplication?.isVariableCalculation || false);
    const [isbulkgeneration, setIsbulkgeneration] = useState(specsData.isbulkgeneration || renewApplication?.isbulkgeneration || false);
    const [no_of_units, setNoOfUnits] = useState(specsData.no_of_units || renewApplication?.grbgCollectionUnits?.[0]?.no_of_units || "");
    const [isAdditional, setIsAdditional] = useState(specsData.isAdditional || renewApplication?.isAdditional || false);
    const [isInheritance, setIsInheritance] = useState(specsData.isInheritance || renewApplication?.grbgCollectionUnits?.[0]?.isInheritance || false);

    // --- Subcategory list: filter SubCategories master by the selected category's code ---
    const UniqueSubCategories = subCategoriesData
        ?.filter((item) => item.category === category?.code)
        ?.map((item) => ({
            i18nKey: item.subcategory,
            code: item.subcategory,
            name: item.name,
        })) || [];

    // --- Subcategory-type list: pulled straight from the matched subcategory's nested array ---
    const matchedSubCategory = subCategoriesData?.find(
        (item) => item.category === category?.code && item.subcategory === subCategory?.code
    );

    const UniqueSubCategoryTypes = matchedSubCategory?.subcategorytype
        ?.filter((typeItem) => typeItem.active !== false)
        ?.map((typeItem) => ({
            i18nKey: typeItem.code,
            code: typeItem.code,
            name: typeItem.name,
            isAdditional: typeItem.isAdditonalCostAdded === true,
        })) || [];

    const handleCategoryChange = (val) => {
        setCategory(val);
        setSubCategory("");
        setSubCategoryType("");
    };

    const handleSubCategoryChange = (val) => {
        setSubCategory(val);
        setSubCategoryType("");
    };

    const handleSubCategoryTypeChange = (val) => {
        const selectedType = UniqueSubCategoryTypes.find((item) => item.code === val?.code);

        setSubCategoryType(val);
        setIsAdditional(selectedType?.isAdditional || false);

        setIsvariablecalculation(false);
        setIsbulkgeneration(false);
    };

    const goNext = () => {
        const emailRegex = /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/;

        if (email && !emailRegex.test(email)) {
            alert("Please enter a valid email address");
            return;
        }

        const data = {
            oldGarbageId,
            typeOfCollection,
            propertyOwnerType,
            name,
            phoneNumber,
            gender,
            email,
            category,
            subCategory,
            subCategoryType,
            isVariableCalculation,
            isbulkgeneration,
            no_of_units,
            isAdditional,
            isInheritance,
        };

        onSelect(config.key, data, false);
    };

    return (
        <React.Fragment>
            <FormStep
                config={config}
                onSelect={goNext}
                t={t}
                isDisabled={
                    !typeOfCollection ||
                    !propertyOwnerType ||
                    !name ||
                    !phoneNumber ||
                    !gender ||
                    !category ||
                    !subCategory ||
                    !subCategoryType ||
                    (multiUnits.includes(typeOfCollection?.code) && !no_of_units)
                }
            >
                <div className="gc-form-step-wrapper">
                    <div className="gc-form-field-wrap">
                        <CardLabel>{t("GC_OLD_GARBAGE_ID")}</CardLabel>
                        <TextInput
                            value={oldGarbageId}
                            placeholder="Enter Old Garbage ID (optional)"
                            onChange={(e) => {
                                const value = e.target.value.replace(/\D/g, "");
                                setOldGarbageId(value);
                                if (!value) setIsInheritance(false);
                            }}
                        />
                    </div>

                    <div className="gc-form-field-wrap">
                        <CardLabel>
                            {t("GC_TYPE_OF_COLLECTION")} <span className="gc-required-star">*</span>
                        </CardLabel>
                        <Dropdown
                            option={CollectionTypes}
                            optionKey="i18nKey"
                            selected={typeOfCollection}
                            select={(val) => {
                                setTypeOfCollection(val);
                                if (!multiUnits.includes(val?.code)) setNoOfUnits("");
                            }}
                            placeholder={t("GC_SELECT_TYPE")}
                            t={t}
                        />
                    </div>

                    <div className="gc-form-field-wrap">
                        <CardLabel>
                            {t("GC_OWNER_OR_TENANT")} <span className="gc-required-star">*</span>
                        </CardLabel>
                        <Dropdown
                            option={OwnerTypes}
                            optionKey="i18nKey"
                            selected={propertyOwnerType}
                            select={(val) => {
                                setPropertyOwnerType(val);
                                setPhoneNumber(isOwner(val) ? applicantPhoneNumber : "");
                            }}
                            placeholder="Select Owner or Tenant"
                            t={t}
                        />
                    </div>

                    {multiUnits.includes(typeOfCollection?.code) && (
                        <div className="gc-form-field-wrap">
                            <CardLabel>
                                {t("GC_NO_OF_UNITS")} <span className="gc-required-star">*</span>
                            </CardLabel>
                            <TextInput
                                value={no_of_units}
                                placeholder="Enter Number of Units"
                                onChange={(e) => setNoOfUnits(e.target.value.replace(/\D/g, ""))}
                            />
                        </div>
                    )}

                    <div className="gc-form-field-wrap">
                        <CardLabel>
                            {t("GC_NAME")} <span className="gc-required-star">*</span>
                        </CardLabel>
                        <TextInput
                            value={name}
                            placeholder="Enter Name"
                            onChange={(e) => setName(e.target.value.replace(/[^A-Za-z ]/g, ""))}
                        />
                    </div>

                    <div className="gc-form-field-wrap">
                        <CardLabel>
                            {t("GC_PHONE_NUMBER")} <span className="gc-required-star">*</span>
                        </CardLabel>
                        <TextInput
                            type="text"
                            inputMode="numeric"
                            maxLength={10}
                            value={phoneNumber}
                            placeholder={t("GC_PHONE_NO_DISCLAIMER") || "Enter 10 digit mobile number"}
                            onChange={(e) => {
                                let value = e.target.value.replace(/\D/g, "").slice(0, 10);
                                if (value.length > 0 && !/^[5-9]/.test(value)) value = "";
                                setPhoneNumber(value);
                            }}
                        />
                    </div>

                    <div className="gc-form-field-wrap">
                        <CardLabel>
                            {t("GC_GENDER")} <span className="gc-required-star">*</span>
                        </CardLabel>
                        <Dropdown
                            option={Genders}
                            optionKey="i18nKey"
                            selected={gender}
                            select={setGender}
                            placeholder="Select Gender"
                            t={t}
                        />
                    </div>

                    <div className="gc-form-field-wrap">
                        <CardLabel>{t("GC_EMAIL")}</CardLabel>
                        <TextInput
                            value={email}
                            placeholder="Enter Email Address"
                            onChange={(e) => setEmail(e.target.value.trim())}
                        />
                    </div>

                    <div className="gc-form-field-wrap">
                        <CardLabel>
                            {t("GC_CATEGORY")} <span className="gc-required-star">*</span>
                        </CardLabel>
                        <Dropdown
                            option={UniqueCategories}
                            optionKey="i18nKey"
                            selected={category}
                            select={handleCategoryChange}
                            placeholder="Select Category"
                            t={t}
                        />
                    </div>

                    <div className="gc-form-field-wrap">
                        <CardLabel>
                            {t("GC_SUB_CATEGORY")} <span className="gc-required-star">*</span>
                        </CardLabel>
                        <Dropdown
                            option={UniqueSubCategories}
                            optionKey="i18nKey"
                            selected={subCategory}
                            select={handleSubCategoryChange}
                            placeholder="Select Sub Category"
                            t={t}
                        />
                    </div>

                    <div className="gc-form-field-wrap">
                        <CardLabel>
                            {t("GC_SUB_CATEGORY_TYPE")} <span className="gc-required-star">*</span>
                        </CardLabel>
                        <Dropdown
                            option={UniqueSubCategoryTypes}
                            optionKey="i18nKey"
                            selected={subCategoryType}
                            select={handleSubCategoryTypeChange}
                            placeholder="Select Sub Category Type"
                            t={t}
                        />
                    </div>

                    {oldGarbageId && oldGarbageId.trim().length > 0 && (
                        <div className="gc-form-field-wrap">
                            <CheckBox
                                label={t("GC_IS_INHERITANCE")}
                                checked={isInheritance}
                                onChange={(e) => setIsInheritance(e.target.checked)}
                            />
                        </div>
                    )}

                    {isAdditional && (
                        <div className="gc-form-field-wrap">
                            <CardLabel>{t("GC_CALCULATION_TYPE")}</CardLabel>
                            <div style={{ display: "flex", gap: "16px", marginBottom: "16px" }}>
                                <CheckBox
                                    label={t("GC_FIXED")}
                                    checked={isbulkgeneration}
                                    onChange={(e) => {
                                        setIsbulkgeneration(e.target.checked);
                                        if (e.target.checked) setIsvariablecalculation(false);
                                    }}
                                />
                                <CheckBox
                                    label={t("GC_VARIABLE")}
                                    checked={isVariableCalculation}
                                    onChange={(e) => {
                                        setIsvariablecalculation(e.target.checked);
                                        if (e.target.checked) setIsbulkgeneration(false);
                                    }}
                                />
                            </div>
                        </div>
                    )}

                    {isAdditional && isVariableCalculation && (
                        <div className="gc-form-field-wrap">
                            <CardLabel>{t("GC_NO_OF_UNITS")}</CardLabel>
                            <TextInput
                                value={no_of_units}
                                placeholder="Enter Number of Units"
                                onChange={(e) => setNoOfUnits(e.target.value.replace(/\D/g, ""))}
                            />
                        </div>
                    )}
                </div>
            </FormStep>
        </React.Fragment>
    );
};

export default GCSpecifications;
