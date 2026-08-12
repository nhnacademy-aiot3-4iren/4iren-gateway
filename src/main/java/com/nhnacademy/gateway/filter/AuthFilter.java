package com.nhnacademy.gateway.filter;

import io.netty.util.internal.StringUtil;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


/** 토큰을 확인해서 지저분하거나 가짜면 401로 튕겨내고,
 * 정상이면 사용자정보(userId,loginId,role)를 꺼내서 헤더에 얹어,
 * 서비스로 패스해주는 검문소같은 클래스
 */

@Component
public class AuthFilter implements GatewayFilter {

    //자주 사용할 헤더이름과 권한을 미리 정의하여 오타 방지
    private static final String idHeader="X-USER-ID";
    private static final String loginIdHeader="X-USER-LOGINID";
    private static final String roleHeader="X-USER-ROLE";

    private static final String ROLE_ADMIN="ADMIN";
    private static final String ROLE_USER="NORMAL";

    //토큰 검증 및 해독을 담당할 JwtProvider를 생성자 통해 전달받음
    private final JwtProvider jwtProvider;

    public AuthFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request=exchange.getRequest();

        //header에서 Authorization(Bearer 토큰) 추출
        String bearerToken=request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if(!StringUtils.hasText(bearerToken) || !bearerToken.startsWith("Bearer")){
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }

        //토큰 자르기
        String token= bearerToken.substring(7);

        //JwtProvider로 토큰 유효성 검증
        if(!jwtProvider.validateToken(token)){
            return onError(exchange,HttpStatus.UNAUTHORIZED);
        }

        //토큰에서 실제 Id, LoginId, Role추출
        String userId= jwtProvider.getUserIdFromToken(token);
        String loginId= jwtProvider.getLoginIdFromToken(token);
        String role= jwtProvider.getRoleFromToken(token);

        //헤더에 실제 정보 탑재 후 전달
        ServerHttpRequest req= request
                .mutate()
                .header("X-USER-ID",userId!= null? userId: "")
                .header("X-USER-LOGINID",loginId!=null?loginId:"")
                .header("X-USER-ROLE",role!=null?role:"")
                .build();

        return chain.filter(exchange.mutate().request(req).build());
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus httpStatus) {
        ServerHttpResponse response=exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }
}
