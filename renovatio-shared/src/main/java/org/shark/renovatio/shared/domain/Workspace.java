package org.shark.renovatio.shared.domain;

import lombok.Data;

import java.util.Map;

/**
 * Workspace context for operations
 */
@Data
public class Workspace {
    private String id;
    private String path;
    private String branch;
    private Map<String, Object> metadata;

    public Workspace() {
    }

    public Workspace(String id, String path, String branch) {
        this.id = id;
        this.path = path;
        this.branch = branch;
    }
}