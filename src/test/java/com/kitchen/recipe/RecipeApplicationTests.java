package com.kitchen.recipe;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@TestPropertySource(properties = {"app.security.username=admin", "app.security.password=pass"})
class RecipeApplicationTests {

	@Test
	void contextLoads() {
	}

}
