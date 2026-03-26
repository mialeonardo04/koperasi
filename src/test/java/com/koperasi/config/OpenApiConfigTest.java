package com.koperasi.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OpenApiConfigTest {

    @Test
    void openAPI_setsServersWhenBaseUrlProvided() {
        OpenApiConfig config = new OpenApiConfig();
        var openApi = config.openAPI("http://localhost:8080/api");

        assertNotNull(openApi.getServers());
        assertEquals(1, openApi.getServers().size());
        assertEquals("http://localhost:8080/api", openApi.getServers().getFirst().getUrl());
    }

    @Test
    void openAPI_doesNotSetServersWhenBaseUrlBlank() {
        OpenApiConfig config = new OpenApiConfig();
        var openApi = config.openAPI("  ");

        assertTrue(openApi.getServers() == null || openApi.getServers().isEmpty());
    }
}
