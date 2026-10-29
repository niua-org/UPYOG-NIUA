package org.egov.egf.instrument.persistence.queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
public class ObjectMapperFactoryTest {

    @Mock
    private ObjectMapper objectMapper;

    private ObjectMapperFactory objectMapperFactory;

    @BeforeEach
    public void setup() {
        objectMapperFactory = new ObjectMapperFactory(objectMapper);
    }

    @Test
    public void test_create() {

        ObjectMapper actualRequest = objectMapperFactory.create();

        verify(objectMapper).disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        assertEquals(objectMapper, actualRequest);

    }

}
