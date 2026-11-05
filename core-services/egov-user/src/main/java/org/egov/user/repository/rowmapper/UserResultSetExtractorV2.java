package org.egov.user.repository.rowmapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.egov.user.domain.model.Address;
import org.egov.user.domain.model.Role;
import org.egov.user.domain.model.User;
import org.egov.user.domain.model.enums.*;
import org.egov.user.domain.service.utils.UserConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static java.util.Objects.isNull;
import static org.egov.user.domain.model.enums.AddressType.CORRESPONDENCE;
import static org.egov.user.domain.model.enums.AddressType.PERMANENT;

/**
 * Result set extractor that aggregates tabular query results into domain {@link User} entities,
 * mapping associated {@link Role} objects and extended V2 {@link Address} records.
 */
@Slf4j
@Service
public class UserResultSetExtractorV2 implements ResultSetExtractor<List<User>> {

    private ObjectMapper objectMapper;

    @Autowired
    UserResultSetExtractorV2(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Extracts SQL {@link ResultSet} rows into a grouped list of {@link User} objects with attached roles and addresses.
     *
     * @param rs the database result set
     * @return list of hydrated {@link User} instances
     * @throws SQLException        if a database access error occurs
     * @throws DataAccessException if Spring data access error occurs
     */
    @Override
    public List<User> extractData(ResultSet rs) throws SQLException, DataAccessException {

        Map<Long, User> usersMap = new LinkedHashMap<>();
        ResultSetMetaData rsMeta = rs.getMetaData();
        boolean hasAddress = IntStream.rangeClosed(1, rsMeta.getColumnCount())
                .anyMatch(i -> {
                    try {
                        return "addr_id".equalsIgnoreCase(rsMeta.getColumnLabel(i));
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });

        while (rs.next()) {

            Long userId = rs.getLong("id");
            User user;

            if (!usersMap.containsKey(userId)) {

                user = User.builder().id(rs.getLong("id")).tenantId(rs.getString("tenantid")).title(rs.getString("title"))
                        .salutation(rs.getString("salutation"))
                        .dob(rs.getDate("dob")).locale(rs.getString("locale")).username(rs.getString("username"))
                        .password(rs.getString("password")).passwordExpiryDate(rs.getTimestamp("pwdexpirydate"))
                        .mobileNumber(rs.getString("mobilenumber")).altContactNumber(rs.getString("altcontactnumber"))
                        .emailId(rs.getString("emailid")).active(rs.getBoolean("active")).name(rs.getString("name")).
                        lastModifiedBy(rs.getLong("lastmodifiedby")).lastModifiedDate(rs.getTimestamp("lastmodifieddate"))
                        .pan(rs.getString("pan")).aadhaarNumber(rs.getString("aadhaarnumber")).createdBy(rs.getLong("createdby"))
                        .createdDate(rs.getTimestamp("createddate")).guardian(rs.getString("guardian")).signature(rs.getString("signature"))
                        .accountLocked(rs.getBoolean("accountlocked")).photo(rs.getString("photo"))
                        .identificationMark(rs.getString("identificationmark")).uuid(rs.getString("uuid"))
                        .accountLockedDate(rs.getLong("accountlockeddate")).alternateMobileNumber(rs.getString("alternatemobilenumber"))
                        .build();

                for (UserType type : UserType.values()) {
                    if (type.toString().equals(rs.getString("type"))) {
                        user.setType(type);
                    }
                }


                for (BloodGroup bloodGroup : BloodGroup.values()) {
                    if (bloodGroup.toString().equals(rs.getString("bloodgroup"))) {
                        user.setBloodGroup(bloodGroup);
                    }
                }

                if (rs.getInt("gender") == 1) {
                    user.setGender(Gender.FEMALE);
                } else if (rs.getInt("gender") == 2) {
                    user.setGender(Gender.MALE);
                } else if (rs.getInt("gender") == 3) {
                    user.setGender(Gender.OTHERS);
                } else if (rs.getInt("gender") == 4) {
                    user.setGender(Gender.TRANSGENDER);
                }
                for (GuardianRelation guardianRelation : GuardianRelation.values()) {
                    if (guardianRelation.toString().equals(rs.getString("guardianrelation"))) {
                        user.setGuardianRelation(guardianRelation);
                    }
                }

                if (hasColumn(rs, "father_name")) {
                    user.setFatherName(rs.getString("father_name"));
                }
                if (hasColumn(rs, "mother_name")) {
                    user.setMotherName(rs.getString("mother_name"));
                }
                if (hasColumn(rs, "spouse_name")) {
                    user.setSpouseName(rs.getString("spouse_name"));
                }
                if (hasColumn(rs, "is_identity_verified")) {
                    user.setIsIdentityVerified(rs.getBoolean("is_identity_verified"));
                }
                if (hasColumn(rs, "identity_verified_date")) {
                    user.setIdentityVerifiedDate(rs.getTimestamp("identity_verified_date"));
                }
                if (hasColumn(rs, "identity_verified_source")) {
                    user.setIdentityVerifiedSource(rs.getString("identity_verified_source"));
                }
                if (hasColumn(rs, "last_resync_date")) {
                    user.setLastResyncDate(rs.getTimestamp("last_resync_date"));
                }
                if (hasColumn(rs, "editted_count")) {
                    user.setEdittedCount(rs.getInt("editted_count"));
                }
                if (hasColumn(rs, "last_login_method")) {
                    user.setLastLoginMethod(rs.getString("last_login_method"));
                }
                if (hasColumn(rs, "digilockerid")) {
                    user.setDigilockerid(rs.getString("digilockerid"));
                }

                usersMap.put(userId, user);

            } else {
                user = usersMap.get(userId);
            }

            Role role = populateRole(rs);
            if (!isNull(role)) {
                user.addRolesItem(role);
            }
            // Conditionally add address if the column exists and is not null
            if (hasAddress) {
                Address address = populateAddress(rs, user);
                if (!isNull(address)) {
                    user.addAddressItem(address);
                }
            }
        }

        return new ArrayList<>(usersMap.values());
    }

    private Role populateRole(ResultSet rs) throws SQLException {
        String code = rs.getString("role_code");
        if (code == null) {
            return null;
        }
        return Role.builder()
                .tenantId(rs.getString("role_tenantid"))
                .code(code)
                .build();
    }

    private Address populateAddress(ResultSet rs, User user) throws SQLException {
        if (!hasColumn(rs, "addr_id")) {
            return null;
        }
        long id = rs.getLong("addr_id");
        if (id == 0L) {
            return null;
        }

        Address address = Address.builder()
                .id(rs.getLong("addr_id"))
                .addressType(rs.getString("addr_type"))
                .type(AddressType.fromValue(rs.getString("addr_type")))
                .address(rs.getString("addr_address"))
                .city(rs.getString("addr_city"))
                .pinCode(rs.getString("addr_pincode"))
                .userId(rs.getLong("addr_userid"))
                .userUuid(user != null ? user.getUuid() : null)
                .tenantId(rs.getString("addr_tenantid"))
                .address2(rs.getString("addr_address2"))
                .houseNumber(rs.getString("addr_houseNumber"))
                .houseName(rs.getString("addr_houseName"))
                .streetName(rs.getString("addr_streetName"))
                .landmark(rs.getString("addr_landmark"))
                .locality(rs.getString("addr_locality"))
                .build();

        if (hasColumn(rs, "addr_latitude")) {
            double lat = rs.getDouble("addr_latitude");
            if (!rs.wasNull()) address.setLatitude(lat);
        }
        if (hasColumn(rs, "addr_longitude")) {
            double lon = rs.getDouble("addr_longitude");
            if (!rs.wasNull()) address.setLongitude(lon);
        }
        if (hasColumn(rs, "addr_filestore_id")) {
            address.setFilestoreId(rs.getString("addr_filestore_id"));
        }
        if (hasColumn(rs, "addr_user_uuid") && StringUtils.isNotBlank(rs.getString("addr_user_uuid"))) {
            address.setUserUuid(rs.getString("addr_user_uuid"));
        }
        if (hasColumn(rs, "addr_status") && StringUtils.isNotBlank(rs.getString("addr_status"))) {
            address.setStatus(rs.getString("addr_status").toLowerCase());
        } else {
            address.setStatus(UserConstants.ADDRESS_ACTIVE_STATUS);
        }

        return address;

    }

    private boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int columns = rsmd.getColumnCount();
        for (int x = 1; x <= columns; x++) {
            if (columnName.equalsIgnoreCase(rsmd.getColumnLabel(x)) || columnName.equalsIgnoreCase(rsmd.getColumnName(x))) {
                return true;
            }
        }
        return false;
    }
}
