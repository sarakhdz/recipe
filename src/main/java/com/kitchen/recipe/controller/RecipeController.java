package com.kitchen.recipe.controller;

import com.kitchen.recipe.dto.recipe.RecipeIngredientResponse;
import com.kitchen.recipe.dto.recipe.RecipeRequest;
import com.kitchen.recipe.dto.recipe.RecipeResponse;
import com.kitchen.recipe.service.RecipeService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/recipes")
@AllArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    @PostMapping
    public ResponseEntity<RecipeResponse> createRecipe(@Valid @RequestBody RecipeRequest request) {
        RecipeResponse created = recipeService.createRecipe(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecipeResponse> getRecipe(@PathVariable Long id) {
        return ResponseEntity.ok(recipeService.getRecipe(id));
    }

    @GetMapping
    public ResponseEntity<PagedModel<RecipeResponse>> searchRecipes(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer servings,
            @RequestParam(required = false) List<String> include,
            @RequestParam(required = false) List<String> exclude,
            @RequestParam(required = false) List<String> keywords,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<RecipeResponse> page = recipeService.searchRecipes(category, servings, include, exclude, keywords, pageable);
        return ResponseEntity.ok(new PagedModel<>(page));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecipeResponse> updateRecipe(@PathVariable Long id, @Valid @RequestBody RecipeRequest request) {
        return ResponseEntity.ok(recipeService.updateRecipe(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipe(@PathVariable Long id) {
        recipeService.deleteRecipe(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/ingredients")
    public ResponseEntity<List<RecipeIngredientResponse>> getRecipeIngredients(@PathVariable Long id) {
        return ResponseEntity.ok(recipeService.getRecipeIngredients(id));
    }
}
