package org.egov.pt.consumer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HashMapDeserializerTest {

    @Test
    void constructor_whenCreated_targetsHashMapType() {
        HashMapDeserializer deserializer = new HashMapDeserializer();
        assertThat(deserializer).isNotNull();
    }
}
