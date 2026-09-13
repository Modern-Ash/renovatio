package org.modernash.renovatio.jcl.emit;

import org.modernash.renovatio.profile.MigrationProfile;
import org.modernash.renovatio.semantic.ir.BatchJob;

/** Service-provider boundary for target-specific batch orchestration source. */
public interface BatchEmitter {
    boolean supports(MigrationProfile.BatchTarget target);
    BatchEmission emit(BatchJob job, MigrationProfile profile);
}
