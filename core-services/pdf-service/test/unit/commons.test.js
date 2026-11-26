import { afterAll, afterEach, beforeEach, describe, expect, jest, test } from "@jest/globals";
import axios from "axios";
import moment from "moment-timezone";
import nock from "nock";
import envVariables from "../../src/EnvironmentVariables.js";
import {
  convertFooterStringtoFunctionIfExist,
  findLocalisation,
  getDateInRequiredFormat,
  getLocalisationkey,
  getTransformedLocale,
  getValue,
  verifyCache,
} from "../../src/utils/commons.js";

moment.suppressDeprecationWarnings = true;

const HOST = "http://egov-localization:8080";
const SEARCH = "/localization/messages/v2/_search";

// Stop axios from honouring HTTP_PROXY. Otherwise nock never sees the
// localisation host and the call is blocked by disableNetConnect.
axios.defaults.proxy = false;
nock.disableNetConnect();

if (envVariables.DATE_TIMEZONE !== "Asia/Kolkata") {
  throw new Error(
    `Expected DATE_TIMEZONE Asia/Kolkata before commons.js loaded, got ${envVariables.DATE_TIMEZONE}`
  );
}

function kolkata(epochMs, pattern) {
  const shifted = new Date(epochMs + (5 * 60 + 30) * 60 * 1000);
  const dd = String(shifted.getUTCDate()).padStart(2, "0");
  const mm = String(shifted.getUTCMonth() + 1).padStart(2, "0");
  const yyyy = String(shifted.getUTCFullYear());
  const hh = String(shifted.getUTCHours()).padStart(2, "0");
  const min = String(shifted.getUTCMinutes()).padStart(2, "0");
  if (pattern === "DD/MM/YYYY") return `${dd}/${mm}/${yyyy}`;
  if (pattern === "YYYY-MM-DD HH:mm") return `${yyyy}-${mm}-${dd} ${hh}:${min}`;
  throw new Error(`unsupported pattern ${pattern}`);
}

describe("getTransformedLocale", () => {
  test("uppercases and replaces separators", () => {
    expect(getTransformedLocale("pt.bill:due-date / copy")).toBe(
      "PT_BILL_DUE_DATE___COPY"
    );
  });

  test("returns an empty string unchanged", () => {
    expect(getTransformedLocale("")).toBe("");
  });

  test("collapses repeated separators and keeps characters outside the class", () => {
    expect(getTransformedLocale("a..b")).toBe("A__B");
    expect(getTransformedLocale("a\tb")).toBe("A_B");
    expect(getTransformedLocale("  ")).toBe("__");
    expect(getTransformedLocale("WARD")).toBe("WARD");
    expect(getTransformedLocale("a\\b")).toBe("A\\B");
  });

  test("throws when the label is null, undefined, or not a string", () => {
    expect(() => getTransformedLocale(null)).toThrow(TypeError);
    expect(() => getTransformedLocale(undefined)).toThrow(TypeError);
    expect(() => getTransformedLocale(12)).toThrow(TypeError);
  });
});

describe("getValue", () => {
  test("returns a present value", () => {
    expect(getValue("ward", "NA", "$.ward")).toBe("ward");
    expect(getValue(["a", "b"], "NA", "$.list")).toEqual(["a", "b"]);
  });

  test("returns the default for null, undefined, and empty values", () => {
    expect(getValue(undefined, "NA", "$.missing")).toBe("NA");
    expect(getValue(null, "NA", "$.missing")).toBe("NA");
    expect(getValue("", "NA", "$.name")).toBe("NA");
    expect(getValue([], [], "$.list")).toEqual([]);
    expect(getValue([null], "NA", "$.list")).toBe("NA");
    expect(getValue([""], "NA", "$.list")).toBe("NA");
  });

  test("keeps zero, false, whitespace, and longer blank arrays", () => {
    expect(getValue(0, "NA", "$.count")).toBe(0);
    expect(getValue(false, "NA", "$.flag")).toBe(false);
    expect(getValue(" ", "NA", "$.name")).toBe(" ");
    expect(getValue(["ok"], "NA", "$.list")).toEqual(["ok"]);
    expect(getValue([0], "NA", "$.list")).toEqual([0]);
    expect(getValue(["", ""], "NA", "$.list")).toEqual(["", ""]);
  });
});

describe("getDateInRequiredFormat", () => {
  const epoch = 1609459200000;

  test("formats an epoch in Asia/Kolkata", () => {
    expect(getDateInRequiredFormat(epoch)).toBe(kolkata(epoch, "DD/MM/YYYY"));
    expect(getDateInRequiredFormat(epoch, "YYYY-MM-DD HH:mm")).toBe(
      kolkata(epoch, "YYYY-MM-DD HH:mm")
    );
  });

  test("returns NA for empty and other falsy epochs, including zero", () => {
    expect(getDateInRequiredFormat(undefined)).toBe("NA");
    expect(getDateInRequiredFormat(null)).toBe("NA");
    expect(getDateInRequiredFormat("")).toBe("NA");
    expect(getDateInRequiredFormat(0)).toBe("NA");
    expect(getDateInRequiredFormat(false)).toBe("NA");
    expect(getDateInRequiredFormat(NaN)).toBe("NA");
  });

  test("formats a pre-epoch instant and rejects numeric strings", () => {
    expect(getDateInRequiredFormat(-1)).toBe(kolkata(-1, "DD/MM/YYYY"));
    expect(getDateInRequiredFormat("1609459200000")).toBe("Invalid date");
    expect(getDateInRequiredFormat("not-a-date")).toBe("Invalid date");
  });
});

describe("getLocalisationkey", () => {
  test("builds a key from the prefix when no part is requested", async () => {
    await expect(getLocalisationkey("mod", "north zone/a", false, false, false)).resolves.toBe(
      "MOD_NORTH_ZONE_A"
    );
    await expect(getLocalisationkey(undefined, "ab.cd", false, false, false)).resolves.toBe(
      "AB_CD"
    );
    await expect(getLocalisationkey("", "ab.cd", false, false, false)).resolves.toBe("AB_CD");
  });

  test("returns null and undefined keys unchanged", async () => {
    await expect(getLocalisationkey("mod", null, false, false, false)).resolves.toBeNull();
    await expect(
      getLocalisationkey("mod", undefined, true, true, true)
    ).resolves.toBeUndefined();
  });

  test("returns an empty array and rewrites an empty string when a prefix is present", async () => {
    await expect(getLocalisationkey("mod", [], false, false, false)).resolves.toEqual([]);
    await expect(getLocalisationkey("mod", "", false, false, false)).resolves.toBe("MOD_");
    await expect(getLocalisationkey(undefined, "", false, false, false)).resolves.toBe("");
  });

  test("localises each requested part and each array entry", async () => {
    await expect(
      getLocalisationkey("TL", "GOODS.RETAIL.TST-1", true, false, false)
    ).resolves.toBe("TL_GOODS");
    await expect(
      getLocalisationkey("TL", "GOODS.RETAIL.TST-1", false, true, false)
    ).resolves.toBe("TL_RETAIL");
    await expect(
      getLocalisationkey("TL", "GOODS.RETAIL.TST-1", false, false, true)
    ).resolves.toBe("TL_GOODS_RETAIL_TST_1");
    await expect(
      getLocalisationkey("TL", ["ward", "block"], false, false, false)
    ).resolves.toEqual(["TL_WARD", "TL_BLOCK"]);
  });

  test("honours a custom delimiter when the subtype is appended", async () => {
    await expect(
      getLocalisationkey("TL", "GOODS.RETAIL", true, false, true, " | ")
    ).resolves.toBe("TL_GOODS | TL_GOODS_RETAIL");
  });

  test("drops the category part when the main type is also required", async () => {
    await expect(
      getLocalisationkey("TL", "GOODS.RETAIL.TST-1", true, true, false)
    ).resolves.toBe("TL_RETAIL");
    await expect(
      getLocalisationkey("TL", "GOODS.RETAIL.TST-1", true, true, true)
    ).resolves.toBe("TL_RETAIL / TL_GOODS_RETAIL_TST_1");
  });

  test("turns a missing main-type segment into the literal UNDEFINED", async () => {
    await expect(getLocalisationkey("TL", "GOODS", false, true, false)).resolves.toBe(
      "TL_UNDEFINED"
    );
  });

  test("accepts a number when no part split is required and throws when a part is required", async () => {
    await expect(getLocalisationkey("mod", 0, false, false, false)).resolves.toBe("MOD_0");
    await expect(getLocalisationkey("mod", 12, true, false, false)).rejects.toThrow(TypeError);
    await expect(getLocalisationkey("mod", { a: 1 }, false, false, false)).rejects.toThrow(
      TypeError
    );
  });
});

describe("convertFooterStringtoFunctionIfExist", () => {
  test("restores a footer function expression", () => {
    const footer = convertFooterStringtoFunctionIfExist(
      "function (currentPage, pageCount) { return currentPage + ' of ' + pageCount; }"
    );
    expect(footer(2, 9)).toBe("2 of 9");
  });

  test("returns undefined when no footer is configured", () => {
    expect(convertFooterStringtoFunctionIfExist(undefined)).toBeUndefined();
  });

  test("evaluates a non-function expression and throws on an empty string", () => {
    expect(convertFooterStringtoFunctionIfExist("1+1")).toBe(2);
    expect(convertFooterStringtoFunctionIfExist(null)).toBeNull();
    expect(() => convertFooterStringtoFunctionIfExist("")).toThrow(SyntaxError);
  });
});

describe("verifyCache and findLocalisation", () => {
  beforeEach(() => {
    jest.spyOn(console, "log").mockImplementation(() => {});
  });

  afterEach(() => {
    jest.restoreAllMocks();
    nock.cleanAll();
  });

  afterAll(() => {
    nock.enableNetConnect();
  });

  test("returns null when the key has not been stored", async () => {
    await expect(verifyCache("missing-pdf-key")).resolves.toBeNull();
  });

  test("posts search criteria and returns the localisation body", async () => {
    const pdfKey = "pdf-search-happy";
    const requestInfo = {
      msgId: "req-1|hi_IN",
      userInfo: { tenantId: "pb.amritsar", type: "EMPLOYEE" },
    };
    let captured;
    const scope = nock(HOST)
      .post(SEARCH, (body) => {
        captured = body;
        return true;
      })
      .reply(200, { messages: [{ code: "WATER", message: "Water" }] });

    const data = await findLocalisation(requestInfo, ["PT", "BILLING"], ["WATER", "TAX"], pdfKey);

    expect(data).toEqual({ messages: [{ code: "WATER", message: "Water" }] });
    expect(captured).toEqual({
      RequestInfo: requestInfo,
      messageSearchCriteria: {
        tenantId: "pb",
        locale: "hi_IN",
        module: "PT,BILLING",
        codes: ["WATER", "TAX"],
      },
    });
    expect(scope.isDone()).toBe(true);
  });

  test("falls back to the default locale and tenant", async () => {
    const cases = [
      { msgId: "no-locale", locale: "en_IN" },
      { msgId: null, locale: "en_IN" },
      { msgId: "", locale: "en_IN" },
      { msgId: "id|hi_IN|extra", locale: "hi_IN" },
    ];

    for (const item of cases) {
      let captured;
      nock(HOST)
        .post(SEARCH, (body) => {
          captured = body;
          return true;
        })
        .reply(200, { messages: [{ code: "C", message: "m" }] });

      await findLocalisation(
        { msgId: item.msgId },
        "PT",
        "A,B",
        `pdf-fallback-${item.msgId}`
      );

      expect(captured.messageSearchCriteria.locale).toBe(item.locale);
      expect(captured.messageSearchCriteria.tenantId).toBe("pg");
      expect(captured.messageSearchCriteria.module).toBe("PT");
      expect(captured.messageSearchCriteria.codes).toEqual(["A", "B"]);
    }
  });

  test("stores the body under pdfKey and still calls HTTP for the same key", async () => {
    const pdfKey = "pdf-cache-mismatch";
    const requestInfo = { msgId: "req-9|hi_IN", userInfo: { tenantId: "pb" } };
    const payload = { messages: [{ code: "HELLO", message: "Hello" }] };
    const scope = nock(HOST).post(SEARCH).times(2).reply(200, payload);

    await findLocalisation(requestInfo, ["PT"], ["HELLO"], pdfKey);
    await expect(verifyCache(pdfKey)).resolves.toEqual(payload);
    await expect(verifyCache(`${pdfKey}-hi_IN`)).resolves.toBeNull();

    await findLocalisation(requestInfo, ["PT"], ["HELLO"], pdfKey);
    expect(scope.isDone()).toBe(true);
  });

  test("does not cache when pdfKey is null", async () => {
    const scope = nock(HOST)
      .post(SEARCH)
      .times(2)
      .reply(200, { messages: [{ code: "C", message: "m" }] });

    const requestInfo = { msgId: "req|en_IN", userInfo: { tenantId: "pg" } };
    await findLocalisation(requestInfo, ["PT"], ["C"], null);
    await findLocalisation(requestInfo, ["PT"], ["C"], null);

    await expect(verifyCache("null")).resolves.toBeNull();
    await expect(verifyCache("null-en_IN")).resolves.toBeNull();
    expect(scope.isDone()).toBe(true);
  });

  test("rethrows HTTP errors that include a response", async () => {
    const pdfKey = "pdf-http-500";
    nock(HOST).post(SEARCH).reply(500, { error: "down" });

    let thrown;
    try {
      await findLocalisation(
        { msgId: "req|en_IN", userInfo: { tenantId: "pg" } },
        ["PT"],
        ["C"],
        pdfKey
      );
    } catch (error) {
      thrown = error;
    }

    expect(thrown).toBeInstanceOf(Error);
    expect(thrown.response && thrown.response.status).toBe(500);
    await expect(verifyCache(pdfKey)).resolves.toBeNull();
  });

  test("rethrows transport errors that have no response", async () => {
    nock(HOST).post(SEARCH).replyWithError("socket hang up");

    let thrown;
    try {
      await findLocalisation(
        { msgId: "req|en_IN", userInfo: { tenantId: "pg" } },
        ["PT"],
        ["C"],
        "pdf-transport"
      );
    } catch (error) {
      thrown = error;
    }

    expect(thrown).toBeInstanceOf(Error);
    expect(Boolean(thrown.response)).toBe(false);
    expect(String(thrown.message)).toMatch(/socket hang up/);
  });

  test("throws when requestInfo or moduleList is missing", async () => {
    await expect(findLocalisation(null, ["PT"], ["C"], "pdf-null-request")).rejects.toThrow(
      TypeError
    );
    await expect(
      findLocalisation({ msgId: "req|en_IN" }, null, ["C"], "pdf-null-module")
    ).rejects.toThrow(TypeError);
  });
});
