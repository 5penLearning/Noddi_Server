package com._penLearning.Noddi.global.apiPayload;

import com._penLearning.Noddi.global.apiPayload.code.BaseErrorCode;
import com._penLearning.Noddi.global.apiPayload.code.status.GeneralSuccessCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonPropertyOrder({"isSuccess", "code", "message", "result", "error"})
public class ApiResponse<T> {

    @JsonProperty("isSuccess")
    //중복처리되지 않도록 대문자 Boolean으로 처리
    private final Boolean isSuccess;

    @JsonProperty("code")
    private final String code;

    @JsonProperty("message")
    private final String message;

    @JsonProperty("result")
    private final T result;

    @JsonProperty("error")
    private final Object error;

    //result 있는
    public static <T> ApiResponse<T> onSuccess(String message,T result) {
        return new ApiResponse<>(true, GeneralSuccessCode.OK.getCode(), message, result, null);
    }

    //result 없는
    public static <T> ApiResponse<T> onSuccess(String message) {
        return new ApiResponse<>(true, GeneralSuccessCode.OK.getCode(), message, null, null);
    }

    public static <T> ApiResponse<T> onFailure(BaseErrorCode errorCode, Object error) {
        return new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null, error);
    }
}
