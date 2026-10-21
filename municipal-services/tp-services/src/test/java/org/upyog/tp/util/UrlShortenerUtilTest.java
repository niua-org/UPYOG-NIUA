// generated-by: generate_per_class_unit_tests.py
package org.upyog.tp.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.upyog.tp.config.TreePruningConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class UrlShortenerUtilTest {
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private TreePruningConfiguration config;

    private UrlShortenerUtil subject;

    @BeforeEach
    void setUp() {
        subject = new UrlShortenerUtil(restTemplate, config);
    }


    @Test
    void getShortenedUrl_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
