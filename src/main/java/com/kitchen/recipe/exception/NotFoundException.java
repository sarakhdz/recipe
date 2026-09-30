package com.kitchen.recipe.exception;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String resourceType, Object id) {

        super(resourceType + " not found: " + id);
    }
}
