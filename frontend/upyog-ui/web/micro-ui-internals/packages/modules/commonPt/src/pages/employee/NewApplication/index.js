import React from "react";
import { Loader } from "@nudmcdgnpm/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import { useQueryClient } from "@tanstack/react-query";
import { Route, useLocation,  Routes } from "react-router-dom";

import CreatePropertyForm from '../../pageComponents/createForm';
import PTAcknowledgement from '../../pageComponents/PTAcknowledgement';

const NewApplication = ({ path }) => {
  let config = [];
  const { t } = useTranslation();
  
  const queryClient = useQueryClient();
  const match = Digit.Hooks.useModuleBasePath();
  const { pathname } = useLocation();
  const navigate = Digit.Hooks.useCustomNavigate();
  const stateId = Digit.ULBService.getStateId();
  const tenantId = Digit.ULBService.getCurrentTenantId();
  const [params, setParams, clearParams] = Digit.Hooks.useSessionStorage("PT_CREATE_PROPERTY", {});
  let { data: commonFields, isLoading } = Digit.Hooks.pt.useMDMS(stateId, "PropertyTax", "CommonFieldsConfig");
  
  const search = useLocation().search;
  const redirectUrl = new URLSearchParams(search).get('redirectToUrl');

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
      <Route path="" element={<CreatePropertyForm onSubmit={createProperty} value={params} redirectUrl={redirectUrl} userType={"employee"} />} />
      <Route path={`save-property`} element={<PTAcknowledgement data={params} onSuccess={onSuccess} redirectUrl={redirectUrl} userType={"employee"} />} />
    </Routes>
  );
};

export default NewApplication;