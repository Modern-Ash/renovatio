package org.shark.renovatio.core.service;

import org.junit.jupiter.api.Test;
import org.shark.renovatio.shared.nql.NqlQuery;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LanguageProviderRegistryInternalsTest {

    @Test
    void createNqlQuery_uses_fallback_language_when_null() throws Exception {
        LanguageProviderRegistry reg = new LanguageProviderRegistry();
        Method m = LanguageProviderRegistry.class.getDeclaredMethod("createNqlQuery", Map.class, String.class);
        m.setAccessible(true);
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("nql", "FIND files");
        args.put("language", "cobol");
        NqlQuery q = (NqlQuery) m.invoke(reg, args, null);
        assertEquals("cobol", q.getLanguage());
        assertEquals("FIND files", q.getOriginalQuery());
        assertTrue(q.getParameters().isEmpty());
    }

    @Test
    void shouldSplitRecipeIdentifier_handles_null_and_known_prefixes() throws Exception {
        LanguageProviderRegistry reg = new LanguageProviderRegistry();
        Method m = LanguageProviderRegistry.class.getDeclaredMethod("shouldSplitRecipeIdentifier", String.class);
        m.setAccessible(true);
        assertFalse((Boolean) m.invoke(reg, (Object) null));
        assertFalse((Boolean) m.invoke(reg, ""));
        assertTrue((Boolean) m.invoke(reg, "apply"));
        assertTrue((Boolean) m.invoke(reg, "PLAN"));
        assertFalse((Boolean) m.invoke(reg, "other"));
    }

    @Test
    void convertToMap_unknown_type_returns_error() throws Exception {
        LanguageProviderRegistry reg = new LanguageProviderRegistry();
        Method m = LanguageProviderRegistry.class.getDeclaredMethod("convertToMap", Object.class);
        m.setAccessible(true);
        Map<String, Object> out = (Map<String, Object>) m.invoke(reg, new Object());
        assertEquals(false, out.get("success"));
        assertEquals("error", out.get("type"));
    }
}

