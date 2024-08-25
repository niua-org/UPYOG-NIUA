import React, { useCallback, useMemo, useEffect, useState, useRef, useParams } from "react";
import HCaptcha from "@hcaptcha/react-hcaptcha";
import { hcaptchaDetails } from "../utils";
import { convertEpochToDate } from "../utils";
import { useForm, Controller } from "react-hook-form";
import {
  TextInput,
  SubmitBar,
  ActionBar,
  DatePicker,
  SearchForm,
  Dropdown,
  SearchField,
  Table,
  Card,
  Loader,
  Header,
} from "@nudmcdgnpm/digit-ui-react-components";
import { Link } from "react-router-dom";

const CMSearchApplication = ({ isLoading, t, data, count, setShowToast, ActionBarStyle = {}, MenuStyle = {} }) => {
  const isMobile = window.Digit.Utils.browser.isMobile();
  const todaydate = new Date();
  const today = todaydate.toISOString().split("T")[0];

  const [token, setToken] = useState("");
  const captcha = useRef();
  const [ishuman, setIshuman] = useState(false);
  const tenantId = "pg.citya";
  const [istable, setistable] = useState(false);

  const [certificate_name, setCertificate_name] = useState("");
  const [certificate_No, setCertificate_No] = useState("");

  const hcaptchaRef = useRef(null);

  const resetCaptcha = () => {
    console.log("hcapthca dat: ", hcaptchaRef);
    if (hcaptchaRef.current) {
      hcaptchaRef.current.resetCaptcha();
    }
  };

  function setcertificate_No(e) {
    setCertificate_No(e.target.value);
  }
  // console.log("certificatename and certificate number ::", certificate_name, certificate_No);

  const { register, control, handleSubmit, setValue, getValues, reset, formState } = useForm({
    defaultValues: {
      offset: 0,
      limit: !isMobile && 10,
      sortBy: "commencementDate",
      sortOrder: "DESC",
      fromDate: today,
      toDate: today,
    },
  });

  useEffect(() => {
    if (token) {
      setIshuman(true);
    }
  }, [token]);

  useEffect(() => {
    register("offset", 0);
    register("limit", 10);
    register("sortBy", "commencementDate");
    register("sortOrder", "DESC");
    // setValue("fromDate", today);
    // setValue("toDate", today);
  }, [register, setValue, today]);

  // useEffect(() => {
  //   console.log("switch case data test : :", updatedData);
  // }, [certificate_name, certificate_No]);

  const columns = [
    { Header: t("CITIZEN_NAME"), accessor: "name" },
    { Header: t("CITIZEN_ADDRESS"), accessor: "address" },
    { Header: t("CERTIFICATE_NUMBER"), accessor: "certificateNumber" },
    { Header: t("ISSUE_DATE"), accessor: "issueDate" },
    { Header: t("VALID_UPTO"), accessor: "validUpto" },
    { Header: t("CERTIFICATE_STATUS"), accessor: "certificateStatus" },
  ];

  const [updatedData, setUpdatedData] = useState([
    {
      name: "",
      address: "",
      certificateNumber: "",
      issueDate: "",
      validUpto: "",
      certificateStatus: "",
    },
  ]);

  const certificateTypes = [
    {
      code: "E-Waste Certificate",
      i18nKey: "E-Waste Certificate",
      crtNo: 1,
    },
    {
      code: "Community Hall Booking",
      i18nKey: "Community Hall Booking",
      crtNo: 2,
    },
    {
      code: "Asset Certificate",
      i18nKey: "Asset Certificate",
      crtNo: 3,
    },
  ];

  const dataObjectConverter = (name, address, certificateNumber, issueDate, validUpto, certificateStatus) => {
    return {
      name: name,
      address: address,
      certificateNumber: certificateNumber,
      issueDate: issueDate,
      validUpto: validUpto,
      certificateStatus: certificateStatus,
    };
  };

  let EW_Req_data;
  let CHB_Req_data;

  const onSort = useCallback(
    (args) => {
      if (args.length === 0) return;
      setValue("sortBy", args.id);
      setValue("sortOrder", args.desc ? "DESC" : "ASC");
    },
    [setValue]
  );

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

  const { data: chb_data, refetch } = Digit.Hooks.chb.useChbSearch({
    tenantId,
    filters: { bookingNo: certificate_No },
  });

  const { isError, error, data: ew_data } = Digit.Hooks.ew.useEWSearch({
    tenantId,
    filters: { requestId: certificate_No },
  });

  const { isError: isAuditError, data: asset_data } = Digit.Hooks.asset.useASSETSearch({
    tenantId,
    // filters: { applicationNo: certificate_No, audit: true },
  });

  console.log("asset hook in cm :", asset_data);

  function onSubmit() {
    switch (certificate_name.crtNo) {
      case 1:
        if (certificate_No) {
          // const dataew = ew_data?.EwasteApplication.filter((d) => d.requestId === certificate_No);
          const dataew = ew_data?.EwasteApplication[0];
          const addressew =
            (dataew?.address?.landmark ? dataew?.address?.landmark + ", " : "") +
            (dataew.address?.locality ? dataew.address?.locality + ", " : "") +
            (dataew?.address?.addressLine1 ? dataew?.address?.addressLine1 + ", " : "") +
            (dataew?.address?.city ? dataew?.address?.city + ", " : "") +
            (dataew?.address?.pincode ? dataew?.address?.pincode : "");
          EW_Req_data = dataObjectConverter(dataew?.applicant?.applicantName, addressew, dataew?.requestId, "-NA-", "-NA-", dataew?.requestStatus);
          // console.log("request data from ew request :: ", EW_Req_data);
          // console.log("ewaste hook in cm :", ew_data, dataew);
          setUpdatedData([EW_Req_data]);
        }
        break;

      case 2:
        if (certificate_No) {
          // const datachb = chb_data?.hallsBookingApplication.filter((d) => d.bookingNo === certificate_No);
          const dataasset = asset_data?.hallsBookingApplication[0];
          const addresschb =
            (datachb?.address?.landmark ? datachb?.address?.landmark + ", " : "") +
            (datachb.address?.locality ? datachb.address?.locality + ", " : "") +
            (datachb?.address?.addressLine1 ? datachb?.address?.addressLine1 + ", " : "") +
            (datachb?.address?.city ? datachb?.address?.city + ", " : "") +
            (datachb?.address?.pincode ? datachb?.address?.pincode : "");
          CHB_Req_data = dataObjectConverter(
            datachb?.applicantDetail?.accountHolderName,
            addresschb,
            convertEpochToDate(datachb?.paymentDate),
            "-NA-",
            datachb?.bookingStatus
          );
          // console.log("request data of chb request ::: ", CHB_Req_data);
          // console.log("epoch daten in cm hook :: ", convertEpochToDate(datachb?.paymentDate))
          // console.log("chb hook in cm :", chb_data);
          setUpdatedData([CHB_Req_data]);
        }
        break;

      case 3:
        if (certificate_No) {
          const dataasset = asset_data
          const addressasset =
            (dataasset?.address?.landmark ? dataasset?.address?.landmark + ", " : "") +
            (dataasset.address?.locality ? dataasset.address?.locality + ", " : "") +
            (dataasset?.address?.addressLine1 ? dataasset?.address?.addressLine1 + ", " : "") +
            (dataasset?.address?.city ? dataasset?.address?.city + ", " : "") +
            (dataasset?.address?.pincode ? dataasset?.address?.pincode : "");

          ASSET_Req_data = dataObjectConverter(
            dataasset?.applicantDetail?.accountHolderName,
            addressasset,
            convertEpochToDate(dataasset?.paymentDate),
            "-NA-",
            dataasset?.bookingStatus
          );
          setUpdatedData([ASSET_Req_data]);

        }
        break;

      default:
        console.log("Please select a certificate");
        break;
    }

    setistable(true);
  }

  return (
    <React.Fragment>
      <div>
        <Header>{t("SEARCH_CERTIFICATE")}</Header>
        <Card className={"card-search-heading"}>
          <span style={{ color: "#505A5F" }}>{t("Provide at least one parameter to search for an application")}</span>
        </Card>
        <SearchForm onSubmit={onSubmit} handleSubmit={handleSubmit}>
          <SearchField>
            <label>{t("CERTIFICATE_TYPE")}</label>
            <Controller
              control={control}
              name="certificateType"
              render={(props) => (
                <Dropdown
                  selected={certificate_name}
                  select={setCertificate_name}
                  onBlur={props.onBlur}
                  option={certificateTypes}
                  optionKey="i18nKey"
                  t={t}
                  disable={false}
                />
              )}
            />
          </SearchField>
          <SearchField>
            <label>{t("CERTIFICATE_NUMBER")}</label>
            <TextInput
              name="certificateNo"
              t={t}
              type={"text"}
              optionKey="i18nKey"
              value={certificate_No}
              onChange={setcertificate_No}
              style={{ width: "86%" }}
            />
          </SearchField>
          <SearchField>
            <HCaptcha
              ref={captcha}
              sitekey="51424344-c730-4ac8-beec-0aca56be0754"
              onVerify={(token, ekey) =>
                //  handleVerificationSuccess(token, ekey)
                setToken(token)
              }
              onExpire={(e) => setToken("")}
            />
          </SearchField>

          <SearchField className="ssecondubmit">
            <SubmitBar label={t("ES_COMMON_SEARCH")} submit disabled={!ishuman} />
            <p
              style={{ marginTop: "10px" }}
              onClick={() => {
                reset({
                  applicationNo: "",
                  fromDate: today,
                  toDate: today,
                  status: "",
                  offset: 0,
                  limit: 10,
                  sortBy: "commencementDate",
                  sortOrder: "DESC",
                });
                setShowToast(null);
                previousPage();
                setistable(false);
                // setIshuman(false);
                setCertificate_name("");
                setCertificate_No("");
                resetCaptcha();
              }}
            >
              {t(`ES_COMMON_CLEAR_ALL`)}
            </p>
          </SearchField>
        </SearchForm>
        {/* {!isLoading && data?.display ? (
          <Card style={{ marginTop: 20 }}>
            {t(data.display)
              .split("\\n")
              .map((text, index) => (
                <p key={index} style={{ textAlign: "center" }}>
                  {text}
                </p>
              ))}
          </Card>
        ) : !isLoading && data !== "" ? ( */}
        {istable && (
          <Table
            t={t}
            data={updatedData}
            // totalRecords={count}
            columns={columns}
            getCellProps={(cellInfo) => {
              return {
                style: {
                  minWidth: cellInfo.column.Header === t("CM_INBOX_APPLICATION_NO") ? "240px" : "",
                  padding: "20px 18px",
                  fontSize: "16px",
                },
              };
            }}
            // onPageSizeChange={onPageSizeChange}
            // currentPage={getValues("offset") / getValues("limit")}
            // onNextPage={nextPage}
            // onPrevPage={previousPage}
            // pageSizeLimit={getValues("limit")}
            // onSort={onSort}
            // disableSort={false}
            // sortParams={[{ id: getValues("sortBy"), desc: getValues("sortOrder") === "DESC" ? true : false }]}
          />
        )}

        {/* ) : (
          data !== "" || (isLoading && <Loader />)
        )} */}
      </div>
    </React.Fragment>
  );
};

export default CMSearchApplication;
