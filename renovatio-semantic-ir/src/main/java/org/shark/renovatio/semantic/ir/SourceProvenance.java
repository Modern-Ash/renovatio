package org.shark.renovatio.semantic.ir;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Content-addressed provenance retained through target emission. */
public record SourceProvenance(String sourcePath, String contentSha256, String sourceLanguage,
                               Optional<String> dialect, List<String> parentEvidenceHashes) {
    public SourceProvenance {
        sourcePath = SemanticIdentity.path(sourcePath);
        contentSha256 = SemanticIdentity.hash(contentSha256, "contentSha256");
        sourceLanguage = SemanticIdentity.text(sourceLanguage, "sourceLanguage");
        dialect = dialect == null ? Optional.empty()
                : dialect.map(value -> SemanticIdentity.text(value, "dialect"));
        parentEvidenceHashes = (parentEvidenceHashes == null ? List.<String>of() : parentEvidenceHashes)
                .stream().map(value -> SemanticIdentity.hash(value, "parentEvidenceHash"))
                .distinct().sorted(Comparator.naturalOrder()).toList();
    }
}
