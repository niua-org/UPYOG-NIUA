import React, { useState, useCallback } from "react";
import { useTranslation } from "react-i18next";
import { EditIcon, LinkButton, SubmitBar } from "@nudmcdgnpm/digit-ui-react-components";
import AddressTranformer from "./AddressTranformer";
import { ADDRESS_FIELDS, Field } from "../utils";
import { profileStyles } from "../utils/profileStyles";

/*
 * EditAddress
 *
 * Handles everything related to a user's saved addresses.
 * Displays the list of existing addresses with an edit option on each card.
 * Allows adding a new address or editing an existing one via the AddressTranformer modal.
 * After any add/edit operation, refreshes the address list from the API to stay in sync.
 * Manages its own addresses state independently — no prop drilling from parent.
 */


const EditAddress = ({ initialAddresses = [] }) => {
  const { t } = useTranslation();
  const userInfo = Digit.UserService.getUser()?.info || {};
  const tenantId = Digit.ULBService.getCurrentTenantId();

  const [addresses, setAddresses] = useState(initialAddresses);
  const [addressModal, setAddressModal] = useState({ open: false, address: null });

  // callback to fetch the new address once updated
  const refreshAddresses = useCallback(async () => {
    if (!userInfo.uuid) return;
    const res = await Digit.UserService.searchAddress(userInfo.uuid, tenantId);
    setAddresses(res?.address || []);
  }, [userInfo.uuid, tenantId]);

  const closeAddressModal = () => setAddressModal({ open: false, address: null });

  return (
    <>
      <div style={profileStyles.addressActions}>
        <SubmitBar
          label={t("ADD_NEW_ADDRESS")}
          onSubmit={() => setAddressModal({ open: true, address: null })}
        />
      </div>
      {addresses.length === 0 && <p>{t("CS_NO_ADDRESS_AVAILABLE")}</p>}
      {addresses.map((address, index) => (
        <div key={address.id ?? index} style={profileStyles.card}>
          <div style={profileStyles.cardHeader}>
            <div style={profileStyles.cardLabel}>{t(address.addressType)}</div>
            <LinkButton
              label={<EditIcon className="profile-edit-icon" />}
              className="check-page-link-button"
              onClick={() => setAddressModal({ open: true, address })}
            />
          </div>
          <div style={profileStyles.grid}>
            {ADDRESS_FIELDS.map(([key, prop]) => {
              const val = prop === "addressProof" ? (address.filestoreId) : address[prop];
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
        />
      )}
    </>
  );
};

export default EditAddress;
