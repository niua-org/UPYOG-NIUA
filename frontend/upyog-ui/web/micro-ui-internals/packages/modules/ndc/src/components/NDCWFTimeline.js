import React, { Fragment, useState, useMemo } from "react";
import { useTranslation } from "react-i18next";
import {
  Card,
  CardSectionHeader,
  CheckPoint,
  ConnectingCheckPoints,
  Loader,
  LinkButton,
  TelePhone,
  DisplayPhotos,
} from "@nudmcdgnpm/digit-ui-react-components";

const NDCWFCaption = ({ data, OpenImage, t }) => {
  return (
    <div style={{ marginTop: "6px" }}>
      {data?.date && (
        <p style={{ color: "#505A5F", fontSize: "14px", margin: "2px 0" }}>
          {data.date}
        </p>
      )}
      {data?.name && (
        <p style={{ fontWeight: 600, fontSize: "14px", color: "#0B0C0C", margin: "2px 0" }}>
          {data.name}
          {data?.designation ? (
            <span style={{ fontWeight: 400, color: "#505A5F", marginLeft: "6px" }}>
              ({data.designation})
            </span>
          ) : null}
        </p>
      )}
      {data?.mobileNumber && (
        <div style={{ margin: "2px 0" }}>
          <TelePhone mobile={data.mobileNumber} />
        </div>
      )}
      {data?.source && (
        <p style={{ color: "#505A5F", fontSize: "14px", margin: "2px 0" }}>
          {t("ES_APPLICATION_DETAILS_APPLICATION_CHANNEL_" + String(data.source).toUpperCase())}
        </p>
      )}
      {data?.comment && (
        <div className="checkpoint-comments-wrap" style={{ marginTop: "6px" }}>
          <h4 style={{ fontWeight: 600, fontSize: "14px", color: "#0B0C0C", margin: "0 0 2px 0" }}>
            {t("WF_COMMON_COMMENTS")}
          </h4>
          <p style={{ color: "#505A5F", fontSize: "14px", margin: 0, whiteSpace: "pre-wrap" }}>
            {data.comment}
          </p>
        </div>
      )}
      {Array.isArray(data?.wfComment) && data.wfComment.length > 0 && (
        <div style={{ marginTop: "6px" }}>
          <h4 style={{ fontWeight: 600, fontSize: "14px", color: "#0B0C0C", margin: "0 0 2px 0" }}>
            {t("WF_COMMON_COMMENTS")}
          </h4>
          {data.wfComment.map((c, i) => (
            <p key={i} style={{ color: "#505A5F", fontSize: "14px", margin: "2px 0", whiteSpace: "pre-wrap" }}>
              {c}
            </p>
          ))}
        </div>
      )}
      {data?.thumbnailsToShow?.thumbs?.length > 0 && (
        <div className="TLComments" style={{ marginTop: "8px" }}>
          <h4 style={{ fontWeight: 600, fontSize: "14px", color: "#0B0C0C", margin: "0 0 4px 0" }}>
            {t("CS_COMMON_ATTACHMENTS")}
          </h4>
          <DisplayPhotos
            srcs={data.thumbnailsToShow.thumbs}
            onClick={(src, idx) => OpenImage(src, idx, data.thumbnailsToShow)}
          />
        </div>
      )}
    </div>
  );
};

const NDCWFTimeline = ({
  workflowDetails: propWorkflowDetails,
  application,
  id,
  tenantId: propTenantId,
  t: propT,
}) => {
  const { t: hookT } = useTranslation();
  const t = propT || hookT;

  const tenantId =
    propTenantId ||
    application?.tenantId ||
    Digit.ULBService.getCurrentTenantId();

  const businessId = id || application?.applicationNo;

  const fetchedWorkflow = Digit.Hooks.useWorkflowDetails({
    tenantId,
    id: businessId,
    moduleCode: "ndc-services",
    config: {
      enabled: !propWorkflowDetails && !!(tenantId && businessId),
    },
  });

  const workflowDetails = propWorkflowDetails || fetchedWorkflow;
  const isLoading = workflowDetails?.isLoading;
  const rawTimeline = workflowDetails?.data?.timeline || [];

  const [showAllTimeline, setShowAllTimeline] = useState(false);

  // Filter out SAVE_AS_DRAFT if present
  const timeline = useMemo(() => {
    if (!Array.isArray(rawTimeline)) return [];
    return rawTimeline.filter((item) => item?.performedAction !== "SAVE_AS_DRAFT");
  }, [rawTimeline]);

  // Fetch employee designations
  const userCodes = useMemo(() => {
    const codes = timeline
      .map((item) => item?.assigner?.userName || item?.assignes?.[0]?.userName)
      .filter(Boolean);
    return [...new Set(codes)].join(",");
  }, [timeline]);

  const { data: employeeData } = Digit.Hooks.useEmployeeSearch(
    tenantId,
    { codes: userCodes, isActive: true },
    { enabled: !!userCodes }
  );

  const designationMap = useMemo(() => {
    const map = {};
    if (employeeData?.Employees) {
      employeeData.Employees.forEach((emp) => {
        const assignment = emp?.assignments?.[0];
        if (assignment?.designation) {
          map[emp?.code] = `COMMON_MASTERS_DESIGNATION_${assignment.designation}`;
        }
      });
    }
    return map;
  }, [employeeData]);

  const OpenImage = (imageSource, index, thumbnailsToShow) => {
    const url =
      thumbnailsToShow?.images?.[index] ||
      thumbnailsToShow?.fullImage?.[index] ||
      imageSource;
    if (url) {
      window.open(url, "_blank");
    }
  };

  const getCheckpointLabel = (checkpoint) => {
    const stateCode = checkpoint?.state || checkpoint?.status;
    const action = checkpoint?.performedAction;

    if (action === "REOPEN") {
      return t("REOPEN");
    }

    const candidateKeys = [
      `WF_NDC-SERVICES_${stateCode}`,
      `WF_ndc-services_${stateCode}`,
      `WF_NDC_${stateCode}`,
      `WF_${stateCode}`,
      stateCode,
    ];

    for (const key of candidateKeys) {
      if (key && t(key) !== key) {
        return t(key);
      }
    }

    return stateCode || t("CS_COMMON_NA");
  };

  const getTimelineCaptions = (checkpoint) => {
    const userName =
      checkpoint?.assigner?.userName || checkpoint?.assignes?.[0]?.userName;
    const designationKey = designationMap[userName];
    const designation = designationKey ? t(designationKey) : null;

    const captionData = {
      date:
        checkpoint?.auditDetails?.lastModified ||
        checkpoint?.auditDetails?.created ||
        (application?.auditDetails?.lastModifiedTime
          ? Digit.DateUtils.ConvertEpochToDate(application.auditDetails.lastModifiedTime)
          : null),
      name: checkpoint?.assigner?.name || checkpoint?.assignes?.[0]?.name,
      designation,
      mobileNumber:
        checkpoint?.assigner?.mobileNumber ||
        checkpoint?.assignes?.[0]?.mobileNumber,
      source: application?.channel || null,
      comment: checkpoint?.comment,
      wfComment: checkpoint?.wfComment,
      thumbnailsToShow: checkpoint?.thumbnailsToShow,
    };

    return <NDCWFCaption data={captionData} OpenImage={OpenImage} t={t} />;
  };

  if (isLoading) {
    return <Loader />;
  }

  if (!timeline || timeline.length === 0) {
    return null;
  }

  const displayedTimeline = showAllTimeline ? timeline : timeline.slice(0, 2);

  return (
    <Card style={{ marginTop: "16px" }}>
      <CardSectionHeader style={{ marginBottom: "16px" }}>
        {t("CS_APPLICATION_DETAILS_APPLICATION_TIMELINE")}
      </CardSectionHeader>

      {timeline.length === 1 ? (
        <CheckPoint
          isCompleted={true}
          label={getCheckpointLabel(timeline[0])}
          customChild={getTimelineCaptions(timeline[0])}
        />
      ) : (
        <ConnectingCheckPoints>
          {displayedTimeline.map((checkpoint, index) => (
            <CheckPoint
              key={checkpoint?.id || checkpoint?.performedAction || index}
              keyValue={index}
              isCompleted={index === 0}
              label={getCheckpointLabel(checkpoint)}
              customChild={getTimelineCaptions(checkpoint)}
            />
          ))}
        </ConnectingCheckPoints>
      )}

      {timeline.length > 2 && (
        <div style={{ marginTop: "16px" }}>
          <LinkButton
            label={showAllTimeline ? t("COLLAPSE") : t("VIEW_TIMELINE")}
            onClick={() => setShowAllTimeline((prev) => !prev)}
          />
        </div>
      )}
    </Card>
  );
};

export default NDCWFTimeline;
