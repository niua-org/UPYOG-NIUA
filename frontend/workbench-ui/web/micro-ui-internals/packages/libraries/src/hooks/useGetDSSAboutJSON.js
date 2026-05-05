import { queryTemplate } from "../common/queryTemplate";


const useGetDSSAboutJSON = (tenantId) => {
    return queryTemplate({
        queryKey: ["ABOUT", tenantId],
        queryFn: () => Digit.MDMSService.getDSSAboutJSONData(tenantId)
    });
};

export default useGetDSSAboutJSON;