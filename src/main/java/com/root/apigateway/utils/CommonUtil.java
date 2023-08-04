package com.root.apigateway.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.root.redis.constants.ExceptionConstants;
import com.root.redis.exception.ValidationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.MultiValueMap;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

public final class CommonUtil {

    public static HttpCookie getSessionCookie(ServerHttpRequest serverHttpRequest) throws ValidationException {
        MultiValueMap<String, HttpCookie> headers = serverHttpRequest.getCookies();
        List<HttpCookie> cookies = headers.get("session-id");
        if(cookies != null){
            return cookies.stream()
                    .findFirst().orElse(null);
        }
        throw new ValidationException.Builder().errorMessage(ExceptionConstants.UNAUTHORISED).build();
    }

    public static HttpCookie getJwtCookie(ServerHttpRequest serverHttpRequest) throws ValidationException {
        MultiValueMap<String, HttpCookie> headers = serverHttpRequest.getCookies();
        List<HttpCookie> cookies = headers.get("auth");
        if(cookies != null){
            return cookies.stream()
                    .findFirst().orElse(null);
        }
        throw new ValidationException.Builder().errorMessage(ExceptionConstants.UNAUTHORISED).build();
    }

    public static Jws<Claims> getJwtClaims(String jwtString, String secret){
        try{
            Key hmacKey = new SecretKeySpec(Base64.getDecoder().decode(secret),
                    SignatureAlgorithm.HS256.getJcaName());
            Jws<Claims> jwt = Jwts.parserBuilder()
                    .setSigningKey(hmacKey)
                    .build()
                    .parseClaimsJws(jwtString);
            return jwt;
        }
        catch (Exception e){
            return null;
        }
    }

    public static boolean isValidJwt(Jws<Claims> jwt) {
        Date expiryDate = jwt.getBody().getExpiration();
        return   Date.from(Instant.now()).before(expiryDate)
                && "login".equalsIgnoreCase(jwt.getBody().getSubject());
    }

    public static ResponseCookie getSessionCookie(String sessionId, int cookieTimeout){
        return ResponseCookie.from("session-id",
                sessionId).domain("localhost").httpOnly(true).maxAge(cookieTimeout).path("/").build();
    }

    public static ResponseCookie getJwtCookie(String jwt, int cookieTimeout){
        return ResponseCookie.from("auth",
                jwt).domain("localhost").httpOnly(true).maxAge(cookieTimeout).path("/").build();
    }

    public static Map<String, Object> getMapFromObject(Object obj){
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map =
                mapper.convertValue(obj, new TypeReference<Map<String, Object>>() {});
        return map;
    }

}
