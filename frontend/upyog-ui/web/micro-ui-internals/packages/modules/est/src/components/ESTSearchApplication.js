import React, { useCallback, useMemo, useEffect, useState } from "react";
import { useForm, Controller } from "react-hook-form";
import {
  TextInput,
  SubmitBar,
  CardLabelError,
  SearchForm,
  SearchField,
  Table,
  Card,
  Loader,
  CardText,
  Header,
} from "@upyog/digit-ui-react-components";
import { Link } from "react-router-dom";

const ESTSearchApplication = ({ tenantId, isLoading, t, onSubmit, data, count, setShowToast }) => {
  const isMobile = window.Digit.Utils.browser.isMobile();
  const [properties, setProperties] = useState([]);
  const { register, control, handleSubmit, setValue, getValues, reset, formState } = useForm({
    defaultValues: {
      offset: 0,
      limit: !isMobile && 10,
      sortBy: "createdDate",
      sortOrder: "DESC",
    },
  });

  useEffect(() => {
    register("offset", 0);
    register("limit", 10);
    register("sortBy", "createdDate");
    register("sortOrder", "DESC");
  }, [register]);

  useEffect(() => {
    if (Array.isArray(data)) {
      setProperties(data);
    }
  }, [data]);

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

  const onSort = useCallback((args) => {
    if (args.length === 0) return;
    setValue("sortBy", args.id);
    setValue("sortOrder", args.desc ? "DESC" : "ASC");
  }, []);

  function onPageSizeChange(e) {
    setValue("limit", Number(e.target.value));
    handleSubmit(onSubmit)();
  }

  function nextPage() {
    setValue("offset", getValues("offset") + getValues("limit"));
    handleSubmit(onSubmit)();
  }

  function previousPage() {
    setValue("offset", getValues("offset") - getValues("limit"));
    handleSubmit(onSubmit)();
  }

  return (
    <React.Fragment>
      <div>
        <Header>{t("EST_SEARCH_APPLICATIONS")}</Header>
        <SearchForm onSubmit={onSubmit} handleSubmit={handleSubmit}>
          <SearchField>
            <label>{t("EST_SEARCH_ASSET_NUMBER")}</label>
            <TextInput name="assetNumber" inputRef={register({})} />
          </SearchField>

          <SearchField className="submit">
            <SubmitBar label={t("ES_COMMON_SEARCH")} submit />
            <p
              style={{ marginTop: "10px", cursor: "pointer" }}
              onClick={() => {
                reset({
                  assetNumber: "",
                });
                setShowToast(null);
              }}
            >
              {t(`ES_COMMON_CLEAR_ALL`)}
            </p>
          </SearchField>
        </SearchForm>

        {!isLoading && data?.display ? (
  <Card style={{ marginTop: 20 }}>
    {t(data.display)
      .split("\\n")
      .map((text, index) => (
        <p key={index} style={{ textAlign: "center" }}>
          {text}
        </p>
      ))}
  </Card>
) : !isLoading && Array.isArray(data) && data.length > 0 ? (
  <div style={{ overflowX: "auto", width: "100%", marginTop: "20px" }}>
    <Table
      t={t}
      data={properties}
      totalRecords={count}
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
      onPageSizeChange={onPageSizeChange}
      currentPage={getValues("offset") / getValues("limit")}
      onNextPage={nextPage}
      onPrevPage={previousPage}
      pageSizeLimit={getValues("limit")}
      onSort={onSort}
      disableSort={false}
      sortParams={[{ id: getValues("sortBy"), desc: getValues("sortOrder") === "DESC" ? true : false }]}
    />
  </div>
) : (
  isLoading && <Loader />
)}

      </div>
    </React.Fragment>
  );
};

export default ESTSearchApplication;
