import {
  Card,
  CardHeader,
  Header,
  CardSubHeader,
  CardText,
  CheckBox,
  CitizenInfoLabel,
  EditIcon,
  LinkButton,
  Row,
  StatusTable,
  SubmitBar,
  Toast,
  resolveAppPath,
  ConfigTimeline,
} from "@nudmcdgnpm/digit-ui-react-components";
import React, { useEffect, useMemo, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useParams, useLocation } from "react-router-dom";
import TLDocument from "../../../pageComponents/TLDocumets";
import Timeline from "../../../components/TLTimeline";

const getPath = (path, params) => {
  params && Object.keys(params)?.map(key => {
    path = path.replace(`:${key}`, params[key]);
  });
  return path;
};
let tlCheckMounts = 0;

const CheckPage = (props) => {
  const allowedRef = useRef(localStorage.getItem("TLAppSubmitEnabled") === "true");
  useEffect(() => {
    tlCheckMounts += 1;
    return () => {
      tlCheckMounts -= 1;
      setTimeout(() => {
        if (tlCheckMounts === 0) {
          try {
            localStorage.setItem("TLAppSubmitEnabled", "false");
          } catch (e) {
            /* ignore */
          }
        }
      }, 0);
    };
  }, []);

  if (!allowedRef.current) {
    window.location.replace(resolveAppPath("/upyog-ui/citizen"));
    return null;
  }
  return <WrapCheckPage {...props} />;
};
const WrapCheckPage = ({
  onSubmit,
  value,
  timeline,
}) => {
  let isEdit = window.location.href.includes("renew-trade");
  const { t } = useTranslation();
  const navigate = Digit.Hooks.useCustomNavigate();
  const routeParams = useParams();
  const { pathname } = useLocation();
  const { path: modulePath } = Digit.Hooks.useModuleBasePath();
  const basePath = useMemo(() => {
    const pattern = pathname.includes("/renew-trade/")
      ? `${modulePath}/tradelicence/renew-trade/:id/:tenantId`
      : `${modulePath}/tradelicence/edit-application/:id/:tenantId`;
    return getPath(pattern, routeParams);
  }, [modulePath, routeParams, pathname]);
  const [toast, setToast] = useState(null);
  // Consent required before submit (same pattern as Estate / PT DynamicCheckPage).
  const [agree, setAgree] = useState(false);
  const setdeclarationhandler = () => setAgree((prev) => !prev);
  const {
    TradeDetails,
    address,
    owners,
    propertyType,
    subtype,
    pitType,
    pitDetail,
    isEditProperty,
    cpt,
    cptId
  } = value;
  const {
    data: billingSlabTradeTypeData,
    isLoading: isBillingSlabLoading
  } = Digit.Hooks.tl.useTradeLicenseBillingslab({
    tenantId: value?.tenantId || Digit.ULBService.getCurrentTenantId(),
    filters: {}
  }, {
    select: data => {
      return data?.billingSlab.filter(e => e.tradeType && e.applicationType === (window.location.href.includes("renew-trade") ? "RENEWAL" : "NEW") && e.licenseType === "PERMANENT" && e.uom);
    }
  });
  function getdate(date) {
    let newdate = Date.parse(date);
    return `${new Date(newdate).getDate().toString() + "/" + (new Date(newdate).getMonth() + 1).toString() + "/" + new Date(newdate).getFullYear().toString()}`;
  }
  function routeTo(jumpTo) {
    sessionStorage.getItem("isDirectRenewal") ? sessionStorage.removeItem("isDirectRenewal") : "";
    navigate(jumpTo);
    //location.href = jumpTo;
  }
  useEffect(() => {
    if (sessionStorage.getItem("isCreateEnabledEmployee") === "true") {
      sessionStorage.removeItem("isCreateEnabledEmployee");
      navigate("/employee", { replace: true });
    }
    else
      sessionStorage.removeItem("isCreateEnabledEmployee");

  })

  const CheckForBillingSlab = () => {
    if (window.location.href.includes("renew-trade") && value) {
      let flag = true;
      value?.TradeDetails?.units?.map(val => {
        if (val && billingSlabTradeTypeData?.filter(ob => ob?.tradeType === val?.tradesubtype?.code && (ob?.structureType === value?.TradeDetails?.VehicleType?.code || ob?.structureType === value?.TradeDetails?.BuildingType?.code))?.length <= 0) {
          flag = false;
          setToast({
            key: "error",
            message: "TL_BILLING_SLAB_NOT_FOUND_FOR_COMB"
          });
        }
      });
      if (flag) onSubmit();
    } else onSubmit();
  };
  const typeOfApplication = !isEditProperty ? `new-application` : `renew-trade`;
  let routeLink = resolveAppPath(`/upyog-ui/citizen/tl/tradelicence/${typeOfApplication}`);
  if (window.location.href.includes("edit-application") || window.location.href.includes("renew-trade")) {
    routeLink = `${basePath}`;
    routeLink = routeLink.replace("/check", "");
  }
  // New application and renewal edits open the accordion at /apply.
  // The older edit-application flow still uses the step named on each pencil.
  const opensAccordion =
    pathname.includes("/renew-trade/") ||
    (value?.__dynamicForm && !pathname.includes("/edit-application/"));
  const editRoute = opensAccordion ? `${routeLink}/apply` : null;
  const goEdit = (legacyPath) => routeTo(editRoute || `${routeLink}/${legacyPath}`);
  return <React.Fragment>
    {window.location.href.includes("/citizen") ? (
      value?.__dynamicForm && timeline?.steps?.length ? (
        <ConfigTimeline timeline={timeline} />
      ) : (
        <Timeline currentStep={4} />
      )
    ) : null}

    <Header styles={{
      fontSize: "32px"
    }}>{t("TL_COMMON_SUMMARY")}</Header>
    <Card className="tl-check-page-card">
      <CardHeader styles={{
        fontSize: "28px"
      }}>{t("TL_LOCALIZATION_TRADE_DETAILS")}</CardHeader>
      <StatusTable>
        <LinkButton label={<EditIcon className="tl-check-page-btn-relative-mt-neg" />} onClick={() => goEdit("TradeName")} className="tl-check-page-btn" />
        <Row className="border-none" textStyle={{
          marginRight: "-10px"
        }} label={t("TL_LOCALIZATION_TRADE_NAME")} text={t(TradeDetails?.TradeName)} />
        <Row className="border-none" label={t("TL_STRUCTURE_TYPE")} text={t(`TL_${TradeDetails?.StructureType?.code}`)} />
        <Row className="border-none" label={t("TL_STRUCTURE_SUB_TYPE")} text={t(TradeDetails?.StructureType?.code !== "IMMOVABLE" ? TradeDetails?.VehicleType?.i18nKey : TradeDetails?.BuildingType?.i18nKey)} />
        <Row className="border-none" label={t("TL_TRADE_GST_NO")} text={TradeDetails?.TradeGSTNumber || t("CS_NA")} />
        <Row className="border-none" label={t("TL_OPERATIONAL_AREA")} text={TradeDetails?.OperationalSqFtArea || t("CS_NA")} />
        <Row className="border-none" label={t("TL_NO_OF_EMPLOYEES")} text={TradeDetails?.NumberOfEmployees || t("CS_NA")} />
        <Row className="border-none" label={t("TL_NEW_TRADE_DETAILS_TRADE_COMM_DATE_LABEL")} text={t(getdate(TradeDetails?.CommencementDate))} />
        {TradeDetails?.units?.map((unit, index) => <div key={index}>
          <CardSubHeader>
            {t("TL_UNIT_HEADER")}-{index + 1}
          </CardSubHeader>
          <Row className="border-none" label={t("TL_NEW_TRADE_DETAILS_TRADE_CAT_LABEL")} text={t(unit?.tradecategory?.i18nKey)} />
          <Row className="border-none" label={t("TL_NEW_TRADE_DETAILS_TRADE_TYPE_LABEL")} text={t(unit?.tradetype?.i18nKey)} />
          <Row className="border-none" label={t("TL_NEW_TRADE_DETAILS_TRADE_SUBTYPE_LABEL")} text={t(unit?.tradesubtype?.i18nKey)} />
          <Row className="border-none" label={t("TL_UNIT_OF_MEASURE_LABEL")} text={`${unit?.unit ? t(unit?.unit) : t("CS_NA")}`} />
          <Row className="border-none" label={t("TL_NEW_TRADE_DETAILS_UOM_VALUE_LABEL")} text={`${unit?.uom ? t(unit?.uom) : t("CS_NA")}`} />
        </div>)}
        {TradeDetails?.accessories && TradeDetails?.isAccessories?.i18nKey?.includes("YES") && TradeDetails?.accessories?.map((acc, index) => <div key={index}>
          <CardSubHeader>
            {t("TL_ACCESSORY_LABEL")}-{index + 1}
          </CardSubHeader>
          <Row className="border-none" label={t("TL_TRADE_ACC_HEADER")} text={t(acc?.accessory?.i18nKey)} />
          <Row className="border-none" label={t("TL_NEW_TRADE_ACCESSORY_COUNT")} text={t(acc?.accessorycount)} />
          <Row className="border-none" label={t("TL_ACC_UOM_LABEL")} text={`${acc?.unit ? t(acc?.unit) : t("CS_NA")}`} />
          <Row className="border-none" label={t("TL_ACC_UOM_VALUE_LABEL")} text={`${acc?.unit ? t(acc?.uom) : t("CS_NA")}`} />
        </div>)}
      </StatusTable>
    </Card>
    {!(TradeDetails?.StructureType?.code === "MOVABLE") && <Card>
      <StatusTable>
        <CardHeader styles={{
          fontSize: "28px"
        }}>{t("TL_NEW_TRADE_DETAILS_HEADER_TRADE_LOC_DETAILS")}</CardHeader>
        {cpt && cpt.details && cpt.details.propertyId ? <React.Fragment>
          <LinkButton label={<EditIcon className="tl-check-page-btn-relative-mt-neg" />} onClick={() => goEdit("know-your-property")} className="tl-check-page-btn" />
          <Row className="border-none" textStyle={{
            marginRight: "-10px"
          }} label={t("TL_PROPERTY_ID")} text={`${cpt.details.propertyId?.trim()}`} />
          <Row className="border-none" label={t("TL_CHECK_ADDRESS")} text={`${cpt.details?.address?.doorNo?.trim() ? `${cpt.details?.address?.doorNo?.trim()}, ` : ""} ${cpt.details?.address?.street?.trim() ? `${cpt.details?.address?.street?.trim()}, ` : ""} ${cpt.details?.address?.buildingName?.trim() ? `${cpt.details?.address?.buildingName?.trim()}, ` : ""}
              ${t(cpt.details?.address?.locality?.name)}, ${t(cpt.details?.address?.city)} ${cpt.details?.address?.pincode?.trim() ? `,${cpt.details?.address?.pincode?.trim()}` : ""}`} />
        </React.Fragment> : <React.Fragment>
          <LinkButton label={<EditIcon className="tl-check-page-btn-relative-mt-neg" />} onClick={() => goEdit("map")} className="tl-check-page-btn" />
          <Row className="border-none" label={t("TL_CHECK_ADDRESS")} text={`${address?.doorNo?.trim() ? `${address?.doorNo?.trim()}, ` : ""} ${address?.street?.trim() ? `${address?.street?.trim()}, ` : ""}${t(address?.locality?.i18nkey)}, ${t(address?.city?.code || address?.city)} ${address?.pincode?.trim() ? `,${address?.pincode?.trim()}` : ""}`} />
        </React.Fragment>}
      </StatusTable>
    </Card>}
    <Card>
      <StatusTable>
        <CardHeader styles={{
          fontSize: "28px"
        }}>{t("TL_NEW_OWNER_DETAILS_HEADER")}</CardHeader>
        <LinkButton label={<EditIcon className="tl-check-page-btn-relative-mt-neg" />} onClick={() => goEdit("owner-details")} className="tl-check-page-btn" />
        {owners?.owners && owners?.owners?.map((owner, index) => <div key={index}>
          <CardSubHeader>
            {t("TL_PAYMENT_PAID_BY_PLACEHOLDER")}-{index + 1}
          </CardSubHeader>
          <Row className="border-none" label={t("TL_COMMON_TABLE_COL_OWN_NAME")} text={t(owner?.name)} />
          <Row className="border-none" label={t("TL_NEW_OWNER_DETAILS_GENDER_LABEL")} text={t(owner?.gender?.i18nKey) || t("CS_NA")} />
          <Row className="border-none" label={t("TL_MOBILE_NUMBER_LABEL")} text={t(owner?.mobilenumber)} />
          <Row className="border-none" label={t("TL_GUARDIAN_S_NAME_LABEL")} text={t(owner?.fatherOrHusbandName) || t("CS_NA")} />
          <Row className="border-none" label={t("TL_RELATIONSHIP_WITH_GUARDIAN_LABEL")} text={t(owner?.relationship?.i18nKey) || t("CS_NA")} />
          <Row className="border-none" label={t("TL_EMAIL_ID_LABEL")} text={t(owner?.emailId) || t("CS_NA")} />
          <Row className="border-none" label={t("TL_COMMON_TABLE_COL_OWN_CATEGORY_SHIP")} text={t(value?.ownershipCategory?.i18nKey || value?.ownershipCategory?.code)} />
          <Row className="border-none" label={t("TL_CORRESPONDENCE_ADDRESS")} labelStyle={{
            marginRight: "2px"
          }} text={t(owners?.permanentAddress) || t("CS_NA")} />
        </div>)}
      </StatusTable>
    </Card>
    <Card>
      <StatusTable>
        <CardHeader styles={{
          fontSize: "28px"
        }}>{t("TL_COMMON_DOCS")}</CardHeader>
        <LinkButton label={<EditIcon className="tl-check-page-btn-relative-mt-neg" />} onClick={() => goEdit("proof-of-identity")} className="tl-check-page-btn" />
        <div>
          {owners?.documents?.["OwnerPhotoProof"] || owners?.documents?.["ProofOfIdentity"] || owners?.documents?.["ProofOfOwnership"] ? <TLDocument value={value}></TLDocument> : <StatusTable>
            <Row className="border-none" text={t("TL_NO_DOCUMENTS_MSG")} />
          </StatusTable>}
        </div>
      </StatusTable>
      {toast && <Toast error={toast.key === "error"} label={t(toast.message)} onClose={() => setToast(null)} isDleteBtn={true} className="tl-check-page-link" />}
      <CheckBox
        label={t("TL_DECLARATION_CONFIRM")}
        onChange={setdeclarationhandler}
        checked={agree}
        styles={{ height: "auto", marginTop: "16px" }}
      />
      <SubmitBar
        label={t("CS_COMMON_SUBMIT")}
        onSubmit={CheckForBillingSlab}
        disabled={!agree}
      />
    </Card>
  </React.Fragment>;
};
export default CheckPage;
