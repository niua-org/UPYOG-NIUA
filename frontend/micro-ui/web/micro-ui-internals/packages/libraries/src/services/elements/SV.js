import Urls from "../atoms/urls";
import { Request } from "../atoms/Utils/Request";
export const SVService = {
  create: (details, tenantId) =>
    Request({
      url: Urls.sv.create,
      data: details,
      useCache: false,
      setTimeParam: false,
      userService: true,
      method: "POST",
      params: {},
      auth: true,
    })
};



