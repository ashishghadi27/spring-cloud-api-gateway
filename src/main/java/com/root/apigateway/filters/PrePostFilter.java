package com.root.apigateway.filters;

import com.root.apigateway.configurations.ConsulConfig;
import com.root.apigateway.helpers.PreFilterCookieRefresher;
import com.root.apigateway.helpers.PreFilterHelper;
import com.root.redis.exception.ValidationException;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;

@Component
public class PrePostFilter implements GlobalFilter, Ordered {

    private final ConsulConfig consulConfig;

    private final PreFilterHelper prefilterHelper;

    private final PreFilterCookieRefresher preFilterCookieRefresher;

    @Autowired
    public PrePostFilter(ConsulConfig consulConfig,
                         PreFilterHelper prefilterHelper,
                         PreFilterCookieRefresher preFilterCookieRefresher){
        this.consulConfig = consulConfig;
        this.prefilterHelper = prefilterHelper;
        this.preFilterCookieRefresher = preFilterCookieRefresher;
    }

    private boolean isWhiteListedUrl(String requestUrl){
        List<String> whiteListedUrls = consulConfig.getWhitelistedUrls();
        return whiteListedUrls.stream().anyMatch(requestUrl::contains);
    }

    @SneakyThrows
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        try{
            ServerHttpRequest serverHttpRequest = exchange.getRequest();
            String requestUrl = serverHttpRequest.getURI().toString().toLowerCase(Locale.ROOT);
            if(isWhiteListedUrl(requestUrl)){
                serverHttpRequest = prefilterHelper.createSession(serverHttpRequest);
                ServerWebExchange finalExchange = exchange.mutate().request(serverHttpRequest).build();
                return chain.filter(finalExchange);
            }
            else {
                prefilterHelper.validateRequest(serverHttpRequest);
                ServerHttpResponse response = exchange.getResponse();
                preFilterCookieRefresher.refreshSessionIfNeeded(serverHttpRequest, response);
                ServerWebExchange finalExchange = exchange.mutate().request(serverHttpRequest).response(response).build();
                return chain.filter(finalExchange);
            }

        }
        catch (ValidationException e){
            System.out.println(e.getCause() + "::::" + e.getErrorMessage());
            throw e;
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
