import React, { Fragment, useMemo, useState } from "react";
import { Header, Loader, Table } from "@upyog/digit-ui-react-components";

const ESTPaymentApplication = ({ t, tenantId, isLoading, data, onSubmit, setShowToast, count }) => {
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  const columns = useMemo(() => [
    {
      Header: t("Payment ID"),
      accessor: "paymentId",
    },
    {
      Header: t("Application Number"),
      accessor: "applicationNumber",
    },
    {
      Header: t("Amount"),
      accessor: "amount",
      Cell: ({ row }) => `₹${row.original.amount}`,
    },
    {
      Header: t("Payment Date"),
      accessor: "paymentDate",
    },
    {
      Header: t("Payment Type"),
      accessor: "paymentType",
    },
    {
      Header: t("Status"),
      accessor: "status",
      Cell: ({ row }) => (
        <span style={{ 
          color: row.original.status === "Success" ? "green" : "red",
          fontWeight: "bold"
        }}>
          {row.original.status}
        </span>
      )
    },
    {
      Header: t("Receipt"),
      accessor: "receiptNumber",
      Cell: ({ row }) => (
        <button 
          onClick={() => downloadReceipt(row.original)}
          style={{ 
            background: "#0B4B66", 
            color: "white", 
            border: "none", 
            padding: "5px 10px", 
            borderRadius: "4px",
            cursor: "pointer"
          }}
        >
          Download
        </button>
      )
    }
  ], [t]);

  const downloadReceipt = (payment) => {
    console.log("Downloading receipt for:", payment.receiptNumber);
  };

  if (isLoading) {
    return <Loader />;
  }

  return (
    <Fragment>
      <Header>Payment History {count ? `(${count})` : ""}</Header>
      
      {data?.length > 0 ? (
        <Table
          t={t}
          data={data}
          columns={columns}
          globalSearch={false}
          manualPagination={true}
          pageSizeLimit={pageSize}
          totalRecords={count}
          currentPage={currentPage}
          onNextPage={() => setCurrentPage(currentPage + 1)}
          onPrevPage={() => setCurrentPage(currentPage - 1)}
          onPageSizeChange={(newSize) => setPageSize(newSize)}
        />
      ) : (
        <div style={{ padding: "20px", textAlign: "center" }}>
          <p>No payment records found</p>
        </div>
      )}
    </Fragment>
  );
};

export default ESTPaymentApplication;
