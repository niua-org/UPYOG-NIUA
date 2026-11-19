jest.mock("../../../src/utils/api", () => ({
  httpRequest: jest.fn(),
}));

jest.mock("../../../src/envVariables", () => ({
  IS_ENVIRONMENT_CENTRAL_INSTANCE: "false",
  EGOV_FN_CALCULATOR_HOST: "http://calculator",
  EGOV_FN_CALCULATOR_CONTEXT_PATH: "/fn-calculator",
  EGOV_FN_CALCULATOR_CALCULATOR_ENPOINT: "/v1/_calculate",
}));

const { httpRequest } = require("../../../src/utils/api");
const { calculate } = require("../../../src/services/firenocCalculatorService");

describe("firenocCalculatorService.calculate", () => {
  beforeEach(() => {
    httpRequest.mockReset();
  });

  test("posts calculation criteria with tenant header from firenoc", async () => {
    httpRequest.mockResolvedValue({ Calculation: [{ totalAmount: 100 }] });
    const firenoc = { tenantId: "pb.amritsar", applicationNumber: "FN-1" };
    const requestInfo = { apiId: "firenoc", msgId: "m1" };
    const header = { authToken: "token" };

    const response = await calculate(firenoc, requestInfo, header);

    expect(response.Calculation[0].totalAmount).toBe(100);
    expect(httpRequest).toHaveBeenCalledTimes(1);
    const call = httpRequest.mock.calls[0][0];
    expect(call.requestBody.CalulationCriteria[0].tenantId).toBe("pb.amritsar");
    expect(call.headers.tenantId).toBe("pb.amritsar");
  });

});
