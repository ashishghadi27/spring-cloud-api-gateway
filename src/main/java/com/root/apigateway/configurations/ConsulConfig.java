package com.root.apigateway.configurations;

import org.apache.commons.lang3.StringUtils;
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

    public List<String> getJwtByPassedUrls(){
        JSONObject jsonObject = new JSONObject(configProperties);
        JSONArray jsonArray = jsonObject.optJSONArray("jwtByPassUrls");
        return jsonArray.toList().stream().map(x -> (String)x).collect(Collectors.toList());
    }

    public List<String> getCookieRemovalUrls(){
        JSONObject jsonObject = new JSONObject(configProperties);
        JSONArray jsonArray = jsonObject.optJSONArray("cookieRemovalUrls");
        return jsonArray.toList().stream().map(x -> (String)x).collect(Collectors.toList());
    }

    public String getConfigValueByKey(String key){
        return getStringValueFromJson(key);
    }

    private String getStringValueFromJson(String key){
        JSONObject jsonObject = new JSONObject(configProperties);
        return jsonObject.optString(key);
    }

    public int getConfigValueByKey(String key, int defaultValue){
        String configValue = getStringValueFromJson(key);
        return StringUtils.isNotEmpty(configValue) ? Integer.parseInt(configValue) : defaultValue;
    }

}
