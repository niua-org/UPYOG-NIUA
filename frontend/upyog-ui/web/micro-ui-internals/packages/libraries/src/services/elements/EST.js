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

  search: ({ tenantId, filters, auth }) =>
    Request({
      url: Urls.est.search,
      useCache: false,
      data: filters,
      method: "POST",
      auth: auth === false ? auth : true,
      userService: auth === false ? auth : true,
      params: { tenantId },
    }),
    

  update: (details, tenantId) =>
    Request({
      url: Urls.est.update,
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



  applicationSearch: ({ tenantId, filters, auth }) =>
    Request({
      url: Urls.est.applicationSearch,
      useCache: false,
      method: "POST",
      auth: auth === false ? auth : true,
      userService: auth === false ? auth : true,
      params: { tenantId, ...filters },
    }),

    allotmentSearch: ({ tenantId, filters }) =>
  Request({
    url: Urls.est.allotmentSearch,
    useCache: false,
    method: "POST",
    auth: true,
    userService: true,
    params: { tenantId },
    data: filters,
  }),
    
 
};
