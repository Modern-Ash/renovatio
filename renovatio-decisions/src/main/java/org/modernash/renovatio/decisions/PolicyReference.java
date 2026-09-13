package org.modernash.renovatio.decisions;

import org.modernash.renovatio.profile.ReusableAssetIdentifier;

/** Explicit immutable decision-policy catalog binding. */
public record PolicyReference(String name, String version) {
    public PolicyReference {
        ReusableAssetIdentifier.require(name, "name");
        ReusableAssetIdentifier.require(version, "version");
    }
}
