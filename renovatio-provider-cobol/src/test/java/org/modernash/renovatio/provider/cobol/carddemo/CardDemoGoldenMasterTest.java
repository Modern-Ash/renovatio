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
    void performThruAndStructuredGoToMatchCobol() throws Exception {
        assertStdoutMatchesCobol("perform-thru-goto", "GOTOS", "Gotos");
    }

    @Test
    void intrinsicFunctionsMatchCobol() throws Exception {
        assertStdoutMatchesCobol("intrinsic-functions", "FUNCS", "Funcs");
    }

    @Test
    void numericEditingMatchesCobol() throws Exception {
        assertStdoutMatchesCobol("numeric-edited", "EDITS", "Edits");
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

    /**
     * CardDemo's sign-on program COSGN00C, generated from the real sources and copybooks, run as a
     * screen handler over the in-memory USRSEC dataset: first entry, missing password, unknown
     * user, wrong password, admin and regular sign-on (XCTL with the user in the COMMAREA), PF3.
     */
    @Test
    void cardDemoSignOnValidatesUsersAgainstUsrsec() throws Exception {
        Path workspace = cardDemoWorkspace("COSGN00C");
        ClassLoader classes = compile(generate(workspace));

        String admin = String.format("%-8s%-20s%-20s%-8s%-1s%-23s", "ADMIN001", "Ada", "Admin", "PASSWORD", "A", "");
        String user = String.format("%-8s%-20s%-20s%-8s%-1s%-23s", "USER0001", "Uma", "User", "SECRET01", "U", "");
        Object users = classes.loadClass(PACKAGE + ".Cosgn00cUsrsecInMemoryAdapter").getConstructor(List.class)
                .newInstance(List.of(admin, user));
        Object service = wire(classes, "Cosgn00cServiceImpl", Map.of(PACKAGE + ".Cosgn00cUsrsecPort", users));
        Class<?> dto = classes.loadClass(PACKAGE + ".Cosgn00cDTO");
        Class<?> requestType = classes.loadClass(PACKAGE + ".Cosgn00cDTO$ScreenRequest");
        Object controller = classes.loadClass(PACKAGE + ".Cosgn00cCicsController")
                .getConstructor(classes.loadClass(PACKAGE + ".Cosgn00cService")).newInstance(service);
        var handle = controller.getClass().getMethod("handle", requestType);
        var newRequest = requestType.getConstructor(String.class, String.class, Map.class);

        // 1. First entry: the sign-on screen, resume transaction CC00.
        Object first = handle.invoke(controller, requestType.getMethod("firstEntry").invoke(null));
        assertThat(get(first, "map")).isEqualTo("COSGN0A");
        assertThat(get(first, "transid")).isEqualTo("CC00");
        String commarea = (String) get(first, "commarea");

        // The terminal's input map, built with the program's own symbolic map layout.
        java.util.function.BiFunction<String, String, String> typed = (userId, password) -> {
            try {
                Object screen = dto.getConstructor().newInstance();
                dto.getMethod("setUseridi", String.class).invoke(screen, userId);
                dto.getMethod("setPasswdi", String.class).invoke(screen, password);
                return (String) dto.getMethod("getCosgn0ai").invoke(screen);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        };
        java.util.function.Function<String, Object> enter = map -> {
            try {
                return handle.invoke(controller, newRequest.newInstance("ENTER", commarea, Map.of("COSGN0A", map)));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        };

        assertThat((String) get(enter.apply(typed.apply("admin001", "")), "screen"))
                .contains("Please enter Password ...");
        assertThat((String) get(enter.apply(typed.apply("nobody", "x")), "screen"))
                .contains("User not found. Try again ...");
        assertThat((String) get(enter.apply(typed.apply("user0001", "wrong")), "screen"))
                .contains("Wrong Password. Try again ...");

        // User ids and passwords are upper-cased before the lookup, as COSGN00C does.
        Object adminIn = enter.apply(typed.apply("admin001", "password"));
        assertThat(get(adminIn, "nextProgram")).isEqualTo("COADM01C");
        assertThat((String) get(adminIn, "commarea")).contains("CC00COSGN00C").contains("ADMIN001A");
        Object userIn = enter.apply(typed.apply("user0001", "secret01"));
        assertThat(get(userIn, "nextProgram")).isEqualTo("COMEN01C");
        assertThat((String) get(userIn, "commarea")).contains("USER0001U");

        // PF3: thank-you text and the conversation ends.
        Object exit = handle.invoke(controller, newRequest.newInstance("PF3", commarea, Map.of()));
        assertThat((String) get(exit, "text")).contains("Thank you");
        assertThat(get(exit, "ended")).isEqualTo(true);
    }

    /**
     * CardDemo's user list COUSR00C over 12 users in USRSEC: ten per page through STARTBR /
     * READNEXT, PF8 to the next page, PF7 back with READPREV, and a row selected for update.
     */
    @Test
    void cardDemoUserListPagesThroughUsrsec() throws Exception {
        Path workspace = cardDemoWorkspace("COUSR00C");
        ClassLoader classes = compile(generate(workspace));
        List<String> records = new ArrayList<>();
        for (int n = 1; n <= 12; n++) {
            records.add(String.format("%-8s%-20s%-20s%-8s%-1s%-23s", String.format("USER%04d", n),
                    "First" + n, "Last" + n, "PASSWORD", "U", ""));
        }
        Object users = classes.loadClass(PACKAGE + ".Cousr00cUsrsecInMemoryAdapter").getConstructor(List.class)
                .newInstance(records);
        Object service = wire(classes, "Cousr00cServiceImpl", Map.of(PACKAGE + ".Cousr00cUsrsecPort", users));
        Class<?> dto = classes.loadClass(PACKAGE + ".Cousr00cDTO");
        Class<?> requestType = classes.loadClass(PACKAGE + ".Cousr00cDTO$ScreenRequest");
        Object controller = classes.loadClass(PACKAGE + ".Cousr00cCicsController")
                .getConstructor(classes.loadClass(PACKAGE + ".Cousr00cService")).newInstance(service);
        var handle = controller.getClass().getMethod("handle", requestType);
        var newRequest = requestType.getConstructor(String.class, String.class, Map.class);
        // Coming from the admin menu: a COMMAREA that is not yet a re-entry.
        String fromMenu = (String) dto.getMethod("getCarddemoCommarea").invoke(dto.getConstructor().newInstance());

        Object page1 = handle.invoke(controller, newRequest.newInstance("ENTER", fromMenu, Map.of()));
        assertThat(get(page1, "map")).isEqualTo("COUSR0A");
        assertThat((String) get(page1, "screen")).contains("USER0001", "USER0010").doesNotContain("USER0011");

        Object page2 = handle.invoke(controller, newRequest.newInstance("PF8", (String) get(page1, "commarea"), Map.of()));
        assertThat((String) get(page2, "screen")).contains("USER0011", "USER0012").doesNotContain("USER0010");

        Object back = handle.invoke(controller, newRequest.newInstance("PF7", (String) get(page2, "commarea"), Map.of()));
        assertThat((String) get(back, "screen")).contains("USER0001", "USER0010").doesNotContain("USER0011");

        // 'U' next to the third row: transfer to the update program with that user selected.
        Object screen = dto.getConstructor().newInstance();
        dto.getMethod("setSel0003i", String.class).invoke(screen, "U");
        dto.getMethod("setUsrid03i", String.class).invoke(screen, "USER0003");
        String typed = (String) dto.getMethod("getCousr0ai").invoke(screen);
        Object update = handle.invoke(controller, newRequest.newInstance("ENTER", (String) get(back, "commarea"),
                Map.of("COUSR0A", typed)));
        assertThat(get(update, "nextProgram")).isEqualTo("COUSR02C");
        assertThat((String) get(update, "commarea")).contains("UUSER0003");
    }

    @Test
    void cardDemoUserMaintenanceAddsUpdatesAndDeletesInUsrsec() throws Exception {
        String bob = String.format("%-8s%-20s%-20s%-8s%-1s%-23s", "USER0002", "Bob", "Builder", "PASSWORD", "U", "");

        // COUSR01C: add a user; a missing field and a duplicate id are rejected.
        Online add = online("COUSR01C", List.of(bob));
        String commarea = add.firstScreen();
        Object missing = add.enter(commarea, "COUSR1A", Map.of("Fnamei", "Ann", "Useridi", "USER0001"));
        assertThat((String) get(missing, "screen")).contains("Last Name can NOT be empty...");
        Map<String, String> ann = Map.of("Fnamei", "Ann", "Lnamei", "Lee", "Useridi", "USER0001",
                "Passwdi", "SECRET01", "Usrtypei", "A");
        assertThat((String) get(add.enter(commarea, "COUSR1A", ann), "screen")).contains("USER0001 has been added ...");
        assertThat((String) get(add.enter(commarea, "COUSR1A", ann), "screen")).contains("User ID already exist...");
        assertThat(add.records()).anySatisfy(record -> assertThat(record).startsWith("USER0001Ann"));

        // COUSR02C: show a user, then PF5 saves the changed first name.
        Online update = online("COUSR02C", List.of(bob));
        commarea = update.firstScreen();
        Object shown = update.enter(commarea, "COUSR2A", Map.of("Usridini", "USER0002"));
        assertThat((String) get(shown, "screen")).contains("Bob").contains("Press PF5 key to save your updates ...");
        Object saved = update.send("PF5", commarea, "COUSR2A", Map.of("Usridini", "USER0002", "Fnamei", "Robert",
                "Lnamei", "Builder", "Passwdi", "PASSWORD", "Usrtypei", "U"));
        assertThat((String) get(saved, "screen")).contains("USER0002 has been updated ...");
        assertThat(update.records()).containsExactly(String.format("%-8s%-20s%-20s%-8s%-1s%-23s",
                "USER0002", "Robert", "Builder", "PASSWORD", "U", ""));

        // COUSR03C: show a user, then PF5 deletes it.
        Online delete = online("COUSR03C", List.of(bob));
        commarea = delete.firstScreen();
        assertThat((String) get(delete.enter(commarea, "COUSR3A", Map.of("Usridini", "USER0002")), "screen"))
                .contains("Press PF5 key to delete this user ...");
        Object deleted = delete.send("PF5", commarea, "COUSR3A", Map.of("Usridini", "USER0002"));
        assertThat((String) get(deleted, "screen")).contains("USER0002 has been deleted ...");
        assertThat(delete.records()).isEmpty();
    }

    /**
     * CardDemo's account view COACTVWC over three datasets: the card cross-reference through its
     * alternate index on the account id (CXACAIX), the account (ACCTDAT) and the customer (CUSTDAT).
     */
    @Test
    void cardDemoAccountViewReadsXrefAccountAndCustomer() throws Exception {
        ClassLoader classes = compile(generate(cardDemoWorkspace("COACTVWC")));
        Class<?> dto = classes.loadClass(PACKAGE + ".CoactvwcDTO");
        // Records built with the program's own record layouts (zoned signed amounts included).
        Object data = dto.getConstructor().newInstance();
        dto.getMethod("setXrefCardNum", String.class).invoke(data, "4111111111111111");
        dto.getMethod("setXrefCustId", Integer.class).invoke(data, 7);
        dto.getMethod("setXrefAcctId", Long.class).invoke(data, 42L);
        dto.getMethod("setAcctId", Long.class).invoke(data, 42L);
        dto.getMethod("setAcctActiveStatus", String.class).invoke(data, "Y");
        dto.getMethod("setAcctCurrBal", java.math.BigDecimal.class).invoke(data, new java.math.BigDecimal("-1234.50"));
        dto.getMethod("setAcctCreditLimit", java.math.BigDecimal.class).invoke(data, new java.math.BigDecimal("5000.00"));
        dto.getMethod("setCustId", Integer.class).invoke(data, 7);
        dto.getMethod("setCustFirstName", String.class).invoke(data, "Grace");
        dto.getMethod("setCustLastName", String.class).invoke(data, "Hopper");
        String xref = (String) dto.getMethod("getCardXrefRecord").invoke(data);
        String account = (String) dto.getMethod("getAccountRecord").invoke(data);
        String customer = (String) dto.getMethod("getCustomerRecord").invoke(data);

        // CXACAIX is an alternate index path: keyed by XREF-ACCT-ID at offset 25, not by the card number.
        Object xrefs = classes.loadClass(PACKAGE + ".CoactvwcCxacaixInMemoryAdapter")
                .getConstructor(List.class, int.class, int.class).newInstance(List.of(xref), 25, 11);
        Object accounts = classes.loadClass(PACKAGE + ".CoactvwcAcctdatInMemoryAdapter")
                .getConstructor(List.class).newInstance(List.of(account));
        Object customers = classes.loadClass(PACKAGE + ".CoactvwcCustdatInMemoryAdapter")
                .getConstructor(List.class).newInstance(List.of(customer));
        Object service = wire(classes, "CoactvwcServiceImpl", Map.of(
                PACKAGE + ".CoactvwcCxacaixPort", xrefs, PACKAGE + ".CoactvwcAcctdatPort", accounts,
                PACKAGE + ".CoactvwcCustdatPort", customers));
        Class<?> requestType = classes.loadClass(PACKAGE + ".CoactvwcDTO$ScreenRequest");
        Object controller = classes.loadClass(PACKAGE + ".CoactvwcCicsController")
                .getConstructor(classes.loadClass(PACKAGE + ".CoactvwcService")).newInstance(service);
        var handle = controller.getClass().getMethod("handle", requestType);
        var newRequest = requestType.getConstructor(String.class, String.class, Map.class);

        String fromMenu = (String) dto.getMethod("getCarddemoCommarea").invoke(dto.getConstructor().newInstance());
        Object prompt = handle.invoke(controller, newRequest.newInstance("ENTER", fromMenu, Map.of()));
        assertThat(get(prompt, "map")).isEqualTo("CACTVWA");
        assertThat((String) get(prompt, "screen")).contains("Enter or update id of account to display");

        Object screen = dto.getConstructor().newInstance();
        dto.getMethod("setAcctsidi", Long.class).invoke(screen, 42L);
        String typed = (String) dto.getMethod("getCactvwai").invoke(screen);
        Object shown = handle.invoke(controller, newRequest.newInstance("ENTER", (String) get(prompt, "commarea"),
                Map.of("CACTVWA", typed)));
        assertThat((String) get(shown, "screen"))
                .contains("Grace", "Hopper")
                .contains("-      1,234.50")      // ACURBALO PIC +ZZZ,ZZZ,ZZZ.99
                .contains("+      5,000.00");     // ACRDLIMO

        Object missing = dto.getConstructor().newInstance();
        dto.getMethod("setAcctsidi", Long.class).invoke(missing, 99L);
        Object notFound = handle.invoke(controller, newRequest.newInstance("ENTER", (String) get(prompt, "commarea"),
                Map.of("CACTVWA", (String) dto.getMethod("getCactvwai").invoke(missing))));
        assertThat((String) get(notFound, "screen")).contains("Account:00000000099 not found in Cross ref file");
    }

    /**
     * CardDemo's card view COCRDSLC over CARDDAT: a card typed on the screen, a card picked on the
     * card list (passed in the COMMAREA, from COCRDLIC), and a card that is not on file.
     */
    @Test
    void cardDemoCardViewReadsCarddat() throws Exception {
        ClassLoader classes = compile(generate(cardDemoWorkspace("COCRDSLC")));
        Class<?> dto = classes.loadClass(PACKAGE + ".CocrdslcDTO");
        Object data = dto.getConstructor().newInstance();
        dto.getMethod("setCardNum", String.class).invoke(data, "4111111111111111");
        dto.getMethod("setCardAcctId", Long.class).invoke(data, 42L);
        dto.getMethod("setCardEmbossedName", String.class).invoke(data, "GRACE HOPPER");
        dto.getMethod("setCardExpiraionDate", String.class).invoke(data, "2027-05-31");
        dto.getMethod("setCardActiveStatus", String.class).invoke(data, "Y");
        Object cards = classes.loadClass(PACKAGE + ".CocrdslcCarddatInMemoryAdapter")
                .getConstructor(List.class).newInstance(List.of((String) dto.getMethod("getCardRecord").invoke(data)));
        Object byAccount = classes.loadClass(PACKAGE + ".CocrdslcCardaixInMemoryAdapter")
                .getConstructor(List.class).newInstance(List.of());
        Object service = wire(classes, "CocrdslcServiceImpl", Map.of(
                PACKAGE + ".CocrdslcCarddatPort", cards, PACKAGE + ".CocrdslcCardaixPort", byAccount));
        Class<?> requestType = classes.loadClass(PACKAGE + ".CocrdslcDTO$ScreenRequest");
        Object controller = classes.loadClass(PACKAGE + ".CocrdslcCicsController")
                .getConstructor(classes.loadClass(PACKAGE + ".CocrdslcService")).newInstance(service);
        var handle = controller.getClass().getMethod("handle", requestType);
        var newRequest = requestType.getConstructor(String.class, String.class, Map.class);

        String fromMenu = (String) dto.getMethod("getCarddemoCommarea").invoke(dto.getConstructor().newInstance());
        Object prompt = handle.invoke(controller, newRequest.newInstance("ENTER", fromMenu, Map.of()));
        assertThat(get(prompt, "map")).isEqualTo("CCRDSLA");

        Object screen = dto.getConstructor().newInstance();
        dto.getMethod("setAcctsidi", String.class).invoke(screen, "00000000042");
        dto.getMethod("setCardsidi", String.class).invoke(screen, "4111111111111111");
        Object shown = handle.invoke(controller, newRequest.newInstance("ENTER", (String) get(prompt, "commarea"),
                Map.of("CCRDSLA", (String) dto.getMethod("getCcrdslai").invoke(screen))));
        assertThat((String) get(shown, "screen")).contains("GRACE HOPPER", "2027");

        Object fromList = dto.getConstructor().newInstance();
        dto.getMethod("setCdemoFromProgram", String.class).invoke(fromList, "COCRDLIC");
        dto.getMethod("setCdemoAcctId", Long.class).invoke(fromList, 42L);
        dto.getMethod("setCdemoCardNum", Long.class).invoke(fromList, 4111111111111111L);
        Object picked = handle.invoke(controller, newRequest.newInstance("ENTER",
                (String) dto.getMethod("getCarddemoCommarea").invoke(fromList), Map.of()));
        assertThat((String) get(picked, "screen")).contains("GRACE HOPPER");

        dto.getMethod("setCardsidi", String.class).invoke(screen, "4000000000000002");
        Object missing = handle.invoke(controller, newRequest.newInstance("ENTER", (String) get(prompt, "commarea"),
                Map.of("CCRDSLA", (String) dto.getMethod("getCcrdslai").invoke(screen))));
        assertThat((String) get(missing, "screen")).contains("Did not find cards for this search condition");
    }

    /** A CardDemo online program generated from the real sources, wired to an in-memory USRSEC. */
    private record Online(Class<?> dto, Object controller, java.lang.reflect.Method handle,
                          java.lang.reflect.Constructor<?> request, Object users) {

        /** First task from the admin menu (not a re-entry); returns the COMMAREA to resume with. */
        String firstScreen() throws Exception {
            String fromMenu = (String) dto.getMethod("getCarddemoCommarea").invoke(dto.getConstructor().newInstance());
            return (String) get(handle.invoke(controller, request.newInstance("ENTER", fromMenu, Map.of())), "commarea");
        }

        Object enter(String commarea, String map, Map<String, String> fields) throws Exception {
            return send("ENTER", commarea, map, fields);
        }

        /** One task: the key pressed, the COMMAREA, and the input map built from field values. */
        Object send(String aid, String commarea, String map, Map<String, String> fields) throws Exception {
            Object screen = dto.getConstructor().newInstance();
            for (Map.Entry<String, String> field : fields.entrySet()) {
                dto.getMethod("set" + field.getKey(), String.class).invoke(screen, field.getValue());
            }
            String data = (String) dto.getMethod("get" + map.charAt(0) + map.substring(1).toLowerCase(java.util.Locale.ROOT)
                    + "i").invoke(screen);
            return handle.invoke(controller, request.newInstance(aid, commarea, Map.of(map, data)));
        }

        @SuppressWarnings("unchecked")
        List<String> records() throws Exception {
            return (List<String>) users.getClass().getMethod("records").invoke(users);
        }
    }

    private Online online(String program, List<String> usrsec) throws Exception {
        ClassLoader classes = compile(generate(cardDemoWorkspace(program)));
        String base = program.charAt(0) + program.substring(1).toLowerCase(java.util.Locale.ROOT);
        Object users = classes.loadClass(PACKAGE + "." + base + "UsrsecInMemoryAdapter").getConstructor(List.class)
                .newInstance(usrsec);
        Object service = wire(classes, base + "ServiceImpl", Map.of(PACKAGE + "." + base + "UsrsecPort", users));
        Class<?> requestType = classes.loadClass(PACKAGE + "." + base + "DTO$ScreenRequest");
        Object controller = classes.loadClass(PACKAGE + "." + base + "CicsController")
                .getConstructor(classes.loadClass(PACKAGE + "." + base + "Service")).newInstance(service);
        return new Online(classes.loadClass(PACKAGE + "." + base + "DTO"), controller,
                controller.getClass().getMethod("handle", requestType),
                requestType.getConstructor(String.class, String.class, Map.class), users);
    }

    /** A workspace with one CardDemo program and every CardDemo copybook (data and BMS). */
    private Path cardDemoWorkspace(String program) throws Exception {
        Path workspace = tempDir.resolve(program.toLowerCase(java.util.Locale.ROOT));
        Files.createDirectories(workspace);
        Files.copy(CORPUS.resolve("cbl/" + program + ".cbl"), workspace.resolve(program + ".cbl"));
        for (String dir : List.of("cpy", "cpy-bms")) {
            try (Stream<Path> copybooks = Files.list(CORPUS.resolve(dir))) {
                for (Path copybook : copybooks.toList()) {
                    Files.copy(copybook, workspace.resolve(copybook.getFileName()));
                }
            }
        }
        return workspace;
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
