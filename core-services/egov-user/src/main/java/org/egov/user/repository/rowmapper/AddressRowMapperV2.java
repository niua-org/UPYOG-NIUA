package org.egov.user.repository.rowmapper;

import org.egov.user.domain.model.Address;
import org.egov.user.domain.model.enums.AddressType;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * RowMapper implementation for mapping SQL {@link ResultSet} rows to V2 {@link Address} domain models,
 * checking column availability dynamically for backward-compatible schema queries.
 */
@Component
public class AddressRowMapperV2 implements RowMapper<Address> {

    /**
     * Maps the current row of the {@link ResultSet} to an {@link Address} domain instance.
     *
     * @param rs     the database result set
     * @param rowNum current row index
     * @return mapped {@link Address} object
     * @throws SQLException if a SQL exception occurs during column extraction
     */
    @Override
    public Address mapRow(final ResultSet rs, final int rowNum) throws SQLException {

        final Address address = Address.builder().id(rs.getLong("id")).addressType(rs.getString("type")).address(rs.getString("address"))
                .city(rs.getString("city")).pinCode(rs.getString("pincode")).userId(rs.getLong("userid")).tenantId(rs.getString("tenantid"))
                .address2(rs.getString("address2")).houseNumber(rs.getString("houseNumber")).houseName(rs.getString("houseName"))
                .streetName(rs.getString("streetName")).landmark(rs.getString("landmark")).locality(rs.getString("locality")).build();

        for (AddressType addressType : AddressType.values()) {
            if (addressType.toString().equals(rs.getString("type"))) {
                address.setType(addressType);
            }
        }

        if (hasColumn(rs, "user_uuid")) {
            address.setUserUuid(rs.getString("user_uuid"));
        }

        if (hasColumn(rs, "version")) {
            long ver = rs.getLong("version");
            if (!rs.wasNull()) address.setVersion(ver);
        }

        if (hasColumn(rs, "latitude")) {
            double lat = rs.getDouble("latitude");
            if (!rs.wasNull()) address.setLatitude(lat);
        }

        if (hasColumn(rs, "longitude")) {
            double lon = rs.getDouble("longitude");
            if (!rs.wasNull()) address.setLongitude(lon);
        }

        if (hasColumn(rs, "filestore_id")) {
            address.setFilestoreId(rs.getString("filestore_id"));
        }

        if (hasColumn(rs, "status")) {
            String status = rs.getString("status");
            if (status != null) address.setStatus(status.toLowerCase());
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


