package com._penLearning.Noddi.domain.qa.entity;

import com._penLearning.Noddi.domain.qa.dto.QaResponseDto;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.global.exception.GeneralException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QaQuestionTest {

    @Test
    void questionFollowsAiAnswerLifecycle() {
        QaQuestion question = createQuestion();

        assertThat(question.getStatus()).isEqualTo(QaStatus.PENDING);

        question.startProcessing();
        assertThat(question.getStatus()).isEqualTo(QaStatus.PROCESSING);

        question.markAsAnswered();
        assertThat(question.getStatus()).isEqualTo(QaStatus.ANSWERED);
    }

    @Test
    void failedQuestionCanBeRetried() {
        QaQuestion question = createQuestion();

        question.startProcessing();
        question.markAsFailed();
        assertThat(question.getStatus()).isEqualTo(QaStatus.FAILED);

        question.startProcessing();
        assertThat(question.getStatus()).isEqualTo(QaStatus.PROCESSING);
    }

    @Test
    void questionCannotBeCompletedBeforeProcessingStarts() {
        QaQuestion question = createQuestion();

        assertThatThrownBy(question::markAsAnswered)
                .isInstanceOf(GeneralException.class);
    }

    @Test
    void revisedAnswerKeepsOnlyCurrentContentAndLastReviser() {
        QaQuestion question = createQuestion();
        QaAnswer answer = QaAnswer.createAiAnswer(question, "original AI answer");
        answer.revise("official revised answer", question.getQuestioner());

        assertThat(answer.getContent()).isEqualTo("official revised answer");
        assertThat(answer.getAnswerType()).isEqualTo(AnswerType.AI);
        assertThat(answer.isRevised()).isTrue();
        assertThat(answer.getRevisedBy()).isEqualTo(question.getQuestioner());

        QaResponseDto.AnswerInfo response = QaResponseDto.AnswerInfo.from(answer);
        assertThat(response.getContent()).isEqualTo("official revised answer");
        assertThat(response.isRevised()).isTrue();
        assertThat(response.getLastRevisedByName()).isEqualTo("questioner");
    }

    private QaQuestion createQuestion() {
        User questioner = User.builder()
                .email("questioner@example.com")
                .name("questioner")
                .password("password")
                .build();
        Team targetTeam = Team.builder()
                .name("target team")
                .createdBy(questioner)
                .build();

        return QaQuestion.builder()
                .questioner(questioner)
                .targetTeam(targetTeam)
                .content("question")
                .build();
    }
}
