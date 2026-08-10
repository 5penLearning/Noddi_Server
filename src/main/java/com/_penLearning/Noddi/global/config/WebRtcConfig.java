package com._penLearning.Noddi.global.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class WebRtcConfig {
    @Value("${daily.api.key}")
    private String dailyApiKey;

    @Bean(name = "dailyRestClient")
    public RestClient dailyRestClient() {
        // 💡 3초 타임아웃 설정으로 DB 커넥션 풀 고갈 완벽 방어!
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000); // 연결 타임아웃 3초
        factory.setReadTimeout(3000);    // 응답 타임아웃 3초
        return RestClient.builder()
                .baseUrl("https://api.daily.co/v1")
                .defaultHeader("Authorization", "Bearer " + dailyApiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
