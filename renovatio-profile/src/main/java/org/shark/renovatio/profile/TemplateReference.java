package org.shark.renovatio.profile;

/** Explicit immutable profile-template binding. */
public record TemplateReference(String name, String version) {
    public TemplateReference {
        ReusableAssetIdentifier.require(name, "name");
        ReusableAssetIdentifier.require(version, "version");
    }
}
