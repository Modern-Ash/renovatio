package org.shark.renovatio.jcl.emit;

import org.shark.renovatio.profile.MigrationProfile;
import org.shark.renovatio.semantic.ir.BatchJob;

/** Service-provider boundary for target-specific batch orchestration source. */
public interface BatchEmitter {
    boolean supports(MigrationProfile.BatchTarget target);
    BatchEmission emit(BatchJob job, MigrationProfile profile);
}
