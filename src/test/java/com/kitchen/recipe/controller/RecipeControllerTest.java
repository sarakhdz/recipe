package com.kitchen.recipe.controller;

import com.kitchen.recipe.dto.recipe.RecipeIngredientResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import tools.jackson.databind.ObjectMapper;
import com.kitchen.recipe.config.SecurityConfig;
import com.kitchen.recipe.dto.recipe.RecipeIngredientRequest;
import com.kitchen.recipe.dto.recipe.RecipeRequest;
import com.kitchen.recipe.dto.recipe.RecipeResponse;
import com.kitchen.recipe.exception.NotFoundException;
import com.kitchen.recipe.service.RecipeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecipeController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.security.username=admin",
        "app.security.password=pass"})
public class RecipeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RecipeService recipeService;

    @Test
    public void getRecipeWithId_returnsTheRecipe() throws Exception {
        RecipeResponse sampleRecipeResponse = sampleRecipeResponse(5L);
        when(recipeService.getRecipe(5L)).thenReturn(sampleRecipeResponse);
        mockMvc.perform(get("/recipes/{id}", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sample Recipe"));
    }

    @Test
    void getRecipeWithId_throwsNotFoundWhenMissing() throws Exception {
        when(recipeService.getRecipe(5L)).thenThrow(new NotFoundException("Recipe", 5L));
        mockMvc.perform(get("/recipes/{id}", 5L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void getRecipeIngredients_returnsIngredients() throws Exception {
        when(recipeService.getRecipeIngredients(5L)).thenReturn(List.of(sampleIngredientResponse()));
        mockMvc.perform(get("/recipes/{id}/ingredients", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Sugar"));
    }

    @Test
    void getRecipeIngredients_throwsNotFoundWhenMissing() throws Exception {
        when(recipeService.getRecipeIngredients(5L)).thenThrow(new NotFoundException("Recipe", 5L));
        mockMvc.perform(get("/recipes/{id}/ingredients", 5L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void searchRecipes_returnsRecipes() throws Exception {
        Page<RecipeResponse> page = new PageImpl<>(List.of(sampleRecipeResponse(1L)));
        when(recipeService.searchRecipes(any(),any(),any(),any(),any(),any(Pageable.class))).thenReturn(page);
        mockMvc.perform(get("/recipes")
                        .param("category", "dinner")
                        .param("servings", "4")
                        .param("include", "sugar", "flour")
                        .param("exclude", "nuts")
                        .param("keywords", "sweet", "quick"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Sample Recipe"));

    }

    @Test
    void deleteRecipe_deletesRecipeWithId() throws Exception {
        mockMvc.perform(delete("/recipes/{id}", 5L).with(httpBasic("admin", "pass")))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteRecipe_throwsNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/recipes/{id}", 5L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createRecipe_createsRecipe() throws Exception {
        RecipeRequest request = sampleRecipeRequest();
        when(recipeService.createRecipe(request)).thenReturn(sampleRecipeResponse(1L));

        mockMvc.perform(post("/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(httpBasic("admin", "pass")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Sample Recipe"));
    }

    @Test
    void createRecipe_throwsNotAuthenticated() throws Exception {
        RecipeRequest request = sampleRecipeRequest();
        mockMvc.perform(post("/recipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createRecipe_returnsBadRequestWhenNameIsBlank() throws Exception {
        RecipeRequest request = new RecipeRequest("", "desc", "dinner", 1, "mix", List.of(new RecipeIngredientRequest("sugar", 200.0, "grams")));
        mockMvc.perform(post("/recipes")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
                    .with(httpBasic("admin","pass")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

    }

    @Test
    void updateRecipe_returnsUpdatedRecipe() throws Exception {
        RecipeRequest request = sampleRecipeRequest();
        when(recipeService.updateRecipe(5L, request)).thenReturn(sampleRecipeResponse(5L));
        mockMvc.perform(put("/recipes/{id}",5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(httpBasic("admin", "pass")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Sample Recipe"));
    }

    @Test
    void updateRecipe_throwsNotFoundWhenMissing() throws Exception {
        RecipeRequest request = sampleRecipeRequest();
        when(recipeService.updateRecipe(5L,request)).thenThrow(new NotFoundException("Recipe", 5L));
        mockMvc.perform(put("/recipes/{id}",5L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
                .with(httpBasic("admin","pass")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }

    @Test
    void updateRecipe_throwsNotAuthenticated() throws Exception {
        RecipeRequest request = sampleRecipeRequest();
        mockMvc.perform(put("/recipes/{id}",5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateRecipe_returnsBadRequestWhenNameIsBlank() throws Exception {
        RecipeRequest request = new RecipeRequest("","desc", "dinner", 1, "mix", List.of(new RecipeIngredientRequest("sugar", 200.0, "grams")));
        mockMvc.perform(put("/recipes/{id}",5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(httpBasic("admin","pass")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        }

    private RecipeResponse sampleRecipeResponse(Long id) {
        return new RecipeResponse(
                id,
                "Sample Recipe",
                "Sample description",
                "dinner",
                4,
                "Mix and cook.",
                List.of()
        );
    }

    private RecipeRequest sampleRecipeRequest() {
        return new RecipeRequest(
                "sample recipe",
                "sample description",
                "dinner",
                5,
                "cook",
                List.of(new RecipeIngredientRequest("sugar", 200.0, "grams"))
        );
    }

    private RecipeIngredientResponse sampleIngredientResponse() {
        return new RecipeIngredientResponse(1L, "Sugar", 200.0, "grams");
    }
}
