package com.shopsphere.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StripInternalHeadersGlobalFilterTest {

    private final StripInternalHeadersGlobalFilter filter = new StripInternalHeadersGlobalFilter();

    @Test
    void filter_stripsUntrustedInternalHeaders() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/cart")
                .header("X-User-Id", "11111111-1111-1111-1111-111111111111")
                .header("X-Internal-Service", "attacker-service")
                .header("X-Internal-Secret", "stolen-secret")
                .header("Authorization", "Bearer mock-token")
                .build();

        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any(ServerWebExchange.class))).thenAnswer(invocation -> {
            ServerWebExchange passedExchange = invocation.getArgument(0);
            assertNull(passedExchange.getRequest().getHeaders().getFirst("X-User-Id"));
            assertNull(passedExchange.getRequest().getHeaders().getFirst("X-Internal-Service"));
            assertNull(passedExchange.getRequest().getHeaders().getFirst("X-Internal-Secret"));
            assertNotNull(passedExchange.getRequest().getHeaders().getFirst("Authorization"));
            return Mono.empty();
        });

        filter.filter(exchange, chain).block();

        verify(chain, times(1)).filter(any(ServerWebExchange.class));
    }
}
