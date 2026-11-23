import React, { useState, useCallback, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { EditIcon, LinkButton, SubmitBar, Toast } from "@nudmcdgnpm/digit-ui-react-components";
import AddressTranformer from "./AddressTranformer";
import { ADDRESS_FIELDS, Field } from "../utils";

/**
 * EditAddress
 *
 * Displays the list of existing addresses and manages Add / Edit modals.
 * Synchronizes updates back to the parent component via `onAddressUpdate`.
 */
const EditAddress = ({ initialAddresses = [], onAddressUpdate }) => {
  const { t } = useTranslation();
  const userInfo = Digit.UserService.getUser()?.info || {};
  const tenantId = Digit.ULBService.getCurrentTenantId();

  const [addresses, setAddresses] = useState(initialAddresses);
  const [addressModal, setAddressModal] = useState({ open: false, address: null });
  const [toast, setToast] = useState(null);

  // Auto-sync when parent fetches fresh addresses
  useEffect(() => {
    setAddresses(initialAddresses);
  }, [initialAddresses]);

  // Auto-dismiss toast after 3 seconds
  useEffect(() => {
    if (!toast) return;
    const timer = setTimeout(() => setToast(null), 3000);
    return () => clearTimeout(timer);
  }, [toast]);

  // Callback to fetch new addresses once added or updated
  const refreshAddresses = useCallback(async () => {
    if (!userInfo.uuid) return;
    const res = await Digit.UserService.searchAddress(userInfo.uuid, tenantId);
    const updated = res?.address || [];
    setAddresses(updated);
    onAddressUpdate?.(updated);
  }, [userInfo.uuid, tenantId, onAddressUpdate]);

  const closeAddressModal = () => setAddressModal({ open: false, address: null });

  // Called by AddressTranformer on successful create/update
  const handleSuccess = (message) => {
    setToast({ error: false, label: message });
  };

  return (
    <>
      <div className="edit-address-actions">
        <SubmitBar
          label={t("ADD_NEW_ADDRESS")}
          onSubmit={() => setAddressModal({ open: true, address: null })}
        />
      </div>
      {addresses.length === 0 && <p>{t("CS_NO_ADDRESS_AVAILABLE")}</p>}
      {addresses.map((address, index) => (
        <div key={address.id ?? index} className="profile-card">
          <div className="profile-details-card-header">
            <div className="profile-card-label">{t(address.addressType)}</div>
            <LinkButton
              label={<EditIcon className="profile-edit-icon" />}
              className="check-page-link-button"
              onClick={() => setAddressModal({ open: true, address })}
            />
          </div>
          <div className="profile-grid">
            {ADDRESS_FIELDS.map(([key, prop]) => {
              if (prop === "addressProof" && !address.filestoreId) {
                return null;
              }
              const val = prop === "addressProof" ? address.filestoreId : address[prop];
              return (
                <Field
                  key={key}
                  fieldKey={key}
                  label={t(key)}
                  value={val || t("CS_NA")}
                  tenantId={tenantId}
                />
              );
            })}
          </div>
        </div>
      ))}

      {addressModal.open && (
        <AddressTranformer
          {...(addressModal.address ? { isEdit: true, address: addressModal.address } : {})}
          refreshAddresses={refreshAddresses}
          actionCancelOnSubmit={closeAddressModal}
          onSuccess={handleSuccess}
        />
      )}

      {toast && (
        <Toast
          error={toast.error}
          label={toast.label}
          onClose={() => setToast(null)}
        />
      )}
    </>
  );
};

export default EditAddress;
