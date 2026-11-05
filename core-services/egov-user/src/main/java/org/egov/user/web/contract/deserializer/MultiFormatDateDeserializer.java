package org.egov.user.web.contract.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Flexible Date deserializer supporting Unix epoch timestamp (milliseconds),
 * ISO date formats (yyyy-MM-dd), standard Indian format (dd/MM/yyyy, dd-MM-yyyy),
 * and datetime stamps.
 */
@Slf4j
public class MultiFormatDateDeserializer extends JsonDeserializer<Date> {

    private static final String[] DATE_PATTERNS = new String[] {
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "yyyy-MM-dd HH:mm:ss",
            "dd-MM-yyyy HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX"
    };

    /**
     * Deserializes JSON token value into a {@link Date} object supporting timestamps,
     * numeric strings, and standard date/datetime formatting patterns.
     *
     * @param p    the Jackson {@link JsonParser}
     * @param ctxt the deserialization context
     * @return the parsed {@link Date}, or {@code null} if empty/unparseable
     * @throws IOException if parser error occurs during value reading
     */
    @Override
    public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken currentToken = p.getCurrentToken();
        if (currentToken == JsonToken.VALUE_NUMBER_INT) {
            return new Date(p.getLongValue());
        }
        if (currentToken == JsonToken.VALUE_STRING) {
            String dateStr = p.getText().trim();
            if (StringUtils.isBlank(dateStr)) {
                return null;
            }
            if (StringUtils.isNumeric(dateStr)) {
                try {
                    return new Date(Long.parseLong(dateStr));
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse numeric timestamp: {}", dateStr);
                }
            }
            for (String pattern : DATE_PATTERNS) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                    sdf.setLenient(false);
                    return sdf.parse(dateStr);
                } catch (ParseException ignored) {
                }
            }
            log.warn("Unable to parse date string '{}' with supported formats", dateStr);
        }
        return null;
    }
}
