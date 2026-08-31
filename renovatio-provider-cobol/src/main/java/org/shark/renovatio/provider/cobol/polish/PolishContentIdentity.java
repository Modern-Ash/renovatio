package org.shark.renovatio.provider.cobol.polish;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;

final class PolishContentIdentity {

    private final ObjectMapper objectMapper;

    PolishContentIdentity(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    }

    String requestId(PolishProposalRequest request) {
        return id(request);
    }

    String proposalId(PolishProposalRequest request, PolishCandidate candidate) {
        return id(java.util.List.of(request, candidate));
    }

    String sourcesHash(PolishProposalRequest request) {
        return digest(request.generatedSources());
    }

    private String id(Object value) {
        return "polish-" + digest(value).substring(0, 24);
    }

    private String digest(Object value) {
        try {
            return PolishContracts.sha256(objectMapper.writeValueAsString(value));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Cannot canonicalize polish identity", exception);
        }
    }
}
