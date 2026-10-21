import React from "react";
import { FieldsRow, PaletteSelectField } from "./ThemeCustomizeComponents";
import { getNestedValue } from "../utils/getNestedValue";

/**
 * PaletteFieldGroup Component
 * Config-driven component that maps over an array of field configuration objects
 * and renders a FieldsRow containing PaletteSelectField dropdown components.
 * 
 * @param {Object} props
 * @param {Array} props.fields - Array of field config objects { label, path, defaultToken, defaultHex, extraPaths }.
 * @param {Object} props.themeExtend - Active theme extend configuration object.
 * @param {Object} props.palette - Predefined 15-color palette object.
 * @param {Object} props.paletteDescriptions - Map of palette code descriptions.
 * @param {Function} props.onChange - Selection trigger callback (handleThemeFieldChange).
 * @param {Function} [props.t] - Optional translation function.
 */
export function PaletteFieldGroup({
  fields = [],
  themeExtend,
  palette = {},
  paletteDescriptions = {},
  onChange,
  t
}) {
  return (
    <FieldsRow>
      {fields.map((field) => {
        let val = getNestedValue(themeExtend, field.path);

        if (val === undefined && field.extraPaths) {
          for (const extraPath of field.extraPaths) {
            val = getNestedValue(themeExtend, extraPath);
            if (val !== undefined) break;
          }
        }

        if (val === undefined) {
          val = field.defaultToken ? palette[field.defaultToken] : field.defaultHex;
        }

        const handleChange = (newVal) => {
          onChange(field.path, newVal);
          if (field.extraPaths) {
            field.extraPaths.forEach((extraPath) => onChange(extraPath, newVal));
          }
        };

        const labelText = t ? t(field.label) : field.label;

        return (
          <PaletteSelectField
            key={field.path}
            label={labelText}
            value={val}
            palette={palette}
            paletteDescriptions={paletteDescriptions}
            onChange={handleChange}
          />
        );
      })}
    </FieldsRow>
  );
}

export default PaletteFieldGroup;
