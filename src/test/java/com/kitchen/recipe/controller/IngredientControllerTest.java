package com.kitchen.recipe.controller;

import com.kitchen.recipe.config.SecurityConfig;
import com.kitchen.recipe.dto.IngredientResponse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import com.kitchen.recipe.exception.NotFoundException;
import com.kitchen.recipe.service.IngredientService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(IngredientController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.security.username=admin",
        "app.security.password=pass"})
public class IngredientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IngredientService ingredientService;

    @Test
    void getAllIngredients_returnsAllIngredients() throws Exception {
        when(ingredientService.getAllIngredients()).thenReturn(List.of(sampleIngredientResponse()));
        mockMvc.perform(get("/ingredients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Sugar"));
    }

    @Test
    void getAllIngredients_returnsEmptyListWhenNoIngredients() throws Exception {
        when(ingredientService.getAllIngredients()).thenReturn(List.of());
        mockMvc.perform(get("/ingredients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getIngredientById_returnsIngredient() throws Exception {
        when(ingredientService.getIngredient(1L)).thenReturn(sampleIngredientResponse());
        mockMvc.perform(get("/ingredients/{id}",1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sugar"));
    }

    @Test
    void getIngredientById_throwsNotFoundWhenMissing() throws Exception {
        when(ingredientService.getIngredient(1L)).thenThrow(new NotFoundException("Ingredient", 1L));
        mockMvc.perform(get("/ingredients/{id}", 1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("NOT_FOUND"));
    }


    private IngredientResponse sampleIngredientResponse() {
        return new IngredientResponse(1L, "Sugar", "Sample Note");
    }
}
