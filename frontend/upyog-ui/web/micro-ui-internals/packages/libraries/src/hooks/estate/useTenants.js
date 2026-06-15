import React, { useEffect, useState } from "react";

// Custom hook to retrieve tenants for the EST module
// This hook accesses session storage to get the list of tenants
// associated with the Estate module.

const useTenants = () => {
  const tenantInfo = Digit.SessionStorage.get("EST_TENANTS");
  const [tenants, setTenants] = useState(tenantInfo ? tenantInfo : null);

  return tenants;
};

export default useTenants;
