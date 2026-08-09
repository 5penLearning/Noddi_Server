package com._penLearning.Noddi.global.config;

import com._penLearning.Noddi.domain.organization.entity.Organization;
import com._penLearning.Noddi.domain.organization.repository.OrganizationRepository;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestDataInitializer implements CommandLineRunner {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (organizationRepository.count() == 0 && userRepository.count() == 0) {

            // 1. 조직 생성
            Organization hongik = Organization.builder()
                    .name("홍익대학교")
                    .emailDomain("g.hongik.ac.kr")
                    .build();

            Organization testOrg = Organization.builder()
                    .name("테스트 조직(Gmail)")
                    .emailDomain("gmail.com")
                    .build();

            organizationRepository.saveAll(List.of(hongik, testOrg));

            String encodedPassword = passwordEncoder.encode("1234");

            User user1 = User.builder()
                    .name("김철수")
                    .email("chulsoo@g.hongik.ac.kr")
                    .password(encodedPassword)
                    .organization(hongik)
                    .build();

            User user2 = User.builder()
                    .name("김영희")
                    .email("younghee@gmail.com")
                    .password(encodedPassword)
                    .organization(testOrg)
                    .build();

            userRepository.saveAll(List.of(user1, user2));

            log.info("[TestDataInitializer] 초기 테스트 데이터 주입 완료: 조직 2개, 유저 2명");
        }
    }
}