package com.kitchen.recipe.dto.recipe;

import com.kitchen.recipe.model.RecipeIngredient;

public record RecipeIngredientResponse(
        Long ingredientId,
        String name,
        double quantity,
        String unit
) {
    public static RecipeIngredientResponse from(RecipeIngredient recipeIngredient) {
        return new RecipeIngredientResponse(
                recipeIngredient.getIngredient().getId(),
                recipeIngredient.getIngredient().getName(),
                recipeIngredient.getQuantity(),
                recipeIngredient.getUnit()
        );
    }
}
