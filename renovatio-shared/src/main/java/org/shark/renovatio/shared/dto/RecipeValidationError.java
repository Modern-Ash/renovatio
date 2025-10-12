package org.shark.renovatio.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a validation error for an OpenRewrite recipe.
 * Used to report misconfiguration or missing parameters in recipes in a structured, MCP-compliant way.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeValidationError {
    private String recipeName;
    private String message;
}
