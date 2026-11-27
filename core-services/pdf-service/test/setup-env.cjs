"use strict";

// The sandbox injects an HTTP proxy. Axios would send localisation calls
// there, and nock would not see the service host. Unit tests must not
// leave the process.
for (const name of [
  "HTTP_PROXY",
  "HTTPS_PROXY",
  "ALL_PROXY",
  "http_proxy",
  "https_proxy",
  "all_proxy",
  "npm_config_proxy",
  "npm_config_https_proxy",
]) {
  delete process.env[name];
}
process.env.NO_PROXY = "*";
process.env.no_proxy = "*";

// Pin values before src/EnvironmentVariables.js is first imported.
process.env.DATE_TIMEZONE = "Asia/Kolkata";
process.env.EGOV_LOCALISATION_HOST = "http://egov-localization:8080";
process.env.EGOV_LOCALISATION_SEARCH = "/localization/messages/v2/_search";
process.env.DEFAULT_LOCALISATION_LOCALE = "en_IN";
process.env.DEFAULT_LOCALISATION_TENANT = "pg";
