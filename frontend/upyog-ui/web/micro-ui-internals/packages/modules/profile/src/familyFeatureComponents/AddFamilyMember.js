import React from "react";
import { useNavigate } from "react-router-dom";
import AadhaarVerificationModal from "../verifications/AadhaarVerificationModal";
import { PROFILE_ROUTES } from "../utils/constants";

/**
 * AddFamilyMember Component
 *
 * Route entry component for /upyog-ui/citizen/profile/add-family-member
 * Delegates to the unified AadhaarVerificationModal in "family" mode.
 */
const AddFamilyMember = ({ isModal = false, isOpen = true, onClose, onSuccess }) => {
  const navigate = useNavigate();

  return (
    <AadhaarVerificationModal
      isModal={isModal}
      isOpen={isOpen}
      mode="family"
      onClose={onClose}
      onSuccess={({ verifiedMember, aadhaarLast4, maskedMobile }) => {
        if (onSuccess) {
          onSuccess({ verifiedMember, aadhaarLast4, maskedMobile });
        } else {
          navigate(`${PROFILE_ROUTES.PROFILE_CREATE_FAMILY}/profileDetails?member=${verifiedMember?.userUuid}`);
        }
      }}
    />
  );
};

export default AddFamilyMember;
