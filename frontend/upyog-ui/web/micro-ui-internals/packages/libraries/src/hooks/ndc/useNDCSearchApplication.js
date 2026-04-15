import { useQuery, useQueryClient } from "react-query";

//useNDCSearch is a custom hook that fetches NDC applications based on the provided parameters, tenant ID, and configuration. 
// It uses the Digit.NDCService.NDCsearch method to perform the search and returns the data in a structured format for use in the application.
const useNDCSearch = (params, tenantId, config) => {
  return async () => {
    const data = await Digit.NDCService.NDCsearch({ filters: params, tenantId, config });
    const Applications = data?.Applications;

    return { data };
  };
};

export const useNDCSearchApplication = (params, tenantId, config = {}, t) => {
  const client = useQueryClient();
  const result = useQuery(["NDC_APPLICATIONS_LIST", params], useNDCSearch(params, tenantId, config), {
    staleTime: Infinity,
    select: (data) => {
      console.log("data=====", data);
      const applications = data?.data?.Applications || [];
      const count = data?.data?.totalCount || 0;

      const mappedData = applications.map((owner) => ({
        BPA_APPLICATION_NUMBER_LABEL: owner?.applicationNo,
        TL_LOCALIZATION_OWNER_NAME: owner?.owners[0]?.name,
        TL_HOME_SEARCH_RESULTS_APP_STATUS_LABEL: owner?.applicationStatus,

        Applications: owner,
      }));

      return {
        data: mappedData,
        count,
      };
    },
  });

  return { ...result, revalidate: () => client.invalidateQueries(["NDC_APPLICATIONS_LIST", params]) };
};

export const useNDCSearchApplicationEmployee = (params, tenantId, config = {}, t) => {
  const client = useQueryClient();
  const result = useQuery(["NDC_APPLICATIONS_LIST", params], useNDCSearch(params, tenantId, config), {
    staleTime: Infinity,
    select: (data) => {
      const objData = data?.data;
      return objData;
    },
  });

  return { ...result, revalidate: () => client.invalidateQueries(["NDC_APPLICATIONS_LIST", params]), refetch: result.refetch };
};
