package com.example.gateway.utils;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class URLProviderTest {

    @Mock
    private ResourceLoader resourceLoader;

    @Mock
    private Resource resource;

    private URLProvider urlProvider;

    @BeforeEach
    void setUp() throws Exception {
        resetStaticMaps();
        urlProvider = new URLProvider();
        setField(urlProvider, "resourceLoader", resourceLoader);
    }

    @AfterEach
    void tearDown() throws Exception {
        resetStaticMaps();
    }

    @Test
    void loadUrls_inlinePostHooksAndEmptyPreHooks_returnsPostHookMap() throws Exception {
        setField(urlProvider, "postHookUrls", "postA:http://post-a|postB:http://host:8080/hook");
        setField(urlProvider, "preHookUrls", "");

        urlProvider.loadUrls();

        assertThat(URLProvider.getUrlPostHooksMap())
                .containsEntry("postA", "http://post-a")
                .containsEntry("postB", "http://host:8080/hook");
        assertThat(URLProvider.getUrlPreHooksMap()).isEmpty();
        assertThatThrownBy(() -> URLProvider.getUrlPostHooksMap().put("extra", "http://nope"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void loadUrls_inlinePreHookConfig_overwritesPostHookMap() throws Exception {
        setField(urlProvider, "postHookUrls", "postA:http://post-a");
        setField(urlProvider, "preHookUrls", "preA:http://pre-a");

        urlProvider.loadUrls();

        assertThat(URLProvider.getUrlPreHooksMap()).containsExactlyEntriesOf(MapOf.preOnly());
        assertThat(URLProvider.getUrlPostHooksMap())
                .containsEntry("preA", "http://pre-a")
                .doesNotContainKey("postA");
    }

    @Test
    void loadUrls_inlineEntryWithoutColon_throwsArrayIndexOutOfBoundsException() throws Exception {
        setField(urlProvider, "postHookUrls", "missing-separator");
        setField(urlProvider, "preHookUrls", "");

        assertThatThrownBy(() -> urlProvider.loadUrls())
                .isInstanceOf(ArrayIndexOutOfBoundsException.class);
        assertThat(URLProvider.getUrlPostHooksMap()).isNull();
    }

    @Test
    void loadUrls_classpathJson_loadsPostHookMap() throws Exception {
        setField(urlProvider, "postHookUrls", "classpath:hooks.json");
        setField(urlProvider, "preHookUrls", "");
        when(resourceLoader.getResource("classpath:hooks.json")).thenReturn(resource);
        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(
                "{\"postA\":\"http://post-a\"}".getBytes(StandardCharsets.UTF_8)));

        urlProvider.loadUrls();

        assertThat(URLProvider.getUrlPostHooksMap()).containsEntry("postA", "http://post-a");
        assertThat(URLProvider.getUrlPreHooksMap()).isEmpty();
    }

    @Test
    void loadUrls_classpathReadFails_returnsEmptyMap() throws Exception {
        setField(urlProvider, "postHookUrls", "classpath:hooks.json");
        setField(urlProvider, "preHookUrls", "");
        when(resourceLoader.getResource("classpath:hooks.json")).thenReturn(resource);
        when(resource.getInputStream()).thenThrow(new IOException("disk"));

        urlProvider.loadUrls();

        assertThat(URLProvider.getUrlPostHooksMap()).isEmpty();
        assertThat(URLProvider.getUrlPreHooksMap()).isEmpty();
    }

    @Test
    void loadUrls_unsetProperties_returnsEmptyMaps() {
        urlProvider.loadUrls();

        assertThat(URLProvider.getUrlPostHooksMap()).isEmpty();
        assertThat(URLProvider.getUrlPreHooksMap()).isEmpty();
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = URLProvider.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void resetStaticMaps() throws Exception {
        setField(null, "urlPostHooksMap", null);
        setField(null, "urlPreHooksMap", null);
    }

    private static final class MapOf {
        private static java.util.Map<String, String> preOnly() {
            return java.util.Map.of("preA", "http://pre-a");
        }
    }
}
