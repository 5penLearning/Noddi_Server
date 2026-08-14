package com._penLearning.Noddi.domain.qa.service;

import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.project.repository.ProjectMemberRepository;
import com._penLearning.Noddi.domain.qa.dto.QaResponseDto;
import com._penLearning.Noddi.domain.qa.entity.AnswerType;
import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import com._penLearning.Noddi.domain.qa.entity.QaAnswerSource;
import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.entity.QaStatus;
import com._penLearning.Noddi.domain.qa.entity.SourceType;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRepository;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerSourceRepository;
import com._penLearning.Noddi.domain.qa.repository.QaQuestionRepository;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.team.repository.TeamRepository;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QaQuestionQueryServiceTest {

    /*
     * 이 테스트는 DB까지 연결하는 통합 테스트가 아니라 QaQuestionQueryService만 검증하는 단위 테스트다.
     * Repository는 모두 Mock으로 대체해서 다음 흐름에 집중한다.
     *
     * 1. 조회 권한을 검사한다.
     * 2. 질문을 size + 1개 조회해 다음 페이지 존재 여부를 판단한다.
     * 3. 답변과 출처를 한 번씩 일괄 조회한다.
     * 4. 질문-답변-출처를 묶어 화면에 필요한 Feed DTO로 변환한다.
     */

    @Mock
    private QaQuestionRepository qaQuestionRepository;

    @Mock
    private QaAnswerRepository qaAnswerRepository;

    @Mock
    private QaAnswerSourceRepository qaAnswerSourceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @Mock
    private Team targetTeam;

    @Mock
    private Project project;

    @Mock
    private User requester;

    private QaQuestionQueryService service;

    @BeforeEach
    void setUp() {
        service = new QaQuestionQueryService(
                qaQuestionRepository,
                qaAnswerRepository,
                qaAnswerSourceRepository,
                userRepository,
                teamRepository,
                projectMemberRepository
        );
    }

    @Test
    void returnsQuestionAndAnswerFeedInChronologicalOrder() {
        // Given: 최신순으로 조회된 질문 3개 중 실제 응답에는 요청한 size(2개)만 포함되는 상황이다.
        // 세 번째 질문은 데이터가 더 남았는지 확인하기 위한 size + 1번째 데이터다.
        QaQuestion newestQuestion = mock(QaQuestion.class);
        QaQuestion olderQuestion = mock(QaQuestion.class);
        QaQuestion nextPageQuestion = mock(QaQuestion.class);
        User newestQuestioner = mock(User.class);
        User olderQuestioner = mock(User.class);
        QaAnswer olderAnswer = mock(QaAnswer.class);
        QaAnswerSource answerSource = mock(QaAnswerSource.class);

        LocalDateTime olderQuestionTime = LocalDateTime.of(2026, 8, 15, 10, 0);
        LocalDateTime newestQuestionTime = LocalDateTime.of(2026, 8, 15, 10, 10);
        LocalDateTime answerTime = LocalDateTime.of(2026, 8, 15, 10, 0, 5);

        allowProjectAccess();

        when(newestQuestion.getQuestionId()).thenReturn(103L);
        when(newestQuestion.getQuestioner()).thenReturn(newestQuestioner);
        when(newestQuestion.getContent()).thenReturn("출시 담당자는 누구인가요?");
        when(newestQuestion.getStatus()).thenReturn(QaStatus.PROCESSING);
        when(newestQuestion.getCreatedAt()).thenReturn(newestQuestionTime);
        when(newestQuestioner.getUserId()).thenReturn(7L);
        when(newestQuestioner.getName()).thenReturn("홍길동");

        when(olderQuestion.getQuestionId()).thenReturn(102L);
        when(olderQuestion.getQuestioner()).thenReturn(olderQuestioner);
        when(olderQuestion.getContent()).thenReturn("출시일은 언제인가요?");
        when(olderQuestion.getStatus()).thenReturn(QaStatus.ANSWERED);
        when(olderQuestion.getCreatedAt()).thenReturn(olderQuestionTime);
        when(olderQuestioner.getUserId()).thenReturn(5L);
        when(olderQuestioner.getName()).thenReturn("김유진");

        when(olderAnswer.getAnswerId()).thenReturn(201L);
        when(olderAnswer.getQuestion()).thenReturn(olderQuestion);
        when(olderAnswer.getContent()).thenReturn("출시일은 9월 5일입니다.");
        when(olderAnswer.getAnswerType()).thenReturn(AnswerType.AI);
        when(olderAnswer.isRevised()).thenReturn(false);
        when(olderAnswer.getCreatedAt()).thenReturn(answerTime);
        when(olderAnswer.getUpdatedAt()).thenReturn(answerTime);

        when(answerSource.getAnswer()).thenReturn(olderAnswer);
        when(answerSource.getCitationIndex()).thenReturn(1);
        when(answerSource.getSourceType()).thenReturn(SourceType.TRANSCRIPT);
        when(answerSource.getReferenceId()).thenReturn(30L);
        when(answerSource.getSourceTitle()).thenReturn("8월 기획 회의");
        when(answerSource.getExcerpt()).thenReturn("출시일을 9월 5일로 변경합니다.");

        when(qaQuestionRepository.findFeedByTargetTeam(
                eq(targetTeam),
                isNull(),
                any(Pageable.class)
        )).thenReturn(List.of(newestQuestion, olderQuestion, nextPageQuestion));
        when(qaAnswerRepository.findAllByQuestionsWithReviser(anyCollection()))
                .thenReturn(List.of(olderAnswer));
        when(qaAnswerSourceRepository.findAllByAnswersOrderByCitationIndex(anyCollection()))
                .thenReturn(List.of(answerSource));

        // When: 첫 페이지를 2개 크기로 조회한다.
        QaResponseDto.Feed response = service.getTeamFeed(1L, 10L, null, 2);

        // Then: 프로젝트/팀 정보와 다음 페이지 정보가 응답에 정확히 들어간다.
        assertThat(response.getProjectId()).isEqualTo(1L);
        assertThat(response.getProjectName()).isEqualTo("노디프로젝트");
        assertThat(response.getTeamId()).isEqualTo(10L);
        assertThat(response.getTeamName()).isEqualTo("마케팅팀");
        assertThat(response.isHasNext()).isTrue();
        assertThat(response.getNextCursor()).isEqualTo(102L);
        assertThat(response.getItems()).hasSize(2);

        QaResponseDto.FeedItem firstItem = response.getItems().get(0);
        QaResponseDto.FeedItem secondItem = response.getItems().get(1);

        // Repository는 최신순(103 -> 102)으로 반환하지만, 채팅 화면에는 과거 질문부터 보이도록
        // 서비스가 응답 순서를 102 -> 103으로 뒤집었는지 확인한다.
        assertThat(firstItem.getQuestion().getQuestionId()).isEqualTo(102L);
        assertThat(firstItem.getStatus()).isEqualTo(QaStatus.ANSWERED);
        assertThat(firstItem.getAnswer()).isNotNull();
        assertThat(firstItem.getAnswer().getAnswerId()).isEqualTo(201L);
        assertThat(firstItem.getAnswer().getContent()).isEqualTo("출시일은 9월 5일입니다.");
        assertThat(firstItem.getAnswer().getSources()).hasSize(1);
        assertThat(firstItem.getAnswer().getSources().get(0).getSourceTitle())
                .isEqualTo("8월 기획 회의");

        assertThat(secondItem.getQuestion().getQuestionId()).isEqualTo(103L);
        assertThat(secondItem.getStatus()).isEqualTo(QaStatus.PROCESSING);
        assertThat(secondItem.getAnswer()).isNull();

        // hasNext를 별도 count 쿼리 없이 판단하기 위해 요청 크기보다 1개 더 조회했는지 확인한다.
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(qaQuestionRepository).findFeedByTargetTeam(
                eq(targetTeam),
                isNull(),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(3);

        // size + 1번째 질문은 다음 페이지 확인용이므로 제외하고,
        // 실제 응답에 포함될 질문 2개에 대해서만 답변과 출처를 일괄 조회했는지 확인한다.
        verify(qaAnswerRepository).findAllByQuestionsWithReviser(
                List.of(newestQuestion, olderQuestion)
        );
        verify(qaAnswerSourceRepository).findAllByAnswersOrderByCitationIndex(
                List.of(olderAnswer)
        );
    }

    @Test
    void returnsEmptySourcesWhenAnswerHasNoCitation() {
        // Given: 답변은 있지만 근거로 인용한 회의록 조각이 없는 질문이다.
        QaQuestion question = mock(QaQuestion.class);
        User questioner = mock(User.class);
        QaAnswer answer = mock(QaAnswer.class);
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 15, 11, 0);

        allowProjectAccess();

        when(question.getQuestionId()).thenReturn(101L);
        when(question.getQuestioner()).thenReturn(questioner);
        when(question.getContent()).thenReturn("자료에 없는 질문");
        when(question.getStatus()).thenReturn(QaStatus.ANSWERED);
        when(question.getCreatedAt()).thenReturn(createdAt);
        when(questioner.getUserId()).thenReturn(5L);
        when(questioner.getName()).thenReturn("김유진");

        when(answer.getAnswerId()).thenReturn(201L);
        when(answer.getQuestion()).thenReturn(question);
        when(answer.getContent()).thenReturn("제공된 팀 자료에서 확인할 수 없습니다.");
        when(answer.getAnswerType()).thenReturn(AnswerType.AI);
        when(answer.isRevised()).thenReturn(false);
        when(answer.getCreatedAt()).thenReturn(createdAt);
        when(answer.getUpdatedAt()).thenReturn(createdAt);

        when(qaQuestionRepository.findFeedByTargetTeam(
                eq(targetTeam),
                isNull(),
                any(Pageable.class)
        )).thenReturn(List.of(question));
        when(qaAnswerRepository.findAllByQuestionsWithReviser(anyCollection()))
                .thenReturn(List.of(answer));
        when(qaAnswerSourceRepository.findAllByAnswersOrderByCitationIndex(anyCollection()))
                .thenReturn(List.of());

        // When: Q&A 피드를 조회한다.
        QaResponseDto.Feed response = service.getTeamFeed(1L, 10L, null, 20);

        // Then: 답변은 정상적으로 반환하고 sources는 null이 아닌 빈 배열로 내려준다.
        // 프론트에서는 null 여부를 따로 검사하지 않고 배열로 일관되게 처리할 수 있다.
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getAnswer()).isNotNull();
        assertThat(response.getItems().get(0).getAnswer().getSources()).isEmpty();
    }

    @Test
    void returnsEmptyFeedWhenTeamHasNoQuestions() {
        // Given: 조회 권한은 있지만 해당 팀에 등록된 질문이 없다.
        allowProjectAccess();
        when(qaQuestionRepository.findFeedByTargetTeam(
                eq(targetTeam),
                isNull(),
                any(Pageable.class)
        )).thenReturn(List.of());

        // When: Q&A 피드를 조회한다.
        QaResponseDto.Feed response = service.getTeamFeed(1L, 10L, null, 20);

        // Then: 빈 목록과 함께 다음 페이지가 없다는 정보를 반환한다.
        assertThat(response.getItems()).isEmpty();
        assertThat(response.isHasNext()).isFalse();
        assertThat(response.getNextCursor()).isNull();

        // 질문이 없으면 답변/출처를 조회할 필요가 없으므로 불필요한 Repository 호출도 없어야 한다.
        verifyNoInteractions(qaAnswerRepository, qaAnswerSourceRepository);
    }

    @Test
    void rejectsRequesterWhoIsNotProjectMember() {
        // Given: 팀과 사용자는 존재하지만 사용자가 해당 팀이 속한 프로젝트의 멤버가 아니다.
        when(teamRepository.findById(10L)).thenReturn(Optional.of(targetTeam));
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(targetTeam.getProject()).thenReturn(project);
        when(projectMemberRepository.existsByProjectAndUser(project, requester)).thenReturn(false);

        // When & Then: 프로젝트 외부 사용자의 피드 조회 요청은 예외로 거절한다.
        assertThatThrownBy(() -> service.getTeamFeed(1L, 10L, null, 20))
                .isInstanceOf(GeneralException.class);

        // 권한 검증에 실패한 뒤에는 실제 Q&A 데이터를 조회하지 않아야 한다.
        verifyNoInteractions(
                qaQuestionRepository,
                qaAnswerRepository,
                qaAnswerSourceRepository
        );
    }

    @Test
    void rejectsInvalidFeedSize() {
        // When & Then: 허용 범위(1~50)를 벗어난 페이지 크기는 즉시 거절한다.
        assertThatThrownBy(() -> service.getTeamFeed(1L, 10L, null, 0))
                .isInstanceOf(GeneralException.class);
        assertThatThrownBy(() -> service.getTeamFeed(1L, 10L, null, 51))
                .isInstanceOf(GeneralException.class);

        // 입력값 자체가 잘못됐으므로 팀/사용자/피드 조회까지 진행하지 않아야 한다.
        verifyNoInteractions(
                teamRepository,
                userRepository,
                projectMemberRepository,
                qaQuestionRepository,
                qaAnswerRepository,
                qaAnswerSourceRepository
        );
    }

    @Test
    void rejectsInvalidFeedCursor() {
        // When & Then: 실제 질문 ID가 될 수 없는 0 이하 cursor는 즉시 거절한다.
        assertThatThrownBy(() -> service.getTeamFeed(1L, 10L, 0L, 20))
                .isInstanceOf(GeneralException.class);

        // cursor 검증에서 실패했으므로 Repository에는 접근하지 않아야 한다.
        verifyNoInteractions(
                teamRepository,
                userRepository,
                projectMemberRepository,
                qaQuestionRepository,
                qaAnswerRepository,
                qaAnswerSourceRepository
        );
    }

    private void allowProjectAccess() {
        // 여러 성공 테스트에서 반복되는 "요청자가 프로젝트 멤버인 상황"을 공통으로 준비한다.
        when(teamRepository.findById(10L)).thenReturn(Optional.of(targetTeam));
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(targetTeam.getProject()).thenReturn(project);
        when(targetTeam.getTeamId()).thenReturn(10L);
        when(targetTeam.getName()).thenReturn("마케팅팀");
        when(project.getProjectId()).thenReturn(1L);
        when(project.getName()).thenReturn("노디프로젝트");
        when(projectMemberRepository.existsByProjectAndUser(project, requester)).thenReturn(true);
    }
}
