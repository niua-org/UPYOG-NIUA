// Utility function to check if a value is not null, undefined, or an empty string
export const checkForNotNull = (value = "") => {
  return value && value != null && value != undefined && value != "" ? true : false;
};

// Utility function to replace all dots in a string with underscores
export const convertDotValues = (value = "") => {
  return (
    (checkForNotNull(value) && ((value.replaceAll && value.replaceAll(".", "_")) || (value.replace && stringReplaceAll(value, ".", "_")))) || "NA"
  );
};

// Utility function to truncate a filename to a fixed size and append ellipsis
export const getFixedFilename = (filename = "", size = 5) => {
  if (filename.length <= size) {
    return filename;
  }
  return `${filename.substr(0, size)}...`;
};

// Utility function to determine if the back button should be hidden based on the current URL
export const shouldHideBackButton = (config = []) => {
  return config.filter((key) => window.location.href.includes(key.screenPath)).length > 0 || window.location.href.includes("acknowledgement")
    ? true
    : false;
};

export const estAccess = () => {
  return true;
};

const toYMD = (d) => {
  if (!d) return null;
  const date = new Date(d);
  if (isNaN(date.getTime())) return null;
  return date.toISOString().split("T")[0]; // YYYY-MM-DD
};

export const estPayloadData = (data) => {
  const user = Digit.UserService.getUser().info;
  const tenantId = Digit.ULBService.getCurrentTenantId();

  // ✅ CORRECT asset source (THIS IS THE FIX)
  const assetData =
    data?.AssignAssetsData?.asset ||
    data?.AssignAssetsData?.Asset ||
    data?.AssignAssetsData?.assetData ||
    data?.assetData ||
    data?.Assetdata?.Assetdata ||
    data?.Assetdata ||
    {};

  console.log("FINAL assetData used for payload:", assetData);

  const formdata = {
    Assets: [
      {
        assetStatus: "1",
        assetType: assetData?.assetType || "",
        buildingName: assetData?.buildingName || "",
        buildingNo: assetData?.buildingNo || "",  
        dimensionLength: Number(assetData?.dimensionLength) || 0,
        dimensionWidth: Number(assetData?.dimensionWidth) || 0,
        floor: Number(assetData?.buildingFloor) || 0,
        locality:assetData?.localityName || assetData?.locality || assetData?.serviceType?.i18nKey || "",
        localityCode: assetData?.localityCode || (typeof assetData?.serviceType === "string" ? assetData?.serviceType : assetData?.serviceType?.code) || "",
        rate: Number(assetData?.rate) || 0,
        tenantId,
        totalFloorArea: Number(assetData?.totalFloorArea) || 0,
        assetId: crypto.randomUUID(),
        estateNo: assetData?.estateNo || "",
        assetAllotmentType: "DONATED",
        assetAllotmentStatus: "INITIATED",
        refAssetNo: assetData?.refAssetNo || "",
        auditDetails: {
          createdBy: user?.uuid || "",
          lastModifiedBy: user?.uuid || "",
          createdTime: Date.now(),
          lastModifiedTime: Date.now(),
        },
        additionalDetails: assetData?.additionalDetails || {},
        assetName: assetData?.buildingName || assetData?.name || "",
        description: assetData?.description || "",
        assetClassification: "IMMOVABLE",
        assetParentCategory: "LAND",
        assetCategory: assetData?.assetType || "",
        assetSubCategory: assetData?.assetSubCategory || null,
        department: assetData?.department || "DEPT_2",
      },
    ],
  };

  return formdata;
};


export const createAllotmentData = (data) => {
  const user = Digit.UserService.getUser().info;
  const tenantId = Digit.ULBService.getCurrentTenantId();
  
  const allotmentData = data?.AssignAssetsData?.AllotmentData || {};

const extractDuration = (durationStr) => {
  if (!durationStr) return 0;
  const match = durationStr.toString().match(/\d+/);
  return match ? Number(match[0]) : 0;
};
  
const formdata = {
  Allotments: [
    {
      allotmentId: allotmentData?.allotmentId || "",
      assetNo: data?.assetData?.estateNo || allotmentData?.assetNo || "",
      tenantId: tenantId,
      userUuid: user?.uuid || "",

      alloteeName: allotmentData?.allotteeName || "",
      mobileNo: allotmentData?.phoneNumber || "",
      alternateMobileNo: allotmentData?.altPhoneNumber || "",
      emailId: allotmentData?.email || "",

      agreementStartDate: toYMD(allotmentData?.startDate),
      agreementEndDate: toYMD(allotmentData?.endDate),
      advancePaymentDate: toYMD(allotmentData?.advancePaymentDate),

     duration: extractDuration(allotmentData?.duration),

      rentRate: Number(allotmentData?.rate) || 0,
      monthlyRent: Number(allotmentData?.monthlyRent) || 0,
      advancePayment: Number(allotmentData?.advancePayment) || 0,

      eofficeFileNo: allotmentData?.eOfficeFileNo || "",

      auditDetails: {
        createdBy: user?.uuid || "",
        lastModifiedBy: user?.uuid || "",
        createdTime: Date.now(),
        lastModifiedTime: Date.now(),
      },
    },
  ],
};

  return formdata;
};


export const CompareTwoObjects = (ob1, ob2) => {
  let comp = 0;
  Object.keys(ob1).map((key) => {
    if (typeof ob1[key] == "object") {
      if (key == "institution") {
        if ((ob1[key].name || ob2[key].name) && ob1[key]?.name !== ob2[key]?.name) comp = 1;
        else if (ob1[key]?.type?.code !== ob2[key]?.type?.code) comp = 1;
      } else if (ob1[key]?.code !== ob2[key]?.code) comp = 1;
    } else {
      if ((ob1[key] || ob2[key]) && ob1[key] !== ob2[key]) comp = 1;
    }
  });
  if (comp == 1) return false;
  else return true;
};


export const checkForNA = (value = "") => {
  return checkForNotNull(value) ? value : "EST_NA";
};


export const pdfDownloadLink = (documents = {}, fileStoreId = "", format = "") => {
  let downloadLink = documents[fileStoreId] || "";
  let differentFormats = downloadLink?.split(",") || [];
  let fileURL = "";
  differentFormats.length > 0 &&
    differentFormats.map((link) => {
      if (!link.includes("large") && !link.includes("medium") && !link.includes("small")) {
        fileURL = link;
      }
    });
  return fileURL;
};


export const pdfDocumentName = (documentLink = "", index = 0) => {
  let documentName = decodeURIComponent(documentLink.split("?")[0].split("/").pop().slice(13)) || `Document - ${index + 1}`;
  return documentName;
};


export const convertEpochToDate = (dateEpoch, businessService) => {
  if (dateEpoch) {
    const dateFromApi = new Date(dateEpoch);
    let month = dateFromApi.getMonth() + 1;
    let day = dateFromApi.getDate();
    let year = dateFromApi.getFullYear();
    month = (month > 9 ? "" : "0") + month;
    day = (day > 9 ? "" : "0") + day;
    if (businessService == "ewst") return `${day}-${month}-${year}`;
    else return `${day}/${month}/${year}`;
  } else {
    return null;
  }
};

export const stringReplaceAll = (str = "", searcher = "", replaceWith = "") => {
  if (searcher == "") return str;
  while (str.includes(searcher)) {
    str = str.replace(searcher, replaceWith);
  }
  return str;
};


export const DownloadReceipt = async (consumerCode, tenantId, businessService, pdfKey = "consolidatedreceipt") => {
  tenantId = tenantId ? tenantId : Digit.ULBService.getCurrentTenantId();
  await Digit.Utils.downloadReceipt(consumerCode, businessService, "consolidatedreceipt", tenantId);
};


export const checkIsAnArray = (obj = []) => {
  return obj && Array.isArray(obj) ? true : false;
};

export const checkArrayLength = (obj = [], length = 0) => {
  return checkIsAnArray(obj) && obj.length > length ? true : false;
};

export const getWorkflow = (data = {}) => {
  return {
    businessService: `est`,
    moduleName: "estate-services",
  };
};


export const calculateDuration = (start, end) => {
  if (!start || !end) return 0;

  const startDate = new Date(start);
  const endDate = new Date(end);

  let years = endDate.getFullYear() - startDate.getFullYear();
  let months = endDate.getMonth() - startDate.getMonth();

  // adjust negative months
  if (months < 0) {
    years--;
    months += 12;
  }

  // total months
  const totalMonths = years * 12 + months;

  return totalMonths >= 0 ? totalMonths : 0;
};






// utils.js
import React from "react";
import { useTranslation } from "react-i18next";

/**
 * Large PDF SVG used in previews
 */
const LargePdfSvg = ({ size = 48 }) => (
  <svg
    width={size}
    height={size}
    viewBox="0 0 24 24"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    aria-hidden
    style={{ flexShrink: 0 }}
  >
    <rect width="24" height="24" rx="4" fill="#D32F2F" />
    <text x="3" y="16" fontFamily="Arial, sans-serif" fontSize="10" fontWeight="700" fill="#FFFFFF">PDF</text>
  </svg>
);

/**
 * Compact single document row: [LABEL] [ICON] [Click to View File]
 * - labelWidth: controls label column width (px)
 * - small gap between icon and link so they appear close
 */
function DocLink({ href, label, titleStyles = {}, pdfSize = 48, labelWidth = 220 }) {
  return (
    <a
      href={href}
      target="_blank"
      rel="noopener noreferrer"
      style={{
        display: "flex",
        alignItems: "center",
        gap: 12,
        textDecoration: "none",
        marginBottom: 12,
        width: "100%",
      }}
    >
      {/* label */}
      <div style={{ minWidth: labelWidth, fontWeight: 700, color: "#111", ...titleStyles }}>
        {label}
      </div>

      {/* icon */}
      <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
        <LargePdfSvg size={pdfSize} />
        {/* Click text immediately after icon */}
        <div style={{ color: "#0B5FFF", textDecoration: "none", fontWeight: 500 }}>
          Click to View File
        </div>
      </div>
    </a>
  );
}

/**
 * ESTDocumnetPreview
 * - documents: Array of groups with `values: [{ url, title, documentType }]`
 * Renders compact rows where icon and link are adjacent.
 */
export function ESTDocumnetPreview({ documents = [], titleStyles = {}, isHrLine = false, pdfSize = 48, labelWidth = 220 }) {
  const { t } = useTranslation();

  // flatten groups -> values
  const flattened = (documents || []).flatMap((group) =>
    (group.values || []).map((v) => ({
      url: v.url,
      title: t(v.title || v.documentType || "DOCUMENT"),
      documentType: v.documentType,
    }))
  );

  return (
    <div style={{ marginTop: 8 }}>
      {flattened.length > 0 ? (
        flattened.map((val, idx) => (
          <div key={`est-link-${idx}`}>
            <DocLink
              href={val.url}
              label={val.title}
              titleStyles={titleStyles}
              pdfSize={pdfSize}
              labelWidth={labelWidth}
            />
            {isHrLine && idx !== flattened.length - 1 ? (
              <hr style={{ border: 0, height: 1, backgroundColor: "#E5E5E5", margin: "8px 0 12px" }} />
            ) : null}
          </div>
        ))
      ) : (
        !(window.location.href.includes("citizen")) && <div style={{ color: "#666" }}>{t("EST_NO_DOCUMENTS_UPLOADED_LABEL")}</div>
      )}
    </div>
  );
}


export const formatEpochDate = (value) => {
  if (!value) return "N/A";

  let num = Number(value);

  // If epoch is in seconds (10 digits), convert to milliseconds
  if (String(num).length === 10) {
    num = num * 1000;
  }

  const date = new Date(num);

  if (isNaN(date.getTime())) return "N/A";

  const day = String(date.getDate()).padStart(2, "0");
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const year = date.getFullYear();

  return `${day}/${month}/${year}`;
};




// src/utils/index.js

/* -------------------- Utility helpers -------------------- */

export const pick = (...vals) =>
  vals.find((v) => v !== undefined && v !== null && v !== "") || "";

export const filterEmpty = (arr = []) =>
  arr.filter(
    (i) =>
      i &&
      i.value !== undefined &&
      i.value !== null &&
      i.value !== ""
  );

/* -------------------- Date formatter -------------------- */
/**
 * Returns: DD/MM/YYYY
 */
export const formatDate = (d) => {
  if (!d) return "";
  const date = d instanceof Date ? d : new Date(d);
  if (isNaN(date.getTime())) return "";
  return date.toLocaleDateString("en-GB");
};

/* -------------------- Duration calculator -------------------- */
/**
 * Returns:
 * 1 year 6 months
 */
export const calculatemonths = (start, end) => {
  if (!start || !end) return "";

  const startDate = new Date(start);
  const endDate = new Date(end);

  if (isNaN(startDate.getTime()) || isNaN(endDate.getTime())) return "";

  let years = endDate.getFullYear() - startDate.getFullYear();
  let months = endDate.getMonth() - startDate.getMonth();

  if (months < 0) {
    years--;
    months += 12;
  }

  let result = "";

  if (years > 0) {
    result += `${years} year${years > 1 ? "s" : ""}`;
  }

  if (months > 0) {
    result += `${result ? " " : ""}${months} month${months > 1 ? "s" : ""}`;
  }

  return result || "0 months";
};

/* -------------------- Duration formatter -------------------- */
/**
 * Final Output:
 * 18 months (1 year 6 months)
 */
export const formatDurationWithMonths = (allotment = {}) => {
  const totalMonths = Number(allotment.duration || 0);
  if (!totalMonths) return "";

  const readable = calculatemonths(
    allotment.agreementStartDate,
    allotment.agreementEndDate
  );

  return readable
    ? `${totalMonths} months (${readable})`
    : `${totalMonths} months`;
};



