package com._penLearning.Noddi.global.config;

import com._penLearning.Noddi.domain.organization.entity.Organization;
import com._penLearning.Noddi.domain.organization.repository.OrganizationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TestDataInitializer implements CommandLineRunner {

    private final OrganizationRepository organizationRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (organizationRepository.count() == 0) {

            // 1. 홍익대학교 학생 도메인 적용
            Organization hongik = Organization.builder()
                    .name("홍익대학교")
                    .emailDomain("g.hongik.ac.kr")
                    .build();

            // 2. 메일 발송 기능 자체 테스트용 (필요 시 사용)
            Organization testOrg = Organization.builder()
                    .name("테스트 조직(Gmail)")
                    .emailDomain("gmail.com")
                    .build();

            organizationRepository.saveAll(List.of(hongik, testOrg));
        }
    }
}