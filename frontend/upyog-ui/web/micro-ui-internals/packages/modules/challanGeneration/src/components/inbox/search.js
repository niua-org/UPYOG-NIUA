import React from "react";
import { useForm, Controller } from "react-hook-form";
import { TextInput, Label, SubmitBar, LinkLabel, ActionBar, CloseSvg, DatePicker, MobileNumber } from "@upyog/digit-ui-react-components";
import { useTranslation } from "react-i18next";

/**
 * SearchApplication component:
 * - Dynamic search form using react-hook-form
 * - Supports multiple input types (text, date, mobile)
 * - Handles search, clear, and mobile/desktop layouts
 */

const fieldComponents = {
  date: DatePicker,
  mobileNumber: MobileNumber,
};

const SearchApplication = ({ onSearch, type, onClose, searchFields, searchParams, isInboxPage, defaultSearchParams, clearSearch: _clearSearch }) => {
  const { t } = useTranslation();
  const { handleSubmit, reset, watch, control, formState, setValue } = useForm({
    defaultValues: searchParams,
  });

  const form = watch();
  const mobileView = innerWidth <= 640;

  const onSubmitInput = (data) => {
    if (!data.mobileNumber) {
      delete data.mobileNumber;
    }

    data.delete = [];

    searchFields.forEach((field) => {
      if (!data[field.name]) data.delete.push(field.name);
    });

    onSearch(data);
    if (type === "mobile") {
      onClose();
    }
  };

  function clearSearch() {
    const resetValues = searchFields.reduce((acc, field) => ({ ...acc, [field?.name]: "" }), {});
    reset(resetValues);
    if (isInboxPage) {
      const _newParams = { ...searchParams };
      _newParams.delete = [];
      searchFields.forEach((e) => {
        _newParams.delete.push(e?.name);
      });
      onSearch({ ..._newParams });
    } else if (_clearSearch) {
      _clearSearch();
    }
  }

  const clearAll = (mobileView) => {
    const mobileViewStyles = mobileView ? { margin: 0 } : {};
    return (
      <LinkLabel style={{ ...mobileViewStyles }} onClick={clearSearch}>
        {t("CS_COMMON_CLEAR_SEARCH")}
      </LinkLabel>
    );
  };

  return (
    <form onSubmit={handleSubmit(onSubmitInput)}>
      <React.Fragment>
        <div className="search-container" style={{ marginLeft: isInboxPage ? "24px" : "revert" }}>
          <div className="search-complaint-container">
            {(type === "mobile" || mobileView) && (
              <div className="complaint-header">
                <h2>{t("ES_COMMON_SEARCH_BY")}</h2>
                <span onClick={onClose}>
                  <CloseSvg />
                </span>
              </div>
            )}
            <div className={"complaint-input-container for-pt " + (!isInboxPage ? "for-search" : "")} style={{ width: "100%", display: "grid" }}>
              {searchFields
                ?.filter((e) => true)
                ?.map((input, index) => (
                  <div key={input.name} className="input-fields">
                    <span className={"mobile-input"}>
                      <Label>{t(input.label) + ` ${input.isMendatory ? "*" : ""}`}</Label>
                      {!input.type ? (
                        <Controller
                          render={(props) => {
                            return <TextInput onChange={props.onChange} value={props.value} />;
                          }}
                          name={input.name}
                          control={control}
                          defaultValue={""}
                        />
                      ) : (
                        <Controller
                          render={(props) => {
                            const Comp = fieldComponents?.[input.type];
                            return <Comp formValue={form} setValue={setValue} onChange={props.onChange} value={props.value} />;
                          }}
                          name={input.name}
                          control={control}
                          defaultValue={""}
                        />
                      )}
                    </span>
                  </div>
                ))}

              {isInboxPage && (
                <div className="search-action-wrapper" style={{width: "100%"}}>
                  <SubmitBar className="submit-bar-search" label={t("ES_COMMON_SEARCH")} submit />
                  <span style={{ paddingTop: "9px" }} className="clear-search">
                    {clearAll()}
                  </span>
                </div>
              )}

              {type === "desktop" && !mobileView && !isInboxPage && (
                <div className="search-action-wrapper">
                  <SubmitBar className="submit-bar-search" label={t("ES_COMMON_SEARCH")} submit />
                  <div style={{ width: "100%", textAlign: "right", width: "240px", textAlign: "right", marginLeft: "96px" }}>
                    {clearAll()}
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
        {(type === "mobile" || mobileView) && (
          <ActionBar className="clear-search-container">
            <button className="clear-search" style={{ flex: 1 }}>
              {clearAll(mobileView)}
            </button>
            <SubmitBar label={t("ES_COMMON_SEARCH")} style={{ flex: 1 }} submit={true} />
          </ActionBar>
        )}
      </React.Fragment>
    </form>
  );
};

export default SearchApplication;
