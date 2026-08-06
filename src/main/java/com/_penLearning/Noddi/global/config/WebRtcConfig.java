package com._penLearning.Noddi.global.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class WebRtcConfig {
    @Value("${daily.api.key}")
    private String dailyApiKey;

    @Bean(name = "dailyRestClient")
    public RestClient dailyRestClient() {
        return RestClient.builder()
                .baseUrl("https://api.daily.co/v1")
                .defaultHeader("Authorization", "Bearer " + dailyApiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
