package com._penLearning.Noddi.domain.qa.service;

import com._penLearning.Noddi.domain.project.entity.Project;
import com._penLearning.Noddi.domain.project.repository.ProjectMemberRepository;
import com._penLearning.Noddi.domain.qa.dto.QaResponseDto;
import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import com._penLearning.Noddi.domain.qa.entity.QaAnswerRevision;
import com._penLearning.Noddi.domain.qa.entity.QaAnswerSource;
import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.entity.RevisionEditorType;
import com._penLearning.Noddi.domain.qa.entity.SourceType;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRepository;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRevisionRepository;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerSourceRepository;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.team.repository.TeamMemberRepository;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QaAnswerRevisionQueryServiceTest {

    /*
     * 수정 이력의 내용 권한과 AI 출처 권한은 서로 다르다.
     *
     * - 같은 프로젝트 구성원: 모든 답변 버전을 조회할 수 있다.
     * - 질문 대상 팀 구성원: AI 최초 버전의 출처까지 조회할 수 있다.
     * - 프로젝트 외부 사용자: 수정 이력 자체를 조회할 수 없다.
     *
     * 이 테스트는 위 세 경계를 각각 분리해서 검증한다.
     */

    @Mock private QaAnswerRepository answerRepository;
    @Mock private QaAnswerRevisionRepository answerRevisionRepository;
    @Mock private QaAnswerSourceRepository answerSourceRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private TeamMemberRepository teamMemberRepository;

    @Mock private QaAnswer answer;
    @Mock private QaQuestion question;
    @Mock private Team targetTeam;
    @Mock private Project project;
    @Mock private User requester;

    private QaAnswerRevisionQueryService service;

    @BeforeEach
    void setUp() {
        service = new QaAnswerRevisionQueryService(
                answerRepository,
                answerRevisionRepository,
                answerSourceRepository,
                userRepository,
                projectMemberRepository,
                teamMemberRepository
        );
    }

    @Test
    void returnsAllRevisionsAndInitialSourcesToTargetTeamMember() {
        // Given: 대상 팀 담당자가 AI 원문(v1)과 담당자 수정본(v2)을 조회한다.
        allowAnswerAccess(true);

        QaAnswerRevision aiRevision = org.mockito.Mockito.mock(QaAnswerRevision.class);
        QaAnswerRevision humanRevision = org.mockito.Mockito.mock(QaAnswerRevision.class);
        QaAnswerSource source = org.mockito.Mockito.mock(QaAnswerSource.class);
        User reviser = org.mockito.Mockito.mock(User.class);

        LocalDateTime aiCreatedAt = LocalDateTime.of(2026, 8, 17, 10, 0);
        LocalDateTime revisedAt = LocalDateTime.of(2026, 8, 17, 11, 0);

        when(answer.getAnswerId()).thenReturn(10L);

        when(aiRevision.getVersionNumber()).thenReturn(1);
        when(aiRevision.getContent()).thenReturn("AI 최초 답변");
        when(aiRevision.getEditorType()).thenReturn(RevisionEditorType.AI);
        when(aiRevision.getCreatedAt()).thenReturn(aiCreatedAt);

        when(humanRevision.getVersionNumber()).thenReturn(2);
        when(humanRevision.getContent()).thenReturn("담당자가 수정한 최종 답변");
        when(humanRevision.getEditorType()).thenReturn(RevisionEditorType.HUMAN);
        when(humanRevision.getRevisedBy()).thenReturn(reviser);
        when(humanRevision.getCreatedAt()).thenReturn(revisedAt);
        when(reviser.getUserId()).thenReturn(20L);
        when(reviser.getName()).thenReturn("홍길동");

        when(source.getCitationIndex()).thenReturn(1);
        when(source.getSourceType()).thenReturn(SourceType.TRANSCRIPT);
        when(source.getReferenceId()).thenReturn(30L);
        when(source.getSourceTitle()).thenReturn("8월 기획 회의");
        when(source.getExcerpt()).thenReturn("출시일은 9월 5일입니다.");

        when(answerRevisionRepository.findAllByAnswerWithReviserOrderByVersionNumberAsc(answer))
                .thenReturn(List.of(aiRevision, humanRevision));
        when(answerSourceRepository.findByAnswer_AnswerIdOrderByCitationIndexAsc(10L))
                .thenReturn(List.of(source));

        // When: 답변 수정 이력을 조회한다.
        QaResponseDto.AnswerRevisionHistory response =
                service.getAnswerRevisions(1L, 10L);

        // Then: 전체 버전은 시간 순서대로 제공하고, 대상 팀원에게만 v1 출처를 제공한다.
        assertThat(response.getAnswerId()).isEqualTo(10L);
        assertThat(response.getTotalVersions()).isEqualTo(2);
        assertThat(response.isCanViewSources()).isTrue();
        assertThat(response.getRevisions()).hasSize(2);

        QaResponseDto.AnswerRevisionItem version1 = response.getRevisions().get(0);
        assertThat(version1.getVersionNumber()).isEqualTo(1);
        assertThat(version1.getContent()).isEqualTo("AI 최초 답변");
        assertThat(version1.getEditorType()).isEqualTo(RevisionEditorType.AI);
        assertThat(version1.getEditorId()).isNull();
        assertThat(version1.getEditorName()).isEqualTo("AI");
        assertThat(version1.getCreatedAt()).isEqualTo(aiCreatedAt);
        assertThat(version1.getSources()).hasSize(1);
        assertThat(version1.getSources().get(0).getReferenceId()).isEqualTo(30L);

        QaResponseDto.AnswerRevisionItem version2 = response.getRevisions().get(1);
        assertThat(version2.getVersionNumber()).isEqualTo(2);
        assertThat(version2.getEditorId()).isEqualTo(20L);
        assertThat(version2.getEditorName()).isEqualTo("홍길동");
        assertThat(version2.getCreatedAt()).isEqualTo(revisedAt);
        assertThat(version2.getSources()).isEmpty();
    }

    @Test
    void hidesInitialSourcesFromProjectMemberOutsideTargetTeam() {
        // Given: 같은 프로젝트 구성원이지만 질문 대상 팀에는 소속되지 않은 사용자가 조회한다.
        allowAnswerAccess(false);

        QaAnswerRevision aiRevision = org.mockito.Mockito.mock(QaAnswerRevision.class);
        when(answer.getAnswerId()).thenReturn(10L);
        when(aiRevision.getVersionNumber()).thenReturn(1);
        when(aiRevision.getContent()).thenReturn("AI 최초 답변");
        when(aiRevision.getEditorType()).thenReturn(RevisionEditorType.AI);
        when(answerRevisionRepository.findAllByAnswerWithReviserOrderByVersionNumberAsc(answer))
                .thenReturn(List.of(aiRevision));

        // When: 수정 이력을 조회한다.
        QaResponseDto.AnswerRevisionHistory response =
                service.getAnswerRevisions(1L, 10L);

        // Then: 답변 내용은 볼 수 있지만 출처 권한과 출처 목록은 제공하지 않는다.
        assertThat(response.isCanViewSources()).isFalse();
        assertThat(response.getRevisions()).hasSize(1);
        assertThat(response.getRevisions().get(0).getContent()).isEqualTo("AI 최초 답변");
        assertThat(response.getRevisions().get(0).getSources()).isEmpty();

        // 민감한 회의록 발췌문이 DB에서 불필요하게 조회되지 않는지도 함께 검증한다.
        verifyNoInteractions(answerSourceRepository);
    }

    @Test
    void rejectsRequesterOutsideProjectBeforeLoadingRevisionContents() {
        // Given: 답변과 사용자는 존재하지만 요청자가 답변이 속한 프로젝트의 멤버가 아니다.
        when(answerRepository.findByIdWithQuestionTeamAndProject(10L))
                .thenReturn(Optional.of(answer));
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(answer.getQuestion()).thenReturn(question);
        when(question.getTargetTeam()).thenReturn(targetTeam);
        when(targetTeam.getProject()).thenReturn(project);
        when(projectMemberRepository.existsByProjectAndUser(project, requester)).thenReturn(false);

        // When & Then: 프로젝트 권한 단계에서 즉시 차단한다.
        assertThatThrownBy(() -> service.getAnswerRevisions(1L, 10L))
                .isInstanceOf(GeneralException.class);

        // 권한이 없으므로 수정 내용과 출처에는 접근하지 않아야 한다.
        verifyNoInteractions(
                answerRevisionRepository,
                answerSourceRepository,
                teamMemberRepository
        );
    }

    private void allowAnswerAccess(boolean targetTeamMember) {
        // 성공 테스트에서 공통으로 필요한 답변 소속 관계와 프로젝트 권한을 준비한다.
        when(answerRepository.findByIdWithQuestionTeamAndProject(10L))
                .thenReturn(Optional.of(answer));
        when(userRepository.findById(1L)).thenReturn(Optional.of(requester));
        when(answer.getQuestion()).thenReturn(question);
        when(question.getTargetTeam()).thenReturn(targetTeam);
        when(targetTeam.getProject()).thenReturn(project);
        when(projectMemberRepository.existsByProjectAndUser(project, requester)).thenReturn(true);
        when(teamMemberRepository.existsByTeamAndUser(targetTeam, requester))
                .thenReturn(targetTeamMember);
    }
}
