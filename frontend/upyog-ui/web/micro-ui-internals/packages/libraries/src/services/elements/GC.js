import { Request } from "../atoms/Utils/Request";
import Urls from "../atoms/urls";

export const GCServices = {
  create: (details, tenantId) =>
    Request({
      url: Urls.gc.create,
      data: details,
      useCache: false,
      setTimeParam: false,
      userService: true,
      method: "POST",
      params: { tenantId },
      auth: true,
    }),
  search: async ({ tenantId, filters, auth, data }) => {
    return await Request({
      url: Urls.gc.search,
      data: data || {},
      useCache: false,
      setTimeParam: false,
      method: "POST",
      auth: auth === false ? auth : true,
      userService: auth === false ? auth : true,
      params: { tenantId, ...filters },
    });
  },
  update: (details, tenantId) =>
    Request({
      url: Urls.gc.update,
      data: details,
      useCache: false,
      setTimeParam: false,
      userService: true,
      method: "POST",
      params: { tenantId },
      auth: true,
    }),
  disconnect: (details, tenantId) =>
    Request({
      url: Urls.gc.disconnect,
      data: details,
      useCache: false,
      setTimeParam: false,
      userService: true,
      method: "POST",
      params: { tenantId },
      auth: true,
    }),
  reconnect: (details, tenantId) =>
    Request({
      url: Urls.gc.reconnect,
      data: details,
      useCache: false,
      setTimeParam: false,
      userService: true,
      method: "POST",
      params: { tenantId },
      auth: true,
    }),
  permanent: (details, tenantId) =>
    Request({
      url: Urls.gc.permanent,
      data: details,
      useCache: false,
      setTimeParam: false,
      userService: true,
      method: "POST",
      params: { tenantId },
      auth: true,
    }),
  connectionUpdate: (details, tenantId) =>
    Request({
      url: Urls.gc.connectionUpdate,
      data: details,
      useCache: false,
      setTimeParam: false,
      userService: true,
      method: "POST",
      params: { tenantId },
      auth: true,
    }),
  connectionHistory: (details, tenantId, params = {}) =>
    Request({
      url: Urls.gc.connectionHistory,
      data: details || {},
      useCache: false,
      setTimeParam: false,
      userService: true,
      method: "POST",
      params: { tenantId, ...params },
      auth: true,
    }),
};