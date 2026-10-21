import { FORM_TYPES } from "./workbenchUtils";

/**
 * Form schema helpers shared by the Form Builder list (create popup / JSON upload)
 * and the Form Create builder page.
 *
 * Schema shape (same for uploaded and saved JSON):
 * {
 *   tenantId, moduleName,
 *   [FORM_NAME]: [{ head, headDefault, navigation: { pattern: "accordion" | "single", ... }, body: [section, ...] }]
 * }
 * Each body section holds its questions in form[]. A section still to be built has form: [].
 */

// Field types the builder canvas can edit. Any other item (sectionHeader, fieldArray, hidden
// helper fields ...) is kept exactly as it came in and written back unchanged.
const EDITABLE_FIELD_TYPES = ["text", "password", "dropdown", "radio", "checkbox", "date", "textarea", "file"];

// Display-only builder types (Heading / Paragraph) <-> schema item type
const SCHEMA_TYPE_BY_DISPLAY_TYPE = { heading: "sectionHeader", paragraph: "paragraph" };
const DISPLAY_TYPE_BY_SCHEMA_TYPE = { sectionHeader: "heading", paragraph: "paragraph" };

// Builder file-format option code <-> field.accept
export const FILE_ACCEPT_BY_TYPE = {
  pdf: ".pdf",
  image: ".jpg,.jpeg,.png",
  pdf_image: ".jpg,.jpeg,.png,.pdf",
  doc: ".doc,.docx",
  excel: ".xls,.xlsx,.csv",
  all: ".jpg,.jpeg,.png,.pdf,.doc,.docx,.xls,.xlsx,.csv",
};

const SCHEMA_META_KEYS = ["tenantId", "environment", "userType", "moduleName"];

const deepClone = (value) => (value === undefined ? undefined : JSON.parse(JSON.stringify(value)));

const isBlank = (value) => value === undefined || value === null || value === "";

// Set obj[key] when value is present, otherwise remove the key (keeps the JSON free of empty values)
const setOrDelete = (obj, key, value) => {
  if (isBlank(value) || value === false) delete obj[key];
  else obj[key] = value;
};

const acceptToFileType = (accept) => {
  if (!accept) return undefined;
  const normalize = (str) => str.split(",").map((ext) => ext.trim().toLowerCase()).sort().join(",");
  const target = normalize(accept);
  return Object.keys(FILE_ACCEPT_BY_TYPE).find((code) => normalize(FILE_ACCEPT_BY_TYPE[code]) === target);
};

/**
 * Localization key from module + label, e.g. ("Trade License", "Applicant Name") -> TRADE_LICENSE_APPLICANT_NAME
 */
export const toLocalizationKey = (module, label) => {
  if (!label) return "";
  const clean = (str) =>
    str
      .trim()
      .toUpperCase()
      .replace(/[^A-Z0-9]/g, "_")
      .replace(/_+/g, "_")
      .replace(/^_+|_+$/g, "");
  return `${clean(module && module !== "Not Specified" ? module : "COMMON")}_${clean(label)}`;
};

/**
 * Form name typed in the create popup -> JSON key, e.g. "new registration" -> NEW_REGISTRATION
 */
export const toFormNameKey = (value) =>
  String(value || "")
    .toUpperCase()
    .replace(/[^A-Z0-9]/g, "_")
    .replace(/_+/g, "_")
    .replace(/^_+/, "");

/**
 * Field data key (field.name) from its label, e.g. "Trade Name" -> tradeName
 */
export const toFieldName = (label) => {
  const words = String(label || "")
    .replace(/[^a-zA-Z0-9]+/g, " ")
    .trim()
    .split(" ")
    .filter(Boolean);
  return words.map((word, idx) => (idx === 0 ? word.toLowerCase() : word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())).join("");
};

const toRoute = (key) =>
  String(key || "")
    .replace(/([a-z0-9])([A-Z])/g, "$1-$2")
    .replace(/[^a-zA-Z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .toLowerCase();

const humanize = (value) =>
  String(value || "")
    .replace(/([a-z0-9])([A-Z])/g, "$1 $2")
    .replace(/[_-]+/g, " ")
    .trim()
    .toLowerCase()
    .replace(/\b\w/g, (ch) => ch.toUpperCase());

/**
 * Section descriptor for a section known only by its key (e.g. a page refresh without navigation state)
 */
export const sectionFromKey = (key) => ({ key, i18nKey: key, labelDefault: humanize(key) });

/**
 * Section descriptor for a free-text section name (e.g. existing list rows)
 */
export const sectionFromLabel = (label) => ({ key: toFieldName(label) || "section1", i18nKey: label, labelDefault: label });

/**
 * Build a new form schema with every selected section present and form: [] (pending).
 *
 * @param {object} params
 * @param {string} params.tenantId
 * @param {string} params.moduleName
 * @param {string} params.formType - FORM_TYPES.ACCORDION | FORM_TYPES.SINGLE
 * @param {string} params.formName - JSON key of the form, e.g. NEW_REGISTRATION
 * @param {Array<{ key: string, i18nKey: string, labelDefault: string }>} params.sections - in display order
 * @returns {object} Form schema JSON
 */
export const buildFormSchemaSkeleton = ({ tenantId, environment, userType, moduleName, formType, formName, sections = [] }) => {
  const isSingle = formType === FORM_TYPES.SINGLE;
  const keyPrefix = toLocalizationKey(moduleName, "STEP").replace(/_STEP$/, "");

  const body = sections.map((section, idx) => ({
    key: section.key,
    route: toRoute(section.key),
    order: idx + 1,
    i18nKey: section.i18nKey,
    labelDefault: section.labelDefault,
    ...(idx === 0 ? { component: formName, payloadKey: moduleName, apiId: "Rainmaker" } : {}),
    form: [],
  }));

  const navigation = isSingle
    ? { pattern: "single", formRoute: "apply", ackRoute: "acknowledgement" }
    : {
      pattern: "accordion",
      formRoute: "apply",
      confirmRoute: "check",
      ackRoute: "acknowledgement",
      accordion: {
        defaultOpenStep: body[0]?.key,
        showFilledHint: true,
        submitLabel: "COMMON_SUBMIT_APPLICATION",
        submitLabelDefault: "Submit Application",
      },
      timeline: {
        citizenOnly: true,
        hideOnRoutes: ["acknowledgement"],
        steps: [
          ...body.map((section, idx) => ({
            step: idx + 1,
            label: section.i18nKey,
            labelDefault: section.labelDefault,
            sections: [section.key],
            ...(idx === 0 ? { routes: ["apply"] } : {}),
          })),
          { step: body.length + 1, label: `${keyPrefix}_STEP_CONFIRM`, labelDefault: "Summary", routes: ["check"] },
        ],
      },
    };

  // Accordion forms end with an auto-generated review section (check page)
  if (!isSingle) {
    body.push({
      key: "confirm",
      route: "check",
      order: body.length + 1,
      i18nKey: `${keyPrefix}_STEP_CONFIRM`,
      labelDefault: "Confirm",
      hintDefault: "Review each section, then confirm the declaration to submit.",
      isPreview: true,
      type: "review",
      reviewSectionsFromSteps: sections.map((section) => section.key),
      declaration: {
        required: true,
        i18nKey: `${keyPrefix}_DECLARATION_CONFIRM`,
        labelDefault:
          "I confirm that the information provided above is true and correct to the best of my knowledge. I understand that providing false information may lead to rejection or legal action.",
      },
      submitLabel: "COMMON_SUBMIT_APPLICATION",
      submitLabelDefault: "Submit Application",
      form: [],
    });
  }

  return {
    tenantId,
    // Chosen in the create popup: UAT / PRODUCTION / TEST and CITIZEN / EMPLOYEE
    ...(environment ? { environment } : {}),
    ...(userType ? { userType } : {}),
    moduleName,
    [formName]: [
      {
        head: toLocalizationKey(moduleName, formName),
        headDefault: humanize(formName),
        navigation,
        body,
      },
    ],
  };
};

/**
 * Validate an uploaded JSON against the form schema template.
 *
 * @param {object} json - Parsed upload
 * @returns {{ error?: string, formName?: string, formType?: string, moduleName?: string, sectionKeys?: string[] }}
 */
export const parseFormSchema = (json) => {
  if (!json || typeof json !== "object" || Array.isArray(json)) return { error: "INVALID_STRUCTURE" };

  const formName = Object.keys(json).find(
    (key) => !key.startsWith("_") && !SCHEMA_META_KEYS.includes(key) && Array.isArray(json[key]) && Array.isArray(json[key][0]?.body)
  );
  const formEntry = formName ? json[formName][0] : null;
  if (!formEntry?.navigation?.pattern) return { error: "INVALID_STRUCTURE" };
  if (!formEntry.body.every((section) => section?.key && Array.isArray(section.form))) return { error: "INVALID_STRUCTURE" };

  return {
    formName,
    moduleName: json.moduleName,
    formType: formEntry.navigation.pattern === "single" ? FORM_TYPES.SINGLE : FORM_TYPES.ACCORDION,
    sectionKeys: getEditableSections(json, formName).map((section) => section.key),
  };
};

/**
 * Sections the builder edits (the review-only confirm section is excluded).
 */
export const getEditableSections = (schema, formName) => (schema?.[formName]?.[0]?.body || []).filter((section) => section?.type !== "review");

/**
 * Return a copy of the schema with one section's form[] replaced.
 */
export const replaceSectionForm = (schema, formName, sectionKey, form) => {
  const next = deepClone(schema);
  const section = next[formName][0].body.find((s) => s.key === sectionKey);
  if (section) section.form = form;
  return next;
};

/**
 * Convert one schema form[] item into a builder canvas field.
 *
 * @param {object} item - Schema form item
 * @param {string} id - Unique builder field id
 * @param {Array<{ code: string, pattern: string }>} regexOptions - Predefined regex dropdown options
 * @returns {object} Builder field
 */
export const schemaItemToField = (item, id, regexOptions = []) => {
  const fieldObj = item?.field || {};

  // Heading (sectionHeader) / Paragraph items: display text only
  const displayType = DISPLAY_TYPE_BY_SCHEMA_TYPE[item?.type];
  if (displayType && item?.hidden !== true) {
    return {
      id,
      source: item,
      type: displayType,
      label: item?.messages?.labelDefault || item?.label?.code || item?.key || "",
      fieldKey: item?.key || item?.label?.code || "",
    };
  }

  const isEditable =
    !["sectionHeader", "fieldArray", "group"].includes(item?.type) && item?.hidden !== true && EDITABLE_FIELD_TYPES.includes(fieldObj.type);

  if (!isEditable) {
    return {
      id,
      passthrough: true,
      source: item,
      label: item?.messages?.labelDefault || item?.label?.code || fieldObj.code || item?.key,
      fieldKey: item?.key || fieldObj.name || "",
      type: item?.type || fieldObj.type || "unknown",
    };
  }

  const validation = item.validation || {};
  const messages = item.messages || {};
  const pattern = validation.pattern || "";
  const presetRegex = pattern ? regexOptions.find((r) => r.pattern && r.pattern === pattern) : null;
  const isFile = fieldObj.type === "file";

  return {
    id,
    source: item,
    label: messages.labelDefault || fieldObj.code || item.key,
    fieldKey: item.key || fieldObj.code || "",
    name: fieldObj.name || "",
    type: fieldObj.type,
    required: validation.required ? "required" : "optional",
    isReadonly: !!validation.readOnly,
    isDisabled: !!validation.disabled,
    placeholder: fieldObj.placeholder || "",
    minLength: isBlank(validation.minLength) ? "" : String(validation.minLength),
    maxLength: isBlank(validation.maxLength) ? "" : String(validation.maxLength),
    regexType: !pattern ? "none" : presetRegex ? presetRegex.code : "custom",
    pattern,
    patternErrorMessage: isFile ? "" : messages.error || "",
    heading: item.heading || "",
    paragraph: item.paragraph || "",
    mdmsModuleName: fieldObj.dataSource?.moduleName || "",
    mdmsMasterName: fieldObj.dataSource?.masterName || "",
    options: Array.isArray(item.options)
      ? item.options.map((opt) => ({ code: opt.code, name: opt.name || opt.i18nKey || opt.code }))
      : ["radio", "checkbox"].includes(fieldObj.type)
        ? []
        : undefined,
    uploadMode: isFile ? (fieldObj.multiple ? "multiple" : "single") : undefined,
    allowedFileTypes: isFile ? acceptToFileType(fieldObj.accept) : undefined,
    maxFileSize: isFile && !isBlank(validation.maxSizeMB) ? String(validation.maxSizeMB) : isFile ? "5" : undefined,
    maxFileCount: isFile ? String(fieldObj.maxFileCount || "5") : undefined,
    fileErrorMessage: isFile ? messages.error || "" : undefined,
  };
};

/**
 * Show/hide rules created with the builder's Logic drawer, keyed by the child field id.
 * The parent field stores { condition, selectedOption, targetFieldId }; the schema stores the rule on the child
 * as visibleWhen { field: <parent field.name>, equals } (or { field, in: [...] } for "Not Equals").
 *
 * @param {Array<object>} allFields - Builder fields of every section
 * @returns {object} { [childFieldId]: visibleWhen }
 */
export const buildVisibilityRules = (allFields = []) =>
  allFields.reduce((rules, parent) => {
    const logic = parent?.logic;
    if (!logic?.targetFieldId || isBlank(logic.selectedOption)) return rules;
    const parentName = parent.name || parent.fieldKey;

    if (logic.condition === "NOT_EQUALS") {
      const otherCodes = (parent.options || []).map((opt) => opt.code).filter((code) => code !== logic.selectedOption);
      if (otherCodes.length > 0) rules[logic.targetFieldId] = { field: parentName, in: otherCodes };
    } else {
      rules[logic.targetFieldId] = { field: parentName, equals: logic.selectedOption };
    }
    return rules;
  }, {});

/**
 * Convert one builder canvas field into a schema form[] item.
 * Keys the builder does not manage are carried over unchanged from the uploaded item.
 *
 * @param {object} field - Builder field
 * @param {number} order - 1-based position in the section
 * @param {object} [visibleWhen] - Rule from buildVisibilityRules for this field
 * @returns {object} Schema form item
 */
export const fieldToSchemaItem = (field, order, visibleWhen) => {
  if (field.passthrough) return { ...deepClone(field.source), order };

  // Heading / Paragraph: { key, type, label: { code }, messages: { labelDefault } }
  const displaySchemaType = SCHEMA_TYPE_BY_DISPLAY_TYPE[field.type];
  if (displaySchemaType) {
    const item = deepClone(field.source) || {};
    item.order = order;
    item.key = field.fieldKey;
    item.type = displaySchemaType;
    item.label = { ...(item.label || {}), code: item.label?.code || field.fieldKey };
    const messages = { ...(item.messages || {}) };
    if (field.label && (messages.labelDefault !== undefined || field.label !== item.label.code)) messages.labelDefault = field.label;
    if (Object.keys(messages).length > 0) item.messages = messages;
    if (visibleWhen) item.visibleWhen = visibleWhen;
    return item;
  }

  const item = deepClone(field.source) || {};
  const srcField = item.field || {};
  const isFile = field.type === "file";

  item.order = order;
  item.key = field.fieldKey;
  setOrDelete(item, "heading", field.heading);
  setOrDelete(item, "paragraph", field.paragraph);

  const fieldObj = {
    ...srcField,
    code: srcField.code || field.fieldKey,
    name: field.name || field.fieldKey,
    type: field.type,
  };
  setOrDelete(fieldObj, "placeholder", field.placeholder);
  // Keys written inside field{} by older builder versions; the schema keeps these elsewhere
  ["heading", "paragraph", "options", "uploadMode", "allowedFileTypes", "maxFileSize"].forEach((key) => delete fieldObj[key]);

  if (field.type === "dropdown") {
    fieldObj.dataSource = { ...(srcField.dataSource || { type: "MDMS", customiztionRequired: true }) };
    setOrDelete(fieldObj.dataSource, "moduleName", field.mdmsModuleName);
    setOrDelete(fieldObj.dataSource, "masterName", field.mdmsMasterName);
  }

  if (isFile) {
    setOrDelete(fieldObj, "accept", FILE_ACCEPT_BY_TYPE[field.allowedFileTypes] || srcField.accept);
    const isMultiple = field.uploadMode === "multiple";
    setOrDelete(fieldObj, "multiple", isMultiple);
    setOrDelete(fieldObj, "maxFileCount", isMultiple ? Number(field.maxFileCount || 5) : undefined);
  }
  item.field = fieldObj;

  if (["radio", "checkbox"].includes(field.type) && Array.isArray(field.options) && field.options.length > 0) {
    item.options = field.options.map((opt) => {
      const srcOpt = (field.source?.options || []).find((so) => so.code === opt.code) || {};
      const nextOpt = { ...srcOpt, code: opt.code || opt.name || "" };
      // Keep the uploaded i18nKey / name while the display label is unchanged; an edited label replaces them
      const isLabelUnchanged = srcOpt.i18nKey && opt.name === (srcOpt.name || srcOpt.i18nKey);
      if (!isLabelUnchanged) {
        nextOpt.name = opt.name || opt.code || "";
        delete nextOpt.i18nKey;
      }
      return nextOpt;
    });
  } else {
    delete item.options;
  }

  // Only rules that are switched on are written (no required/disabled/readOnly: false)
  const validation = { ...(item.validation || {}) };
  setOrDelete(validation, "required", field.required === "required");
  setOrDelete(validation, "disabled", !!field.isDisabled);
  setOrDelete(validation, "readOnly", !!field.isReadonly);
  setOrDelete(validation, "minLength", isBlank(field.minLength) ? undefined : Number(field.minLength));
  setOrDelete(validation, "maxLength", isBlank(field.maxLength) ? undefined : Number(field.maxLength));
  setOrDelete(validation, "pattern", field.pattern);
  if (isFile) setOrDelete(validation, "maxSizeMB", isBlank(field.maxFileSize) ? undefined : Number(field.maxFileSize));
  if (Object.keys(validation).length > 0) item.validation = validation;
  else delete item.validation;

  const messages = { ...(item.messages || {}) };
  setOrDelete(messages, "error", isFile ? field.fileErrorMessage : field.patternErrorMessage);
  if (field.label && (messages.labelDefault !== undefined || field.label !== fieldObj.code)) messages.labelDefault = field.label;
  if (Object.keys(messages).length > 0) item.messages = messages;
  else delete item.messages;

  if (visibleWhen) item.visibleWhen = visibleWhen;

  return item;
};

/**
 * Evaluate a visibleWhen rule against form values keyed by field.name.
 * Same comparison as the upyog-ui renderer (codes compared upper-cased) plus notEmpty / and / or used in schemas.
 */
const evaluateVisibleWhen = (rule, values) => {
  if (!rule) return true;
  if (Array.isArray(rule.and)) return rule.and.every((subRule) => evaluateVisibleWhen(subRule, values));
  if (Array.isArray(rule.or)) return rule.or.some((subRule) => evaluateVisibleWhen(subRule, values));
  if (!rule.field) return true;

  const raw = values?.[rule.field];
  const current = raw && typeof raw === "object" && !Array.isArray(raw) && raw.code != null ? raw.code : raw;
  const norm = (value) => String(value ?? "").trim().toUpperCase();
  // Checkbox values are arrays: the rule matches when any ticked option matches
  const currentList = Array.isArray(current) ? current.map(norm) : [norm(current)];

  if (rule.notEmpty) return currentList.some((value) => value !== "");
  if (rule.equals !== undefined) return currentList.includes(norm(rule.equals));
  if (Array.isArray(rule.in)) return rule.in.map(norm).some((value) => currentList.includes(value));
  return true;
};

/**
 * Whether a schema form item is shown for the given values (hidden items never are).
 *
 * @param {object} item - Schema form item
 * @param {object} values - Form values keyed by field.name
 * @returns {boolean}
 */
export const isSchemaItemVisible = (item, values) => item?.hidden !== true && evaluateVisibleWhen(item?.visibleWhen, values);

/**
 * Send the full form schema. There is no backend endpoint yet, so the payload is logged;
 * replace the body of this function with the API call once the endpoint exists.
 *
 * @param {object} schema - Full form schema JSON (all sections; pending sections have form: [])
 * @returns {Promise<object>}
 */
export const submitFormSchema = (schema) => {
  console.log("Submitting Form Schema JSON Payload: ", JSON.stringify(schema, null, 2));
  return Promise.resolve(schema);
};
