package com.kitchen.recipe.service;

import com.kitchen.recipe.dto.recipe.RecipeIngredientRequest;
import com.kitchen.recipe.dto.recipe.RecipeIngredientResponse;
import com.kitchen.recipe.dto.recipe.RecipeRequest;
import com.kitchen.recipe.dto.recipe.RecipeResponse;
import com.kitchen.recipe.exception.NotFoundException;
import com.kitchen.recipe.model.Ingredient;
import com.kitchen.recipe.model.Recipe;
import com.kitchen.recipe.model.RecipeIngredient;
import com.kitchen.recipe.repository.IngredientsRepository;
import com.kitchen.recipe.repository.RecipeRepository;
import com.kitchen.recipe.repository.spec.RecipeSpecifications;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@AllArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final IngredientsRepository ingredientsRepository;

    public RecipeResponse createRecipe(RecipeRequest request) {
        Recipe recipe = new Recipe();
        applyRequest(recipe, request);
        Recipe saved = recipeRepository.save(recipe);
        return RecipeResponse.from(saved);
    }

    public RecipeResponse getRecipe(Long id) {
        return RecipeResponse.from(findRecipeOrThrow(id));
    }

    public RecipeResponse updateRecipe(Long id, RecipeRequest request) {
        Recipe recipe = findRecipeOrThrow(id);
        applyRequest(recipe, request);
        Recipe saved = recipeRepository.save(recipe);
        return RecipeResponse.from(saved);
    }

    public void deleteRecipe(Long id) {
        if (!recipeRepository.existsById(id)) {
            throw new NotFoundException("Recipe", id);
        }
        recipeRepository.deleteById(id);
    }

    public List<RecipeIngredientResponse> getRecipeIngredients(Long id) {
        Recipe recipe = findRecipeOrThrow(id);
        return recipe.getIngredients().stream()
                .map(RecipeIngredientResponse::from)
                .toList();
    }

    public Page<RecipeResponse> searchRecipes(String category,
                                               Integer servings,
                                               List<String> mustInclude,
                                               List<String> mustExclude,
                                               List<String> keywords,
                                               Pageable pageable) {
        List<String> normalizedInclude = normalize(mustInclude);
        List<String> normalizedExclude = normalize(mustExclude);
        List<String> normalizedKeywords = normalize(keywords);

        List<Specification<Recipe>> specs = Stream.of(
                        RecipeSpecifications.hasCategory(category),
                        RecipeSpecifications.hasServings(servings),
                        RecipeSpecifications.includesAllIngredients(normalizedInclude),
                        RecipeSpecifications.excludesIngredients(normalizedExclude),
                        RecipeSpecifications.instructionsContainAll(normalizedKeywords))
                .filter(Objects::nonNull)
                .toList();

        Specification<Recipe> spec = Specification.allOf(specs);

        return recipeRepository.findAll(spec, pageable)
                .map(RecipeResponse::from);
    }

    private void applyRequest(Recipe recipe, RecipeRequest request) {
        recipe.setName(request.name());
        recipe.setDescription(request.description());
        recipe.setCategory(request.category());
        recipe.setServings(request.servings());
        recipe.setInstructions(request.instructions());

        recipe.getIngredients().clear();
        for (RecipeIngredientRequest ingredientRequest : request.ingredients()) {
            Ingredient ingredient = findOrCreateIngredient(ingredientRequest.name());

            RecipeIngredient line = new RecipeIngredient();
            line.setRecipe(recipe);
            line.setIngredient(ingredient);
            line.setQuantity(ingredientRequest.quantity());
            line.setUnit(ingredientRequest.unit());
            recipe.getIngredients().add(line);
        }
    }

    private Ingredient findOrCreateIngredient(String name) {
        return ingredientsRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> {
                    Ingredient ingredient = new Ingredient();
                    ingredient.setName(name);
                    return ingredientsRepository.save(ingredient);
                });
    }

    private Recipe findRecipeOrThrow(Long id) {
        return recipeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Recipe", id));
    }

    private List<String> normalize(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(v -> v.trim().toLowerCase())
                .toList();
    }

}
