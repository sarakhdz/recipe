package com.kitchen.recipe.dto.recipe;

import com.kitchen.recipe.model.Recipe;

import java.util.List;

public record RecipeResponse(
        Long id,
        String name,
        String description,
        String category,
        int servings,
        String instructions,
        List<RecipeIngredientResponse> ingredients
) {
    public static RecipeResponse from(Recipe recipe) {
        return new RecipeResponse(
                recipe.getId(),
                recipe.getName(),
                recipe.getDescription(),
                recipe.getCategory(),
                recipe.getServings(),
                recipe.getInstructions(),
                recipe.getIngredients().stream()
                        .map(RecipeIngredientResponse::from)
                        .toList()
        );
    }
}
