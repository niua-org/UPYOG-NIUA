import React, { useState } from "react";
import {
  Card,
  CardHeader,
  CardSubHeader,
  CheckBox,
  SubmitBar,
  EditIcon,
  LinkButton,
  DynamicObjectRenderer,
} from "@nudmcdgnpm/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import styles from "../../../styles/ESTRegCheckPage.module.scss";


// ─── ActionButton ──────────────────────────────────────────────────────────────
const ActionButton = ({ jumpTo, flatAsset }) => {
  const navigate = Digit.Hooks.useCustomNavigate();
  return (
    <LinkButton
      label={
        <EditIcon style={{ float: "right", position: "relative", bottom: "32px" }} />
      }
      className="check-page-link-button"
      onClick={() => navigate(jumpTo, { state: { editData: flatAsset } })}
    />
  );
};

// ─── buildDynamicAssetPayload ──────────────────────────────────────────────────
// Walks routeConfig.form and builds the API asset object from config field definitions.
// - type "text"     → copies value, casts to Number for numeric fields
// - type "dropdown" → copies code and label (name + nameName)
// - apiFieldName    → renames the key in the payload (e.g. buildingFloor → floor)
const NUMERIC_FIELDS = new Set([
  "buildingFloor",
  "totalFloorArea",
  "dimensionLength",
  "dimensionWidth",
  "rate",
]);

const buildDynamicAssetPayload = (routeConfig, flatAsset, tenantId) => {
  const asset = {};

  const processField = (fieldConfig) => {
    // Group — recurse into children
    if (fieldConfig.type === "group") {
      (fieldConfig.children || []).forEach(processField);
      return;
    }

    const { field, apiFieldName } = fieldConfig;
    if (!field) return;

    const { name, type } = field;
    const payloadKey = apiFieldName || name;

    if (type === "dropdown") {
      asset[payloadKey]          = flatAsset[name]           || "";
      asset[`${payloadKey}Name`] = flatAsset[`${name}Name`]  || "";
    } else {
      const raw = flatAsset[name] ?? "";
      asset[payloadKey] = NUMERIC_FIELDS.has(name) ? Number(raw) || 0 : raw;
    }
  };

  (routeConfig?.form || []).forEach(processField);

  // ── Static required fields not driven by form config ──
  asset.tenantId             = tenantId;
  asset.assetStatus          = "1";
  asset.assetClassification  = "IMMOVABLE";
  asset.assetParentCategory  = "LAND";
  asset.assetCategory        = flatAsset.assetType || "";
  asset.assetSubCategory     = null;
  asset.assetAllotmentType   = "DONATED";
  asset.assetAllotmentStatus = "INITIATED";
  asset.assetName            = flatAsset.buildingName || "";
  asset.description          = "";
  asset.department           = "DEPT_2";
  asset.estateNo             = "";

  // ── Locality split ──
  // Config field "serviceType" holds locality code; "serviceTypeName" holds label.
  // API wants them as "locality" (label) and "localityCode" (code).
  asset.locality     = flatAsset.serviceTypeName || flatAsset.serviceType || "";
  asset.localityCode = flatAsset.serviceType     || "";
  delete asset.serviceType;
  delete asset.serviceTypeName;

  // ── floor rename ──
  // Handled by apiFieldName: "floor" in config — this block is a safety fallback
  // in case the config doesn't have apiFieldName set yet.
  if (asset.buildingFloor !== undefined) {
    asset.floor = Number(asset.buildingFloor) || 0;
    delete asset.buildingFloor;
  }

  return asset;
};

// ─── ESTRegCheckPage ───────────────────────────────────────────────────────────
const ESTRegCheckPage = ({ onSubmit, onError, value = {}, config }) => {
  const { t } = useTranslation();
  const [agree, setAgree]               = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const tenantId  = Digit.ULBService.getCurrentTenantId();
  const userInfo  = Digit?.UserService?.getUser()?.info  || {};
  const authToken = Digit?.UserService?.getUser()?.access_token || "";

  const mutation = Digit.Hooks.estate.useESTCreateAPI(tenantId);

  // value = params from ESTRegCreate (session storage)
  // shape: { newRegistration: { Assets: [flatAsset] } }
  const assetsArray =
    value?.newRegistration?.Assets ||
    value?.Assets?.Assets ||
    (Array.isArray(value?.Assets) ? value.Assets : null) ||
    [];

  const flatAsset = assetsArray[0] || {};

  // routeConfig = the "newRegistration" step config
  const routeConfig =
    config?.body?.find((step) => step.key === "newRegistration") ||
    config ||
    {};

  console.log("=== flatAsset from session ===", JSON.stringify(flatAsset, null, 2));

  // ─── Build payload ───────────────────────────────────────────────────────────
  const buildAPIPayload = () => {
    const assetPayload = buildDynamicAssetPayload(routeConfig, flatAsset, tenantId);
    console.log("=== Dynamic asset payload ===", JSON.stringify(assetPayload, null, 2));

    return {
      RequestInfo: {
        apiId:   "Rainmaker",
        authToken,
        userInfo,
        msgId:   `${Date.now()}|en_IN`,
        plainAccessRequest: {},
      },
      Assets: [assetPayload],
    };
  };

  // ─── Final submit ─────────────────────────────────────────────────────────────
  // Navigation is handled entirely by ESTRegCreate via onSubmit/onError props.
  // ESTRegCreate uses match.pathnameBase for absolute paths so we never land
  // on /check/acknowledgement by mistake.
  const handleFinalSubmit = () => {
    if (!agree || isSubmitting) return;

    const payload_updated = buildAPIPayload();
    console.log("=== EST Create API payload ===", JSON.stringify(payload_updated, null, 2));

    setIsSubmitting(true);

    mutation.mutate(payload_updated, {
      onSuccess: (response) => {
       // console.log("EST Create success:", response);
        setIsSubmitting(false);
        // Hand off to ESTRegCreate.estcreate — it clears session and navigates
        // to the absolute acknowledgement path
        onSubmit && onSubmit(response);
      },
      onError: (error) => {
        console.error("EST Create error status:", error?.response?.status);
        console.error("EST Create error response:", JSON.stringify(error?.response?.data, null, 2));
        setIsSubmitting(false);
        // Hand off to ESTRegCreate.estcreateError
        onError && onError(error);
      },
    });
  };

  return (
  <Card>
    <CardHeader>{t("EST_REGISTRATION_SUMMARY")}</CardHeader>
    <CardSubHeader>{t("EST_ASSET_DETAILS")}</CardSubHeader>

    <div className={styles["estRegCheckPage__action-row"]}>
      <ActionButton
        jumpTo="/upyog-ui/employee/est/create-asset/newRegistration"
        flatAsset={flatAsset}
      />
    </div>

    <DynamicObjectRenderer data={assetsArray} />

    <div className={styles["estRegCheckPage__declaration"]}>
      <CheckBox
        label={t("EST_FINAL_DECLARATION_MESSAGE")}
        onChange={() => setAgree((prev) => !prev)}
      />
    </div>

    <div className={styles["estRegCheckPage__submit-row"]}>
      <SubmitBar
        label={t("EST_COMMON_SUBMIT")}
        onSubmit={handleFinalSubmit}
        disabled={!agree || isSubmitting}
      />
    </div>
  </Card>
);
};

export default ESTRegCheckPage;
