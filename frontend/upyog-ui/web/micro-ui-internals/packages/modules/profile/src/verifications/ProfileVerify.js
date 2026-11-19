import React from "react";
import AadhaarVerificationModal from "./AadhaarVerificationModal";
import { PROFILE_ROUTES } from "../utils/constants";

/**
 * ProfileVerify Component
 *
 * Route entry component for /upyog-ui/citizen/profile/verify-profile
 * Delegates to the unified AadhaarVerificationModal in "profile" mode.
 */
const ProfileVerify = ({ isModal = false, isOpen = true, onClose, onSuccess }) => {
  return (
    <AadhaarVerificationModal
      isModal={isModal}
      isOpen={isOpen}
      mode="profile"
      onClose={onClose}
      onSuccess={(user) => {
        if (onSuccess) {
          onSuccess(user);
        } else {
          window.location.href = PROFILE_ROUTES.DETAILS;
        }
      }}
    />
  );
};

export default ProfileVerify;
