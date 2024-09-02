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
  MobileNumber,
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
    // console.log("hcapthca dat: ", hcaptchaRef);
    if (hcaptchaRef.current) {
      hcaptchaRef.current.resetCaptcha();
    }
  };

  function setcertificate_No(e) {
    setCertificate_No(e.target.value);
  }

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
      code: "Property Tax",
      i18nKey: "Property Tax",
      crtNo: 3,
    },
    {
      code: "Water and Sewerage",
      i18nKey: "Water and Sewerage",
      crtNo: 4,
    },
    {
      code: "Trade License",
      i18nKey: "Trade License",
      crtNo: 5,
    },
    {
      code: "Public Grievance Redressal",
      i18nKey: "Public Grievance Redressal",
      crtNo: 6,
    },
    {
      code: "Deslugging Service",
      i18nKey: "Deslugging Service",
      crtNo: 7,
    },
    {
      code: "Fire NOC Certificate",
      i18nKey: "Fire NOC Certificate",
      crtNo: 8,
    },
    {
      code: "Building Plan Approval",
      i18nKey: "Building Plan Approval",
      crtNo: 9,
    },
    {
      code: "Pet Certificate",
      i18nKey: "Pet Certificate",
      crtNo: 10,
    },
  ];

  const dataCMObjectConverter = (name, address, certificateNumber, issueDate, validUpto, certificateStatus) => {
    return {
      name: name,
      address: address,
      certificateNumber: certificateNumber,
      issueDate: issueDate,
      validUpto: validUpto,
      certificateStatus: certificateStatus,
    };
  };

  const getAddress = (module_data) => {
    return (
      (module_data?.address?.doorNo ? module_data?.address?.doorNo : "") +
      (module_data?.address?.landmark ? ", " + module_data?.address?.landmark : "") +
      (module_data.address?.locality ? ", " + module_data.address?.locality : "") +
      (module_data?.address?.addressLine1 ? ", " + module_data?.address?.addressLine1 : "") +
      (module_data?.address?.city ? ", " + module_data?.address?.city : "") +
      (module_data?.address?.pincode ? ", " + module_data?.address?.pincode : "")
    );
  };

  // const onSort = useCallback(
  //   (args) => {
  //     if (args.length === 0) return;
  //     setValue("sortBy", args.id);
  //     setValue("sortOrder", args.desc ? "DESC" : "ASC");
  //   },
  //   [setValue]
  // );

  // function onPageSizeChange(e) {
  //   setValue("limit", Number(e.target.value));
  //   handleSubmit(onSubmit)();
  // }

  // function nextPage() {
  //   setValue("offset", getValues("offset") + getValues("limit"));
  //   handleSubmit(onSubmit)();
  // }
  // function previousPage() {
  //   setValue("offset", getValues("offset") - getValues("limit"));
  //   handleSubmit(onSubmit)();
  // }

  async function ewCertificate({ certificate_No }) {
    const applicationDetails = await Digit.EwService.search({ tenantId, filters: { requestId: certificate_No } });
    const dataew = applicationDetails?.EwasteApplication[0];

    const EW_Req_data = dataCMObjectConverter(
      dataew?.applicant?.applicantName,
      getAddress(dataew),
      dataew?.requestId,
      "-NA-",
      "-NA-",
      dataew?.requestStatus
    );

    setUpdatedData([EW_Req_data]);
  }

  async function chbCertificate({ certificate_No }) {
    const applicationDetails = await Digit.CHBServices.search({ tenantId, filters: { bookingNo: certificate_No } });
    const datachb = applicationDetails?.hallsBookingApplication[0];

    const CHB_Req_data = dataCMObjectConverter(
      datachb?.applicantDetail?.accountHolderName,
      getAddress(datachb),
      datachb.bookingNo,
      convertEpochToDate(datachb?.paymentDate),
      "-NA-",
      datachb?.bookingStatus
    );

    setUpdatedData([CHB_Req_data]);
  }

  async function ptCertificate({ certificate_No }) {
    const applicationDetails = await Digit.PTService.search({ tenantId, filters: { propertyId: certificate_No } });
    const datapt = applicationDetails?.Properties[0];
    // console.log("applicatin pt certificate data ::", datapt)

    const ASSET_Req_data = dataCMObjectConverter(
      datapt?.owners[0]?.additionalDetails?.ownerName,
      getAddress(datapt),
      datapt.propertyId,
      convertEpochToDate(datapt?.owners[0]?.createdDate),
      "-NA-",
      datapt?.status
    );

    setUpdatedData([ASSET_Req_data]);
  }

  async function wsCertificate({ certificate_No }) {
    const applicationDetails = await Digit.WSService.wnsSearch({ tenantId, filters: { applicationNumber: certificate_No } });
    const dataws = applicationDetails;
    console.log("applicatin ws certificate data ::", dataws);

    // const ASSET_Req_data = dataCMObjectConverter(
    //   datapt?.owners[0]?.additionalDetails?.ownerName,
    //   getAddress(datapt),
    //   datapt.propertyId,
    //   convertEpochToDate(datapt?.owners[0]?.createdDate),
    //   "-NA-",
    //   datapt?.status
    // );

    // setUpdatedData([ASSET_Req_data]);
  }

  async function tlCertificate({ certificate_No }) {
    const applicationDetails = await Digit.TLService.TLsearch({ tenantId });
    const datatl = applicationDetails;
    console.log("trade license certificate data ::", datatl);

    // const ASSET_Req_data = dataCMObjectConverter(
    //   datapt?.owners[0]?.additionalDetails?.ownerName,
    //   getAddress(datapt),
    //   datapt.propertyId,
    //   convertEpochToDate(datapt?.owners[0]?.createdDate),
    //   "-NA-",
    //   datapt?.status
    // );

    // setUpdatedData([ASSET_Req_data]);
  }

  async function pgrCertificate({ certificate_No }) {
    const applicationDetails = await Digit.PGRService.search(tenantId, { serviceRequestId: "PG-PGR-2024-05-31-001863" }); // serviceRequestId is hardcoded for testing only and will be made dynamic after fixing problems
    const datapgr = applicationDetails;
    console.log("public grievance certificate data ::", datapgr);

    // const ASSET_Req_data = dataCMObjectConverter(
    //   datapt?.owners[0]?.additionalDetails?.ownerName,
    //   getAddress(datapt),
    //   datapt.propertyId,
    //   convertEpochToDate(datapt?.owners[0]?.createdDate),
    //   "-NA-",
    //   datapt?.status
    // );

    // setUpdatedData([ASSET_Req_data]);
  }

  async function OBPSCertificate({ certificate_No }) {
    const applicationDetails = await Digit.OBPSService.BPASearch(tenantId, { requestor: 9999999999, mobileNumber: 9999999999, limit: 50, offset: 0 }); // serviceRequestId is hardcoded for testing only and will be made dynamic after fixing problems
    const datapgr = applicationDetails;
    console.log("public grievance certificate data ::", datapgr);

    // const ASSET_Req_data = dataCMObjectConverter(
    //   datapt?.owners[0]?.additionalDetails?.ownerName,
    //   getAddress(datapt),
    //   datapt.propertyId,
    //   convertEpochToDate(datapt?.owners[0]?.createdDate),
    //   "-NA-",
    //   datapt?.status
    // );

    // setUpdatedData([ASSET_Req_data]);
  }

  async function fsmCertificate({ certificate_No }) {
    // const applicationDetails = await Digit.OBPSService.BPASearch(tenantId, { requestor: 9999999999, mobileNumber: 9999999999, limit: 50, offset: 0 } ); // serviceRequestId is hardcoded for testing only and will be made dynamic after fixing problems
    // const datapgr = applicationDetails;
    // console.log("public grievance certificate data ::", datapgr);
    // const ASSET_Req_data = dataCMObjectConverter(
    //   datapt?.owners[0]?.additionalDetails?.ownerName,
    //   getAddress(datapt),
    //   datapt.propertyId,
    //   convertEpochToDate(datapt?.owners[0]?.createdDate),
    //   "-NA-",
    //   datapt?.status
    // );
    // setUpdatedData([ASSET_Req_data]);
  }

  async function nocCertificate({ certificate_No }) {
    // const applicationDetails = await Digit.OBPSService.BPASearch(tenantId, { requestor: 9999999999, mobileNumber: 9999999999, limit: 50, offset: 0 } ); // serviceRequestId is hardcoded for testing only and will be made dynamic after fixing problems
    // const datapgr = applicationDetails;
    // console.log("public grievance certificate data ::", datapgr);
    // const ASSET_Req_data = dataCMObjectConverter(
    //   datapt?.owners[0]?.additionalDetails?.ownerName,
    //   getAddress(datapt),
    //   datapt.propertyId,
    //   convertEpochToDate(datapt?.owners[0]?.createdDate),
    //   "-NA-",
    //   datapt?.status
    // );
    // setUpdatedData([ASSET_Req_data]);
  }

  async function petCertificate({ certificate_No }) {
    const applicationDetails = await Digit.PTRService.search({tenantId, filters: { applicationNumber: certificate_No } }); 
    const datapet = applicationDetails?.PetRegistrationApplications[0];
    // console.log("pet certificate data ::", datapet);

    const PTR_Req_data = dataCMObjectConverter(
      datapet?.applicantName,
      getAddress(datapet),
      datapet?.applicationNumber,
      "-NA-",
      datapet?.petDetails?.lastVaccineDate,
      "-NA-"
    );
    setUpdatedData([PTR_Req_data]);
  }

  async function petCertificate({ certificate_No }) {
    const applicationDetails = await Digit; 
    const dataasset = applicationDetails;
    console.log("asset certificate data ::", dataasset);
  
    ASSET_Req_data = dataCMObjectConverter(
      dataasset?.applicantDetail?.accountHolderName,
      getAddress(dataasset),
      convertEpochToDate(dataasset?.paymentDate),
      "-NA-",
      dataasset?.bookingStatus
    );
    setUpdatedData([ASSET_Req_data]);
  }



  // const { data: chb_data, refetch } = Digit.Hooks.chb.useChbSearch({
  //   tenantId,
  //   filters: { bookingNo: certificate_No },
  // });

  // const { isError, error, data: ew_data } = Digit.Hooks.ew.useEWSearch({
  //   tenantId,
  //   filters: { requestId: certificate_No },
  // });

  // const { data: cm_data } = Digit.Hooks.cm.useCMSearch({
  //   tenantId,
  //   filters: {requestId: "PG-EW-2024-08-21-000221"},
  //   type: "ewaste"
  // });

  function onSubmit() {
    switch (certificate_name.crtNo) {
      case 1:
        if (certificate_No) {

          // const dataew = cm_data?.EwasteApplication[0];
          // const addressew =
          //   (dataew?.address?.landmark ? dataew?.address?.landmark + ", " : "") +
          //   (dataew.address?.locality ? dataew.address?.locality + ", " : "") +
          //   (dataew?.address?.addressLine1 ? dataew?.address?.addressLine1 + ", " : "") +
          //   (dataew?.address?.city ? dataew?.address?.city + ", " : "") +
          //   (dataew?.address?.pincode ? dataew?.address?.pincode : "");
          // const EW_Req_data = dataCMObjectConverter(dataew?.applicant?.applicantName, addressew, dataew?.requestId, "-NA-", "-NA-", dataew?.requestStatus);
          // // console.log("request data from ew request :: ", EW_Req_data);
          // // console.log("ewaste hook in cm :", ew_data, dataew);
          // setUpdatedData([EW_Req_data]);
          ewCertificate({ certificate_No });
        }
        break;

      case 2:
        if (certificate_No) {
          chbCertificate({ certificate_No });
        }
        break;

      case 3:
        if (certificate_No) {
          ptCertificate({ certificate_No });
        }
        break;

      case 4:
        if (certificate_No) {
          wsCertificate({ certificate_No });
        }
        break;

      case 5:
        if (certificate_No) {
          tlCertificate({ certificate_No });
        }
        break;

      case 6:
        if (certificate_No) {
          pgrCertificate({ certificate_No });
        }
        break;

      case 7:
        if (certificate_No) {
          fsmCertificate({ certificate_No });
        }
        break;

      case 8:
        if (certificate_No) {
          nocCertificate({ certificate_No });
        }
        break;

      case 9:
        if (certificate_No) {
          OBPSCertificate({ certificate_No });
        }
        break;

        case 10:
          if (certificate_No) {
            petCertificate({ certificate_No });
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
            <SubmitBar
              label={t("ES_COMMON_SEARCH")}
              submit
              //  disabled={!ishuman}
            />
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
                setIshuman(false);
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
            isPaginationRequired={false}
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
