package com._penLearning.Noddi.domain.project.dto;

import com._penLearning.Noddi.domain.project.entity.Project;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class ProjectResponseDto {

    @Getter
    @Builder
    public static class Info {

        private Long projectId;
        private String name;
        private String description;
        private String createdByName; // 생성자 이름 (UI 표시용)
        private LocalDateTime createdAt;

        public static Info from(Project project) {
            return Info.builder()
                    .projectId(project.getProjectId())
                    .name(project.getName())
                    .description(project.getDescription())
                    .createdByName(project.getCreatedBy().getName()) // User 엔티티의 이름
                    .createdAt(project.getCreatedAt())
                    .build();
        }
    }
}
