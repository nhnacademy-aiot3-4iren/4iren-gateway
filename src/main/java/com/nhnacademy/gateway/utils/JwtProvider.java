package com.nhnacademy.gateway.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// 토큰 검증, 파싱을 담당하는 클래스
@Slf4j
@Component
public class JwtProvider {
    private final SecretKey secretKey;

    //application.yml의 jwt.secret-key 값을 읽어옴
    public JwtProvider(@Value("${jwt.secret-key}") String secret){
        byte[] keyBytes = io.jsonwebtoken.io.Decoders.BASE64.decode(secret);
        this.secretKey= Keys.hmacShaKeyFor(keyBytes);
    }


    // Claims 파싱
    public Claims getClaims(String token){
        try {
            // 만료되지 않은 토큰에서 클레임 빼내기
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰에서 클레임 빼내기
            return e.getClaims();
        }

    }

    //1. 토큰 유효성 검증
    public boolean validateToken(String token){
        try{
            // jwt 직접 파싱 시도
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;    // 성공 시, 유효한 토큰
        }catch (JwtException | IllegalArgumentException e){
            log.warn("Invalid JWT token:{}",e.getMessage());
            return false;   // 실패 시, 훼손되거나 만료된 토큰
        }
    }


    //2. 토큰에서 UserId 추출
    public String getUserIdFromToken(String token){
        return getClaims(token).getSubject(); // 토큰 생성 할때 JWT 표준 식별자 자리 sub에 넣었기 때문에 바로 getSubject로 꺼낼 수 있음
    }

    //3. 토큰에서 LoginId 추출
    public String getLoginIdFromToken(String token) {
        return getClaims(token).get("loginId",String.class); // JWT 표준 자리가 아니기 때문에 내가 커스텀한 .get("key",타입) 메서드로 꺼내야함
    }

    //4. 토큰에서 Role 추출
    public String getRoleFromToken(String token){
        return getClaims(token).get("role",String.class);
    }


    // 5.토큰에서 남은 유효기간 계산
    public Long getRemainingTime(String token){
        try{
            Date expiration=getClaims(token).getExpiration(); //토큰의 만료시간 추출
            long now= new Date().getTime();
            return expiration.getTime()-now; //남은 유효기간 계산 (만료시간 - 현재시간)
        }catch (JwtException e){
            return 0L; //토큰이 유효하지 않으면 남은 시간 0 반환
        }
    }

    // 6. 토큰에서 JTI(고유 ID) 추출
    public String getJtiFromToken(String token){
        return getClaims(token).getId(); // JWT 표준 식별자 jti 추출
    }


}
