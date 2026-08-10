package com._penLearning.Noddi.global.config;

import com._penLearning.Noddi.domain.organization.entity.Organization;
import com._penLearning.Noddi.domain.organization.repository.OrganizationRepository;
import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.project.repository.ProjectRepository;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.team.entity.TeamMember;
import com._penLearning.Noddi.domain.team.entity.TeamRole;
import com._penLearning.Noddi.domain.team.repository.TeamMemberRepository;
import com._penLearning.Noddi.domain.team.repository.TeamRepository;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Profile({"local", "dev", "default"}) // 👈 default 프로필(로컬 기본 실행)에서도 작동하도록 추가!
@RequiredArgsConstructor
public class TestDataInitializer implements CommandLineRunner {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (organizationRepository.count() == 0 && userRepository.count() == 0) {

            // 1. 조직 생성
        // 💡 유저 데이터가 없을 때 더미 생성을 확실히 실행!
        if (userRepository.count() == 0) {

            // 1. 조직(Organization) 더미 생성
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
            // 비밀번호 'test' 암호화
            String encodedPassword = passwordEncoder.encode("test");

            // 2. 유저(User) 3명 더미 생성
            User user1 = User.builder()
                    .organization(testOrg)
                    .email("test1@gmail.com")
                    .name("테스트 유저 1")
                    .password(encodedPassword)
                    .build();

            User user2 = User.builder()
                    .organization(testOrg)
                    .email("test2@gmail.com")
                    .name("테스트 유저 2")
                    .password(encodedPassword)
                    .build();

            User user3 = User.builder()
                    .organization(testOrg)
                    .email("test3@gmail.com")
                    .name("테스트 유저 3")
                    .password(encodedPassword)
                    .build();

            userRepository.saveAll(List.of(user1, user2, user3));

            // 3. 프로젝트(Project) 더미 생성
            Project testProject = Project.builder()
                    .organization(testOrg)
                    .name("Noddi 협업 캡스톤 프로젝트")
                    .description("실시간 WebRTC 오디오 및 AI 요약 MVP")
                    .createdBy(user1)
                    .build();

            projectRepository.save(testProject);

            // 4. 팀(Team) 더미 생성 (1번 팀)
            Team testTeam = Team.builder()
                    .project(testProject)
                    .name("Noddi 백엔드 개발팀")
                    .createdBy(user1)
                    .build();

            teamRepository.save(testTeam);

            // 5. 1번 팀에 유저 3명 모두 팀원으로 매핑 (TeamMember)
            TeamMember tm1 = TeamMember.builder()
                    .team(testTeam)
                    .user(user1)
                    .role(TeamRole.OWNER)
                    .build();

            TeamMember tm2 = TeamMember.builder()
                    .team(testTeam)
                    .user(user2)
                    .role(TeamRole.MEMBER)
                    .build();

            TeamMember tm3 = TeamMember.builder()
                    .team(testTeam)
                    .user(user3)
                    .role(TeamRole.MEMBER)
                    .build();

            teamMemberRepository.saveAll(List.of(tm1, tm2, tm3));

            log.info("[TestDataInitializer] 로컬/개발용 더미 데이터 생성 완료!");
            log.info(" - 공통 비밀번호: test");
            log.info(" - 1번 팀(teamId: {}) 팀원 계정 목록:", testTeam.getTeamId());
            log.info("   1) test1@gmail.com (userId: {}, LEADER)", user1.getUserId());
            log.info("   2) test2@gmail.com (userId: {}, MEMBER)", user2.getUserId());
            log.info("   3) test3@gmail.com (userId: {}, MEMBER)", user3.getUserId());
        }
    }
}