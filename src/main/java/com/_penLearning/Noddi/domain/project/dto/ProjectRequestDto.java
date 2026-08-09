package com._penLearning.Noddi.domain.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ProjectRequestDto {

    @Getter
    @NoArgsConstructor
    public static class Create {

        @NotBlank(message = "프로젝트 이름은 필수 입력값입니다.")
        private String name;

        @NotBlank(message = "프로젝트 설명은 필수 입력값입니다.")
        private String description;
    }

    @Getter
    @NoArgsConstructor
    public static class Update {
        @NotBlank(message = "프로젝트 이름은 필수 입력값입니다.")
        private String name;

        @NotBlank(message = "프로젝트 설명은 필수 입력값입니다.")
        private String description;
    }
}
