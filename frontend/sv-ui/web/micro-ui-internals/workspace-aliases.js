import fs from "fs";
import path from "path";

/**
 * This function builds a list of alias mappings for all local packages.
 * It is used by the Vite dev server so that imports like
 * "@nudmcdgnpm/digit-ui-libraries" resolve directly to the local source file
 * instead of the built "dist/" folder — this enables live HMR during development.
 *
 * How it works:
 * - Reads the root package.json to find all scripts that start with "dev:" (e.g. dev:libraries, dev:sv).
 * - Scans the packages/ and packages/modules/ directories for matching folders.
 * - For each match, maps the package name to its source entry point (the "source" or "style" field in its package.json).
 *
 * @param {string} rootDir - The root directory of micro-ui-internals (where package.json lives).
 * @returns {Object} - An object of alias mappings, e.g. { "@nudmcdgnpm/digit-ui-libraries": "/path/to/src/index.js" }
 */

export default function getWorkspaceAliases(rootDir) {
  const rootPkg = JSON.parse(fs.readFileSync(path.join(rootDir, "package.json"), "utf-8"));
  const packagesDir = path.join(rootDir, "packages");
  const aliases = {};

  const localNames = Object.keys(rootPkg.scripts || {})
    .filter((k) => k.startsWith("dev:") && k !== "dev:example");

  for (const dir of [packagesDir, path.join(packagesDir, "modules")]) {
    if (!fs.existsSync(dir)) continue;
    for (const name of fs.readdirSync(dir)) {
      const pkgPath = path.join(dir, name, "package.json");
      if (!fs.existsSync(pkgPath)) continue;
      const pkg = JSON.parse(fs.readFileSync(pkgPath, "utf-8"));
      const entry = pkg.source || pkg.style;
      if (pkg.name && entry && localNames.includes(`dev:${name}`)) {
        aliases[pkg.name] = path.join(dir, name, entry);
      }
    }
  }

  return aliases;
}
