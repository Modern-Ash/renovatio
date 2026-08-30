package org.shark.renovatio.llm.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.shark.renovatio.cobol.ir.annotated.CanonicalJson;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;

/** RFC 8785/SHA-256 cache-key derivation over the complete identity. */
public final class CacheKey {
    private static final ObjectMapper JSON = new ObjectMapper();

    private CacheKey() {
    }

    public static String derive(CacheIdentity identity) {
        Map<String, Object> projection = new LinkedHashMap<>();
        projection.put("identityType", CacheIdentity.IDENTITY_TYPE);
        projection.put("identityVersion", CacheIdentity.IDENTITY_VERSION);
        projection.put("canonicalInput", JSON.convertValue(identity.canonicalInput(), Object.class));
        projection.put("promptId", identity.promptId());
        projection.put("outputSchemaId", identity.outputSchemaId());
        projection.put("outputSchemaHash", identity.outputSchemaHash());
        projection.put("validators", identity.validators());
        projection.put("provider", identity.provider());
        projection.put("model", identity.model());
        projection.put("runtimeContractVersion", CacheIdentity.RUNTIME_CONTRACT_VERSION);
        return sha256(CanonicalJson.write(projection));
    }

    public static String sha256(String value) {
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256(byte[] value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value);
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
