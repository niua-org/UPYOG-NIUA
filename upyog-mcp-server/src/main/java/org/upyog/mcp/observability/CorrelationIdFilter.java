package org.upyog.mcp.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.upyog.mcp.client.RequestInfoBuilder;

import java.io.IOException;
import java.util.UUID;

/**
 * Copies or creates {@code x-correlation-id} and stores it in MDC as {@code CORRELATION_ID}.
 * The same value is returned to the caller, written on {@code RequestInfo.correlationId},
 * and forwarded to the gateway.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    /** Incoming or outgoing correlation header echoed on every HTTP response. */
    public static final String HEADER = "x-correlation-id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = request.getHeader(HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(RequestInfoBuilder.CORRELATION_MDC, correlationId);
        response.setHeader(HEADER, correlationId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(RequestInfoBuilder.CORRELATION_MDC);
        }
    }
}
