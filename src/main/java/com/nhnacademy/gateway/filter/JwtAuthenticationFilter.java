package com.nhnacademy.gateway.filter;

import com.nhnacademy.gateway.utils.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpHeaders;

import java.util.Collections;


/**
 * gateway로 들어오는 모든 요청의 헤더를 검사
 * JWT 토큰이 존재하는 경우, 토큰의 유효성 검사 및 Redis에 저장된 블랙리스트 여부를 확인
 * 토큰이 존재하지 않거나, 유효하지 않거나, 블랙리스트에 등록된 경우, 401 Unauthorized 응답을 반환
 * 유효한 토큰인 경우, 요청을 다음 필터 체인으로 전달
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    //토큰 해독 및 유효성 검증을 전담하는 도구를 주입받을 변수
    private final JwtProvider jwtProvider;

    //로그아웃해서 못쓰게 된 토큰(블랙리스트) 목록을 빠르게 찾아보기 위한 검색도구.
    private final ReactiveStringRedisTemplate redisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        //들어온 http요청의 url 경로만 추출해서 변수에 담음
        String path=exchange.getRequest().getURI().getPath();

        //로그인, 회원가입, 토큰 갱신, 헬스체크 요청은 인증 없이 허용
        if(path.startsWith("/actuator") || path.equals("/api/auth/login") || path.equals("/api/auth/refresh")
        || (path.contains("/api/account/signup"))
        || (path.contains("/api/account/reset-password"))
        || (path.equals("/api/payment/plans") && exchange.getRequest().getMethod().matches("GET"))
        || (path.equals("/api/payment/billing-keys/toss/callback") && exchange.getRequest().getMethod().matches("POST"))
        ){
            return chain.filter(exchange);
        }

        //요청 헤더에서 Authorization 헤더 추출
        String authHeader=exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if(authHeader==null || !authHeader.startsWith("Bearer ")) { //Bearer 뒤에 띄어쓰기 한칸 필수
            //401 Unauthorized 응답 반환
            log.warn("토큰이 존재하지 않거나, 잘못된 형식입니다");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        //토큰 자르기
        String accessToken=authHeader.substring(7);

        //토큰 자체의 유효성 검사
        if(!jwtProvider.validateToken(accessToken)){
            log.warn("유효하지 않은 토큰입니다");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        //토큰에서 JTI 추출
        String jti = jwtProvider.getJtiFromToken(accessToken);

        //Redis에 저장된 블랙리스트 여부 확인 (jti 기반)
        String redisKey="blacklist:"+jti;

        //Redis에서 해당 토큰이 블랙리스트에 등록되어 있는지 확인
        return redisTemplate.hasKey(redisKey)
            //블랙리스트 여부에 따라 처리
                .flatMap(isBlacklisted->{
                    //블랙리스트에 등록된 토큰인 경우, 401 Unauthorized 응답 반환
                    if(isBlacklisted){
                        log.warn("로그아웃 처리된 토큰입니다");
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }
                    //토큰이 유효한 경우, 토큰에서 사용자 정보 추출
                    String userId= jwtProvider.getUserIdFromToken(accessToken);
                    String loginId= jwtProvider.getLoginIdFromToken(accessToken);
                    String role= jwtProvider.getRoleFromToken(accessToken);

                    //SecurityContext에 인증 정보 저장(인증 객체 생성)
                    Authentication authentication=new UsernamePasswordAuthenticationToken(
                            userId,null,Collections.singletonList(new SimpleGrantedAuthority(role))
                    );

                    //스프링 시큐리티 전용 지갑인 SecurityContext에 authentication 집어넣기
                    SecurityContext context=new SecurityContextImpl(authentication);

                    //다음 필터 체인으로 요청 전달, SecurityContext를 Reactor Context에 저장하여 이후 인증 정보 활용 가능
                    return chain.filter(exchange)
                            .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context)));
                });


    }


}


