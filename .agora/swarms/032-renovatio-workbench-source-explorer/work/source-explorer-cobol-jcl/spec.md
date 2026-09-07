# Theia 9 · Source Explorer COBOL/JCL/Copybooks

The Workbench SHALL turn the selected project's legacy assets into an explicitly
read-only navigable source workspace. For every COBOL program, copybook and JCL
member inside the project's configured workspace boundary it SHALL expose:

- a **structural tree**: program → divisions → sections → paragraphs, plus
  copybook, JCL step/DD and dataset nodes;
- an **outline / symbol view** listing divisions, paragraphs, `PERFORM`, `CALL`,
  `EXEC SQL` and `EXEC CICS` symbols, each keeping its source file, line and
  column of origin;
- **search** over symbols, references and raw text, resolving each hit to its
  file and position;
- **file metadata**: content SHA-256 hash, workspace-relative path, detected
  encoding and analysis status (`parsed`, `partial`, `unsupported`);
- **IR / diagnostic linkage**: selecting a symbol references the semantic IR
  coordinate for that element and surfaces parse diagnostics in a Problems list.

The area SHALL NOT modify a source, run or re-run analysis, change any backend
analysis flow, or write project state. Unsupported or unparseable files SHALL
appear as an explicit safe node without breaking the tree. Loading, empty,
permission-denied and error states SHALL stay explicit. Dashboard and wizard
behavior remain outside this adapter, and deferred identity/login is out of scope.
