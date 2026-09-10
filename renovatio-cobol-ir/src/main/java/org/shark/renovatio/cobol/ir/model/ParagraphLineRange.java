package org.shark.renovatio.cobol.ir.model;

/**
 * Lexical span of a COBOL paragraph within its source file.
 *
 * <p>Line numbers are one-based and absolute within the COBOL source file
 * (not the PROCEDURE DIVISION section). Both coordinates are inclusive.
 */
public record ParagraphLineRange(int startLine, int endLine) {

    public ParagraphLineRange {
        if (startLine < 1 || endLine < startLine) {
            throw new IllegalArgumentException("line range must be one-based and non-decreasing");
        }
    }
}