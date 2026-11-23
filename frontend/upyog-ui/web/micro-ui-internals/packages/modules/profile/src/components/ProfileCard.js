import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { EmployeeModuleCard, PersonIcon } from "@nudmcdgnpm/digit-ui-react-components";
import { APPLICATION_PATH } from "../utils";
import useInbox from "../hooks/useInbox";

/**
 * ProfileCard component displayed on the Employee Home / Dashboard page.
 * Displays KPI metrics, pending application count, and navigation links for Family Service.
 * Restricted to users holding the FAMILY_APPROVER role.
 *
 * @returns {React.ReactElement|null} Employee module card or null if unauthorized.
 */
const ProfileCard = () => {
  const { t } = useTranslation();
  const [total, setTotal] = useState("-");
  const tenantId = Digit.ULBService.getCurrentTenantId();
  const hasAccess = Boolean(Digit.UserService.hasAccess(["FAMILY_APPROVER"]));

  const { data, isLoading, isFetching, isSuccess } = useInbox({
    tenantId,
    filters: { limit: 10, offset: 0 },
    config: {
      select: (data) => {
        return {
          totalCount: data?.[0]?.totalCount ?? 0,
          nearingSlaCount: data?.[0]?.nearingSlaCount ?? 0,
        };
      },
      enabled: hasAccess,
    },
  });

  useEffect(() => {
    if (!isFetching && isSuccess) setTotal(data);
  }, [isFetching]);

  if (!hasAccess) {
    return null;
  }

  const links = [
    {
      count: isLoading ? "-" : total?.totalCount,
      label: t("ES_COMMON_INBOX"),
      link: `${APPLICATION_PATH}/employee/profile/inbox`,
    },
    {
      label: t("ES_COMMON_APPLICATION_SEARCH"),
      link: `${APPLICATION_PATH}/employee/profile/search`,
    },
  ];

  const propsForModuleCard = {
    Icon: <PersonIcon />,
    moduleCode: "FS",
    moduleName: t("ES_TITLE_FAMILY_SERVICE"),
    kpis: [
      {
        count: total?.totalCount,
        label: t("ES_TITLE_INBOX"),
        link: `${APPLICATION_PATH}/employee/profile/inbox`,
      },
    ],
    links: links,
  };

  return <EmployeeModuleCard {...propsForModuleCard} />;
};

export default ProfileCard;
