package com.kitchen.recipe;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.containsInAnyOrder;

@SpringBootTest
@Transactional
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {"app.security.username=admin", "app.security.password=pass"})
public class RecipeIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception{
        createRecipe("""
        {
          "name": "Tomato Soup",
          "description": "A simple soup",
          "category": "soup",
          "servings": 4,
          "instructions": "Boil water and simmer tomatoes",
          "ingredients": [
            {"name": "tomato", "quantity": 4, "unit": "pcs"},
            {"name": "water", "quantity": 1, "unit": "liter"}
          ]
        }
        """);

        createRecipe("""
        {
          "name": "Chicken Curry",
          "description": "Spicy curry",
          "category": "curry",
          "servings": 2,
          "instructions": "Fry chicken and add spices",
          "ingredients": [
            {"name": "chicken", "quantity": 500, "unit": "g"},
            {"name": "curry powder", "quantity": 2, "unit": "tbsp"}
          ]
        }
        """);

        createRecipe("""
        {
          "name": "Vegetable Stew",
          "description": "Hearty stew",
          "category": "soup",
          "servings": 4,
          "instructions": "Simmer vegetables slowly",
          "ingredients": [
            {"name": "carrot", "quantity": 3, "unit": "pcs"},
            {"name": "potato", "quantity": 2, "unit": "pcs"}
          ]
        }
        """);

        createRecipe("""
        {
          "name": "Fruit Salad",
          "description": "Fresh and light",
          "category": "salad",
          "servings": 6,
          "instructions": "Mix fruits together",
          "ingredients": [
            {"name": "apple", "quantity": 2, "unit": "pcs"},
            {"name": "banana", "quantity": 3, "unit": "pcs"}
          ]
        }
        """);
    }

    private Long createRecipe(String json) throws Exception {
        String response = mockMvc.perform(post("/recipes")
                        .with(httpBasic("admin", "pass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void searchByCategory_returnsOnlyMatchingRecipes() throws Exception {
        mockMvc.perform(get("/recipes").param("category", "soup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].category", everyItem(is("soup"))));
    }

    @Test
    void searchByKeywords_returnsOnlyMatchingRecipes() throws Exception {
        mockMvc.perform(get("/recipes").param("keywords", "simmer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].name", containsInAnyOrder("Tomato Soup", "Vegetable Stew")));
    }

    @Test
    void searchByServings_returnsOnlyMatchingRecipes() throws Exception {
        mockMvc.perform(get("/recipes").param("servings", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].name", containsInAnyOrder("Tomato Soup", "Vegetable Stew")));
    }

    @Test
    void searchByInclude_returnsOnlyMatchingRecipes() throws Exception {
        mockMvc.perform(get("/recipes").param("include", "chicken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Chicken Curry"));
    }

    @Test
    void searchByExclude_returnsOnlyMatchingRecipes() throws Exception {
        mockMvc.perform(get("/recipes").param("exclude", "chicken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.content[*].name", not(hasItem("Chicken Curry"))));
    }

    @Test
    void searchByCategoryAndInclude_combinesCriteria() throws Exception {
        mockMvc.perform(get("/recipes")
                        .param("category", "soup")
                        .param("include", "potato"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Vegetable Stew"));
    }

    @Test
    void searchWithNoCriteria_returnsAllRecipes() throws Exception {
        mockMvc.perform(get("/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(4));
    }

    @Test
    void searchWithNoMatches_returnsEmptyPageNotError() throws Exception {
        mockMvc.perform(get("/recipes").param("category", "dessert"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void getAllRecipes_returnsCollectionWithMultipleRecipes() throws Exception {
        mockMvc.perform(get("/recipes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(4))
                .andExpect(jsonPath("$.content.length()").value(4));
    }

    @Test
    void updateRecipe_persistsChanges() throws Exception {
        Long id = createRecipe("""
                {
                  "name": "Pancakes",
                  "description": "Breakfast",
                  "category": "breakfast",
                  "servings": 2,
                  "instructions": "Mix and fry",
                  "ingredients": [
                    {"name": "flour", "quantity": 200, "unit": "g"}
                  ]
                }
                """);

        mockMvc.perform(put("/recipes/" + id)
                        .with(httpBasic("admin", "pass"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Fluffy Pancakes",
                                  "description": "Better breakfast",
                                  "category": "breakfast",
                                  "servings": 3,
                                  "instructions": "Mix, rest, then fry",
                                  "ingredients": [
                                    {"name": "flour", "quantity": 250, "unit": "g"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Fluffy Pancakes"))
                .andExpect(jsonPath("$.servings").value(3));

        mockMvc.perform(get("/recipes/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Fluffy Pancakes"))
                .andExpect(jsonPath("$.servings").value(3));
    }

    @Test
    void searchWithInvalidServings_returnsBadRequestNotServerError() throws Exception {
        mockMvc.perform(get("/recipes").param("servings", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    @Test
    void deleteRecipe_removesItPermanently() throws Exception {
        Long id = createRecipe("""
                {
                  "name": "Temporary Dish",
                  "description": "Will be deleted",
                  "category": "misc",
                  "servings": 1,
                  "instructions": "N/A",
                  "ingredients": [
                    {"name": "salt", "quantity": 1, "unit": "pinch"}
                  ]
                }
                """);

        mockMvc.perform(delete("/recipes/" + id)
                        .with(httpBasic("admin", "pass")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/recipes/" + id))
                .andExpect(status().isNotFound());
    }

}
