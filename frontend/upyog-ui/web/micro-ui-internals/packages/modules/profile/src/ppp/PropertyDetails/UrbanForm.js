import React, { useState } from "react";
import { Card, Loader, SearchIconSvg, TextInput } from "@nudmcdgnpm/digit-ui-react-components";
import { useTranslation } from "react-i18next";

/**
 * Gets the display name for a property's active owners.
 *
 * @param {object} property Property Tax search result.
 * @returns {string} Owner name or comma-separated active owner names.
 */
const getOwnerName = (property) => {
  if (property.ownerName) return property.ownerName;
  return property.owners
    ?.filter((owner) => owner.status !== "INACTIVE")
    .map((owner) => owner.name)
    .filter(Boolean)
    .join(", ") || "";
};

/**
 * Builds a readable address from a property or its address fields.
 *
 * @param {object} property Property Tax search result.
 * @returns {string} Property address.
 */
const getPropertyAddress = (property) => {
  if (typeof property.propertyAddress === "string") return property.propertyAddress;

  const address = property.address || {};
  return [
    address.doorNo,
    address.street,
    address.locality?.name || address.locality?.area || address.locality?.code,
    address.city?.name || address.city,
    address.pincode,
  ].filter(Boolean).join(", ");
};

/**
 * Searches Property Tax records and lets the user add and manage urban properties.
 *
 * @param {object} props Component props.
 * @param {object} [props.data={}] Existing property details and selected default address.
 * @param {Function} props.onChange Callback invoked when the property selection changes.
 * @param {boolean} [props.disabled=false] Whether property search and editing are disabled.
 * @param {string[]} [props.existingPropertyIds=[]] Property IDs already in the portfolio, used to block duplicates.
 * @returns {JSX.Element} The urban property search form and property cards.
 */
const UrbanPropertyForm = ({ data = {}, onChange, disabled = false, existingPropertyIds = [] }) => {
  const { t } = useTranslation();
  const [propertyId, setPropertyId] = useState("");
  const [searched, setSearched] = useState(false);
  const [searchError, setSearchError] = useState(false);
  const [resultsCount, setResultsCount] = useState(0);
  const [duplicateProperty, setDuplicateProperty] = useState(false);
  const [propertyCards, setPropertyCards] = useState(() => {
    if (data.properties?.length) return data.properties;
    if (data.propertyId && data.ownerName) {
      return [{
        propertyId: data.propertyId,
        propertyTaxAmount: data.propertyTaxAmount ?? 0,
        ownerName: data.ownerName,
        propertyAddress: data.propertyAddress,
        status: data.status,
        isPrimaryAddress: Boolean(data.isPrimaryAddress),
      }];
    }
    return [];
  });
  const [primaryPropertyId, setPrimaryPropertyId] = useState(() => {
    const properties = data.properties || [];
    return (
      data.primaryPropertyId ||
      properties.find((property) => property.isPrimaryAddress)?.propertyId ||
      (data.isPrimaryAddress ? data.propertyId : "") ||
      ""
    );
  });
  const [isLoading, setIsLoading] = useState(false);

  const publishProperties = (properties, primaryId) => {
    const primary = properties.find((property) => property.propertyId === primaryId);
    const selectedProperty = properties[0];
    onChange({
      properties: properties.map((property) => ({
        ...property,
        isPrimaryAddress: property.propertyId === primaryId,
      })),
      primaryPropertyId: primary?.propertyId || "",
      propertyId: selectedProperty?.propertyId || "",
      propertyTaxAmount: selectedProperty?.propertyTaxAmount ?? null,
      ownerName: selectedProperty?.ownerName || "",
      propertyAddress: selectedProperty?.propertyAddress || "",
      status: selectedProperty?.status || "",
      isPrimaryAddress: Boolean(primary && selectedProperty?.propertyId === primary.propertyId),
    });
  };

  const handleSearchInputChange = (event) => {
    setPropertyId(event.target.value);
    setSearched(false);
    setSearchError(false);
    setDuplicateProperty(false);
    setResultsCount(0);
  };

  const handleSearch = async () => {
    const searchId = propertyId.trim();
    if (!searchId || isLoading) return;

    setIsLoading(true);
    setSearched(true);
    setSearchError(false);
    setResultsCount(0);

    try {
      const tenantId = Digit.ULBService.getCurrentTenantId();
      const response = await Digit.PTService.search({
        tenantId,
        filters: {
          propertyIds: searchId,
          limit: 5,
          sortOrder: "ASC",
          sortBy: "createdTime",
          offset: 0,
          status: "ACTIVE",
        },
      });

      const results = response?.Properties || [];
      const property = results.find((result) =>
        String(result.propertyId || "").toLowerCase() === searchId.toLowerCase()
      );

      if (!property) {
        setPropertyCards([]);
        setPrimaryPropertyId("");
        publishProperties([], "");
        setResultsCount(0);
        return;
      }

      const foundProperty = {
        propertyId: property.propertyId,
        propertyTaxAmount: property.propertyTaxAmount ?? property.taxAmount ?? 0,
        ownerName: getOwnerName(property),
        propertyAddress: getPropertyAddress(property),
        status: property.status,
        isPrimaryAddress: false,
      };
      const alreadyAdded = [...existingPropertyIds, ...propertyCards.map((item) => item.propertyId)]
        .some((id) => id && String(id).toLowerCase() === String(foundProperty.propertyId).toLowerCase());
      if (alreadyAdded) {
        setDuplicateProperty(true);
        setResultsCount(1);
        return;
      }

      setDuplicateProperty(false);
      setPropertyCards([foundProperty]);
      setPrimaryPropertyId("");
      publishProperties([foundProperty], "");
      setResultsCount(results.length);
    } catch (error) {
      console.error("Property search failed:", error);
      setSearchError(true);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSearchKeyDown = (event) => {
    if (event.key === "Enter") {
      event.preventDefault();
      handleSearch();
    }
  };

  const handleSelectPrimary = (propertyId) => {
    setPrimaryPropertyId(propertyId);
    publishProperties(propertyCards, propertyId);
  };

  const handleRemovePrimary = () => {
    const updatedProperties = propertyCards.map((property) => ({
      ...property,
      isPrimaryAddress: false,
    }));
    setPropertyCards(updatedProperties);
    setPrimaryPropertyId("");
    setDuplicateProperty(false);
    publishProperties(updatedProperties, "");
  };

  return (
    <div className="urban-property-form-container">
      {!disabled && (
        <div className="urban-property-form-field">
          <label className="profile-form-label">{t("PROFILE_SEARCH_LINK_PROPERTY_ID")}</label>
          <div className="urban-property-form-search-row profile-property-urban-search-row">
            <TextInput
              className="urban-property-form-search-input"
              value={propertyId}
              onChange={handleSearchInputChange}
              onKeyDown={handleSearchKeyDown}
              placeholder="Search by Property ID"
              signature={true}
              signatureImg={
                <SearchIconSvg
                  className="signature-img"
                  onClick={() => handleSearch()}
                />
              }
            />
            <button
              type="button"
              className="profile-btn-primary urban-property-form-search-btn"
              onClick={handleSearch}
              disabled={isLoading || !propertyId.trim()}
            >
              {t("CS_COMMON_SUBMIT")}
            </button>
          </div>
          <div className="urban-property-create-wrap">
            <span>Not able to find the property?</span>{" "}
            <a
              href="#"
              onClick={(event) => event.preventDefault()}
              className="urban-property-register-link"
            >
              Click here to Register New Property
            </a>
          </div>
          {isLoading && <Loader />}
          {searchError && (
            <div className="urban-property-no-property" role="alert">
              {t("PROFILE_PROPERTY_SEARCH_FAILED")}
            </div>
          )}
          {searched && !isLoading && !searchError && resultsCount === 0 && (
            <div className="urban-property-no-property">{t("PROFILE_NO_PROPERTY_FOUND")}</div>
          )}
          {duplicateProperty && (
            <div className="urban-property-duplicate-error" role="alert">
              {t("PROFILE_PROPERTY_ALREADY_ADDED", "This property has already been added.")}
            </div>
          )}
        </div>
      )}

      {propertyCards.length > 0 && (
        <div className="urban-property-results">
          {propertyCards.map((property) => {
            const propertyIdValue = property.propertyId || "";
            const status = property.status
              ? property.status.charAt(0) + property.status.slice(1).toLowerCase()
              : "NA";

            return (
              <Card
                key={propertyIdValue}
                className="profile-property-urban-card urban-property-card urban-property-card-with-actions"
              >
                <div className="urban-property-card-details">
                  {[
                    [t("PROFILE_PROPERTY_TAX_AMOUNT"), `₹ ${property.propertyTaxAmount ?? 0}`],
                    [t("PROFILE_PROPERTY_ID"), propertyIdValue || "NA"],
                    [t("PROFILE_OWNER_NAME"), property.ownerName || "NA"],
                    [t("PROFILE_PROPERTY_ADDRESS"), property.propertyAddress || "NA"],
                    [t("PROFILE_STATUS"), status],
                  ].map(([label, value]) => (
                    <div key={label} className="urban-property-card-field">
                      <div className="urban-property-card-label">{label}</div>
                      <div className="urban-property-card-value">{value}</div>
                    </div>
                  ))}
                </div>
                <div className="profile-property-urban-actions urban-property-card-actions">
                  {primaryPropertyId === propertyIdValue ? (
                    <span className="urban-property-default-btn">{t("PROFILE_PRIMARY_ADDRESS", "Primary address")}</span>
                  ) : (
                    <button
                      type="button"
                      aria-label={`Set ${propertyIdValue} as primary address`}
                      onClick={() => handleSelectPrimary(propertyIdValue)}
                      disabled={disabled}
                      className="urban-property-action-btn"
                    >
                      {t("PROFILE_MARK_AS_PRIMARY", "Mark as primary")}
                    </button>
                  )}
                  {primaryPropertyId === propertyIdValue && (
                    <button
                      type="button"
                      aria-label={`Remove ${propertyIdValue} as primary address`}
                      onClick={handleRemovePrimary}
                      disabled={disabled}
                      className="urban-property-action-btn urban-property-remove-btn"
                    >
                      {t("PROFILE_REMOVE_AS_PRIMARY", "Remove as primary")}
                    </button>
                  )}
                </div>
              </Card>
            );
          })}
        </div>
      )}

    </div>
  );
};

export default UrbanPropertyForm;
