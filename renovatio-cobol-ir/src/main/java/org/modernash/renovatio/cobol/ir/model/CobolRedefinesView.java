package org.modernash.renovatio.cobol.ir.model;

import org.modernash.renovatio.cobol.runtime.PicType;

/**
 * An item that shares storage with another through REDEFINES (the redefining item itself or an
 * elementary item under it): it has no storage of its own and reads/writes {@code length} bytes at
 * {@code offset} of the base item's encoded form.
 *
 * @param name        COBOL name of the view item
 * @param base        COBOL name of the redefined (storage-owning) item, {@code null} when the view
 *                    cannot be modeled (base not encodable, longer than the base, nested in a table
 *                    or redefining another view)
 * @param baseIsGroup whether the base is a group (its storage is its composed record)
 * @param offset      byte offset of the view inside the base storage
 * @param length      bytes the view covers
 * @param picType     picture of an elementary view, {@code null} for a group view (read as text)
 */
public record CobolRedefinesView(String name, String base, boolean baseIsGroup, int offset, int length,
                                 PicType picType) {

    public boolean supported() {
        return base != null;
    }
}
