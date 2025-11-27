import React, { useMemo, useState, useEffect } from "react";
import {
  Table,
  Header,
  Card,
  SearchField,
  SearchForm,
  TextInput,
  SubmitBar,
  Loader,
} from "@upyog/digit-ui-react-components";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next";
import { useHistory } from "react-router-dom";

const ESTPropertyAllotteeDetails = () => {
  const { t } = useTranslation();
  const history = useHistory();
  const tenantId = Digit.ULBService.getCurrentTenantId();

  const [allotteeDetails, setAllotteeDetails] = useState([]);
  const [filteredData, setFilteredData] = useState([]);
  const [loading, setLoading] = useState(true);
  const { register, handleSubmit, reset } = useForm();
  const isMountedRef = React.useRef(true);
  const stickyTableStyle = `
  .sticky-table thead th {
    position: sticky !important;
    top: 0 !important;
    background-color: #f5f5f5 !important;
    z-index: 10 !important;
    border-bottom: 2px solid #ddd !important;
  }
`;
  // Fetch allotment data on component mount
  useEffect(() => {
    const styleElement = document.createElement("style");
    styleElement.innerHTML = stickyTableStyle;
    document.head.appendChild(styleElement);
     
    fetchAllotmentData();
    return () => {
      isMountedRef.current = false;
    };
  },
   []);

  const fetchAllotmentData = async (searchParams = {}) => {
  if (!isMountedRef.current) return;
  setLoading(true);
  try {
    const response = await Digit.ESTService.allotmentSearch({
      tenantId,
      filters: {
        tenantId,
        ...searchParams
      }
    });
    if (!isMountedRef.current) return;
    
     const allotments = response?.Allotments || [];
    
    setAllotteeDetails(allotments);
    setFilteredData(allotments);
  } catch (error) {
    console.error("Error fetching allotment data:", error);
    if (isMountedRef.current){
      setAllotteeDetails([]);
      setFilteredData([]);
    }
  } finally {
    if (isMountedRef.current)
      setLoading(false);
  }
};

  const onSearch = (data) => {
    const searchParams = {};
    if (data.assetNumber) searchParams.assetNo = data.assetNumber;
    if (data.allotteeName) searchParams.alloteeName = data.allotteeName;
    
    fetchAllotmentData(searchParams);
  };

  const clearFilters = () => {
    reset();
    fetchAllotmentData();
  };

 const GetCell = (value) => {
  if (!value || value === "string" || value === "0" || value === 0) {
    return <span className="cell-text">N/A</span>;
  }
  return <span className="cell-text">{value}</span>;
};

  const formatDate = (timestamp) => {
    if (!timestamp) return "N/A";
    return new Date(timestamp).toLocaleDateString("en-GB");
  };

  const columns = useMemo(
  () => [
    { 
      Header: <div style={{whiteSpace: "normal", lineHeight: "1.2", textAlign: "center"}}>Asset<br/>Number</div>, 
      accessor: "assetNo", 
      disableSortBy: true, 
      Cell: ({ row }) => GetCell(row.original.assetNo) 
    },
    { 
      Header: <div style={{whiteSpace: "normal", lineHeight: "1.2", textAlign: "center"}}>Allottee<br/>Name</div>, 
      accessor: "alloteeName", 
      disableSortBy: true, 
      Cell: ({ row }) => GetCell(row.original.alloteeName) 
    },
    { 
      Header: <div style={{whiteSpace: "normal", lineHeight: "1.2", textAlign: "center"}}>Phone<br/>Number</div>, 
      accessor: "mobileNo", 
      disableSortBy: true, 
      Cell: ({ row }) => {
  const mobile = row.original.mobileNo === "string" ? "" : row.original.mobileNo;
  const altMobile = row.original.alternateMobileNo === "string" ? "" : row.original.alternateMobileNo;
  const phoneText = mobile && altMobile ? `${mobile} / ${altMobile}` : mobile || altMobile;
  return GetCell(phoneText); }
    },
    { 
      Header: <div style={{whiteSpace: "normal", lineHeight: "1.2", textAlign: "center"}}>Email<br/>ID</div>, 
      accessor: "emailId", 
      disableSortBy: true, 
      Cell: ({ row }) => GetCell(row.original.emailId) 
    },
    { 
  Header: <div style={{whiteSpace: "normal", lineHeight: "1.2", textAlign: "center"}}>Duration<br/>(Years)</div>, 
  accessor: "duration", 
  disableSortBy: true, 
  Cell: ({ row }) => {
    const duration = row.original.duration;
    // Don't filter out 0 for duration - it's a valid value
    return <span className="cell-text">{duration !== null && duration !== undefined && duration !== "" ? duration : "N/A"}</span>;
  }
},
    { 
      Header: <div style={{whiteSpace: "normal", lineHeight: "1.2", textAlign: "center"}}>Monthly<br/>Rent</div>, 
      accessor: "monthlyRent", 
      disableSortBy: true, 
      Cell: ({ row }) => {
      const rent = row.original.monthlyRent;
      return GetCell(rent && rent !== "string" && rent !== 0 ? `Rs.${rent}` : "");
    } 
    },
    { 
      Header: <div style={{whiteSpace: "normal", lineHeight: "1.2", textAlign: "center"}}>Advance<br/>Payment</div>, 
      accessor: "advancePayment", 
      disableSortBy: true, 
      Cell: ({ row }) => {
      const advance = row.original.advancePayment;
      return GetCell(advance && advance !== "string" && advance !== 0 ? `Rs.${advance}` : "");
}
 
    },
    { 
      Header: <div style={{whiteSpace: "normal", lineHeight: "1.2", textAlign: "center"}}>E-Office<br/>File No</div>, 
      accessor: "eofficeFileNo", 
      disableSortBy: true, 
      Cell: ({ row }) => GetCell(row.original.eofficeFileNo) 
    },
  ],
  [t]
);

  if (loading) return <Loader />;

  return (
    <React.Fragment>
      <div>
       <div style={{ padding: "0 20px", maxWidth: "100%", margin: "0 auto" }}>
          <Header>{t("EST_COMMON_ALLOTTEE_DETAILS")}</Header>
        </div>
        
        <div style={{ margin: "20px" }}>
          <SearchForm onSubmit={onSearch} handleSubmit={handleSubmit}>
            <SearchField>
              <label>{t("EST_ASSET_NUMBER")}</label>
              <TextInput name="assetNumber" inputRef={register({})} />
            </SearchField>

            <SearchField>
              <label>{t("EST_ALLOTTEE_NAME")}</label>
              <TextInput name="allotteeName" inputRef={register({})} />
            </SearchField>

            <SearchField className="submit">
              <SubmitBar label={t("ES_COMMON_SEARCH")} submit />
              <p style={{ marginTop: "10px", cursor: "pointer" }} onClick={clearFilters}>
                {t("ES_COMMON_CLEAR_ALL")}
              </p>
            </SearchField>
          </SearchForm>

          <div className="sticky-table" style={{ width: "100%", marginTop: "20px", overflowX: "auto" }}>
  <Table
    t={t}
    data={filteredData}
    columns={columns}
    totalRecords={filteredData.length}
    isPaginationRequired={true}
    pageSizeLimit={10}
    getCellProps={() => ({
      style: {
        padding: "6px",
        fontSize: "14px",
        textAlign: "center",
        whiteSpace: "normal",
        maxWidth: "120px",
        overflow: "hidden",
        wordWrap: "break-word",
      },
    })}
  />
</div>


        </div>
      </div>
    </React.Fragment>
  );
};

export default ESTPropertyAllotteeDetails;