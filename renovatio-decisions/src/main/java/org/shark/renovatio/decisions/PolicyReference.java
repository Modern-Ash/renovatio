package org.shark.renovatio.decisions;

import org.shark.renovatio.profile.ReusableAssetIdentifier;

/** Explicit immutable decision-policy catalog binding. */
public record PolicyReference(String name, String version) {
    public PolicyReference {
        ReusableAssetIdentifier.require(name, "name");
        ReusableAssetIdentifier.require(version, "version");
    }
}
