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
} from "@nudmcdgnpm/digit-ui-react-components";
import { Link } from "react-router-dom";
import { useForm } from "react-hook-form";

// All Properties Component
// This component displays all properties with filtering options based on asset number, building name, asset status, and asset type.

const AllProperties = ({ t }) => {
  const tenantId = Digit.ULBService.getCurrentTenantId();
  const [isMobile, setIsMobile] = useState(window.innerWidth <= 768);

  useEffect(() => {
    const handleResize = () => setIsMobile(window.innerWidth <= 768);
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);
  
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
    <div style={{ padding: isMobile ? '10px' : '20px' }}>
      <Header style={{ fontSize: isMobile ? '18px' : '24px', marginBottom: '15px' }}>All Properties</Header>

      <SearchForm onSubmit={onFilterSubmit} handleSubmit={handleSubmit} style={{ display: 'flex', flexDirection: isMobile ? 'column' : 'row', gap: isMobile ? '10px' : '15px', flexWrap: 'wrap' }}>
        <SearchField style={{ marginBottom: isMobile ? '10px' : '15px', flex: isMobile ? '1 1 100%' : '1 1 200px' }}>
          <label style={{ fontSize: isMobile ? '14px' : '16px', marginBottom: '5px', display: 'block' }}>{t("EST_ASSET_NUMBER")}</label>
          <TextInput name="assetNumber" inputRef={register({})} style={{ width: '100%', fontSize: isMobile ? '14px' : '16px', padding: isMobile ? '8px' : '10px' }} />
        </SearchField>

        <SearchField style={{ marginBottom: isMobile ? '10px' : '15px', flex: isMobile ? '1 1 100%' : '1 1 200px' }}>
          <label style={{ fontSize: isMobile ? '14px' : '16px', marginBottom: '5px', display: 'block' }}>{t("EST_BUILDING_NAME")}</label>
          <TextInput name="buildingName" inputRef={register({})} style={{ width: '100%', fontSize: isMobile ? '14px' : '16px', padding: isMobile ? '8px' : '10px' }} />
        </SearchField>

        <SearchField style={{ marginBottom: isMobile ? '10px' : '15px', flex: isMobile ? '1 1 100%' : '1 1 200px' }}>
          <label style={{ fontSize: isMobile ? '14px' : '16px', marginBottom: '5px', display: 'block' }}>{t("EST_ASSET_STATUS")}</label>
          <Dropdown
            name="assetStatus"
            inputRef={register({})}
            option={assetStatusOptions}
            optionKey="name"
            selected={{ name: "All" }}
            style={{ width: '100%', fontSize: isMobile ? '14px' : '16px' }}
          />
        </SearchField>

        <SearchField style={{ marginBottom: isMobile ? '10px' : '15px', flex: isMobile ? '1 1 100%' : '1 1 200px' }}>
          <label style={{ fontSize: isMobile ? '14px' : '16px', marginBottom: '5px', display: 'block' }}>{t("EST_ASSET_TYPE")}</label>
          <Dropdown
            name="assetType"
            inputRef={register({})}
            option={assetTypeOptions}
            optionKey="name"
            selected={{ name: "All" }}
            style={{ width: '100%', fontSize: isMobile ? '14px' : '16px' }}
          />
        </SearchField>

        <SearchField className="submit" style={{ display: 'flex', flexDirection: isMobile ? 'column' : 'row', gap: '10px', alignItems: isMobile ? 'stretch' : 'center', flex: isMobile ? '1 1 100%' : '1 1 200px' }}>
          <SubmitBar label={t("ES_COMMON_SEARCH")} submit style={{ width: isMobile ? '100%' : 'auto', fontSize: isMobile ? '14px' : '16px' }} />
          <p
            style={{ 
              marginTop: isMobile ? "10px" : "0", 
              cursor: "pointer",
              width: isMobile ? '100%' : 'auto',
              textAlign: isMobile ? 'center' : 'left',
              fontSize: isMobile ? '14px' : '16px'
            }}
            onClick={clearFilters}
          >
            {t("ES_COMMON_CLEAR_ALL")}
          </p>
        </SearchField>
      </SearchForm>

      <div style={{ 
        overflowX: "auto", 
        width: "100%", 
        marginTop: "20px",
        WebkitOverflowScrolling: "touch",
        padding: isMobile ? '5px' : '10px'
      }}>
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
              minWidth: isMobile ? "70px" : "100px",
              padding: isMobile ? "4px 2px" : "8px 6px",
              fontSize: isMobile ? "10px" : "12px",
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