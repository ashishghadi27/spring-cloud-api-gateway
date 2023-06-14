package com.root.apigateway.configurations;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RefreshScope
public class ConsulConfig {

    private final String configProperties;

    public ConsulConfig(@Value("${config-properties}") String configProperties){
        this.configProperties = configProperties;
    }

    public List<String> getWhitelistedUrls(){
        JSONObject jsonObject = new JSONObject(configProperties);
        JSONArray jsonArray = jsonObject.optJSONArray("whitelistedUrls");
        return jsonArray.toList().stream().map(x -> (String)x).collect(Collectors.toList());
    }

}
