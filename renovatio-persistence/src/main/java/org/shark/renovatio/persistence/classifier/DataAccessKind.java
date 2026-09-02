package org.shark.renovatio.persistence.classifier;

/**
 * Taxonomy of data access kinds detected by the classifier.
 */
public enum DataAccessKind {
    VSAM_KEY,
    VSAM_SEQUENTIAL,
    SEQUENTIAL_FD,
    EXEC_SQL,
    FLAT_FILE_REDEFINES,
    RESIDUAL
}
