CREATE TABLE IF NOT EXISTS projects (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    workspace_path VARCHAR(1024) NOT NULL,
    branch VARCHAR(255),
    java_output_path VARCHAR(1024),
    java_package VARCHAR(255),
    java_architecture VARCHAR(255),
    profile_template_name VARCHAR(255),
    profile_template_version VARCHAR(255),
    policy_catalog_name VARCHAR(255),
    policy_catalog_version VARCHAR(255),
    workbench_active_area VARCHAR(255),
    workbench_selected_asset_id VARCHAR(512),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS jobs (
    id VARCHAR(255) PRIMARY KEY,
    project_id VARCHAR(255) NOT NULL,
    operation VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    progress DOUBLE,
    params_json TEXT,
    result_json TEXT,
    error TEXT,
    created_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS plan_snapshots (
    id VARCHAR(255) PRIMARY KEY,
    project_id VARCHAR(255) NOT NULL,
    plan_id VARCHAR(255) NOT NULL,
    plan_content_json TEXT,
    steps_json TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS run_snapshots (
    id VARCHAR(255) PRIMARY KEY,
    project_id VARCHAR(255) NOT NULL,
    run_id VARCHAR(255) NOT NULL,
    plan_id VARCHAR(255),
    dry_run BOOLEAN DEFAULT 0,
    diff_json TEXT,
    result_json TEXT,
    started_at TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS action_items (
    id VARCHAR(255) PRIMARY KEY,
    project_id VARCHAR(255) NOT NULL,
    run_id VARCHAR(255),
    severity VARCHAR(255) NOT NULL,
    reason TEXT,
    required_human_action TEXT,
    acceptance_condition TEXT,
    review_status VARCHAR(255) DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL,
    reviewed_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS project_profiles (
    project_id VARCHAR(255) PRIMARY KEY,
    schema_version VARCHAR(16) NOT NULL,
    overlay_json TEXT NOT NULL,
    profile_revision BIGINT NOT NULL,
    lock_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS project_decisions (
    project_id VARCHAR(255) NOT NULL,
    decision_id VARCHAR(64) NOT NULL,
    category VARCHAR(32) NOT NULL,
    decision_key VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    confidence NUMERIC(6, 5) NOT NULL,
    active BOOLEAN NOT NULL,
    decision_json TEXT NOT NULL,
    lock_version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (project_id, decision_id)
);

CREATE INDEX IF NOT EXISTS idx_decision_category
    ON project_decisions (project_id, active, category);
CREATE INDEX IF NOT EXISTS idx_decision_status
    ON project_decisions (project_id, active, status);
CREATE INDEX IF NOT EXISTS idx_decision_confidence
    ON project_decisions (project_id, active, confidence);

CREATE TABLE IF NOT EXISTS project_domain_model_versions (
    id VARCHAR(512) PRIMARY KEY,
    project_id VARCHAR(255) NOT NULL,
    revision BIGINT NOT NULL,
    canonical_hash VARCHAR(64) NOT NULL,
    model_json TEXT NOT NULL,
    saved_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_domain_model_project_revision UNIQUE (project_id, revision)
);

CREATE TABLE IF NOT EXISTS domain_suggestion_decisions (
    id VARCHAR(768) PRIMARY KEY,
    project_id VARCHAR(255) NOT NULL,
    suggestion_id VARCHAR(512) NOT NULL,
    action VARCHAR(16) NOT NULL,
    model_revision BIGINT NOT NULL,
    decided_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS project_architecture_profile_versions (
    id VARCHAR(512) PRIMARY KEY,
    project_id VARCHAR(255) NOT NULL,
    revision BIGINT NOT NULL,
    canonical_hash VARCHAR(64) NOT NULL,
    profile_json TEXT NOT NULL,
    saved_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_architecture_profile_project_revision UNIQUE (project_id, revision)
);
