// localStorage has no native bulk-write API. Accept a key/value map so callers
// describe the complete session once while this helper performs one setItem
// operation per key as required by the Web Storage API.
const setLocalStorageItems = (items) => {
  Object.entries(items).forEach(([key, value]) => localStorage.setItem(key, value));
};

/**
 * Persist a normalized authenticated user for Citizen or Employee consumers.
 *
 * Both user types use the same generic compatibility keys. `storageNamespace`
 * adds the matching `Citizen.*` or `Employee.*` aliases, while
 * `sessionTenantKey` supports the extra Employee tenant entry without forcing
 * it on Citizen sessions.
 */
export const persistAuthSession = ({
  user,
  tenantId,
  locale,
  storageNamespace,
  sessionTenantKey,
}) => {
  // UPYOG services consume the complete user object from their session wrapper.
  Digit.SessionStorage.set("citizen.userRequestObject", user);
  if (sessionTenantKey) Digit.SessionStorage.set(sessionTenantKey, tenantId);
  Digit.UserService.setUser(user);

  const userInfo = JSON.stringify(user?.info);
  const accessToken = user?.access_token;

  // Keep generic legacy keys and user-type-specific aliases synchronized from
  // one declarative map. setLocalStorageItems performs the required individual
  // browser writes internally.
  setLocalStorageItems({
    [`${storageNamespace}.tenant-id`]: tenantId,
    "tenant-id": tenantId,
    "citizen.userRequestObject": userInfo,
    locale,
    [`${storageNamespace}.locale`]: locale,
    token: accessToken,
    [`${storageNamespace}.token`]: accessToken,
    "user-info": userInfo,
    [`${storageNamespace}.user-info`]: userInfo,
  });
};
