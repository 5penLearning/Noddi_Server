package com._penLearning.Noddi.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@ConditionalOnProperty(
        prefix = "profile-image",
        name = "storage-type",
        havingValue = "s3"
)
public class S3Config {

    @Bean
    public S3Client s3Client(
            @Value("${profile-image.s3.region}") String region
    ) {
        /*
         * credentialsProvider를 직접 지정하지 않는다.
         *
         * 로컬에서는 AWS CLI의 로그인 정보 또는 AWS_PROFILE을 사용하고,
         * EC2에서는 인스턴스에 연결된 IAM Role의 임시 자격 증명을
         * AWS SDK 기본 자격 증명 체인이 자동으로 사용한다.
         *
         * Access Key와 Secret Key를 코드에 저장하면 안 된다.
         */
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }
}