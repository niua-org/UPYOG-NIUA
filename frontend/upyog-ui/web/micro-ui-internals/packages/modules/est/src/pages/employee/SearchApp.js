import React, { useState } from "react";
import { Toast } from "@upyog/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import ESTSearchApplication from "../../components/ESTSearchApplication";

const SearchApp = ({ path }) => {
    const { t } = useTranslation();
    const tenantId = Digit.ULBService.getCurrentTenantId();
    const [payload, setPayload] = useState({});
    const [showToast, setShowToast] = useState(null);
    const [hasSearched, setHasSearched] = useState(false);
    

    function onSubmit(_data) {
        setHasSearched(true);
        
        const data = { ..._data };
        let searchCriteria = {};
        
        Object.keys(data).forEach(key => {
            if (data[key]) {
                searchCriteria[key] = typeof data[key] === "object" ? data[key].code : data[key];
            }
        });

        if (Object.entries(searchCriteria).length > 0) {
            searchCriteria.tenantId = tenantId;
            setPayload(searchCriteria);
        } else {
            setShowToast({ error: true, label: "Please enter search criteria" });
        }
    }

    // Using a custom hook to fetch EST asset search results based on the payload
    // and tenantId.
    // The query is enabled only when there is a valid payload.

    const { isLoading, isSuccess, data, error } = Digit.Hooks.estate.useESTAssetSearch({
        tenantId,
        filters: {"AssetSearchCriteria": { "estateNo": payload.estateNo}},
    }, {
        enabled: !!(payload && Object.keys(payload).length > 0),
    });

    const searchResult = data?.Assets || [];
    const count = searchResult.length;

    return (
        <React.Fragment>
            <ESTSearchApplication
                t={t}
                isLoading={isLoading}
                tenantId={tenantId}
                setShowToast={setShowToast}
                onSubmit={onSubmit}
                data={
                     hasSearched && isSuccess && !isLoading
                     ? searchResult?.length > 0
                     ? searchResult
            : { display: "ES_COMMON_NO_DATA" }
        : ""
}
                count={count}
            />

            {showToast && (
                <Toast
                    error={showToast.error}
                    warning={showToast.warning}
                    label={t(showToast.label)}
                    isDeleteBtn={true}
                    onClose={() => {
                        setShowToast(null);
                    }}
                />
            )}

            {error && (
                <Toast
                    error={true}
                    label={`Search failed: ${error.message || "Unknown error"}`}
                    isDeleteBtn={true}
                    onClose={() => {
                        // Handle error close if needed
                    }}
                />
            )}
        </React.Fragment>
    );
};

export default SearchApp;
