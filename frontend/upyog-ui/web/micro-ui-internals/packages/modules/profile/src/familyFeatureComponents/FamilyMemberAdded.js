import React, { useState, useEffect, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import {
  Card,
  CardHeader,
  CardLabel,
  SubmitBar,
  CheckBox,
  Toast,
  Loader,
  Table,
  DeleteIcon,
  EditIcon,
  ViewsIcon,
} from "@nudmcdgnpm/digit-ui-react-components";

import { getFamilyMembers, clearFamilyMembers, setFamilyMembers } from "../utils";
import { buildCreateFamilyPayload } from "../utils/payloads";
import { profileStyles } from "../utils/profileStyles";
import { PROFILE_ROUTES } from "../utils/constants";

/**
 * Helper to retrieve applicant's relationship with HOF chosen in CreateProfile (Step 1).
 */
const getApplicantRelationship = (uuid) => {
  try {
    const familyDetails = Digit.SessionStorage.get("Family_Details") || {};
    const rel = familyDetails[uuid]?.profileDetails?.relationshipWithHof;
    if (!rel) return "";
    return String(rel?.code || rel?.name || (typeof rel === "string" ? rel : "")).toUpperCase();
  } catch {
    return "";
  }
};

/**
 * Helper to construct the initial Member #1 (applicant) object from logged-in user info.
 * @param {Object} currentUser - User profile object from Digit.UserService.getUser()?.info
 * @param {string} [relationshipWithHead=""] - Relationship chosen in CreateProfile (e.g. SELF)
 * @returns {Object} Initial applicant member object
 */
const createInitialApplicant = (currentUser, relationshipWithHead = "") => ({
  userUuid: currentUser?.uuid,
  name: currentUser?.name || "Citizen",
  relationshipWithHead: relationshipWithHead || "SELF",
  isHead: true,
  isApplicant: true,
  mobileNumber: currentUser?.mobileNumber,
  gender: currentUser?.gender,
  aadhaarLast4: currentUser?.aadhaarNumber ? String(currentUser.aadhaarNumber).slice(-4) : undefined,
});

/**
 * FamilyMemberAdded Component
 *
 * Central review and submission screen for creating a new family group.
 *
 * Core Features:
 * 1. Member Seeding: Automatically seeds Member #1 as the logged-in applicant
 *    from `Digit.UserService.getUser()?.info` and designates them as initial Head.
 * 2. Member Table: Renders all family members in a tabular layout:
 *    - Name: Member full name, flagged with a "(You)" tag for the applicant.
 *    - Relationships: Editable Dropdown for the applicant to define their relationship to Head;
 *      read-only display for subsequently added members (assigned during verification).
 *    - Head of Family (HOF): Interactive radio button to designate any member as Head.
 *    - Actions: Red delete icon to remove added members (applicant row cannot be removed).
 * 3. Add Member Navigation: Outlined button navigating to Aadhaar verification to add more members.
 * 4. Submission & Cleanup: Compiles payload via `buildCreateFamilyPayload`, calls `createFamily`
 *    backend API, clears temporary session keys, and navigates to `FamilyCreatedResponse`.
 */
const FamilyMemberAdded = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const styles = profileStyles.familyMemberAdded;
  const stateId = Digit.ULBService.getStateId() || Digit.ULBService.getCurrentTenantId() || "pg";
  const loggedInUuid = Digit.UserService.getUser()?.info?.uuid;

  // Initialize family members list: Read from sessionStorage or seed with logged-in user
  const [members, setMembers] = useState(() => {
    const stored = getFamilyMembers();
    const applicantRel = getApplicantRelationship(loggedInUuid);

    // Validate stored members belong to currently logged-in user to prevent stale/cross-user data
    if (stored && stored.length > 0 && stored.some((m) => m.userUuid === loggedInUuid)) {
      const updated = stored.map((m) => {
        if (m.userUuid === loggedInUuid) {
          return {
            ...m,
            relationshipWithHead: applicantRel || m.relationshipWithHead || "SELF",
          };
        }
        return m;
      });
      setFamilyMembers(updated);
      return updated;
    }
    const currentUser = Digit.UserService.getUser()?.info;
    if (currentUser?.uuid) {
      const initialHead = createInitialApplicant(currentUser, applicantRel);
      setFamilyMembers([initialHead]);
      return [initialHead];
    }
    return [];
  });

  const [submitting, setSubmitting] = useState(false);
  // Declaration must be ticked before the family can be submitted
  const [agree, setAgree] = useState(false);
  const [toast, setToast] = useState(null);

  // Toast notification helper
  const showToast = (type, message, duration = 4000) => {
    setToast({ type, message });
    setTimeout(() => setToast(null), duration);
  };

  // Track the userUuid of the member currently designated as Head of Family via radio button
  const [selectedHeadUuid, setSelectedHeadUuid] = useState(() => {
    const existingHead = members.find((m) => m.isHead);
    return existingHead?.userUuid || members[0]?.userUuid || loggedInUuid || "";
  });

  // Ensure applicant is seeded on mount; if unauthenticated, redirect to profile details
  useEffect(() => {
    const currentUser = Digit.UserService.getUser()?.info;
    const applicantRel = getApplicantRelationship(loggedInUuid || currentUser?.uuid);

    if (!members.length) {
      if (currentUser?.uuid) {
        const initialHead = createInitialApplicant(currentUser, applicantRel);
        setFamilyMembers([initialHead]);
        setMembers([initialHead]);
        setSelectedHeadUuid(initialHead.userUuid);
      } else {
        navigate(PROFILE_ROUTES.DETAILS, { replace: true });
      }
    } else if (applicantRel) {
      setMembers((prev) => {
        const applicant = prev.find((m) => m.userUuid === loggedInUuid);
        if (applicant && (!applicant.relationshipWithHead || applicant.relationshipWithHead !== applicantRel)) {
          const updated = prev.map((m) => (m.userUuid === loggedInUuid ? { ...m, relationshipWithHead: applicantRel } : m));
          setFamilyMembers(updated);
          return updated;
        }
        return prev;
      });
    }
  }, [members.length, navigate, loggedInUuid]);

  /**
   * Remove a family member from the table.
   * If the removed member was currently selected as Head, default Head back to applicant.
   */
  const handleRemoveMember = (userUuidToRemove) => {
    setMembers((prev) => {
      const updated = prev.filter((m) => m.userUuid !== userUuidToRemove);
      setFamilyMembers(updated);
      if (selectedHeadUuid === userUuidToRemove) {
        setSelectedHeadUuid(loggedInUuid || updated[0]?.userUuid || "");
      }
      return updated;
    });
  };

  // View opens the Create Family steps in read-only view mode for that member
  const handleViewMember = (member, isApplicant) =>
    navigate(isApplicant ? `${PROFILE_ROUTES.PROFILE_CREATE_FAMILY}/profileDetails?mode=view` : `${PROFILE_ROUTES.PROFILE_CREATE_FAMILY}/profileDetails?member=${member.userUuid}&mode=view`);

  // Edit re-opens the Create Family steps for that member; their saved details are prefilled from the flow data
  const handleEditMember = (member, isApplicant) =>
    navigate(isApplicant ? PROFILE_ROUTES.PROFILE_CREATE_FAMILY : `${PROFILE_ROUTES.PROFILE_CREATE_FAMILY}/profileDetails?member=${member.userUuid}`);

  // Table column specifications conforming to Digit UI <Table>
  const columns = useMemo(() => [
    {
      Header: t("CORE_COMMON_NAME"),
      accessor: "name",
      Cell: ({ row }) => {
        const member = row.original;
        const isApplicant = member.userUuid === loggedInUuid;
        return (
          <div style={styles.nameWrap}>
            <span style={styles.nameText}>
              {member.name || "Family Member"}
            </span>
            {isApplicant && (
              <span style={styles.youBadge}>
                ({t("CORE_COMMON_YOU", "You")})
              </span>
            )}
          </div>
        );
      },
    },
    {
      Header: t("PROFILE_RELATIONSHIP_WITH_HOF", "Relationship with HOF"),
      accessor: "relationshipWithHead",
      Cell: ({ row }) => (
        <span style={styles.relationshipText}>
          {row.original.relationshipWithHead || "-"}
        </span>
      ),
    },
    {
      Header: t("HEAD_OF_FAMILY_COL"),
      accessor: "isHead",
      Cell: ({ row }) => {
        const member = row.original;
        const isSelected = selectedHeadUuid === member.userUuid;
        return (
          <div
            style={styles.radioWrap}
            onClick={() => setSelectedHeadUuid(member.userUuid)}
          >
            <input
              type="radio"
              name="hof_selection"
              checked={isSelected}
              onChange={() => setSelectedHeadUuid(member.userUuid)}
              style={styles.radioInput}
            />
          </div>
        );
      },
    },
    {
      Header: t("ACTIONS"),
      accessor: "actions",
      Cell: ({ row }) => {
        const member = row.original;
        const isApplicant = member.userUuid === loggedInUuid;

        return (
          <div style={styles.actionsWrap}>
            <button type="button" style={styles.deleteBtn} title={t("CORE_COMMON_VIEW")} onClick={() => handleViewMember(member, isApplicant)}>
              <ViewsIcon />
            </button>
            {/* Applicant edits the profile steps; added members only have their relationship editable */}
            <button type="button" style={styles.deleteBtn} title={t("CORE_COMMON_EDIT")} onClick={() => handleEditMember(member, isApplicant)}>
              <EditIcon />
            </button>
            {/* If there is only one row, the Delete button should not be displayed */}
            {members.length > 1 && (
              <button type="button" style={styles.deleteBtn} title={t("REMOVE")} disabled={isApplicant} onClick={() => handleRemoveMember(member.userUuid)}>
                <DeleteIcon fill={isApplicant ? "#9CA3AF" : styles.deleteIconFill} style={styles.deleteIcon} />
              </button>
            )}
          </div>
        );
      },
    },
  ], [members, selectedHeadUuid, loggedInUuid, t, styles]);

  // Navigate to Aadhaar verification to add another family member
  const handleAddAnother = () => {
    navigate(PROFILE_ROUTES.ADD_FAMILY_MEMBER);
  };

  /**
   * Final Submission Handler:
   * 1. Validates that the family has at least 2 members.
   * 2. Validates that non-head members have assigned relationships.
   * 3. Formats payload using validated React `members` state.
   * 4. Dispatches createFamily API call.
   * 5. Handles missing familyId explicitly before navigating.
   * 6. Clears temporary session storage.
   * 7. Navigates to FamilyCreatedResponse.
   */
  const handleCreateFamilyId = async () => {
    if (submitting || !selectedHeadUuid) return;

    // 1. Client-side validation: Family must have at least 2 members
    if (members.length < 2) {
      showToast("error", t("ERR_FAMILY_MIN_MEMBERS", "Please add at least one family member before creating Family ID."));
      return;
    }

    // 2. Validation: Ensure non-head members have an assigned relationship
    const unassignedMember = members.find((m) => m.userUuid !== selectedHeadUuid && !m.relationshipWithHead);
    if (unassignedMember) {
      showToast("error", t("ERR_SELECT_RELATIONSHIP_FOR_ALL"));
      return;
    }

    setSubmitting(true);

    try {
      const tenantId = Digit.ULBService.getCurrentTenantId();
      const familyDetails = Digit.SessionStorage.get("Family_Details") || {};

      // 3. Use validated `members` React state (avoids state vs. storage divergence)
      const payload = buildCreateFamilyPayload({
        membersList: members,
        tenantId: tenantId,
        selectedHeadUuid: selectedHeadUuid,
        familyDetails
      });

      // API Call: Create Family
      const res = await Digit.UserService.createFamily(payload, tenantId);
      const createdFamilyId = res?.Families?.[0]?.familyId;

      // 4. Handle missing familyId explicitly
      if (!createdFamilyId) {
        throw new Error(
          t("ERR_FAMILY_ID_NOT_GENERATED", "Family created, but failed to retrieve generated Family ID. Please check profile details.")
        );
      }

      // Clear cached family data and temporary verification tokens from session
      clearFamilyMembers();
      Digit.SessionStorage.del("Family_Details"); // per-member step data of the Create Family flow
      try {
        sessionStorage.removeItem("Digit.family.verifiedMember");
        sessionStorage.removeItem("Digit.family.aadhaarLast4");
        sessionStorage.removeItem("Digit.family.cleanAadhaar");
        sessionStorage.removeItem("Digit.family.maskedMobile");
      } catch (storageErr) {
        console.error("Error clearing temporary session keys:", storageErr);
      }

      // Navigate to success response screen
      navigate(PROFILE_ROUTES.FAMILY_CREATED, {
        replace: true,
        state: { familyId: createdFamilyId },
      });
    } catch (err) {
      // 5. Fallback string is now localized via t(...)
      showToast(
        "error",
        err?.response?.data?.Errors?.[0]?.message || err?.message || t("ERR_FAILED_TO_CREATE_FAMILY_ID", "Failed to create Family ID.")
      );
    } finally {
      setSubmitting(false);
    }
  };

  if (!members.length) return <Loader />;

  return (
    <React.Fragment>
      <Card>
        {/* Card Header & Section Subtitle */}
        <CardHeader>{t("FAMILY_MEMBER_ADDED_TITLE")}</CardHeader>
        <CardLabel style={styles.headLabel}>
          {t("SELECT_HEAD_OF_FAMILY")}
        </CardLabel>

        {/* Member Table */}
        <div style={styles.tableWrapper}>
          <Table
            t={t}
            data={members}
            columns={columns}
            isPaginationRequired={false}
            totalRecords={members.length}
            getCellProps={() => ({
              style: styles.tableCell,
            })}
          />
        </div>

        {/* Add Another Member Action Button */}
        <div style={styles.addAnotherSection}>
          <button
            type="button"
            onClick={handleAddAnother}
            style={styles.addBtn}
            onMouseEnter={(e) => {
              e.currentTarget.style.backgroundColor = styles.addBtnHoverBg;
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.backgroundColor = styles.addBtn.background;
            }}
          >
            <span style={styles.addBtnIcon}>+</span>
            <span>{t("ADD_ANOTHER_MEMBER_BTN")}</span>
          </button>
        </div>

        {/* Final Submit Bar */}
        <CheckBox
          label={t("FAMILY_FINAL_DECLARATION_MESSAGE")}
          onChange={(e) => setAgree(e.target.checked)}
          checked={agree}
          styles={{ height: "auto" }}
        />
        <SubmitBar
          label={t("CREATE_FAMILY_ID_BTN")}
          onSubmit={handleCreateFamilyId}
          disabled={submitting || !selectedHeadUuid || !agree}
        />
        {submitting && <Loader />}
      </Card>

      {/* Toast Notification Container */}
      {toast && (
        <div className="profile-toast-container">
          <Toast error={toast.type === "error"} label={toast.message} onClose={() => setToast(null)} />
        </div>
      )}
    </React.Fragment>
  );
};

export default FamilyMemberAdded;
