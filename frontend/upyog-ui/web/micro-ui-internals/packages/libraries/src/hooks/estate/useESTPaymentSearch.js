import { useQuery, useQueryClient } from "react-query";

// Custom hook to search EST payments
// This hook provides a query interface for searching EST payments
// based on various filters, tenant ID, and authentication details.

const useESTPaymentSearch = ({ tenantId, filters, auth, searchedFrom = "" }, config = {}) => {
  const client = useQueryClient();
  const args = tenantId ? { tenantId, filters, auth } : { filters, auth };

  const defaultSelect = (data) => {
    if(data.Payments?.length > 0) {
      data.Payments[0].paymentId = data.Payments[0].paymentId || [];
    }
    return data;
  };

  const { isLoading, error, data, isSuccess } = useQuery(
    ["estPaymentSearchList", tenantId, filters, auth, config], 
    () => Digit.ESTService.paymentSearch(args), 
    {
      select: defaultSelect,
      ...config,
    }
  );

  return { 
    isLoading, 
    error, 
    data, 
    isSuccess, 
    revalidate: () => client.invalidateQueries(["estPaymentSearchList", tenantId, filters, auth]) 
  };
};

export default useESTPaymentSearch;
