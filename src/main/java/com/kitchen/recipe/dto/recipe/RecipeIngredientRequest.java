package com.kitchen.recipe.dto.recipe;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RecipeIngredientRequest(
        @NotBlank(message = "ingredient name is required") String name,
        @NotNull(message = "quantity is required") @Positive(message = "quantity must be greater than 0") Double quantity,
        @NotBlank(message = "unit is required") String unit
) {
}
