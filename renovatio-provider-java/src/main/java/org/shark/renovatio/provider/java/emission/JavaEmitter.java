package org.shark.renovatio.provider.java.emission;

import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.profile.DocumentationSettings;
import org.shark.renovatio.shared.emission.EmittedArtifact;
import org.shark.renovatio.shared.emission.EmittedArtifacts;
import org.shark.renovatio.shared.emission.TargetModel;
import org.shark.renovatio.shared.emission.TranslationDocumentation;
import org.shark.renovatio.shared.spi.TargetEmitter;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The sole F2 Java target adapter. */
public final class JavaEmitter implements TargetEmitter {
    private static final Pattern HEADER_DECLARATION = Pattern.compile(
            "(?m)^(?:package|import)\\s+[^\\r\\n]+;\\R?");
    private final JavaArtifactRenderer renderer;

    public JavaEmitter(JavaArtifactRenderer renderer) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
    }

    @Override
    public boolean supports(MigrationProfile.Language target) {
        return target == MigrationProfile.Language.JAVA;
    }

    @Override
    public EmittedArtifacts emit(TargetModel model, MigrationProfile profile) {
        Objects.requireNonNull(model, "model");
        if (!model.profile().equals(profile)) throw new IllegalArgumentException("emitter profile must equal target model profile");
        EmittedArtifacts emitted = Objects.requireNonNull(renderer.render(model, profile), "renderer result");
        if (!DocumentationSettings.enabled(profile)) return emitted;
        String documentation = TranslationDocumentation.javadoc(model);
        List<EmittedArtifact> decorated = emitted.artifacts().stream()
                .map(artifact -> artifact.path().endsWith(".java")
                        ? EmittedArtifact.utf8(artifact.path(), document(artifact.utf8Text(), documentation))
                        : artifact)
                .toList();
        return EmittedArtifacts.of(decorated);
    }

    private static String document(String source, String documentation) {
        Matcher matcher = HEADER_DECLARATION.matcher(source);
        int headerEnd = 0;
        while (matcher.find()) headerEnd = matcher.end();
        if (headerEnd == 0) return documentation + source;
        int declarationStart = headerEnd;
        while (declarationStart < source.length() && Character.isWhitespace(source.charAt(declarationStart))) {
            declarationStart++;
        }
        return source.substring(0, headerEnd) + "\n" + documentation + source.substring(declarationStart);
    }
}
