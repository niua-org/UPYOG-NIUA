import React, { useMemo, useState } from "react";
import { 
  Table, 
  Header, 
  SearchField, 
  TextInput, 
  Dropdown, 
  SubmitBar,
  SearchForm 
} from "@upyog/digit-ui-react-components";
import { Link, useHistory } from "react-router-dom";
import { useForm } from "react-hook-form";

const AllProperties = ({ t }) => {
  const history = useHistory();
  const [properties, setProperties] = useState([
    {
      assetNumber: "EST-001-A",
      assetRef: "REF001",
      buildingName: "Central Plaza",
      locality: "Ramnagar",
      totalPlotArea: "5000 sqft",
      dimensions: "50x100 ft",
      assetType: "Residential",
      ratePerSqft: "2000 INR",
      assetStatus: "Allotted"
    },
    {
      assetNumber: "EST002-B",
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
      totalPlotArea: "6000 sqft",
      dimensions: "60x100 ft",
      assetType: "Commercial",
      ratePerSqft: "2500 INR",
      assetStatus: "Allotted"
    }
  ]);

  const [filteredProperties, setFilteredProperties] = useState(properties);
  const { register, handleSubmit, reset } = useForm();

  const assetStatusOptions = [
    { code: "", name: "All" },
    { code: "Allotted", name: "Allotted" },
    { code: "Not Allotted", name: "Not Allotted" }
  ];

  const assetTypeOptions = [
    { code: "", name: "All" },
    { code: "Commercial", name: "Commercial" },
    { code: "Residential", name: "Residential" }
  ];

  const onFilterSubmit = (data) => {
    let filtered = properties;

    if (data.assetNumber) {
      filtered = filtered.filter(p => 
        p.assetNumber.toLowerCase().includes(data.assetNumber.toLowerCase())
      );
    }

    if (data.buildingName) {
      filtered = filtered.filter(p => 
        p.buildingName.toLowerCase().includes(data.buildingName.toLowerCase())
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

  const GetCell = (value) => <span className="cell-text">{value}</span>;

  const handleAllotAsset = (assetNumber) => {
    setProperties(prev => 
      prev.map(property => 
        property.assetNumber === assetNumber 
          ? { ...property, assetStatus: "Allotted" }
          : property
      )
    );
  };

  const columns = useMemo(
    () => [
      {
        Header: "Asset Number",
        accessor: "assetNumber",
        disableSortBy: true,
        Cell: ({ row }) => {
          return (
            <div>
              <span className="link">
                <Link to={`property-details/${row.original["assetNumber"]}`}>
                  {row.original["assetNumber"]}
                </Link>
              </span>
            </div>
          );
        },
      },
      {
        Header: "Asset Ref",
        Cell: ({ row }) => {
          return GetCell(row.original["assetRef"]);
        },
        disableSortBy: true,
      },
      {
        Header: "Building Name",
        Cell: ({ row }) => {
          return GetCell(row.original["buildingName"]);
        },
        disableSortBy: true,
      },
      {
        Header: "Locality",
        Cell: ({ row }) => {
          return GetCell(row.original["locality"]);
        },
        disableSortBy: true,
      },
      {
        Header: "Plot Area",
        Cell: ({ row }) => {
          return GetCell(row.original["totalPlotArea"]);
        },
        disableSortBy: true,
      },
      {
        Header: "Dimensions",
        Cell: ({ row }) => {
          return GetCell(row.original["dimensions"]);
        },
        disableSortBy: true,
      },
      {
        Header: "Asset Type",
        Cell: ({ row }) => {
          return GetCell(row.original["assetType"]);
        },
        disableSortBy: true,
      },
      {
        Header: "Rate/sqft",
        Cell: ({ row }) => {
          return GetCell(row.original["ratePerSqft"]);
        },
        disableSortBy: true,
      },
      {
        Header: "Status",
        Cell: ({ row }) => {
          return GetCell(row.original["assetStatus"]);
        },
        disableSortBy: true,
      },
      {
        Header: "Action",
        Cell: ({ row }) => {
          const isAllotted = row.original["assetStatus"] === "Allotted";
          return (
            <button
              onClick={() => !isAllotted && handleAllotAsset(row.original["assetNumber"])}
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

  return (
    <div>
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
        <Header>All Properties</Header>
      </div>
      
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
