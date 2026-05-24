package com.example.medicalservice;

import com.example.medicalservice.config.KafkaTestMocksConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(KafkaTestMocksConfig.class)
class MedicalServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}

