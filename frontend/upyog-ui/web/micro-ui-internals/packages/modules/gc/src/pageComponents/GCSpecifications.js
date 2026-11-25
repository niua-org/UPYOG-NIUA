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
 * - Always shows the option to add tenants, even when property has only 0 floor (Ground Floor)
 * - Supports 3 Occupancy Scenarios:
 *   1. Owner lives here + Rents to tenants (OWNER_AND_TENANTS) -> Ground Floor 0 available for tenants, or Unit 2 / upper floors
 *   2. Entire property rented to tenants (FULLY_RENTED) -> Tenant occupies Ground Floor 0, Owner is non-resident (floor=null)
 *   3. Owner occupies entire property alone (FULL_OWNER) -> 1 unified application, no tenants
 */
const GCSpecifications = ({ t, config, onSelect, formData }) => {
    // 1. Context & Form Data Variables at the top
    const user = Digit.UserService.getUser().info;
    const stateId = Digit.ULBService.getStateId();
    const specsData = formData?.[config?.key] || {};
    const propertyLoc = formData?.gcpropertylocdetails;
    const detectedFloorCount = Number(propertyLoc?.detectedFloorCount) || 1;
    const inputStyles = { width: user?.type === "EMPLOYEE" ? "50%" : "100%" };

    const {
        oldGarbageId: initOldGarbageId = "",
        typeOfCollection: initTypeOfCollection = "",
        category: initCategory = "",
        subCategory: initSubCategory = "",
        subCategoryType: initSubCategoryType = "",
        specialCategory: initSpecialCategory = "",
        occupancyType: initOccupancyType = "",
        ownerFloor: initOwnerFloor = "0",
        no_of_units: initNoOfUnits = 1,
        isInheritance: initIsInheritance = false,
        tenants: initTenants = [],
    } = specsData;

    // Default to OWNER_AND_TENANTS so tenant add-on option is ALWAYS visible by default
    const defaultOccupancy = initOccupancyType || (
        initOwnerFloor === "NONE"
            ? "FULLY_RENTED"
            : (initTenants && initTenants.length > 0 ? "OWNER_AND_TENANTS" : "OWNER_AND_TENANTS")
    );

    // 2. React State Declarations at the top
    const [oldGarbageId, setOldGarbageId] = useState(initOldGarbageId);
    const [typeOfCollection, setTypeOfCollection] = useState(initTypeOfCollection);
    const [category, setCategory] = useState(initCategory);
    const [subCategory, setSubCategory] = useState(initSubCategory);
    const [subCategoryType, setSubCategoryType] = useState(initSubCategoryType);
    const [specialCategory, setSpecialCategory] = useState(initSpecialCategory);
    const [occupancyType, setOccupancyType] = useState(defaultOccupancy);
    const [ownerFloor, setOwnerFloor] = useState(initOwnerFloor || "0");
    const [no_of_units, setNoOfUnits] = useState(initNoOfUnits || (detectedFloorCount > 1 ? detectedFloorCount : 1));
    const [isInheritance, setIsInheritance] = useState(initIsInheritance);
    const [tenants, setTenants] = useState(initTenants);
    const [toast, setToast] = useState(null);

    // 3. MDMS Data Fetch
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

    // 3 Building Occupancy Scenarios
    const occupancyOptions = useMemo(() => [
        {
            code: "OWNER_AND_TENANTS",
            i18nKey: "GC_OCCUPANCY_OWNER_AND_TENANTS",
            name: t("GC_OCCUPANCY_OWNER_AND_TENANTS") || "Owner lives here + Rents to tenants (Add Tenant)",
        },
        {
            code: "FULLY_RENTED",
            i18nKey: "GC_OCCUPANCY_FULLY_RENTED",
            name: t("GC_OCCUPANCY_FULLY_RENTED") || "Entire property rented to tenants (Owner does not reside)",
        },
        {
            code: "FULL_OWNER",
            i18nKey: "GC_OCCUPANCY_FULL_OWNER",
            name: t("GC_OCCUPANCY_FULL_OWNER") || "Owner occupies entire property alone (No tenants)",
        },
    ], [t]);

    // Physical floor options (for owner)
    const physicalFloorOptions = useMemo(() => {
        const options = [
            { code: "0", i18nKey: "GC_FLOOR_0", name: t("GC_FLOOR_GROUND") || "Ground Floor (0)", value: "0" },
        ];
        const maxFloors = Math.max(2, (detectedFloorCount || 1));
        for (let i = 1; i < maxFloors; i++) {
            options.push({
                code: String(i),
                i18nKey: `GC_FLOOR_${i}`,
                name: `${i}${i === 1 ? "st" : i === 2 ? "nd" : i === 3 ? "rd" : "th"} Floor (${i})`,
                value: String(i),
            });
        }
        return options;
    }, [detectedFloorCount, t]);

    // Floor options for tenants - Ground Floor (0) is ALWAYS available!
    const tenantFloorOptions = useMemo(() => {
        const maxFloors = Math.max(5, (detectedFloorCount || 1) + 2);
        const options = [];

        // Ground Floor (0) is ALWAYS available for tenant selection!
        options.push({
            code: "0",
            i18nKey: "GC_FLOOR_0",
            name: t("GC_FLOOR_GROUND") || "Ground Floor (0)",
            value: "0",
        });

        // Portion / Unit 2 on Ground Floor (if owner and tenant both live on ground floor)
        options.push({
            code: "0-B",
            i18nKey: "GC_FLOOR_0_UNIT_2",
            name: t("GC_FLOOR_0_UNIT_2") || "Ground Floor - Portion / Unit 2 (0-B)",
            value: "0-B",
        });

        for (let i = 1; i <= Math.max(maxFloors, 10); i++) {
            options.push({
                code: String(i),
                i18nKey: `GC_FLOOR_${i}`,
                name: `${i}${i === 1 ? "st" : i === 2 ? "nd" : i === 3 ? "rd" : "th"} Floor (${i})`,
                value: String(i),
            });
        }
        return options;
    }, [detectedFloorCount, t]);

    const isMultiCollection = multiUnits.includes(typeOfCollection?.code);

    const handleOccupancyChange = (val) => {
        setOccupancyType(val.code);
        if (val.code === "FULL_OWNER") {
            setOwnerFloor("0");
            setTenants([]);
        } else if (val.code === "FULLY_RENTED") {
            setOwnerFloor("NONE");
        } else if (val.code === "OWNER_AND_TENANTS") {
            setOwnerFloor("0");
        }
    };

    const goNext = () => {
        // Validation: If FULLY_RENTED (Scenario 3), at least 1 tenant must be present
        if (isMultiCollection && occupancyType === "FULLY_RENTED" && (!tenants || tenants.length === 0)) {
            setToast({
                error: true,
                label: t("GC_ERR_TENANT_REQUIRED_WHEN_OWNER_NONE") || "Please add at least one tenant when the property is fully rented out.",
            });
            return;
        }

        // Check duplicate mobiles among tenants
        const tenantMobiles = tenants.map((tn) => tn.mobileNumber?.trim()).filter(Boolean);
        if (new Set(tenantMobiles).size !== tenantMobiles.length) {
            setToast({ error: true, label: t("GC_DUPLICATE_TENANT_MOBILE_ERR") || "Duplicate tenant mobile numbers found." });
            return;
        }

        // Check duplicate floors among tenants
        const tenantFloors = tenants.map((tn) => String(tn.floorNo)).filter(Boolean);
        if (new Set(tenantFloors).size !== tenantFloors.length) {
            setToast({ error: true, label: t("GC_DUPLICATE_TENANT_FLOOR_ERR") || "Duplicate tenant floor assignments found." });
            return;
        }

        // Collision safety: If tenant occupies Ground Floor "0", owner automatically becomes master (NONE)
        let effectiveOwnerFloor = ownerFloor;
        if (occupancyType === "FULL_OWNER" || !isMultiCollection) {
            effectiveOwnerFloor = "0";
        } else if (occupancyType === "FULLY_RENTED" || tenantFloors.includes("0")) {
            effectiveOwnerFloor = "NONE";
        }

        const effectiveTenants =
            !isMultiCollection || occupancyType === "FULL_OWNER"
                ? []
                : tenants;

        const data = {
            oldGarbageId,
            typeOfCollection,
            category,
            subCategory,
            subCategoryType,
            specialCategory,
            occupancyType: isMultiCollection ? occupancyType : "FULL_OWNER",
            ownerFloor: effectiveOwnerFloor,
            no_of_units: isMultiCollection ? (Number(no_of_units) || 1) : 1,
            isInheritance,
            detectedFloorCount,
            tenants: effectiveTenants,
        };

        onSelect(config.key, data, false);
    };

    const hasValidUnits = !isMultiCollection ? true : Boolean(no_of_units);
    const isFormValid =
        typeOfCollection &&
        category &&
        subCategory &&
        subCategoryType &&
        hasValidUnits &&
        (!isMultiCollection || occupancyType !== "FULLY_RENTED" || (tenants && tenants.length > 0)) &&
        tenants.every((tUnit) => tUnit.floorNo && tUnit.name?.trim().length >= 2 && /^[5-9]\d{9}$/.test(tUnit.mobileNumber));

    const selectedOccupancyObj = occupancyOptions.find((o) => o.code === occupancyType) || occupancyOptions[0];
    const selectedOwnerFloorObj = physicalFloorOptions.find((f) => f.code === ownerFloor) || physicalFloorOptions[0];

    return (
        <React.Fragment>
            <FormStep config={config} onSelect={goNext} t={t} isDisabled={!isFormValid}>
                <div>
                    {/* Card 1: Owner Collection Specifications */}
                    <CardSubHeader className="gc-spec-subheader">{t("GC_OWNER_SPECIFICATIONS_HEADER") || "Property & Collection Specifications"}</CardSubHeader>

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
                        optionKey="name"
                        selected={typeOfCollection}
                        select={(val) => {
                            setTypeOfCollection(val);
                            if (!multiUnits.includes(val?.code)) {
                                setNoOfUnits(1);
                            }
                        }}
                        placeholder={t("GC_SELECT_TYPE")}
                        style={inputStyles}
                        t={t}
                    />

                    {/* Multi-Collection Occupancy Scenarios */}
                    {isMultiCollection && (
                        <>
                            <CardLabel>{t("GC_BUILDING_OCCUPANCY") || "Building Occupancy Pattern"} <span className="astericColor">*</span></CardLabel>
                            <Dropdown
                                option={occupancyOptions}
                                optionKey="name"
                                selected={selectedOccupancyObj}
                                select={handleOccupancyChange}
                                style={inputStyles}
                                t={t}
                            />

                            {/* Scenario 1 Notice */}
                            {occupancyType === "FULL_OWNER" && (
                                <div style={{ padding: "12px", background: "#f3f2f1", borderLeft: "4px solid #1d70b8", margin: "12px 0 16px 0" }}>
                                    <p style={{ margin: 0, fontSize: "14px", color: "#0b0c0c" }}>
                                        <strong>{t("GC_NOTE") || "Note"}:</strong> {t("GC_FULL_OWNER_INFO") || "The property owner occupies the entire building. A single unified garbage collection connection will be created for the property."}
                                    </p>
                                </div>
                            )}

                            {/* Scenario 2 Owner Floor Selection */}
                            {occupancyType === "OWNER_AND_TENANTS" && (
                                <>
                                    <CardLabel>{t("GC_OWNER_OCCUPIED_FLOOR") || "Floor Occupied by Owner"} <span className="astericColor">*</span></CardLabel>
                                    <Dropdown
                                        option={physicalFloorOptions}
                                        optionKey="name"
                                        selected={selectedOwnerFloorObj}
                                        select={(val) => setOwnerFloor(val.code)}
                                        style={inputStyles}
                                        t={t}
                                    />
                                </>
                            )}

                            {/* Scenario 3 Fully Rented Notice */}
                            {occupancyType === "FULLY_RENTED" && (
                                <div style={{ padding: "12px", background: "#f3f2f1", borderLeft: "4px solid #00703c", margin: "12px 0 16px 0" }}>
                                    <p style={{ margin: 0, fontSize: "14px", color: "#0b0c0c" }}>
                                        <strong>{t("GC_NOTE") || "Note"}:</strong> {t("GC_FULLY_RENTED_INFO") || "The owner does not reside on the property. All floors (including Ground Floor) can be allocated to tenants below."}
                                    </p>
                                </div>
                            )}

                            <CardLabel>{t("GC_NO_OF_UNITS")} <span className="astericColor">*</span></CardLabel>
                            <TextInput value={no_of_units} style={inputStyles} onChange={(e) => setNoOfUnits(e.target.value.replace(/\D/g, ""))} />
                        </>
                    )}

                    <CardLabel>{t("GC_CATEGORY")} <span className="astericColor">*</span></CardLabel>
                    <Dropdown
                        option={categoryOptions}
                        optionKey="name"
                        selected={category}
                        select={(val) => { setCategory(val); setSubCategory(""); setSubCategoryType(""); }}
                        style={inputStyles}
                        t={t}
                    />

                    <CardLabel>{t("GC_SUB_CATEGORY")} <span className="astericColor">*</span></CardLabel>
                    <Dropdown
                        option={subCategoryOptions}
                        optionKey="name"
                        selected={subCategory}
                        select={(val) => { setSubCategory(val); setSubCategoryType(""); }}
                        style={inputStyles}
                        t={t}
                    />

                    <CardLabel>{t("GC_SUB_CATEGORY_TYPE")} <span className="astericColor">*</span></CardLabel>
                    <Dropdown
                        option={subCategoryTypeOptions}
                        optionKey="name"
                        selected={subCategoryType}
                        select={(val) => setSubCategoryType(val)}
                        style={inputStyles}
                        t={t}
                    />

                    <CardLabel>{t("GC_SPECIAL_CATEGORY")} <span className="astericColor">*</span></CardLabel>
                    <Dropdown option={specialCategoryOptions} optionKey="name" selected={specialCategory} select={(val) => setSpecialCategory(val)} style={inputStyles} t={t} />

                    {oldGarbageId && oldGarbageId.trim().length > 0 && (
                        <div className="gc-inheritance-wrapper">
                            <CheckBox label={t("GC_IS_INHERITANCE")} checked={isInheritance} onChange={(e) => setIsInheritance(e.target.checked)} />
                        </div>
                    )}

                    {/* Card 2: Dynamic Floor Tenant Add-on - ALWAYS visible and available to add tenants! */}
                    {isMultiCollection && (
                        <GCTenantDetails
                            t={t}
                            user={user}
                            tenants={tenants}
                            setTenants={(newTenants) => {
                                setTenants(newTenants);
                                if (Array.isArray(newTenants) && newTenants.length > 0 && occupancyType === "FULL_OWNER") {
                                    setOccupancyType("OWNER_AND_TENANTS");
                                }
                            }}
                            detectedFloorCount={detectedFloorCount}
                            floorOptions={tenantFloorOptions}
                            ownerFloor={occupancyType === "FULLY_RENTED" ? "NONE" : ownerFloor}
                            setToast={setToast}
                        />
                    )}
                </div>
            </FormStep>

            {toast && (
                <Toast error={toast.error} warning={toast.warning} label={toast.label} onClose={() => setToast(null)} />
            )}
        </React.Fragment>
    );
};

export default GCSpecifications;
