package org.modernash.renovatio.cobol.ir.model;

import java.util.List;
import java.util.Objects;

/**
 * A file as declared by its FILE-CONTROL SELECT entry and FD.
 *
 * @param name            SELECT name
 * @param assignTarget    ASSIGN TO name (DD / dataset), {@code null} when absent
 * @param organization    file organization
 * @param accessMode      access mode
 * @param recordKey       RECORD KEY data item, {@code null} unless indexed
 * @param alternateKeys   ALTERNATE RECORD KEY data items
 * @param fileStatus      FILE STATUS data item, {@code null} when absent
 * @param recordName      the 01 record under the file's FD, {@code null} when not found
 */
public record CobolFileDescriptor(String name, String assignTarget, Organization organization,
                                  AccessMode accessMode, String recordKey, List<String> alternateKeys,
                                  String fileStatus, String recordName) {

    public enum Organization { SEQUENTIAL, LINE_SEQUENTIAL, INDEXED, RELATIVE }

    public enum AccessMode { SEQUENTIAL, RANDOM, DYNAMIC }

    public CobolFileDescriptor {
        name = Objects.requireNonNull(name, "name");
        organization = organization == null ? Organization.SEQUENTIAL : organization;
        accessMode = accessMode == null ? AccessMode.SEQUENTIAL : accessMode;
        alternateKeys = List.copyOf(alternateKeys == null ? List.of() : alternateKeys);
    }

    public boolean keyed() {
        return organization == Organization.INDEXED || organization == Organization.RELATIVE;
    }
}
