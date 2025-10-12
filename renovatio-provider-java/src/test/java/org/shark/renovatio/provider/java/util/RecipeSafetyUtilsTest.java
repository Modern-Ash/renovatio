package org.shark.renovatio.provider.java.util;

import org.junit.jupiter.api.Test;
import org.openrewrite.Recipe;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecipeSafetyUtilsTest {

    @Test
    void nullRecipe_isMissing() {
        assertTrue(RecipeSafetyUtils.hasMissingRequiredParameters(null));
    }

    @Test
    void createEmptyJavaClass_requiresPackageAndClassName() {
        Recipe r = mock(Recipe.class, RETURNS_DEEP_STUBS);
        when(r.getName()).thenReturn("org.openrewrite.java.CreateEmptyJavaClass");
        // Methods getPackageName/getClassName are not present on the mock type -> safeInvoke returns null
        assertTrue(RecipeSafetyUtils.hasMissingRequiredParameters(r));
    }

    @Test
    void explicitRecipeNames_returnMissing() {
        Recipe r = mock(Recipe.class);
        when(r.getName()).thenReturn("org.openrewrite.text.CreateTextFile");
        assertTrue(RecipeSafetyUtils.hasMissingRequiredParameters(r));
    }

    @Test
    void otherwise_returnsSafeWhenNoHintsOfMissing() {
        Recipe r = mock(Recipe.class);
        when(r.getName()).thenReturn("com.example.SafeRecipe");
        when(r.toString()).thenReturn("Recipe{ok='value'}");
        assertFalse(RecipeSafetyUtils.hasMissingRequiredParameters(r));
    }
}

