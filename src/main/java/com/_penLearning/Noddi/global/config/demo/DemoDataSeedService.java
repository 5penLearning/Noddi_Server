package com._penLearning.Noddi.global.config.demo;

import com._penLearning.Noddi.domain.meeting.entity.Meeting;
import com._penLearning.Noddi.domain.meeting.entity.MeetingParticipant;
import com._penLearning.Noddi.domain.meeting.repository.MeetingParticipantRepository;
import com._penLearning.Noddi.domain.meeting.repository.MeetingRepository;
import com._penLearning.Noddi.domain.organization.entity.Organization;
import com._penLearning.Noddi.domain.organization.repository.OrganizationRepository;
import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.project.entity.ProjectMember;
import com._penLearning.Noddi.domain.project.entity.ProjectRole;
import com._penLearning.Noddi.domain.project.repository.ProjectMemberRepository;
import com._penLearning.Noddi.domain.project.repository.ProjectRepository;
import com._penLearning.Noddi.domain.summary.entity.MeetingSummary;
import com._penLearning.Noddi.domain.summary.model.MeetingTranscriptSegment;
import com._penLearning.Noddi.domain.summary.repository.MeetingSummaryRepository;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.team.entity.TeamMember;
import com._penLearning.Noddi.domain.team.entity.TeamRole;
import com._penLearning.Noddi.domain.team.repository.TeamMemberRepository;
import com._penLearning.Noddi.domain.team.repository.TeamRepository;
import com._penLearning.Noddi.domain.teamPage.entity.TeamPage;
import com._penLearning.Noddi.domain.teamPage.repository.TeamPageRepository;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 시연에 필요한 관계형 데이터를 한 트랜잭션으로 생성한다.
 *
 * Pinecone 호출은 외부 API 작업이므로 이 클래스의 DB 트랜잭션에 포함하지 않는다.
 * DB 커밋 이후 {@link DemoDataInitializer}가 저장된 원본 ID를 이용해 색인을 실행한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DemoDataSeedService {

    public static final String DEMO_ORGANIZATION_DOMAIN = "noddi-demo.com";

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MeetingSummaryRepository meetingSummaryRepository;
    private final TeamPageRepository teamPageRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    @Transactional
    public SeedResult seed(String demoPassword) {
        validatePassword(demoPassword);

        boolean created = false;
        if (!organizationRepository.existsByEmailDomain(DEMO_ORGANIZATION_DOMAIN)) {
            createDemoData(demoPassword);
            created = true;
        }

        // 아래 JPQL이 방금 저장한 회의와 공유페이지까지 확실히 조회하도록 먼저 flush한다.
        entityManager.flush();

        List<Long> meetingIds = findDemoMeetingIds();
        List<Long> teamPageIds = findDemoTeamPageIds();
        return new SeedResult(created, meetingIds, teamPageIds);
    }

    private void createDemoData(String demoPassword) {
        Organization organization = organizationRepository.save(Organization.builder()
                .name("노디")
                .emailDomain(DEMO_ORGANIZATION_DOMAIN)
                .build());

        DemoUsers users = createUsers(organization, demoPassword);
        Project project = createProject(organization, users);
        DemoTeams teams = createTeams(project, users);

        createProductTeamKnowledge(teams.product(), users);
        createBackendTeamKnowledge(teams.backend(), users);
        createMarketingTeamKnowledge(teams.marketing(), users);

        log.info("[DemoData] 관계형 시연 데이터 생성 완료: organization={}, project={}, teamCount=3",
                organization.getName(), project.getName());
    }

    private DemoUsers createUsers(Organization organization, String demoPassword) {
        String encodedPassword = passwordEncoder.encode(demoPassword);

        User productManager = User.builder()
                .organization(organization)
                .email("demo.pm@" + DEMO_ORGANIZATION_DOMAIN)
                .name("김유진")
                .department("서비스기획팀")
                .position("프로젝트 매니저")
                .password(encodedPassword)
                .build();
        User backendLead = User.builder()
                .organization(organization)
                .email("demo.backend@" + DEMO_ORGANIZATION_DOMAIN)
                .name("홍길동")
                .department("백엔드팀")
                .position("백엔드 리드")
                .password(encodedPassword)
                .build();
        User productDesigner = User.builder()
                .organization(organization)
                .email("demo.design@" + DEMO_ORGANIZATION_DOMAIN)
                .name("이서연")
                .department("디자인팀")
                .position("프로덕트 디자이너")
                .password(encodedPassword)
                .build();
        User marketingManager = User.builder()
                .organization(organization)
                .email("demo.marketing@" + DEMO_ORGANIZATION_DOMAIN)
                .name("박지훈")
                .department("마케팅팀")
                .position("마케팅 매니저")
                .password(encodedPassword)
                .build();

        userRepository.saveAll(List.of(
                productManager,
                backendLead,
                productDesigner,
                marketingManager
        ));
        return new DemoUsers(productManager, backendLead, productDesigner, marketingManager);
    }

    private Project createProject(Organization organization, DemoUsers users) {
        Project project = projectRepository.save(Project.builder()
                .organization(organization)
                .name("노디 정식 출시 프로젝트")
                .description("팀의 회의 기록과 공유 문서를 지식으로 연결하는 노디의 정식 출시 프로젝트")
                .createdBy(users.productManager())
                .build());

        projectMemberRepository.saveAll(List.of(
                ProjectMember.create(project, users.productManager(), ProjectRole.LEADER),
                ProjectMember.create(project, users.backendLead(), ProjectRole.MEMBER),
                ProjectMember.create(project, users.productDesigner(), ProjectRole.MEMBER),
                ProjectMember.create(project, users.marketingManager(), ProjectRole.MEMBER)
        ));
        return project;
    }

    private DemoTeams createTeams(Project project, DemoUsers users) {
        Team productTeam = Team.builder()
                .project(project)
                .name("프로덕트팀")
                .description("서비스 정책과 출시 일정을 관리하는 팀")
                .createdBy(users.productManager())
                .build();
        Team backendTeam = Team.builder()
                .project(project)
                .name("백엔드팀")
                .description("API, 배포, 장애 대응을 담당하는 팀")
                .createdBy(users.backendLead())
                .build();
        Team marketingTeam = Team.builder()
                .project(project)
                .name("마케팅팀")
                .description("출시 캠페인과 콘텐츠 제작을 담당하는 팀")
                .createdBy(users.marketingManager())
                .build();
        teamRepository.saveAll(List.of(productTeam, backendTeam, marketingTeam));

        // PM 계정 하나로 모든 팀의 자료를 시연할 수 있도록 각 팀에 소속시킨다.
        teamMemberRepository.saveAll(List.of(
                teamMember(productTeam, users.productManager(), TeamRole.LEADER),
                teamMember(productTeam, users.backendLead(), TeamRole.MEMBER),
                teamMember(productTeam, users.productDesigner(), TeamRole.MEMBER),
                teamMember(productTeam, users.marketingManager(), TeamRole.MEMBER),
                teamMember(backendTeam, users.backendLead(), TeamRole.LEADER),
                teamMember(backendTeam, users.productManager(), TeamRole.MEMBER),
                teamMember(marketingTeam, users.marketingManager(), TeamRole.LEADER),
                teamMember(marketingTeam, users.productManager(), TeamRole.MEMBER),
                teamMember(marketingTeam, users.productDesigner(), TeamRole.MEMBER)
        ));

        return new DemoTeams(productTeam, backendTeam, marketingTeam);
    }

    private TeamMember teamMember(Team team, User user, TeamRole role) {
        return TeamMember.builder()
                .team(team)
                .user(user)
                .role(role)
                .build();
    }

    private void createProductTeamKnowledge(Team team, DemoUsers users) {
        List<MeetingTranscriptSegment> segments = List.of(
                segment(1, "김유진", 0, 18, "오늘은 노디 정식 출시 일정과 기능 동결 시점을 확정하겠습니다."),
                segment(2, "홍길동", 19, 43, "백엔드는 8월 24일 오후 6시까지 기능 개발을 끝내고 이후에는 치명적인 오류만 수정하겠습니다."),
                segment(3, "이서연", 44, 67, "디자인도 같은 시각에 화면을 동결하고 회원가입과 회의 요약 화면을 우선 점검하겠습니다."),
                segment(4, "박지훈", 68, 91, "캠페인 공개는 출시 하루 전인 8월 26일 오전 10시로 맞추겠습니다."),
                segment(5, "김유진", 92, 121, "정식 출시일은 8월 27일입니다. 출시 직후 두 시간 동안 전원이 오류율과 사용자 문의를 함께 모니터링합니다."),
                segment(6, "홍길동", 122, 151, "출시 당일 최우선 확인 항목은 회원가입, 로그인, 회의 전사 생성, AI 질의응답입니다."),
                segment(7, "김유진", 152, 178, "일정 변경이 생기면 공유페이지의 출시 체크리스트를 먼저 수정하고 팀 공지로 알리겠습니다.")
        );

        createCompletedMeeting(
                team,
                users.productManager(),
                List.of(users.productManager(), users.backendLead(), users.productDesigner(), users.marketingManager()),
                "노디 정식 출시 일정 확정 회의",
                "출시일, 기능 동결, 출시 당일 점검 항목 확정",
                LocalDateTime.now().minusDays(6),
                "노디 정식 출시일을 8월 27일로 확정하고 8월 24일 오후 6시에 기능과 디자인을 동결한다. 출시 직후 두 시간 동안 핵심 기능을 집중 모니터링한다.",
                List.of(
                        "정식 출시일은 8월 27일로 확정한다.",
                        "기능 및 디자인 동결은 8월 24일 오후 6시로 한다.",
                        "출시 후 두 시간 동안 전원이 집중 모니터링한다."
                ),
                List.of("회원가입과 회의 전사 생성 흐름을 출시 전 우선 점검해야 한다."),
                segments
        );

        createTeamPages(team, users.productManager(), List.of(
                page("정식 출시 체크리스트", """
                        ## 핵심 일정
                        - 기능 및 디자인 동결: 8월 24일 오후 6시
                        - 캠페인 공개: 8월 26일 오전 10시
                        - 정식 출시: 8월 27일 오전 10시

                        ## 출시 전 필수 점검
                        1. 회원가입과 이메일 인증이 정상 동작하는지 확인한다.
                        2. 회의 전사와 요약이 팀별로 정확히 저장되는지 확인한다.
                        3. AI 질의응답이 현재 팀의 전사와 공유페이지만 근거로 사용하는지 확인한다.
                        4. 운영 환경변수와 CORS 허용 주소를 마지막으로 검토한다.

                        ## 출시 당일
                        출시 직후 두 시간은 전원이 대기한다. 장애가 발생하면 백엔드팀이 원인을 분류하고 프로덕트팀이 사용자 공지를 작성한다.
                        """),
                page("제품 운영 정책과 사용자 응대 원칙", """
                        ## 운영 우선순위
                        노디의 핵심 가치는 회의가 끝난 뒤 기록을 다시 찾지 않아도 팀의 지식을 즉시 활용할 수 있게 하는 것이다.
                        장애 대응 우선순위는 로그인, 회의 참여, 전사 생성, AI 질의응답 순서로 정한다.

                        ## AI 답변 정책
                        AI는 사용자가 현재 질문한 팀의 회의 전사와 팀 공유페이지만 근거로 답변한다. 근거가 충분하지 않으면 추측하지 않고 팀원의 확인이 필요하다고 안내한다.

                        ## 문의 응대
                        사용자의 데이터가 다른 팀에 노출됐다는 제보는 최우선 보안 이슈로 분류한다. 일반 기능 문의는 영업일 기준 하루 안에 첫 답변을 제공한다.
                        """)));
    }

    private void createBackendTeamKnowledge(Team team, DemoUsers users) {
        List<MeetingTranscriptSegment> segments = List.of(
                segment(1, "홍길동", 0, 22, "정기 배포 시간을 매주 목요일 오후 3시로 통일하겠습니다."),
                segment(2, "김유진", 23, 46, "배포 전에는 스테이징에서 회원가입, 회의 종료, 전사 생성, 질문 등록을 확인해주세요."),
                segment(3, "홍길동", 47, 72, "배포 후 10분 동안 5xx 오류율이 5퍼센트를 넘거나 로그인 실패가 연속 발생하면 즉시 롤백합니다."),
                segment(4, "김유진", 73, 99, "환경변수 누락도 자주 발생하니 OpenAI와 Pinecone 키, 프론트 CORS 주소를 체크리스트에 넣겠습니다."),
                segment(5, "홍길동", 100, 126, "장애가 나면 최초 발견자가 팀 채널에 증상과 발생 시각을 남기고 백엔드 리드가 대응 담당자를 정합니다."),
                segment(6, "김유진", 127, 151, "복구 뒤에는 원인과 재발 방지 대책을 공유페이지에 정리하는 것으로 마무리하겠습니다.")
        );

        createCompletedMeeting(
                team,
                users.backendLead(),
                List.of(users.backendLead(), users.productManager()),
                "백엔드 배포 및 장애 대응 회의",
                "정기 배포 시간, 롤백 기준, 장애 대응 절차 확정",
                LocalDateTime.now().minusDays(4),
                "정기 배포는 매주 목요일 오후 3시에 진행한다. 배포 직후 5xx 오류율이 5%를 넘거나 로그인 실패가 연속되면 즉시 롤백하고, 복구 후 재발 방지 대책을 문서화한다.",
                List.of(
                        "정기 배포는 매주 목요일 오후 3시에 진행한다.",
                        "5xx 오류율이 5%를 초과하거나 로그인 실패가 연속되면 즉시 롤백한다.",
                        "장애 복구 후 원인과 재발 방지 대책을 공유페이지에 기록한다."
                ),
                List.of("운영 환경변수 누락을 방지할 공통 체크리스트가 필요하다."),
                segments
        );

        createTeamPages(team, users.backendLead(), List.of(
                page("백엔드 배포 운영 가이드", """
                        ## 배포 시간
                        정기 배포는 매주 목요일 오후 3시에 진행한다. 긴급 배포는 백엔드 리드와 프로젝트 매니저의 승인을 모두 받은 뒤 진행한다.

                        ## 배포 전 확인
                        - 스테이징 환경에서 회원가입과 로그인 테스트
                        - 회의 종료 후 전사 및 요약 생성 확인
                        - 팀별 AI 질의응답의 Pinecone 필터 확인
                        - OpenAI, Pinecone, JWT, CORS 환경변수 확인

                        ## 롤백 기준
                        배포 후 10분 안에 5xx 오류율이 5%를 초과하거나 로그인 실패가 연속으로 발생하면 이전 안정 버전으로 즉시 롤백한다.
                        """),
                page("서비스 장애 대응 절차", """
                        ## 1. 장애 선언
                        최초 발견자는 증상, 발생 시각, 영향받는 기능을 팀 채널에 남긴다. 백엔드 리드는 심각도를 판단하고 대응 담당자를 지정한다.

                        ## 2. 우선 복구
                        데이터 손상 가능성이 있으면 쓰기 기능부터 제한한다. 단순 배포 장애라면 이전 버전으로 롤백하고 로그인과 회의 참여 기능부터 확인한다.

                        ## 3. 사후 정리
                        복구 완료 후 24시간 안에 발생 원인, 사용자 영향, 대응 과정, 재발 방지 작업을 이 공유페이지에 기록한다. 비밀키나 개인정보는 문서에 남기지 않는다.
                        """)));
    }

    private void createMarketingTeamKnowledge(Team team, DemoUsers users) {
        List<MeetingTranscriptSegment> segments = List.of(
                segment(1, "박지훈", 0, 23, "런칭 캠페인의 핵심 메시지는 회의의 기록이 팀의 지식이 됩니다로 정하겠습니다."),
                segment(2, "이서연", 24, 49, "메인 이미지는 회의 전사에서 AI 답변으로 이어지는 흐름을 한 화면에 보여주면 좋겠습니다."),
                segment(3, "김유진", 50, 74, "주요 대상은 대학 프로젝트 팀과 초기 스타트업으로 잡고 실시간 협업 문제를 강조해주세요."),
                segment(4, "박지훈", 75, 101, "티저는 8월 23일, 기능 소개 콘텐츠는 8월 25일, 최종 캠페인은 8월 26일 오전 10시에 공개합니다."),
                segment(5, "이서연", 102, 128, "모든 콘텐츠에서 민감한 회의 내용과 실제 사용자 정보는 예시로도 노출하지 않겠습니다."),
                segment(6, "박지훈", 129, 154, "성과 지표는 랜딩 페이지 방문 수와 데모 신청 수로 확인하고 출시 다음 날 첫 결과를 공유하겠습니다.")
        );

        createCompletedMeeting(
                team,
                users.marketingManager(),
                List.of(users.marketingManager(), users.productDesigner(), users.productManager()),
                "노디 런칭 캠페인 기획 회의",
                "캠페인 메시지, 타깃, 콘텐츠 공개 일정 확정",
                LocalDateTime.now().minusDays(3),
                "런칭 캠페인의 핵심 메시지는 '회의의 기록이 팀의 지식이 됩니다'로 정한다. 대학 프로젝트 팀과 초기 스타트업을 주요 대상으로 하며 최종 캠페인은 8월 26일 오전 10시에 공개한다.",
                List.of(
                        "핵심 메시지는 '회의의 기록이 팀의 지식이 됩니다'로 한다.",
                        "주요 대상은 대학 프로젝트 팀과 초기 스타트업으로 한다.",
                        "최종 캠페인은 8월 26일 오전 10시에 공개한다."
                ),
                List.of("콘텐츠 제작 시 실제 사용자 정보나 민감한 회의 내용을 노출하지 않아야 한다."),
                segments
        );

        createTeamPages(team, users.marketingManager(), List.of(
                page("노디 런칭 캠페인 가이드", """
                        ## 핵심 메시지
                        **회의의 기록이 팀의 지식이 됩니다.**
                        노디는 회의 전사와 팀이 작성한 공유 문서를 연결해 필요한 답을 빠르게 찾도록 돕는 협업 서비스다.

                        ## 주요 대상
                        첫 번째 대상은 회의와 과제가 많은 대학 프로젝트 팀이고, 두 번째 대상은 문서화 인력이 부족한 초기 스타트업이다.

                        ## 공개 일정
                        - 8월 23일: 티저 콘텐츠
                        - 8월 25일: 핵심 기능 소개
                        - 8월 26일 오전 10시: 최종 런칭 캠페인

                        성과는 랜딩 페이지 방문 수와 데모 신청 수로 측정한다.
                        """),
                page("마케팅 콘텐츠 제작 원칙", """
                        ## 표현 원칙
                        기능 이름보다 사용자가 얻는 결과를 먼저 설명한다. 회의를 녹음한다는 표현보다 회의의 결정 사항을 다시 찾을 수 있다는 가치를 강조한다.

                        ## 보안 원칙
                        실제 사용자의 이름, 이메일, 민감한 회의 내용은 콘텐츠에 사용하지 않는다. 화면 예시는 반드시 시연용 계정과 가공된 데이터로 제작한다.

                        ## 검수 절차
                        초안은 마케팅팀이 작성하고 디자인팀이 시각 요소를 확인한다. 공개 전 최종 문구와 기능 설명은 프로젝트 매니저가 승인한다.
                        """)));
    }

    private void createCompletedMeeting(
            Team team,
            User creator,
            List<User> participants,
            String title,
            String agenda,
            LocalDateTime scheduledStartAt,
            String summaryText,
            List<String> decisions,
            List<String> issues,
            List<MeetingTranscriptSegment> transcriptSegments
    ) {
        Meeting meeting = Meeting.builder()
                .team(team)
                .title(title)
                .agenda(agenda)
                .scheduledStartAt(scheduledStartAt)
                .scheduledEndAt(scheduledStartAt.plusHours(1))
                .createdBy(creator)
                .build();
        meeting.start("demo-" + team.getName());
        meeting.end();
        meeting.completeAiProcessing();
        meetingRepository.save(meeting);

        List<MeetingParticipant> participantEntities = participants.stream()
                .map(user -> {
                    MeetingParticipant participant = MeetingParticipant.builder()
                            .meeting(meeting)
                            .user(user)
                            .build();
                    participant.leave();
                    return participant;
                })
                .toList();
        meetingParticipantRepository.saveAll(participantEntities);

        meetingSummaryRepository.save(MeetingSummary.builder()
                .meeting(meeting)
                .summaryText(summaryText)
                .decisions(decisions)
                .issues(issues)
                .rawTranscript(toRawTranscript(transcriptSegments))
                .transcriptSegments(transcriptSegments)
                .build());
    }

    private void createTeamPages(Team team, User author, List<PageSeed> pages) {
        teamPageRepository.saveAll(pages.stream()
                .map(page -> TeamPage.builder()
                        .team(team)
                        .author(author)
                        .title(page.title())
                        .content(page.content().strip())
                        .build())
                .toList());
    }

    private String toRawTranscript(List<MeetingTranscriptSegment> segments) {
        return segments.stream()
                .map(segment -> "%s: %s".formatted(segment.speakerLabel(), segment.text()))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    private MeetingTranscriptSegment segment(
            int sequence,
            String speaker,
            long startSeconds,
            long endSeconds,
            String text
    ) {
        return new MeetingTranscriptSegment(
                sequence,
                speaker,
                startSeconds * 1_000,
                endSeconds * 1_000,
                text
        );
    }

    private PageSeed page(String title, String content) {
        return new PageSeed(title, content);
    }

    private List<Long> findDemoMeetingIds() {
        return entityManager.createQuery("""
                        SELECT summary.meeting.meetingId
                        FROM MeetingSummary summary
                        WHERE summary.meeting.team.project.organization.emailDomain = :domain
                        ORDER BY summary.meeting.meetingId
                        """, Long.class)
                .setParameter("domain", DEMO_ORGANIZATION_DOMAIN)
                .getResultList();
    }

    private List<Long> findDemoTeamPageIds() {
        return entityManager.createQuery("""
                        SELECT page.pageId
                        FROM TeamPage page
                        WHERE page.team.project.organization.emailDomain = :domain
                        ORDER BY page.pageId
                        """, Long.class)
                .setParameter("domain", DEMO_ORGANIZATION_DOMAIN)
                .getResultList();
    }

    private void validatePassword(String demoPassword) {
        if (demoPassword == null || demoPassword.isBlank()) {
            throw new IllegalArgumentException("demo-data.password must not be blank");
        }
    }

    public record SeedResult(
            boolean created,
            List<Long> meetingIds,
            List<Long> teamPageIds
    ) {
        public SeedResult {
            meetingIds = List.copyOf(meetingIds);
            teamPageIds = List.copyOf(teamPageIds);
        }
    }

    private record DemoUsers(
            User productManager,
            User backendLead,
            User productDesigner,
            User marketingManager
    ) {
    }

    private record DemoTeams(Team product, Team backend, Team marketing) {
    }

    private record PageSeed(String title, String content) {
    }
}
