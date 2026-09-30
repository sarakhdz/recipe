package com.kitchen.recipe.dto.recipe;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record RecipeRequest(
        @NotBlank(message = "name is required") String name,
        @NotBlank(message = "description is required") String description,
        @NotBlank(message = "category is required") String category,
        @Positive(message = "servings must be greater than 0") int servings,
        @NotBlank(message = "instructions are required") String instructions,
        @NotEmpty(message = "at least one ingredient is required")
        List<@Valid RecipeIngredientRequest> ingredients
) {
}
