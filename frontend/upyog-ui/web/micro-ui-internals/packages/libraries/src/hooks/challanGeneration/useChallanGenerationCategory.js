import { useQuery } from "react-query";
import { MdmsService } from "../../services/elements/MDMS";

/**
 * Fetches challan generation categories from MDMS using React Query.
 *
 * - Calls MdmsService.getPaymentRules with tenantId and filter.
 * - Extracts BusinessService data from the response.
 * - Removes duplicate categories based on code prefix.
 * - Adds an i18n key for each category for localization.
 *
 * @param {string} tenantId - Tenant identifier
 * @param {Object} filter - MDMS query filters
 * @param {Object} config - Optional React Query config
 *
 * @returns {Object} { Categories, data }
 */

const useChallanGenerationCategory = (tenantId, filter, config = {}) => {

const {data} =  useQuery("ChallanGeneration_CATEGORY_SERVICE", () => MdmsService.getPaymentRules(tenantId, filter), config);
let Categories = [];
data?.MdmsRes?.BillingService?.BusinessService.map((ob) => {
  let found = Categories.length>0? Categories?.some(el => el?.code.split(".")[0] === ob.code.split(".")[0]) : false;  
  if(!found) Categories.push({...ob, i18nkey:`BILLINGSERVICE_BUSINESSSERVICE_${(ob.code.split(".")[0]).toUpperCase()}`})
})

return {Categories, data};
};


export default useChallanGenerationCategory;