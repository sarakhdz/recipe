package com.kitchen.recipe.service;

import com.kitchen.recipe.dto.IngredientResponse;
import com.kitchen.recipe.exception.NotFoundException;
import com.kitchen.recipe.repository.IngredientsRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class IngredientService {

    private final IngredientsRepository ingredientsRepository;

    public List<IngredientResponse> getAllIngredients() {
        return ingredientsRepository.findAll().stream()
                .map(IngredientResponse::from)
                .toList();
    }

    public IngredientResponse getIngredient(Long id) {
        return ingredientsRepository.findById(id)
                .map(IngredientResponse::from)
                .orElseThrow(() -> new NotFoundException("Ingredient", id));
    }
}
