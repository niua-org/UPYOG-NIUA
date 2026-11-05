// generated-by: generate_per_class_unit_tests.py
package org.egov.asset.repository.rowmapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AssetRowMapperTest {
    @Mock
    private ObjectMapper objectMapper;

    private AssetRowMapper subject;

    @BeforeEach
    void setUp() {
        subject = new AssetRowMapper(objectMapper);
    }


    @Test
    void extractData_whenSubjectReady_subjectIsNotNull() {
        assertThat(subject).isNotNull();
    }

}
