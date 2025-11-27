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
import { Link } from "react-router-dom";
import { useForm } from "react-hook-form";

const AllProperties = ({ t }) => {
  const tenantId = Digit.ULBService.getCurrentTenantId();
  
  const { isLoading, isSuccess, data: apiData } = Digit.Hooks.estate.useESTAssetSearch({
    tenantId,
    filters: {"AssetSearchCriteria": {}},
  }, {
    enabled: true,
  });

  const [properties, setProperties] = useState([]);
  const [filteredProperties, setFilteredProperties] = useState([]);
  const { register, handleSubmit, reset } = useForm();

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

  const assetStatusOptions = [
    { code: "All", name: "All" },
    { code: "active", name: "Active" },
    { code: "inactive", name: "Inactive" }
  ];

  const assetTypeOptions = [
    { code: "All", name: "All" },
    { code: "COMMERCIAL", name: "Commercial" },
    { code: "RESIDENTIAL", name: "Residential" }
  ];

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
        Header: "Floor Area",
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
        Header: "Allotment Status",
        Cell: ({ row }) => GetCell(row.original["assetAllotmentStatus"] || "Available"),
        disableSortBy: true,
      },
    ],
    [properties]
  );

  if (isLoading) return <Loader />;

  return (
    <div>
      <Header>All Properties</Header>

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
          <label>{t("EST_ASSET_STATUS")}</label>
          <Dropdown
            name="assetStatus"
            inputRef={register({})}
            option={assetStatusOptions}
            optionKey="name"
            selected={{ name: "All" }}
          />
        </SearchField>

        <SearchField>
          <label>{t("EST_ASSET_TYPE")}</label>
          <Dropdown
            name="assetType"
            inputRef={register({})}
            option={assetTypeOptions}
            optionKey="name"
            selected={{ name: "All" }}
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

export default AllProperties;