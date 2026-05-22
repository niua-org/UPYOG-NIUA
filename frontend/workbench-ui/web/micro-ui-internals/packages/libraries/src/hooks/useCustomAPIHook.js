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

  const {
    isLoading,
    data,
    isFetching,
    refetch,
    error,
    isError,
  } = useQuery({
    queryKey: [url, changeQueryName, params, body].filter((e) => e),
    queryFn: () =>
      CustomService.getResponse({
        url,
        params,
        body,
        plainAccessRequest,
      }),
    staleTime: 0, // Always fetch fresh data
    gcTime: 5 * 60 * 1000, // Keep in cache for 5 minutes
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
          queryKey: [url, changeQueryName, params, body].filter((e) => e),
        });
      }
    },
  };
};

export default useCustomAPIHook;