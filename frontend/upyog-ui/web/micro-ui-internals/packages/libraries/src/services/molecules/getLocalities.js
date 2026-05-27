import { LocationService } from "../elements/Location";
import { StoreService } from "./Store/service";

export const getLocalities = {
  admin: async (tenant) => {
    await StoreService.defaultData(tenant, tenant, Digit.StoreData.getCurrentLanguage());
    const response = await LocationService.getLocalities(tenant);
    return response?.TenantBoundary?.[0] || { boundary: [] };
  },
  revenue: async (tenant) => {
    await StoreService.defaultData(tenant, tenant, Digit.StoreData.getCurrentLanguage());
    const response = await LocationService.getRevenueLocalities(tenant);
    return response?.TenantBoundary?.[0] || { boundary: [] };
  },
  grampanchayats: async (tenant) => {
    await StoreService.defaultData(tenant, tenant, Digit.StoreData.getCurrentLanguage());
    const response = await LocationService.getGramPanchayats(tenant);
    return response?.TenantBoundary?.[0] || { boundary: [] };
  },
};