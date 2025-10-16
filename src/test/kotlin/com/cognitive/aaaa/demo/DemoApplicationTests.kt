package com.cognitive.aaaa.demo

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource

@Import(TestcontainersConfiguration::class)
@SpringBootTest
@TestPropertySource(properties = [
    "spring.thymeleaf.cache=false",
    "spring.web.resources.cache.cachecontrol.max-age=0"
])
class DemoApplicationTests {

	@Test
	fun contextLoads() {
	}

	@Test
	fun `should start application successfully`() {
		// This test verifies that the Spring Boot application starts without errors
		// and all beans are properly configured
	}

}
