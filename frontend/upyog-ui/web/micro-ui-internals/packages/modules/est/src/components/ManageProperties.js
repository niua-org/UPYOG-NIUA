import React, { useMemo, useState, useEffect } from "react";
import { 
  Table, 
  Header, 
  SearchField, 
  TextInput, 
  Dropdown, 
  SubmitBar,
  SearchForm,
  Loader
} from "@upyog/digit-ui-react-components";
import { Link, useHistory } from "react-router-dom";
import { useForm } from "react-hook-form";

const ManageProperties = ({ t }) => {
  const history = useHistory();
  const tenantId = Digit.ULBService.getCurrentTenantId();
  
  const { isLoading, isSuccess, data: apiData, error } = Digit.Hooks.estate.useESTAssetSearch({
    tenantId,
    filters: {"AssetSearchCriteria": {}},
  }, {
    enabled: true,
  });

  const [properties, setProperties] = useState([]);
  const [filteredProperties, setFilteredProperties] = useState([]);
  const { register, handleSubmit, reset } = useForm();

   const { data: assetStatusData } = Digit.Hooks.useCustomMDMS(
    Digit.ULBService.getStateId(), 
    "Estate", 
    [{ name: "AssetStatus" }],
    {
      select: (data) => {
        const formattedData = data?.["Estate"]?.["AssetStatus"];
        return formattedData;
      },
    }
  );

    const { data: assetTypeData } = Digit.Hooks.useCustomMDMS(
    Digit.ULBService.getStateId(), 
    "Estate", 
    [{ name: "AssetType" }],
    {
      select: (data) => {
        const formattedData = data?.["Estate"]?.["AssetType"];
        return formattedData;
      },
    }
  );

  let assetStatusOptions = [{ code: "", name: "All", i18nKey: "ES_COMMON_ALL" }];
  if (assetStatusData && assetStatusData.length > 0) {
    assetStatusData.forEach((status) => {
      assetStatusOptions.push({ 
        code: status.code, 
        name: status.name, 
        i18nKey: status.name 
      });
    });
  } else {
    assetStatusOptions = [
      { code: "", name: "All", i18nKey: "ES_COMMON_ALL" },
      { code: "Allotted", name: "Allotted", i18nKey: "EST_ALLOTTED" },
      { code: "Available", name: "Available", i18nKey: "EST_AVAILABLE" }
    ];
  }
  let assetTypeOptions = [{ code: "", name: "All", i18nKey: "ES_COMMON_ALL" }];
  if (assetTypeData && assetTypeData.length > 0) {
    assetTypeData.forEach((type) => {
      assetTypeOptions.push({ 
        code: type.code, 
        name: type.name, 
        i18nKey: type.name 
      });
    });
  } else {
    assetTypeOptions = [
    { code: "", name: "All", i18nKey: "ES_COMMON_ALL" },
    { code: "Commercial", name: "Commercial", i18nKey: "EST_COMMERCIAL" },
    { code: "Residential", name: "Residential", i18nKey: "EST_RESIDENTIAL" }
  ];
}
    


  useEffect(() => {
    if (isSuccess && apiData?.Assets) {
      const sortedAssets = apiData.Assets.sort((a, b) => {
        const numA = parseInt(a.estateNo.split('-').pop());
        const numB = parseInt(b.estateNo.split('-').pop());
        return numA - numB;
      });
      setProperties(sortedAssets);
      setFilteredProperties(sortedAssets);
    }
  }, [isSuccess, apiData]);

  const onFilterSubmit = (data) => {
    let filtered = properties;

    if (data.assetNumber) {
      filtered = filtered.filter(p => 
        p.estateNo?.toLowerCase().includes(data.assetNumber.toLowerCase())
      );
    }

    if (data.buildingName) {
      filtered = filtered.filter(p => 
        p.buildingName?.toLowerCase().includes(data.buildingName.toLowerCase())
      );
    }

    if (data.assetStatus && data.assetStatus !== "") {
      filtered = filtered.filter(p => p.assetStatus === data.assetStatus);
    }

    if (data.assetType && data.assetType !== "") {
      filtered = filtered.filter(p => p.assetType === data.assetType);
    }

    setFilteredProperties(filtered);
  };

  const clearFilters = () => {
    reset();
    setFilteredProperties(properties);
  };

  const GetCell = (value) => <span className="cell-text">{value || "N/A"}</span>;

  const handleAllotAsset = (asset) => {
    history.push("/upyog-ui/employee/est/assignassets/info", { 
      assetData: asset 
    });
  };

  const columns = useMemo(
    () => [
      {
        Header: "Asset Number",
        accessor: "estateNo",
        disableSortBy: true,
        Cell: ({ row }) => {
          return (
            <div>
              <span className="link">
                <Link to={`property-details/${row.original["estateNo"]}`}>
                  {row.original["estateNo"]}
                </Link>
              </span>
            </div>
          );
        },
      },
      {
        Header: "Asset Ref",
        Cell: ({ row }) => GetCell(row.original["assetRef"]),
        disableSortBy: true,
      },
      {
        Header: "Building Name",
        Cell: ({ row }) => GetCell(row.original["buildingName"]),
        disableSortBy: true,
      },
      {
        Header: "Locality",
        Cell: ({ row }) => GetCell(row.original["locality"]),
        disableSortBy: true,
      },
      {
        Header: "Plot Area",
        Cell: ({ row }) => GetCell(row.original["totalFloorArea"]),
        disableSortBy: true,
      },
      {
        Header: "Dimensions",
        Cell: ({ row }) => GetCell(`${row.original["dimensionLength"] || ""}x${row.original["dimensionWidth"] || ""}`),
        disableSortBy: true,
      },
      {
        Header: "Asset Type",
        Cell: ({ row }) => GetCell(row.original["assetType"]),
        disableSortBy: true,
      },
      {
        Header: "Rate/sqft",
        Cell: ({ row }) => GetCell(row.original["rate"]),
        disableSortBy: true,
      },
      {
        Header: "Status",
        Cell: ({ row }) => GetCell(row.original["assetStatus"]),
        disableSortBy: true,
      },
      {
        Header: "Action",
        Cell: ({ row }) => {
          const isAllotted = row.original["assetStatus"] === "Allotted";
          return (
            <button
              onClick={() => !isAllotted && handleAllotAsset(row.original)}
              style={{
                backgroundColor: isAllotted ? "#ccc" : "#007bff",
                color: "white",
                border: "none",
                padding: "6px 10px",
                borderRadius: "4px",
                cursor: isAllotted ? "not-allowed" : "pointer",
                fontSize: "12px"
              }}
              disabled={isAllotted}
            >
              Allot Asset
            </button>
          );
        },
        disableSortBy: true,
      },
    ],
    [properties]
  );

  if (isLoading) return <Loader />;

  return (
    <div>
      <Header>Manage Properties</Header>

      <SearchForm onSubmit={onFilterSubmit} handleSubmit={handleSubmit}>
        <SearchField>
          <label>{t("EST_ASSET_NUMBER")}</label>
          <TextInput name="assetNumber" inputRef={register({})} />
        </SearchField>

        <SearchField>
          <label>{t("EST_BUILDING_NAME")}</label>
          <TextInput name="buildingName" inputRef={register({})} />
        </SearchField>

        <SearchField>
          <label>{t("EST_LOCALITY")}</label>
          <TextInput name="locality" inputRef={register({})} />
        </SearchField>

        <SearchField>
          <label>{t("EST_ASSET_STATUS")}</label>
         <Dropdown
  name="assetStatus"
  inputRef={register({})}
  option={assetStatusOptions}
  optionKey="i18nKey"  // Changed from "name" to "i18nKey"
  selected={{ i18nKey: "ES_COMMON_ALL" }}
  t={t}  // Add translation function
/>
        </SearchField>

        <SearchField>
          <label>{t("EST_ASSET_TYPE")}</label>
          <Dropdown
  name="assetType"
  inputRef={register({})}
  option={assetTypeOptions}
  optionKey="i18nKey"  // Changed from "name" to "i18nKey"
  selected={{ i18nKey: "ES_COMMON_ALL" }}
  t={t}  // Add translation function
/>
        </SearchField>

        <SearchField className="submit">
          <SubmitBar label={t("ES_COMMON_SEARCH")} submit />
          <p
            style={{ marginTop: "10px", cursor: "pointer" }}
            onClick={clearFilters}
          >
            {t("ES_COMMON_CLEAR_ALL")}
          </p>
        </SearchField>
      </SearchForm>

      <div style={{ overflowX: "auto", width: "100%", marginTop: "20px" }}>
        <Table
          t={t}
          data={filteredProperties}
          totalRecords={filteredProperties.length}
          columns={columns}
          manualPagination={false}
          globalSearch={false}
          pageSizeLimit={10}
          getCellProps={(cellInfo) => ({
            style: {
              minWidth: "100px",
              padding: "8px 6px",
              fontSize: "12px",
              textAlign: "center",
              whiteSpace: "nowrap",
            },
          })}
          disableSort={false}
        />
      </div>
    </div>
  );
};

export default ManageProperties;
