import React, { useState } from "react";
import { Card, Loader, SearchIconSvg, TextInput } from "@nudmcdgnpm/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import { profileStyles } from "../../utils/profileStyles";

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
 * @returns {JSX.Element} The urban property search form and property cards.
 */
const UrbanPropertyForm = ({ data = {}, onChange, disabled = false }) => {
  const { t } = useTranslation();
  const styles = profileStyles.urbanPropertyForm;
  const [propertyId, setPropertyId] = useState("");
  const [searched, setSearched] = useState(false);
  const [searchError, setSearchError] = useState(false);
  const [resultsCount, setResultsCount] = useState(0);
  const [propertyCards, setPropertyCards] = useState(() => {
    if (data.properties?.length) return data.properties;
    if (data.propertyId && data.ownerName) {
      return [{
        propertyId: data.propertyId,
        propertyTaxAmount: data.propertyTaxAmount ?? 0,
        ownerName: data.ownerName,
        propertyAddress: data.propertyAddress,
        status: data.status,
        isPrimaryAddress: true,
      }];
    }
    return [];
  });
  const [primaryPropertyId, setPrimaryPropertyId] = useState(() => {
    const properties = data.properties || [];
    return (
      data.primaryPropertyId ||
      properties.find((property) => property.isPrimaryAddress)?.propertyId ||
      (properties.length === 1 ? properties[0].propertyId : "") ||
      (!properties.length ? data.propertyId : "") ||
      ""
    );
  });
  const [isLoading, setIsLoading] = useState(false);

  const publishProperties = (properties, primaryId) => {
    const primary = properties.find((property) => property.propertyId === primaryId);
    onChange({
      properties: properties.map((property) => ({
        ...property,
        isPrimaryAddress: property.propertyId === primaryId,
      })),
      primaryPropertyId: primary?.propertyId || "",
      propertyId: primary?.propertyId || "",
      propertyTaxAmount: primary?.propertyTaxAmount ?? null,
      ownerName: primary?.ownerName || "",
      propertyAddress: primary?.propertyAddress || "",
      status: primary?.status || "",
    });
  };

  const handleSearchInputChange = (event) => {
    setPropertyId(event.target.value);
    setSearched(false);
    setSearchError(false);
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
      const foundProperties = results.map((property) => ({
        propertyId: property.propertyId,
        propertyTaxAmount: property.propertyTaxAmount ?? property.taxAmount ?? 0,
        ownerName: getOwnerName(property),
        propertyAddress: getPropertyAddress(property),
        status: property.status,
      }));
      const existingIds = new Set(propertyCards.map((property) => property.propertyId));
      const newProperties = foundProperties.filter((property) => property.propertyId && !existingIds.has(property.propertyId));
      const nextProperties = [...propertyCards, ...newProperties];
      const nextPrimaryId =
        nextProperties.length <= 1
          ? primaryPropertyId || nextProperties[0]?.propertyId || ""
          : newProperties.length > 0
            ? ""
            : primaryPropertyId;

      setPropertyCards(nextProperties);
      setPrimaryPropertyId(nextPrimaryId);
      publishProperties(nextProperties, nextPrimaryId);
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

  const handleRemoveProperty = (propertyId) => {
    const nextProperties = propertyCards.filter((property) => property.propertyId !== propertyId);
    const nextPrimaryId =
      nextProperties.length === 1
        ? nextProperties[0].propertyId
        : primaryPropertyId === propertyId
          ? ""
          : primaryPropertyId;

    setPropertyCards(nextProperties);
    setPrimaryPropertyId(nextPrimaryId);
    publishProperties(nextProperties, nextPrimaryId);
  };

  const handleAddMore = () => {
    setPropertyId("");
    setSearched(false);
    setSearchError(false);
    setResultsCount(0);
  };

  return (
    <div style={styles.container}>
      {!disabled && (
        <div style={styles.field}>
          <label style={profileStyles.label}>{t("PROFILE_SEARCH_LINK_PROPERTY_ID")}</label>
          <div style={styles.searchRow}>
            <TextInput
              style={styles.searchInput}
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
              style={{ ...profileStyles.primaryButton, ...styles.searchSubmitButton }}
              onClick={handleSearch}
              disabled={isLoading || !propertyId.trim()}
            >
              {t("CS_COMMON_SUBMIT")}
            </button>
          </div>
          {propertyCards.length > 0 && (
            <div style={{ marginTop: "12px" }}>
              <button type="button" style={profileStyles.primaryButton} onClick={handleAddMore}>
                {t("PROFILE_ADD_MORE")}
              </button>
            </div>
          )}
          <div style={styles.createProperty}>
            <span>Not able to find the property?</span>{" "}
            <a
              href="#"
              onClick={(event) => event.preventDefault()}
              style={styles.registerPropertyLink}
            >
              Click here to Register New Property
            </a>
          </div>
          {isLoading && <Loader />}
          {searchError && (
            <div style={styles.noProperty} role="alert">
              {t("PROFILE_PROPERTY_SEARCH_FAILED")}
            </div>
          )}
          {searched && !isLoading && !searchError && resultsCount === 0 && (
            <div style={styles.noProperty}>{t("PROFILE_NO_PROPERTY_FOUND")}</div>
          )}
        </div>
      )}

      {propertyCards.length > 0 && (
        <div style={styles.results}>
          {propertyCards.length > 1 && (
            <div style={styles.primaryAddressLabel}>Select primary address</div>
          )}
          {propertyCards.map((property) => {
            const propertyIdValue = property.propertyId || "";
            const status = property.status
              ? property.status.charAt(0) + property.status.slice(1).toLowerCase()
              : "NA";

            return (
              <Card
                key={propertyIdValue}
                style={{
                  ...styles.propertyCard,
                  ...styles.propertyCardWithActions,
                }}
              >
                <div style={styles.propertyDetails}>
                  {[
                    [t("PROFILE_PROPERTY_TAX_AMOUNT"), `₹ ${property.propertyTaxAmount ?? 0}`],
                    [t("PROFILE_PROPERTY_ID"), propertyIdValue || "NA"],
                    [t("PROFILE_OWNER_NAME"), property.ownerName || "NA"],
                    [t("PROFILE_PROPERTY_ADDRESS"), property.propertyAddress || "NA"],
                    [t("PROFILE_STATUS"), status],
                  ].map(([label, value]) => (
                    <div key={label} style={styles.propertyField}>
                      <div style={styles.propertyLabel}>{label}</div>
                      <div style={styles.propertyValue}>{value}</div>
                    </div>
                  ))}
                </div>
                <div style={styles.propertyActions}>
                  {propertyCards.length > 1 && (
                    <button
                      type="button"
                      aria-label={`Set ${propertyIdValue} as default property`}
                      onClick={() => handleSelectPrimary(propertyIdValue)}
                      disabled={primaryPropertyId === propertyIdValue || disabled}
                      style={{
                        ...styles.propertyActionButton,
                        ...(primaryPropertyId === propertyIdValue ? styles.defaultPropertyButton : {}),
                      }}
                    >
                      {primaryPropertyId === propertyIdValue ? "Primary" : "Set as Primary"}
                    </button>
                  )}
                  <button
                    type="button"
                    aria-label={`Remove property ${propertyIdValue}`}
                    onClick={() => handleRemoveProperty(propertyIdValue)}
                    disabled={disabled}
                    style={{ ...styles.propertyActionButton, ...styles.removePropertyButton }}
                  >
                    {t("CORE_COMMON_REMOVE", "Remove")}
                  </button>
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
