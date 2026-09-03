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
    private static final String NOTIFICATION_URL="lb://4iren-notification";
    private static final String PAYMENT_URL="lb://4iren-payment";
    private static final String PROCESSING_URL = "lb://4iren-processing";
    private static final String RULE_URL = "lb://4iren-rule-engine";

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


                // 인증 필터 미적용 API (회원가입, 비밀번호 찾기 등)
                .route("account-open-api", p -> p
                        .path("/api/account/signup/**", "/api/account/reset-password/**")
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

                .route("4iren-notification",
                        p->p.path("/api/notification/**")
                                .filters(f->f.filter(authFilter))
                                .uri(NOTIFICATION_URL)
                )

                // 가격 조회 - 로그인 안 해도 요금제를 볼 수 있어야 함 -> 인증 필터 미적용
                .route("payment-plans", p -> p
                        .path("/api/payment/plans")
                        .and()
                        .method(HttpMethod.GET)
                        .uri(PAYMENT_URL)
                )

                // 토스 콜백 - 토스 서버가 직접 호출(서버-투-서버, 우리 인증 토큰 없음) -> 인증 필터 미적용
                .route("payment-toss-callback", p -> p
                        .path("/api/payment/billing-keys/toss/callback")
                        .and()
                        .method(HttpMethod.POST)
                        .uri(PAYMENT_URL)
                )

                // 카카오 콜백 - 사용자 브라우저가 리다이렉트되어 오는 요청이라 세션 쿠키가 있어서 인증 필터 그대로 적용.
                // 토스 콜백 라우트와 나란히 명시해서 콜백 두 개가 대칭적으로 보이게 함(동작은 아래 4iren-payment 와일드카드와 동일).
                .route("payment-kakao-callback", p -> p
                        .path("/api/payment/billing-keys/kakao/callback")
                        .and()
                        .method(HttpMethod.GET)
                        .filters(f -> f.filter(authFilter))
                        .uri(PAYMENT_URL)
                )

                .route("4iren-payment",
                        p->p.path("/api/payment/**")
                                .filters(f->f.filter(authFilter))
                                .uri(PAYMENT_URL)
                )

                .route("4iren-processing",
                        p -> p.path("/api/processing/**")
                                .filters(f -> f.filter(authFilter))
                                .uri(PROCESSING_URL)
                )

                .route("4iren-rule",
                        p -> p.path("/api/rule/**")
                                .filters(f -> f.filter(authFilter))
                                .uri(RULE_URL)
                )

                .build();
    }
}
