package org.upyog.mcp.guard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.upyog.mcp.config.McpProperties;
import org.upyog.mcp.web.AuthenticatedUser;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfirmationTokenServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void bindsUserTenantAndPayloadAndRejectsExpiry() {
        ConfirmationTokenService service = service(Duration.ofMinutes(5));
        AuthenticatedUser user = user("user-1");
        ObjectNode body = objectMapper.createObjectNode().put("tenantId", "pg.citya");
        ConfirmationTokenService.Issued issued = service.issue(user, "pgr", "create", "pg.citya", body);
        ObjectNode claims = service.verify(issued.token(), user);
        assertEquals("pg.citya", claims.path("tenant").asText());
        assertEquals("user-1", claims.path("userId").asText());
        assertThrows(IllegalArgumentException.class, () -> service.verify(issued.token(), user("other")));
    }

    @Test
    void rejectsExpiredToken() {
        ConfirmationTokenService service = service(Duration.ofSeconds(-1));
        AuthenticatedUser user = user("user-1");
        ObjectNode body = objectMapper.createObjectNode().put("tenantId", "pg.citya");
        String token = service.issue(user, "pgr", "create", "pg.citya", body).token();
        assertThrows(IllegalArgumentException.class, () -> service.verify(token, user));
    }

    @Test
    void tenantMustBelongToUser() {
        TenantValidator validator = new TenantValidator();
        AuthenticatedUser user = user("user-1");
        validator.requireAllowed(user, "pg.citya");
        assertThrows(IllegalArgumentException.class, () -> validator.requireAllowed(user, "pb.amritsar"));
        assertTrue(TenantValidator.covers("pg", "pg.citya"));
    }

    private static ConfirmationTokenService service(Duration ttl) {
        McpProperties properties = new McpProperties();
        properties.setTokenSecret("01234567890123456789012345678901");
        properties.setConfirmationTtl(ttl);
        return new ConfirmationTokenService(new ObjectMapper(), properties);
    }

    private static AuthenticatedUser user(String uuid) {
        return new AuthenticatedUser(uuid, "CITIZEN", "pg", List.of("CITIZEN"), List.of("pg"), "token");
    }
}
