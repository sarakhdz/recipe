package com.kitchen.recipe.service;

import com.kitchen.recipe.dto.IngredientResponse;
import com.kitchen.recipe.exception.NotFoundException;
import com.kitchen.recipe.model.Ingredient;
import com.kitchen.recipe.repository.IngredientsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IngredientServiceTest {

    @Mock
    private IngredientsRepository ingredientsRepository;

    private IngredientService ingredientService;

    @BeforeEach
    void setUp() {
        ingredientService = new IngredientService(ingredientsRepository);
    }

    // ---------- getIngredient ----------

    @Test
    void getIngredient_returnsIngredientWhenFound() {
        Ingredient ingredient = sampleIngredient(5L);
        when(ingredientsRepository.findById(5L)).thenReturn(Optional.of(ingredient));

        IngredientResponse response = ingredientService.getIngredient(5L);

        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.name()).isEqualTo("sample ingredient");
        assertThat(response.notes()).isEqualTo("sample note");
    }

    @Test
    void getIngredient_throwsNotFoundWhenMissing() {
        when(ingredientsRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ingredientService.getIngredient(5L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("5");
    }

    // ---------- getAllIngredients ----------

    @Test
    void getAllIngredients_returnsAllIngredientsMappedToResponses() {
        Ingredient ingredient1 = sampleIngredient(1L);
        Ingredient ingredient2 = sampleIngredient(2L);
        ingredient2.setName("second ingredient");

        when(ingredientsRepository.findAll()).thenReturn(List.of(ingredient1, ingredient2));

        List<IngredientResponse> response = ingredientService.getAllIngredients();

        assertThat(response).hasSize(2);
        assertThat(response.get(0).name()).isEqualTo("sample ingredient");
        assertThat(response.get(1).name()).isEqualTo("second ingredient");
    }

    // ---------- test helper ----------

    private Ingredient sampleIngredient(Long id) {
        Ingredient ingredient = new Ingredient();
        ingredient.setId(id);
        ingredient.setName("sample ingredient");
        ingredient.setNotes("sample note");
        return ingredient;
    }
}
