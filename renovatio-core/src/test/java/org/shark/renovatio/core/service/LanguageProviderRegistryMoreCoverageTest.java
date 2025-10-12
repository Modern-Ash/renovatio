package org.shark.renovatio.core.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.shark.renovatio.shared.spi.LanguageProvider;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LanguageProviderRegistryMoreCoverageTest {

    @Test
    void getProvider_returnsNull_whenNoProviders() {
        LanguageProviderRegistry reg = new LanguageProviderRegistry();
        assertNull(reg.getProvider("java"));
    }

    @Test
    void registerDefaultProviders_registersBeansFromContext() throws Exception {
        LanguageProvider provider = mock(LanguageProvider.class);
        when(provider.language()).thenReturn("java");
        when(provider.capabilities()).thenReturn(java.util.EnumSet.of(LanguageProvider.Capabilities.ANALYZE));

        ApplicationContext ctx = mock(ApplicationContext.class);
        when(ctx.getBeansOfType(LanguageProvider.class)).thenReturn(Map.of("p1", provider));

        LanguageProviderRegistry reg = new LanguageProviderRegistry();
        // inject mock ApplicationContext
        Field f = LanguageProviderRegistry.class.getDeclaredField("applicationContext");
        f.setAccessible(true);
        f.set(reg, ctx);

        reg.registerDefaultProviders();
        assertNotNull(reg.getProvider("java"));
    }

    @Test
    void generateDescription_formatsNicely() throws Exception {
        LanguageProviderRegistry reg = new LanguageProviderRegistry();
        Method m = LanguageProviderRegistry.class.getDeclaredMethod("generateDescription", String.class, String.class);
        m.setAccessible(true);
        String s = (String) m.invoke(reg, "java", "analyze");
        assertEquals("Analyze for java", s);
    }
}

