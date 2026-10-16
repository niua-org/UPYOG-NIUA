/**
 * Citizen My Applications config.
 * Local JSON is the default. MDMS TradeLicense.CitizenMyApplicationsConfig
 * overrides it when that master is present, same as Estate.
 */

/** JSON keys that start with _ are author notes, not screen config. */
const isNoteKey = (key) => String(key).startsWith("_");

/** Drop note keys so they are not copied onto the live screen config. */
const withoutNotes = (value) => {
  if (!value || typeof value !== "object" || Array.isArray(value)) return value;
  return Object.keys(value).reduce((acc, key) => {
    if (!isNoteKey(key)) acc[key] = value[key];
    return acc;
  }, {});
};

/** MDMS may return one object, a list, or { body: [...] }. Use the first record. */
export const pickFirstMdmsEntry = (data) => {
  if (!data) return null;
  if (Array.isArray(data)) return data[0] || null;
  if (Array.isArray(data.body)) return data.body[0] || null;
  return data;
};

/**
 * Merge MDMS over the local CitizenMyApplications.json.
 * Filters, card rows, and actions stay local when the master omits them.
 */
export const resolveCitizenMyApplicationsConfig = (localConfig, mdmsData) => {
  const local = withoutNotes(localConfig) || {};
  const entry = withoutNotes(pickFirstMdmsEntry(mdmsData));
  if (!entry || typeof entry !== "object") return local;

  return {
    ...local,
    ...entry,
    emptyState: { ...(local.emptyState || {}), ...(entry.emptyState || {}) },
    messages: { ...(local.messages || {}), ...(entry.messages || {}) },
    searchDefaults: {
      ...(local.searchDefaults || {}),
      ...(entry.searchDefaults || {}),
    },
    paginationDefaults: {
      ...(local.paginationDefaults || {}),
      ...(entry.paginationDefaults || {}),
    },
    filters:
      Array.isArray(entry.filters) && entry.filters.length > 0
        ? entry.filters
        : local.filters,
    resultFields:
      Array.isArray(entry.resultFields) && entry.resultFields.length > 0
        ? entry.resultFields
        : local.resultFields,
    cardActions:
      Array.isArray(entry.cardActions) && entry.cardActions.length > 0
        ? entry.cardActions
        : local.cardActions,
    actionButton: { ...(local.actionButton || {}), ...(entry.actionButton || {}) },
  };
};

/** Prototype keys a config path must never read. */
const BLOCKED_PATH_KEYS = new Set(["__proto__", "constructor", "prototype"]);

/** Read a dotted path off a license. Config-supplied keys cannot walk the prototype. */
const readField = (license, accessor) => {
  if (!accessor || license == null) return undefined;
  return String(accessor)
    .split(".")
    .reduce((current, key) => {
      if (current == null || BLOCKED_PATH_KEYS.has(key)) return undefined;
      return current[key];
    }, license);
};

/** Owner line used by resultFields format "owner". */
export const ownerLabel = (license) => {
  const detail = license?.tradeLicenseDetail;
  const category = detail?.subOwnerShipCategory || "";
  // Institutions show the organisation name. Other licenses list owner names in sequence.
  if (category.includes("INSTITUTION")) {
    return detail?.institution?.name || detail?.institution?.instituionName || "";
  }
  const owners = Array.isArray(detail?.owners) ? [...detail.owners] : [];
  owners.sort(
    (a, b) =>
      (a?.additionalDetails?.ownerSequence ?? 0) -
      (b?.additionalDetails?.ownerSequence ?? 0)
  );
  return owners.map((owner) => owner?.name).filter(Boolean).join(", ");
};

/** Card button rule. No rule means always show. equals compares the license field as text. */
export const matchesVisibleWhen = (rule, license) => {
  if (!rule) return true;
  const value = readField(license, rule.field);
  if (rule.equals !== undefined) return String(value ?? "") === String(rule.equals);
  return true;
};

/** Card cell text for one resultFields entry. */
export const formatMyApplicationField = (license, field, { t, emptyLabel, formatDate }) => {
  if (field?.format === "owner") return ownerLabel(license) || emptyLabel;
  if (field?.format === "workflowStatus") {
    const status = readField(license, field.accessor);
    if (!status) return emptyLabel;
    return t(`${field.i18nPrefix || "WF_NEWTL_"}${status}`);
  }
  if (field?.format === "epochDate") {
    const formatted = formatDate ? formatDate(readField(license, field.accessor)) : "";
    return formatted || emptyLabel;
  }
  const raw = readField(license, field?.accessor);
  if (raw === undefined || raw === null || raw === "") return emptyLabel;
  return raw;
};

/**
 * Apply JSON filters on the client.
 * text: case-insensitive includes on matchFields (or the filter name).
 * dropdown: exact match on the filter name.
 * Returns the same array when nothing is filtering, so the list does not copy.
 */
export const applyMyApplicationFilters = (licenses, filters, applied) => {
  if (!Array.isArray(licenses)) return [];
  const active = (filters || []).filter((filter) => {
    const query = applied?.[filter.name];
    return query !== undefined && query !== null && String(query).trim() !== "";
  });
  if (active.length === 0) return licenses;

  return active.reduce((rows, filter) => {
    const query = applied[filter.name];
    if (filter.type === "dropdown") {
      const wanted = String(query).toUpperCase();
      return rows.filter(
        (item) => String(readField(item, filter.name) || "").toUpperCase() === wanted
      );
    }
    const fields =
      Array.isArray(filter.matchFields) && filter.matchFields.length > 0
        ? filter.matchFields
        : [filter.name];
    const needle = String(query).trim().toUpperCase();
    return rows.filter((item) =>
      fields.some((field) => String(readField(item, field) || "").toUpperCase().includes(needle))
    );
  }, licenses);
};

/**
 * Sort card rows when paginationDefaults.sortBy is set.
 * Numbers (epoch dates) compare numerically. Empty values stay at the end.
 *
 * @param {object[]} rows
 * @param {{ sortBy?: string, sortOrder?: string }} [pagination]
 * @returns {object[]}
 */
export const sortMyApplications = (rows, pagination = {}) => {
  const sortBy = pagination?.sortBy;
  if (!sortBy || !Array.isArray(rows) || rows.length < 2) return rows;
  const direction = String(pagination.sortOrder || "ASC").toUpperCase() === "DESC" ? -1 : 1;
  return [...rows].sort((a, b) => {
    const left = readField(a, sortBy);
    const right = readField(b, sortBy);
    const leftEmpty = left === undefined || left === null || left === "";
    const rightEmpty = right === undefined || right === null || right === "";
    if (leftEmpty && rightEmpty) return 0;
    if (leftEmpty) return 1;
    if (rightEmpty) return -1;
    const leftNum = Number(left);
    const rightNum = Number(right);
    if (Number.isFinite(leftNum) && Number.isFinite(rightNum)) {
      if (leftNum === rightNum) return 0;
      return leftNum > rightNum ? direction : -direction;
    }
    return String(left).localeCompare(String(right)) * direction;
  });
};
