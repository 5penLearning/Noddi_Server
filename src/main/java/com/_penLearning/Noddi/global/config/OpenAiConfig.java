package com._penLearning.Noddi.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class OpenAiConfig {
    @Value("${openai.api-key:}")
    private String openAiApiKey;

    @Bean(name = "openAiRestClient")
    public RestClient openAiRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000); //30초
        factory.setReadTimeout(120000); //2분
        return RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .requestFactory(factory)
                .defaultHeader("Authorization", "Bearer " + openAiApiKey)
                .build();
    }
}
