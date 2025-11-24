import React, { useState, useEffect, useMemo } from "react";
import { Header, Loader, TextInput, SubmitBar, Card, Table } from "@upyog/digit-ui-react-components";
import { useTranslation } from "react-i18next";

export const ESTPaymentHistory = () => {
  const { t } = useTranslation();
  const tenantId = Digit.ULBService.getCitizenCurrentTenant(true) || Digit.ULBService.getCurrentTenantId();
  const user = Digit.UserService.getUser().info;

  const [paymentData, setPaymentData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");

  useEffect(() => {
    fetchPaymentHistory();
  }, []);

  const fetchPaymentHistory = async () => {
    setLoading(true);
    try {
      const response = await Digit.ESTService.allotmentSearch({
        tenantId,
        filters: {
          tenantId,
          limit: "100"
        }
      });
      
      const allotments = response?.Allotments || [];
      
      // Filter by user's mobile number
      const userAllotments = allotments.filter(allotment => 
        allotment?.mobileNo === user?.mobileNumber
      );

      // Transform to payment history format
      const paymentHistoryData = userAllotments.map((allotment) => {
        const createdDate = allotment?.auditDetails?.createdTime ? 
          new Date(allotment.auditDetails.createdTime) : new Date();
        
        const nextDue = new Date();
        nextDue.setMonth(nextDue.getMonth() + 1);
        nextDue.setDate(1);

        const lastPayment = new Date(createdDate);
        lastPayment.setMonth(lastPayment.getMonth() + 1);
        
        const previousPayment = new Date(createdDate);

        return {
          assetNo: allotment?.assetNo || "N/A",
          assetRefNo: allotment?.applicationNo || allotment?.refAssetNo || allotment?.id || "N/A",
          monthlyRent: allotment?.monthlyRent || 0,
          due: nextDue.toLocaleDateString("en-GB"),
          paymentDate: lastPayment.toLocaleDateString("en-GB"),
          lastPaymentDate: lastPayment.toLocaleDateString("en-GB"),
          previousMonthPaymentDate: previousPayment.toLocaleDateString("en-GB"),
          duration: allotment?.duration || 0,
          lateFee: 0,
          duePayment: allotment?.monthlyRent || 0,
          agreementStartDate: createdDate.toLocaleDateString("en-GB"),
          paymentStatus: allotment?.status === "ACTIVE" ? "Paid" : "Pending"
        };
      });

      setPaymentData(paymentHistoryData);
      
    } catch (error) {
      console.error("Error fetching payment history:", error);
      setPaymentData([]);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = () => {
    fetchPaymentHistory();
  };

  const columns = useMemo(() => [
    {
      Header: "Asset\nNumber",
      accessor: "assetNo",
    },
    {
      Header: "Asset\nRef. No.",
      accessor: "assetRefNo",
    },
    {
      Header: "Monthly Rent\n(INR)",
      accessor: "monthlyRent",
      Cell: ({ row }) => `₹${row.original.monthlyRent}`,
    },
    {
      Header: "Due",
      accessor: "due",
    },
    {
      Header: "Payment\nDate",
      accessor: "paymentDate",
    },
    {
      Header: "Last Date\nof Payment",
      accessor: "lastPaymentDate",
    },
    {
      Header: "Previous Month\nPayment Date",
      accessor: "previousMonthPaymentDate",
    },
    {
      Header: "Duration\n(Years)",
      accessor: "duration",
    },
    {
      Header: "Fine / Late fee\n(If any)",
      accessor: "lateFee",
      Cell: ({ row }) => row.original.lateFee > 0 ? `₹${row.original.lateFee}` : "N/A",
    },
    {
      Header: "Due\nPayment",
      accessor: "duePayment",
      Cell: ({ row }) => `₹${row.original.duePayment}`,
    },
    {
      Header: "Agreement\nStart Date",
      accessor: "agreementStartDate",
    },
    {
      Header: "Payment\nStatus",
      accessor: "paymentStatus",
      Cell: ({ row }) => (
        <span style={{ 
          color: row.original.paymentStatus === "Paid" ? "green" : "red",
          fontWeight: "bold"
        }}>
          {row.original.paymentStatus}
        </span>
      ),
    },
  ], []);

  // UPYOG Standard getCellProps function
  const getCellProps = (cellInfo) => {
    const columnWidth = {
      "Asset\nNumber": "150px",
      "Asset\nRef. No.": "150px",
      "Payment\nStatus": "140px",
      "Previous Month\nPayment Date": "180px",
      "Fine / Late fee\n(If any)": "160px"
    };
    
    return {
      style: {
        minWidth: columnWidth[cellInfo.column.Header] || "120px",
        padding: "12px 8px",
        textAlign: "center",
        whiteSpace: "pre-line",
      },
    };
  };

  if (loading) return <Loader />;

  return (
    <React.Fragment>
      <div style={{ padding: "20px" }}>
        <Header>EST Payment History ({paymentData.length})</Header>
        
        <Card style={{ marginBottom: "20px" }}>
          <div style={{ 
            display: "flex", 
            gap: "15px", 
            alignItems: "flex-end",
            padding: "10px"
          }}>
            <div style={{ flex: "0 0 auto" }}>
              <label style={{ display: "block", marginBottom: "5px" }}>Asset Number</label>
              <TextInput
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Enter Asset Number"
              />
            </div>
            <div style={{ flex: "0 0 auto" }}>
              <SubmitBar label="Search" onSubmit={handleSearch} />
            </div>
            <div style={{ flex: "0 0 auto" }}>
              <p 
                style={{ 
                  cursor: "pointer", 
                  margin: "0",
                  color: "#000000ff",
                  textDecoration: "underline"
                }} 
                onClick={() => setSearchTerm("")}
              >
                Clear All
              </p>
            </div>
          </div>
        </Card>

        <div style={{ 
          overflowX: "auto",
          width: "100%",
          minWidth: "1600px"
        }}>
          <Table
            t={t}
            data={paymentData}
            columns={columns}
            getCellProps={getCellProps}
            disableSort={false}
            onSort={() => {}}
            isPaginationRequired={false}
            totalRecords={paymentData.length}
          />
        </div>
      </div>
    </React.Fragment>
  );
};

export default ESTPaymentHistory;
