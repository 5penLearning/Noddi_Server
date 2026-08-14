package com._penLearning.Noddi.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI swagger() {
        // Swagger UI 화면에서 JWT 인증 버튼을 사용하기 위한 보안 키 정의
        String securityJwtName = "JWT Token";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(securityJwtName);
        Components components = new Components()
                .addSecuritySchemes(securityJwtName, new SecurityScheme()
                        .name(securityJwtName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("Bearer")
                        .bearerFormat("JWT"));

        return new OpenAPI()
                .info(new Info()
                        .title("Noddi API Document")
                        .version("v0.0.1"))
                .tags(List.of(
                        new Tag().name("Auth API").description("인증 API"),
                        new Tag().name("User API").description("사용자 API"),
                        new Tag().name("Organization API").description("조직 API"),
                        new Tag().name("Project API").description("프로젝트 API"),
                        new Tag().name("Project Member API").description("프로젝트 멤버 API"),
                        new Tag().name("Team API").description("팀 API"),
                        new Tag().name("Team Page API").description("팀 공유 페이지 API"),
                        new Tag().name("Meeting API").description("회의 API"),
                        new Tag().name("ActionItem API").description("회의 할 일 API"),
                        new Tag().name("Q&A API").description("질의응답 API")
                ))
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}
