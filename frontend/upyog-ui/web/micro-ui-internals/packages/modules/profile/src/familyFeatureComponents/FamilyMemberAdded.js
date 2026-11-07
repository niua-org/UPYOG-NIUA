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
} from "@nudmcdgnpm/digit-ui-react-components";

import { getFamilyMembers, clearFamilyMembers, setFamilyMembers } from "../utils";
import { buildCreateFamilyPayload } from "../utils/payloads";
import { profileStyles } from "../utils/profileStyles";
import { PROFILE_ROUTES } from "../utils/constants";

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
    if (stored && stored.length > 0) return stored;
    const currentUser = Digit.UserService.getUser()?.info;
    if (currentUser?.uuid) {
      const initialHead = {
        userUuid: currentUser.uuid,
        name: currentUser.name || "Citizen",
        relationshipWithHead: "",
        isHead: true,
        mobileNumber: currentUser.mobileNumber,
        gender: currentUser.gender,
        aadhaarLast4: currentUser.aadhaarNumber ? String(currentUser.aadhaarNumber).slice(-4) : undefined,
      };
      setFamilyMembers([initialHead]);
      return [initialHead];
    }
    return [];
  });

  const [submitting, setSubmitting] = useState(false);
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
    if (!members.length) {
      const currentUser = Digit.UserService.getUser()?.info;
      if (currentUser?.uuid) {
        const initialHead = {
          userUuid: currentUser.uuid,
          name: currentUser.name,
          relationshipWithHead: "",
          isHead: true,
          mobileNumber: currentUser.mobileNumber,
          gender: currentUser.gender,
          aadhaarLast4: currentUser.aadhaarNumber ? String(currentUser.aadhaarNumber).slice(-4) : undefined,
        };
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

        // Applicant row cannot be deleted
        if (isApplicant) {
          return <span style={styles.actionDisabled}>-</span>;
        }

        // Delete button for added family members
        return (
          <button
            type="button"
            onClick={() => handleRemoveMember(member.userUuid)}
            style={styles.deleteBtn}
            title={t("REMOVE")}
          >
            <DeleteIcon fill={styles.deleteIconFill} style={styles.deleteIcon} />
          </button>
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
   * 1. Validates that all members have relationships selected.
   * 2. Formats payload matching backend user-family create schema.
   * 3. Dispatches createFamily API call.
   * 4. Clears temporary session storage.
   * 5. Navigates to FamilyCreatedResponse with generated familyId.
   */
  const handleCreateFamilyId = async () => {
    if (submitting || !selectedHeadUuid) return;

    // Validation: Ensure all members have an assigned relationship
    const unassignedMember = members.find((m) => !m.relationshipWithHead);
    if (unassignedMember) {
      showToast("error", t("ERR_SELECT_RELATIONSHIP_FOR_ALL"));
      return;
    }

    setSubmitting(true);

    try {
      const tenantId = Digit.ULBService.getCurrentTenantId();
      const membersList = getFamilyMembers();

      // Build structured payload for backend
      const payload = buildCreateFamilyPayload({
        membersList: membersList,
        tenantId: tenantId,
        selectedHeadUuid: selectedHeadUuid,
      });

      // API Call: Create Family
      const res = await Digit.UserService.createFamily(payload, tenantId);
      const createdFamilyId = res?.Families?.[0]?.familyId;

      // Clear cached family data and temporary verification tokens from session
      clearFamilyMembers();
      try {
        sessionStorage.removeItem("Digit.family.verifiedMember");
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
      showToast(
        "error",
        err?.response?.data?.Errors?.[0]?.message || err?.message || "Failed to create Family ID."
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
