import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { TextInput, Label, SubmitBar } from "@nudmcdgnpm/digit-ui-react-components";

/**
 * SearchApplication component for the Family Service Inbox search bar.
 * Provides input fields for filtering family records by Family ID and Mobile Number.
 * All layout and typography are controlled via family.scss stylesheet classes.
 *
 * @param {Object} props - Component properties.
 * @param {Function} props.onSearch - Callback invoked with search criteria when user submits form.
 * @param {Object} props.searchParams - Current search parameter state.
 * @param {Function} props.clearSearch - Callback to reset search criteria.
 * @returns {React.ReactElement} Search form component.
 */
const SearchApplication = ({ onSearch, searchParams, clearSearch }) => {
  const { t } = useTranslation();
  const [familyId, setFamilyId] = useState(searchParams?.familyId || "");
  const [mobileNumber, setMobileNumber] = useState(searchParams?.mobileNumber || "");

  /**
   * Handles submission of the search form.
   *
   * @param {React.FormEvent} e - Form event.
   */
  const handleSearch = (e) => {
    e?.preventDefault?.();
    const data = {};
    if (familyId.trim()) data.familyId = familyId.trim();
    if (mobileNumber.trim()) data.mobileNumber = mobileNumber.trim();
    onSearch(data);
  };

  /**
   * Resets the search inputs and triggers clearSearch callback.
   */
  const handleClear = () => {
    setFamilyId("");
    setMobileNumber("");
    clearSearch();
  };

  return (
    <form onSubmit={handleSearch}>
      <div className="search-container search-container-auto-margin">
        <div className="search-complaint-container family-search-card">
          <div className="complaint-input-container family-search-inputs">
            <div className="input-fields">
              <span className="mobile-input">
                <Label>{t("CORE_COMMON_FAMILY_ID")}</Label>
                <TextInput
                  type="text"
                  name="familyId"
                  value={familyId}
                  onChange={(e) => setFamilyId(e.target.value)}
                  placeholder={t("CORE_COMMON_FAMILY_ID")}
                />
              </span>
            </div>

            <div className="input-fields">
              <span className="mobile-input">
                <Label>{t("CORE_COMMON_PROFILE_MOBILE_NUMBER")}</Label>
                <TextInput
                  type="text"
                  name="mobileNumber"
                  value={mobileNumber}
                  onChange={(e) => setMobileNumber(e.target.value)}
                  placeholder={t("CORE_COMMON_PROFILE_MOBILE_NUMBER")}
                />
              </span>
            </div>

            <div className="input-fields family-search-actions">
              <SubmitBar
                label={t("ES_COMMON_SEARCH")}
                onSubmit={handleSearch}
                className="family-search-submit-bar"
              />
              <span
                className="family-clear-search-btn"
                onClick={handleClear}
                role="button"
                tabIndex={0}
              >
                {t("ES_COMMON_CLEAR_ALL")}
              </span>
            </div>
          </div>
        </div>
      </div>
    </form>
  );
};

export default SearchApplication;
