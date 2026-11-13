import React, { useState } from "react";
import { Card, Loader, SearchIconSvg, TextInput } from "@nudmcdgnpm/digit-ui-react-components";

import { profileStyles } from "../../utils/profileStyles";

const getOwnerName = (property) => {
  if (property.ownerName) return property.ownerName;
  return property.owners
    ?.filter((owner) => owner.status !== "INACTIVE")
    .map((owner) => owner.name)
    .filter(Boolean)
    .join(", ") || "";
};

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

const UrbanPropertyForm = ({ data = {}, onChange, disabled = false }) => {
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
  const [primaryPropertyId, setPrimaryPropertyId] = useState(
    data.primaryPropertyId ||
      data.properties?.find((property) => property.isPrimaryAddress)?.propertyId ||
      data.properties?.[0]?.propertyId ||
      data.propertyId ||
      ""
  );
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
      const nextPrimaryId = primaryPropertyId || nextProperties[0]?.propertyId || "";

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

  const handleSelectPrimary = (propertyId) => {
    setPrimaryPropertyId(propertyId);
    publishProperties(propertyCards, propertyId);
  };

  const handleAddMore = () => {
    setPropertyId("");
    setSearched(false);
    setSearchError(false);
    setResultsCount(0);
  };

  const handleCreateProperty = () => {
    window.open("/upyog-ui/citizen/property/property-create", "_blank");
  };

  return (
    <div style={styles.container}>
      {!disabled && (
        <div style={styles.field}>
          <label style={profileStyles.label}>Search &amp; Link Property ID</label>
          <div className="flex items-center gap-md">
            <TextInput
              style={styles.searchInput}
              value={propertyId}
              onChange={handleSearchInputChange}
              placeholder="Search by Property ID"
              signature={true}
              signatureImg={
                <SearchIconSvg
                  className="signature-img"
                  onClick={() => handleSearch()}
                />
              }
            />
            <button type="button" style={profileStyles.primaryButton} onClick={handleAddMore}>
              + Add more
            </button>
          </div>
          {isLoading && <Loader />}
          {searchError && (
            <div style={styles.noProperty} role="alert">
              Property search failed. Please try again.
            </div>
          )}
          {searched && !isLoading && !searchError && resultsCount === 0 && (
            <div style={styles.noProperty}>No property found.</div>
          )}
        </div>
      )}

      {propertyCards.length > 0 && (
        <div style={styles.results}>
          {propertyCards.map((property) => {
            const propertyIdValue = property.propertyId || "";
            const status = property.status
              ? property.status.charAt(0) + property.status.slice(1).toLowerCase()
              : "NA";

            return (
              <Card key={propertyIdValue} style={styles.propertyCard}>
                <div style={styles.propertyDetails}>
                  {[
                    ["Property Tax Amount", `₹ ${property.propertyTaxAmount ?? 0}`],
                    ["Property ID", propertyIdValue || "NA"],
                    ["Owner Name", property.ownerName || "NA"],
                    ["Property Address", property.propertyAddress || "NA"],
                    ["Status", status],
                  ].map(([label, value]) => (
                    <div key={label} style={styles.propertyField}>
                      <div style={styles.propertyLabel}>{label}</div>
                      <div style={styles.propertyValue}>{value}</div>
                    </div>
                  ))}
                </div>
                <input
                  type="radio"
                  name="primaryPropertyAddress"
                  aria-label={`Mark ${propertyIdValue} as primary address`}
                  title="Mark as primary address"
                  checked={primaryPropertyId === propertyIdValue}
                  onChange={() => !disabled && handleSelectPrimary(propertyIdValue)}
                  disabled={disabled}
                  style={styles.selectRadio}
                />
              </Card>
            );
          })}
        </div>
      )}

      {!disabled && (
        <div style={styles.createProperty}>
          <button type="button" style={profileStyles.primaryButton} onClick={handleCreateProperty}>
            + Create Property ID
          </button>
        </div>
      )}
    </div>
  );
};

export default UrbanPropertyForm;
