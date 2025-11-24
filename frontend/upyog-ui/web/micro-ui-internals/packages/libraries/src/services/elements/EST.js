import Urls from "../atoms/urls";
import { Request } from "../atoms/Utils/Request";

export const ESTService = {
  create: (details, tenantId) =>
    Request({
      url: Urls.est.create,
      data: details,
      useCache: false,
      setTimeParam: false,
      userService: true,
      method: "POST",
      params: {},
      auth: true,
    }),

    allotmentcreate: (details, tenantId) =>
  Request({
    url: Urls.est.allotment,
    data: details,
    useCache: false,
    setTimeParam: false,
    userService: true,
    method: "POST",
    params: {},
    auth: true,
  }),


  assetSearch: ({ tenantId, filters }) =>
  Request({
    url: Urls.est.search,
    useCache: false,
    method: "POST",
    auth: true,
    userService: true,
    params: { tenantId },
    data: filters,
  }),

  allotmentSearch:(details, tenantId) =>
  Request({
      url: Urls.est.allotmentSearch,
      data: details,
      useCache: false,
      setTimeParam: false,
      userService: true,
      method: "POST",
      params: {},
      auth: true,
    }),

fetchBill: ({ tenantId, consumerCode, businessService }) =>
  Request({
    url: Urls.payment.fetch_bill,
    useCache: false,
    method: "POST",
    auth: true,
    userService: true,
    params: { tenantId, consumerCode, businessService },
    data: {},
  }),

};
