package com._penLearning.Noddi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class NoddiApplicationTests {

	@MockitoBean
	private VectorStore vectorStore;

	@Test
	void contextLoads() {
	}

}
