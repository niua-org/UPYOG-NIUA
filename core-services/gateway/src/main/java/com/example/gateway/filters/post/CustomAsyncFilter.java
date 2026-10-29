package com.example.gateway.filters.post;

import com.example.gateway.metrics.GatewayCpuFixMetrics;
import com.example.gateway.model.CustomAsyncRequest;
import com.example.gateway.utils.UserAuthCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.egov.common.contract.request.RequestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

import static com.example.gateway.constants.GatewayConstants.ORIGINAL_REQUEST_BODY;
import static com.example.gateway.constants.GatewayConstants.REQUEST_INFO_FIELD_NAME_PASCAL_CASE;
import static com.example.gateway.utils.CommonUtils.isCustomAsyncUri;

@Slf4j
@Component
public class CustomAsyncFilter implements GlobalFilter, Ordered {

    private static final String LOG_PREFIX = "GATEWAY_CPU_FIX";

    @Value("#{'${egov.custom.async.uris}'.split(',')}")
    private List<String> sourceUri;

    @Value("${egov.custom.async.filter.topic}")
    private String topic;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private UserAuthCache userAuthCache;

    @Autowired
    private GatewayCpuFixMetrics gatewayCpuFixMetrics;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestPath = exchange.getRequest().getURI().getPath();
        if (!isCustomAsyncUri(requestPath, sourceUri)) {
            gatewayCpuFixMetrics.recordCustomAsyncSkipped();
            return chain.filter(exchange);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> requestBody = (Map<String, Object>) exchange.getAttributes().get(ORIGINAL_REQUEST_BODY);

        invalidateUserCacheForLogout(requestBody);

        CustomAsyncRequest customAsyncRequest = CustomAsyncRequest.builder()
                .sourceUri(requestPath)
                .queryParamMap(exchange.getRequest().getQueryParams())
                .request(requestBody)
                .build();

        gatewayCpuFixMetrics.recordCustomAsyncPublishAttempt();

        // Fire-and-forget: run blocking Kafka send off the Netty event loop (boundedElastic), same as Zuul's servlet thread model.
        Mono.fromRunnable(() -> {
            String workerThread = Thread.currentThread().getName();
            log.info("{}: CustomAsyncFilter publishing path={} topic={} thread={}",
                    LOG_PREFIX, requestPath, topic, workerThread);
            try {
                kafkaTemplate.send(topic, customAsyncRequest).whenComplete((result, ex) -> {
                    if (ex != null) {
                        gatewayCpuFixMetrics.recordCustomAsyncPublishFailure();
                        log.info("{}: CustomAsyncFilter publish failed path={} topic={} thread={} error={}",
                                LOG_PREFIX, requestPath, topic, Thread.currentThread().getName(), ex.getMessage());
                    } else {
                        gatewayCpuFixMetrics.recordCustomAsyncPublishSuccess();
                        log.info(
                                "{}: CustomAsyncFilter publish succeeded path={} topic={} thread={} partition={} offset={}",
                                LOG_PREFIX,
                                requestPath,
                                topic,
                                Thread.currentThread().getName(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
            } catch (Exception ex) {
                gatewayCpuFixMetrics.recordCustomAsyncPublishFailure();
                log.info("{}: CustomAsyncFilter publish failed path={} topic={} thread={} error={}",
                        LOG_PREFIX, requestPath, topic, workerThread, ex.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic()).subscribe();

        return chain.filter(exchange);
    }

    private void invalidateUserCacheForLogout(Map<String, Object> requestBody) {
        if (requestBody == null || !requestBody.containsKey(REQUEST_INFO_FIELD_NAME_PASCAL_CASE)) {
            return;
        }
        try {
            RequestInfo requestInfo = objectMapper.convertValue(
                    requestBody.get(REQUEST_INFO_FIELD_NAME_PASCAL_CASE), RequestInfo.class);
            String authToken = requestInfo.getAuthToken();
            if (userAuthCache.invalidate(authToken)) {
                log.info("{}: user auth cache invalidated on logout tokenSuffix={}",
                        LOG_PREFIX, maskTokenSuffix(authToken));
            }
        } catch (Exception e) {
            log.debug("Could not invalidate user auth cache on logout: {}", e.getMessage());
        }
    }

    static String maskTokenSuffix(String token) {
        if (token == null || token.isEmpty()) {
            return "n/a";
        }
        if (token.length() <= 4) {
            return "****";
        }
        return "****" + token.substring(token.length() - 4);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE - 2;
    }
}
