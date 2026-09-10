package org.shark.renovatio.cobol.runtime;

import java.nio.charset.Charset;
import java.util.Comparator;

/**
 * Orders strings by the EBCDIC (CP037) collating sequence, as COBOL {@code IF}
 * and {@code SORT} comparisons do on a mainframe: space &lt; lowercase &lt;
 * uppercase &lt; digits.
 */
public final class EbcdicCollator implements Comparator<String> {

    public static final EbcdicCollator INSTANCE = new EbcdicCollator();

    private static final Charset CP037 = Charset.forName("Cp037");

    @Override
    public int compare(String left, String right) {
        byte[] l = left.getBytes(CP037);
        byte[] r = right.getBytes(CP037);
        int min = Math.min(l.length, r.length);
        for (int i = 0; i < min; i++) {
            int cmp = Integer.compare(l[i] & 0xFF, r[i] & 0xFF);
            if (cmp != 0) {
                return cmp;
            }
        }
        return Integer.compare(l.length, r.length);
    }
}
