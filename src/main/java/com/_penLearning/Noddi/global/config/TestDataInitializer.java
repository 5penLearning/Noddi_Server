package com._penLearning.Noddi.global.config;

import com._penLearning.Noddi.domain.organization.entity.Organization;
import com._penLearning.Noddi.domain.organization.repository.OrganizationRepository;
import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.project.entity.ProjectMember;
import com._penLearning.Noddi.domain.project.entity.ProjectRole;
import com._penLearning.Noddi.domain.project.repository.ProjectMemberRepository;
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
@Profile({"local", "dev", "default"})
@RequiredArgsConstructor
public class TestDataInitializer implements CommandLineRunner {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (organizationRepository.count() == 0 && userRepository.count() == 0) {
            Organization testOrg = Organization.builder()
                    .name("테스트 조직(Gmail)")
                    .emailDomain("gmail.com")
                    .build();
            organizationRepository.save(testOrg);

            String encodedPassword = passwordEncoder.encode("test");
            User user1 = User.builder()
                    .organization(testOrg)
                    .email("test1@gmail.com")
                    .name("테스트 유저 1")
                    .department("백엔드팀")
                    .position("백엔드 개발자")
                    .password(encodedPassword)
                    .build();

            User user2 = User.builder()
                    .organization(testOrg)
                    .email("test2@gmail.com")
                    .name("테스트 유저 2")
                    .department("백엔드팀")
                    .position("백엔드 개발자")
                    .password(encodedPassword)
                    .build();

            User user3 = User.builder()
                    .organization(testOrg)
                    .email("test3@gmail.com")
                    .name("테스트 유저 3")
                    .department("기획팀")
                    .position("서비스 기획자")
                    .password(encodedPassword)
                    .build();

            userRepository.saveAll(List.of(user1, user2, user3));

            Project testProject = Project.builder()
                    .organization(testOrg)
                    .name("Noddi 협업 캡스톤 프로젝트")
                    .description("실시간 WebRTC 오디오 및 AI 요약 MVP")
                    .createdBy(user1)
                    .build();

            projectRepository.save(testProject);

            projectMemberRepository.save(ProjectMember.create(
                    testProject,
                    user1,
                    ProjectRole.LEADER
            ));

            Team testTeam = Team.builder()
                    .project(testProject)
                    .name("Noddi 백엔드 개발팀")
                    .createdBy(user1)
                    .build();

            teamRepository.save(testTeam);

            TeamMember tm1 = TeamMember.builder()
                    .team(testTeam)
                    .user(user1)
                    .role(TeamRole.LEADER)
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
            log.info("   1) test1@gmail.com (userId: {}, OWNER)", user1.getUserId());
            log.info("   2) test2@gmail.com (userId: {}, MEMBER)", user2.getUserId());
            log.info("   3) test3@gmail.com (userId: {}, MEMBER)", user3.getUserId());
        }
    }
}
