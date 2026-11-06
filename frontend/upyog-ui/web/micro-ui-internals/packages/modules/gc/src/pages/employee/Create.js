import { Loader, Timeline } from "@nudmcdgnpm/digit-ui-react-components";
import React from "react";
import { useTranslation } from "react-i18next";
import { useQueryClient } from "@tanstack/react-query";
import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { commonConfig } from "../../config/Create/citizenconfig";
import { GCDataConvert } from "../../utils";
import CheckPage from "../citizen/Create/CheckPage";
import GCAcknowledgement from "../citizen/Create/GCAcknowledgement";

/**
 * CreateApplication Component (Employee)
 * 
 * Renders the same step-by-step wizard from the citizen side,
 * formatted and routed for the Employee dashboard. Reuses the citizen config
 * and check/acknowledgement pages but injects `userType="employee"` to adjust
 * field layout widths (50% instead of 86%).
 * 
 * Handles employee-specific payload transformations:
 * - Unwraps nested garbageAccount objects
 * - Sets tenantId on the account and property location
 * - Adds workflow action "APPLY" on submission
 * - Clears session on first visit to applicant-details to prevent stale data
 */
const CreateApplication = () => {
  const queryClient = useQueryClient();
  const { t } = useTranslation();
  const { pathname } = useLocation();
  const navigate = Digit.Hooks.useCustomNavigate();
  const stateId = Digit.ULBService.getStateId();

  let config = [];
  const tenantId = Digit.ULBService.getCurrentTenantId();
  const mutation = Digit.Hooks.gc.useGCCreateAPI(tenantId);
  const [params, setParams, clearParams] = Digit.Hooks.useSessionStorage("GC_Create_Emp", {});
  let { isLoading } = Digit.Hooks.pt.useMDMS(stateId, "PropertyTax", "CommonFieldsConfig");

  const goNext = (skipStep, index, isAddMultiple, key) => {
    let currentPath = pathname.split("/").pop(),
      lastchar = currentPath.charAt(currentPath.length - 1),
      isMultiple = false,
      nextPage;
    if (Number(parseInt(currentPath)) || currentPath == "0" || currentPath == "-1") {
      if (currentPath == "-1" || currentPath == "-2") {
        currentPath = pathname.slice(0, -3);
        currentPath = currentPath.split("/").pop();
        isMultiple = true;
      } else {
        currentPath = pathname.slice(0, -2);
        currentPath = currentPath.split("/").pop();
        isMultiple = true;
      }
    } else {
      isMultiple = false;
    }
    if (!isNaN(lastchar)) {
      isMultiple = true;
    }
    let { nextStep = {} } = config.find((routeObj) => routeObj.route === currentPath);

    let redirectWithHistory = (to, state) => navigate(to, state != null ? { state } : undefined);
    if (skipStep) {
      redirectWithHistory = (to, state) => navigate(to, state != null ? { replace: true, state } : { replace: true });
    }
    if (isAddMultiple) {
      nextStep = key;
    }
    if (nextStep === null) {
      return redirectWithHistory(`check`);
    }
    if (!isNaN(nextStep.split("/").pop())) {
      nextPage = `${nextStep}`;
    } else {
      nextPage = isMultiple && nextStep !== "map" ? `${nextStep}/${index}` : `${nextStep}`;
    }

    redirectWithHistory(nextPage);
  };

  if (params && Object.keys(params).length > 0 && window.location.href.includes("/garbage-propertyLocation") && sessionStorage.getItem("docReqScreenByBack") !== "true") {
    clearParams();
    queryClient.invalidateQueries("GC_Create_Emp");
  }

  const clearSession = () => {
    clearParams();
    queryClient.invalidateQueries("GC_Create_Emp");
  };

  const handleSubmit = async () => {
    try {
      const formdata = GCDataConvert(params);
      const targetApp = formdata?.garbageApplications?.[0];

      if (targetApp) {
        targetApp.tenantId = tenantId;
        if (targetApp.propertyLocation) {
          targetApp.propertyLocation.tenantId = tenantId;
          targetApp.propertyLocation.city = tenantId;
        }
        targetApp.workflow = { action: "APPLY" };
      }
      
      mutation.mutate(formdata, {
        onSuccess: (response) => {
          clearSession();
          navigate("acknowledgement", {
            state: {
              data: response,
              isSuccess: true,
            },
          });
        },
        onError: (error) => {
          console.error("API Error:", error);
          navigate("acknowledgement", {
            state: {
              data: null,
              isSuccess: false,
              error: error
            },
          });
        }
      });
    } catch (err) {
      console.error("Data Conversion Error:", err);
    }
  };

  function handleSelect(key, data, skipStep, index, isAddMultiple = false) {
    if (key === "owners") {
      let owners = params.owners || [];
      owners[index] = data;
      setParams({ ...params, ...{ [key]: [...owners] } });
    } else if (key === "units") {
      let units = params.units || [];
      units = data;
      setParams({ ...params, units });
    } else if (key === "gcpropertylocdetails") {
      let ownerUpdate = {};
      if (data?.ownerInfo) {
        const o = data.ownerInfo;
        const currentOwner = params.owner || {};
        const genderVal = o.gender || currentOwner.gender || "";
        const relVal = o.relationship || currentOwner.relationShipType || "";
        ownerUpdate = {
          applicantName: o.name || currentOwner.applicantName || "",
          mobileNumber: o.mobileNumber || currentOwner.mobileNumber || "",
          emailId: o.emailId || currentOwner.emailId || "",
          gender: genderVal ? (typeof genderVal === "object" ? genderVal : { code: genderVal, i18nKey: genderVal, name: genderVal }) : "",
          guardianName: o.fatherOrHusbandName || o.guardianName || currentOwner.guardianName || "",
          alternateNumber: o.altContactNumber || o.alternatemobilenumber || currentOwner.alternateNumber || "",
          relationShipType: relVal ? (typeof relVal === "object" ? relVal : { name: relVal, code: String(relVal).toUpperCase(), i18nKey: `COMMON_${String(relVal).toUpperCase()}` }) : ""
        };
      }
      const currentAddress = params.address || {};
      const addressUpdate = {
        pincode: data?.pincode || currentAddress.pincode || "",
        city: data?.city || currentAddress.city || "",
        locality: data?.locality || currentAddress.locality || "",
        streetName: data?.streetName || currentAddress.streetName || "",
        houseNo: data?.houseNo || currentAddress.houseNo || "",
        landmark: data?.landmark || currentAddress.landmark || "",
        houseName: data?.houseName || currentAddress.houseName || "",
        addressLine1: data?.addressline1 || data?.addressLine1 || currentAddress.addressLine1 || "",
        addressLine2: data?.addressline2 || data?.addressLine2 || data?.streetName || data?.landmark || currentAddress.addressLine2 || "N/A",
        addressType: currentAddress.addressType || { name: "Permanent", code: "PERMANENT", i18nKey: "COMMON_ADDRESS_TYPE_PERMANENT" }
      };
      setParams({
        ...params,
        gcpropertylocdetails: { ...params.gcpropertylocdetails, ...data },
        address: { ...(params.address || {}), ...addressUpdate },
        ...(Object.keys(ownerUpdate).length > 0 ? { owner: { ...(params.owner || {}), ...ownerUpdate } } : {})
      });
    } else {
      setParams({ ...params, ...{ [key]: { ...params[key], ...data } } });
    }
    goNext(skipStep, index, isAddMultiple, key);
  }

  if (isLoading) {
    return <Loader />;
  }

  let commonFields = commonConfig;
  commonFields.forEach((obj) => {
    config = config.concat(obj.body.filter((a) => !a.hideInCitizen));
  });

  config.indexRoute = "garbage-propertyLocation";

  return (
    <React.Fragment>
      <div className="gc-create-bottom-spacing">
        <Timeline config={config} />
      </div>
      <div className="gc-create-container-padding">
        <Routes>
          {config.filter(r => r.route !== "check").map((routeObj, index) => {
            const { component, texts, inputs, key, additionaFields, labels, disableFields } = routeObj;
            const Component = typeof component === "string" ? Digit.ComponentRegistryService.getComponent(component) : component;
            return (
              <Route
                  path={`${routeObj.route}`}
                  key={index}
                  element={
                    <Component
                      config={{ texts, inputs, key, additionaFields }}
                      onSelect={handleSelect}
                      t={t}
                      formData={params}
                      userType="employee"
                      applicantData={params?.owner}
                      addressData={params?.address}
                      labels={labels}
                      disableFields={disableFields}
                    />
                  }
              />
            );
          })}

          <Route path="check/*" element={<CheckPage onSubmit={handleSubmit} value={params} />} />
          <Route path="acknowledgement/*" element={<GCAcknowledgement />} />
          <Route path="/*" element={<Navigate to={`${config.indexRoute}`} replace />} />
        </Routes>
      </div>
    </React.Fragment>
  );
};

export default CreateApplication;