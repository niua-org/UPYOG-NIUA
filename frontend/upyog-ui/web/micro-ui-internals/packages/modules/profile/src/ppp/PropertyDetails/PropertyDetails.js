import React, { useMemo, useState } from "react";
import { createPortal } from "react-dom";
import { useSearchParams } from "react-router-dom";
import { FormStep, Card, CardHeader, CardLabel, Dropdown, EditIcon, DeleteIcon, RadioButtons, SubmitBar, Toast } from "@nudmcdgnpm/digit-ui-react-components";
import { profileStyles, propertyDetailsResponsiveCss } from "../../utils/profileStyles";
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
const PropertyDetails = ({ t, config, onSelect, onUpdate, formData }) => {
  const styles = profileStyles.propertyDetails;
  const [searchParams] = useSearchParams();
  const isViewMode = searchParams.get("mode") === "view" || !!config?.isViewMode;
  const saved = formData?.[config?.key] || {};
  
  

  const initialPortfolio = useMemo(() => {
    if (Array.isArray(saved.propertiesPortfolio) && saved.propertiesPortfolio.length) {
      return [...saved.propertiesPortfolio].sort((first, second) =>
        Number(Boolean(second.isPrimaryAddress)) - Number(Boolean(first.isPrimaryAddress))
      );
    }
    if (saved.localityType) return [{ ...saved, portfolioId: `property-${Date.now()}` }];
    return [];
  }, []);

  const [properties, setProperties] = useState(initialPortfolio);
  const [noProperty, setNoProperty] = useState(saved.noProperty === true);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editingIndex, setEditingIndex] = useState(null);
  const [localityType, setLocalityType] = useState(null);
  const [draft, setDraft] = useState({});
  const [toast, setToast] = useState(null);

  const persistProperties = (nextProperties) => {
    const primaryFirst = [...nextProperties].sort((first, second) =>
      Number(Boolean(second.isPrimaryAddress)) - Number(Boolean(first.isPrimaryAddress))
    );
    setProperties(primaryFirst);
    onUpdate?.(config.key, {
      propertiesPortfolio: primaryFirst,
      localityType: primaryFirst[0]?.localityType || "",
      noProperty: false,
      primaryPropertyId:
        primaryFirst.find((property) => property.localityType === "URBAN" && property.isPrimaryAddress)?.propertyId || "",
    });
  };

  const openAddProperty = () => {
    if (isViewMode) return;
    setNoProperty(false);
    onUpdate?.(config.key, { noProperty: false });
    setEditingIndex(null);
    setLocalityType(null);
    setDraft({});
    setDrawerOpen(true);
  };

  const openEditProperty = (index) => {
    if (isViewMode) return;
    const property = properties[index];
    const selectedLocality = LOCALITY_TYPE_OPTIONS.find((option) => option.code === property.localityType) || null;
    setEditingIndex(index);
    setLocalityType(selectedLocality);
    setDraft({ ...property });
    setDrawerOpen(true);
  };

  const closeDrawer = () => {
    setDrawerOpen(false);
    setEditingIndex(null);
    setLocalityType(null);
    setDraft({});
  };

  const handleLocalityTypeChange = (value) => {
    setLocalityType(value);
    setDraft((previous) => ({ localityType: value?.code }));
  };

  const updateDraft = (data) => {
    setDraft((previous) => ({ ...previous, ...data }));
  };

  const isUrbanValid = () => {
    const urbanProperties = draft.properties || [];
    return Boolean(draft.propertyId || urbanProperties[0]?.propertyId);
  };

  const isRuralValid = () =>
    draft.gramPanchayat && draft.village && draft.ward && draft.propertyAddress &&
    draft.propertyType && draft.propertyUsage && draft.surveyNumber && draft.ownershipType &&
    draft.documentType && draft.propertyDocument &&
    (draft.ownershipType !== "JOINT" || (draft.ownerName && draft.ownerMobile));

  const canSaveProperty =
    localityType?.code === "URBAN" ? isUrbanValid() :
    localityType?.code === "RURAL" ? isRuralValid() : false;

  const saveProperty = () => {
    if (!canSaveProperty) return;
    const savedProperties = localityType.code === "URBAN"
      ? (() => {
          const urbanProperty = draft.properties?.[0] || draft;
          const propertyId = urbanProperty.propertyId || draft.propertyId;
          const isPrimaryAddress = Boolean(urbanProperty.isPrimaryAddress || draft.isPrimaryAddress);
          const existingIndex = properties.findIndex(
            (property, index) =>
              index !== editingIndex &&
              property.localityType === "URBAN" &&
              property.propertyId?.toLowerCase() === propertyId?.toLowerCase()
          );

          if (existingIndex !== -1) {
            setToast({ error: true, label: t("PROFILE_PROPERTY_ALREADY_ADDED", "This property has already been added.") });
            return null;
          }

          return [{
            ...urbanProperty,
            localityType: "URBAN",
            propertyId,
            isPrimaryAddress,
            primaryPropertyId: isPrimaryAddress ? propertyId : "",
            portfolioId: editingIndex !== null
              ? properties[editingIndex].portfolioId
              : `property-${Date.now()}`,
          }];
        })()
      : [{
          ...draft,
          localityType: localityType.code,
          portfolioId: editingIndex !== null
            ? properties[editingIndex].portfolioId
            : `property-${Date.now()}`,
        }];

    if (!savedProperties) return;
    const nextProperties = [...properties];

    if (editingIndex !== null) {
      nextProperties.splice(editingIndex, 1, ...savedProperties);
    } else {
      nextProperties.push(...savedProperties);
    }

    if (savedProperties.some((property) => property.localityType === "URBAN" && property.isPrimaryAddress)) {
      nextProperties.forEach((property, index) => {
        if (property.localityType === "URBAN") {
          nextProperties[index] = { ...property, isPrimaryAddress: false, primaryPropertyId: "" };
        }
      });
      const newPrimary = nextProperties.find((property) => property.portfolioId === savedProperties[0].portfolioId);
      if (newPrimary) {
        const primaryIndex = nextProperties.indexOf(newPrimary);
        nextProperties[primaryIndex] = { ...newPrimary, isPrimaryAddress: true };
      }
    }

    persistProperties(nextProperties);
    closeDrawer();
  };

  const deleteProperty = (index) => {
    persistProperties(properties.filter((_, propertyIndex) => propertyIndex !== index));
  };

  const setPrimaryProperty = (index) => {
    const nextProperties = properties.map((property, propertyIndex) => ({
      ...property,
      isPrimaryAddress: property.localityType === "URBAN" && propertyIndex === index,
      primaryPropertyId:
        property.localityType === "URBAN" && propertyIndex === index ? property.propertyId : "",
    }));
    persistProperties(nextProperties);
  };

  const handlePropertyOwnershipChange = (option) => {
    const selectingNoProperty = option.code === "NO";
    setNoProperty(selectingNoProperty);
    setToast(null);

    if (selectingNoProperty) {
      setProperties([]);
      onUpdate?.(config.key, {
        propertiesPortfolio: [],
        localityType: "",
        primaryPropertyId: "",
        noProperty: true,
      });
    } else {
      onUpdate?.(config.key, { noProperty: false });
    }
  };

  const getPropertyTitle = (property) => {
    if (property.localityType === "RURAL") {
      return property.village?.name || property.village || "Rural property";
    }
    return `Property ID: ${property.propertyId || property.primaryPropertyId || "Urban property"}`;
  };

  const getPropertyLocation = (property) => {
    if (property.localityType === "RURAL") {
      const gramPanchayat = property.gramPanchayat?.name || property.gramPanchayat;
      const village = property.village?.name || property.village;
      return [village, gramPanchayat].filter(Boolean).join(", ") || "Rural locality";
    }
    return property.propertyAddress || "Urban locality";
  };

  const goNext = () => {
    if (noProperty) {
      onSelect(config.key, {
        propertiesPortfolio: [],
        localityType: "",
        primaryPropertyId: "",
        noProperty: true,
      }, false);
      return;
    }
    if (!properties.length) return;
    const hasUrbanProperty = properties.some((property) => property.localityType === "URBAN");
    const hasPrimaryUrbanProperty = properties.some(
      (property) => property.localityType === "URBAN" && property.isPrimaryAddress
    );

    if (hasUrbanProperty && !hasPrimaryUrbanProperty) {
      setToast({
        error: true,
        label: t(("PROFILE_SELECT_PRIMARY_URBAN_PROPERTY")),
      });
      return;
    }

    onSelect(config.key, {
      propertiesPortfolio: properties,
      localityType: properties[0].localityType,
      noProperty: false,
      primaryPropertyId:
        properties.find((property) => property.localityType === "URBAN" && property.isPrimaryAddress)?.propertyId || "",
    }, false);
  };
  const hasUrbanProperty = properties.some((property) => property.localityType === "URBAN");
  const hasPrimaryUrbanProperty = properties.some(
    (property) => property.localityType === "URBAN" && property.isPrimaryAddress
  );
  const canProceed = noProperty || (properties.length > 0 && (!hasUrbanProperty || hasPrimaryUrbanProperty));
  const propertyOwnershipOptions = [
    { code: "YES", name: t(("PROFILE_PROPERTY_OWNED_YES"))},
    { code: "NO", name: t(("PROFILE_PROPERTY_OWNED_NO"))},
  ];

  return (
    <React.Fragment>
      <style>{propertyDetailsResponsiveCss}</style>
      <div><FamilyCreateTabs t={t} /></div>
      <Card style={styles.container}>
        <CardHeader>{t(("ES_TITILE_PROPERTY_DETAILS"))}</CardHeader>
        <p>{t(("PROFILE_PROPERTY_PORTFOLIO_SUBTITLE"))}</p>

        <div style={styles.propertyOwnershipQuestion}>
          <CardLabel>{t(("PROFILE_DO_YOU_OWN_PROPERTY"))}</CardLabel>
          <RadioButtons  name="ppp-property-ownership" options={propertyOwnershipOptions} optionsKey="name"
            selectedOption={propertyOwnershipOptions.find((option) => option.code === (noProperty ? "NO" : "YES"))}
            onSelect={handlePropertyOwnershipChange} disabled={isViewMode} innerStyles={{ display: "flex", alignItems: "center", marginRight: "24px" }}
          />
        </div>

        {!isViewMode && !noProperty && (
          <div style={styles.addPropertySticky}>
            <button type="button" style={styles.addPropertyButton} onClick={openAddProperty}> + Add Property</button>
          </div>
        )}

        {noProperty ? (
          <div style={styles.emptyState}>{t(("PROFILE_NO_PROPERTY_DECLARED"))} </div>
        ) : properties.length === 0 ? (
          <div style={styles.emptyState}>
            {t(("PROFILE_NO_PROPERTIES_ADDED"))}
          </div>
        ) : (
          <div className="profile-property-summary-list" style={styles.list}>
            {properties.map((property, index) => (
              <div className="profile-property-summary-card" key={property.portfolioId || index} style={styles.summaryCard}>
                <div>
                  <div style={styles.summaryTitle}>
                    {getPropertyTitle(property)}
                    {property.localityType === "URBAN" && property.isPrimaryAddress && (
                      <span style={styles.primaryBadge}>{t(("PROFILE_PRIMARY_ADDRESS"))}</span>
                    )}
                  </div>
                  <div style={styles.summaryLocation}>Location: {getPropertyLocation(property)}</div>
                  <div style={styles.summaryType}>
                    <span>Type:</span>{" "}
                    <span style={property.localityType === "RURAL" ? styles.ruralBadge : styles.urbanBadge}>
                      {property.localityType === "RURAL" ? "🌾 Rural" : "🏢 Urban"}
                    </span>
                  </div>
                  {property.localityType === "RURAL" && property.ownershipType && (
                    <div style={styles.summaryMeta}>Ownership: {property.ownershipType === "JOINT" ? "Joint" : property.ownershipType}</div>
                  )}
                </div>
                {!isViewMode && (
                  <div className="profile-property-summary-actions" style={styles.cardActions}>
                    {property.localityType === "URBAN" && !property.isPrimaryAddress && (
                      <button
                        type="button"
                        style={styles.primaryActionButton}
                        onClick={() => setPrimaryProperty(index)}
                      >
                        {t(("PROFILE_MARK_AS_PRIMARY"))}
                      </button>
                    )}
                    <button type="button" style={styles.iconButton} onClick={() => openEditProperty(index)} aria-label="Edit property">
                      <EditIcon />
                    </button>
                    <button type="button" style={styles.iconButton} onClick={() => deleteProperty(index)} aria-label="Delete property">
                      <DeleteIcon fill="#D4353A" />
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}

        <div style={styles.saveNextWrapper}>
          <SubmitBar label={t(isViewMode ? "COMMON_NEXT" : (config?.texts?.submitBarLabel || "COMMON_SAVE_NEXT"))}
            onSubmit={goNext} disabled={!isViewMode && !canProceed} />
        </div>
      </Card>

      {toast && (<Toast error={toast.error} label={toast.label}  onClose={() => setToast(null)}/> )}
      {drawerOpen && createPortal(
        <div style={styles.drawerOverlay} role="dialog" aria-modal="true" aria-label="Add Property">
          <div className="profile-property-drawer" style={styles.drawer}>
            <div style={styles.drawerHeader}>
              <h2 style={{ margin: 0 }}>{editingIndex !== null ? "Edit Property" : "Add Property"}</h2>
              <button type="button" style={styles.closeButton} onClick={closeDrawer} aria-label="Close">×</button>
            </div>

            <div className="profile-property-drawer-body" style={styles.drawerBody}>
              <CardLabel>Locality Type <span className="astericColor">*</span></CardLabel>
              <Dropdown selected={localityType} select={handleLocalityTypeChange} option={LOCALITY_TYPE_OPTIONS}
                optionKey="name" placeholder="Urban/Rural" />
              {localityType?.code === "URBAN" && (<UrbanPropertyForm t={t} data={draft} onChange={updateDraft}
                  existingPropertyIds={properties.filter((property, index) => index !== editingIndex && property.localityType === "URBAN")
                    .map((property) => property.propertyId)}
                />
              )}
              {localityType?.code === "RURAL" && (<RuralPropertyForm t={t} data={draft} onChange={updateDraft} />)}
            </div>

            <div className="profile-property-drawer-footer" style={styles.drawerFooter}>
              <button type="button" style={styles.drawerCancelButton} onClick={closeDrawer}>
                {t(("CORE_COMMON_CANCEL"))}
              </button>
              <SubmitBar label={t(("PROFILE_SAVE_PROPERTY"))} onSubmit={saveProperty} disabled={!canSaveProperty} />
            </div>
          </div>
        </div>,
        document.body
      )}
    </React.Fragment>
  );
};

export default PropertyDetails;