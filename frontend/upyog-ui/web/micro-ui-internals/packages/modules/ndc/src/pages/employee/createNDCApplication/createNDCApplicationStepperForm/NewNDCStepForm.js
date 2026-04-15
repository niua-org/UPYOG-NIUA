import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useTranslation } from "react-i18next";
import { useParams, useHistory } from "react-router-dom";
//
import Stepper from "../../../../../../../react-components/src/customComponents/Stepper";
import { config } from "../../../../config/citizen/CitizenNDCApplicationConfig";
import { setNDCStep, updateNDCForm, resetNDCForm } from "../../../../redux/actions/NDCFormActions";
import { CardHeader, Toast } from "@upyog/digit-ui-react-components";

const createEmployeeConfig = [
  {
    head: "Application Details",
    stepLabel: "Application Details",
    stepNumber: 1,
    isStepEnabled: true,
    type: "component",
    component: "NewNDCStepFormOneEmployee",
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
    component: "NewNDCStepFormTwoEmployee",
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
    component: "NDCNewFormSummaryStepThreeEmployee",
    key: "PTSummary",
    withoutLabel: true,
    // texts: {
    //   submitBarLabel: "Submit",
    // },
  },
];

const updatedCreateEmployeeconfig = createEmployeeConfig.map((item) => {
  return { ...item, currStepConfig: config.filter((newConfigItem) => newConfigItem.stepNumber === item.stepNumber) };
});


export const NewNDCStepForm = () => {
  const history = useHistory();
  // const { id } = useParams();
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const [showToast, setShowToast] = useState(null);
  const formState = useSelector((state) => state.ndc.NDCForm);
  const formData = formState.formData;
  const step = formState.step;
  const tenantId = Digit.ULBService.getCurrentTenantId();

  const id = window.location.pathname.split("/").pop();

  const { isLoading, data: applicationDetails } = Digit.Hooks.ndc.useSearchEmployeeApplication({ applicationNo: id }, tenantId);

  useEffect(() => {
    if (applicationDetails?.Applications.length) {
      dispatch(updateNDCForm("responseData", applicationDetails?.Applications));
    }
  }, [applicationDetails]);

  const setStep = (updatedStepNumber) => {
    dispatch(setNDCStep(updatedStepNumber));
  };
console.log("formData in step form", formData);
  const handleSubmit = () => {};

  useEffect(() => {
    const unlisten = history.listen(() => {
      // route changed
      dispatch(resetNDCForm());
      // dispatch(updateNDCForm("reset", {}));
      // dispatch(setNDCStep(1));
    });

    return () => unlisten();
  }, [history, dispatch]);

  return (
    <div className="card">
      <CardHeader className="ndc-emp-step-form"  divider={true}>
        {t("ndc_header_application")}
      </CardHeader>
      <Stepper stepsList={updatedCreateEmployeeconfig} onSubmit={handleSubmit} step={step} setStep={setStep} />
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
