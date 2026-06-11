package org.egov.pg.service.gateways.icici;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.egov.pg.constants.PgConstants;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class ICICIUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static String getCurrentTxnDate() {

        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    /*============================================ GENERATE PLAIN HASH TEXT==============*/
    public static  String generatePlainHashText(Object request) {

        try {

            Map<String, Object> map = OBJECT_MAPPER.convertValue(request, TreeMap.class);

            map.remove("secureHash");

            StringBuilder plainHash = new StringBuilder();

            for (Map.Entry<String, Object> entry : map.entrySet()) {

                Object value = entry.getValue();

                if (value != null) {
                    plainHash.append(value);
                }
            }

            return plainHash.toString();

        } catch (Exception ex) {

            throw new RuntimeException("Failed to generate plain hash text", ex);
        }
    }


    /*============================================ HMAC SHA256==============*/
    public static String generateSecureHash(Object request, String secretKey) {

        try {

            // Convert request object to map
            Map<String, Object> requestMap = OBJECT_MAPPER.convertValue(request, TreeMap.class);

            // Remove secureHash field if already present
            requestMap.remove("secureHash");

            // Create plain hash text
            StringBuilder plainHashText = new StringBuilder();

            for (Map.Entry<String, Object> entry : requestMap.entrySet()) {

                Object value = entry.getValue();

                if (value != null) {
                    plainHashText.append(value);
                }
            }

            log.info("ICICI Plain Hash Text : {}", plainHashText);

            // Generate HMAC SHA256
            Mac mac = Mac.getInstance(PgConstants.HMAC_SHA256);

            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), PgConstants.HMAC_SHA256);

            mac.init(keySpec);

            byte[] hashBytes = mac.doFinal(plainHashText.toString().getBytes(StandardCharsets.UTF_8));

            String secureHash = HexFormat.of().formatHex(hashBytes);

            log.info("ICICI Secure Hash : {}", secureHash);

            return secureHash;

        } catch (Exception ex) {

            log.error("Failed to generate secure hash", ex);

            throw new RuntimeException("Failed to generate secure hash", ex);
        }
    }
}