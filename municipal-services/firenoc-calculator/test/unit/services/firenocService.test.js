jest.mock("../../../src/utils/api", () => ({
  httpRequest: jest.fn(),
}));

jest.mock("../../../src/utils", () => ({
  generateFireNOCSearchURL: jest.fn(() => "/firenoc/v1/_search?applicationNumber=FN-1"),
}));

jest.mock("../../../src/envVariables", () => ({
  IS_ENVIRONMENT_CENTRAL_INSTANCE: "false",
  EGOV_FIRENOC_SERVICE_HOST: "http://firenoc",
}));

const { httpRequest } = require("../../../src/utils/api");
const { getFireNoc } = require("../../../src/services/firenocService");

describe("firenocService.getFireNoc", () => {
  beforeEach(() => {
    httpRequest.mockReset();
  });

  test("searches firenoc with tenant header from tenantId", async () => {
    httpRequest.mockResolvedValue({ FireNOCs: [{ id: "1" }] });

    const result = await getFireNoc({ apiId: "calc" }, "FN-1", "pb", {});

    expect(result.FireNOCs).toHaveLength(1);
    expect(httpRequest).toHaveBeenCalledWith(
      expect.objectContaining({
        hostURL: "http://firenoc",
        endPoint: "/firenoc/v1/_search?applicationNumber=FN-1",
        headers: expect.objectContaining({ tenantId: "pb" }),
      }),
    );
  });

  test("when httpRequest fails throws search error message", async () => {
    httpRequest.mockRejectedValue(new Error("network"));

    await expect(getFireNoc({}, "FN-1", "pb", {})).rejects.toEqual("FireNOC Search Error");
  });
});
