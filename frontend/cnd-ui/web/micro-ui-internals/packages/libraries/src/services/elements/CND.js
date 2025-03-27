import Urls from "../atoms/urls";
import { Request } from "../atoms/Utils/Request";


export const CNDService = {
  create: (details, tenantId) =>
    Request({
      url: Urls.cnd.create,
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
      url: Urls.cnd.search,
      useCache: false,
      method: "POST",
      auth: auth === false ? auth : true,
      userService: auth === false ? auth : true,
      params: { tenantId, ...filters },
    }),

  update: (details) =>
    Request({
      url: Urls.cnd.update,
      data: details,
      useCache: false,
      setTimeParam: false,
      userService: true,
      method: "POST",
      params: {},
      auth: true,
  }),



};




