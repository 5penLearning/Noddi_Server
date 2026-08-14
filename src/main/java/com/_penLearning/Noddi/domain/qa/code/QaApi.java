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

    @Operation(summary = "질문 상세 조회", description = "단건 질문의 상세 내용과 (존재할 경우) 답변을 조회합니다.")
    ApiResponse<QaResponseDto.QuestionDetail> getQuestionDetail(
            @Parameter(description = "질문 ID") @PathVariable Long questionId, @Parameter(hidden = true) AuthMember authMember
    );

    @Operation(summary = "AI 답변 수정", description = "대상 팀 멤버가 AI 답변을 수정하고 마지막 수정자를 기록합니다.")
    ApiResponse<QaResponseDto.ReviseAnswer> reviseAnswer(
            @Parameter(description = "답변 ID") @PathVariable Long answerId,
            QaRequestDto.ReviseAnswer request,
            @Parameter(hidden = true) AuthMember authMember
    );

}
