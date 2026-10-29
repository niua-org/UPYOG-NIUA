package org.egov.user.repository.builder;

import org.egov.user.domain.model.AddressSearchCriteria;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Query builder component for generating SQL queries targeting the {@code eg_user_address} table.
 */
@Component
public class AddressQueryBuilder {

    public static final String GET_ADDRESSBY_IDAND_TENANT = "select * from eg_user_address where userid=:userId and tenantid=:tenantId";

    public static final String GET_ADDRESS_BY_USERID = "select * from eg_user_address where userid=:userId and tenantid =:tenantId and status=:status";

    public static final String GET_ADDRESS_BY_USERID_AND_STATUS = "select * from eg_user_address where userid=:userId and status=:status";

    public static final String INSERT_ADDRESS_BYUSERID = "insert into eg_user_address (id,type,address,city,pincode,userid,tenantid,createddate,lastmodifieddate,createdby,lastmodifiedby) "
            + "values(:id,:type,:address,:city,:pincode,:userid,:tenantid,:createddate,:lastmodifieddate,:createdby,:lastmodifiedby)";

    public static final String SELECT_NEXT_SEQUENCE = "select nextval('seq_eg_user_address')";

    public static final String DELETE_ADDRESSES = "delete from eg_user_address where id IN (:id)";

    public static final String DELETE_ADDRESS_BY_ID = "delete from eg_user_address where id=:id";

    public static final String UPDATE_ADDRESS_BYIDAND_TENANTID = "update eg_user_address set address=:address,city=:city,pincode=:pincode,lastmodifiedby=:lastmodifiedby,lastmodifieddate=:lastmodifieddate where userid=:userid and tenantid=:tenantid and type=:type";

    public static final String SELECT_USER_UUID_BY_USER_ID = "SELECT uuid FROM eg_user WHERE id = ?";

    public static final String DELETE_ADDRESS = "delete from eg_user_address where id IN (:id) ";

    // Adding new fields in Address select query: address2, houseNumber, houseName, streetName, landmark, locality, version, latitude, longitude, filestore_id, user_uuid
    private static final String GET_ADDRESS_BY_USER_UUID_V2 = "select uadd.id as id, uadd.type as type, uadd.address as address, uadd.city as city, uadd.pincode as pincode, uadd.userid as userid,\n" +
            "uadd.tenantid as tenantid, uadd.createddate as createddate, uadd.lastmodifieddate as lastmodifieddate, uadd.createdby as createdby,\n" +
            "uadd.address2, uadd.houseNumber, uadd.houseName, uadd.streetName, uadd.landmark, uadd.locality, uadd.status as status, uadd.version, uadd.latitude, uadd.longitude, uadd.filestore_id, " +
            "uadd.lastmodifiedby as lastmodifiedby, coalesce(uadd.user_uuid, usr.uuid) as user_uuid from eg_user_address uadd \n" +
            "left join eg_user usr on uadd.userid = usr.id \n";
    // Adding new fields in Address insert query: address2, houseNumber, houseName, streetName, landmark, locality, version, latitude, longitude, filestore_id, user_uuid
    public static final String INSERT_ADDRESS_BYUSERID_V2 = "insert into eg_user_address (id,type,address,city,pincode,userid,user_uuid,tenantid,createddate,lastmodifieddate,createdby,lastmodifiedby, address2, houseNumber, houseName, streetName, landmark, locality, status, version, latitude, longitude, filestore_id) "
            + "values(:id,:type,:address,:city,:pincode,:userid,:user_uuid,:tenantid,:createddate,:lastmodifieddate,:createdby,:lastmodifiedby, :address2, :houseNumber, :houseName, :streetName, :landmark, :locality, :status, :version, :latitude, :longitude, :filestore_id)";

    // To update the address status with last modified date and last modified by
    public static final String UPDATE_ADDRESS_STATUS = "update eg_user_address set status=:status, lastmodifieddate=:lastmodifieddate, lastmodifiedby=:lastmodifiedby where id=:addressid";

    // To update address details directly in place by id
    public static final String UPDATE_ADDRESS_BY_ID_V2 = "update eg_user_address set "
            + "type = :type, "
            + "address = :address, "
            + "city = :city, "
            + "pincode = :pincode, "
            + "tenantid = :tenantid, "
            + "address2 = :address2, "
            + "houseNumber = :houseNumber, "
            + "houseName = :houseName, "
            + "streetName = :streetName, "
            + "landmark = :landmark, "
            + "locality = :locality, "
            + "status = :status, "
            + "latitude = :latitude, "
            + "longitude = :longitude, "
            + "filestore_id = :filestore_id, "
            + "lastmodifieddate = :lastmodifieddate, "
            + "lastmodifiedby = :lastmodifiedby, "
            + "version = coalesce(version, 0) + 1 "
            + "where id = :id";

    /**
     * Constructs a parameterized SQL select query to search addresses matching {@link AddressSearchCriteria}.
     *
     * @param addressSearchCriteria  search filtering options
     * @param prepareStatementValues list to accumulate prepared statement argument values
     * @return parameterized SQL query string
     */
    public String getAddressQuery(AddressSearchCriteria addressSearchCriteria, final List<Object> prepareStatementValues) {
        final StringBuilder selectQuery;
        selectQuery = new StringBuilder(GET_ADDRESS_BY_USER_UUID_V2);
        addWhereClause(addressSearchCriteria, selectQuery, prepareStatementValues);

        return selectQuery.toString();
    }

    private void addWhereClause(AddressSearchCriteria addressSearchCriteria, StringBuilder selectQuery, List<Object> prepareStatementValues) {

        if (addressSearchCriteria.getUserUuid() != null) {
            addClauseIfRequired(prepareStatementValues, selectQuery);
            selectQuery.append(" (uadd.user_uuid = ? OR uadd.userid = (select id from eg_user where uuid = ?)) ");
            prepareStatementValues.add(addressSearchCriteria.getUserUuid());
            prepareStatementValues.add(addressSearchCriteria.getUserUuid());
        } else if (addressSearchCriteria.getUserId() != null) {
            addClauseIfRequired(prepareStatementValues, selectQuery);
            selectQuery.append(" uadd.userid = ?");
            prepareStatementValues.add(addressSearchCriteria.getUserId());
        }
        if (addressSearchCriteria.getTenantId() != null) {
            addClauseIfRequired(prepareStatementValues, selectQuery);
            if (addressSearchCriteria.getTenantId().contains(".")) {
                selectQuery.append(" (uadd.tenantid = ? OR uadd.tenantid = ?)");
                prepareStatementValues.add(addressSearchCriteria.getTenantId());
                prepareStatementValues.add(addressSearchCriteria.getTenantId().split("\\.")[0]);
            } else {
                selectQuery.append(" (uadd.tenantid = ? OR uadd.tenantid LIKE ?)");
                prepareStatementValues.add(addressSearchCriteria.getTenantId());
                prepareStatementValues.add(addressSearchCriteria.getTenantId() + ".%");
            }
        }
        if (addressSearchCriteria.getAddressType() != null) {
            addClauseIfRequired(prepareStatementValues, selectQuery);
            selectQuery.append(" uadd.type = ?");
            prepareStatementValues.add(addressSearchCriteria.getAddressType().toString()); // Convert enum to String
        }
        if (addressSearchCriteria.getCity() != null) {
            addClauseIfRequired(prepareStatementValues, selectQuery);
            selectQuery.append(" uadd.city = ?");
            prepareStatementValues.add(addressSearchCriteria.getCity());
        }
        if (addressSearchCriteria.getPinCode() != null) {
            addClauseIfRequired(prepareStatementValues, selectQuery);
            selectQuery.append(" uadd.pincode = ?");
            prepareStatementValues.add(addressSearchCriteria.getPinCode());
        }
        if (addressSearchCriteria.getId() != null) {
            addClauseIfRequired(prepareStatementValues, selectQuery);
            selectQuery.append(" uadd.id = ?");
            prepareStatementValues.add(addressSearchCriteria.getId());
        }
        if (addressSearchCriteria.getStatus() != null) {
            addClauseIfRequired(prepareStatementValues, selectQuery);
            selectQuery.append(" LOWER(uadd.status) = ?");
            prepareStatementValues.add(addressSearchCriteria.getStatus().trim().toLowerCase());
        }

    }

    /**
     * add if clause to the Statement if required or else AND
     *
     * @param values
     * @param queryString
     */
    private void addClauseIfRequired(List<Object> values, StringBuilder queryString) {
        if (values.isEmpty())
            queryString.append(" WHERE ");
        else {
            queryString.append(" AND ");
        }
    }

}
