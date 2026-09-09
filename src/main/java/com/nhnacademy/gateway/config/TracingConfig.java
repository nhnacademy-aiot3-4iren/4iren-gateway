package com.nhnacademy.gateway.config;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;

@Configuration
public class TracingConfig {

    // Zipkin(Micrometer) 추적 시 /actuator 경로는 추적 대상에서 제외하는 설정
    @Bean
    public ObservationPredicate noActuatorObservations() {
        return (name, context) -> {
            if (context instanceof ServerRequestObservationContext) {
                ServerRequestObservationContext serverContext = (ServerRequestObservationContext) context;
                String path = serverContext.getCarrier().getURI().getPath();
                // /actuator로 시작하는 요청은 추적(Tracing)하지 않음
                return !path.startsWith("/actuator");
            }
            return true;
        };
    }
}
