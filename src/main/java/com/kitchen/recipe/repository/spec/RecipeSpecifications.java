package com.kitchen.recipe.repository.spec;

import com.kitchen.recipe.model.Ingredient;
import com.kitchen.recipe.model.Recipe;
import com.kitchen.recipe.model.RecipeIngredient;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * One static method per search criterion. Each returns null when the criterion
 * doesn't apply (Specification.where(...).and(...) skips null specs automatically),
 * so the database only does the filtering that was actually requested.
 */
public final class RecipeSpecifications {

    private RecipeSpecifications() {
    }

    public static Specification<Recipe> hasCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        String normalized = category.trim().toLowerCase();
        return (root, query, cb) -> cb.equal(cb.lower(root.get("category")), normalized);
    }

    public static Specification<Recipe> hasServings(Integer servings) {
        if (servings == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("servings"), servings);
    }

    /**
     * Recipe must contain EVERY ingredient in `names`.
     * One EXISTS subquery per required ingredient, ANDed together, so a match
     * requires each one to be present independently (not just any one of them).
     */
    public static Specification<Recipe> includesAllIngredients(List<String> names) {
        if (names == null || names.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> {
            Predicate[] existsPerIngredient = names.stream()
                    .map(name -> existsLineItemWithIngredientName(root, query, cb, name))
                    .toArray(Predicate[]::new);
            return cb.and(existsPerIngredient);
        };
    }

    /**
     * Recipe must contain NONE of the ingredients in `names`.
     * A single NOT EXISTS subquery: no recipe_ingredients row for this recipe
     * may reference any of the excluded ingredient names.
     */
    public static Specification<Recipe> excludesIngredients(List<String> names) {
        if (names == null || names.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<RecipeIngredient> line = subquery.from(RecipeIngredient.class);
            Join<RecipeIngredient, Ingredient> ingredient = line.join("ingredient");

            List<String> lowered = names.stream().map(String::toLowerCase).toList();

            subquery.select(line.get("id"))
                    .where(
                            cb.equal(line.get("recipe"), root),
                            cb.lower(ingredient.get("name")).in(lowered)
                    );

            return cb.not(cb.exists(subquery));
        };
    }

    /**
     * Instructions text must contain EVERY keyword (case-insensitive substring match).
     */
    public static Specification<Recipe> instructionsContainAll(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> {
            Predicate[] likePerKeyword = keywords.stream()
                    .map(word -> cb.like(cb.lower(root.get("instructions")), "%" + word.toLowerCase() + "%"))
                    .toArray(Predicate[]::new);
            return cb.and(likePerKeyword);
        };
    }

    private static Predicate existsLineItemWithIngredientName(Root<Recipe> root,
                                                                CriteriaQuery<?> query,
                                                                CriteriaBuilder cb,
                                                                String name) {
        Subquery<Long> subquery = query.subquery(Long.class);
        Root<RecipeIngredient> line = subquery.from(RecipeIngredient.class);
        Join<RecipeIngredient, Ingredient> ingredient = line.join("ingredient");

        subquery.select(line.get("id"))
                .where(
                        cb.equal(line.get("recipe"), root),
                        cb.equal(cb.lower(ingredient.get("name")), name.toLowerCase())
                );

        return cb.exists(subquery);
    }
}
