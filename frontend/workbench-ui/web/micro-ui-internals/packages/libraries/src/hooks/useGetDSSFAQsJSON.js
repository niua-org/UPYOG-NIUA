import { queryTemplate } from "../common/queryTemplate"; 

const useGetDSSFAQsJSON = (tenantId) => {
    return queryTemplate({
        queryKey: ["FAQS", tenantId],
        queryFn: () => Digit.MDMSService.getDSSFAQsJSONData(tenantId)
    });
};

export default useGetDSSFAQsJSON;