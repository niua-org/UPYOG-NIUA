import { CardLabel, Dropdown, SubmitBar, TextInput } from "@upyog/workbench-ui-react-components";
import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { getEditableSections, isSchemaItemVisible } from "../utils/formSchemaUtils";

/**
 * Live preview of a form schema (the JSON the builder saves), laid out the way the form will be used:
 * accordion forms as collapsible section panels followed by the confirm declaration, single forms as one form.
 * visibleWhen rules are applied as values change, so show/hide logic can be tried out.
 */

const getItemLabel = (item, t) => item?.messages?.labelDefault || t(item?.field?.code || item?.label?.code || item?.key || "");

// Display label for an option (i18nKey first, same order as the upyog-ui renderer)
const getOptionLabel = (option, t) => (option.i18nKey ? t(option.i18nKey) : option.name || option.code);

// Options of a radio / checkbox / dropdown; MDMS-backed fields get sample options in the preview
const getItemOptions = (item, t) => {
  const options =
    Array.isArray(item.options) && item.options.length > 0
      ? item.options
      : [1, 2].map((num) => ({ code: `OPTION_${num}`, name: `${item.field?.dataSource?.masterName || "Sample"} Option ${num}` }));
  return options.map((option) => ({ ...option, label: getOptionLabel(option, t) }));
};

const PreviewItem = ({ item, values, setValue, t }) => {
  if (!isSchemaItemVisible(item, values)) return null;

  const label = getItemLabel(item, t);
  if (item.type === "sectionHeader") return <h4 className="preview-field-heading preview-item-heading">{label}</h4>;
  if (item.type === "paragraph") return <p className="preview-field-paragraph preview-item-paragraph">{label}</p>;

  const fieldObj = item.field || {};
  // Repeatable groups and other items the builder keeps as uploaded
  if (["fieldArray", "group"].includes(item.type) || !fieldObj.type) {
    return (
      <div className="passthrough-field-note">
        {label} ({item.type || "field"}) — {t("WBH_PREVIEW_NOT_AVAILABLE") || "preview not available for this item"}
      </div>
    );
  }

  const validation = item.validation || {};
  const name = fieldObj.name || item.key;
  const value = values[name];
  const disabled = !!validation.disabled;
  const readOnly = !!validation.readOnly;

  const renderControl = () => {
    switch (fieldObj.type) {
      case "text":
      case "password":
      case "date":
        return (
          <TextInput
            type={fieldObj.type}
            value={value || ""}
            onChange={(e) => setValue(name, e.target.value)}
            placeholder={fieldObj.placeholder || ""}
            disabled={disabled}
            readOnly={readOnly}
          />
        );
      case "textarea":
        return (
          <textarea
            value={value || ""}
            onChange={(e) => setValue(name, e.target.value)}
            placeholder={fieldObj.placeholder || ""}
            disabled={disabled}
            readOnly={readOnly}
            rows={3}
            className="preview-textarea"
          />
        );
      case "dropdown": {
        const options = getItemOptions(item, t);
        return (
          <Dropdown
            option={options}
            optionKey="label"
            selected={options.find((option) => option.code === value)}
            select={(option) => setValue(name, option?.code)}
            placeholder={fieldObj.placeholder || ""}
            disable={disabled || readOnly}
            t={t}
          />
        );
      }
      case "radio":
        return (
          <div className="preview-options-group">
            {getItemOptions(item, t).map((option) => (
              <label key={option.code} className={`preview-option-label ${disabled ? "disabled" : ""}`}>
                <input
                  type="radio"
                  name={`preview_${name}`}
                  checked={value === option.code}
                  disabled={disabled || readOnly}
                  onChange={() => setValue(name, option.code)}
                  className="preview-control-input"
                />
                <span>{option.label}</span>
              </label>
            ))}
          </div>
        );
      case "checkbox": {
        const checked = Array.isArray(value) ? value : [];
        return (
          <div className="preview-options-group">
            {getItemOptions(item, t).map((option) => (
              <label key={option.code} className={`preview-option-label ${disabled ? "disabled" : ""}`}>
                <input
                  type="checkbox"
                  checked={checked.includes(option.code)}
                  disabled={disabled || readOnly}
                  onChange={(e) =>
                    setValue(name, e.target.checked ? [...checked, option.code] : checked.filter((code) => code !== option.code))
                  }
                  className="preview-control-input"
                />
                <span>{option.label}</span>
              </label>
            ))}
          </div>
        );
      }
      case "file":
        return (
          <div className="preview-file-box">
            <span className="preview-file-icon">📁</span>
            <span className="preview-file-title">{t("WBH_CLICK_TO_UPLOAD") || "Choose file to upload"}</span>
            <span className="preview-file-hint">
              {fieldObj.accept || ".png,.jpg,.jpeg,.pdf"}
              {validation.maxSizeMB ? ` | Max: ${validation.maxSizeMB}MB` : ""}
            </span>
          </div>
        );
      default:
        return null;
    }
  };

  return (
    <div className="preview-field-item">
      {item.heading && <h4 className="preview-field-heading preview-item-heading">{item.heading}</h4>}
      <CardLabel className="config-label preview-field-label">
        {label}
        {validation.required && <span className="mandatory-asterisk"> *</span>}
      </CardLabel>
      {item.paragraph && <p className="preview-field-paragraph preview-item-paragraph">{item.paragraph}</p>}
      {renderControl()}
      {item.messages?.error && <span className="preview-rule-hint">Rules: {t(item.messages.error)}</span>}
    </div>
  );
};

const PreviewSectionFields = ({ section, values, setValue, t }) =>
  section.form.length === 0 ? (
    <div className="preview-empty-state">
      <p>{t("WBH_NO_FIELDS_TO_PREVIEW") || "No fields added in this section yet."}</p>
    </div>
  ) : (
    <div className="preview-fields-grid">
      {section.form.map((item, idx) => (
        <PreviewItem key={item.key || idx} item={item} values={values} setValue={setValue} t={t} />
      ))}
    </div>
  );

/**
 * @param {object} props
 * @param {object} props.schema - Form schema JSON (as built for save)
 * @param {string} props.formName - Form key in the schema
 * @param {object} props.values - Preview values keyed by field.name
 * @param {Function} props.onChange - (nextValues) => void
 */
const FormSchemaPreview = ({ schema, formName, values, onChange }) => {
  const { t } = useTranslation();
  const formEntry = schema?.[formName]?.[0] || {};
  const navigation = formEntry.navigation || {};
  const isAccordion = navigation.pattern === "accordion";
  const sections = getEditableSections(schema, formName);
  const reviewSection = (formEntry.body || []).find((section) => section?.type === "review");

  const [openSectionKey, setOpenSectionKey] = useState(navigation.accordion?.defaultOpenStep || sections[0]?.key);
  const [isDeclared, setIsDeclared] = useState(false);

  const setValue = (name, value) => onChange({ ...values, [name]: value });

  if (!isAccordion) {
    return sections[0] ? (
      <div className="preview-card-container">
        <h3 className="preview-section-title">{sections[0].labelDefault || t(sections[0].i18nKey)}</h3>
        <PreviewSectionFields section={sections[0]} values={values} setValue={setValue} t={t} />
      </div>
    ) : null;
  }

  return (
    <div className="preview-card-container">
      <h3 className="preview-section-title">{formEntry.headDefault || t(formEntry.head)}</h3>

      {/* Accordion: one panel per section, opened from its header */}
      {sections.map((section, idx) => {
        const isOpen = openSectionKey === section.key;
        return (
          <div key={section.key} className={`preview-accordion-panel ${isOpen ? "open" : ""}`}>
            <button type="button" className="preview-accordion-header" onClick={() => setOpenSectionKey(isOpen ? null : section.key)}>
              <span className="preview-accordion-step">{idx + 1}</span>
              <span className="preview-accordion-title">{section.labelDefault || t(section.i18nKey)}</span>
              <span className="preview-accordion-chevron">{isOpen ? "▲" : "▼"}</span>
            </button>
            {isOpen && (
              <div className="preview-accordion-body">
                <PreviewSectionFields section={section} values={values} setValue={setValue} t={t} />
              </div>
            )}
          </div>
        );
      })}

      {/* Confirm (check page): declaration before submit */}
      {reviewSection?.declaration && (
        <div className="preview-declaration">
          <label className="preview-option-label">
            <input type="checkbox" checked={isDeclared} onChange={(e) => setIsDeclared(e.target.checked)} className="preview-control-input" />
            <span>{reviewSection.declaration.labelDefault || t(reviewSection.declaration.i18nKey)}</span>
          </label>
          <SubmitBar
            label={reviewSection.submitLabelDefault || t(reviewSection.submitLabel || "COMMON_SUBMIT_APPLICATION")}
            disabled={reviewSection.declaration.required && !isDeclared}
            onSubmit={() => {}}
          />
        </div>
      )}
    </div>
  );
};

export default FormSchemaPreview;
