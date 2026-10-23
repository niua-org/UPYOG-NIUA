package org.egov.user.repository.rowmapper;

import org.egov.user.domain.model.User;
import org.egov.user.domain.model.enums.BloodGroup;
import org.egov.user.domain.model.enums.Gender;
import org.egov.user.domain.model.enums.GuardianRelation;
import org.egov.user.domain.model.enums.UserType;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * RowMapper implementation for mapping single SQL {@link ResultSet} rows to {@link User} entities,
 * supporting new demographic and verification columns dynamically.
 */
public class UserRowMapper implements RowMapper<User> {

    /**
     * Maps the current SQL result set row to a {@link User} domain entity.
     *
     * @param rs     database result set
     * @param rowNum row index
     * @return mapped {@link User} domain entity
     * @throws SQLException if a column extraction error occurs
     */
    @Override
    public User mapRow(final ResultSet rs, final int rowNum) throws SQLException {
        final User user = User.builder().id(rs.getLong("id")).tenantId(rs.getString("tenantid")).title(rs.getString("title")).salutation(rs.getString("salutation"))
                .dob(rs.getDate("dob")).locale(rs.getString("locale")).username(rs.getString("username"))
                .password(rs.getString("password")).passwordExpiryDate(rs.getTimestamp("pwdexpirydate"))
                .mobileNumber(rs.getString("mobilenumber")).altContactNumber(rs.getString("altcontactnumber"))
                .emailId(rs.getString("emailid")).active(rs.getBoolean("active")).name(rs.getString("name")).lastModifiedBy(rs.getLong("lastmodifiedby")).lastModifiedDate(rs.getTimestamp("lastmodifieddate"))
                .pan(rs.getString("pan")).aadhaarNumber(rs.getString("aadhaarnumber")).createdBy(rs.getLong("createdby")).createdDate(rs.getTimestamp("createddate"))
                .guardian(rs.getString("guardian")).signature(rs.getString("signature"))
                .accountLocked(rs.getBoolean("accountlocked")).photo(rs.getString("photo")).identificationMark(rs.getString("identificationmark")).uuid(rs.getString("uuid")).alternateMobileNumber(rs.getString("alternatemobilenumber")).build();

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

        return user;
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
