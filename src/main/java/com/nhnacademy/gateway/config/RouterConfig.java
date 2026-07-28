package com.nhnacademy.gateway.config;

import com.nhnacademy.gateway.filter.AuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RouterConfig {

    private final AuthFilter authFilter;

    private static final String RECOMMENDATION_URL="lb://4iren-recommendation";
    private static final String CORE_URL="lb://4iren-core";

    @Bean
    public RouteLocator customRouter(
            RouteLocatorBuilder builder
    ) {
        return builder.routes()


                .route("4iren-recommendation",
                        p -> p.path("/api/recommendation/**")
                                .filters(f->f.filter(authFilter))
                                .uri(RECOMMENDATION_URL)
                )

                .build();
    }
}
