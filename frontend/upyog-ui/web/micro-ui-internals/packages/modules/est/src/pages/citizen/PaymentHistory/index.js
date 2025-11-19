import React, { useState, useEffect, useMemo } from "react";
import { Header, Loader, TextInput, SubmitBar, Card, Table, SearchForm, SearchField } from "@upyog/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import { useForm } from "react-hook-form";

export const ESTPaymentHistory = () => {
  const { t } = useTranslation();
  const tenantId = Digit.ULBService.getCitizenCurrentTenant(true) || Digit.ULBService.getCurrentTenantId();
  const user = Digit.UserService.getUser().info;

  const [paymentData, setPaymentData] = useState([]);
  const [loading, setLoading] = useState(true);
  const { register, handleSubmit, reset } = useForm();

  const stickyTableStyle = `
    .sticky-table thead th {
      position: sticky !important;
      top: 0 !important;
      background-color: #f5f5f5 !important;
      z-index: 10 !important;
      border-bottom: 2px solid #ddd !important;
    }
  `;

  useEffect(() => {
    const styleElement = document.createElement("style");
    styleElement.innerHTML = stickyTableStyle;
    document.head.appendChild(styleElement);
    return () => {
      document.head.removeChild(styleElement);
    };
  }, []);

  useEffect(() => {
    fetchPaymentHistory();
  }, []);

  const fetchPaymentHistory = async (searchParams = {}) => {
  setLoading(true);
  try {
    const response = await Digit.ESTService.allotmentSearch({
      tenantId,
      filters: {
        tenantId,
        mobileNo: user?.mobileNumber,
        ...searchParams
      }
    });
    
    const allotments = response?.Allotments || [];
    
    // Filter by mobile number on frontend as backup
    const filteredAllotments = allotments.filter(allotment => 
      allotment?.mobileNo === user?.mobileNumber
    );
    
    const paymentsWithHistory = filteredAllotments.map((allotment, index) => {
  console.log("Allotment data:", allotment); // Debug log
  
  return {
    srNo: index + 1,
    assetNo: allotment?.assetNo,
    assetRefNo: allotment?.refAssetNo || allotment?.estateNo || allotment?.assetRefNo || "N/A",
    monthlyRent: allotment?.monthlyRent || 0,
    duration: allotment?.duration || 0,
    agreementStartDate: allotment?.auditDetails?.createdTime ? 
      new Date(allotment.auditDetails.createdTime).toLocaleDateString("en-GB") : "N/A",
    alloteeName: allotment?.alloteeName,
    mobileNo: allotment?.mobileNo,
    paymentDate: "N/A",
    lastPaymentDate: "N/A", 
    previousMonthPaymentDate: "N/A",
    due: "Pending",
    lateFee: 0,
    duePayment: allotment?.monthlyRent || 0,
    paymentStatus: "Pending"
  };
});


    setPaymentData(paymentsWithHistory);
  } catch (error) {
    console.error("Error fetching payment history:", error);
    setPaymentData([]);
  } finally {
    setLoading(false);
  }
};

  const onSearch = (data) => {
    const searchParams = {};
    if (data.assetNumber) searchParams.assetNo = data.assetNumber;
    if (data.paymentStatus && data.paymentStatus !== "All") searchParams.status = data.paymentStatus;
    
    fetchPaymentHistory(searchParams);
  };

  const clearFilters = () => {
    reset();
    fetchPaymentHistory();
  };

  const GetCell = (value) => {
    if (!value || value === "string" || value === "0" || value === 0) {
      return <span className="cell-text">N/A</span>;
    }
    return <span className="cell-text">{value}</span>;
  };

  const columns = useMemo(
    () => [
      {
        Header: <div style={{whiteSpace:"normal", lineHeight:"1.2", textAlign: "center"}}>Asset<br/>Number</div>,
        accessor: "assetNo",
        Cell: ({ row }) => GetCell(row.original.assetNo),
        disableSortBy: true,
      },
      {
        Header: <div style={{whiteSpace:"normal", lineHeight:"1.2", textAlign: "center"}}>Asset Ref.<br/>No.</div>,
        accessor: "assetRefNo",
        Cell: ({ row }) => GetCell(row.original.assetRefNo),
        disableSortBy: true,
      },
      {
        Header: <div style={{whiteSpace:"normal", lineHeight:"1.2", textAlign: "center"}}>Monthly Rent<br/>(INR)</div>,
        accessor: "monthlyRent",
        Cell: ({ row }) => {
          const rent = row.original.monthlyRent;
          return GetCell(rent && rent !== "string" && rent !== 0 ? `₹${rent}` : "");
        },
        disableSortBy: true,
      },
      {
        Header: <div style={{whiteSpace:"normal", lineHeight:"1.2", textAlign: "center"}}>Payment<br/>Due Date</div>,
        accessor: "dueDate",
        Cell: ({ row }) => GetCell(row.original.due),
        disableSortBy: true,
      },
      {
        Header: <div style={{whiteSpace:"normal", lineHeight:"1.2", textAlign: "center"}}>Payment<br/>Date</div>,
        accessor: "paymentDate",
        Cell: ({ row }) => GetCell(row.original.paymentDate),
        disableSortBy: true,
      },
      {
        Header: <div style={{whiteSpace:"normal", lineHeight:"1.2", textAlign: "center"}}>Last Date of<br/>Payment</div>,
        accessor: "lastPaymentDate",
        Cell: ({ row }) => GetCell(row.original.lastPaymentDate),
        disableSortBy: true,
      },
      {
        Header: <div style={{whiteSpace:"normal", lineHeight:"1.2", textAlign: "center"}}>Previous Month<br/>Payment Date</div>,
        accessor: "previousMonthPaymentDate",
        Cell: ({ row }) => GetCell(row.original.previousMonthPaymentDate),
        disableSortBy: true,
      },
      {
        Header: <div style={{whiteSpace:"normal", lineHeight:"1.2", textAlign: "center"}}>Duration<br/>(Years)</div>,
        accessor: "duration",
        Cell: ({ row }) => {
          const duration = row.original.duration;
          return <span className="cell-text">{duration !== null && duration !== undefined && duration !== "" ? duration : "N/A"}</span>;
        },
        disableSortBy: true,
      },
      {
        Header: <div style={{whiteSpace:"normal", lineHeight:"1.2", textAlign: "center"}}>Fine/ <br/>Late fee</div>,
        accessor: "lateFee",
        Cell: ({ row }) => {
          const fee = row.original.lateFee;
          return GetCell(fee && fee !== "string" && fee !== 0 ? `₹${fee}` : "");
        },
        disableSortBy: true,
      },
      {
        Header: <div style={{whiteSpace:"normal", lineHeight:"1.2", textAlign: "center"}}>Due</div>,
        accessor: "duePayment",
        Cell: ({ row }) => {
          const due = row.original.duePayment;
          return GetCell(due && due !== "string" && due !== 0 ? `₹${due}` : "");
        },
        disableSortBy: true,
      },
      {
        Header: <div style={{whiteSpace:"normal", lineHeight:"1.2", textAlign: "center"}}>Agreement<br/>Start Date</div>,
        accessor: "agreementStartDate",
        Cell: ({ row }) => GetCell(row.original.agreementStartDate),
        disableSortBy: true,
      },
      {
        Header: <div style={{whiteSpace:"normal", lineHeight:"1.2", textAlign: "center"}}>Payment<br/>Status</div>,
        accessor: "paymentStatus",
        Cell: ({ row }) => GetCell(row.original.paymentStatus),
        disableSortBy: true,
      },
    ],
    [t]
  );

  if (loading) return <Loader />;

  return (
    <React.Fragment>
      <div>
        <div style={{ padding: "0 20px", maxWidth: "100%", margin: "0 auto" }}>
          <Header>{t("EST_PAYMENT_HISTORY")}</Header>
        </div>
        
        <div style={{ margin: "20px" }}>
          <SearchForm onSubmit={onSearch} handleSubmit={handleSubmit}>
            <SearchField>
              <label>{t("EST_ASSET_NUMBER")}</label>
              <TextInput name="assetNumber" inputRef={register({})} />
            </SearchField>

            <SearchField>
              <label>{t("EST_PAYMENT_STATUS")}</label>
              <TextInput name="paymentStatus" inputRef={register({})} />
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
              data={paymentData}
              columns={columns}
              totalRecords={paymentData.length}
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

export default ESTPaymentHistory;