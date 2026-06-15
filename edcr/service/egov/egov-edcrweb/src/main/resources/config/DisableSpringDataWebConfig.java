package org.egov.edcr.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class DisableSpringDataWebConfig implements WebMvcConfigurer {
    // Do nothing → prevents DomainClassConverter registration
}