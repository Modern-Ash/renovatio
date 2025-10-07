package org.shark.renovatio.shared.dto;

/**
 * Represents a validation error for an OpenRewrite recipe.
 * Used to report misconfiguration or missing parameters in recipes in a structured, MCP-compliant way.
 */
public class RecipeValidationError {
    private final String recipeName;
    private final String message;

    public RecipeValidationError(String recipeName, String message) {
        this.recipeName = recipeName;
        this.message = message;
    }

    public String getRecipeName() {
        return recipeName;
    }

    public String getMessage() {
        return message;
    }
}

