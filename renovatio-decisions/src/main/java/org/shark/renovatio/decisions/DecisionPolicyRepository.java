package org.shark.renovatio.decisions;

import java.util.List;
import java.util.Optional;

public interface DecisionPolicyRepository {
    DecisionPolicyCatalog save(DecisionPolicyCatalog catalog);
    Optional<DecisionPolicyCatalog> find(PolicyReference reference);
    List<DecisionPolicyCatalog> list();
}
