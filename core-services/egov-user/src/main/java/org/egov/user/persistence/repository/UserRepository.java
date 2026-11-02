package org.egov.user.persistence.repository;

import lombok.extern.slf4j.Slf4j;

import org.egov.tracer.model.CustomException;
import org.egov.user.domain.model.Address;
import org.egov.user.domain.model.Role;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.UserSearchCriteria;
import org.egov.user.domain.model.enums.BloodGroup;
import org.egov.user.domain.model.enums.Gender;
import org.egov.user.domain.model.enums.GuardianRelation;
import org.egov.user.domain.model.enums.UserType;
import org.egov.user.domain.service.utils.UserConstants;
import org.egov.user.persistence.dto.FailedLoginAttempt;
import org.egov.user.repository.builder.RoleQueryBuilder;
import org.egov.user.repository.builder.UserTypeQueryBuilder;
import org.egov.user.repository.rowmapper.DigilockerUserResultSetExtractor;
import org.egov.user.repository.rowmapper.UserResultSetExtractor;
import org.egov.user.repository.rowmapper.UserResultSetExtractorV2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static org.egov.user.repository.builder.UserTypeQueryBuilder.*;
import static org.springframework.util.StringUtils.isEmpty;

/**
 * Repository layer for managing {@link User} entities in the {@code eg_user} table.
 * Supports legacy and V2 CRUD operations, authentication metadata, role associations,
 * and user audit logging.
 */
@Repository
@Slf4j
public class UserRepository {

    private AddressRepository addressRepository;
    private AuditRepository auditRepository;
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private JdbcTemplate jdbcTemplate;
    private UserTypeQueryBuilder userTypeQueryBuilder;
    private RoleRepository roleRepository;
    private UserResultSetExtractor userResultSetExtractor;
    private UserResultSetExtractorV2 userResultSetExtractorV2;

    @Value("${user.address.mandatory.fields.enabled}")
    private boolean addressMandatoryFieldsEnabled;

    @Value("${user.address.update.strategy}")
    private Boolean addressSoftUpdateFlag;

    @Autowired
    UserRepository(RoleRepository roleRepository, UserTypeQueryBuilder userTypeQueryBuilder,
                   AddressRepository addressRepository, UserResultSetExtractor userResultSetExtractor,
                   JdbcTemplate jdbcTemplate,
                   NamedParameterJdbcTemplate namedParameterJdbcTemplate, AuditRepository auditRepository,
                   @Autowired(required = false) UserResultSetExtractorV2 userResultSetExtractorV2) {
        this.addressRepository = addressRepository;
        this.roleRepository = roleRepository;
        this.userTypeQueryBuilder = userTypeQueryBuilder;
        this.userResultSetExtractor = userResultSetExtractor;
        this.jdbcTemplate = jdbcTemplate;
        this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        this.auditRepository = auditRepository;
        this.userResultSetExtractorV2 = userResultSetExtractorV2;
    }

    public UserRepository(RoleRepository roleRepository, UserTypeQueryBuilder userTypeQueryBuilder,
                          AddressRepository addressRepository, UserResultSetExtractor userResultSetExtractor,
                          JdbcTemplate jdbcTemplate,
                          NamedParameterJdbcTemplate namedParameterJdbcTemplate, AuditRepository auditRepository) {
        this(roleRepository, userTypeQueryBuilder, addressRepository, userResultSetExtractor,
             jdbcTemplate, namedParameterJdbcTemplate, auditRepository, null);
    }

    /**
     * api will get the all users by userSearchCriteria.After that roles and
     * address are set in to the user object.
     *
     * @param userSearch
     * @return
     */
    public List<User> findAll(UserSearchCriteria userSearch) {
    	log.info("user find all method call");
        final List<Object> preparedStatementValues = new ArrayList<>();
        boolean RoleSearchHappend = false;
        List<Long> userIds = new ArrayList<>();
        if (!isEmpty(userSearch.getRoleCodes()) && userSearch.getTenantId() != null) {
        	log.info("User with role if condition");
            userIds = findUsersWithRole(userSearch);
            RoleSearchHappend = true;
        }
        List<User> users = new ArrayList<>();
        if (RoleSearchHappend) {
            if (!CollectionUtils.isEmpty(userIds)) {
                if (CollectionUtils.isEmpty(userSearch.getId()))
                    userSearch.setId(userIds);
                else {
                    userSearch.setId(userSearch.getId().stream().filter(userIds::contains).collect(Collectors.toList()));
                    if (CollectionUtils.isEmpty(userSearch.getId()))
                        return users;
                }
                userSearch.setTenantId(null);
                userSearch.setRoleCodes(null);
            } else {
                return users;
            }
        }
        String queryStr = userTypeQueryBuilder.getQuery(userSearch, preparedStatementValues);
        log.info("User search Query : {}", queryStr);
        log.info("User search Parameters: {}", preparedStatementValues);
        log.info("User search Criteria - UUID: {}, tenantId: {}, active: {}",
                 userSearch.getUuid(), userSearch.getTenantId(), userSearch.getActive());

        users = jdbcTemplate.query(queryStr, preparedStatementValues.toArray(), userResultSetExtractor);
        enrichRoles(users);

        return users;
    }


    /**
     * get list of all userids with role in given tenant
     *
     * @param userSearch
     * @return
     */
    public List<Long> findUsersWithRole(UserSearchCriteria userSearch) {
        final List<Object> preparedStatementValues = new ArrayList<>();
        List<Long> usersIds = new ArrayList<>();
        String queryStr = userTypeQueryBuilder.getQueryUserRoleSearch(userSearch, preparedStatementValues);
        log.debug(queryStr);

        usersIds = jdbcTemplate.queryForList(queryStr, preparedStatementValues.toArray(), Long.class);

        return usersIds;
    }


    /**
     * Api will check user is present or not with userName And tenantId
     *
     * @param userName
     * @param tenantId
     * @return
     */
    public boolean isUserPresent(String userName, String tenantId, UserType userType) {

        String query = USER_PRESENT_BY_USERNAME_AND_TENANT_SQL;

        final Map<String, Object> parametersMap = new HashMap<String, Object>();
        parametersMap.put("userName", userName);
        parametersMap.put("tenantId", tenantId);
        parametersMap.put("userType", userType.toString());

        int count = namedParameterJdbcTemplate.queryForObject(query, parametersMap, Integer.class);

        return count > 0;
    }

    /**
     * this api will create the user.
     *
     * @param user
     * @return
     */
    public User create(User user) {
        validateAndEnrichRoles(Collections.singletonList(user));
        final Long newId = getNextSequence();
        user.setId(newId);
        user.setUuid(UUID.randomUUID().toString());
        user.setDigilockerid(user.getDigilockerid());
        user.setCreatedDate(new Date());
        user.setLastModifiedDate(new Date());
        user.setCreatedBy(user.getLoggedInUserId());
        user.setLastModifiedBy(user.getLoggedInUserId());
        final User savedUser = save(user);
        if (user.getRoles().size() > 0) {
            saveUserRoles(user);
        }
        return savedUser;
    }

    /**
     * api will update the user details.
     *
     * @param user
     * @param uuid 
     * @param  
     * @return
     */
    public void update(final User user, User oldUser, long userId, String uuid) {


        Map<String, Object> updateuserInputs = new HashMap<>();

        Long targetId = (oldUser != null && oldUser.getId() != null) ? oldUser.getId() : (user != null ? user.getId() : null);
        String targetUuid = (oldUser != null && oldUser.getUuid() != null) ? oldUser.getUuid() : (user != null ? user.getUuid() : null);

        updateuserInputs.put("id", targetId != null ? targetId : -1L);
        updateuserInputs.put("uuid", targetUuid != null ? targetUuid : "");
        updateuserInputs.put("username", oldUser.getUsername());
        updateuserInputs.put("type", oldUser.getType().toString());
        updateuserInputs.put("tenantid", oldUser.getTenantId());
        updateuserInputs.put("AadhaarNumber", user.getAadhaarNumber());

        if (isNull(user.getAccountLocked()))
            updateuserInputs.put("AccountLocked", oldUser.getAccountLocked());
        else
            updateuserInputs.put("AccountLocked", user.getAccountLocked());

        if (isNull(user.getAccountLockedDate()))
            updateuserInputs.put("AccountLockedDate", oldUser.getAccountLockedDate());
        else
            updateuserInputs.put("AccountLockedDate", user.getAccountLockedDate());

        updateuserInputs.put("Active", user.getActive());
        updateuserInputs.put("AltContactNumber", user.getAltContactNumber());

        List<Enum> bloodGroupEnumValues = Arrays.asList(BloodGroup.values());
        if (user.getBloodGroup() != null) {
            if (bloodGroupEnumValues.contains(user.getBloodGroup()))
                updateuserInputs.put("BloodGroup", user.getBloodGroup().toString());
            else
                updateuserInputs.put("BloodGroup", "");
        }
        else if (oldUser != null && oldUser.getBloodGroup() != null) {
            if (bloodGroupEnumValues.contains(oldUser.getBloodGroup()))
                updateuserInputs.put("BloodGroup", oldUser.getBloodGroup().toString());
            else
                updateuserInputs.put("BloodGroup", "");
        }
        else {
            updateuserInputs.put("BloodGroup", "");
        }

        if (user.getDob() != null) {
            updateuserInputs.put("Dob", user.getDob());
        } else {
            updateuserInputs.put("Dob", oldUser.getDob());
        }
        updateuserInputs.put("EmailId", user.getEmailId());

        if (user.getGender() != null) {
            if (Gender.FEMALE.toString().equals(user.getGender().toString())) {
                updateuserInputs.put("Gender", 1);
            } else if (Gender.MALE.toString().equals(user.getGender().toString())) {
                updateuserInputs.put("Gender", 2);
            } else if (Gender.OTHERS.toString().equals(user.getGender().toString())) {
                updateuserInputs.put("Gender", 3);
            } else if (Gender.TRANSGENDER.toString().equals(user.getGender().toString())) {
                updateuserInputs.put("Gender", 4); 
            } else {
                updateuserInputs.put("Gender", 0);
            }
        } else {
            updateuserInputs.put("Gender", 0);
        }
        updateuserInputs.put("Guardian", user.getGuardian());

        List<Enum> enumValues = Arrays.asList(GuardianRelation.values());
        if (user.getGuardianRelation() != null) {
            if(enumValues.contains(user.getGuardianRelation()))
                updateuserInputs.put("GuardianRelation", user.getGuardianRelation().toString());
            else {
                updateuserInputs.put("GuardianRelation", "");
            }
            
        } else {
            updateuserInputs.put("GuardianRelation", "");
        }
        updateuserInputs.put("IdentificationMark", user.getIdentificationMark());
        updateuserInputs.put("Locale", user.getLocale());
        if (null != user.getMobileNumber())
            updateuserInputs.put("MobileNumber", user.getMobileNumber());
        else
            updateuserInputs.put("MobileNumber", oldUser.getMobileNumber());
        updateuserInputs.put("Name", user.getName());
        updateuserInputs.put("Pan", user.getPan());

        if (!isEmpty(user.getPassword()))
            updateuserInputs.put("Password", user.getPassword());
        else
            updateuserInputs.put("Password", oldUser.getPassword());

        if (oldUser != null && user.getPhoto() != null && user.getPhoto().contains("http"))
            updateuserInputs.put("Photo", oldUser.getPhoto());
        else
            updateuserInputs.put("Photo", user.getPhoto());

        if (null != user.getPasswordExpiryDate())
            updateuserInputs.put("PasswordExpiryDate", user.getPasswordExpiryDate());
        else
            updateuserInputs.put("PasswordExpiryDate", oldUser.getPasswordExpiryDate());
        updateuserInputs.put("Salutation", user.getSalutation());
        updateuserInputs.put("Signature", user.getSignature());
        updateuserInputs.put("Title", user.getTitle());
        updateuserInputs.put("DigilockerID",user.getDigilockerid());

            List<Enum> userTypeEnumValues = Arrays.asList(UserType.values());
            if (user.getType() != null) {
                if (userTypeEnumValues.contains(user.getType()))
                    updateuserInputs.put("Type", user.getType().toString());
                else {
                    updateuserInputs.put("Type", "");
                }
            } else {
                updateuserInputs.put("Type", oldUser.getType().toString());
            }


        updateuserInputs.put("alternatemobilenumber", user.getAlternateMobileNumber());

        updateuserInputs.put("FatherName", user.getFatherName() != null ? user.getFatherName() : (oldUser != null ? oldUser.getFatherName() : null));
        updateuserInputs.put("MotherName", user.getMotherName() != null ? user.getMotherName() : (oldUser != null ? oldUser.getMotherName() : null));
        updateuserInputs.put("SpouseName", user.getSpouseName() != null ? user.getSpouseName() : (oldUser != null ? oldUser.getSpouseName() : null));
        boolean isIdentityAuthSource = user.getIdentityVerifiedSource() != null &&
                (UserConstants.IDENTITY_SOURCE_AADHAAR.equalsIgnoreCase(user.getIdentityVerifiedSource())
                        || UserConstants.IDENTITY_SOURCE_DIGILOCKER.equalsIgnoreCase(user.getIdentityVerifiedSource())
                        || UserConstants.IDENTITY_SOURCE_FAMILY_ID.equalsIgnoreCase(user.getIdentityVerifiedSource()));

        Boolean effectiveVerified = (oldUser != null && oldUser.getIsIdentityVerified() != null)
                ? (isIdentityAuthSource && user.getIsIdentityVerified() != null ? user.getIsIdentityVerified() : oldUser.getIsIdentityVerified())
                : (isIdentityAuthSource && user.getIsIdentityVerified() != null ? user.getIsIdentityVerified() : false);

        Date effectiveVerifiedDate = (oldUser != null && oldUser.getIdentityVerifiedDate() != null)
                ? (isIdentityAuthSource && user.getIdentityVerifiedDate() != null ? user.getIdentityVerifiedDate() : oldUser.getIdentityVerifiedDate())
                : (isIdentityAuthSource && user.getIdentityVerifiedDate() != null ? user.getIdentityVerifiedDate() : null);

        String effectiveVerifiedSource = (oldUser != null && oldUser.getIdentityVerifiedSource() != null)
                ? (isIdentityAuthSource && user.getIdentityVerifiedSource() != null ? user.getIdentityVerifiedSource() : oldUser.getIdentityVerifiedSource())
                : (isIdentityAuthSource && user.getIdentityVerifiedSource() != null ? user.getIdentityVerifiedSource() : null);

        updateuserInputs.put("IsIdentityVerified", effectiveVerified);
        updateuserInputs.put("IdentityVerifiedDate", effectiveVerifiedDate);
        updateuserInputs.put("IdentityVerifiedSource", effectiveVerifiedSource);
        updateuserInputs.put("LastResyncDate", user.getLastResyncDate() != null ? user.getLastResyncDate() : (oldUser != null ? oldUser.getLastResyncDate() : null));

        int currentEditCount = (oldUser != null && oldUser.getEdittedCount() != null) ? oldUser.getEdittedCount() : 0;
        int nextEditCount = user.getEdittedCount() != null ? user.getEdittedCount() : (currentEditCount + 1);
        updateuserInputs.put("EdittedCount", nextEditCount);
        updateuserInputs.put("LastLoginMethod", user.getLastLoginMethod() != null ? user.getLastLoginMethod() : (oldUser != null ? oldUser.getLastLoginMethod() : null));

        updateuserInputs.put("LastModifiedDate", new Date());
        updateuserInputs.put("LastModifiedBy", userId );
        if(user.isDigilockerRegistration()) {
            namedParameterJdbcTemplate.update(UPDATE_DIGILOCKER_ID_SQL, updateuserInputs);
        }else {
            updateAuditDetails(oldUser, userId, uuid);
            int rowsUpdated = namedParameterJdbcTemplate.update(UPDATE_USER_SQL, updateuserInputs);
            log.info("[USER-REPO] Updated {} rows for user id: {}, uuid: {}", rowsUpdated, targetId, targetUuid);
            if (rowsUpdated == 0) {
                log.warn("[USER-REPO] Zero rows updated for user id: {}, uuid: {}, username: {}", targetId, targetUuid, oldUser != null ? oldUser.getUsername() : null);
                throw new CustomException("DATA_MISMATCH", "Data mismatch: User with id " + targetId + " or uuid " + targetUuid + " does not exist in the database");
            }
            if (user.getRoles() != null && !CollectionUtils.isEmpty(user.getRoles()) && !oldUser.getRoles().equals(user.getRoles())) {
                validateAndEnrichRoles(Collections.singletonList(user));
                updateRoles(user);
            }
        }
    }

	public void fetchFailedLoginAttemptsByUser(String uuid) {
        fetchFailedAttemptsByUserAndTime(uuid, 0L);
    }

    public List<FailedLoginAttempt> fetchFailedAttemptsByUserAndTime(String uuid, long attemptStartDate) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("user_uuid", uuid);
        params.put("attempt_date", attemptStartDate);

//		RowMapper<FailedLoginAttempt> rowMapper = (rs, rowNum) -> {
//			FailedLoginAttempt failedLoginAttempt = new FailedLoginAttempt();
//			failedLoginAttempt.setUserUuid(rs.getString("user_uuid"));
//			failedLoginAttempt.setIp(rs.getString("ip"));
//			failedLoginAttempt.setAttemptDate(rs.getLong("attempt_date"));
//			return failedLoginAttempt;
//		};

        return namedParameterJdbcTemplate.query(SELECT_FAILED_ATTEMPTS_BY_USER_SQL, params,
                new BeanPropertyRowMapper<>(FailedLoginAttempt.class));

    }

    public FailedLoginAttempt insertFailedLoginAttempt(FailedLoginAttempt failedLoginAttempt) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("user_uuid", failedLoginAttempt.getUserUuid());
        inputs.put("ip", failedLoginAttempt.getIp());
        inputs.put("attempt_date", failedLoginAttempt.getAttemptDate());
        inputs.put("active", failedLoginAttempt.isActive());

        namedParameterJdbcTemplate.update(UserTypeQueryBuilder.INSERT_FAILED_ATTEMPTS_SQL, inputs);

        return failedLoginAttempt;
    }

    public void resetFailedLoginAttemptsForUser(String uuid) {

        namedParameterJdbcTemplate.update(UserTypeQueryBuilder.UPDATE_FAILED_ATTEMPTS_SQL,
                Collections.singletonMap("user_uuid", uuid));
    }


    /**
     * Fetch roles by role codes
     *
     * @param roleCodes role code for which roles need to be retrieved
     * @param tenantId  tenant id of the roles
     * @return enriched roles
     */
    private Set<Role> fetchRolesByCode(Set<String> roleCodes, String tenantId) {


        Set<Role> validatedRoles = roleRepository.findRolesByCode(roleCodes, tenantId);

        return validatedRoles;
    }

    private void validateAndEnrichRoles(List<User> users) {

        if (users.isEmpty())
            return;

        Map<String, Role> roleCodeMap = fetchRolesFromMdms(users);

        for (User user : users) {
            if (!isNull(user.getRoles())) {
                for (Role role : user.getRoles()) {
                    if (roleCodeMap.containsKey(role.getCode())) {
                        role.setDescription(roleCodeMap.get(role.getCode()).getDescription());
                        role.setName(roleCodeMap.get(role.getCode()).getName());
                        if (isNull(role.getTenantId()))
                            role.setTenantId(user.getTenantId());
                    } else {
                        log.error("Role : {} is invalid", role);
                        throw new CustomException("INVALID_ROLE", "Unable to validate role from MDMS");
                    }
                }
            }
        }
    }

    private void enrichRoles(List<User> users) {
        log.info("enrichRoles called with {} users", users.size());

        if (users.isEmpty())
            return;

        for (User user : users) {
            log.info("enrichRoles - User {} (UUID: {}) has {} roles before enrichment",
                    user.getId(), user.getUuid(), user.getRoles() != null ? user.getRoles().size() : 0);
            if (user.getRoles() != null) {
                for (Role role : user.getRoles()) {
                    log.info("  - Role: code={}, tenantId={}, name={}", role.getCode(), role.getTenantId(), role.getName());
                }
            }
        }

        Map<String, Role> roleCodeMap = fetchRolesFromMdms(users);
        log.info("enrichRoles - fetched {} role definitions from MDMS", roleCodeMap.size());

        for (User user : users) {
            if (!isNull(user.getRoles())) {
                for (Role role : user.getRoles()) {
                    if (roleCodeMap.containsKey(role.getCode())) {
                        log.debug("Enriching role {} with name and description", role.getCode());
                        role.setDescription(roleCodeMap.get(role.getCode()).getDescription());
                        role.setName(roleCodeMap.get(role.getCode()).getName());
                    } else {
                        log.warn("Role code {} not found in MDMS", role.getCode());
                    }
                }
            } else {
                log.warn("User {} (UUID: {}) has null roles", user.getId(), user.getUuid());
            }
        }
    }

    private Map<String, Role> fetchRolesFromMdms(List<User> users) {

        Set<String> roleCodes = new HashSet<>();

        for (User user : users) {
            if (!isNull(user.getRoles()) && !user.getRoles().isEmpty()) {
                for (Role role : user.getRoles())
                    roleCodes.add(role.getCode());
            }
        }

        if (roleCodes.isEmpty())
            return Collections.emptyMap();

        Set<Role> validatedRoles = fetchRolesByCode(roleCodes, getStateLevelTenant(users.get(0).getTenantId()));

        Map<String, Role> roleCodeMap = new HashMap<>();

        for (Role validatedRole : validatedRoles)
            roleCodeMap.put(validatedRole.getCode(), validatedRole);

        return roleCodeMap;
    }


    /**
     * api will do the mapping between user and role.
     *
     * @param entityUser
     */
    private void saveUserRoles(User entityUser) {
        List<Map<String, Object>> batchValues = new ArrayList<>(entityUser.getRoles().size());

        for (Role role : entityUser.getRoles()) {
            batchValues.add(
                    new MapSqlParameterSource("role_code", role.getCode())
                            .addValue("role_tenantid", role.getTenantId())
                            .addValue("user_id", entityUser.getId())
                            .addValue("user_tenantid", entityUser.getTenantId())
                            .addValue("lastmodifieddate", new Date())
                            .getValues());
        }
        namedParameterJdbcTemplate.batchUpdate(RoleQueryBuilder.INSERT_USER_ROLES,
                batchValues.toArray(new Map[entityUser.getRoles().size()]));
    }

    /**
     * api will persist the user.
     *
     * @param entityUser
     * @return
     */
    private User save(User entityUser) {

        Map<String, Object> userInputs = new HashMap<String, Object>();

        userInputs.put("id", entityUser.getId());
        userInputs.put("uuid", entityUser.getUuid());
        userInputs.put("digilockerid", entityUser.getDigilockerid());
        userInputs.put("tenantid", entityUser.getTenantId());
        userInputs.put("salutation", entityUser.getSalutation());
        userInputs.put("dob", entityUser.getDob());
        userInputs.put("locale", entityUser.getLocale());
        userInputs.put("username", entityUser.getUsername());
        userInputs.put("password", entityUser.getPassword());
        userInputs.put("pwdexpirydate", entityUser.getPasswordExpiryDate());
        userInputs.put("mobilenumber", entityUser.getMobileNumber());
        userInputs.put("altcontactnumber", entityUser.getAltContactNumber());
        userInputs.put("emailid", entityUser.getEmailId());
        userInputs.put("active", entityUser.getActive());
        userInputs.put("name", entityUser.getName());

        if (Gender.FEMALE.equals(entityUser.getGender())) {
            userInputs.put("gender", 1);
        } else if (Gender.MALE.equals(entityUser.getGender())) {
            userInputs.put("gender", 2);
        } else if (Gender.OTHERS.equals(entityUser.getGender())) {
            userInputs.put("gender", 3);
        } else if (Gender.TRANSGENDER.equals(entityUser.getGender())) {
            userInputs.put("gender", 4);
        } else {
            userInputs.put("gender", 0);
        }

        userInputs.put("pan", entityUser.getPan());
        userInputs.put("aadhaarnumber", entityUser.getAadhaarNumber());

        List<Enum> userTypeEnumValues = Arrays.asList(UserType.values());
        if (entityUser.getType() != null) {
            if (userTypeEnumValues.contains(entityUser.getType()))
                userInputs.put("type", entityUser.getType().toString());
            else {
                userInputs.put("type", "");
            }
        }
        else {
            userInputs.put("type", "");
        }

        List<Enum> enumValues = Arrays.asList(GuardianRelation.values());
        userInputs.put("guardian", entityUser.getGuardian());
        if (entityUser.getGuardianRelation() != null) {
            if (enumValues.contains(entityUser.getGuardianRelation()))
                userInputs.put("guardianrelation", entityUser.getGuardianRelation().toString());
            else {
                userInputs.put("guardianrelation", "");
            }
        }
        else {
            userInputs.put("guardianrelation", "");
        }
        userInputs.put("signature", entityUser.getSignature());
        userInputs.put("accountlocked", entityUser.getAccountLocked());


        List<Enum> bloodGroupEnumValues = Arrays.asList(BloodGroup.values());
        if(entityUser.getBloodGroup() != null){
            if (bloodGroupEnumValues.contains(entityUser.getBloodGroup()))
                userInputs.put("bloodgroup", entityUser.getBloodGroup().toString());
            else {
                userInputs.put("bloodgroup", "");
            }
        }
        else {
            userInputs.put("bloodgroup", "");
        }

        userInputs.put("photo", entityUser.getPhoto());
        userInputs.put("identificationmark", entityUser.getIdentificationMark());
        userInputs.put("createddate", entityUser.getCreatedDate());
        userInputs.put("lastmodifieddate", entityUser.getLastModifiedDate());
        userInputs.put("createdby", entityUser.getLoggedInUserId());
        userInputs.put("lastmodifiedby", entityUser.getLoggedInUserId());
        userInputs.put("alternatemobilenumber", entityUser.getAlternateMobileNumber());

        userInputs.put("father_name", entityUser.getFatherName());
        userInputs.put("mother_name", entityUser.getMotherName());
        userInputs.put("spouse_name", entityUser.getSpouseName());
        userInputs.put("is_identity_verified", entityUser.getIsIdentityVerified() != null ? entityUser.getIsIdentityVerified() : false);
        userInputs.put("identity_verified_date", entityUser.getIdentityVerifiedDate());
        userInputs.put("identity_verified_source", entityUser.getIdentityVerifiedSource());
        userInputs.put("last_resync_date", entityUser.getLastResyncDate());
        userInputs.put("editted_count", entityUser.getEdittedCount() != null ? entityUser.getEdittedCount() : 0);
        userInputs.put("last_login_method", entityUser.getLastLoginMethod());

        namedParameterJdbcTemplate.update(INSERT_USER_SQL, userInputs);
        return entityUser;
    }

    /**
     * This api will return the next generate sequence of eg_user
     *
     * @return
     */
    private Long getNextSequence() {
        return jdbcTemplate.queryForObject(SELECT_NEXT_SEQUENCE_USER, Long.class);
    }

    /**
     * This api will save addresses for particular user.
     *
     * @param address
     * @param userId
     * @param tenantId
     * @return
     */
    private Address saveAddress(Address address, Long userId, String tenantId) {
        if (address != null) {
            addressRepository.create(address, userId, tenantId);
            return address;
        }
        return null;
    }


    /**
     * api will update the user Roles.
     *
     * @param user
     */
    private void updateRoles(User user) {
        Map<String, Object> roleInputs = new HashMap<String, Object>();
        roleInputs.put("user_id", user.getId());
        roleInputs.put("user_tenantid", user.getTenantId());
        namedParameterJdbcTemplate.update(RoleQueryBuilder.DELETE_USER_ROLES, roleInputs);
        saveUserRoles(user);
    }

    private String getStateLevelTenant(String tenantId) {
        return tenantId.split("\\.")[0];
    }

	
	private void updateAuditDetails(User oldUser, long userId, String uuid) {
		auditRepository.auditUser(oldUser,userId,uuid);
		
	}

    /**
     * this method will create the user with address part of v2.
     *
     * @param user
     * @return
     */
    public User createWithAddressV2(User user) {
        validateAndEnrichRoles(Collections.singletonList(user));
        final Long newId = getNextSequence();
        user.setId(newId);
        user.setUuid(UUID.randomUUID().toString());
        user.setCreatedDate(new Date());
        user.setLastModifiedDate(new Date());
        user.setCreatedBy(user.getLoggedInUserId());
        user.setLastModifiedBy(user.getLoggedInUserId());
        final User savedUser = save(user); // this will remain same as create
        if (!user.getRoles().isEmpty()) {
            saveUserRoles(user);
        }
        return savedUser;
    }

    /**
     * This method will save addresses for particular user.
     *
     * @param address
     * @param userId
     * @param tenantId
     * @return
     */
    private Address saveAddressV2(Address address, Long userId, String tenantId) {
        if (address != null) {
            addressRepository.createAddressV2(address, userId, tenantId);
            return address;
        }
        return null;
    }

    /**
     * method will update the user details with address object with new fields v2.
     *
     * @param user
     * @param uuid
     * @param
     * @return
     */
    public void updateV2(final User user, User oldUser, long userId, String uuid) {


        Map<String, Object> updateuserInputs = new HashMap<>();

        Long targetId = (oldUser != null && oldUser.getId() != null) ? oldUser.getId() : (user != null ? user.getId() : null);
        String targetUuid = (oldUser != null && oldUser.getUuid() != null) ? oldUser.getUuid() : (user != null ? user.getUuid() : null);

        updateuserInputs.put("id", targetId != null ? targetId : -1L);
        updateuserInputs.put("uuid", targetUuid != null ? targetUuid : "");
        updateuserInputs.put("username", oldUser.getUsername());
        updateuserInputs.put("type", oldUser.getType().toString());
        updateuserInputs.put("tenantid", oldUser.getTenantId());
        updateuserInputs.put("AadhaarNumber", user.getAadhaarNumber());

        if (isNull(user.getAccountLocked()))
            updateuserInputs.put("AccountLocked", oldUser.getAccountLocked());
        else
            updateuserInputs.put("AccountLocked", user.getAccountLocked());

        if (isNull(user.getAccountLockedDate()))
            updateuserInputs.put("AccountLockedDate", oldUser.getAccountLockedDate());
        else
            updateuserInputs.put("AccountLockedDate", user.getAccountLockedDate());

        updateuserInputs.put("Active", user.getActive());
        updateuserInputs.put("AltContactNumber", user.getAltContactNumber());

        List<Enum> bloodGroupEnumValues = Arrays.asList(BloodGroup.values());
        if (user.getBloodGroup() != null) {
            if (bloodGroupEnumValues.contains(user.getBloodGroup()))
                updateuserInputs.put("BloodGroup", user.getBloodGroup().toString());
            else
                updateuserInputs.put("BloodGroup", "");
        }
        else if (oldUser != null && oldUser.getBloodGroup() != null) {
            if (bloodGroupEnumValues.contains(oldUser.getBloodGroup()))
                updateuserInputs.put("BloodGroup", oldUser.getBloodGroup().toString());
            else
                updateuserInputs.put("BloodGroup", "");
        }
        else {
            updateuserInputs.put("BloodGroup", "");
        }

        if (user.getDob() != null) {
            updateuserInputs.put("Dob", user.getDob());
        } else {
            updateuserInputs.put("Dob", oldUser.getDob());
        }
        updateuserInputs.put("EmailId", user.getEmailId());

        if (user.getGender() != null) {
            if (Gender.FEMALE.toString().equals(user.getGender().toString())) {
                updateuserInputs.put("Gender", 1);
            } else if (Gender.MALE.toString().equals(user.getGender().toString())) {
                updateuserInputs.put("Gender", 2);
            } else if (Gender.OTHERS.toString().equals(user.getGender().toString())) {
                updateuserInputs.put("Gender", 3);
            } else if (Gender.TRANSGENDER.toString().equals(user.getGender().toString())) {
                updateuserInputs.put("Gender", 4);
            } else {
                updateuserInputs.put("Gender", 0);
            }
        } else {
            updateuserInputs.put("Gender", 0);
        }
        updateuserInputs.put("Guardian", user.getGuardian());

        List<Enum> enumValues = Arrays.asList(GuardianRelation.values());
        if (user.getGuardianRelation() != null) {
            if(enumValues.contains(user.getGuardianRelation()))
                updateuserInputs.put("GuardianRelation", user.getGuardianRelation().toString());
            else {
                updateuserInputs.put("GuardianRelation", "");
            }

        } else {
            updateuserInputs.put("GuardianRelation", "");
        }
        updateuserInputs.put("IdentificationMark", user.getIdentificationMark());
        updateuserInputs.put("Locale", user.getLocale());
        if (null != user.getMobileNumber())
            updateuserInputs.put("MobileNumber", user.getMobileNumber());
        else
            updateuserInputs.put("MobileNumber", oldUser.getMobileNumber());
        updateuserInputs.put("Name", user.getName());
        updateuserInputs.put("Pan", user.getPan());

        if (!isEmpty(user.getPassword()))
            updateuserInputs.put("Password", user.getPassword());
        else
            updateuserInputs.put("Password", oldUser.getPassword());

        if (oldUser != null && user.getPhoto() != null && user.getPhoto().contains("http"))
            updateuserInputs.put("Photo", oldUser.getPhoto());
        else
            updateuserInputs.put("Photo", user.getPhoto());

        if (null != user.getPasswordExpiryDate())
            updateuserInputs.put("PasswordExpiryDate", user.getPasswordExpiryDate());
        else
            updateuserInputs.put("PasswordExpiryDate", oldUser.getPasswordExpiryDate());
        updateuserInputs.put("Salutation", user.getSalutation());
        updateuserInputs.put("Signature", user.getSignature());
        updateuserInputs.put("Title", user.getTitle());


        List<Enum> userTypeEnumValues = Arrays.asList(UserType.values());
        if (user.getType() != null) {
            if (userTypeEnumValues.contains(user.getType()))
                updateuserInputs.put("Type", user.getType().toString());
            else {
                updateuserInputs.put("Type", "");
            }
        }
        else {
            updateuserInputs.put("Type", oldUser.getType().toString());
        }

        updateuserInputs.put("alternatemobilenumber", user.getAlternateMobileNumber());
        updateuserInputs.put("FatherName", user.getFatherName() != null ? user.getFatherName() : (oldUser != null ? oldUser.getFatherName() : null));
        updateuserInputs.put("MotherName", user.getMotherName() != null ? user.getMotherName() : (oldUser != null ? oldUser.getMotherName() : null));
        updateuserInputs.put("SpouseName", user.getSpouseName() != null ? user.getSpouseName() : (oldUser != null ? oldUser.getSpouseName() : null));
        boolean isIdentityAuthSource = user.getIdentityVerifiedSource() != null &&
                (UserConstants.IDENTITY_SOURCE_AADHAAR.equalsIgnoreCase(user.getIdentityVerifiedSource())
                        || UserConstants.IDENTITY_SOURCE_DIGILOCKER.equalsIgnoreCase(user.getIdentityVerifiedSource())
                        || UserConstants.IDENTITY_SOURCE_FAMILY_ID.equalsIgnoreCase(user.getIdentityVerifiedSource()));

        Boolean effectiveVerified = (oldUser != null && oldUser.getIsIdentityVerified() != null)
                ? (isIdentityAuthSource && user.getIsIdentityVerified() != null ? user.getIsIdentityVerified() : oldUser.getIsIdentityVerified())
                : (isIdentityAuthSource && user.getIsIdentityVerified() != null ? user.getIsIdentityVerified() : false);

        Date effectiveVerifiedDate = (oldUser != null && oldUser.getIdentityVerifiedDate() != null)
                ? (isIdentityAuthSource && user.getIdentityVerifiedDate() != null ? user.getIdentityVerifiedDate() : oldUser.getIdentityVerifiedDate())
                : (isIdentityAuthSource && user.getIdentityVerifiedDate() != null ? user.getIdentityVerifiedDate() : null);

        String effectiveVerifiedSource = (oldUser != null && oldUser.getIdentityVerifiedSource() != null)
                ? (isIdentityAuthSource && user.getIdentityVerifiedSource() != null ? user.getIdentityVerifiedSource() : oldUser.getIdentityVerifiedSource())
                : (isIdentityAuthSource && user.getIdentityVerifiedSource() != null ? user.getIdentityVerifiedSource() : null);

        updateuserInputs.put("IsIdentityVerified", effectiveVerified);
        updateuserInputs.put("IdentityVerifiedDate", effectiveVerifiedDate);
        updateuserInputs.put("IdentityVerifiedSource", effectiveVerifiedSource);
        updateuserInputs.put("LastResyncDate", user.getLastResyncDate() != null ? user.getLastResyncDate() : (oldUser != null ? oldUser.getLastResyncDate() : null));

        int currentEditCountV2 = (oldUser != null && oldUser.getEdittedCount() != null) ? oldUser.getEdittedCount() : 0;
        int nextEditCountV2 = user.getEdittedCount() != null ? user.getEdittedCount() : (currentEditCountV2 + 1);
        updateuserInputs.put("EdittedCount", nextEditCountV2);
        updateuserInputs.put("LastLoginMethod", user.getLastLoginMethod() != null ? user.getLastLoginMethod() : (oldUser != null ? oldUser.getLastLoginMethod() : null));

        updateuserInputs.put("LastModifiedDate", new Date());
        updateuserInputs.put("LastModifiedBy", userId );

        updateAuditDetails(oldUser, userId, uuid);

        int rowsUpdated = namedParameterJdbcTemplate.update(UPDATE_USER_SQL, updateuserInputs);
        log.info("[USER-REPO-V2] Updated {} rows for user id: {}, uuid: {}", rowsUpdated, targetId, targetUuid);
        if (rowsUpdated == 0) {
            log.warn("[USER-REPO-V2] Zero rows updated for user id: {}, uuid: {}, username: {}", targetId, targetUuid, oldUser != null ? oldUser.getUsername() : null);
            throw new CustomException("DATA_MISMATCH", "Data mismatch: User with id " + targetId + " or uuid " + targetUuid + " does not exist in the database");
        }
        if (user.getRoles() != null && !CollectionUtils.isEmpty(user.getRoles()) && !oldUser.getRoles().equals(user.getRoles())) {
            validateAndEnrichRoles(Collections.singletonList(user));
            updateRoles(user);
        }
    }

    /**
     * Retrieves a list of users along with their addresses based on the provided search criteria.
     *
     * This method filters users by role codes and tenant ID if specified, constructs the appropriate query,
     * fetches user details including address information using a result set extractor, and then enriches
     * the user objects with their associated roles before returning the final user list.
     *
     * @param userSearch The criteria used to filter users.
     * @return A list of users matching the search criteria, with addresses and roles populated.
     */
    public List<User> findAllV2(UserSearchCriteria userSearch) {
        final List<Object> preparedStatementValues = new ArrayList<>();
        boolean RoleSearchHappend = false;
        List<Long> userIds = new ArrayList<>();
        if (!isEmpty(userSearch.getRoleCodes()) && userSearch.getTenantId() != null) {
            userIds = findUsersWithRole(userSearch);
            RoleSearchHappend = true;
        }
        List<User> users = new ArrayList<>();
        if (RoleSearchHappend) {
            if (!CollectionUtils.isEmpty(userIds)) {
                if (CollectionUtils.isEmpty(userSearch.getId()))
                    userSearch.setId(userIds);
                else {
                    userSearch.setId(userSearch.getId().stream().filter(userIds::contains).collect(Collectors.toList()));
                    if (CollectionUtils.isEmpty(userSearch.getId()))
                        return users;
                }
                userSearch.setTenantId(null);
                userSearch.setRoleCodes(null);
            } else {
                return users;
            }
        }
        // Getting V2 query to bring all new fields of address object in user object
        String queryStr = userTypeQueryBuilder.getQueryV2(userSearch, preparedStatementValues);
        log.debug(queryStr);
        // using new row mapper to get all new fields of address object in user object
        users = jdbcTemplate.query(queryStr, preparedStatementValues.toArray(), userResultSetExtractorV2);
        enrichRoles(users);

        return users;
    }

    /**
     * Retrieves the internal numeric user ID corresponding to the given user UUID.
     *
     * @param userUuid the unique user UUID string
     * @return numeric user ID, or {@code null} if not found or blank
     */
    public Long getUserIdByUuid(String userUuid) {
        if (StringUtils.isBlank(userUuid)) {
            return null;
        }
        try {
            String query = SELECT_USER_ID_BY_UUID_SQL;
            MapSqlParameterSource params = new MapSqlParameterSource("uuid", userUuid);
            return namedParameterJdbcTemplate.queryForObject(query, params, Long.class);
        } catch (EmptyResultDataAccessException e) {
            log.warn("No user found for uuid: {}", userUuid);
            return null;
        } catch (Exception e) {
            log.error("Error fetching user id for uuid: {}", userUuid, e);
            return null;
        }
    }

    /**
     * Fetches the tenant identifier for a given user ID.
     *
     * @param userId the numeric user ID
     * @return the tenant identifier string, or {@code null} if not found or invalid
     */
    public String getTenantIdByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            return namedParameterJdbcTemplate.queryForObject(
                    SELECT_TENANT_ID_BY_USER_ID_SQL,
                    Collections.singletonMap("id", userId),
                    String.class);
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (Exception e) {
            log.warn("Error fetching tenantId for userId: {}", userId, e);
            return null;
        }
    }

    /**
     * Fetches the tenant identifier for a given user UUID.
     *
     * @param uuid the unique user UUID string
     * @return the tenant identifier string, or {@code null} if not found or invalid
     */
    public String getTenantIdByUuid(String uuid) {
        if (StringUtils.isBlank(uuid)) {
            return null;
        }
        try {
            return namedParameterJdbcTemplate.queryForObject(
                    SELECT_TENANT_ID_BY_UUID_SQL,
                    Collections.singletonMap("uuid", uuid),
                    String.class);
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (Exception e) {
            log.warn("Error fetching tenantId for uuid: {}", uuid, e);
            return null;
        }
    }

    /**
     * Fetches the UUID string corresponding to a numeric user ID.
     *
     * @param userId the numeric user ID
     * @return the UUID string, or {@code null} if not found or invalid
     */
    public String getUuidByUserId(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            return namedParameterJdbcTemplate.queryForObject(
                    SELECT_UUID_BY_USER_ID_SQL,
                    Collections.singletonMap("id", userId),
                    String.class);
        } catch (EmptyResultDataAccessException e) {
            return null;
        } catch (Exception e) {
            log.warn("Error fetching uuid for userId: {}", userId, e);
            return null;
        }
    }

    /**
     * Helper method to save address if it has valid required fields.
     *
     * @param address     the address object to persist
     * @param savedUser   the parent user entity
     * @param addressType address classification (e.g. correspondence or permanent)
     */
    private void saveAddressIfValid(Address address, User savedUser, String addressType) {
        if (address != null && (addressMandatoryFieldsEnabled ? address.isNotEmpty() : true)) {
            log.info("Saving {} address for user: {}", addressType, savedUser.getUsername());
            Address savedAddress = saveAddress(address, savedUser.getId(), savedUser.getTenantId());
            if ("correspondence".equals(addressType)) {
                savedUser.setCorrespondenceAddress(savedAddress);
            } else if ("permanent".equals(addressType)) {
                savedUser.setPermanentAddress(savedAddress);
            }
        } else {
            log.info("Skipping {} address persistence - missing required fields (city, pincode, or address) for user: {}", 
                    addressType, savedUser.getUsername());
        }
    }

    /**
     * Helper method to save V2 address if it has valid required fields.
     *
     * @param address   the address object to persist
     * @param savedUser the parent user entity
     */
    private void saveAddressV2IfValid(Address address, User savedUser) {
        if (address != null && (addressMandatoryFieldsEnabled ? address.isNotEmpty() : true)) {
            log.info("Saving address with type: {} for user: {}", address.getType(), savedUser.getUsername());
            Address responseAddress = saveAddressV2(address, savedUser.getId(), savedUser.getTenantId());
            savedUser.addAddressItem(responseAddress);
        } else {
            log.info("Skipping address persistence for type: {} - missing required fields (city, pincode, or address) for user: {}", 
                    address != null ? address.getType() : "null", savedUser.getUsername());
        }
    }

    /**
     * Updates the user's last authenticated login method (e.g., OTP, PASSWORD, AADHAAR, FAMILY_ID).
     *
     * @param userId      the numeric ID of the user
     * @param loginMethod the authentication method identifier
     */
    public void updateLastLoginMethod(Long userId, String loginMethod) {
        if (userId == null || StringUtils.isBlank(loginMethod)) {
            return;
        }
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("id", userId);
            params.put("lastLoginMethod", loginMethod);
            namedParameterJdbcTemplate.update(UPDATE_LAST_LOGIN_METHOD_SQL, params);
            log.info("[USER-REPO] Updated last_login_method to '{}' for user ID: {}", loginMethod, userId);
        } catch (Exception e) {
            log.warn("[USER-REPO] Failed to update last_login_method for user ID {}: {}", userId, e.getMessage());
        }
    }
}
