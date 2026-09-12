import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const contribution = await readFile(new URL('../src/browser/renovatio-workbench-contribution.ts', import.meta.url), 'utf8');
const widget = await readFile(new URL('../src/browser/renovatio-workbench-widget.tsx', import.meta.url), 'utf8');
const shell = await readFile(new URL('../src/browser/renovatio-shell-widget.tsx', import.meta.url), 'utf8');
const styles = await readFile(new URL('../src/browser/style/renovatio-workbench.css', import.meta.url), 'utf8');
const domainMapper = await readFile(new URL('../src/browser/domain-diagram-mapper.ts', import.meta.url), 'utf8');
const domainClassNode = await readFile(new URL('../src/browser/domain-class-node.tsx', import.meta.url), 'utf8');
const architectureMapper = await readFile(new URL('../src/browser/architecture-diagram-mapper.ts', import.meta.url), 'utf8');
const architectureNode = await readFile(new URL('../src/browser/architecture-node.tsx', import.meta.url), 'utf8');
const domainErNode = await readFile(new URL('../src/browser/domain-er-node.tsx', import.meta.url), 'utf8');
const domainErMarkers = await readFile(new URL('../src/browser/domain-er-markers.tsx', import.meta.url), 'utf8');
const diagramCanvas = await readFile(new URL('../../renovatio-diagram-canvas/src/browser/diagram-canvas.tsx', import.meta.url), 'utf8');

test('registers the stable command and menu contribution', () => {
    assert.match(contribution, /renovatio\.workbench\.open/);
    assert.match(contribution, /registerCommand/);
    assert.match(contribution, /registerMenuAction/);
});

test('renders a named accessible view and environment contract', () => {
    assert.match(widget, /aria-labelledby='renovatio-workbench-heading'/);
    assert.match(widget, /aria-live='polite'/);
    assert.match(widget, /RENOVATIO_BACKEND_URL/);
    assert.match(widget, /RENOVATIO_AUTH_MODE/);
});

test('provides focus, responsive, and reduced-motion styles', () => {
    assert.match(styles, /focus-visible/);
    assert.match(styles, /@media \(max-width: 860px\)/);
    assert.match(styles, /prefers-reduced-motion: no-preference/);
});

test('registers eight activity areas with command palette keybindings', () => {
    for (const area of ['project', 'analysis', 'domain', 'architecture', 'shadow', 'ai', 'changes', 'equivalence']) {
        assert.match(contribution, new RegExp(`renovatio\\.shell\\.${area}`));
    }
    assert.match(contribution, /registerKeybindings/);
    assert.match(contribution, /ctrlcmd\+alt\+1/);
    assert.match(contribution, /ctrlcmd\+alt\+8/);
});

test('provides accessible project navigation over all required asset classes', () => {
    for (const group of ['COBOL sources', 'Copybooks', 'JCL', 'Models', 'Runs', 'Evidence']) {
        assert.match(shell, new RegExp(group));
    }
    assert.match(shell, /aria-label='Renovatio activity areas'/);
    assert.match(shell, /aria-label='Project assets'/);
    assert.match(shell, /const showProjectExplorer = this\.activeArea === 'project'/);
    assert.match(shell, /showProjectExplorer && this\.renderProjectExplorer\(\)/);
    assert.match(shell, /renovatio-shell-grid is-focused-area/);
    assert.match(styles, /renovatio-shell-grid\.is-focused-area/);
    assert.match(shell, /aria-current=/);
});

test('persists the active project and area while exposing failure states', () => {
    assert.match(shell, /renovatio\.workbench\.active-area/);
    assert.match(shell, /renovatio\.workbench\.selected-project/);
    assert.match(shell, /window\.localStorage\.setItem/);
    assert.match(shell, /'permission-denied'/);
    assert.match(shell, /'error'/);
});

test('keeps dashboard continuity at a configuration boundary', () => {
    assert.match(shell, /RENOVATIO_DASHBOARD_URL/);
    assert.match(shell, /Open administrative dashboard/);
    assert.doesNotMatch(shell, /renovatio-ui\/src/);
    assert.doesNotMatch(shell, /Wizard/);
});

test('loads live projects through the Spring Boot adapter with explicit failure states', () => {
    assert.match(shell, /\/api\/workbench\/projects/);
    assert.match(shell, /\/workbench\/assets/);
    assert.match(shell, /encodeURIComponent\(this\.selectedProject\)/);
    assert.match(shell, /response\.status === 401 \|\| response\.status === 403/);
    assert.match(shell, /this\.shellState = 'empty'/);
});

test('reads assets and limits saves to development-approved generated targets', () => {
    assert.match(shell, /assetContentState/);
    assert.match(shell, /method: 'PUT'/);
    assert.match(shell, /selectedAssetWritable/);
    assert.match(shell, /Legacy source and evidence are read-only/);
    assert.match(shell, /Temporary development mode/);
});

test('restores only project-scoped non-sensitive context through the adapter', () => {
    assert.match(shell, /\/workbench\/context/);
    assert.match(shell, /activeArea: this\.activeArea/);
    assert.match(shell, /selectedAssetId: this\.selectedAssetId/);
    assert.match(shell, /loadContext\(\)/);
    assert.match(shell, /persistContext\(\)/);
});

test('shows read-only inventory and persisted run summaries in Analysis', () => {
    assert.match(shell, /\/workbench\/analysis/);
    assert.match(shell, /analysisState/);
    assert.match(shell, /startAnalysis/);
    assert.match(shell, /operation: 'analyze'/);
    assert.match(shell, /pollAnalysisJob/);
    assert.match(shell, /\/api\/jobs\/\$\{encodeURIComponent\(jobId\)\}/);
    assert.match(shell, /Analysis completed but found 0 COBOL programs/);
    assert.match(shell, /Run Analyze to inventory COBOL programs/);
    assert.match(shell, /COBOL scan root/);
    assert.match(shell, /Last run/);
    assert.doesNotMatch(shell, /UI boundary is available; live data remains governed by the existing backend contract\.'\}\<\/p\>\\n            \<\/article\>\\n            \{this\.activeArea === 'analysis'/);
    assert.doesNotMatch(shell, /method: 'POST'.*workbench\/analysis/);
});

test('exposes a read-only Source Explorer over the governed adapter (issue #178)', () => {
    assert.match(shell, /\/workbench\/source-explorer/);
    assert.match(shell, /sourceExplorerState/);
    for (const state of ["'loading'", "'ready'", "'empty'", "'permission-denied'", "'error'"]) {
        assert.match(shell, new RegExp(`sourceExplorerState = ${state}`));
    }
    // navigable tree + outline + search + Problems + IR linkage
    assert.match(shell, /aria-label='Source files'/);
    assert.match(shell, /aria-label=\{`Outline of \$\{file\.name\}`\}/);
    assert.match(shell, /aria-label='Search symbols and references'/);
    assert.match(shell, /aria-label='Problems'/);
    assert.match(shell, /symbol\.irCoordinate/);
    assert.match(shell, /aria-current=\{this\.selectedSourceFileId === entry\.id \? 'true' : undefined\}/);
    // file metadata surfaced
    assert.match(shell, /entry\.hash/);
    assert.match(shell, /entry\.encoding/);
    assert.match(shell, /entry\.analysisStatus/);
    // strictly read-only: never mutates the endpoint
    assert.doesNotMatch(shell, /source-explorer`, \{\s*method/);
    assert.doesNotMatch(shell, /method: '(POST|PUT|DELETE|PATCH)'[^;]*source-explorer/);
});

test('provides the governed DomainModel editor and immutable version boundary (issue #179)', () => {
    assert.match(shell, /\/workbench\/domain-model/);
    assert.match(shell, /method: 'PUT'/);
    assert.match(shell, /expectedRevision: this\.domain\.revision/);
    assert.match(shell, /\/domain-model\/versions/);
    assert.match(shell, /\/domain-model\/compare/);
    assert.match(shell, /\/domain-model\/suggestions/);
    for (const state of ['loading', 'ready', 'empty', 'permission-denied', 'conflict', 'error']) {
        assert.match(shell, new RegExp(`'${state}'`));
    }
    assert.match(shell, /aria-label='Business DomainModel editor'/);
    assert.match(shell, /aria-label='Selected domain element editor'/);
    assert.match(shell, /aria-label='Domain provenance and history'/);
    assert.match(shell, /aria-live='polite'/);
    assert.match(shell, /DOMAIN_KINDS/);
    assert.match(shell, /CARDINALITIES/);
    assert.match(shell, /Stable ids and evidence references remain immutable/);
});

test('links source evidence and symbols in both directions without triggering analysis', () => {
    assert.match(shell, /navigateToSource\(evidence\.sourceRef\)/);
    assert.match(shell, /navigateToDomainFromSource\(file, symbol\)/);
    assert.match(shell, /aria-label='Related DomainModel elements'/);
    assert.match(shell, /sourceRef\.split\('#'\)/);
    assert.doesNotMatch(shell, /navigateToDomainFromSource[\s\S]{0,700}loadAnalysis\(/);
});

test('keeps DomainModel work neutral and outside generation or architecture mutation', () => {
    assert.doesNotMatch(shell, /domain-model[^\n]+method: '(DELETE|PATCH)'/);
    assert.doesNotMatch(shell, /saveDomainModel[\s\S]{0,1200}(generate|architecture)/i);
    assert.match(styles, /renovatio-domain-grid/);
    assert.match(styles, /state-conflict/);
});

test('provides the editable Architecture Canvas with profile history (issue #180)', () => {
    assert.match(shell, /\/workbench\/architecture\/canvas/);
    assert.match(shell, /\/workbench\/architecture\/canvas:preview/);
    assert.match(shell, /previewArchitectureDraft/);
    assert.match(shell, /saveArchitectureProfile/);
    assert.match(shell, /expectedRevision: this\.architecture\.revision/);
    assert.match(shell, /\/architecture\/canvas\/versions/);
    assert.match(shell, /\/architecture\/canvas\/compare/);
    for (const style of ['LAYERED_MVC', 'HEXAGONAL', 'CLEAN', 'LAYERED', 'TRANSACTION_SCRIPT']) {
        assert.match(shell, new RegExp(style));
    }
    for (const state of ['loading', 'ready', 'empty', 'permission-denied', 'conflict', 'error']) {
        assert.match(shell, new RegExp(`'${state}'`));
    }
    assert.match(shell, /aria-label='Architecture Canvas editor'/);
    assert.match(shell, /aria-label='Artifact and package manifest preview'/);
    assert.match(shell, /illegal dependencies visible before generate/);
    assert.match(styles, /renovatio-architecture-grid/);
    assert.match(styles, /renovatio-segmented/);
});

test('maps Architecture profiles to a layer graph canvas (issue #267)', () => {
    assert.match(architectureMapper, /export function architectureToDiagram/);
    assert.match(architectureMapper, /architecture\.canvas/);
    assert.match(architectureMapper, /ARCHITECTURE_LAYER/);
    assert.match(architectureMapper, /ALLOWED_DEPENDENCY/);
    assert.match(architectureMapper, /DENIED_DEPENDENCY/);
    assert.match(architectureMapper, /dependencyDiagnostics/);
    assert.match(architectureNode, /export function ArchitectureNode/);
    assert.match(architectureNode, /has-diagnostics/);
    assert.match(shell, /architectureViewMode: 'diagram' \| 'table'/);
    assert.match(shell, /architectureToDiagram\(view, this\.ai\?\.items \?\? \[\]\)/);
    assert.match(shell, /nodeTypes=\{\{ architectureNode: ArchitectureNode \}\}/);
    assert.match(shell, /edgeStyleFor=\{this\.architectureEdgeStyle\}/);
    assert.match(shell, /aria-label='Architecture view mode'/);
    assert.match(shell, /ADVANCED MAPPING/);
    assert.match(styles, /renovatio-architecture-flow-surface/);
    assert.match(styles, /renovatio-architecture-flow-node\.has-diagnostics/);
});

test('persists diagram layout and soft-prune metadata from both canvases (issue #268)', () => {
    assert.match(shell, /layout: Record<string, DiagramLayoutPosition>/);
    assert.match(shell, /excludedNodeIds: ExcludedNode\[\]/);
    assert.match(shell, /markDomainChanged\(\{ \.\.\.this\.domainDraft, layout:/);
    assert.match(shell, /toggleDomainPrune/);
    assert.match(shell, /toggleArchitecturePrune/);
    assert.match(shell, /markArchitectureChanged\(\{[\s\S]*layout:/);
    assert.match(shell, /enablePrune/);
    assert.match(domainMapper, /model\.layout/);
    assert.match(domainMapper, /excludedNodeIds/);
    assert.match(domainClassNode, /is-excluded/);
    assert.match(architectureMapper, /architecture\.profile\.layout/);
    assert.match(architectureNode, /is-excluded/);
    assert.match(styles, /renovatio-diagram-prune-selection/);
    assert.match(styles, /renovatio-domain-class-node\.is-excluded/);
    assert.match(styles, /renovatio-architecture-flow-node\.is-excluded/);
});

test('generates Architecture canvas change sets through the existing Changes flow (issue #269)', () => {
    assert.match(shell, /generateArchitectureChangeSet/);
    assert.match(shell, /architecture\/canvas:generate/);
    assert.match(shell, /const canGenerate = Boolean/);
    assert.match(shell, /view\.revision > 0/);
    assert.match(shell, /!this\.architectureDirty/);
    assert.match(shell, /!hasBlockingDiagnostics/);
    assert.match(shell, /this\.activateArea\('changes'\)/);
    assert.match(shell, />Generate<\/button>/);
});

test('keeps Architecture Canvas outside DomainModel and generation execution', () => {
    assert.doesNotMatch(shell, /saveArchitectureProfile[\s\S]{0,1200}domain-model/i);
    assert.doesNotMatch(shell, /saveArchitectureProfile[\s\S]{0,1200}generate/i);
    assert.doesNotMatch(shell, /architecture\/canvas[^\n]+method: '(DELETE|PATCH)'/);
});

test('provides read-only Shadow diff and impact analysis (issue #181)', () => {
    assert.match(shell, /\/workbench\/shadow-impact/);
    assert.match(shell, /shadowImpactState/);
    assert.match(shell, /renderShadowImpact/);
    assert.match(shell, /aria-label='Shadow diff and impact analysis'/);
    assert.match(shell, /COBOL → IR → DomainModel → Architecture → Java/);
    assert.match(shell, /Manifest diff before generate/);
    assert.match(shell, /Artifact impact links/);
    assert.match(shell, /Source impact links/);
    assert.match(shell, /Export impact report/);
    assert.match(shell, /JSON\.stringify\(this\.shadowImpact\.report/);
    assert.doesNotMatch(shell, /shadow-impact[^\n]+method: '(POST|PUT|DELETE|PATCH)'/);
    assert.match(styles, /renovatio-shadow-grid/);
});

test('provides governed AI agents and explainable review boundaries (issue #182)', () => {
    assert.match(shell, /\/workbench\/ai/);
    assert.match(shell, /renderGovernedAi/);
    assert.match(shell, /aria-label='Governed AI agents and explainability'/);
    assert.match(shell, /agent\.name/);
    assert.match(shell, /agent\.purpose/);
    assert.match(shell, /agent\.promptId/);
    assert.match(shell, /command\.command/);
    assert.match(shell, /command\.toolCall/);
    assert.match(shell, /command\.humanConfirmationRequired/);
    assert.match(shell, /Context snapshot/);
    assert.match(shell, /Prompt catalog/);
    assert.match(shell, /Audit trail and tool policy/);
    assert.match(shell, /AI never writes final files directly/);
    assert.match(shell, /accept.*edit.*reject/s);
    assert.match(shell, /responseHash/);
    assert.match(shell, /permission-denied/);
    assert.doesNotMatch(shell, /workbench\/ai[^\n]+method: '(POST|PUT|DELETE|PATCH)'/);
    assert.match(styles, /renovatio-ai-grid/);
});

test('provides governed change sets with approval and rollback boundaries (issue #183)', () => {
    assert.match(shell, /\/workbench\/change-sets/);
    assert.match(shell, /renderChangeSets/);
    assert.match(shell, /aria-label='Reviewable change sets approval and rollback'/);
    assert.match(shell, /Mandatory diff/);
    assert.match(shell, /manifest hash is approved/);
    assert.match(shell, /APPROVE DANGEROUS CHANGE SET/);
    assert.match(shell, /APPLY APPROVED CHANGE SET/);
    assert.match(shell, /ROLL BACK APPLIED CHANGE SET/);
    assert.match(shell, /Submit review/);
    assert.match(shell, /Approve diff/);
    assert.match(shell, /Apply approved manifest/);
    assert.match(shell, /Rollback/);
    assert.match(shell, /changeSetAction/);
    assert.match(styles, /renovatio-change-grid/);
    assert.match(styles, /state-rolled-back/);
});

test('provides integrated Equivalence Lab execution and promotion gates (issue #184)', () => {
    assert.match(shell, /\/workbench\/equivalence\/runs/);
    assert.match(shell, /renderEquivalenceLab/);
    assert.match(shell, /aria-label='Integrated Equivalence Lab'/);
    assert.match(shell, /Equivalence fixture selector/);
    assert.match(shell, /Generated target evidence/);
    assert.match(shell, /lab\.generatedTargets/);
    assert.match(shell, /Input editor for sequential files and DB2 responses/);
    assert.match(shell, /Async runs, progress, logs and cancellation/);
    assert.match(shell, /State output file SQL comparison and divergences/);
    assert.match(shell, /Promotion gate/);
    assert.match(shell, /Repeat exactly/);
    assert.match(shell, /Cancel run/);
    assert.match(shell, /Export audited report/);
    assert.match(shell, /Accept difference/);
    assert.match(shell, /Mark defect/);
    assert.match(shell, /Ask AI analysis/);
    assert.match(shell, /Gate blocks promotion/);
    assert.match(styles, /renovatio-equivalence-grid/);
    assert.match(styles, /state-cancelled/);
});

test('renders the DomainModel as a UML class diagram alongside the original list view (issue #265)', () => {
    assert.match(shell, /from '@renovatio\/diagram-canvas\/lib\/browser'/);
    assert.match(shell, /domainViewMode: 'diagram' \| 'der' \| 'list' = 'diagram'/);
    assert.match(shell, /aria-label='Domain view mode'/);
    assert.match(shell, /this\.domainViewMode = 'diagram'/);
    assert.match(shell, /this\.domainViewMode = 'list'/);
    assert.match(shell, /renderDomainDiagramSurface/);
    assert.match(shell, /renderDomainCatalog/);
    assert.match(shell, /<DiagramCanvas/);
    assert.match(shell, /nodeTypes=\{\{ domainClass: DomainClassNode \}\}/);
    assert.match(shell, /domainModelToDiagram\(draft, this\.domainLayoutHints, this\.domain\?\.suggestions \?\? \[\]\)/);
    assert.match(styles, /renovatio-domain-diagram\b/);
    assert.match(styles, /renovatio-domain-class-node/);
});

test('wires DiagramCanvas events back into real DomainModel mutations (issue #265)', () => {
    assert.match(shell, /handleDomainDiagramEvent = \(event: DiagramEvent\)/);
    // Every DiagramEvent case the canvas can emit must be handled explicitly.
    assert.match(shell, /event\.type === 'nodeSelected'/);
    assert.match(shell, /event\.type === 'nodeMoved'/);
    assert.match(shell, /event\.type === 'edgeCreated'/);
    // Selecting a node in the diagram reuses the exact same selection path
    // list mode uses, so the structured editor/inspector panels work for both.
    assert.match(shell, /this\.selectDomainItem\('node', event\.id\)/);
    // A drag-to-connect creates a real DomainRelation with the actual
    // endpoints (not the "same node twice" placeholder addDomainItem uses).
    assert.match(shell, /createDomainRelationFromDrag\(event\.source, event\.target\)/);
    assert.match(shell, /protected createDomainRelationFromDrag\(fromId: string, toId: string\): void/);
    assert.match(shell, /fromId, toId, kind: 'ASSOCIATES_WITH'/);
    // Moved positions are kept in memory only for now (issue #268 persists
    // them); this must not be routed through markDomainChanged/domainDirty.
    assert.match(shell, /domainLayoutHints = \{ \.\.\.this\.domainLayoutHints, \[event\.id\]: \{ x: event\.x, y: event\.y \} \}/);
});

test('domainModelToDiagram maps DomainNode/DomainRelation to the canvas-agnostic view-model (issue #265)', () => {
    assert.match(domainMapper, /export function domainModelToDiagram/);
    assert.match(domainMapper, /id: node\.id/);
    assert.match(domainMapper, /label: node\.name/);
    assert.match(domainMapper, /source: relation\.fromId/);
    assert.match(domainMapper, /target: relation\.toId/);
    // Must not reimplement or duplicate @renovatio/diagram-canvas's own protocol types.
    assert.match(domainMapper, /from '@renovatio\/diagram-canvas\/lib\/browser'/);
});

test('DomainClassNode renders name, stereotype and properties as a UML class box (issue #265)', () => {
    assert.match(domainClassNode, /export function DomainClassNode/);
    assert.match(domainClassNode, /data\.kind/);
    assert.match(domainClassNode, /data\.properties/);
    assert.match(domainClassNode, /property\.required/);
    assert.match(domainClassNode, /from '@xyflow\/react'/);
});

test('surfaces pending Domain suggestions directly on canvas nodes (issue #272)', () => {
    assert.match(domainMapper, /pendingSuggestionCounts/);
    assert.match(domainMapper, /pendingSuggestionCount/);
    assert.match(shell, /domainSuggestionsOnly/);
    assert.match(shell, /aria-pressed=\{this\.domainSuggestionsOnly\}/);
    assert.match(domainClassNode, /renovatio-domain-suggestion-badge/);
    assert.match(styles, /renovatio-domain-suggestion-badge/);
});

test('surfaces governed data migration planning and change-set generation (issue #271)', () => {
    assert.match(shell, /\/workbench\/data-migration/);
    assert.match(shell, /dataMigrationState/);
    assert.match(shell, /loadDataMigration/);
    assert.match(shell, /generateDataMigrationChangeSet/);
    assert.match(shell, /data-migration:generate/);
    assert.match(shell, /DATA MIGRATION/);
    assert.match(shell, /Generate data migration/);
});

test('surfaces AI target bindings on Architecture canvas nodes (issue #272)', () => {
    assert.match(shell, /targetType\?: string; targetId\?: string/);
    assert.match(architectureMapper, /ArchitectureDiagramSuggestion/);
    assert.match(architectureMapper, /pendingSuggestionCounts/);
    assert.match(architectureNode, /renovatio-architecture-suggestion-badge/);
    assert.match(styles, /renovatio-architecture-suggestion-badge/);
});

test('extends the "suggestions only" canvas filter to Architecture, not just Domain (issue #272 gap fix)', () => {
    assert.match(shell, /architectureSuggestionsOnly = false/);
    assert.match(shell, /aria-pressed=\{this\.architectureSuggestionsOnly\}/);
    assert.match(shell, /this\.architectureSuggestionsOnly = !this\.architectureSuggestionsOnly/);
    assert.match(styles, /renovatio-architecture-diagram-toolbar/);
});

test('renders the dry-run preview as an actual row-by-row table, not just a count (issue #271 gap fix)', () => {
    assert.match(shell, /this\.dataMigration\.dryRun\.previewRows\.length > 0/);
    assert.match(shell, /renovatio-data-migration-preview-table/);
    assert.match(shell, /row\.cells\.map\(\(cell, index\)/);
    assert.match(shell, /cell\.sourceValue/);
    assert.match(shell, /cell\.transformedValue/);
    assert.match(shell, /this\.dataMigration\.dryRun\.warnings/);
    assert.match(styles, /renovatio-data-migration-preview-table/);
});

test('gives Domain a real third DER canvas mode with its own node and crow\'s-foot edges, not just annotated UML boxes (issue #266 gap fix)', () => {
    assert.match(shell, /domainViewMode: 'diagram' \| 'der' \| 'list'/);
    assert.match(shell, /aria-pressed=\{this\.domainViewMode === 'der'\}/);
    assert.match(shell, /renderDomainDerSurface/);
    assert.match(shell, /nodeTypes=\{\{ domainClass: DomainErNode \}\}/);
    assert.match(shell, /defs=\{<DomainErMarkerDefs \/>\}/);
    assert.match(shell, /edgeMarkerFor=\{edge => \(\{/);
    assert.match(shell, /markerUrl\(edge\.data\?\.sourceCardinality as string \| undefined\)/);
    assert.match(shell, /markerUrl\(edge\.data\?\.targetCardinality as string \| undefined\)/);
    // FK relations render solid, plain associations render dashed.
    assert.match(shell, /edge\.data\?\.foreignKey \? \{\} : \{ strokeDasharray: '4 3' \}/);
});

test('DomainErNode renders a table box with PK markers, distinct from the UML class box (issue #266 gap fix)', () => {
    assert.match(domainErNode, /export function DomainErNode/);
    assert.match(domainErNode, /property\.isKey \? 'PK' : ''/);
    assert.match(domainErNode, /data\.tableName \|\| data\.label/);
    assert.match(domainErNode, /from '@xyflow\/react'/);
});

test('DomainErMarkerDefs supplies one <marker> per CARDINALITIES value for crow\'s-foot notation (issue #266 gap fix)', () => {
    for (const cardinality of ['ONE', 'ZERO_OR_ONE', 'ONE_OR_MORE', 'ZERO_OR_MORE']) {
        assert.match(domainErMarkers, new RegExp(`${cardinality}:`));
    }
    assert.match(domainErMarkers, /export function DomainErMarkerDefs/);
    assert.match(domainErMarkers, /export function markerUrl/);
    assert.match(domainErMarkers, /<marker /);
});

test('DiagramCanvas exposes edgeMarkerFor/defs generically, with no ER-specific logic baked in (issue #266 gap fix)', () => {
    assert.match(diagramCanvas, /edgeMarkerFor\?:/);
    assert.match(diagramCanvas, /defs\?: React\.ReactNode/);
    // Comments may explain the feature using an ER example; the package must
    // not itself branch on cardinality values or know what a foreign key is —
    // that logic belongs to the host (domain-er-markers.tsx / domain-diagram-mapper.ts).
    assert.doesNotMatch(diagramCanvas, /ONE_OR_MORE|ZERO_OR_MORE|ZERO_OR_ONE|\.foreignKey/);
});
