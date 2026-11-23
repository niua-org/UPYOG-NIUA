import "../../../../css/ndc.css";
import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useTranslation } from "react-i18next";
import { useLocation } from "react-router-dom";
import { Stepper, CardHeader, Toast } from "@nudmcdgnpm/digit-ui-react-components";

import { config } from "../../../config/citizen/CitizenNDCApplicationConfig";
import { setNDCStep, updateNDCForm, resetNDCForm } from "../../../redux/actions/NDCFormActions";

const createEmployeeConfig = [
  {
    head: "Application Details",
    stepLabel: "Application Details",
    stepNumber: 1,
    isStepEnabled: true,
    type: "component",
    component: "NewNDCStepFormOneCitizen",
    key: "NDCDetails",
    withoutLabel: true,
    texts: {
      submitBarLabel: "Next",
    },
  },

  {
    head: "NDC_DOCUMENTS_REQUIRED",
    stepLabel: "Document Info",
    stepNumber: 2,
    isStepEnabled: true,
    type: "component",
    component: "NewNDCStepFormTwoCitizen",
    key: "DocummentDetails",
    withoutLabel: true,
    texts: {
      submitBarLabel: "Next",
    },
  },

  {
    head: "Summary",
    stepLabel: "Summary",
    stepNumber: 3,
    isStepEnabled: true,
    type: "component",
    component: "NDCNewFormSummaryStepThreeCitizen",
    key: "PTSummary",
    withoutLabel: true,
    texts: {
      submitBarLabel: "Submit",
    },
  },
];

const updatedCreateEmployeeconfig = createEmployeeConfig.map((item) => {
  return {
    ...item,
    currStepConfig: config.filter((newConfigItem) => newConfigItem.stepNumber === item.stepNumber),
  };
});

const STEP_ROUTES = {
  1: "application-details",
  2: "documents",
  3: "summary",
};

const ROUTE_TO_STEP = {
  "application-details": 1,
  "documents": 2,
  "summary": 3,
};

const getRouteInfo = (pathname) => {
  const match = pathname.match(/^(.*?)\/(application-details|documents|summary)\/?$/);
  if (match) {
    return {
      basePath: match[1],
      stepRoute: match[2],
      stepNumber: ROUTE_TO_STEP[match[2]] || 1,
    };
  }
  return {
    basePath: pathname.replace(/\/+$/, ""),
    stepRoute: null,
    stepNumber: 1,
  };
};

const getApplicationNumber = (pathname) => {
  const match = pathname.match(/(NDC-[A-Za-z0-9\-_]+)/);
  if (match) return match[1];

  const cleaned = pathname.replace(/\/(application-details|documents|summary)\/?$/, "");
  const segments = cleaned.split("/").filter(Boolean);
  const last = segments[segments.length - 1];
  if (last && last !== "new-application" && last !== "create") {
    return last;
  }
  return null;
};

export const NewNDCStepForm = () => {
  const navigate = Digit.Hooks.useCustomNavigate();
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const location = useLocation();

  const [showToast, setShowToast] = useState(null);

  const formState = useSelector((state) => state.ndc.NDCForm);
  const formData = formState.formData;

  const { basePath, stepRoute, stepNumber } = getRouteInfo(location.pathname);
  const applicationId = getApplicationNumber(location.pathname);

  const tenantId = Digit.ULBService.getCitizenCurrentTenant(true) || Digit.ULBService.getCurrentTenantId();

  const { isLoading, data: applicationDetails } = Digit.Hooks.ndc.useSearchEmployeeApplication(
    {
      applicationNo: applicationId,
    },
    tenantId,
    {
      enabled: !!applicationId,
    }
  );

  useEffect(() => {
    if (applicationDetails?.Applications?.length) {
      dispatch(updateNDCForm("responseData", applicationDetails.Applications));
    }
  }, [applicationDetails, dispatch]);

  // If user accesses base path without step sub-route, redirect to application-details
  useEffect(() => {
    if (!stepRoute) {
      navigate(`${basePath}/application-details`, { replace: true });
    }
  }, [stepRoute, basePath, navigate]);

  // Keep Redux step in sync with URL route
  useEffect(() => {
    if (stepRoute && ROUTE_TO_STEP[stepRoute] && formState.step !== ROUTE_TO_STEP[stepRoute]) {
      dispatch(setNDCStep(ROUTE_TO_STEP[stepRoute]));
    }
  }, [stepRoute, formState.step, dispatch]);

  // Guard: if user jumps to step 2 or 3 directly without form data or existing application, redirect to step 1
  useEffect(() => {
    if (stepNumber > 1 && !applicationId) {
      const hasData =
        formData?.NDCDetails?.PropertyDetails ||
        formData?.NDCDetails?.cpt ||
        formData?.responseData?.[0] ||
        formData?.apiData?.Applications?.[0];
      if (!hasData) {
        navigate(`${basePath}/application-details`, { replace: true });
      }
    }
  }, [stepNumber, applicationId, formData, basePath, navigate]);

  // Reset form only when unmounting (leaving NDC form flow)
  useEffect(() => {
    return () => {
      dispatch(resetNDCForm());
    };
  }, [dispatch]);

  const handleStepChange = (targetStep) => {
    const nextRoute = STEP_ROUTES[targetStep] || "application-details";
    dispatch(setNDCStep(targetStep));
    navigate(`${basePath}/${nextRoute}`);
  };

  const handleSubmit = () => {};

  return (
    <div className="employeeCard ndc-hide-vertical-stepper">
      <CardHeader className="ndc-step-form" divider={true}>
        {t("ndc_header_application")}
      </CardHeader>

      <Stepper
        stepsList={updatedCreateEmployeeconfig}
        onSubmit={handleSubmit}
        step={stepNumber}
        setStep={handleStepChange}
      />

      {showToast && (
        <Toast
          error={showToast.key}
          label={t(showToast.label)}
          onClose={() => {
            setShowToast(null);
          }}
          isDleteBtn={"true"}
        />
      )}
    </div>
  );
};
