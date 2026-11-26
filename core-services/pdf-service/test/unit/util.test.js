import { describe, expect, test } from "@jest/globals";
import { toRes } from "../../src/lib/util.js";

function mockRes() {
  return {
    status(code) {
      this.statusCode = code;
      return this;
    },
    send(body) {
      this.sent = body;
      return this;
    },
    json(body) {
      this.jsonBody = body;
      return this;
    },
  };
}

describe("toRes", () => {
  test("sends json with the default status when there is no error", () => {
    const res = mockRes();
    toRes(res)(null, { ok: true });
    expect(res.statusCode).toBe(200);
    expect(res.jsonBody).toEqual({ ok: true });
  });

  test("uses a caller-supplied success status", () => {
    const res = mockRes();
    toRes(res, 201)(null, { id: 1 });
    expect(res.statusCode).toBe(201);
    expect(res.jsonBody).toEqual({ id: 1 });
  });

  test("sends 400 when the callback receives an error", () => {
    const res = mockRes();
    const err = new Error("boom");
    toRes(res)(err, { ignored: true });
    expect(res.statusCode).toBe(400);
    expect(res.sent).toBe(err);
    expect(res.jsonBody).toBeUndefined();
  });

  test("serialises models that implement toObject", () => {
    const res = mockRes();
    toRes(res)(null, { toObject: () => ({ id: 7, nested: true }) });
    expect(res.statusCode).toBe(200);
    expect(res.jsonBody).toEqual({ id: 7, nested: true });
  });

  test("leaves objects whose toObject property is not a function as-is", () => {
    const res = mockRes();
    const thing = { toObject: "nope", id: 3 };
    toRes(res)(null, thing);
    expect(res.jsonBody).toEqual({ toObject: "nope", id: 3 });
  });

  test("passes null, empty, and zero payloads through json", () => {
    const missing = mockRes();
    toRes(missing)(null, null);
    expect(missing.statusCode).toBe(200);
    expect(missing.jsonBody).toBeNull();

    const empty = mockRes();
    toRes(empty, 204)(null, "");
    expect(empty.statusCode).toBe(204);
    expect(empty.jsonBody).toBe("");

    const zero = mockRes();
    toRes(zero)(null, 0);
    expect(zero.jsonBody).toBe(0);
  });
});
