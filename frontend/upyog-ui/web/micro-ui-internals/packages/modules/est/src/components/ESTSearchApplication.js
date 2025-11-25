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
  BreadCrumb,
  Dropdown,
} from "@upyog/digit-ui-react-components";
import { Link, useHistory, useLocation } from "react-router-dom";

const ESTSearchApplication = ({ tenantId, isLoading, t, onSubmit, data, count, setShowToast }) => {
  const history = useHistory();
  const isMobile = window.Digit.Utils.browser.isMobile();
  const [properties, setProperties] = useState([]);
  const { register, handleSubmit, setValue, getValues, reset } = useForm({
    defaultValues: {
      offset: 0,
      limit: 10,
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

  const GetCell = (value) => <span className="cell-text">{value || "N/A"}</span>;

  const handleAllotAsset = (asset) => {
    history.push("/upyog-ui/employee/est/assignassets/info",{ assetData: asset });
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
                <Link to={`property-details/${row.original["esateNo"]}`}>
                  {row.original["estateNo"]}
                </Link>
              </span>
            </div>
          );
        },
      },
      {
        Header: "Asset Ref",
        Cell: ({ row }) => GetCell(row.original["refAssetNo"]),
        disableSortBy: true,
      },
      {
        Header: "Building Name",
        Cell: ({ row }) => GetCell(row.original["buildingName"]),
        disableSortBy: true,
      },
      {
        Header: "Locality",
        Cell: ({ row }) => GetCell(t(row.original["locality"])),
        disableSortBy: true,
      },
      {
        Header: "Plot Area",
        Cell: ({ row }) => GetCell(row.original["totalFloorArea"]),
        disableSortBy: true,
      },
      {
        Header: "Dimensions",
        Cell: ({ row }) => GetCell(`${row.original["dimensionLength"]} x ${row.original["dimensionWidth"]}`),
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
    []
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
            <TextInput name="estateNo" inputRef={register({})} />
          </SearchField>
          
          <SearchField>
            <label>{t("EST_ASSET_TYPE")}</label>
            <Dropdown
              name="assetType"
              inputRef={register({})}
              option={[
                { code: "All", name: "All" },
                { code: "RESIDENTIAL", name: "Residential" },
                { code: "COMMERCIAL", name: "Commercial" },
              ]}
              optionKey="name"
              selected={{ name: "All" }}
              select={() => {}}
              placeholder={t("EST_SELECT_ASSET_TYPE")}
            />
          </SearchField>

          <SearchField className="submit">
            <SubmitBar label={t("ES_COMMON_SEARCH")} submit />
            <p
              style={{ marginTop: "10px", cursor: "pointer" }}
              onClick={() => {
                reset({
                  estateNo: "",
                  assetType: "",
                });
                setShowToast(null);
              }}
            >
              {t(`ES_COMMON_CLEAR_ALL`)}
            </p>
          </SearchField>
        </SearchForm>

        {!isLoading && data?.display ? (
          <Card style={{ marginTop: 20, textAlign: "center" }}>
            {t(data.display)
              .split("\\n")
              .map((text, index) => (
                <p key={index} >
                  {text}
                </p>
              ))}
              <button
              onClick={() => history.push("/upyog-ui/employee/est/create-asset")}
              style={{
                backgroundColor: "#007bff",
                color: "white",
                border: "none",
                padding: "10px 10px",
                borderRadius: "4px",
                cursor: "pointer",
                marginTop: "10px"
              }}
            >
              {t("EST_CREATE_ASSET")}
            </button>
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
