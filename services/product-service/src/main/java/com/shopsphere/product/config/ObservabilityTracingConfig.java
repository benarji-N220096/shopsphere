package com.shopsphere.product.config;

import brave.Tracing;
import brave.context.slf4j.MDCScopeDecorator;
import brave.handler.MutableSpan;
import brave.handler.SpanHandler;
import brave.propagation.CurrentTraceContext;
import brave.propagation.ThreadLocalCurrentTraceContext;
import brave.propagation.TraceContext;
import brave.propagation.TraceContextOrSamplingFlags;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.brave.bridge.BraveBaggageManager;
import io.micrometer.tracing.brave.bridge.BraveCurrentTraceContext;
import io.micrometer.tracing.brave.bridge.BraveTracer;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import zipkin2.reporter.brave.MutableSpanBytesEncoder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ObservabilityTracingConfig {

    private static final Logger log = LoggerFactory.getLogger(ObservabilityTracingConfig.class);

    @Value("${spring.application.name:product-service}")
    private String applicationName;

    @Value("${management.zipkin.tracing.endpoint:http://localhost:9411/api/v2/spans}")
    private String zipkinEndpoint;

    @Bean
    @ConditionalOnMissingBean
    public CurrentTraceContext braveCurrentTraceContext() {
        return ThreadLocalCurrentTraceContext.newBuilder()
                .addScopeDecorator(MDCScopeDecorator.get())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public SpanHandler zipkinSpanHandler() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "zipkin-" + applicationName);
            t.setDaemon(true);
            return t;
        });

        return new SpanHandler() {
            @Override
            public boolean end(TraceContext context, MutableSpan span, Cause cause) {
                executor.submit(() -> {
                    try {
                        byte[] bytes = MutableSpanBytesEncoder.JSON_V2.encode(span);
                        byte[] payload = new byte[bytes.length + 2];
                        payload[0] = '[';
                        System.arraycopy(bytes, 0, payload, 1, bytes.length);
                        payload[payload.length - 1] = ']';

                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(zipkinEndpoint))
                                .timeout(Duration.ofSeconds(2))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                                .build();

                        httpClient.send(request, HttpResponse.BodyHandlers.discarding());
                    } catch (Exception ignored) {
                    }
                });
                return true;
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public Tracing braveTracing(CurrentTraceContext currentTraceContext, SpanHandler spanHandler) {
        return Tracing.newBuilder()
                .localServiceName(applicationName)
                .currentTraceContext(currentTraceContext)
                .addSpanHandler(spanHandler)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public Tracer micrometerTracer(Tracing tracing, CurrentTraceContext currentTraceContext) {
        brave.Tracer braveTracer = tracing.tracer();
        BraveCurrentTraceContext bridgeContext = new BraveCurrentTraceContext(currentTraceContext);
        BraveBaggageManager baggageManager = new BraveBaggageManager();
        return new BraveTracer(braveTracer, bridgeContext, baggageManager);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public Filter serverTracingFilter(Tracing tracing) {
        brave.Tracer tracer = tracing.tracer();
        return new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
                if (!(request instanceof HttpServletRequest httpRequest) || !(response instanceof HttpServletResponse httpResponse)) {
                    chain.doFilter(request, response);
                    return;
                }

                TraceContextOrSamplingFlags flags = tracing.propagation().extractor(HttpServletRequest::getHeader).extract(httpRequest);
                brave.Span span = flags.context() != null ? tracer.newChild(flags.context()) : tracer.nextSpan(flags);
                span.name(httpRequest.getMethod() + " " + httpRequest.getRequestURI());
                span.tag("http.method", httpRequest.getMethod());
                span.tag("http.path", httpRequest.getRequestURI());

                String reqId = httpRequest.getHeader("X-Request-Id");
                if (reqId != null) {
                    span.tag("request_id", reqId);
                    httpResponse.setHeader("X-Request-Id", reqId);
                }
                httpResponse.setHeader("X-Trace-Id", span.context().traceIdString());

                org.slf4j.MDC.put("traceId", span.context().traceIdString());
                org.slf4j.MDC.put("spanId", Long.toHexString(span.context().spanId()));
                try (brave.Tracer.SpanInScope ws = tracer.withSpanInScope(span.start())) {
                    chain.doFilter(request, response);
                    span.tag("http.status_code", String.valueOf(httpResponse.getStatus()));
                } catch (Exception ex) {
                    span.error(ex);
                    throw ex;
                } finally {
                    span.finish();
                    org.slf4j.MDC.remove("traceId");
                    org.slf4j.MDC.remove("spanId");
                }
            }
        };
    }
}
