import {
  convertFooterStringtoFunctionIfExist,
  getDateInRequiredFormat,
  getLocalisationkey,
  getTransformedLocale,
  getValue,
} from "../src/utils/commons.js";

describe("getTransformedLocale", () => {
  test("uppercases and replaces separators with underscores", () => {
    expect(getTransformedLocale("pdf.key:name-value/part here")).toBe(
      "PDF_KEY_NAME_VALUE_PART_HERE"
    );
  });

  test("returns an empty string for empty input", () => {
    expect(getTransformedLocale("")).toBe("");
  });

  test("throws when the label is null", () => {
    expect(() => getTransformedLocale(null)).toThrow(TypeError);
  });
});

describe("getValue", () => {
  test("returns the value when it is present", () => {
    expect(getValue("citizen", "NA", "user.name")).toBe("citizen");
    expect(getValue(["ward"], "NA", "address.ward")).toEqual(["ward"]);
  });

  test("returns the default for null, empty, and blank values", () => {
    expect(getValue(null, "NA", "user.name")).toBe("NA");
    expect(getValue(undefined, "NA", "user.name")).toBe("NA");
    expect(getValue("", "NA", "user.name")).toBe("NA");
    expect(getValue([], "NA", "user.name")).toBe("NA");
    expect(getValue([null], "NA", "user.name")).toBe("NA");
    expect(getValue([""], "NA", "user.name")).toBe("NA");
  });
});

describe("getLocalisationkey", () => {
  test("builds a prefixed localisation key", async () => {
    await expect(
      getLocalisationkey("PT", "property-tax", false, false, false)
    ).resolves.toBe("PT_PROPERTY_TAX");
  });

  test("returns null or an empty list when the key is null or empty", async () => {
    await expect(
      getLocalisationkey("PT", null, true, true, true)
    ).resolves.toBeNull();
    await expect(
      getLocalisationkey("PT", [], false, false, false)
    ).resolves.toEqual([]);
    await expect(
      getLocalisationkey("", "", false, false, false)
    ).resolves.toBe("");
  });

  test("rejects when a numeric key is split into category parts", async () => {
    await expect(
      getLocalisationkey("PT", 10, true, false, false)
    ).rejects.toThrow(TypeError);
  });
});

describe("getDateInRequiredFormat", () => {
  test("formats an epoch in the configured timezone", () => {
    // 1700000000000 ms is 2023-11-15 03:43:20 in Asia/Kolkata.
    expect(getDateInRequiredFormat(1700000000000, "DD/MM/YYYY")).toBe(
      "15/11/2023"
    );
  });

  test("returns NA when the epoch is missing", () => {
    expect(getDateInRequiredFormat(null)).toBe("NA");
    expect(getDateInRequiredFormat(undefined)).toBe("NA");
    expect(getDateInRequiredFormat("")).toBe("NA");
    expect(getDateInRequiredFormat(0)).toBe("NA");
  });

  test("returns Invalid date for an unparseable epoch", () => {
    expect(getDateInRequiredFormat("not-a-date", "DD/MM/YYYY")).toBe(
      "Invalid date"
    );
  });
});

describe("convertFooterStringtoFunctionIfExist", () => {
  test("restores a pdfmake footer function from its string form", () => {
    const footer = convertFooterStringtoFunctionIfExist(
      "function (currentPage, pageCount) { return currentPage + '/' + pageCount; }"
    );
    expect(typeof footer).toBe("function");
    expect(footer(2, 5)).toBe("2/5");
  });

  test("returns undefined or null when no footer string is stored", () => {
    expect(convertFooterStringtoFunctionIfExist(undefined)).toBeUndefined();
    expect(convertFooterStringtoFunctionIfExist(null)).toBeNull();
  });

  test("throws when the footer string is not a function expression", () => {
    expect(() => convertFooterStringtoFunctionIfExist("not a function {{{")).toThrow(
      SyntaxError
    );
  });
});
