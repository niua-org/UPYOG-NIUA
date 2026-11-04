package org.egov.demand.testutil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class TestResourceLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TestResourceLoader() {}

    public static String readUtf8(String classpathResource) {
        try (InputStream in = TestResourceLoader.class.getClassLoader().getResourceAsStream(classpathResource)) {
            if (in == null) {
                throw new IllegalArgumentException("Missing test resource: " + classpathResource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + classpathResource, e);
        }
    }

    public static <T> T readJson(String classpathResource, Class<T> type) {
        try {
            return MAPPER.readValue(readUtf8(classpathResource), type);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse " + classpathResource, e);
        }
    }
}
