package com._penLearning.Noddi.global.exception;

import com._penLearning.Noddi.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;

@Getter
public class GeneralException extends RuntimeException{
  private final BaseErrorCode errorCode;
  public GeneralException(BaseErrorCode errorCode) {
    super(errorCode.getMessage());
    this.errorCode = errorCode;
  }

}

