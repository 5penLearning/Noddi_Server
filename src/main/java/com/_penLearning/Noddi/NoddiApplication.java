package com._penLearning.Noddi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableJpaAuditing  // BaseEntity의 createdAt, updatedAt 자동 주입 활성화
@SpringBootApplication
@EnableAsync // 비동기 기능 활성화
public class NoddiApplication {

	public static void main(String[] args) {
		SpringApplication.run(NoddiApplication.class, args);
	}

}
