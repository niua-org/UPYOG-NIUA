# pdf-service test findings

Production behaviour was not changed. `src/index.js` already default-exports the Express app and also calls `app.listen` on import, so no extra testability export was added. Tests characterize current behaviour.

## Covered

Jest unit tests in `pdf-service/test/unit/`, no outbound network (`nock.disableNetConnect()`):

- `getTransformedLocale`: separators, empty string, repeated separators, characters outside the class, null / undefined / number throw.
- `getValue`: present values, null / undefined / empty string / empty array / `[null]` / `[""]`, and boundaries `0`, `false`, whitespace, one-element arrays, longer blank arrays.
- `getDateInRequiredFormat`: Asia/Kolkata formatting, falsy epochs including `0`, a pre-epoch instant, numeric strings, and invalid strings.
- `getLocalisationkey`: prefix on/off, null and undefined, empty array and empty string, category / main / subtype parts, arrays, custom delimiter, the current category drop, a missing segment, numeric keys, and non-arrays.
- `convertFooterStringtoFunctionIfExist`: function expression, missing footer, non-function expression, empty string.
- `verifyCache`: miss, and hit after `findLocalisation` stores under `pdfKey`.
- `findLocalisation`: success body and search criteria (nock), locale and tenant fallbacks, cache key mismatch (second call still hits HTTP), `pdfKey` null, HTTP 500, transport error, null `requestInfo` / `moduleList`.
- `toRes` in `src/lib/util.js`: default status, custom status, error send, `toObject`, non-function `toObject`, null / empty / zero payloads.

`src/lib/util.js` is commented as unused by the service. It is still unit-tested because `toRes` is a pure export.

## Not covered

- HTTP routes and PDF generation in `src/index.js`. Importing that module listens on `SERVER_PORT` and then calls `listenConsumer` / `connectProducer`. supertest was not used.
- Auth, validation, and error-handler middleware. `src/middleware/index.js` only exports a factory that returns an empty router, and the file is commented as unused. Those handlers are not exported.
- `src/utils/directMapping.js`, `src/utils/externalAPIMapping.js`, `src/utils/fileStoreAPICall.js`.
- `src/queries.js` (Postgres), Kafka producer/consumer, qrcode, pdfmake, and PDF merge.
- The localisation cache-hit return (`src/utils/commons.js` line 70). Nothing writes the key that `findLocalisation` reads.
- Docker integration. `npm run test:integration` is a real Jest script; its only test is skipped when `docker info` fails.

## Bugs not fixed

1. **Localisation cache key does not match.** `findLocalisation` reads `pdfKey + '-' + locale` (`src/utils/commons.js` around lines 63–65) and writes `cache.set(pdfKey, responseBody.data)` (around lines 121–122). The hit return on line 70 never runs. A repeat call with the same `pdfKey` still performs HTTP. `verifyCache(pdfKey)` can see the stored body; `verifyCache(pdfKey + '-' + locale)` cannot.

2. **Main-type localisation overwrites the category.** In `getLocalisationkey`, when `isMainTypeRequired` is set, the code assigns the delimiter and then replaces `codeFromKey` with only the main-type label (`src/utils/commons.js` around lines 177–183). Category text built just above is dropped. The subtype branch appends instead of replacing. A key with no second segment becomes the literal `UNDEFINED` (`TL` + `GOODS` with main type required → `TL_UNDEFINED`).

3. **Epoch `0` is treated as missing.** `getDateInRequiredFormat` returns `"NA"` for every falsy value (`if (!et)`), so the Unix epoch cannot be formatted. A numeric string such as `"1609459200000"` is not coerced to milliseconds; moment warns and the formatted result is `"Invalid date"`.

4. **`getTransformedLocale` has no null guard.** `null`, `undefined`, and numbers throw `TypeError` on `toUpperCase`. Other helpers in the same file tolerate null.

5. **`getLocalisationkey` accepts numbers, then splits them.** A number with no part flags is stringified (`0` + prefix `mod` → `MOD_0`). The same number with a part flag throws `TypeError` because `.split` is called. Plain objects and booleans are treated as arrays and throw because they have no `.map`.

6. **Footer strings are compiled with `Function`.** `convertFooterStringtoFunctionIfExist` evaluates the config string. An empty string throws `SyntaxError`. A non-function expression such as `1+1` is executed and returned. `null` is not treated as “no footer” (`!= undefined` is true for null) and comes back as `null`.

7. **`findLocalisation` does not validate inputs.** `requestInfo` null throws on `requestInfo.msgId`. `moduleList` null throws on `toString()` before the HTTP call.

8. **`getValue` only special-cases a one-element empty array.** `["", ""]` is returned as-is. `0` and `false` are kept, which is consistent for those values.

9. **Duplicate `pn_IN` font mapping.** `defaultFontMapping` in `src/index.js` (around lines 117–123) sets `pn_IN` to `BalooPaaji` and then to `AnmolUni`. The BalooPaaji entry is dead.

## Test run

Node `v22.22.2`, cwd `pdf-service`.

`npm test` (`npm run test:unit`): exit 0. 2 suites passed, 35 tests passed. Coverage gate 98/96/100/98 met. Measured statements 98.96% (96/97), branches 97.05% (66/68), functions 100% (13/13), lines 98.93% (93/94). Uncovered line: `src/utils/commons.js:70`.

`npm run test:integration`: exit 0. 1 suite skipped, 1 test skipped (`docker info` failed in this environment). No containers were started and no outbound HTTP was made.
