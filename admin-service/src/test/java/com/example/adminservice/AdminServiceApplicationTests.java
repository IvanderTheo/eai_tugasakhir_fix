package com.example.adminservice;

import com.example.adminservice.config.KafkaTestMocksConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(KafkaTestMocksConfig.class)
class AdminServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}

