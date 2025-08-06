import { ArrowRightInbox, ShippingTruck, EmployeeModuleCard, Loader } from "@upyog/digit-ui-react-components";
import React, { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useHistory } from "react-router-dom";
import { checkForEmployee } from "../utils";

const ROLES = {
  plant: ["PQM_TP_OPERATOR"],
  ulb: ["PQM_ADMIN"],
};

const TqmCard = ({ reRoute = true }) => {
  const history = useHistory();
  const isMobile = Digit.Utils.browser.isMobile();
  const isPlantOperatorLoggedIn = Digit.Utils.isPlantOperatorLoggedIn();
  const { t } = useTranslation();
  const tenantId = Digit.ULBService.getCurrentTenantId();

  if (!Digit.Utils.tqmAccess()) {
    return null;
  }

  const userInfo = Digit.UserService.getUser();
  const userRoles = userInfo.info.roles.map((roleData) => roleData.code);

  const requestCriteriaPlantUsers = {
    params: {},
    url: "/pqm-service/plant/user/v1/_search",
    body: {
      plantUserSearchCriteria: {
        tenantId,
        plantUserUuids: userInfo?.info?.uuid ? [userInfo?.info?.uuid] : [],
        additionalDetails: {},
      },
      pagination: {},
    },
    config: {
      select: (data) => {
        let userPlants = data?.plantUsers
          ?.map((row) => {
            row.i18nKey = `PQM_PLANT_${row?.plantCode}`;
            return row;
          })
          ?.filter((row) => row.isActive);
        Digit.SessionStorage.set("user_plants", userPlants);
        return userPlants;
      },
    },
  };

  const { isLoading: isLoadingPlantUsers, data: dataPlantUsers } = Digit.Hooks.useCustomAPIHook(requestCriteriaPlantUsers);

  const requestCriteria = {
    url: "/inbox/v2/_search",
    body: {
      inbox: {
        tenantId,
        processSearchCriteria: {
          businessService: ["PQM"],
          moduleName: "pqm",
          tenantId,
        },
        moduleSearchCriteria: {
          tenantId,
        },
        limit: 100,
        offset: 0,
      },
    },
    config: {
      enabled:
        dataPlantUsers?.length > 0
          ? Digit.Utils.didEmployeeHasAtleastOneRole(ROLES.plant) || Digit.Utils.didEmployeeHasAtleastOneRole(ROLES.ulb)
          : false,
    },
  };

  const activePlantCode = Digit.SessionStorage.get("active_plant")?.plantCode
    ? [Digit.SessionStorage.get("active_plant")?.plantCode]
    : Digit.SessionStorage.get("user_plants")
        ?.filter((row) => row.plantCode)
        ?.map((row) => row.plantCode);

  if (activePlantCode?.length > 0) {
    requestCriteria.body.inbox.moduleSearchCriteria.plantCodes = [...activePlantCode];
  }

  const { isLoading, data: tqmInboxData } = Digit.Hooks.useCustomAPIHook(requestCriteria);

  const getLandingUrl = () => {
    if (userRoles.includes("PQM_ADMIN")) {
      return "/tqm-ui/employee";
    }
    return "/tqm-ui/employee/tqm/landing";
  };

  const handleNavigation = (e) => {
    if (e) e.preventDefault();
    const url = getLandingUrl();
    if (url) {
      window.location.href = url;
    }
  };

  let links = [
    {
      label: t("TQM_MONITOR"),
      link: getLandingUrl(),
      onClick: handleNavigation,
    },
  ];

  const propsForModuleCard = {
    Icon: <ShippingTruck />,
    moduleName: t("ACTION_TEST_TQM"),
    links: links,
    onCardClick: handleNavigation,
  };

  if (isPlantOperatorLoggedIn) {
    delete propsForModuleCard.kpis;
    delete propsForModuleCard.links[2];
  }

  // Handle single role automatic redirect
  useEffect(() => {
    if (reRoute && userRoles.length === 1) {
      const role = userRoles[0];
      let redirectUrl;
      switch (role) {
        case "PQM_TP_OPERATOR":
          redirectUrl = "/tqm-ui/employee/tqm/landing";
          break;
        case "PQM_ADMIN":
          redirectUrl = "/tqm-ui/employee";
          break;
      }
      if (redirectUrl) {
        window.location.href = redirectUrl;
      }
    }
  }, [reRoute, userRoles, history]);

  if (isLoading) {
    return <Loader />;
  }

  return (
    <div onClick={handleNavigation}>
      <EmployeeModuleCard {...propsForModuleCard} TqmEnableUrl={true} />
    </div>
  );
};

export default TqmCard;
