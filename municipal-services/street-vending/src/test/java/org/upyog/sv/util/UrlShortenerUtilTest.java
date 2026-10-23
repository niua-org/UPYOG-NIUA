// generated-by: generate_per_class_unit_tests.py
package org.upyog.sv.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class UrlShortenerUtilTest {
    @Mock
    private RestTemplate restTemplate;

    private UrlShortenerUtil subject;

    @BeforeEach
    void setUp() {
        subject = new UrlShortenerUtil(restTemplate);
    }


    @Test
    void getShortenedUrl_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
