package org.shark.renovatio.cobol.ir.context;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class CobolExecutionContext {

    private final Map<String, String> variableScopes;
    private final Map<String, Object> attributes;

    private CobolExecutionContext(Map<String, String> variableScopes, Map<String, Object> attributes) {
        this.variableScopes = Collections.unmodifiableMap(variableScopes);
        this.attributes = Collections.unmodifiableMap(attributes);
    }

    public Optional<String> resolveScope(String variable) {
        if (variable == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(variableScopes.get(variable.toUpperCase()));
    }

    public Optional<Object> attribute(String key) {
        return Optional.ofNullable(attributes.get(key));
    }

    public Map<String, String> getVariableScopes() {
        return variableScopes;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public static CobolExecutionContext empty() {
        return new CobolExecutionContext(Map.of(), Map.of());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<String, String> scopes = new LinkedHashMap<>();
        private final Map<String, Object> attributes = new LinkedHashMap<>();

        public Builder registerVariable(String name, String scope) {
            if (name != null && scope != null) {
                scopes.put(name.toUpperCase(), scope.toLowerCase());
            }
            return this;
        }

        public Builder registerVariables(Set<String> names, String scope) {
            if (names != null) {
                names.forEach(name -> registerVariable(name, scope));
            }
            return this;
        }

        public Builder attribute(String key, Object value) {
            if (key != null && value != null) {
                attributes.put(key, value);
            }
            return this;
        }

        public CobolExecutionContext build() {
            return new CobolExecutionContext(new LinkedHashMap<>(scopes), new LinkedHashMap<>(attributes));
        }
    }
}
