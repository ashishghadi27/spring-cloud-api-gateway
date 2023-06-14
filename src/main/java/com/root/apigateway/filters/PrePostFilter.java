package com.root.apigateway.filters;

import com.root.apigateway.configurations.ConsulConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;

@Component
public class PrePostFilter implements GlobalFilter, Ordered {

    @Autowired
    private ConsulConfig consulConfig;

    private boolean isWhiteListedUrl(String requestUrl){
        List<String> whiteListedUrls = consulConfig.getWhitelistedUrls();
        return whiteListedUrls.stream().anyMatch(requestUrl::contains);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest serverHttpRequest = exchange.getRequest();
        String requestUrl = serverHttpRequest.getURI().toString().toLowerCase(Locale.ROOT);
        if(!isWhiteListedUrl(requestUrl)){
            throw new RuntimeException("NOT AUTHORIZED");
        }
        return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> {
                    System.out.println("Last Post Global Filter");
                }));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
