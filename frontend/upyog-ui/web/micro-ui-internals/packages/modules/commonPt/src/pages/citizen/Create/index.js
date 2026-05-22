import { Loader } from "@nudmcdgnpm/digit-ui-react-components";
import React ,{Fragment}from "react";
import { useTranslation } from "react-i18next";
import { useQueryClient } from "@tanstack/react-query";
import { Route, useLocation,  Routes, Navigate } from "react-router-dom";
// import { newConfig } from "../../../config/Create/config";
import CreatePropertyForm from '../../pageComponents/createForm';
import PTAcknowledgement from '../../pageComponents/PTAcknowledgement';

const CreateProperty = ({ parentRoute, onSelect }) => {
  const queryClient = useQueryClient();
  const match = Digit.Hooks.useModuleBasePath();
  const { t } = useTranslation();
  const { pathname } = useLocation();
  const navigate = Digit.Hooks.useCustomNavigate();
  const stateId = Digit.ULBService.getStateId();
  let config = [];
  const [params, setParams, clearParams] = Digit.Hooks.useSessionStorage("PT_CREATE_PROPERTY", {});
  let { data: commonFields, isLoading } = Digit.Hooks.pt.useMDMS(stateId, "PropertyTax", "CommonFieldsConfig");

  const search = useLocation().search;
  const redirectUrl = new URLSearchParams(search).get('redirectUrl');

  const createProperty = async () => {
    navigate(`acknowledgement`);
  };

  const onSuccess = () => {
    clearParams();
    queryClient.invalidateQueries("PT_CREATE_PROPERTY");
  };

  if (isLoading) {
    return <Loader />;
  }

  return (
    <Routes>
      <Route path={`${match.path}`} element={<CreatePropertyForm onSubmit={createProperty} value={params} userType={"citizen"} />} />
      <Route path={`save-property`} element={<PTAcknowledgement data={params} onSuccess={onSuccess} redirectUrl={redirectUrl} userType={"citizen"} />} />
    </Routes>
  );
};

export default CreateProperty;
