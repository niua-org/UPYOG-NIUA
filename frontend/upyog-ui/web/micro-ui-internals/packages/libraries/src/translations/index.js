import i18next from "i18next";
import { initReactI18next } from "react-i18next";
import ReactPostprocessor from "i18next-react-postprocessor";

const i18nextConfig = () => ({
  lng: Digit.StoreData.getCurrentLanguage(),
  fallbackLng: "en_IN",
  debug: false,
  ns: ["translations"],
  defaultNS: "translations",
  keySeparator: false,
  saveMissing: false,
  saveMissingTo: "current",
  interpolation: {
    escapeValue: false,
    formatSeparator: ",",
  },
  postProcess: [`reactPostprocessor`, "templatePostprocessor"],
  react: {
    useSuspense: true,
    bindI18n: "loaded",
    bindI18nStore: "added",
  },
  resources: {
    en_IN: {
      translations: {
        welcome: "Welcome",
        ACTION_TEST_FAMILY_ID_REPORTS: "Family ID Reports",
        ACTION_TEST_FAMILY_ID_COVERAGE_REPORT: "Family ID Coverage Report",
        "rainmaker.family.total_registered_families": "Total Registered Families",
        "rainmaker.family.total_registered_members": "Total Registered Members",
        "rainmaker.family.families_verified": "Families Verified",
        "rainmaker.family.families_pending_verification": "Families Pending Verification",
        "rainmaker.family.families_rejected": "Families Rejected",
        "rainmaker.family.families_without_status": "Families Without Status",
        "rainmaker.family.fromDate": "From Date",
        "rainmaker.family.toDate": "To Date",
      },
    },
  },
});

function replaceLiterals(text = "", dynamicValues = {}) {
  let returnText = text;
  const regex = /[^\{\{][\{]\w+/;
  if (regex.exec(text) !== null) {
    Object.keys(dynamicValues).forEach((key) => {
      returnText = returnText.replace(`{${key.toUpperCase()}}`, dynamicValues[key]);
    });
  }
  return returnText;
}

const templatePostprocessor = {
  type: "postProcessor",
  name: "templatePostprocessor",
  process: function (value, key, options, translator) {
    return replaceLiterals(value, options);
  },
};

export const initI18n = (callback) => {
  return i18next
    .use(new ReactPostprocessor())
    .use(templatePostprocessor)
    .use(initReactI18next)
    .init(i18nextConfig(), () => {
      window.i18next = i18next;
      callback();
    });
};
