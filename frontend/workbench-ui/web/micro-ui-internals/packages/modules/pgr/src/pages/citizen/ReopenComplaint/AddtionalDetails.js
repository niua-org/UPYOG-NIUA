import React, { useCallback, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import { useParams, Navigate } from "react-router-dom";

import { BackButton, Card, CardHeader, CardText, TextArea, SubmitBar } from "@upyog/workbench-ui-react-components";

import { updateComplaints } from "../../../redux/actions/index";
import { LOCALIZATION_KEY } from "../../../constants/Localization";

const AddtionalDetails = (props) => {
  // const [details, setDetails] = useState(null);
  const navigate = Digit.Hooks.useCustomNavigate();
  let { id } = useParams();
  const dispatch = useDispatch();
  const appState = useSelector((state) => state)["common"];
  let { t } = useTranslation();
  
  const {complaintDetails} = props

  useEffect(() => {
    if (appState.complaints) {
      const { response } = appState.complaints;
      if (response && response.responseInfo.status === "successful") {
        //  Use navigate with relative path or calculate parent path
        navigate(`response/:${id}`); // or use absolute path if needed
      }
    }
  }, [appState.complaints, navigate, id]); // Fixed dependencies

  const updateComplaint = useCallback(
    async (complaintDetails) => {
      await dispatch(updateComplaints(complaintDetails));
      // Use navigate instead of history.push
      navigate(`response/${id}`); // or use absolute path if needed
    },
    [dispatch]
  );

  const getUpdatedWorkflow = (reopenDetails, type) => {
    switch (type) {
      case "REOPEN":
        return {
          action: "REOPEN",
          comments: reopenDetails.addtionalDetail,
          assignes: [],
          verificationDocuments: reopenDetails.verificationDocuments,
        };
      default:
        return "";
    }
  };

  function reopenComplaint() {
    let reopenDetails = Digit.SessionStorage.get(`reopen.${id}`);
    if (complaintDetails) {
      complaintDetails.workflow = getUpdatedWorkflow(
        reopenDetails,
        // complaintDetails,
        "REOPEN"
      );
      complaintDetails.service.additionalDetail = {
        REOPEN_REASON: reopenDetails.reason,
      };
      updateComplaint({ service: complaintDetails.service, workflow: complaintDetails.workflow });
    }
    // Changed Redirect to Navigate (though this logic seems wrong - see note below)
    return (
      <Navigate
        to={`${props.parentRoute}/response`}
        state={{ complaintDetails }}
        replace
      />

    );
  }

  function textInput(e) {
    // setDetails(e.target.value);
    let reopenDetails = Digit.SessionStorage.get(`reopen.${id}`);
    Digit.SessionStorage.set(`reopen.${id}`, {
      ...reopenDetails,
      addtionalDetail: e.target.value,
    });
  }

  return (
    <React.Fragment>
      <Card>
        <CardHeader>{t(`${LOCALIZATION_KEY.CS_ADDCOMPLAINT}_PROVIDE_ADDITIONAL_DETAILS`)}</CardHeader>
        <CardText>{t(`${LOCALIZATION_KEY.CS_ADDCOMPLAINT}_ADDITIONAL_DETAILS_TEXT`)}</CardText>
        <TextArea name={"AdditionalDetails"} onChange={textInput}></TextArea>
        <div onClick={reopenComplaint}>
          <SubmitBar label={t(`${LOCALIZATION_KEY.CS_HEADER}_REOPEN_COMPLAINT`)} />
        </div>
      </Card>
    </React.Fragment>
  );
};

export default AddtionalDetails;
