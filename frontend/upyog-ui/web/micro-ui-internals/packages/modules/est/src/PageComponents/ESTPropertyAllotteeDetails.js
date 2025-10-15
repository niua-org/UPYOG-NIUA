import React, { useMemo, useState } from "react";
import {
  Table,
  Header,
  Card,
  SearchField,
  SearchForm,
  TextInput,
  SubmitBar,
} from "@upyog/digit-ui-react-components";
import { useForm } from "react-hook-form";
import { useTranslation } from "react-i18next"; // ✅ Import translation hook

const ESTPropertyAllotteeDetails = () => {
  const { t } = useTranslation(); // ✅ Initialize translation function

  // ✅ Allottee sample data
  const [allotteeDetails] = useState([
    {
      assetNumber: "AS-123452BB",
      assetRef: "AS-123452BB",
      allotteeName: "Rahul Sharma",
      allotteePhone: "8888800999 / 8888800877",
      allotteeEmail: "rahul@gmail.com",
      duration: "10",
      monthlyRent: "Rs.15,000",
      lastPaymentDate: "11-05-2025",
      duePaymentDate: "11-06-2025",
      agreementStartDate: "11-03-2025",
      advancePayment: "50,00,000",
    },
    {
      assetNumber: "AS-123452CC",
      assetRef: "AS-123452CC",
      allotteeName: "Amit Verma",
      allotteePhone: "9999911122 / 8888800877",
      allotteeEmail: "amit@example.com",
      duration: "8",
      monthlyRent: "Rs.12,000",
      lastPaymentDate: "10-04-2025",
      duePaymentDate: "10-05-2025",
      agreementStartDate: "10-03-2025",
      advancePayment: "30,00,000",
    },
    {
      assetNumber: "AS-12345DDD",
      assetRef: "AS-123452DD",
      allotteeName: "Motshin Khan",
      allotteePhone: "9821414213 / 9999999999",
      allotteeEmail: "motshin@niua.org",
      duration: "10",
      monthlyRent: "Rs.15,000",
      lastPaymentDate: "15-04-2025",
      duePaymentDate: "16-05-2025",
      agreementStartDate: "20-03-2025",
      advancePayment: "30,00,000",
    },
  ]);

  const [filteredData, setFilteredData] = useState(allotteeDetails);
  const { register, handleSubmit, reset } = useForm();


  const onSearch = (data) => {
    let filtered = allotteeDetails;

    if (data.assetNumber) {
      filtered = filtered.filter((p) =>
        p.assetNumber.toLowerCase().includes(data.assetNumber.toLowerCase())
      );
    }
    if (data.assetRef) {
      filtered = filtered.filter((p) =>
        p.assetRef.toLowerCase().includes(data.assetRef.toLowerCase())
      );
    }
    if (data.allotteeName) {
      filtered = filtered.filter((p) =>
        p.allotteeName.toLowerCase().includes(data.allotteeName.toLowerCase())
      );
    }

    setFilteredData(filtered);
  };

  const clearFilters = () => {
    reset();
    setFilteredData(allotteeDetails);
  };

   const GetCell = (value) => <span className="cell-text">{value}</span>;


  const columns = useMemo(
    () => [
      { Header: t("EST_ASSET_NUMBER"), accessor: "assetNumber", Cell: ({ row }) => GetCell(row.original.assetNumber) },
      { Header: t("EST_ASSET_REF_NO"), accessor: "assetRef", Cell: ({ row }) => GetCell(row.original.assetRef) },
      { Header: t("EST_ALLOTTEE_NAME"), accessor: "allotteeName", Cell: ({ row }) => GetCell(row.original.allotteeName) },
      { Header: t("EST_ALLOTTEE_PHONE_NO"), accessor: "allotteePhone", Cell: ({ row }) => GetCell(row.original.allotteePhone) },
      { Header: t("EST_ALLOTTEE_EMAIL_ID"), accessor: "allotteeEmail", Cell: ({ row }) => GetCell(row.original.allotteeEmail) },
      { Header: t("EST_DURATION_YEARS"), accessor: "duration", Cell: ({ row }) => GetCell(row.original.duration) },
      { Header: t("EST_MONTHLY_RENT_INR"), accessor: "monthlyRent", Cell: ({ row }) => GetCell(row.original.monthlyRent) },
      { Header: t("EST_LAST_PAYMENT_DATE"), accessor: "lastPaymentDate", Cell: ({ row }) => GetCell(row.original.lastPaymentDate) },
      { Header: t("EST_DUE_PAYMENT_DATE"), accessor: "duePaymentDate", Cell: ({ row }) => GetCell(row.original.duePaymentDate) },
      { Header: t("EST_AGREEMENT_START_DATE"), accessor: "agreementStartDate", Cell: ({ row }) => GetCell(row.original.agreementStartDate) },
      { Header: t("EST_ADVANCE_PAYMENT_PAID"), accessor: "advancePayment", Cell: ({ row }) => GetCell(row.original.advancePayment) },
    ],
    [t]
  );

  return (
    <div style={{ margin: "20px" }}>
       <Header>{t("EST_COMMON_ALLOTTEE_DETAILS")}</Header>

       <SearchForm onSubmit={onSearch} handleSubmit={handleSubmit}>
        <SearchField>
          <label>{t("EST_ASSET_NUMBER")}</label>
          <TextInput name="assetNumber" inputRef={register({})} />
        </SearchField>

        <SearchField>
          <label>{t("EST_ASSET_REF_NO")}</label>
          <TextInput name="assetRef" inputRef={register({})} />
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

 
      <div style={{ marginLeft: "-15px", marginRight: "0px", textAlign: "left" }}>
        <Card style={{ marginTop: "10px", padding: "10px", textAlign: "left" }}>
          {filteredData.length > 0 ? (
            <Table
              t={t}
              data={filteredData}
              totalRecords={filteredData.length}
              columns={columns}
              getCellProps={() => ({
                style: {
                  padding: "10px",
                  fontSize: "13px",
                  textAlign: "left",
                  whiteSpace: "normal",
                  wordBreak: "break-word",
                  maxWidth: "180px",
                },
              })}
            />
          ) : (
            <p style={{ textAlign: "center", padding: "20px" }}>
              {t("ES_COMMON_NO_RECORDS_FOUND")}
            </p>
          )}
        </Card>
      </div>
    </div>
  );
};

export default ESTPropertyAllotteeDetails;
