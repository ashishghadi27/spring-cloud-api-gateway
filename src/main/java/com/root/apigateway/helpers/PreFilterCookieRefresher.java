package com.root.apigateway.helpers;

import com.root.apigateway.configurations.ConsulConfig;
import com.root.apigateway.utils.CommonUtil;
import com.root.redis.constants.ExceptionConstants;
import com.root.redis.exception.ValidationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static com.root.apigateway.utils.CommonUtil.*;

@Component
public class PreFilterCookieRefresher {

    public static final int JWT_DEFAULT_TIMEOUT = 20;

    private final ConsulConfig config;

    @Autowired
    public PreFilterCookieRefresher(ConsulConfig config) {
        this.config = config;
    }

    public void refreshSessionIfNeeded(String requestUrl,
                                       ServerHttpRequest serverHttpRequest,
                                       ServerHttpResponse serverHttpResponse) throws ValidationException {
        if (!isJwtByPassedUrl(requestUrl)) {
            String secret = config.getConfigValueByKey("JWT_SECRET");
            int jwtBufferTime = config.getConfigValueByKey("JWT_BUFFER_TIME", 5);
            int timeout = config.getConfigValueByKey("JWT_TIMEOUT", JWT_DEFAULT_TIMEOUT);
            int cookieTimeout = config.getConfigValueByKey("COOKIE_TIMEOUT", 1080);

            HttpCookie sessionCookie = getSessionCookie(serverHttpRequest);
            HttpCookie jwtCookie = getJwtCookie(serverHttpRequest);

            Jws<Claims> claims = getJwtClaims(jwtCookie.getValue(), secret);
            if (claims != null && isValidJwt(claims)) {
                if (isRefreshRequired(claims, jwtBufferTime)) {
                    String newJwt = createJWTToken(secret, timeout, claims);
                    serverHttpResponse.addCookie(CommonUtil.getSessionCookie(sessionCookie.getValue(), cookieTimeout));
                    serverHttpResponse.addCookie(CommonUtil.getJwtCookie(newJwt, cookieTimeout));
                }
                if(cookieDeleteAllowed(requestUrl)){
                    serverHttpResponse.addCookie(CommonUtil.getSessionCookie(sessionCookie.getValue(), 0));
                    serverHttpResponse.addCookie(CommonUtil.getJwtCookie(jwtCookie.getValue(), 0));
                }
            } else {
                throw new ValidationException.Builder().errorMessage(ExceptionConstants.UNAUTHORISED).build();
            }
        }

    }

    private boolean cookieDeleteAllowed(String requestUrl) {
        List<String> cookieRemovalUrls = config.getCookieRemovalUrls();
        return cookieRemovalUrls.stream().anyMatch(requestUrl::contains);
    }

    private static boolean isRefreshRequired(Jws<Claims> claimsJws, int jwtBufferTime) {
        Date expiryDate = claimsJws.getBody().getExpiration();
        Date bufferDate = Date.from(Instant.now().minus(jwtBufferTime, ChronoUnit.MINUTES));
        return expiryDate.after(bufferDate);
    }

    private String createJWTToken(String secret, int timeout, Jws<Claims> claims) {

        Key hmacKey = new SecretKeySpec(Base64.getDecoder().decode(secret),
                SignatureAlgorithm.HS256.getJcaName());

        Instant now = Instant.now();
        return Jwts.builder()
                .claim("name", claims.getBody().get("name"))
                .claim("email", claims.getBody().get("email"))
                .setSubject("login")
                .setId(claims.getBody().getId())
                .setIssuedAt(claims.getBody().getIssuedAt())
                .setExpiration(Date.from(now.plus(timeout, ChronoUnit.MINUTES)))
                .signWith(hmacKey)
                .compact();
    }

    private boolean isJwtByPassedUrl(String requestUrl) {
        List<String> byPassedUrls = config.getJwtByPassedUrls();
        return byPassedUrls.stream().anyMatch(requestUrl::contains);
    }

}
