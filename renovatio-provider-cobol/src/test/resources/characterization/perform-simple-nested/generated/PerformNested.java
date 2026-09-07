/** Generated target fixture for PERFORM-NESTED. The COBOL program has no observable output. */
public final class PerformNested {
    public static void main(String[] args) {
        mainParagraph();
    }
    private static void mainParagraph() { outerParagraph(); }
    private static void outerParagraph() { innerParagraph(); }
    private static void innerParagraph() { }
}
