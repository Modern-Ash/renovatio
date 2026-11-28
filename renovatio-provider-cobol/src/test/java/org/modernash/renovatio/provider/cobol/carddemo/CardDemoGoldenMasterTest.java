package org.modernash.renovatio.provider.cobol.carddemo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.modernash.renovatio.provider.cobol.service.CobolParsingService;
import org.modernash.renovatio.provider.cobol.service.JavaGenerationService;
import org.modernash.renovatio.provider.cobol.service.TemplateCodeGenerationService;
import org.modernash.renovatio.provider.cobol.translation.CobolIntermediateModelService;
import org.modernash.renovatio.provider.cobol.translation.CobolSemanticTranspiler;
import org.modernash.renovatio.shared.domain.StubResult;
import org.modernash.renovatio.shared.domain.Workspace;
import org.modernash.renovatio.shared.nql.NqlQuery;

import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioral equivalence of migrated CardDemo programs: the Java generated from the COBOL source
 * is compiled and run on the same input records as the original program, and its standard
 * output must match the reference output of the original COBOL program byte for byte.
 */
class CardDemoGoldenMasterTest {

    private static final Path CORPUS = Path.of("src/test/resources/corpus/carddemo/app");
    private static final Path GOLDEN = Path.of("src/test/resources/golden-master/carddemo");
    private static final String PACKAGE = "org.modernash.renovatio.generated.cobol";

    @TempDir
    Path tempDir;

    @Test
    void cbact02cPrintsTheSameCardFileListingAsTheCobolProgram() throws Exception {
        Path workspace = tempDir.resolve("workspace");
        Files.createDirectories(workspace);
        Files.copy(CORPUS.resolve("cbl/CBACT02C.cbl"), workspace.resolve("CBACT02C.cbl"));
        try (Stream<Path> copybooks = Files.list(CORPUS.resolve("cpy"))) {
            for (Path copybook : copybooks.toList()) {
                Files.copy(copybook, workspace.resolve(copybook.getFileName()));
            }
        }

        Map<String, String> generated = generate(workspace);
        ClassLoader classes = compile(generated);
        List<String> records = Files.readAllLines(GOLDEN.resolve("CBACT02C/carddata.txt"), StandardCharsets.UTF_8);

        Object cardFile = classes.loadClass(PACKAGE + ".Cbact02cCardfileFileInMemoryAdapter")
                .getConstructor(List.class).newInstance(records);
        Object service = wire(classes, "Cbact02cServiceImpl", Map.of(PACKAGE + ".Cbact02cCardfileFilePort", cardFile));
        Class<?> dtoType = classes.loadClass(PACKAGE + ".Cbact02cDTO");
        Object input = dtoType.getConstructor().newInstance();

        byte[] stdout = captureStdout(() -> service.getClass().getMethod("process", dtoType).invoke(service, input));

        assertThat(new String(stdout, StandardCharsets.UTF_8))
                .isEqualTo(Files.readString(GOLDEN.resolve("CBACT02C/expected-stdout.txt"), StandardCharsets.UTF_8));
    }

    @Test
    void binaryPackedAndZonedRecordsMatchGnuCobolBytes() throws Exception {
        Path workspace = tempDir.resolve("encoding");
        Files.createDirectories(workspace);
        Path golden = Path.of("src/test/resources/golden-master/record-encoding");
        Files.copy(golden.resolve("BINREC.cbl"), workspace.resolve("BINREC.cbl"));

        ClassLoader classes = compile(generate(workspace));
        Object outFile = classes.loadClass(PACKAGE + ".BinrecOutFileInMemoryAdapter").getConstructor().newInstance();
        Object service = wire(classes, "BinrecServiceImpl", Map.of(PACKAGE + ".BinrecOutFilePort", outFile));
        Class<?> dtoType = classes.loadClass(PACKAGE + ".BinrecDTO");
        service.getClass().getMethod("process", dtoType).invoke(service, dtoType.getConstructor().newInstance());

        @SuppressWarnings("unchecked")
        List<String> written = (List<String>) outFile.getClass().getMethod("records").invoke(outFile);
        // Records carry one byte per char (ISO-8859-1), so binary fields round-trip exactly.
        byte[] actual = String.join("", written).getBytes(StandardCharsets.ISO_8859_1);
        assertThat(actual).isEqualTo(Files.readAllBytes(golden.resolve("expected-records.bin")));
    }

    @Test
    void redefinesViewsShareStorageWithTheirBaseAsInCobol() throws Exception {
        assertStdoutMatchesCobol("redefines", "REDEF", "Redef");
    }

    @Test
    void referenceModificationAndAlphanumericMoveMatchCobol() throws Exception {
        assertStdoutMatchesCobol("reference-modification", "REFMOD", "Refmod");
    }

    @Test
    void qualifiedNamesAndEvaluateTrueMatchCobol() throws Exception {
        assertStdoutMatchesCobol("qualified-evaluate", "QUALEVAL", "Qualeval");
    }

    @Test
    void stringAndUnstringMatchCobol() throws Exception {
        assertStdoutMatchesCobol("string-unstring", "STRUNS", "Struns");
    }

    @Test
    void onlineProgramRunsAsAScreenHandlerAcrossAPseudoConversation() throws Exception {
        Path workspace = tempDir.resolve("screen-handler");
        Files.createDirectories(workspace);
        Files.copy(Path.of("src/test/resources/golden-master/screen-handler/SIGNON.cbl"), workspace.resolve("SIGNON.cbl"));
        Map<String, String> generated = generate(workspace);
        assertThat(generated.keySet()).anyMatch(path -> path.endsWith("SignonCicsController.java"));
        ClassLoader classes = compile(generated);

        Class<?> dto = classes.loadClass(PACKAGE + ".SignonDTO");
        Class<?> requestType = classes.loadClass(PACKAGE + ".SignonDTO$ScreenRequest");
        Object service = classes.loadClass(PACKAGE + ".SignonServiceImpl").getConstructor().newInstance();
        Object controller = classes.loadClass(PACKAGE + ".SignonCicsController")
                .getConstructor(classes.loadClass(PACKAGE + ".SignonService")).newInstance(service);
        var handle = controller.getClass().getMethod("handle", requestType);
        var newRequest = requestType.getConstructor(String.class, String.class, Map.class);

        // 1. First entry (no COMMAREA): the sign-on screen, resume SGN0 with an empty user.
        Object first = handle.invoke(controller, requestType.getMethod("firstEntry").invoke(null));
        assertThat(get(first, "map")).isEqualTo("SGNMAP");
        assertThat(get(first, "transid")).isEqualTo("SGN0");
        String commarea = (String) get(first, "commarea");
        assertThat(commarea).isEqualTo("        00");

        // 2. ENTER with a blank user id: same screen with an error, visit counted in the COMMAREA.
        Object blank = handle.invoke(controller, newRequest.newInstance("ENTER", commarea, Map.of("SGNMAP", " ".repeat(38))));
        assertThat((String) get(blank, "screen")).endsWith("Please enter User ID          ");
        assertThat(get(blank, "commarea")).isEqualTo("        01");
        assertThat(get(blank, "nextProgram")).isNull();

        // 3. ENTER with a user id: XCTL to MENU with the user in the COMMAREA, no resume transaction.
        Object signed = handle.invoke(controller, newRequest.newInstance("ENTER", (String) get(blank, "commarea"),
                Map.of("SGNMAP", "ALICE   " + " ".repeat(30))));
        assertThat(get(signed, "nextProgram")).isEqualTo("MENU");
        assertThat(get(signed, "commarea")).isEqualTo("ALICE   02");
        assertThat(get(signed, "transid")).isNull();

        // 4. PF3: plain text and the conversation ends.
        Object exit = handle.invoke(controller, newRequest.newInstance("PF3", commarea, Map.of()));
        assertThat(get(exit, "text")).isNotNull();
        assertThat(get(exit, "ended")).isEqualTo(true);
        assertThat(get(exit, "transid")).isNull();
        assertThat(dto).isNotNull();
    }

    @Test
    void cicsFileControlRunsOnTheDatasetPort() throws Exception {
        Path workspace = tempDir.resolve("cics-files");
        Files.createDirectories(workspace);
        Files.copy(Path.of("src/test/resources/golden-master/screen-handler/USERMNT.cbl"), workspace.resolve("USERMNT.cbl"));
        ClassLoader classes = compile(generate(workspace));

        Object users = classes.loadClass(PACKAGE + ".UsermntUsrsecInMemoryAdapter").getConstructor(List.class)
                .newInstance(List.of("ALICE   Alice A ", "BOB     Bob B   ", "CAROL   Carol C "));
        Object service = wire(classes, "UsermntServiceImpl", Map.of(PACKAGE + ".UsermntUsrsecPort", users));
        Class<?> requestType = classes.loadClass(PACKAGE + ".UsermntDTO$ScreenRequest");
        Object controller = classes.loadClass(PACKAGE + ".UsermntCicsController")
                .getConstructor(classes.loadClass(PACKAGE + ".UsermntService")).newInstance(service);
        var handle = controller.getClass().getMethod("handle", requestType);
        var newRequest = requestType.getConstructor(String.class, String.class, Map.class);
        java.util.function.Function<String, Object> run = commarea -> {
            try {
                return get(handle.invoke(controller, newRequest.newInstance("ENTER", commarea, Map.of())), "text");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        };

        assertThat(run.apply("RBOB             ")).isEqualTo("00|BOB     Bob B   ");
        assertThat(run.apply("RZED             ")).isEqualTo("13|                ");   // NOTFND
        assertThat(run.apply("WDAVE    Dave D  ")).isEqualTo("00|                ");
        assertThat(run.apply("WDAVE    Dave D  ")).isEqualTo("14|                ");   // DUPREC
        assertThat(run.apply("UBOB     Bobby   ")).isEqualTo("00|                ");
        assertThat(run.apply("DALICE           ")).isEqualTo("00|                ");
        assertThat(run.apply("RALICE           ")).isEqualTo("13|                ");
        // Browse forward from "B": BOB, then CAROL; backward from CAROL: CAROL, then BOB.
        assertThat(run.apply("NB               ")).isEqualTo("00|CAROL   Carol C ");
        assertThat(run.apply("PCAROL           ")).isEqualTo("00|BOB     Bobby   ");
        assertThat(run.apply("NZZZ             ")).isEqualTo("13|                ");   // STARTBR past the end
        // ASKTIME + FORMATTIME: today's date with '-' and the time without separators.
        assertThat((String) run.apply("T                ")).matches(
                "00\\|" + java.time.LocalDate.now() + "\\d{6}");
        assertThatThrownBy(() -> run.apply("A                ")).rootCause().hasMessage("CICS ABEND 9999");
        @SuppressWarnings("unchecked")
        List<String> content = (List<String>) users.getClass().getMethod("records").invoke(users);
        assertThat(content).containsExactly("BOB     Bobby   ", "CAROL   Carol C ", "DAVE    Dave D  ");
    }

    private static Object get(Object target, String accessor) throws Exception {
        return target.getClass().getMethod(accessor).invoke(target);
    }

    /** Runs a self-contained program (no files) and compares its stdout with the GnuCOBOL reference. */
    private void assertStdoutMatchesCobol(String goldenDir, String program, String classBase) throws Exception {
        Path workspace = tempDir.resolve(goldenDir);
        Files.createDirectories(workspace);
        Path golden = Path.of("src/test/resources/golden-master").resolve(goldenDir);
        Files.copy(golden.resolve(program + ".cbl"), workspace.resolve(program + ".cbl"));

        ClassLoader classes = compile(generate(workspace));
        Object service = classes.loadClass(PACKAGE + "." + classBase + "ServiceImpl").getConstructor().newInstance();
        Class<?> dtoType = classes.loadClass(PACKAGE + "." + classBase + "DTO");
        Object input = dtoType.getConstructor().newInstance();

        byte[] stdout = captureStdout(() -> service.getClass().getMethod("process", dtoType).invoke(service, input));

        assertThat(new String(stdout, StandardCharsets.UTF_8))
                .isEqualTo(Files.readString(golden.resolve("expected-stdout.txt"), StandardCharsets.UTF_8));
    }

    /**
     * Builds the service through its injecting constructor: the given adapters for the ports
     * under test, the generated default adapter ({@code XxxPort} -> {@code XxxAdapter} /
     * {@code XxxInMemoryAdapter}) for every other port.
     */
    private static Object wire(ClassLoader classes, String serviceName, Map<String, Object> adapters) throws Exception {
        Class<?> serviceType = classes.loadClass(PACKAGE + "." + serviceName);
        var constructor = java.util.Arrays.stream(serviceType.getConstructors())
                .max(java.util.Comparator.comparingInt(java.lang.reflect.Constructor::getParameterCount))
                .orElseThrow();
        Object[] arguments = new Object[constructor.getParameterCount()];
        for (int i = 0; i < arguments.length; i++) {
            String port = constructor.getParameterTypes()[i].getName();
            Object adapter = adapters.get(port);
            if (adapter == null) {
                String stem = port.substring(0, port.length() - "Port".length());
                String adapterName = stem.endsWith("Program") ? stem + "Adapter" : stem + "InMemoryAdapter";
                adapter = classes.loadClass(adapterName).getConstructor().newInstance();
            }
            arguments[i] = adapter;
        }
        return constructor.newInstance(arguments);
    }

    private Map<String, String> generate(Path workspace) {
        CobolParsingService parsing = new CobolParsingService(CobolParsingService.Dialect.IBM);
        JavaGenerationService generator = new JavaGenerationService(parsing, new TemplateCodeGenerationService(),
                new CobolIntermediateModelService(), new CobolSemanticTranspiler(),
                new ObjectMapper().findAndRegisterModules(), true);
        StubResult result = generator.generateInterfaceStubs(new NqlQuery(),
                new Workspace("golden-master", workspace.toString(), "main"));
        assertThat(result.isSuccess()).as(result.getMessage()).isTrue();
        return result.getGeneratedCode();
    }

    private ClassLoader compile(Map<String, String> sources) throws Exception {
        Path sourceDir = tempDir.resolve("src");
        Path classDir = tempDir.resolve("classes");
        Files.createDirectories(sourceDir);
        Files.createDirectories(classDir);
        List<String> arguments = new ArrayList<>(List.of("-proc:none", "-classpath",
                System.getProperty("java.class.path"), "-d", classDir.toString()));
        for (Map.Entry<String, String> source : sources.entrySet()) {
            Path file = sourceDir.resolve(Path.of(source.getKey()).getFileName());
            Files.writeString(file, source.getValue(), StandardCharsets.UTF_8);
            arguments.add(file.toString());
        }
        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
        int status = ToolProvider.getSystemJavaCompiler()
                .run(null, null, diagnostics, arguments.toArray(String[]::new));
        assertThat(status).as(diagnostics.toString(StandardCharsets.UTF_8)).isZero();
        return new URLClassLoader(new URL[] {classDir.toUri().toURL()}, getClass().getClassLoader());
    }

    private interface Action {
        void run() throws Exception;
    }

    private static byte[] captureStdout(Action action) throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return captured.toByteArray();
    }
}
