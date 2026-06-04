import { useQuery, useQueryClient } from "@tanstack/react-query";
import { CustomService } from "../services/elements/CustomService";

const useCustomAPIHook = ({
  url,
  params,
  body,
  config = {},
  plainAccessRequest,
  changeQueryName,
}) => {
  const client = useQueryClient();
  const bodyKey = JSON.stringify(body);

  const {
    isLoading,
    data,
    isFetching,
    refetch,
    error,
    isError,
  } = useQuery({
    queryKey: [url, changeQueryName, params, bodyKey].filter((e) => e),
    queryFn: () =>
      CustomService.getResponse({
        url,
        params,
        body,
        plainAccessRequest,
      }),
    staleTime: 0,
    gcTime: 5 * 60 * 1000,
    ...config,
  });

  return {
    isLoading,
    isFetching,
    data,
    error,
    isError,
    refetch,
    revalidate: () => {
      if (data) {
        client.invalidateQueries({
          queryKey: [url, changeQueryName, params, bodyKey].filter((e) => e),
        });
      }
    },
  };
};

export default useCustomAPIHook;
