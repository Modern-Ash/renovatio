# Correction: four-commit cache promotion lifecycle

The authoritative promotion sequence is A/B/C/D. Commit A contains the promoted envelope, Commit B
the technical index, Commit C the owner approval and evidence, and Commit D only the generated
verified manifest. This supersedes earlier report language that asked Commit C to contain the
manifest. Actual Commit D is `d547e8face947e6b5dfa5d6409366c3f54ca5f74`.

Runtime/build verification discovers the Git commit that introduced the manifest, requires its
changed-path set to contain only the manifest, requires Commit C to be its ancestor and Commit D to
be an ancestor of `HEAD`, and compares the manifest at D with the loaded authority.
