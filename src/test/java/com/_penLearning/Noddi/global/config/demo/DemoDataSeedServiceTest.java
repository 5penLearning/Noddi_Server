package com._penLearning.Noddi.global.config.demo;

import com._penLearning.Noddi.domain.meeting.repository.MeetingRepository;
import com._penLearning.Noddi.domain.organization.repository.OrganizationRepository;
import com._penLearning.Noddi.domain.project.repository.ProjectRepository;
import com._penLearning.Noddi.domain.summary.repository.MeetingSummaryRepository;
import com._penLearning.Noddi.domain.team.repository.TeamRepository;
import com._penLearning.Noddi.domain.teamPage.repository.TeamPageRepository;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({DemoDataSeedService.class, DemoDataSeedServiceTest.PasswordEncoderConfig.class})
class DemoDataSeedServiceTest {

    @Autowired
    private DemoDataSeedService seedService;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private MeetingRepository meetingRepository;
    @Autowired
    private MeetingSummaryRepository meetingSummaryRepository;
    @Autowired
    private TeamPageRepository teamPageRepository;

    @Test
    void createsThreeTeamTranscriptsAndSixTeamPagesOnlyOnce() {
        DemoDataSeedService.SeedResult firstResult = seedService.seed("Demo1234!");

        assertThat(firstResult.created()).isTrue();
        assertThat(firstResult.meetingIds()).hasSize(3);
        assertThat(firstResult.teamPageIds()).hasSize(6);
        assertThat(organizationRepository.count()).isEqualTo(1);
        assertThat(userRepository.count()).isEqualTo(4);
        assertThat(projectRepository.count()).isEqualTo(1);
        assertThat(teamRepository.count()).isEqualTo(3);
        assertThat(meetingRepository.count()).isEqualTo(3);
        assertThat(meetingSummaryRepository.count()).isEqualTo(3);
        assertThat(teamPageRepository.count()).isEqualTo(6);

        DemoDataSeedService.SeedResult secondResult = seedService.seed("Demo1234!");

        assertThat(secondResult.created()).isFalse();
        assertThat(secondResult.meetingIds()).containsExactlyElementsOf(firstResult.meetingIds());
        assertThat(secondResult.teamPageIds()).containsExactlyElementsOf(firstResult.teamPageIds());
        assertThat(organizationRepository.count()).isEqualTo(1);
        assertThat(userRepository.count()).isEqualTo(4);
        assertThat(meetingRepository.count()).isEqualTo(3);
        assertThat(teamPageRepository.count()).isEqualTo(6);
    }

    @TestConfiguration
    static class PasswordEncoderConfig {

        @Bean
        BCryptPasswordEncoder bCryptPasswordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }
}
