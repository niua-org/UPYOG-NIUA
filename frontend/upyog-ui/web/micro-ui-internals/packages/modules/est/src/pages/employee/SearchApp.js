import React, { useState } from "react";
import { Toast } from "@upyog/digit-ui-react-components";
import { useParams, useHistory } from "react-router-dom";
import { useTranslation } from "react-i18next";
import ESTSearchApplication from "../../components/ESTSearchApplication";

const SearchApp = ({ path }) => {
    const { t } = useTranslation();
    const history = useHistory();
    const tenantId = Digit.ULBService.getCurrentTenantId();
    const [payload, setPayload] = useState({});
    const [showToast, setShowToast] = useState(null);
    const dummyData = [
        {
      assetNumber: "EST-001-A",
      assetRef: "REF001",
      buildingName: "Central Plaza",
      locality: "Ramnagar",
      totalPlotArea: "5000 sqft",
      dimensions: "50x100 ft",
      totalFloors: "5",
      assetType: "Residential",
      ratePerSqft: "2000 INR",
      assetStatus: "Allotted"
    },
    {
      assetNumber: "EST-002-B",
      assetRef: "REF002",
      buildingName: "Tower Heights",
      locality: "Lakshmipuram",
      totalPlotArea: "4000 sqft",
      dimensions: "40x100 ft",
      assetType: "Commercial",
      ratePerSqft: "1500 INR",
      assetStatus: "Not Allotted"
    },
    {
      assetNumber: "EST-003-C",
      assetRef: "REF003",
      buildingName: "Green Valley",
      locality: "Shivaji Nagar",
      totalPlotArea: "5000 sqft",
      dimensions: "50x100 ft",
      assetType: "Industrial",
      ratePerSqft: "2000 INR",
      assetStatus: "Allotted"
    }
    ];
    function onSubmit(_data) {
        const data = {
            ..._data,
        };

        let payload = Object.keys(data)
            .filter((k) => data[k])
            .reduce((acc, key) => ({ ...acc, [key]: typeof data[key] === "object" ? data[key].code : data[key] }), {});

        if (Object.entries(payload).length > 0) {
            setPayload(payload);
        }
    }

    const config = {
        enabled: !!(payload && Object.keys(payload).length > 0),
    };
    const getFilteredData = () => {
    if (!payload.assetNumber) {
        return [];
    }
    
    const filtered = dummyData.filter(item => 
        item.assetNumber.toLowerCase().includes(payload.assetNumber.toLowerCase())
    );
    
    return filtered;
};

    const filteredData = getFilteredData();
    const hasSearched = Object.keys(payload).length > 0;

    const {
        isLoading,
        isSuccess,
        data: { EstateApplication: searchResult, Count: count } = {},
} = { 
        isLoading: false, 
        isSuccess: true, 
        data: { 
           EstateApplication: hasSearched ? filteredData : [], 
           Count: hasSearched ? filteredData.length : 0 
    } 
};


   return (
        <React.Fragment>
            <div style={{ display: "flex", alignItems: "center", marginBottom: "20px" }}>
                <button
                    onClick={() => history.push("/upyog-ui/employee")}
                    style={{
                        padding: "8px 16px",
                        borderRadius: "4px",
                        cursor: "pointer",
                        marginRight: "20px"
                    }}
                >
                    ← Back
                </button>
            </div>
            
            <ESTSearchApplication
                t={t}
                isLoading={isLoading}
                tenantId={tenantId}
                setShowToast={setShowToast}
                onSubmit={onSubmit}
                data={
                    isSuccess && !isLoading
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
                    isDleteBtn={true}
                    onClose={() => {
                        setShowToast(null);
                    }}
                />
            )}
        </React.Fragment>
    );
};

export default SearchApp;