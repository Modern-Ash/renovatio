package org.shark.renovatio.profile;

import java.util.List;
import java.util.Optional;

/** Storage port for immutable profile-template versions. */
public interface ProfileTemplateRepository {
    MigrationProfileTemplate save(MigrationProfileTemplate template);
    Optional<MigrationProfileTemplate> find(TemplateReference reference);
    List<MigrationProfileTemplate> list();
}
