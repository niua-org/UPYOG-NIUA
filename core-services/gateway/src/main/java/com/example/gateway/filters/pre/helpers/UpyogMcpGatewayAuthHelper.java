package com.example.gateway.filters.pre.helpers;

import com.example.gateway.utils.UpyogMcpGatewaySupport;
import com.example.gateway.utils.UserUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.User;
import org.egov.tracer.model.CustomException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Gateway authentication for UPYOG MCP ({@code /upyog-mcp-server/**}).
 * <p>
 * Streamable HTTP MCP sends JSON-RPC (for example {@code initialize}, {@code tools/call}) with
 * <strong>no</strong> {@code RequestInfo} in the body. Standard {@link AuthCheckFilterHelper} expects
 * a business JSON body with {@code RequestInfo.authToken}, so MCP uses this helper instead:
 * <ol>
 *   <li>Read {@code Auth-Token} / {@code auth-token} from HTTP headers.</li>
 *   <li>Resolve the citizen/employee via {@link UserUtils#getUser(String)} (same as other gateway auth).</li>
 *   <li>Store {@link RequestInfo} on the exchange for RBAC ({@link UpyogMcpGatewaySupport#GATEWAY_REQUEST_INFO_ATTR}).</li>
 *   <li>Forward to MCP with {@code x-user-info} (serialized user) and {@code x-pass-through-gateway: true}
 *       so the MCP server can trust identity on hop 1.</li>
 * </ol>
 * The JSON-RPC payload is not rewritten; downstream business APIs still get {@code RequestInfo} from MCP on hop 2.
 */
@Slf4j
@Component
public class UpyogMcpGatewayAuthHelper {

    private static final String USER_INFO_HEADER = "x-user-info";
    private static final String PASS_THROUGH_HEADER = "x-pass-through-gateway";
    private static final String PASS_THROUGH_VALUE = "true";

    private final UserUtils userUtils;
    private final ObjectMapper objectMapper;

    public UpyogMcpGatewayAuthHelper(UserUtils userUtils, ObjectMapper objectMapper) {
        this.userUtils = userUtils;
        this.objectMapper = objectMapper;
    }

    /**
     * Validates the session token and attaches gateway identity headers for the MCP upstream call.
     *
     * @param exchange inbound gateway exchange (path under {@link UpyogMcpGatewaySupport#PATH_PREFIX})
     * @param chain    remaining filter chain toward {@code upyog-mcp-server}
     * @return completion when MCP route is forwarded or {@link CustomException} if token/user lookup fails
     */
    public Mono<Void> authenticateAndForward(ServerWebExchange exchange, GatewayFilterChain chain) {
        String authToken = UpyogMcpGatewaySupport.firstAuthToken(exchange.getRequest().getHeaders());
        if (ObjectUtils.isEmpty(authToken)) {
            CustomException customException = new CustomException(
                    "You are not authorized to access this resource",
                    "You are not authorized to access this resource");
            customException.setCode(HttpStatus.UNAUTHORIZED.toString());
            throw customException;
        }
        try {
            User user = userUtils.getUser(authToken);
            RequestInfo requestInfo = new RequestInfo();
            requestInfo.setAuthToken(authToken);
            requestInfo.setUserInfo(user);
            exchange.getAttributes().put(UpyogMcpGatewaySupport.GATEWAY_REQUEST_INFO_ATTR, requestInfo);

            String userJson = objectMapper.writeValueAsString(user);
            ServerWebExchange forwarded = exchange.mutate().request(builder -> builder.headers(headers -> {
                headers.set(USER_INFO_HEADER, userJson);
                headers.set(PASS_THROUGH_HEADER, PASS_THROUGH_VALUE);
                if (!headers.containsKey("Auth-Token")) {
                    headers.set("Auth-Token", authToken);
                }
            })).build();

            log.info("UPYOG MCP: authenticated user {} for {}", user.getUuid(), exchange.getRequest().getPath());
            return chain.filter(forwarded);
        } catch (CustomException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("UPYOG MCP authentication failed", exception);
            CustomException customException = new CustomException("AUTHENTICATION_ERROR", exception.getMessage());
            customException.setCode(HttpStatus.UNAUTHORIZED.toString());
            throw customException;
        }
    }
}
