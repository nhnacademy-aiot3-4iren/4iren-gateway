package com.nhnacademy.gateway.config;

import com.nhnacademy.gateway.filter.AuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;


// 게이트웨이의 교통정리 및 안내소
@Configuration
@RequiredArgsConstructor
public class RouterConfig {

    private final AuthFilter authFilter;

    private static final String RECOMMENDATION_URL="lb://4iren-recommendation";
    private static final String CORE_URL="lb://4iren-core";
    private static final String ACCOUNT_URL="lb://4iren-account";
    private static final String AUTH_URL="lb://4iren-auth";

    @Bean
    public RouteLocator customRouter(
            RouteLocatorBuilder builder
    ) {
        return builder.routes()

                // auth
                .route("auth-api", p -> p
                        .path("/api/auth/**")
                        .and()
                        .uri(AUTH_URL)
                )


                // 회원가입 (POST /signup) -> 인증 필터 미적용
                .route("account-signup", p -> p
                        .path("/api/account/signup")
                        .and()
                        .method(HttpMethod.POST)
                        .uri(ACCOUNT_URL)
                )

                // account 나머지 엔드포인트
                .route("account-api", p -> p
                        .path("/api/account/**")
                        .filters(f -> f.filter(authFilter))
                        .uri(ACCOUNT_URL)
                )

                // recommendation
                .route("4iren-recommendation",
                        p -> p.path("/api/recommendation/**")
                                .filters(f->f.filter(authFilter))
                                .uri(RECOMMENDATION_URL)
                )

                .route("4iren-core",
                        p->p.path("/api/core/**")
                                .filters(f->f.filter(authFilter))
                                .uri(CORE_URL)
                )

                .build();
    }
}
