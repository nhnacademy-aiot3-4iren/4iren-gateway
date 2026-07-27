package com.nhnacademy.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthFilter implements GatewayFilter {

    private static final String idHeader="X-USER-ID";
    private static final String roleHeader="X-USER-ROLE";

    private static final String ROLE_ADMIN="ROLE_ADMIN";
    private static final String ROLE_USER="ROLE_USER";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest req= exchange.getRequest().mutate()
                .header(idHeader, "1")
                .header(roleHeader, ROLE_USER)
                .build();

        return chain.filter(exchange.mutate().request(req).build());
    }
}
