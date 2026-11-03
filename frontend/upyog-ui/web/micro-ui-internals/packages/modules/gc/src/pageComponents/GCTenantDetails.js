import React from "react";
import {
  CardLabel,
  TextInput,
  Dropdown,
  CardSubHeader,
  LinkButton,
} from "@nudmcdgnpm/digit-ui-react-components";

/**
 * GCTenantDetails Component
 * 
 * Manages dynamic tenant units mapped to detected property floors as per UPYOG GC HLD:
 * - Dynamic addition of tenant units up to detected floors limit
 * - Per-tenant details: Assigned floor, Name, Mobile number (for login & billing), and Email
 */
const GCTenantDetails = ({
  t,
  user,
  tenants = [],
  setTenants,
  detectedFloorCount = 1,
  floorOptions = [],
  setToast,
}) => {
  const isEmployee = user?.type === "EMPLOYEE";

  const handleAddTenant = () => {
    if (tenants.length >= detectedFloorCount) {
      if (setToast) {
        setToast({
          warning: true,
          label: t("GC_MAX_TENANTS_REACHED"),
        });
      }
      return;
    }

    const usedFloors = tenants.map((tn) => String(tn.floorNo));
    const nextAvailableFloor = floorOptions.find((f) => !usedFloors.includes(String(f.code)))?.code;

    setTenants([
      ...tenants,
      {
        id: Date.now().toString(),
        floorNo: nextAvailableFloor,
        name: "",
        mobileNumber: "",
        email: "",
      },
    ]);
  };

  const handleRemoveTenant = (index) => {
    const updated = tenants.filter((_, idx) => idx !== index);
    setTenants(updated);
  };

  const handleTenantFieldChange = (index, field, value) => {
    const updated = [...tenants];
    updated[index] = { ...updated[index], [field]: value };
    setTenants(updated);
  };

  return (
    <div className={`gc-tenant-section ${isEmployee ? "employee" : ""}`}>
      <div className="gc-tenant-header-row">
        <div>
          <CardSubHeader className="gc-tenant-header-title">
            {t("GC_TENANT_ADDON_HEADER")}
          </CardSubHeader>
          <p className="gc-tenant-floor-info">
            {t("GC_DETECTED_FLOOR_INFO")}
          </p>
        </div>

        <button
          type="button"
          className="gc-tenant-add-btn"
          onClick={handleAddTenant}
          disabled={tenants.length >= detectedFloorCount}
        >
          + {t("GC_ADD_TENANT_BTN")}
        </button>
      </div>

      {tenants.length === 0 && (
        <div className="gc-tenant-empty-box">
          {t("GC_NO_TENANTS_ADDED_MSG")}
        </div>
      )}

      {tenants.map((tenant, idx) => {
        const selectedFloor = floorOptions.find((f) => String(f.code) === String(tenant.floorNo));
        return (
          <div key={tenant.id} className="gc-tenant-card">
            <div className="gc-tenant-card-header">
              <span className="gc-tenant-title">
                {t("GC_TENANT")} #{idx + 1}
              </span>
              <LinkButton
                label={t("GC_REMOVE_TENANT")}
                className="gc-tenant-remove-btn"
                onClick={() => handleRemoveTenant(idx)}
              />
            </div>

            <div className={`gc-tenant-grid ${isEmployee ? "employee" : ""}`}>
              <div>
                <CardLabel>
                  {t("GC_FLOOR_UNIT")} <span className="astericColor">*</span>
                </CardLabel>
                <Dropdown
                  option={floorOptions}
                  optionKey="name"
                  selected={selectedFloor}
                  select={(val) => handleTenantFieldChange(idx, "floorNo", val.code)}
                  placeholder={t("GC_SELECT_FLOOR")}
                  t={t}
                />
              </div>

              <div>
                <CardLabel>
                  {t("GC_TENANT_NAME")} <span className="astericColor">*</span>
                </CardLabel>
                <TextInput
                  value={tenant.name}
                  placeholder={t("GC_ENTER_TENANT_NAME")}
                  onChange={(e) => handleTenantFieldChange(idx, "name", e.target.value.replace(/[^A-Za-z ]/g, ""))}
                />
              </div>

              <div>
                <CardLabel>
                  {t("GC_TENANT_MOBILE_NO")} <span className="astericColor">*</span>
                </CardLabel>
                <TextInput
                  type="text"
                  inputMode="numeric"
                  maxLength={10}
                  value={tenant.mobileNumber}
                  placeholder={t("GC_ENTER_MOBILE_NO")}
                  onChange={(e) => {
                    let value = e.target.value.replace(/\D/g, "").slice(0, 10);
                    if (value.length > 0 && !/^[5-9]/.test(value)) value = "";
                    handleTenantFieldChange(idx, "mobileNumber", value);
                  }}
                />
                <span className="gc-tenant-subtext">
                  {t("GC_TENANT_LOGIN_DISCLAIMER")}
                </span>
              </div>

              <div>
                <CardLabel>{t("GC_TENANT_EMAIL_ID")}</CardLabel>
                <TextInput
                  type="text"
                  value={tenant.email}
                  placeholder={t("ENTER_EMAIL_ID")}
                  onChange={(e) => handleTenantFieldChange(idx, "email", e.target.value.trim())}
                />
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
};

export const AddTenant = GCTenantDetails;
export default GCTenantDetails;
