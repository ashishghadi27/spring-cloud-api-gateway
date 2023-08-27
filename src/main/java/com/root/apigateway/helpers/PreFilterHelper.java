package com.root.apigateway.helpers;

import com.root.apigateway.configurations.ConsulConfig;
import com.root.commondependencies.constants.ExceptionConstants;
import com.root.commondependencies.exception.ValidationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static com.root.apigateway.utils.CommonUtil.*;

@Component
public class PreFilterHelper {

    private final ConsulConfig config;

    @Autowired
    public PreFilterHelper(ConsulConfig config) {
        this.config = config;
    }

    public ServerHttpRequest createSession(ServerHttpRequest serverHttpRequest){
        UUID uuid = UUID.randomUUID();
        String sessionId = uuid.toString();

        serverHttpRequest.mutate().headers((httpHeaders) -> {
            String session = new HttpCookie("session-id", sessionId).toString();
            httpHeaders.set("Cookie", session);
        }).build();

        return serverHttpRequest;
    }

    public void validateRequest(ServerHttpRequest serverHttpRequest, String requestUrl) throws ValidationException {
        HttpCookie sessionCookie = getSessionCookie(serverHttpRequest);
        validateSession(sessionCookie);

        if(!isJwtByPassedUrl(requestUrl)){
            HttpCookie jwtCookie = getJwtCookie(serverHttpRequest);
            validateJwt(jwtCookie);
        }
    }

    private void validateSession(HttpCookie sessionCookie) throws ValidationException {
        if(sessionCookie == null || StringUtils.isEmpty(sessionCookie.getValue())){
            throw new ValidationException.Builder().errorMessage(ExceptionConstants.UNAUTHORISED).build();
        }
    }

    private void validateJwt(HttpCookie jwtCookie) throws ValidationException {
        if(jwtCookie == null || StringUtils.isEmpty(jwtCookie.getValue())){
            throw new ValidationException.Builder().errorMessage(ExceptionConstants.UNAUTHORISED).build();
        }
        String jwt = jwtCookie.getValue();
        String secret = config.getConfigValueByKey("JWT_SECRET");

        Jws<Claims> claimsJws = getJwtClaims(jwt, secret);
        if(claimsJws == null || !isValidJwt(claimsJws)){
            throw new ValidationException.Builder().errorMessage(ExceptionConstants.UNAUTHORISED).build();
        }
    }

    private boolean isJwtByPassedUrl(String requestUrl){
        List<String> byPassedUrls = config.getJwtByPassedUrls();
        return byPassedUrls.stream().anyMatch(requestUrl::contains);
    }


}
