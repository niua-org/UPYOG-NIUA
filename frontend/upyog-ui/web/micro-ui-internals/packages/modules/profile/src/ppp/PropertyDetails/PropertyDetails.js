import React, { useMemo, useState } from "react";
import { createPortal } from "react-dom";
import { useSearchParams } from "react-router-dom";
import { FormStep, Card, CardHeader, CardLabel, Dropdown, EditIcon, DeleteIcon, RadioButtons, SubmitBar, Toast } from "@nudmcdgnpm/digit-ui-react-components";
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

const getPropertyAddressId = (property) =>
  property?.propertyId || property?.primaryPropertyId || property?.surveyNumber || property?.khasraNumber || "";

/**
 * Collects and manages a family's urban and rural property portfolio.
 *
 *  1. Property Proof
 *  2. Locality Type
 *  3. Rural / Urban Property Details
 *  5. Owner Name
 *  6. Property Address
 *
 * @param {object} props Component props.
 * @param {Function} props.t Translation function.
 * @param {object} props.config Form-step configuration; its key identifies this property's saved value.
 * @param {Function} props.onSelect Completes the step and submits the final property value.
 * @param {Function} [props.onUpdate] Persists partial property changes while the step is active.
 * @param {object} props.formData Form data keyed by step configuration key.
 * @returns {JSX.Element} Property portfolio form with add, edit, and primary-property controls.
 */
const PropertyDetails = ({ t, config, onSelect, onUpdate, formData }) => {
  const [searchParams] = useSearchParams();
  const isViewMode = searchParams.get("mode") === "view" || !!config?.isViewMode;
  const saved = formData?.[config?.key] || {};
  
  

  const initialPortfolio = useMemo(() => {
    let portfolio = [];
    if (Array.isArray(saved.propertiesPortfolio) && saved.propertiesPortfolio.length) {
      portfolio = saved.propertiesPortfolio;
    } else if (saved.localityType) {
      portfolio = [{ ...saved, portfolioId: `property-${Date.now()}` }];
    }

    if (!portfolio.some((property) => property.isPrimaryAddress)) {
      const savedPrimaryIndex = saved.primaryPropertyId
        ? portfolio.findIndex((property) => getPropertyAddressId(property) === saved.primaryPropertyId)
        : -1;
      const primaryIndex = portfolio.length === 1 ? 0 : savedPrimaryIndex;
      if (primaryIndex >= 0) {
        portfolio = portfolio.map((property, index) => ({
          ...property,
          isPrimaryAddress: index === primaryIndex,
          primaryPropertyId: index === primaryIndex ? getPropertyAddressId(property) : "",
        }));
      }
    }

    return [...portfolio].sort((first, second) =>
      Number(Boolean(second.isPrimaryAddress)) - Number(Boolean(first.isPrimaryAddress))
    );
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
      primaryPropertyId: getPropertyAddressId(primaryFirst.find((property) => property.isPrimaryAddress)),
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

    if (savedProperties.some((property) => property.isPrimaryAddress)) {
      nextProperties.forEach((property, index) => {
        nextProperties[index] = { ...property, isPrimaryAddress: false, primaryPropertyId: "" };
      });
      const newPrimary = nextProperties.find((property) => property.portfolioId === savedProperties[0].portfolioId);
      if (newPrimary) {
        const primaryIndex = nextProperties.indexOf(newPrimary);
        nextProperties[primaryIndex] = {
          ...newPrimary,
          isPrimaryAddress: true,
          primaryPropertyId: getPropertyAddressId(newPrimary),
        };
      }
    } else if (nextProperties.length === 1) {
      nextProperties[0] = {
        ...nextProperties[0],
        isPrimaryAddress: true,
        primaryPropertyId: getPropertyAddressId(nextProperties[0]),
      };
    }

    persistProperties(nextProperties);
    closeDrawer();
  };

  const deleteProperty = (index) => {
    const remainingProperties = properties.filter((_, propertyIndex) => propertyIndex !== index);
    if (remainingProperties.length === 1 && !remainingProperties[0].isPrimaryAddress) {
      remainingProperties[0] = {
        ...remainingProperties[0],
        isPrimaryAddress: true,
        primaryPropertyId: getPropertyAddressId(remainingProperties[0]),
      };
    }
    persistProperties(remainingProperties);
  };

  const setPrimaryProperty = (index) => {
    const nextProperties = properties.map((property, propertyIndex) => ({
      ...property,
      isPrimaryAddress: propertyIndex === index,
      primaryPropertyId: propertyIndex === index ? getPropertyAddressId(property) : "",
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
    const hasPrimaryProperty = properties.some((property) => property.isPrimaryAddress);

    if (properties.length > 1 && !hasPrimaryProperty) {
      setToast({
        error: true,
        label: t(("PROFILE_SELECT_PRIMARY_URBAN_PROPERTY")),
      });
      return;
    }

    const selectedProperties =
      properties.length === 1 && !hasPrimaryProperty
        ? [{ ...properties[0], isPrimaryAddress: true, primaryPropertyId: getPropertyAddressId(properties[0]) }]
        : properties;
    onSelect(config.key, {
      propertiesPortfolio: selectedProperties,
      localityType: selectedProperties[0].localityType,
      noProperty: false,
      primaryPropertyId: getPropertyAddressId(selectedProperties.find((property) => property.isPrimaryAddress)),
    }, false);
  };
  const hasPrimaryProperty = properties.some((property) => property.isPrimaryAddress);
  const canProceed = noProperty || (properties.length > 0 && (properties.length === 1 || hasPrimaryProperty));
  const propertyOwnershipOptions = [
    { code: "YES", name: t(("PROFILE_PROPERTY_OWNED_YES"))},
    { code: "NO", name: t(("PROFILE_PROPERTY_OWNED_NO"))},
  ];

  return (
    <React.Fragment>
      <div><FamilyCreateTabs t={t} /></div>
      <Card className="property-details-container">
        <CardHeader>{t(("ES_TITILE_PROPERTY_DETAILS"))}</CardHeader>
        <p>{t(("PROFILE_PROPERTY_PORTFOLIO_SUBTITLE"))}</p>

        <div className="property-details-ownership-question">
          <CardLabel>{t(("PROFILE_DO_YOU_OWN_PROPERTY"))}</CardLabel>
          <RadioButtons  name="ppp-property-ownership" options={propertyOwnershipOptions} optionsKey="name"
            selectedOption={propertyOwnershipOptions.find((option) => option.code === (noProperty ? "NO" : "YES"))}
            onSelect={handlePropertyOwnershipChange} disabled={isViewMode} innerStyles={{ display: "flex", alignItems: "center", marginRight: "24px" }}
          />
        </div>

        {!isViewMode && !noProperty && (
          <div className="property-details-add-sticky">
            <button type="button" className="property-details-add-btn" onClick={openAddProperty}> + Add Property</button>
          </div>
        )}

        {noProperty ? (
          <div className="property-details-empty-state">{t(("PROFILE_NO_PROPERTY_DECLARED"))} </div>
        ) : properties.length === 0 ? (
          <div className="property-details-empty-state">
            {t(("PROFILE_NO_PROPERTIES_ADDED"))}
          </div>
        ) : (
          <div className="profile-property-summary-list property-details-list">
            {properties.map((property, index) => (
              <div className="profile-property-summary-card property-details-summary-card" key={property.portfolioId || index}>
                <div>
                  <div className="property-details-summary-title">
                    {getPropertyTitle(property)}
                    {property.isPrimaryAddress && (
                      <span className="property-details-primary-badge">{t(("PROFILE_PRIMARY_ADDRESS"))}</span>
                    )}
                  </div>
                  <div className="property-details-summary-location">Location: {getPropertyLocation(property)}</div>
                  <div className="property-details-summary-type">
                    <span>Type:</span>{" "}
                    <span className={property.localityType === "RURAL" ? "property-details-rural-badge" : "property-details-urban-badge"}>
                      {property.localityType === "RURAL" ? "🌾 Rural" : "🏢 Urban"}
                    </span>
                  </div>
                  {property.localityType === "RURAL" && property.ownershipType && (
                    <div className="property-details-summary-meta">Ownership: {property.ownershipType === "JOINT" ? "Joint" : property.ownershipType}</div>
                  )}
                </div>
                {!isViewMode && (
                  <div className="profile-property-summary-actions property-details-card-actions">
                    {!property.isPrimaryAddress && (
                      <button
                        type="button"
                        className="property-details-primary-action-btn"
                        onClick={() => setPrimaryProperty(index)}
                      >
                        {t(("PROFILE_MARK_AS_PRIMARY"))}
                      </button>
                    )}
                    <button type="button" className="property-details-icon-btn" onClick={() => openEditProperty(index)} aria-label="Edit property">
                      <EditIcon />
                    </button>
                    <button type="button" className="property-details-icon-btn" onClick={() => deleteProperty(index)} aria-label="Delete property">
                      <DeleteIcon fill="#D4353A" />
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        )}

        <div className="property-details-save-next-wrap">
          <SubmitBar label={t(isViewMode ? "COMMON_NEXT" : (config?.texts?.submitBarLabel || "COMMON_SAVE_NEXT"))}
            onSubmit={goNext} disabled={!isViewMode && !canProceed} />
        </div>
      </Card>

      {toast && (<Toast error={toast.error} label={toast.label}  onClose={() => setToast(null)}/> )}
      {drawerOpen && createPortal(
        <div className="profile-citizen">
          <div className="property-details-drawer-overlay" role="dialog" aria-modal="true" aria-label="Add Property">
            <div className="profile-property-drawer property-details-drawer">
              <div className="property-details-drawer-header">
                <h2 style={{ margin: 0 }}>{editingIndex !== null ? "Edit Property" : "Add Property"}</h2>
                <button type="button" className="property-details-drawer-close-btn" onClick={closeDrawer} aria-label="Close">×</button>
              </div>

              <div className="profile-property-drawer-body property-details-drawer-body">
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

              <div className="profile-property-drawer-footer property-details-drawer-footer">
                <button type="button" className="property-details-drawer-cancel-btn" onClick={closeDrawer}>
                  {t(("CORE_COMMON_CANCEL"))}
                </button>
                <SubmitBar label={t(("PROFILE_SAVE_PROPERTY"))} onSubmit={saveProperty} disabled={!canSaveProperty} />
              </div>
            </div>
          </div>
        </div>,
        document.body
      )}
    </React.Fragment>
  );
};

export default PropertyDetails;