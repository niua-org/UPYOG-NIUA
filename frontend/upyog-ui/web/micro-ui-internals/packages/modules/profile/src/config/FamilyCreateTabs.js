import React from "react";
import { useLocation } from "react-router-dom";
import { createfamilyconfig } from "./createfamilyconfig";
import { profileStyles } from "../utils/profileStyles";

const TABS = createfamilyconfig.map((section) => ({
  head: section.head || section.body[0].texts?.header,
  route: section.body[0].route,
  allRoutes: section.body.map((b) => b.route),
}));

/**
 * @file FamilyCreateTabs.js
 * @module profile/ppp
 *
 * @description
 * Step-based tab navigator for the Family Create multi-step flow.
 *
 * Tabs are auto-derived from `createfamilyconfig` — each section's `head`
 * becomes a tab label, and its first `route` is the navigation target.
 *
 * Behaviour:
 * - Active tab is detected from the current URL route segment.
 * - Clicking a tab navigates to that step's first route.
 * - Future (not-yet-reached) tabs are visually disabled and non-clickable.
 * - Uses the same `profileStyles.tab / tabActive` as `EditProfile` tabs
 *   so the design stays consistent across the profile module.
 *
 *
 * @param {Function} props.t - i18n translation function
 */

const FamilyCreateTabs = ({ t }) => {
  const { pathname } = useLocation();
  const navigate = Digit.Hooks.useCustomNavigate();

  const basePath = pathname.substring(0, pathname.lastIndexOf("/"));
  const currentRoute = pathname.split("/").pop();
  const activeTabIndex = TABS.findIndex((tab) => tab.allRoutes.includes(currentRoute));

  // Handle the tab navigation, clicking on tab, user navigated as per the register route
  const handleTabClick = (tab, index) => {
    navigate(`${basePath}/${tab.route}`);
  };

  return (
    <div style={profileStyles.tabs}>
      {TABS.map((tab, index) => {
        const isActive = index === activeTabIndex;
        return (
          <button
            key={tab.route}
            type="button"
            style={{
              ...profileStyles.tab,
              ...(isActive ? profileStyles.tabActive : {}),
            }}
            onClick={() => handleTabClick(tab, index)}
          >
            {t(tab.head)}
          </button>
        );
      })}
    </div>
  );
};

export default FamilyCreateTabs;
