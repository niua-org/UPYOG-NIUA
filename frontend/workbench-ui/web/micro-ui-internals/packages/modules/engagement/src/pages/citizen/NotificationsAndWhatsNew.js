import { Card, CardCaption, Header, Loader, OnGroundEventCard, WhatsNewCard } from "@upyog/workbench-ui-react-components";
import React, { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { Navigate, useLocation } from "react-router-dom";
import BroadcastWhatsNewCard from "../../components/Messages/BroadcastWhatsNewCard";

const NotificationsAndWhatsNew = ({ variant, parentRoute }) => {
  const { t } = useTranslation();
  const location = useLocation();
  const navigate = Digit.Hooks.useCustomNavigate();

  const tenantId = Digit.ULBService.getCitizenCurrentTenant();
  const {
    data: { unreadCount: preVisitUnseenNotificationCount } = {},
    isSuccess: preVisitUnseenNotificationCountLoaded,
    refetch,
  } = Digit.Hooks.useNotificationCount({
    tenantId,
    config: {
      enabled: !!Digit.UserService?.getUser()?.access_token,
    },
  });

  const { mutate, isSuccess } = Digit.Hooks.useClearNotifications();

  useEffect(() => {
    isSuccess ? refetch() : false;
  }, [isSuccess]);

  useEffect(() => (preVisitUnseenNotificationCount && tenantId ? mutate({ tenantId }) : null), [tenantId, preVisitUnseenNotificationCount]);

  const { data: EventsData, isLoading: EventsDataLoading } = Digit.Hooks.useEvents({ tenantId, variant });

  // if (!Digit.UserService?.getUser()?.access_token) {
  //     return <Navigate to={{ pathname: `/${window?.contextPath}/citizen/login`, state: { from: location.pathname + location.search } }} />
  // }

  if (EventsDataLoading) return <Loader />;

  if (EventsData?.length === 0) {
    return (
      <div className="CitizenEngagementNotificationWrapper">
        <Header>{`${t("CS_HEADER_NOTIFICATIONS")}`}</Header>
        <h1>Nothing to show</h1>
      </div>
    );
  }

  const VariantWiseRender = () => {
    switch (variant) {
      case "notifications":
        return <Header>{`${t("CS_HEADER_NOTIFICATIONS")} ${preVisitUnseenNotificationCount ? `(${preVisitUnseenNotificationCount})` : ""}`}</Header>;

      case "whats-new":
        return <Header>{t("CS_HEADER_WHATSNEW")}</Header>;

      default:
        return <Navigate to={{ pathname: `/${window?.contextPath}/citizen`, state: { from: location.pathname + location.search } }} />;
    }
  };

  function onEventCardClick(id) {
    navigate(parentRoute + "/events/details/" + id);
  }

  return (
    <div className="CitizenEngagementNotificationWrapper">
      <VariantWiseRender />
      {EventsData?.length ? (
        EventsData.map((DataParamsInEvent) =>
          DataParamsInEvent?.eventType === "EVENTSONGROUND" ? (
            <OnGroundEventCard onClick={onEventCardClick} {...DataParamsInEvent} />
          ) : DataParamsInEvent?.eventType === "BROADCAST" ? (
            <BroadcastWhatsNewCard {...DataParamsInEvent} />
          ) : (
            <WhatsNewCard {...DataParamsInEvent} />
          )
        )
      ) : (
        <Card>
          <CardCaption>{t("COMMON_INBOX_NO_DATA")}</CardCaption>
        </Card>
      )}
    </div>
  );
};

export default NotificationsAndWhatsNew;
