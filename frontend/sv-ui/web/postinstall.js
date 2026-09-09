const fs = require("fs");
const path = require("path");
const { execSync } = require("child_process");

const baseDir = __dirname;

// 1. Fix ALL broken `module` fields anywhere in node_modules (any nesting level)
// A broken module field points to a file that doesn't exist
try {
  const result = execSync(
    `find . -name "package.json" -path "*/node_modules/*" -not -path "*/node_modules/*/node_modules/*/node_modules/*/node_modules/*"`,
    { cwd: baseDir, encoding: "utf-8" }
  );
  result.trim().split("\n").filter(Boolean).forEach((rel) => {
    const full = path.join(baseDir, rel);
    const dir = path.dirname(full);
    try {
      const json = JSON.parse(fs.readFileSync(full, "utf-8"));
      if (json.module && !fs.existsSync(path.join(dir, json.module))) {
        delete json.module;
        fs.writeFileSync(full, JSON.stringify(json, null, 2));
        console.log(`Fixed broken module field: ${json.name || rel}`);
      }
    } catch {}
  });
} catch {}

// 2. Shim renamed babel plugins
const nodeModulesDirs = [
  path.join(baseDir, "node_modules"),
  path.join(baseDir, "micro-ui-internals", "node_modules"),
];

const shims = [
  {
    old: "@babel/plugin-proposal-unicode-property-regex",
    new: "@babel/plugin-transform-unicode-property-regex",
  },
];

shims.forEach(({ old: oldPkg, new: newPkg }) => {
  nodeModulesDirs.forEach((nmDir) => {
    const newPkgPath = path.join(nmDir, newPkg);
    if (!fs.existsSync(newPkgPath)) return;

    const shimDir = path.join(nmDir, oldPkg);
    const shimPkg = path.join(shimDir, "package.json");
    const shimIndex = path.join(shimDir, "index.js");

    if (fs.existsSync(shimPkg)) return;

    fs.mkdirSync(shimDir, { recursive: true });
    fs.writeFileSync(shimPkg, JSON.stringify({ name: oldPkg, version: "7.0.0", main: "index.js" }, null, 2));
    fs.writeFileSync(shimIndex, `module.exports = require(${JSON.stringify(newPkg)});\n`);
    console.log(`Shimmed: ${oldPkg} -> ${newPkg} in ${nmDir}`);
  });
});
