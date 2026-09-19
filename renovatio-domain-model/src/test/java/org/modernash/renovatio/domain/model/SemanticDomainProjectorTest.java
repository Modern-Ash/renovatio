package org.modernash.renovatio.domain.model;

import org.junit.jupiter.api.Test;
import org.modernash.renovatio.semantic.ir.SemanticProgram;
import org.modernash.renovatio.semantic.ir.SourceProvenance;
import org.modernash.renovatio.semantic.ir.SourceSpan;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticDomainProjectorTest {
    private static final String HASH = "0".repeat(64);
    private static final SourceSpan SPAN = new SourceSpan("src/program.cob", 1, 1, 2, 9);

    @Test
    void mergesAFileRepositoryWithTheRecordItReadsIntoOneNode() {
        // Reported bug: a plain COBOL file (FD/SELECT) and its 01-level
        // record used to show up as two boxes joined by a "maps to" line,
        // but they're the same physical resource described twice. A
        // FILE-kind repository with exactly one unambiguous name-matching
        // ENTITY should collapse into a single REPOSITORY node carrying
        // the record's fields, not two nodes plus a relation.
        SemanticProgram.SemanticType idField = elementaryType("CUSTOMER-ID");
        SemanticProgram.SemanticType recordType = new SemanticProgram.SemanticType(SemanticProgram.Header.create("TEST",
                SemanticProgram.NodeKind.TYPE, "group:CUSTOMER-RECORD", SPAN), "CUSTOMER-RECORD",
                SemanticProgram.TypeKind.GROUP, SemanticProgram.Signedness.UNKNOWN,
                OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(),
                List.of(idField.header().id()));
        SemanticProgram program = program(
                List.of(recordType, idField),
                List.of(io(SemanticProgram.IoKind.FILE, "READ", "CUSTOMER-FILE")));

        DomainModel model = new SemanticDomainProjector().project("p1", List.of(program));

        List<DomainModel.DomainNode> repositoryOrEntityNodes = model.nodes().stream()
                .filter(node -> node.kind() == DomainModel.Kind.REPOSITORY || node.kind() == DomainModel.Kind.ENTITY)
                .toList();
        assertEquals(1, repositoryOrEntityNodes.size(), "the file and its record should collapse into one node");
        DomainModel.DomainNode merged = repositoryOrEntityNodes.get(0);
        assertEquals(DomainModel.Kind.REPOSITORY, merged.kind());
        assertTrue(merged.properties().stream().anyMatch(property -> property.name().equals("CUSTOMER-ID")),
                "the merged node should carry the record's fields");
        assertFalse(model.relations().stream().anyMatch(relation -> relation.kind() == DomainModel.RelationKind.MAPS_TO),
                "a merged file+record has no separate node left to MAPS_TO");
    }

    @Test
    void mergesRepositoriesWithTheSameNameRootAcrossDifferentPrograms() {
        // Reported bug (#280 follow-up): "sigo viendo customer file y
        // customer record como 2 entidades" — confirmed via a live
        // analysis that CUSTOMER-FILE and CUSTOMER-RECORD were both
        // REPOSITORY boundary nodes, but from two *different* programs, so
        // the per-program merge above never even considered them together
        // (each program only ever sees its own boundary/type nodes). This
        // global pass has to run after every program is projected.
        SemanticProgram fileProgram = program("CBTRN01C",
                List.of(), List.of(io("CBTRN01C", SemanticProgram.IoKind.FILE, "READ", "CUSTOMER-FILE")));
        SemanticProgram recordProgram = program("CBIMPORT",
                List.of(), List.of(io("CBIMPORT", SemanticProgram.IoKind.FILE, "WRITE", "CUSTOMER-RECORD")));

        DomainModel model = new SemanticDomainProjector().project("p1", List.of(fileProgram, recordProgram));

        List<DomainModel.DomainNode> repositories = model.nodes().stream()
                .filter(node -> node.kind() == DomainModel.Kind.REPOSITORY).toList();
        assertEquals(1, repositories.size(), "same-core-name repositories from different programs should merge");
        // Both programs' use-cases should still USE the single merged
        // repository, just not each other or itself.
        String repositoryId = repositories.get(0).id();
        assertTrue(model.relations().stream().anyMatch(relation ->
                relation.kind() == DomainModel.RelationKind.USES
                        && relation.fromId().equals("use-case:CBTRN01C") && relation.toId().equals(repositoryId)));
        assertTrue(model.relations().stream().anyMatch(relation ->
                relation.kind() == DomainModel.RelationKind.USES
                        && relation.fromId().equals("use-case:CBIMPORT") && relation.toId().equals(repositoryId)));
    }

    @Test
    void mergesRepositoriesViaStructuralBindingWhenNamesDoNotShareARoot() {
        // Reported bug (second follow-up): "no corrijas solo ese caso...
        // fijate que ahora quedaron casos que no son customer... asociar
        // siempre el record al file". Real CardDemo pairs like
        // ACCTFILE-FILE / ACCOUNT-RECORD or DALYTRAN-FILE / TRAN-RECORD
        // never share a fuzzy name root (ACCTFILE vs ACCOUNT, DALYTRAN vs
        // TRAN) — only the COBOL FD's actual "RECORD IS" binding proves
        // they're the same resource. A FILE-kind IoOperation carrying a
        // structural boundRecordSymbol should merge across programs even
        // when the fuzzy name check alone would never have matched them.
        SemanticProgram fileProgram = program("CBACT01C", List.of(),
                List.of(ioWithBoundRecord("CBACT01C", "READ", "ACCTFILE-FILE", "ACCOUNT-RECORD")));
        SemanticProgram recordProgram = program("CBTRN02C",
                List.of(), List.of(io("CBTRN02C", SemanticProgram.IoKind.FILE, "WRITE", "ACCOUNT-RECORD")));

        DomainModel model = new SemanticDomainProjector().project("p1", List.of(fileProgram, recordProgram));

        List<DomainModel.DomainNode> repositories = model.nodes().stream()
                .filter(node -> node.kind() == DomainModel.Kind.REPOSITORY).toList();
        assertEquals(1, repositories.size(),
                "ACCTFILE-FILE and ACCOUNT-RECORD should merge via structural binding despite no shared name root");
    }

    @Test
    void infersARelationBetweenTwoDifferentRepositoriesViaAsharedRecordKeyField() {
        // "no puedo creer que renovatio no detecte relaciones... tiene que
        // inferir files o vsam a estructuras db relacionales modernas" —
        // everything up to now only ever linked a file to its *own*
        // record (the same physical resource, described twice). This is
        // the actually-missing piece: two *different* business entities
        // related to each other, the way a real reverse-engineered
        // relational schema would show it — a cross-reference file
        // (XREF-FILE) carries ACCOUNT-FILE's own declared key
        // (ACCT-ID) as one of its own fields, so XREF-FILE holds a
        // foreign key into ACCOUNT-FILE.
        SemanticProgram.SemanticType acctIdField = new SemanticProgram.SemanticType(SemanticProgram.Header.create("CBACT",
                SemanticProgram.NodeKind.TYPE, "field:ACCT-ID", SPAN), "ACCT-ID",
                SemanticProgram.TypeKind.TEXT, SemanticProgram.Signedness.UNKNOWN,
                OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), List.of());
        SemanticProgram.SemanticType accountRecord = new SemanticProgram.SemanticType(SemanticProgram.Header.create("CBACT",
                SemanticProgram.NodeKind.TYPE, "group:ACCOUNT-RECORD", SPAN), "ACCOUNT-RECORD",
                SemanticProgram.TypeKind.GROUP, SemanticProgram.Signedness.UNKNOWN,
                OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(),
                List.of(acctIdField.header().id()));
        SemanticProgram accountProgram = program("CBACT", List.of(accountRecord, acctIdField),
                List.of(ioWithKey("CBACT", "READ", "ACCOUNT-FILE", "ACCOUNT-RECORD", "ACCT-ID")));

        SemanticProgram.SemanticType xrefCardNum = new SemanticProgram.SemanticType(SemanticProgram.Header.create("CBXREF",
                SemanticProgram.NodeKind.TYPE, "field:CARD-NUM", SPAN), "CARD-NUM",
                SemanticProgram.TypeKind.TEXT, SemanticProgram.Signedness.UNKNOWN,
                OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), List.of());
        SemanticProgram.SemanticType xrefAcctId = new SemanticProgram.SemanticType(SemanticProgram.Header.create("CBXREF",
                SemanticProgram.NodeKind.TYPE, "field:ACCT-ID", SPAN), "ACCT-ID",
                SemanticProgram.TypeKind.TEXT, SemanticProgram.Signedness.UNKNOWN,
                OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), List.of());
        SemanticProgram.SemanticType xrefRecord = new SemanticProgram.SemanticType(SemanticProgram.Header.create("CBXREF",
                SemanticProgram.NodeKind.TYPE, "group:XREF-RECORD", SPAN), "XREF-RECORD",
                SemanticProgram.TypeKind.GROUP, SemanticProgram.Signedness.UNKNOWN,
                OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(),
                List.of(xrefCardNum.header().id(), xrefAcctId.header().id()));
        SemanticProgram xrefProgram = program("CBXREF", List.of(xrefRecord, xrefCardNum, xrefAcctId),
                List.of(ioWithBoundRecord("CBXREF", "READ", "XREF-FILE", "XREF-RECORD")));

        DomainModel model = new SemanticDomainProjector().project("p1", List.of(accountProgram, xrefProgram));

        DomainModel.DomainNode accountFile = model.nodes().stream()
                .filter(node -> node.kind() == DomainModel.Kind.REPOSITORY && node.name().equals("ACCOUNT-FILE"))
                .findFirst().orElseThrow();
        DomainModel.DomainNode xrefFile = model.nodes().stream()
                .filter(node -> node.kind() == DomainModel.Kind.REPOSITORY && node.name().equals("XREF-FILE"))
                .findFirst().orElseThrow();

        assertTrue(model.relations().stream().anyMatch(relation ->
                relation.kind() == DomainModel.RelationKind.ASSOCIATES_WITH
                        && relation.fromId().equals(xrefFile.id())
                        && relation.toId().equals(accountFile.id())),
                "expected XREF-FILE to reference ACCOUNT-FILE via the shared ACCT-ID key field");
    }

    @Test
    void infersARelationFromAMoveThatCarriesAKeyFieldAcrossEntitiesWithNoSharedFieldName() {
        // CardDemo's real join pattern: XREF-FILE's own key field
        // (FD-XREF-CARD-NUM) is unrelated in name to ACCTFILE-FILE's key
        // (FD-ACCT-ID), so the property-name-based inference above cannot
        // see the relation at all — but CBACT01C literally does
        // "MOVE XREF-ACCT-ID TO WS-ACCT-ID-KEY" before reading the account
        // file by that key. That MOVE is real procedure-division evidence
        // the program joins these two entities, independent of any name
        // coincidence — "podemos acceder al corte del control... por el
        // proceso que hace".
        SemanticProgram.SemanticType acctIdField = new SemanticProgram.SemanticType(SemanticProgram.Header.create("CBACT",
                SemanticProgram.NodeKind.TYPE, "field:WS-ACCT-ID-KEY", SPAN), "WS-ACCT-ID-KEY",
                SemanticProgram.TypeKind.TEXT, SemanticProgram.Signedness.UNKNOWN,
                OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), List.of());
        SemanticProgram.SemanticType accountRecord = new SemanticProgram.SemanticType(SemanticProgram.Header.create("CBACT",
                SemanticProgram.NodeKind.TYPE, "group:ACCOUNT-RECORD", SPAN), "ACCOUNT-RECORD",
                SemanticProgram.TypeKind.GROUP, SemanticProgram.Signedness.UNKNOWN,
                OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), List.of());
        SemanticProgram.SemanticType xrefAcctId = new SemanticProgram.SemanticType(SemanticProgram.Header.create("CBACT",
                SemanticProgram.NodeKind.TYPE, "field:XREF-ACCT-ID", SPAN), "XREF-ACCT-ID",
                SemanticProgram.TypeKind.TEXT, SemanticProgram.Signedness.UNKNOWN,
                OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), List.of());
        SemanticProgram.SemanticType xrefRecord = new SemanticProgram.SemanticType(SemanticProgram.Header.create("CBACT",
                SemanticProgram.NodeKind.TYPE, "group:XREF-RECORD", SPAN), "XREF-RECORD",
                SemanticProgram.TypeKind.GROUP, SemanticProgram.Signedness.UNKNOWN,
                OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(),
                List.of(xrefAcctId.header().id()));

        SemanticProgram.IoOperation accountIo = ioWithKey("CBACT", "READ", "ACCOUNT-FILE", "ACCOUNT-RECORD", "WS-ACCT-ID-KEY");
        SemanticProgram.IoOperation xrefIo = ioWithBoundRecord("CBACT", "READ", "XREF-FILE", "XREF-RECORD");
        SemanticProgram.FieldFlow moveAcctIdIntoKey = fieldFlow("CBACT", xrefAcctId.header().id(), acctIdField.header().id(), 0);

        SemanticProgram program = program("CBACT",
                List.of(accountRecord, acctIdField, xrefRecord, xrefAcctId),
                List.of(accountIo, xrefIo),
                List.of(moveAcctIdIntoKey));

        DomainModel model = new SemanticDomainProjector().project("p1", List.of(program));

        DomainModel.DomainNode accountFile = model.nodes().stream()
                .filter(node -> node.kind() == DomainModel.Kind.REPOSITORY && node.name().equals("ACCOUNT-FILE"))
                .findFirst().orElseThrow();
        DomainModel.DomainNode xrefFile = model.nodes().stream()
                .filter(node -> node.kind() == DomainModel.Kind.REPOSITORY && node.name().equals("XREF-FILE"))
                .findFirst().orElseThrow();

        assertTrue(model.relations().stream().anyMatch(relation ->
                relation.kind() == DomainModel.RelationKind.ASSOCIATES_WITH
                        && relation.fromId().equals(xrefFile.id())
                        && relation.toId().equals(accountFile.id())),
                "expected the MOVE of XREF-ACCT-ID into ACCOUNT-FILE's key field to be traced as XREF-FILE -> ACCOUNT-FILE");
    }

    @Test
    void doesNotInferAFalsePositiveRelationBetweenSameFileDeclaredUnderDifferentSelectNames() {
        // Confirmed via CardDemo: ACCT-FILE, ACCOUNT-FILE and ACCTFILE-FILE
        // are three different local SELECT names, declared in three
        // different programs, that all ASSIGN TO the exact same physical
        // dataset ACCTFILE — the same file, not three entities. Their core
        // names ("ACCT" / "ACCOUNT" / "ACCTFILE") are dissimilar enough to
        // evade the fuzzy/structural merge, yet they share the same
        // declared RECORD KEY (FD-ACCT-ID) — which previously tricked
        // inferForeignKeyRelations into emitting a bogus ASSOCIATES_WITH
        // relation between what are really duplicate descriptions of one
        // entity. The ASSIGN TO union pass in mergeCrossProgramRepositories
        // must unify them first, so no such relation is produced and they
        // collapse into a single repository.
        SemanticProgram acctProgram = program("CBSTM03B", List.of(),
                List.of(ioWithAssignTarget("CBSTM03B", "READ", "ACCT-FILE", "ACCT-RECORD", "FD-ACCT-ID", "ACCTFILE")));
        SemanticProgram accountProgram = program("CBACT04C", List.of(),
                List.of(ioWithAssignTarget("CBACT04C", "READ", "ACCOUNT-FILE", "ACCOUNT-RECORD", "FD-ACCT-ID", "ACCTFILE")));
        SemanticProgram acctfileProgram = program("CBACT01C", List.of(),
                List.of(ioWithAssignTarget("CBACT01C", "READ", "ACCTFILE-FILE", "ACCTFILE-RECORD", "FD-ACCT-ID", "ACCTFILE")));

        DomainModel model = new SemanticDomainProjector().project("p1",
                List.of(acctProgram, accountProgram, acctfileProgram));

        long repositoryCount = model.nodes().stream().filter(node -> node.kind() == DomainModel.Kind.REPOSITORY).count();
        assertEquals(1, repositoryCount,
                "ACCT-FILE/ACCOUNT-FILE/ACCTFILE-FILE all ASSIGN TO ACCTFILE and must collapse into one repository");
        assertFalse(model.relations().stream().anyMatch(relation -> relation.kind() == DomainModel.RelationKind.ASSOCIATES_WITH),
                "same physical file under different SELECT names must never produce a self-referential FK relation");
    }

    @Test
    void keepsADatabaseTableAndItsHostVariableRecordAsSeparateNodesJoinedByMapsTo() {
        // A DB2 table and a COBOL host-variable record are still genuinely
        // distinct — a table can be queried into several different
        // projections — so DATABASE-kind access is never merged, only
        // FILE-kind.
        SemanticProgram program = program(
                List.of(groupType("CUSTOMER-RECORD")),
                List.of(io(SemanticProgram.IoKind.DATABASE, "SELECT", "CUSTOMER-TABLE")));

        DomainModel model = new SemanticDomainProjector().project("p1", List.of(program));

        DomainModel.DomainNode repository = model.nodes().stream()
                .filter(node -> node.kind() == DomainModel.Kind.REPOSITORY).findFirst().orElseThrow();
        DomainModel.DomainNode record = model.nodes().stream()
                .filter(node -> node.kind() == DomainModel.Kind.ENTITY).findFirst().orElseThrow();

        assertTrue(model.relations().stream().anyMatch(relation ->
                relation.kind() == DomainModel.RelationKind.MAPS_TO
                        && relation.fromId().equals(repository.id())
                        && relation.toId().equals(record.id())),
                "expected a MAPS_TO relation from the table to the record it's queried into");
    }

    @Test
    void doesNotMapUnrelatedRepositoryAndRecordNames() {
        SemanticProgram program = program(
                List.of(groupType("TRANSACTION-RECORD")),
                List.of(io(SemanticProgram.IoKind.FILE, "READ", "CUSTOMER-FILE")));

        DomainModel model = new SemanticDomainProjector().project("p1", List.of(program));

        assertFalse(model.relations().stream().anyMatch(relation -> relation.kind() == DomainModel.RelationKind.MAPS_TO),
                "unrelated names should not produce a MAPS_TO relation");
    }

    @Test
    void populatesEntityPropertiesFromGroupMembers() {
        SemanticProgram.SemanticType nameField = elementaryType("CUSTOMER-NAME");
        SemanticProgram.SemanticType idField = elementaryType("CUSTOMER-ID");
        SemanticProgram.SemanticType recordType = new SemanticProgram.SemanticType(SemanticProgram.Header.create("TEST",
                SemanticProgram.NodeKind.TYPE, "group:CUSTOMER-RECORD", SPAN), "CUSTOMER-RECORD",
                SemanticProgram.TypeKind.GROUP, SemanticProgram.Signedness.UNKNOWN,
                OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(),
                List.of(idField.header().id(), nameField.header().id()));
        SemanticProgram program = program(List.of(recordType, idField, nameField), List.of());

        DomainModel model = new SemanticDomainProjector().project("p1", List.of(program));

        DomainModel.DomainNode entity = model.nodes().stream()
                .filter(node -> node.kind() == DomainModel.Kind.ENTITY).findFirst().orElseThrow();
        assertTrue(entity.properties().stream().anyMatch(property -> property.name().equals("CUSTOMER-ID")));
        assertTrue(entity.properties().stream().anyMatch(property -> property.name().equals("CUSTOMER-NAME")));
    }

    @Test
    void doesNotMapAOneWayInterchangeFileEvenWithAMatchingRecordName() {
        SemanticProgram program = program(
                List.of(groupType("CUSTOMER-RECORD")),
                List.of(io(SemanticProgram.IoKind.FILE, "WRITE", "CUSTOMER-OUTPUT")));

        DomainModel model = new SemanticDomainProjector().project("p1", List.of(program));

        boolean anyRepository = model.nodes().stream().anyMatch(node -> node.kind() == DomainModel.Kind.REPOSITORY);
        assertFalse(anyRepository, "a one-way interchange file must not become a REPOSITORY");
        assertFalse(model.relations().stream().anyMatch(relation -> relation.kind() == DomainModel.RelationKind.MAPS_TO));
    }

    private static SemanticProgram.SemanticType elementaryType(String symbol) {
        return new SemanticProgram.SemanticType(SemanticProgram.Header.create("TEST",
                SemanticProgram.NodeKind.TYPE, "field:" + symbol, SPAN), symbol,
                SemanticProgram.TypeKind.TEXT, SemanticProgram.Signedness.UNKNOWN,
                OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), List.of());
    }

    private static SemanticProgram.SemanticType groupType(String symbol) {
        return new SemanticProgram.SemanticType(SemanticProgram.Header.create("TEST",
                SemanticProgram.NodeKind.TYPE, "group:" + symbol, SPAN), symbol,
                SemanticProgram.TypeKind.GROUP, SemanticProgram.Signedness.UNKNOWN,
                OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), List.of());
    }

    private static SemanticProgram.IoOperation io(SemanticProgram.IoKind kind, String operation, String resource) {
        return io("TEST", kind, operation, resource);
    }

    private static SemanticProgram.IoOperation io(String programId, SemanticProgram.IoKind kind, String operation, String resource) {
        return new SemanticProgram.IoOperation(SemanticProgram.Header.create(programId,
                SemanticProgram.NodeKind.IO_OPERATION, "io:" + operation + ":" + resource, SPAN),
                kind, operation, Optional.of(resource), SemanticProgram.Direction.READ, List.of());
    }

    private static SemanticProgram.IoOperation ioWithBoundRecord(String programId, String operation, String resource, String boundRecord) {
        return new SemanticProgram.IoOperation(SemanticProgram.Header.create(programId,
                SemanticProgram.NodeKind.IO_OPERATION, "io:" + operation + ":" + resource, SPAN),
                SemanticProgram.IoKind.FILE, operation, Optional.of(resource), SemanticProgram.Direction.READ,
                List.of(), Optional.of(boundRecord), List.of(), Optional.empty());
    }

    private static SemanticProgram.IoOperation ioWithKey(String programId, String operation, String resource,
            String boundRecord, String keyField) {
        return new SemanticProgram.IoOperation(SemanticProgram.Header.create(programId,
                SemanticProgram.NodeKind.IO_OPERATION, "io:" + operation + ":" + resource, SPAN),
                SemanticProgram.IoKind.FILE, operation, Optional.of(resource), SemanticProgram.Direction.READ,
                List.of(), Optional.of(boundRecord), List.of(keyField), Optional.empty());
    }

    private static SemanticProgram.IoOperation ioWithAssignTarget(String programId, String operation, String resource,
            String boundRecord, String keyField, String assignTarget) {
        return new SemanticProgram.IoOperation(SemanticProgram.Header.create(programId,
                SemanticProgram.NodeKind.IO_OPERATION, "io:" + operation + ":" + resource, SPAN),
                SemanticProgram.IoKind.FILE, operation, Optional.of(resource), SemanticProgram.Direction.READ,
                List.of(), Optional.of(boundRecord), List.of(keyField), Optional.of(assignTarget));
    }

    private static SemanticProgram program(List<SemanticProgram.SemanticType> types,
                                            List<SemanticProgram.IoOperation> ioOperations) {
        return program("TEST", types, ioOperations);
    }

    private static SemanticProgram program(String programId, List<SemanticProgram.SemanticType> types,
                                            List<SemanticProgram.IoOperation> ioOperations) {
        return program(programId, types, ioOperations, List.of());
    }

    private static SemanticProgram program(String programId, List<SemanticProgram.SemanticType> types,
                                            List<SemanticProgram.IoOperation> ioOperations,
                                            List<SemanticProgram.FieldFlow> fieldFlows) {
        return new SemanticProgram("1", SemanticProgram.Header.create(programId, SemanticProgram.NodeKind.PROGRAM,
                "program", SPAN), programId, provenance(), types, List.of(), List.of(), ioOperations,
                new SemanticProgram.ControlFlow(Optional.empty(), List.of(), List.of()), List.of(), fieldFlows);
    }

    private static SemanticProgram.FieldFlow fieldFlow(String programId, String sourceTypeId, String targetTypeId, int ordinal) {
        return new SemanticProgram.FieldFlow(SemanticProgram.Header.create(programId,
                SemanticProgram.NodeKind.FIELD_FLOW, "field-flow:" + ordinal, SPAN), sourceTypeId, targetTypeId);
    }

    private static SourceProvenance provenance() {
        return new SourceProvenance("src/program.cob", HASH, "COBOL", Optional.of("IBM"), List.of());
    }
}
