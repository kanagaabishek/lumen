package com.lumen.server.unit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperties;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestConfiguration.class)
@EnabledIfSystemProperty(named = "cassandra.enabled", matches = "true")
class LumenApplicationTests {

	@Test
	void contextLoads() {
	}

}
