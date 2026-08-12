package com.nhnacademy.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// 토큰 검증, 파싱을 담당하는 클래스
@Component
public class JwtProvider {
    private final SecretKey secretKey;

    //application.yml의 jwt.secret-key 값을 읽어옴
    public JwtProvider(@Value("${jwt.secret-key}") String secret){
        this.secretKey= Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }


    // Claims 파싱
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)      //토큰 서명 검증에 사용할 비밀 키 설정
                .build()                    //JWT 파서 빌드
                .parseSignedClaims(token)   //파싱 시도 - 서명된 클레임을 파싱
                .getPayload();              //파싱 성공 시, 클레임(토큰의 내용) 반환
    }

    //1. 토큰 유효성 검증
    public boolean validateToken(String token){
        try{
            getClaims(token);
            return true;
        }catch (JwtException | IllegalArgumentException e){
            return false;
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


}
