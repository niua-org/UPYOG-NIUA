import React, { useState, useEffect, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import {
  Card,
  CardHeader,
  CardLabel,
  SubmitBar,
  Toast,
  Loader,
  Table,
  Dropdown,
  DeleteIcon,
  EditIcon,
  ViewsIcon,
  Modal,
} from "@nudmcdgnpm/digit-ui-react-components";

import { getFamilyMembers, clearFamilyMembers, setFamilyMembers, FieldGrid, withValues } from "../utils";
import { buildCreateFamilyPayload } from "../utils/payloads";
import { profileStyles } from "../utils/profileStyles";
import { PROFILE_ROUTES } from "../utils/constants";

/**
 * Helper to construct the initial Member #1 (applicant) object from logged-in user info.
 * @param {Object} currentUser - User profile object from Digit.UserService.getUser()?.info
 * @returns {Object} Initial applicant member object
 */
const createInitialApplicant = (currentUser) => ({
  userUuid: currentUser?.uuid,
  name: currentUser?.name || "Citizen",
  relationshipWithHead: "",
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

  // MDMS Hook: Fetch active relationship master data from common-masters.FamilyRelationship
  const { data: FamilyRelationShips } = Digit.Hooks.useEnabledMDMS(
    stateId,
    "common-masters",
    [{ name: "FamilyRelationship" }],
    {
      select: (data) => {
        const formattedData = data?.["common-masters"]?.["FamilyRelationship"];
        return formattedData?.filter((option) => option.active !== false) || [];
      },
    }
  );

  // Memoize formatted relationship dropdown options from MDMS
  const familyRelationShips = useMemo(() => {
    if (Array.isArray(FamilyRelationShips) && FamilyRelationShips.length > 0) {
      return FamilyRelationShips.map((relation) => ({
        code: relation.code,
        name: relation.name || relation.code,
        i18nKey: `${relation.code}`,
      }));
    }
    return [];
  }, [FamilyRelationShips]);

  // Initialize family members list: Read from sessionStorage or seed with logged-in user
  const [members, setMembers] = useState(() => {
    const stored = getFamilyMembers();
    // Validate stored members belong to currently logged-in user to prevent stale/cross-user data
    if (stored && stored.length > 0 && stored.some((m) => m.userUuid === loggedInUuid)) {
      return stored;
    }
    const currentUser = Digit.UserService.getUser()?.info;
    if (currentUser?.uuid) {
      const initialHead = createInitialApplicant(currentUser);
      setFamilyMembers([initialHead]);
      return [initialHead];
    }
    return [];
  });

  const [submitting, setSubmitting] = useState(false);
  const [toast, setToast] = useState(null);
  // Row action dialog: { mode: "view" | "edit", member }
  const [actionModal, setActionModal] = useState(null);

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
    if (!members.length) {
      const currentUser = Digit.UserService.getUser()?.info;
      if (currentUser?.uuid) {
        const initialHead = createInitialApplicant(currentUser);
        setFamilyMembers([initialHead]);
        setMembers([initialHead]);
        setSelectedHeadUuid(initialHead.userUuid);
      } else {
        navigate(PROFILE_ROUTES.DETAILS, { replace: true });
      }
    }
  }, [members.length, navigate, loggedInUuid]);

  /**
   * Handle relationship selection changes in the table (specifically Member #1).
   * Updates state and syncs to sessionStorage.
   */
  const handleRelationshipChange = (userUuid, selectedOption) => {
    const code = (selectedOption?.code || selectedOption?.rawName || "").toUpperCase();
    setMembers((prev) => {
      const updated = prev.map((m) =>
        m.userUuid === userUuid ? { ...m, relationshipWithHead: code } : m
      );
      setFamilyMembers(updated);
      return updated;
    });
  };

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

  // Edit: applicant re-opens the Create Family profile step; other members edit their relationship in a dialog
  const handleEditMember = (member, isApplicant) =>
    isApplicant ? navigate(PROFILE_ROUTES.PROFILE_CREATE_FAMILY) : setActionModal({ mode: "edit", member });

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
      Header: t("CORE_COMMON_RELATIONSHIP"),
      accessor: "relationshipWithHead",
      Cell: ({ row }) => {
        const member = row.original;
        const isApplicant = member.userUuid === loggedInUuid;

        // Applicant (Member #1) selects relationship via Dropdown
        if (isApplicant) {
          const selectedRelOption = familyRelationShips.find(
            (r) => r.code === member.relationshipWithHead
          ) || null;

          return (
            <div style={styles.dropdownWrap}>
              <Dropdown
                t={t}
                option={familyRelationShips}
                optionKey="name"
                selected={selectedRelOption}
                select={(val) => handleRelationshipChange(member.userUuid, val)}
                placeholder={t("SELECT_RELATIONSHIP")}
                optionCardStyles={styles.dropdownCard}
                style={styles.dropdown}
              />
            </div>
          );
        }

        // Added members display relationship selected during their verification step
        return (
          <span style={styles.relationshipText}>
            {member.relationshipWithHead || "-"}
          </span>
        );
      },
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
            <button type="button" style={styles.deleteBtn} title={t("CORE_COMMON_VIEW")} onClick={() => setActionModal({ mode: "view", member })}>
              <ViewsIcon />
            </button>
            {/* Applicant edits the profile steps; added members only have their relationship editable */}
            <button type="button" style={styles.deleteBtn} title={t("CORE_COMMON_EDIT")} onClick={() => handleEditMember(member, isApplicant)}>
              <EditIcon />
            </button>
            {/* Applicant (Member #1) cannot be removed */}
            <button type="button" style={styles.deleteBtn} title={t("REMOVE")} disabled={isApplicant} onClick={() => handleRemoveMember(member.userUuid)}>
              <DeleteIcon fill={isApplicant ? "#9CA3AF" : styles.deleteIconFill} style={styles.deleteIcon} />
            </button>
          </div>
        );
      },
    },
  ], [members, selectedHeadUuid, familyRelationShips, loggedInUuid, t, styles]);

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

      // 3. Use validated `members` React state (avoids state vs. storage divergence)
      const payload = buildCreateFamilyPayload({
        membersList: members,
        tenantId: tenantId,
        selectedHeadUuid: selectedHeadUuid,
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
        <SubmitBar
          label={t("CREATE_FAMILY_ID_BTN")}
          onSubmit={handleCreateFamilyId}
          disabled={submitting || !selectedHeadUuid}
        />
        {submitting && <Loader />}
      </Card>

      {actionModal && (
        <Modal
          headerBarMain={<h1 className="heading-m">{t(actionModal.mode === "view" ? "CORE_COMMON_VIEW" : "CORE_COMMON_EDIT")}</h1>}
          actionCancelLabel={t("BACK")}
          actionCancelOnSubmit={() => setActionModal(null)}
          hideSubmit
          formId="family-member-action"
        >
          {actionModal.mode === "view" ? (
            <FieldGrid
              t={t}
              fields={withValues([
                ["CORE_COMMON_NAME", actionModal.member.name],
                ["CORE_COMMON_RELATIONSHIP", actionModal.member.relationshipWithHead],
                ["CORE_COMMON_PROFILE_MOBILE_NUMBER", actionModal.member.mobileNumber],
                ["CORE_COMMON_PROFILE_GENDER", actionModal.member.gender],
                ["CORE_COMMON_AADHAR_NUMBER", actionModal.member.aadhaarLast4 && `XXXX-XXXX-${actionModal.member.aadhaarLast4}`],
              ])}
            />
          ) : (
            <Dropdown
              t={t}
              option={familyRelationShips}
              optionKey="name"
              selected={familyRelationShips.find((r) => r.code === actionModal.member.relationshipWithHead) || null}
              select={(val) => {
                handleRelationshipChange(actionModal.member.userUuid, val);
                setActionModal(null);
              }}
              placeholder={t("SELECT_RELATIONSHIP")}
            />
          )}
        </Modal>
      )}

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
