package org.modernash.renovatio.cobol.ir.model;

/**
 * A file an online program reaches through CICS file control (a VSAM dataset CICS owns and
 * keeps open), as its commands describe it.
 *
 * @param name        dataset name (the DATASET / FILE literal, or the VALUE of the item naming it)
 * @param recordName  record area of its INTO / FROM options, {@code null} when not known
 * @param keyOffset   offset of the record key inside the record
 * @param keyLength   key length, 0 when no command gives a RIDFLD
 */
public record CobolCicsDataset(String name, String recordName, int keyOffset, int keyLength) {

    public boolean keyed() {
        return keyLength > 0;
    }
}
