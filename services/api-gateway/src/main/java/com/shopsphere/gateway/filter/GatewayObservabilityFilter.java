package com.shopsphere.gateway.filter;

import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Global filter providing HTTP request correlation, timing, trace header propagation,
 * and structured perimeter logging for API Gateway.
 */
@Component
public class GatewayObservabilityFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(GatewayObservabilityFilter.class);

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private final ObjectProvider<Tracer> tracerProvider;

    public GatewayObservabilityFilter(ObjectProvider<Tracer> tracerProvider) {
        this.tracerProvider = tracerProvider;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();

        // 1. Preserve or generate X-Request-Id and Trace Context
        String requestId = request.getHeaders().getFirst(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        final String finalRequestId = requestId;

        String traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = request.getHeaders().getFirst("X-B3-TraceId");
        }
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        final String finalTraceId = traceId;
        String spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        // 2. Mutate request to ensure downstream services receive X-Request-Id & B3 tracing headers
        ServerHttpRequest.Builder reqBuilder = request.mutate()
                .header(REQUEST_ID_HEADER, finalRequestId)
                .header(TRACE_ID_HEADER, finalTraceId);

        if (request.getHeaders().getFirst("X-B3-TraceId") == null) {
            reqBuilder.header("X-B3-TraceId", finalTraceId)
                      .header("X-B3-SpanId", spanId)
                      .header("X-B3-Sampled", "1")
                      .header("b3", finalTraceId + "-" + spanId + "-1");
        }

        ServerHttpRequest mutatedRequest = reqBuilder.build();

        String method = request.getMethod().name();
        String path = request.getURI().getPath();

        log.info("GATEWAY_REQUEST_IN method={} path={} traceId={} requestId={}", method, path, finalTraceId, finalRequestId);

        // 3. Add correlation headers to response
        exchange.getResponse().beforeCommit(() -> {
            exchange.getResponse().getHeaders().set(REQUEST_ID_HEADER, finalRequestId);
            exchange.getResponse().getHeaders().set(TRACE_ID_HEADER, finalTraceId);
            return Mono.empty();
        });

        // 4. Execute filter chain and record completion metrics & logging
        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .doFinally(signalType -> {
                    long duration = System.currentTimeMillis() - startTime;
                    HttpStatusCode status = exchange.getResponse().getStatusCode();
                    int statusCode = status != null ? status.value() : 500;

                    Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
                    String routeId = route != null ? route.getId() : "direct";

                    log.info("GATEWAY_REQUEST_OUT method={} path={} status={} durationMs={} route={} traceId={} requestId={}",
                            method, path, statusCode, duration, routeId, finalTraceId, finalRequestId);
                });
    }

    @Override
    public int getOrder() {
        // Run immediately after StripInternalHeadersGlobalFilter
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
