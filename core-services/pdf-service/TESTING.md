# pdf-service tests

Unit tests cover the pure helpers in `src/utils/commons.js` and `src/lib/util.js`. They do not import `src/index.js`, because that module calls `app.listen` on load and then starts Kafka.

Requires Node `>=22` (`package.json` `engines`). Jest 29 does not run on the Node 15 binary that is first on some developer PATH values. Use a Node 22 binary, for example `~/.nvm/versions/node/v22.22.2/bin`.

## Commands

| Script | What it runs |
| --- | --- |
| `npm test` / `npm run test:unit` | Jest unit tests, in band, with coverage. No Docker and no outbound HTTP. Localisation calls are mocked with nock, and axios proxy settings are disabled so the mock sees the localisation host. |
| `npm run test:integration` | Jest suite under `test/integration`. The placeholder is skipped when `docker info` fails. It does not start containers. |
| `npm run lint` | `eslint src` (this used to be `npm test`). |

## Coverage target and enforced gate

The service-wide target is **80% statements** and **70% branches**.

That target is not the Jest gate. `collectCoverageFrom` is only `src/utils/commons.js` and `src/lib/util.js`. Most of the service (Express routes, pdfmake, Postgres, Kafka, filestore) is outside this suite, so a gate of 80/70 across all of `src/` would fail.

The enforced gate is the level this unit suite meets:

| Metric | Service-wide target | Enforced gate | Measured (`npm test`, Node v22.22.2) |
| --- | --- | --- | --- |
| Statements | 80% | 98% | 98.96% (96/97) |
| Branches | 70% | 96% | 97.05% (66/68) |
| Functions | — | 100% | 100% (13/13) |
| Lines | — | 98% | 98.93% (93/94) |

The uncovered line is the localisation cache-hit return in `src/utils/commons.js`. The cache is written under `pdfKey` and read under `pdfKey-locale`, so that return is unreachable until the key mismatch is fixed.
