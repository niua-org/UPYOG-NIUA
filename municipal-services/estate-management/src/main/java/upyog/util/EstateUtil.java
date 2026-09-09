package upyog.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.egov.common.contract.models.AuditDetails;
import org.egov.tracer.model.CustomException;
import upyog.config.ServiceConstants;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class EstateUtil {
    public final static String DATE_FORMAT = "dd-MM-yyyy";

    public static Long getCurrentTimestamp() {
        return Instant.now().toEpochMilli();
    }

    public static LocalDate getCurrentDate() {
        return LocalDate.now();
    }

    public static AuditDetails getAuditDetails(String by, Boolean isCreate) {
        Long time = getCurrentTimestamp();
        if (isCreate)
            // TODO: check if we can set lastupdated details to empty
            return AuditDetails.builder().createdBy(by).lastModifiedBy(by).createdTime(time).lastModifiedTime(time)
                    .build();
        else
            return AuditDetails.builder().lastModifiedBy(by).lastModifiedTime(time).build();
    }


    public static String getRandomUUID() {
        return UUID.randomUUID().toString();
    }

    public static LocalDate parseStringToLocalDate(String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
        return LocalDate.parse(date, formatter);
    }

    public static Long minusOneDay(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static boolean isDateWithinRange(String startDate, String endDate, String bookingDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        LocalDate booking = LocalDate.parse(bookingDate);

        return (booking.isEqual(start) || booking.isAfter(start)) &&
                (booking.isEqual(end) || booking.isBefore(end));
    }


    public static boolean isDateRangeOverlap(String searchStart, String searchEnd, String bookedStart, String bookedEnd) {
        LocalDate searchStartDate = LocalDate.parse(searchStart);
        LocalDate searchEndDate = LocalDate.parse(searchEnd);
        LocalDate bookedStartDate = LocalDate.parse(bookedStart);
        LocalDate bookedEndDate = LocalDate.parse(bookedEnd);

        return !(searchStartDate.isAfter(bookedEndDate) || searchEndDate.isBefore(bookedStartDate));
    }

    public static String parseLocalDateToString(LocalDate date, String dateFormat) {
        if(dateFormat == null) {
            dateFormat = DATE_FORMAT;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
        // Format the LocalDate
        String formattedDate = date.format(formatter);
        return formattedDate;
    }

    public static AuditDetails getAuditDetails(ResultSet rs) throws SQLException {
        AuditDetails auditdetails = AuditDetails.builder().createdBy(rs.getString("createdBy"))
                .createdTime(rs.getLong("createdTime")).lastModifiedBy(rs.getString("lastModifiedBy"))
                .lastModifiedTime(rs.getLong("lastModifiedTime")).build();
        return auditdetails;
    }

    public static String beuatifyJson(Object result) {
        ObjectMapper mapper = new ObjectMapper();
        String data = null;
        try {
            data = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return data;
    }

    public static LocalDate getMonthsAgo(int month) {
        LocalDate currentDate = LocalDate.now();
        return currentDate.minusMonths(month);
    }

    /**
     * Extracts the base tenant ID before the dot from a full tenant ID string.
     * Throws a CustomException if the tenant ID format is invalid.
     *
     * @param fullTenantId full tenant ID in the format "state.city"
     * @return the base tenant ID (before the dot)
     */
    public static String extractBaseTenantId(String fullTenantId) {
        if (fullTenantId == null || fullTenantId.trim().isEmpty()) {
            throw new CustomException(ServiceConstants.EstateConstants.INVALID_TENANT, "Tenant ID cannot be null or empty");
        }

        String[] parts = fullTenantId.split("\\.");
        if (parts.length < 2) {
            throw new CustomException(ServiceConstants.EstateConstants.INVALID_TENANT, "Tenant ID must be in the format 'state.city'");
        }

        return parts[0];
    }
}
