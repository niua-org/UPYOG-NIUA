package org.egov.user.repository.builder;

import org.springframework.stereotype.Component;

/**
 * SQL Query Builder component for audit log operations on {@code eg_user_audit_table}.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Component
public class AuditQueryBuilder {

    public static final String INSERT_AUDIT_DETAILS =
            "insert into eg_user_audit_table (" +
            "id, uuid, tenantid, salutation, dob, locale, username, password, pwdexpirydate, " +
            "mobilenumber, altcontactnumber, emailid, active, name, gender, pan, aadhaarnumber, " +
            "type, guardian, guardianrelation, signature, accountlocked, bloodgroup, photo, identificationmark, " +
            "auditcreatedby, auditcreatedtime, father_name, mother_name, spouse_name, " +
            "is_identity_verified, identity_verified_date, last_resync_date, editted_count, last_login_method" +
            ") values (" +
            ":id, :uuid, :tenantid, :salutation, :dob, :locale, :username, :password, :pwdexpirydate, " +
            ":mobilenumber, :alternatemobilenumber, :emailid, :active, :name, :gender, :pan, :aadhaarnumber, " +
            ":type, :guardian, :guardianrelation, :signature, :accountlocked, :bloodgroup, :photo, :identificationmark, " +
            ":auditcreatedby, :auditcreatedtime, :father_name, :mother_name, :spouse_name, " +
            ":is_identity_verified, :identity_verified_date, :last_resync_date, :editted_count, :last_login_method" +
            ")";

    /**
     * Returns the insert SQL query for persisting user audit details.
     *
     * @return SQL insert query string
     */
    public String getInsertAuditDetailsQuery() {
        return INSERT_AUDIT_DETAILS;
    }
}
