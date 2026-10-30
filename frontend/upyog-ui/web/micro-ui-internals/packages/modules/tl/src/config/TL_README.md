# Accordion form

All sections sit on one page. The citizen fills the open section, then opens the next one. A later section stays closed until required fields above it are filled.

Edit `AccordionNewApplication.json`. Keys named `_comment` or `_readme` are notes. The screen ignores them.

## Pages

1. `info` — documents the citizen must bring
2. `apply` — the accordion
3. `check` — review and submit
4. `acknowledgement` — after submit

Keep `navigation.pattern` as `"accordion"`.

## Add a section

Add one object in `body[]`. `order` sets the panel order. `key` must match the timeline step.

```json
{
  "key": "ownersDocs",
  "order": 3,
  "i18nKey": "TL_STEP_OWNERS_DOCS",
  "labelDefault": "Ownership & documents",
  "hintDefault": "Owner details and document uploads in this section.",
  "form": []
}
```

The `confirm` block is the check page, not a panel. Leave `isPreview: true`, `type: "review"`, and `form: []`.

## Add a field

Put the question inside that section’s `form[]`.

```json
{
  "order": 1,
  "key": "TL_TRADE_NAME",
  "apiFieldName": "tradeName",
  "field": { "code": "TL_TRADE_NAME", "name": "tradeName", "type": "text" },
  "validation": { "required": true },
  "messages": { "error": "TL_TRADE_NAME_REQUIRED" }
}
```

- `field.name` is the form value.
- `apiFieldName` is the name sent to the server.
- `validation.required: true` makes it mandatory. Leave it out when the field is optional.
- `visibleWhen` shows the field only when another answer matches.

```json
"visibleWhen": { "field": "structureType", "equals": "IMMOVABLE" }
```

A `sectionHeader` is a heading inside a panel. It is not a new panel and is not sent to the server.

## Timeline

`navigation.timeline.steps` is the progress bar. One step per panel, then Summary on the check page. `sections` must use the same `key` as `body[]`.

## What stays in code

Labels, options, and show/hide rules belong in the JSON. JavaScript that JSON cannot hold (property search, billing slabs, create-property path) stays in `tlFormConfig.js` and `TLCitizenNewApplication.js`.
