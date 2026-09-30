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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private IngredientsRepository ingredientsRepository;

    private RecipeService recipeService;

    @BeforeEach
    void setUp() {
        recipeService = new RecipeService(recipeRepository, ingredientsRepository);
    }

    // ---------- createRecipe ----------

    @Test
    void createRecipe_reusesExistingIngredientWhenNameMatches() {
        RecipeIngredientRequest ingredientRequest = new RecipeIngredientRequest("Flour", 2.0, "cups");
        RecipeRequest request = new RecipeRequest(
                "Pancakes", "Fluffy pancakes", "Breakfast", 4, "Mix and cook",
                List.of(ingredientRequest));

        Ingredient existingFlour = new Ingredient();
        existingFlour.setId(1L);
        existingFlour.setName("Flour");
        when(ingredientsRepository.findByNameIgnoreCase("Flour")).thenReturn(Optional.of(existingFlour));

        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> {
            Recipe saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        RecipeResponse response = recipeService.createRecipe(request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("Pancakes");
        assertThat(response.ingredients()).hasSize(1);

        RecipeIngredientResponse ingredientResponse = response.ingredients().get(0);
        assertThat(ingredientResponse.ingredientId()).isEqualTo(1L);
        assertThat(ingredientResponse.name()).isEqualTo("Flour");
        assertThat(ingredientResponse.quantity()).isEqualTo(2.0);

        // We found an existing ingredient, so we should never have created a new one.
        verify(ingredientsRepository, never()).save(any(Ingredient.class));
    }

    @Test
    void createRecipe_createsNewIngredientWhenNoneExistsYet() {
        RecipeIngredientRequest ingredientRequest = new RecipeIngredientRequest("Cinnamon", 1.0, "tsp");
        RecipeRequest request = new RecipeRequest(
                "Spiced Cake", "A warm spiced cake", "Dessert", 6, "Bake at 180C",
                List.of(ingredientRequest));

        when(ingredientsRepository.findByNameIgnoreCase("Cinnamon")).thenReturn(Optional.empty());
        when(ingredientsRepository.save(any(Ingredient.class))).thenAnswer(invocation -> {
            Ingredient toSave = invocation.getArgument(0);
            toSave.setId(99L);
            return toSave;
        });
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RecipeResponse response = recipeService.createRecipe(request);

        assertThat(response.ingredients()).hasSize(1);
        assertThat(response.ingredients().get(0).ingredientId()).isEqualTo(99L);
        assertThat(response.ingredients().get(0).name()).isEqualTo("Cinnamon");

        verify(ingredientsRepository).save(any(Ingredient.class));
    }

    // ---------- getRecipe ----------

    @Test
    void getRecipe_returnsRecipeWhenFound() {
        Recipe recipe = sampleRecipe(5L);
        when(recipeRepository.findById(5L)).thenReturn(Optional.of(recipe));

        RecipeResponse response = recipeService.getRecipe(5L);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.name()).isEqualTo("Sample Recipe");
    }

    @Test
    void getRecipe_throwsNotFoundWhenMissing() {
        when(recipeRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recipeService.getRecipe(5L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("5");
    }

    // ---------- updateRecipe ----------

    @Test
    void updateRecipe_overwritesFieldsAndIngredients() {
        Recipe existing = sampleRecipe(7L);
        when(recipeRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Ingredient sugar = new Ingredient();
        sugar.setId(2L);
        sugar.setName("Sugar");
        when(ingredientsRepository.findByNameIgnoreCase("Sugar")).thenReturn(Optional.of(sugar));

        RecipeIngredientRequest newIngredient = new RecipeIngredientRequest("Sugar", 1.0, "cup");
        RecipeRequest request = new RecipeRequest(
                "Updated Name", "Updated description", "Dessert", 2, "New instructions",
                List.of(newIngredient));

        RecipeResponse response = recipeService.updateRecipe(7L, request);

        assertThat(response.name()).isEqualTo("Updated Name");
        assertThat(response.servings()).isEqualTo(2);
        // the old ingredient line (from sampleRecipe) should be gone, replaced by just "Sugar"
        assertThat(response.ingredients()).hasSize(1);
        assertThat(response.ingredients().get(0).name()).isEqualTo("Sugar");
    }

    @Test
    void updateRecipe_throwsNotFoundWhenMissing() {
        when(recipeRepository.findById(7L)).thenReturn(Optional.empty());

        RecipeRequest request = new RecipeRequest(
                "Name", "Description", "Category", 1, "Instructions",
                List.of(new RecipeIngredientRequest("Salt", 1.0, "pinch")));

        assertThatThrownBy(() -> recipeService.updateRecipe(7L, request))
                .isInstanceOf(NotFoundException.class);

        verify(recipeRepository, never()).save(any(Recipe.class));
    }

    // ---------- deleteRecipe ----------

    @Test
    void deleteRecipe_deletesWhenExists() {
        when(recipeRepository.existsById(3L)).thenReturn(true);

        recipeService.deleteRecipe(3L);

        verify(recipeRepository).deleteById(3L);
    }

    @Test
    void deleteRecipe_throwsNotFoundWhenMissing() {
        when(recipeRepository.existsById(3L)).thenReturn(false);

        assertThatThrownBy(() -> recipeService.deleteRecipe(3L))
                .isInstanceOf(NotFoundException.class);

        verify(recipeRepository, never()).deleteById(any());
    }

    // ---------- getRecipeIngredients ----------

    @Test
    void getRecipeIngredients_returnsIngredientsWhenFound() {
        Recipe recipe = sampleRecipe(4L);
        when(recipeRepository.findById(4L)).thenReturn(Optional.of(recipe));

        List<RecipeIngredientResponse> result = recipeService.getRecipeIngredients(4L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Sugar");
    }

    @Test
    void getRecipeIngredients_throwsNotFoundWhenMissing() {
        when(recipeRepository.findById(4L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recipeService.getRecipeIngredients(4L))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- searchRecipes ----------

    @Test
    void searchRecipes_mapsRepositoryPageToResponsePage() {
        Recipe recipe = sampleRecipe(1L);
        Page<Recipe> repositoryPage = new PageImpl<>(List.of(recipe));

        when(recipeRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(repositoryPage);

        Page<RecipeResponse> result = recipeService.searchRecipes(
                "Dessert", 4, List.of("Sugar"), List.of("Nuts"), List.of("bake"),
                PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(1L);
    }

    // ---------- test helper ----------

    private Recipe sampleRecipe(Long id) {
        Recipe recipe = new Recipe();
        recipe.setId(id);
        recipe.setName("Sample Recipe");
        recipe.setDescription("Sample description");
        recipe.setCategory("Dessert");
        recipe.setServings(4);
        recipe.setInstructions("Bake it");

        Ingredient ingredient = new Ingredient();
        ingredient.setId(1L);
        ingredient.setName("Sugar");

        RecipeIngredient line = new RecipeIngredient();
        line.setId(1L);
        line.setRecipe(recipe);
        line.setIngredient(ingredient);
        line.setQuantity(1.0);
        line.setUnit("cup");

        recipe.getIngredients().add(line);
        return recipe;
    }
}
