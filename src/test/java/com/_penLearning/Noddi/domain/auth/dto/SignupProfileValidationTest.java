package com._penLearning.Noddi.domain.auth.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class SignupProfileValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsTwentyCharactersAndRejectsTwentyOneCharacters() {
        AuthRequestDto.SignupRequestDto request = validRequest();
        ReflectionTestUtils.setField(request, "department", "가".repeat(20));
        ReflectionTestUtils.setField(request, "position", "나".repeat(20));
        assertThat(validator.validate(request)).isEmpty();

        ReflectionTestUtils.setField(request, "department", "가".repeat(21));
        ReflectionTestUtils.setField(request, "position", "나".repeat(21));
        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("department", "position");
    }

    private AuthRequestDto.SignupRequestDto validRequest() {
        AuthRequestDto.SignupRequestDto request = new AuthRequestDto.SignupRequestDto();
        ReflectionTestUtils.setField(request, "organizationId", 1L);
        ReflectionTestUtils.setField(request, "name", "김노디");
        ReflectionTestUtils.setField(request, "email", "member@noddi.com");
        ReflectionTestUtils.setField(request, "password", "Password1!");
        return request;
    }
}
