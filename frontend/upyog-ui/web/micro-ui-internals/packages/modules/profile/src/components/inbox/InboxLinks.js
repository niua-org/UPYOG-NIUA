import React from "react";
import { Card, CHBIcon } from "@nudmcdgnpm/digit-ui-react-components";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { APPLICATION_PATH } from "../../utils";

/**
 * InboxLinks component rendered at the top of the Family Service Inbox sidebar.
 * Displays the module branding logo, title, and quick navigation links.
 * All styles are loaded from family.scss classes without inline styles.
 *
 * @returns {React.ReactElement} Sidebar header links card.
 */
const InboxLinks = () => {
  const { t } = useTranslation();

  /**
   * Renders the branded module logo and title section.
   *
   * @returns {React.ReactElement} Header logo element.
   */
  const GetLogo = () => (
    <div className="header">
      <span className="logo">
        <CHBIcon />
      </span>{" "}
      <span className="text">{t("ES_TITLE_FAMILY_SERVICE")}</span>
    </div>
  );

  return (
    <Card className="employeeCard filter inboxLinks">
      <div className="complaint-links-container">
        {GetLogo()}
        <div className="body">
          <span className="link">
            <Link to={`${APPLICATION_PATH}/employee/profile/search`}>{t("ES_COMMON_APPLICATION_SEARCH")}</Link>
          </span>
        </div>
      </div>
    </Card>
  );
};

export default InboxLinks;
