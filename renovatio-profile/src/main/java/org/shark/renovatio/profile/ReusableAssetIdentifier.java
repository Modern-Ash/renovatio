package org.shark.renovatio.profile;

import java.util.regex.Pattern;

/** Shared validation for local reusable-asset names and immutable versions. */
public final class ReusableAssetIdentifier {
    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");

    private ReusableAssetIdentifier() { }

    public static String require(String value, String field) {
        if (value == null || !VALID.matcher(value).matches()) {
            throw new IllegalArgumentException(field + " must match " + VALID.pattern());
        }
        return value;
    }
}
