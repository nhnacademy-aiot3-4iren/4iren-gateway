package com.nhnacademy.gateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RouterConfig {

    private static final String ROLE_ADMIN="ROLE_ADMIN";
    private static final String ROLE_USER="ROLE_USER";

    private static final String RECOMMENDATION_URL="lb://4iren-recommendation";

    @Bean
    public RouteLocator customRouter(
            RouteLocatorBuilder builder
    ) {
        return builder.routes()


                .route("4iren-recommendation",
                        p -> p.path("/api/recommendation/**")
                                .uri(RECOMMENDATION_URL)
                )

                .build();
    }
}
