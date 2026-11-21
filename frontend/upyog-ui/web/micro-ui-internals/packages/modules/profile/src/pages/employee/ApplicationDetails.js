import React, { useEffect, useState, useRef } from "react";
import { useTranslation } from "react-i18next";
import { useParams, useNavigate } from "react-router-dom";
import { Header } from "@nudmcdgnpm/digit-ui-react-components";
import ApplicationDetailsTemplate from "../../../../templates/ApplicationDetails";

/**
 * ApplicationDetails component for the Family Service application details page.
 * Loads comprehensive family records, head-of-family information, and individual member cards.
 * Integrates directly with the UPYOG workflow engine for APPROVE and REJECT actions.
 *
 * @param {Object} props - Component properties.
 * @param {string} props.parentRoute - Base route path for navigation and return routes.
 * @returns {React.ReactElement} Application details view.
 */
const ApplicationDetails = ({ parentRoute }) => {
  const { t } = useTranslation();
  const { id: familyId } = useParams();
  const navigate = useNavigate();
  const tenantId = Digit.ULBService.getCurrentTenantId();

  const [isLoading, setIsLoading] = useState(true);
  const [familyData, setFamilyData] = useState(null);
  const [appDetailsToShow, setAppDetailsToShow] = useState({});
  const [showToast, setShowToast] = useState(null);
  const navTimerRef = useRef(null);

  useEffect(() => {
    return () => {
      if (navTimerRef.current) {
        clearTimeout(navTimerRef.current);
      }
    };
  }, []);

  /**
   * Formats a DOB value into DD/MM/YYYY for presentation.
   *
   * @param {number|string} dob - Timestamp epoch or formatted date string.
   * @returns {string} Formatted date string or NA fallback.
   */
  const formatDob = (dob) => {
    if (!dob) return t("CS_NA");
    if (typeof dob === "number") {
      return Digit.DateUtils?.ConvertEpochToDate?.(dob) || t("CS_NA");
    }
    return dob;
  };

  /**
   * Builds the formatted key-value attribute array for a family member or head of family.
   * Shows data directly when present, otherwise falls back to NA.
   *
   * @param {Object} member - Family member object from backend response.
   * @returns {Array<Object>} Array of title and value pairs.
   */
  const buildMemberValues = (member) => {
    const citizen = member?.citizenDetails;
    return [
      { title: "CORE_COMMON_NAME", value: member?.name || t("CS_NA") },
      { title: "CORE_COMMON_MOBILE_NUMBER", value: member?.mobileNumber || t("CS_NA") },
      { title: "CORE_COMMON_PROFILE_ALT_MOBILE_NUMBER", value: citizen?.alternateMobileNumber || t("CS_NA") },
      { title: "CORE_COMMON_PROFILE_EMAIL", value: citizen?.emailId || t("CS_NA") },
      { title: "CORE_COMMON_GENDER", value: member?.gender ? t(member.gender) : t("CS_NA") },
      { title: "CORE_COMMON_PROFILE_DOB", value: formatDob(member?.dob || citizen?.dob) },
      { title: "CORE_COMMON_FATHER_NAME", value: citizen?.fatherName || t("CS_NA") },
      { title: "CORE_COMMON_MOTHER_NAME", value: citizen?.motherName || t("CS_NA") },
      { title: "CORE_COMMON_SPOUSENAME", value: citizen?.spouseName || t("CS_NA") },
      { title: "RELATIONSHIP_WITH_HEAD", value: member?.relationshipWithHead ? t(member.relationshipWithHead) : t("CS_NA") },
      { title: "PROFILE_CASTE", value: citizen?.caste || t("CS_NA") },
      { title: "PROFILE_SPECIALLY_ABLED", value: citizen?.isSpeciallyAbled ? t("YES") : t("NO") },
      { title: "CORE_COMMON_STATUS", value: member?.active ? t("ACTIVE") : t("INACTIVE") },
    ];
  };

  /**
   * Fetches the family details using Digit.UserService.familySearch and formats
   * them into sections expected by ApplicationDetailsTemplate.
   */
  const fetchFamilyDetails = async () => {
    try {
      setIsLoading(true);

      const response = await Digit.UserService.familySearch(tenantId, { familyId });
      const found = response?.Families?.[0];

      setFamilyData(found);

      if (found) {
        const headMember = found?.members?.find((m) => m.isHead);

        const memberSections = (found?.members || []).map((m, idx) => ({
          title: `${t("MEMBER")} ${idx + 1}`,
          asSectionHeader: true,
          values: buildMemberValues(m),
        }));

        const formatted = {
          applicationNo: found.applicationNo,
          applicationData: {
            ...found,
            applicationNumber: found.applicationNo,
          },
          applicationDetails: [
            {
              title: "ES_FAMILY_DETAILS",
              asSectionHeader: true,
              values: [
                { title: "ES_INBOX_APPLICATION_NO", value: found.applicationNo },
                { title: "CORE_COMMON_FAMILY_ID", value: found.familyId },
                {
                  title: "CS_INBOX_STATUS_FILTER",
                  value: t(found.applicationStatus),
                },
              ],
            },
            {
              title: "ES_HEAD_OF_FAMILY_DETAILS",
              asSectionHeader: true,
              values: buildMemberValues(headMember),
            },
            ...memberSections,
          ],
        };

        setAppDetailsToShow(formatted);
      }
    } catch (err) {
      setShowToast({
        key: "error",
        error: { message: t("ES_FAMILY_FETCH_FAILED") },
      });
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchFamilyDetails();
  }, [familyId, tenantId]);

  /**
   * Hook retrieving workflow transition actions and history for the family-service business service.
   */
  const workflowDetails = Digit.Hooks.useWorkflowDetails({
    tenantId: familyData?.tenantId || tenantId,
    id: familyId,
    moduleCode: "family-service",
    role: "FAMILY_APPROVER",
  });

  /**
   * Action submission callback invoked by ApplicationDetailsTemplate when the employee
   * confirms an APPROVE or REJECT workflow action in FamilyActionModal.
   *
   * @param {Object} formData - Update payload containing Family and workflow action.
   * @param {Object} callbacks - Success and error callback handlers.
   */
  const mutate = async (formData, callbacks) => {
    try {
      const actionSelected = formData?.Family?.workflow?.action;
      if (!actionSelected) {
        throw new Error(t("ES_FAMILY_INVALID_ACTION"));
      }

      await Digit.UserService.updateFamily(formData, {});

      setShowToast({
        key: "success",
        label:
          actionSelected === "APPROVE"
            ? t("ES_FAMILY_APPROVE_SUCCESS")
            : t("ES_FAMILY_REJECT_SUCCESS"),
      });

      if (callbacks?.onSuccess) {
        callbacks.onSuccess(formData);
      }

      if (workflowDetails?.revalidate) {
        workflowDetails.revalidate();
      }

      navTimerRef.current = setTimeout(() => {
        navigate(`${parentRoute}/inbox`);
      }, 1500);
    } catch (e) {
      if (callbacks?.onError) {
        callbacks.onError(e);
      }
      const backendError =
        e?.response?.data?.Errors?.[0]?.message ||
        e?.message ||
        t("ES_FAMILY_ACTION_FAILED");
      setShowToast({
        key: "error",
        error: { message: backendError },
      });
    }
  };

  /**
   * Closes active toast notification.
   */
  const closeToast = () => {
    setShowToast(null);
  };

  return (
    <div>
      <div className="employee-application-details family-details-header">
        <Header className="family-header-title">
          {t("FAMILY_APPLICATION_DETAILS")}
        </Header>
      </div>

      <ApplicationDetailsTemplate
        applicationDetails={appDetailsToShow}
        isLoading={isLoading}
        isDataLoading={isLoading}
        applicationData={appDetailsToShow?.applicationData}
        mutate={mutate}
        workflowDetails={workflowDetails}
        businessService="family-service"
        moduleCode="family-service"
        showToast={showToast}
        setShowToast={setShowToast}
        closeToast={closeToast}
        timelineStatusPrefix="FAMILY-SERVICE_COMMON_STATUS_"
        forcedActionPrefix="WF_FAMILY"
        statusAttribute="status"
      />
    </div>
  );
};

export default React.memo(ApplicationDetails);
