package com._penLearning.Noddi.domain.qa.code;

import com._penLearning.Noddi.domain.auth.entity.AuthMember;
import com._penLearning.Noddi.domain.qa.dto.QaRequestDto;
import com._penLearning.Noddi.domain.qa.dto.QaResponseDto;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Q&A API", description = "팀 간 Q&A(질문/답변) 관리 API")
public interface QaApi {

    @Operation(summary = "질문 등록", description = "특정 팀에게 질문을 등록합니다. (질문자는 대상 팀과 같은 프로젝트 멤버여야 합니다.)")
    ApiResponse<QaResponseDto.CreateQuestion> createQuestion(
            QaRequestDto.CreateQuestion request,
            @Parameter(hidden = true) AuthMember authMember
    );

    @Operation(summary = "내가 작성한 질문 목록 조회", description = "현재 로그인한 유저가 작성한 질문 목록을 최신순으로 조회합니다.")
    ApiResponse<Page<QaResponseDto.QuestionInfo>> getMyQuestions(
            @Parameter(hidden = true) AuthMember authMember,
            Pageable pageable
    );

    @Operation(summary = "특정 팀의 질문 목록 조회", description = "특정 팀에 등록된 질문 목록을 최신순으로 조회합니다.")
    ApiResponse<Page<QaResponseDto.QuestionInfo>> getTeamQuestions(
            @Parameter(description = "대상 팀 ID") @PathVariable Long teamId,
            @Parameter(hidden = true) AuthMember authMember,
            Pageable pageable
    );
    @Operation(summary = "팀 Q&A 피드 조회",
            description = """
                특정 팀에 등록된 질문과 현재 답변을 피드 형태로 조회합니다.
                cursor를 생략하면 최신 질문부터 조회하며,
                응답의 nextCursor를 다음 요청의 cursor로 전달하면 이전 질문을 조회할 수 있습니다.
                """
    )
    ApiResponse<QaResponseDto.Feed> getTeamFeed(
            @Parameter(description = "대상 팀 ID") Long teamId,
            @Parameter(description = "이전 질문 조회를 위한 커서(questionId)", example = "100") Long cursor,
            @Parameter(description = "한 번에 조회할 질문 수, 1 이상 50 이하 (기본값 20)", example = "20") int size,
            @Parameter(hidden = true) AuthMember authMember
    );

    @Operation(summary = "질문 상세 조회", description = "단건 질문의 상세 내용과 (존재할 경우) 답변을 조회합니다.")
    ApiResponse<QaResponseDto.QuestionDetail> getQuestionDetail(
            @Parameter(description = "질문 ID") @PathVariable Long questionId, @Parameter(hidden = true) AuthMember authMember
    );

    @Operation(
            summary = "답변 작성 및 수정",
            description = "대상 팀 멤버가 AI 답변을 수정하거나 AI 최종 실패 질문에 직접 답변합니다."
    )
    ApiResponse<QaResponseDto.ReviseAnswer> reviseAnswer(
            @Parameter(description = "답변 ID") @PathVariable Long answerId,
            QaRequestDto.ReviseAnswer request,
            @Parameter(hidden = true) AuthMember authMember
    );

    @Operation(
            summary = "AI 답변 스트림 구독",
            description = """
                특정 질문의 AI 답변 생성 과정을 SSE로 구독합니다.
                같은 프로젝트의 멤버만 구독할 수 있습니다.
                
                이벤트 종류:
                - connected: SSE 연결 성공
                - snapshot: 현재까지 생성된 전체 답변
                - chunk: 새롭게 생성된 답변 조각
                - completed: 최종 답변 저장 완료
                - failed: 답변 생성 실패
                """
    )
    SseEmitter subscribeAnswerStream(@Parameter(description = "질문 ID")
                                     @PathVariable Long questionId,
                                     @Parameter(hidden = true) AuthMember authMember);
    @Operation(
            summary = "AI 답변 수정 이력 조회",
            description = """
                특정 답변의 AI 최초 원문과 담당자 수정본을 조회합니다.
                수정 이력은 versionNumber 오름차순으로 반환됩니다.
                같은 프로젝트의 구성원은 모든 버전의 답변 내용,
                수정자, 생성 시간을 조회할 수 있습니다.
                AI 최초 답변의 회의록 및 팀 페이지 출처는
                질문 대상 팀 구성원에게만 제공됩니다.
                """
    )
    ApiResponse<QaResponseDto.AnswerRevisionHistory> getAnswerRevisions(
            @Parameter(description = "답변 ID")
            @PathVariable Long answerId,
            @Parameter(hidden = true) AuthMember authMember
    );
}
