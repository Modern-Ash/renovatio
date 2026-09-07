# Implementation plan

1. Reuse the workspace-bounded project asset adapter to project evidence and
   generated target descriptors into a read-only DTO.
2. Expose that projection through the existing Workbench project controller.
3. Load and render explicit ready, empty, and error states in the Equivalence
   activity area, with no action controls.
4. Verify backend compilation, Theia compilation, and the local adapter
   response.
