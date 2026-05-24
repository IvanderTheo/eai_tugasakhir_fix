package com.example.pharmacyservice;

import com.example.pharmacyservice.config.KafkaTestMocksConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(KafkaTestMocksConfig.class)
class PharmacyServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}

