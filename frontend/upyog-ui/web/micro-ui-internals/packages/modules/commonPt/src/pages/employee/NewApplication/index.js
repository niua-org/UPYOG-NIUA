import React, { useEffect } from "react";
import { Loader } from "@nudmcdgnpm/digit-ui-react-components";
import { useTranslation } from "react-i18next";
import { useQueryClient } from "@tanstack/react-query";
import { Route, useLocation, Routes } from "react-router-dom";

import CreatePropertyForm from '../../pageComponents/createForm';
import PTAcknowledgement from '../../pageComponents/PTAcknowledgement';
import { convertToPropertyLightWeight, convertToUpdatePropertyLightWeight } from "../../utils";

const SaveProperty = ({ onSuccess, redirectUrl, userType }) => {
  const location = useLocation();
  const navigate = Digit.Hooks.useCustomNavigate();
  const stateId = Digit.ULBService.getStateId();
  const tenantId = Digit.ULBService.getCurrentTenantId();

  let data = location?.state?.data;

  let createNUpdate = false;
  let { data: mdmsConfig, isLoading: mdmsLoading } = Digit.Hooks.pt.useMDMS(stateId, "PropertyTax", "PTWorkflow");
  (mdmsConfig?.PropertyTax?.PTWorkfow || []).forEach((data) => {
    if (data.enable) {
      if (data.businessService.includes("WNS")) {
        createNUpdate = true;
      }
    }
  });

  const mutation = Digit.Hooks.pt.usePropertyAPI(
    data?.locationDet?.city?.code || data?.locationDet?.cityCode?.code || tenantId,
    true // create
  );

  const mutationForUpdate = Digit.Hooks.pt.usePropertyAPI(
    data?.locationDet?.city?.code || data?.locationDet?.cityCode?.code || tenantId,
    false // update
  );

  useEffect(() => {
    if (data) {
      try {
        let tenant = userType === "employee" ? tenantId : (data?.locationDet?.cityCode?.code || data?.locationDet?.city?.code);
        data.tenantId = tenant;

        let formdata = convertToPropertyLightWeight(data);
        formdata.Property.tenantId = formdata?.Property?.tenantId || tenant;

        mutation.mutate(formdata, {
          onSuccess: (createData) => {
            if (onSuccess) onSuccess();
            if (!createNUpdate) {
              navigate("../acknowledgement", {
                replace: true,
                state: {
                  isSuccess: true,
                  data: createData,
                  createNUpdate: false,
                }
              });
            } else {
              let updateFormdata = convertToUpdatePropertyLightWeight(data);
              updateFormdata.Property.tenantId = updateFormdata?.Property?.tenantId || tenant;
              mutationForUpdate.mutate(updateFormdata, {
                onSuccess: (updateData) => {
                  navigate("../acknowledgement", {
                    replace: true,
                    state: {
                      isSuccess: true,
                      data: updateData,
                      createNUpdate: true,
                    }
                  });
                },
                onError: (error) => {
                  navigate("../acknowledgement", {
                    replace: true,
                    state: {
                      isSuccess: false,
                      error,
                      createNUpdate: true,
                    }
                  });
                }
              });
            }
          },
          onError: (error) => {
            navigate("../acknowledgement", {
              replace: true,
              state: {
                isSuccess: false,
                error,
                createNUpdate: false,
              }
            });
          }
        });
      } catch (err) {
        navigate("../acknowledgement", {
          replace: true,
          state: {
            isSuccess: false,
            error: err.message || err,
            createNUpdate,
          }
        });
      }
    }
  }, [data]);

  return <Loader />;
};

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
    queryClient.invalidateQueries({ queryKey: ["PT_CREATE_PROPERTY"] });
  };

  if (isLoading) {
    return <Loader />;
  }

  return (
    <Routes>
      <Route path="" element={<CreatePropertyForm onSubmit={createProperty} value={params} redirectUrl={redirectUrl} userType={"employee"} />} />
      <Route path={`save-property`} element={<SaveProperty onSuccess={onSuccess} redirectUrl={redirectUrl} userType={"employee"} />} />
      <Route path={`acknowledgement`} element={<PTAcknowledgement onSuccess={onSuccess} redirectUrl={redirectUrl} userType={"employee"} />} />
    </Routes>
  );
};

export default NewApplication;