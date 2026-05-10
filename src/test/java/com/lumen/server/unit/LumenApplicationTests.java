package com.lumen.server.unit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestConfiguration.class)
class LumenApplicationTests {

	@Test
	void contextLoads() {
	}

}
