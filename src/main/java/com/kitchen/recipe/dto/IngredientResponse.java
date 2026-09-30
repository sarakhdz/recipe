package com.kitchen.recipe.dto;

import com.kitchen.recipe.model.Ingredient;

public record IngredientResponse(
        Long id,
        String name,
        String notes
) {
    public static IngredientResponse from(Ingredient ingredient) {
        return new IngredientResponse(
                ingredient.getId(),
                ingredient.getName(),
                ingredient.getNotes()
        );
    }
}