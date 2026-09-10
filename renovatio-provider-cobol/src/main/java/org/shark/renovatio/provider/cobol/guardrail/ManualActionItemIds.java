package org.shark.renovatio.provider.cobol.guardrail;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Produces stable identifiers from the bounded semantic identity of an action item. */
public final class ManualActionItemIds {

    private ManualActionItemIds() {
    }

    public static String from(String sourceFile, String program, String sourceSpan,
                              String constructionFamily, String reason) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", exception);
        }
        update(digest, sourceFile);
        update(digest, program);
        update(digest, sourceSpan);
        update(digest, constructionFamily);
        update(digest, reason);
        return "mai-" + HexFormat.of().formatHex(digest.digest()).substring(0, 24);
    }

    private static void update(MessageDigest digest, String value) {
        byte[] bytes = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }
}
