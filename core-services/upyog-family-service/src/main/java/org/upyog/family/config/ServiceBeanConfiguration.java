package org.upyog.family.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Spring Bean definitions for RestTemplate and Jackson ObjectMapper for UPYOG services.
 *
 * @author UPYOG Team
 * @since 1.0.0
 */
@Configuration
public class ServiceBeanConfiguration {

    /**
     * Configures the primary RestTemplate bean for UPYOG inter-service HTTP communications.
     *
     * @return configured RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * Configures the primary ObjectMapper bean for JSON serialization and deserialization in UPYOG.
     * Disables FAIL_ON_UNKNOWN_PROPERTIES to maintain forward and backward contract compatibility.
     *
     * @return configured ObjectMapper instance
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }
}
