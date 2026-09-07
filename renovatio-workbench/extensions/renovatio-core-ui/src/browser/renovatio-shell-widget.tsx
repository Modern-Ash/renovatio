import { Message, ReactWidget } from '@theia/core/lib/browser';
import { EnvVariable, EnvVariablesServer } from '@theia/core/lib/common/env-variables';
import { inject, injectable, postConstruct } from '@theia/core/shared/inversify';
import React from '@theia/core/shared/react';

export type RenovatioAreaId = 'project' | 'analysis' | 'domain' | 'architecture' | 'ai' | 'equivalence';
type ShellState = 'loading' | 'ready' | 'empty' | 'permission-denied' | 'error';

interface RenovatioArea {
    id: RenovatioAreaId;
    coordinate: string;
    label: string;
    summary: string;
}

const AREAS: readonly RenovatioArea[] = [
    { id: 'project', coordinate: 'ACT.01', label: 'Project', summary: 'Sources, copybooks, JCL and project evidence.' },
    { id: 'analysis', coordinate: 'ACT.02', label: 'Analysis', summary: 'Readiness signals, inventory and execution runs.' },
    { id: 'domain', coordinate: 'ACT.03', label: 'Domain', summary: 'Neutral business model, provenance and versioned corrections.' },
    { id: 'architecture', coordinate: 'ACT.04', label: 'Architecture', summary: 'Target architecture views and dependency boundaries.' },
    { id: 'ai', coordinate: 'ACT.05', label: 'AI', summary: 'Governed suggestions, provenance and review boundaries.' },
    { id: 'equivalence', coordinate: 'ACT.06', label: 'Equivalence', summary: 'Evidence, test deltas and acceptance history.' }
];

const PROJECT_ASSETS = [
    { group: 'COBOL sources', items: ['PAYROLL.CBL', 'LEDGER.CBL'] },
    { group: 'Copybooks', items: ['COMMON-RECORDS.CPY'] },
    { group: 'JCL', items: ['NIGHTLY-BATCH.JCL'] },
    { group: 'Models', items: ['payments-domain.json'] },
    { group: 'Runs', items: ['analysis-2026-09-05'] },
    { group: 'Evidence', items: ['equivalence-ledger.md'] }
] as const;
type ProjectAssetItem = { id: string; name: string; writable: boolean };
type ProjectAssetGroup = { group: string; items: ProjectAssetItem[] };
type WorkbenchProject = { id: string; name: string };
type WorkbenchAsset = { id: string; name: string; category: string; writable: boolean };
type WorkbenchContext = { activeArea: RenovatioAreaId | null; selectedAssetId: string | null };
type WorkbenchAnalysis = { inventory: Record<string, number>; runs: Array<{ runId: string; dryRun: boolean; startedAt: string }> };
type ArchitectureStyle = 'TRANSACTION_SCRIPT' | 'LAYERED_MVC' | 'HEXAGONAL' | 'CLEAN' | 'LAYERED';
type ModuleGrouping = 'BY_PROGRAM' | 'BY_DOMAIN' | 'SINGLE_MODULE';
type ArchitectureRule = { fromLayer: string; toLayer: string; allowed: boolean; reason: string };
type ArchitectureProfileDraft = { style: ArchitectureStyle; moduleGrouping: ModuleGrouping; framework: string; persistence: string; packageRoots: Record<string, string>; suffixes: Record<string, string>; classNames: Record<string, string>; dependencyRules: ArchitectureRule[] };
type ArchitectureCanvasNode = { id: string; layer: string; kind: string; label: string; packageName: string; className: string; componentId: string };
type ArchitectureDiagnostic = { severity: string; code: string; fromLayer: string; toLayer: string; message: string };
type ArchitectureManifestEntry = { path: string; role: string; layer: string; className: string; packageName: string; componentId: string };
type ArchitectureVersion = { revision: number; canonicalHash: string; savedAt: string; style: ArchitectureStyle };
type ArchitectureComparison = { added: DomainChange[]; removed: DomainChange[]; changed: DomainChange[] };
type WorkbenchArchitecture = { revision: number; canonicalHash: string; savedAt: string | null; profile: ArchitectureProfileDraft; preview: { modules: unknown[]; components: unknown[]; relations: unknown[]; diagnostics: unknown[]; hasFallback: boolean }; canvas: ArchitectureCanvasNode[]; dependencyRules: ArchitectureRule[]; dependencyDiagnostics: ArchitectureDiagnostic[]; manifest: ArchitectureManifestEntry[] };
type WorkbenchAi = { items: Array<{ id: string; category: string; source: string; status: string; confidence: number; evidenceCount: number; llmFailed: boolean }> };
type WorkbenchEquivalence = { evidence: Array<{ id: string; name: string }>; generatedTargets: Array<{ id: string; name: string }>; verdicts: Array<{ fixtureId: string; classification: string; reason: string; blocksRelease: boolean }> };
type SourceSymbol = { id: string; kind: string; name: string; line: number; column: number; parentId: string | null; irCoordinate: string };
type SourceDiagnostic = { severity: string; message: string; line: number };
type SourceFile = { id: string; name: string; kind: string; path: string; hash: string; encoding: string; analysisStatus: string; symbols: SourceSymbol[]; diagnostics: SourceDiagnostic[] };
type SourceExplorer = { files: SourceFile[]; datasets: Array<{ id: string; name: string; referencedBy: string[] }> };
type AreaState = 'idle' | 'loading' | 'ready' | 'empty' | 'permission-denied' | 'error';
type DomainEvidence = { sourceRef: string; provenance: string; rationale: string };
type DomainProperty = { name: string; type: string; required: boolean; evidence: DomainEvidence[] };
type DomainNode = { id: string; kind: string; name: string; properties: DomainProperty[]; evidence: DomainEvidence[]; origin: string; confidence: number };
type DomainRelation = { id: string; fromId: string; toId: string; kind: string; sourceCardinality: string; targetCardinality: string };
type DomainInvariant = { id: string; subjectId: string; expression: string; evidence: DomainEvidence[]; origin: string; confidence: number };
type DomainModel = { schemaVersion: string; projectId: string; nodes: DomainNode[]; relations: DomainRelation[]; invariants: DomainInvariant[] };
type DomainDiagnostic = { severity: string; code: string; targetId: string; message: string };
type DomainSuggestion = { id: string; targetType: string; targetId: string; name: string; status: string; decidedRevision: number | null; decidedAt: string | null };
type DomainModelView = { revision: number; canonicalHash: string; savedAt: string | null; model: DomainModel; diagnostics: DomainDiagnostic[]; suggestions: DomainSuggestion[] };
type DomainVersion = { revision: number; canonicalHash: string; savedAt: string };
type DomainChange = { targetType: string; targetId: string; beforeValue: unknown; afterValue: unknown };
type DomainComparison = { added: DomainChange[]; removed: DomainChange[]; changed: DomainChange[] };
type DomainState = AreaState | 'saving' | 'conflict';
type DomainItemType = 'node' | 'relation' | 'invariant';

const DOMAIN_KINDS = ['ENTITY', 'VALUE_OBJECT', 'AGGREGATE', 'USE_CASE', 'DOMAIN_SERVICE', 'REPOSITORY', 'EXTERNAL_SYSTEM', 'EVENT', 'BOUNDED_CONTEXT'] as const;
const RELATION_KINDS = ['CONTAINS', 'USES', 'IMPLEMENTS', 'DEPENDS_ON', 'PUBLISHES', 'SUBSCRIBES_TO', 'ASSOCIATES_WITH', 'MAPS_TO'] as const;
const CARDINALITIES = ['ONE', 'ZERO_OR_ONE', 'ONE_OR_MORE', 'ZERO_OR_MORE'] as const;
const MVC_LAYERS = ['controller', 'service', 'model'] as const;

const ACTIVE_AREA_KEY = 'renovatio.workbench.active-area';
const SELECTED_PROJECT_KEY = 'renovatio.workbench.selected-project';

@injectable()
export class RenovatioShellWidget extends ReactWidget {
    static readonly ID = 'renovatio.shell.widget';
    static readonly LABEL = 'Renovatio Control Deck';

    @inject(EnvVariablesServer)
    protected readonly envVariables!: EnvVariablesServer;

    protected activeArea: RenovatioAreaId = 'project';
    protected dashboardUrl = 'http://127.0.0.1:5173/';
    protected backendUrl = 'http://127.0.0.1:8080';
    protected projects: WorkbenchProject[] = [];
    protected projectAssets: ProjectAssetGroup[] = PROJECT_ASSETS.map(group => ({ group: group.group, items: group.items.map(name => ({ id: name, name, writable: false })) }));
    protected loading = true;
    protected selectedAsset = 'PAYROLL.CBL';
    protected selectedAssetId = 'PAYROLL.CBL';
    protected selectedAssetWritable = false;
    protected assetContent = '';
    protected assetContentState: 'idle' | 'loading' | 'ready' | 'saving' | 'error' = 'idle';
    protected selectedProject = 'payroll-modernization';
    protected shellState: ShellState = 'loading';
    protected analysis?: WorkbenchAnalysis;
    protected analysisState: 'idle' | 'loading' | 'ready' | 'empty' | 'error' = 'idle';
    protected architecture?: WorkbenchArchitecture;
    protected architectureDraft?: ArchitectureProfileDraft;
    protected architectureState: 'idle' | 'loading' | 'ready' | 'empty' | 'permission-denied' | 'saving' | 'conflict' | 'error' = 'idle';
    protected architectureVersions: ArchitectureVersion[] = [];
    protected architectureComparison?: ArchitectureComparison;
    protected architectureDirty = false;
    protected architectureNotice = '';
    protected selectedArchitectureLayer = 'controller';
    protected architectureCompareFrom = 0;
    protected architectureCompareTo = 0;
    protected ai?: WorkbenchAi;
    protected aiState: 'idle' | 'loading' | 'ready' | 'empty' | 'error' = 'idle';
    protected equivalence?: WorkbenchEquivalence;
    protected equivalenceState: 'idle' | 'loading' | 'ready' | 'empty' | 'error' = 'idle';
    protected sourceExplorer?: SourceExplorer;
    protected sourceExplorerState: AreaState = 'idle';
    protected selectedSourceFileId: string | null = null;
    protected symbolSearch = '';
    protected domain?: DomainModelView;
    protected domainDraft?: DomainModel;
    protected domainState: DomainState = 'idle';
    protected domainVersions: DomainVersion[] = [];
    protected domainComparison?: DomainComparison;
    protected selectedDomainType: DomainItemType = 'node';
    protected selectedDomainId: string | null = null;
    protected domainFilter = '';
    protected domainDirty = false;
    protected domainNotice = '';
    protected compareFrom = 0;
    protected compareTo = 0;

    @postConstruct()
    protected init(): void {
        this.id = RenovatioShellWidget.ID;
        this.title.label = RenovatioShellWidget.LABEL;
        this.title.caption = 'Renovatio IDE shell';
        this.title.closable = true;
        this.title.iconClass = 'codicon codicon-layout';
        this.addClass('renovatio-workbench-widget');
        this.addClass('renovatio-shell-widget');
        this.node.tabIndex = 0;
        this.restoreShellState();
        void this.loadRuntimeConfig();
    }

    activateArea(area: RenovatioAreaId): void {
        this.activeArea = area;
        this.persistShellState();
        void this.persistContext();
        if (area === 'analysis') void this.loadAnalysis();
        if (area === 'domain') void this.loadDomainModel();
        if (area === 'architecture') void this.loadArchitecture();
        if (area === 'ai') void this.loadAi();
        if (area === 'equivalence') void this.loadEquivalence();
        if (area === 'project') void this.loadSourceExplorer();
        this.update();
    }

    protected async loadRuntimeConfig(): Promise<void> {
        const dashboard = await this.readEnv('RENOVATIO_DASHBOARD_URL');
        const backend = await this.readEnv('RENOVATIO_BACKEND_URL');
        this.dashboardUrl = dashboard ?? this.dashboardUrl;
        this.backendUrl = (backend ?? this.backendUrl).replace(/\/$/, '');
        await this.loadProjects();
        this.loading = false;
        if (this.shellState === 'loading') this.shellState = 'ready';
        if (this.activeArea === 'analysis') void this.loadAnalysis();
        if (this.activeArea === 'architecture') void this.loadArchitecture();
        if (this.activeArea === 'ai') void this.loadAi();
        if (this.activeArea === 'equivalence') void this.loadEquivalence();
        void this.loadSourceExplorer();
        void this.loadDomainModel();
        this.update();
    }

    protected async loadProjects(): Promise<void> {
        try {
            const response = await fetch(`${this.backendUrl}/api/workbench/projects`);
            if (response.status === 401 || response.status === 403) { this.shellState = 'permission-denied'; return; }
            if (!response.ok) throw new Error(`Project adapter returned ${response.status}`);
            this.projects = await response.json() as WorkbenchProject[];
            if (!this.projects.length) { this.shellState = 'empty'; return; }
            if (!this.projects.some(project => project.id === this.selectedProject)) this.selectedProject = this.projects[0].id;
            await this.loadAssets();
            await this.loadContext();
        } catch {
            this.shellState = 'error';
        }
    }

    protected async loadAssets(): Promise<void> {
        const response = await fetch(`${this.backendUrl}/api/projects/${encodeURIComponent(this.selectedProject)}/workbench/assets`);
        if (!response.ok) throw new Error(`Asset adapter returned ${response.status}`);
        const assets = await response.json() as WorkbenchAsset[];
        this.projectAssets = assets.reduce<ProjectAssetGroup[]>((groups, asset) => {
            const group = groups.find(candidate => candidate.group === asset.category);
            const item = { id: asset.id, name: asset.name, writable: asset.writable };
            if (group) group.items.push(item); else groups.push({ group: asset.category, items: [item] });
            return groups;
        }, []);
        const first = assets[0];
        if (first) { this.selectedAsset = first.name; this.selectedAssetId = first.id; this.selectedAssetWritable = first.writable; }
    }

    protected async loadContext(): Promise<void> {
        try {
            const response = await fetch(`${this.backendUrl}/api/projects/${encodeURIComponent(this.selectedProject)}/workbench/context`);
            if (response.status === 401 || response.status === 403) { this.shellState = 'permission-denied'; return; }
            if (!response.ok) throw new Error(`Context adapter returned ${response.status}`);
            const context = await response.json() as WorkbenchContext;
            if (context.activeArea && AREAS.some(area => area.id === context.activeArea)) this.activeArea = context.activeArea;
            const asset = this.projectAssets.flatMap(group => group.items).find(item => item.id === context.selectedAssetId)
                ?? this.projectAssets.flatMap(group => group.items)[0];
            if (asset) await this.selectAsset(asset, false);
        } catch { this.persistShellState(); }
    }

    protected async persistContext(): Promise<void> {
        if (!this.projects.some(project => project.id === this.selectedProject) || !this.selectedAssetId) return;
        try {
            await fetch(`${this.backendUrl}/api/projects/${encodeURIComponent(this.selectedProject)}/workbench/context`, {
                method: 'PUT', headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ activeArea: this.activeArea, selectedAssetId: this.selectedAssetId })
            });
        } catch { this.persistShellState(); }
    }

    protected async loadAnalysis(): Promise<void> {
        if (!this.projects.some(project => project.id === this.selectedProject)) return;
        this.analysisState = 'loading'; this.update();
        try {
            const response = await fetch(`${this.backendUrl}/api/projects/${encodeURIComponent(this.selectedProject)}/workbench/analysis`);
            if (!response.ok) throw new Error(`Analysis adapter returned ${response.status}`);
            this.analysis = await response.json() as WorkbenchAnalysis;
            this.analysisState = Object.keys(this.analysis.inventory).length || this.analysis.runs.length ? 'ready' : 'empty';
        } catch { this.analysisState = 'error'; }
        this.update();
    }

    protected async loadArchitecture(): Promise<void> {
        if (!this.projects.some(project => project.id === this.selectedProject)) return;
        this.architectureState = 'loading'; this.architectureNotice = ''; this.update();
        try {
            const base = `${this.backendUrl}/api/projects/${encodeURIComponent(this.selectedProject)}/workbench/architecture/canvas`;
            const [canvasResponse, versionsResponse] = await Promise.all([fetch(base), fetch(`${base}/versions`)]);
            if ([canvasResponse.status, versionsResponse.status].some(status => status === 401 || status === 403)) {
                this.architectureState = 'permission-denied'; this.update(); return;
            }
            if (!canvasResponse.ok || !versionsResponse.ok) throw new Error('Architecture canvas adapter is unavailable');
            this.architecture = await canvasResponse.json() as WorkbenchArchitecture;
            this.architectureVersions = await versionsResponse.json() as ArchitectureVersion[];
            this.architectureDraft = this.cloneArchitectureProfile(this.architecture.profile);
            this.architectureState = this.architecture.canvas.length || this.architecture.manifest.length ? 'ready' : 'empty';
            this.architectureDirty = false;
            this.selectedArchitectureLayer = this.architectureDraft.dependencyRules[0]?.fromLayer ?? 'controller';
            this.architectureCompareFrom = this.architectureVersions[this.architectureVersions.length - 1]?.revision ?? 0;
            this.architectureCompareTo = this.architectureVersions[0]?.revision ?? 0;
        } catch { this.architectureState = 'error'; }
        this.update();
    }

    protected cloneArchitectureProfile(profile: ArchitectureProfileDraft): ArchitectureProfileDraft {
        return JSON.parse(JSON.stringify(profile)) as ArchitectureProfileDraft;
    }

    protected markArchitectureChanged(profile: ArchitectureProfileDraft): void {
        this.architectureDraft = profile;
        this.architectureDirty = true;
        this.architectureNotice = 'Architecture profile has unsaved changes.';
        this.update();
    }

    protected updateArchitectureProfile(patch: Partial<ArchitectureProfileDraft>): void {
        if (!this.architectureDraft) return;
        this.markArchitectureChanged({ ...this.architectureDraft, ...patch });
    }

    protected updateArchitectureMap(section: 'packageRoots' | 'suffixes' | 'classNames', key: string, value: string): void {
        if (!this.architectureDraft) return;
        this.markArchitectureChanged({ ...this.architectureDraft, [section]: { ...this.architectureDraft[section], [key]: value } });
    }

    protected updateArchitectureRule(index: number, patch: Partial<ArchitectureRule>): void {
        if (!this.architectureDraft) return;
        this.markArchitectureChanged({ ...this.architectureDraft, dependencyRules: this.architectureDraft.dependencyRules.map((rule, candidate) => candidate === index ? { ...rule, ...patch } : rule) });
    }

    protected async saveArchitectureProfile(): Promise<void> {
        if (!this.architecture || !this.architectureDraft || !this.architectureDirty || this.architectureState === 'saving') return;
        this.architectureState = 'saving';
        this.architectureNotice = 'Saving architecture profile revision...';
        this.update();
        try {
            const response = await fetch(`${this.backendUrl}/api/projects/${encodeURIComponent(this.selectedProject)}/workbench/architecture/canvas`, {
                method: 'PUT', headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ expectedRevision: this.architecture.revision, profile: this.architectureDraft })
            });
            if (response.status === 401 || response.status === 403) { this.architectureState = 'permission-denied'; this.architectureNotice = 'You do not have permission to save architecture profiles.'; this.update(); return; }
            if (response.status === 409) { this.architectureState = 'conflict'; this.architectureNotice = 'A newer architecture profile exists. Reload before applying these changes.'; this.update(); return; }
            if (!response.ok) {
                const error = await response.json() as { message?: string; diagnostics?: ArchitectureDiagnostic[] };
                this.architectureNotice = error.diagnostics?.map(item => item.message).join(' · ') || error.message || 'Architecture profile validation failed.';
                this.architectureState = 'error'; this.update(); return;
            }
            this.architecture = await response.json() as WorkbenchArchitecture;
            this.architectureDraft = this.cloneArchitectureProfile(this.architecture.profile);
            this.architectureDirty = false;
            this.architectureState = this.architecture.canvas.length || this.architecture.manifest.length ? 'ready' : 'empty';
            this.architectureNotice = `Saved architecture revision ${this.architecture.revision}.`;
            await this.refreshArchitectureVersions();
        } catch { this.architectureState = 'error'; this.architectureNotice = 'The architecture profile could not be saved.'; }
        this.update();
    }

    protected async refreshArchitectureVersions(): Promise<void> {
        const response = await fetch(`${this.backendUrl}/api/projects/${encodeURIComponent(this.selectedProject)}/workbench/architecture/canvas/versions`);
        if (response.ok) this.architectureVersions = await response.json() as ArchitectureVersion[];
    }

    protected async restoreArchitectureVersion(revision: number): Promise<void> {
        if (!this.architecture || this.architectureDirty) { this.architectureNotice = 'Save or reload the draft before restoring history.'; this.update(); return; }
        try {
            const response = await fetch(`${this.backendUrl}/api/projects/${encodeURIComponent(this.selectedProject)}/workbench/architecture/canvas/versions/${revision}:restore`, {
                method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ expectedRevision: this.architecture.revision })
            });
            if (response.status === 409) { this.architectureState = 'conflict'; this.architectureNotice = 'A newer profile exists. Reload before restoring.'; this.update(); return; }
            if (response.status === 401 || response.status === 403) { this.architectureState = 'permission-denied'; this.architectureNotice = 'You do not have permission to restore profiles.'; this.update(); return; }
            if (!response.ok) throw new Error('Restore failed');
            this.architecture = await response.json() as WorkbenchArchitecture;
            this.architectureDraft = this.cloneArchitectureProfile(this.architecture.profile);
            this.architectureDirty = false;
            this.architectureState = this.architecture.canvas.length || this.architecture.manifest.length ? 'ready' : 'empty';
            this.architectureNotice = `Restored architecture revision ${revision} as revision ${this.architecture.revision}.`;
            await this.refreshArchitectureVersions();
        } catch { this.architectureState = 'error'; this.architectureNotice = 'The historical architecture profile could not be restored.'; }
        this.update();
    }

    protected async compareArchitectureVersions(): Promise<void> {
        if (!this.architectureCompareFrom || !this.architectureCompareTo) { this.architectureNotice = 'Choose two persisted profile revisions to compare.'; this.update(); return; }
        try {
            const base = `${this.backendUrl}/api/projects/${encodeURIComponent(this.selectedProject)}/workbench/architecture/canvas/compare`;
            const response = await fetch(`${base}?from=${this.architectureCompareFrom}&to=${this.architectureCompareTo}`);
            if (!response.ok) throw new Error('Comparison failed');
            this.architectureComparison = await response.json() as ArchitectureComparison;
            this.architectureNotice = `Compared architecture revisions ${this.architectureCompareFrom} and ${this.architectureCompareTo}.`;
        } catch { this.architectureState = 'error'; this.architectureNotice = 'The architecture comparison could not be loaded.'; }
        this.update();
    }

    protected async loadAi(): Promise<void> {
        if (!this.projects.some(project => project.id === this.selectedProject)) return;
        this.aiState = 'loading'; this.update();
        try {
            const response = await fetch(`${this.backendUrl}/api/projects/${encodeURIComponent(this.selectedProject)}/workbench/ai`);
            if (!response.ok) throw new Error(`AI adapter returned ${response.status}`);
            this.ai = await response.json() as WorkbenchAi;
            this.aiState = this.ai.items.length ? 'ready' : 'empty';
        } catch { this.aiState = 'error'; }
        this.update();
    }

    protected async loadEquivalence(): Promise<void> {
        if (!this.projects.some(project => project.id === this.selectedProject)) return;
        this.equivalenceState = 'loading'; this.update();
        try {
            const response = await fetch(`${this.backendUrl}/api/projects/${encodeURIComponent(this.selectedProject)}/workbench/equivalence`);
            if (!response.ok) throw new Error(`Equivalence adapter returned ${response.status}`);
            this.equivalence = await response.json() as WorkbenchEquivalence;
            this.equivalenceState = this.equivalence.evidence.length || this.equivalence.generatedTargets.length || this.equivalence.verdicts.length ? 'ready' : 'empty';
        } catch { this.equivalenceState = 'error'; }
        this.update();
    }

    protected async loadSourceExplorer(): Promise<void> {
        if (!this.projects.some(project => project.id === this.selectedProject)) {
            this.sourceExplorerState = this.projects.length ? 'error' : 'idle';
            this.update();
            return;
        }
        this.sourceExplorerState = 'loading'; this.update();
        try {
            const response = await fetch(`${this.backendUrl}/api/projects/${encodeURIComponent(this.selectedProject)}/workbench/source-explorer`);
            if (response.status === 401 || response.status === 403) { this.sourceExplorerState = 'permission-denied'; this.update(); return; }
            if (!response.ok) throw new Error(`Source explorer adapter returned ${response.status}`);
            this.sourceExplorer = await response.json() as SourceExplorer;
            if (this.sourceExplorer.files.length) {
                if (!this.sourceExplorer.files.some(file => file.id === this.selectedSourceFileId)) {
                    this.selectedSourceFileId = this.sourceExplorer.files[0].id;
                }
                this.sourceExplorerState = 'ready';
            } else {
                this.selectedSourceFileId = null;
                this.sourceExplorerState = 'empty';
            }
        } catch { this.sourceExplorerState = 'error'; }
        this.update();
    }

    protected selectSourceFile(id: string): void { this.selectedSourceFileId = id; this.update(); }
    protected updateSymbolSearch(event: React.ChangeEvent<HTMLInputElement>): void { this.symbolSearch = event.target.value; this.update(); }

    protected get selectedSourceFile(): SourceFile | undefined {
        return this.sourceExplorer?.files.find(file => file.id === this.selectedSourceFileId);
    }

    protected symbolMatches(): Array<{ file: SourceFile; symbol: SourceSymbol }> {
        const term = this.symbolSearch.trim().toLowerCase();
        if (!term || !this.sourceExplorer) return [];
        return this.sourceExplorer.files.flatMap(file => file.symbols
            .filter(symbol => symbol.name.toLowerCase().includes(term) || symbol.kind.toLowerCase().includes(term))
            .map(symbol => ({ file, symbol })));
    }

    protected cloneDomainModel(model: DomainModel): DomainModel {
        return JSON.parse(JSON.stringify(model)) as DomainModel;
    }

    protected async loadDomainModel(): Promise<void> {
        if (!this.projects.some(project => project.id === this.selectedProject)) return;
        this.domainState = 'loading';
        this.domainNotice = '';
        this.update();
        try {
            const base = `${this.backendUrl}/api/projects/${encodeURIComponent(this.selectedProject)}/workbench/domain-model`;
            const [modelResponse, versionsResponse] = await Promise.all([fetch(base), fetch(`${base}/versions`)]);
            if ([modelResponse.status, versionsResponse.status].some(status => status === 401 || status === 403)) {
                this.domainState = 'permission-denied';
                this.update();
                return;
            }
            if (!modelResponse.ok || !versionsResponse.ok) throw new Error('DomainModel adapter is unavailable');
            this.domain = await modelResponse.json() as DomainModelView;
            this.domainVersions = await versionsResponse.json() as DomainVersion[];
            this.domainDraft = this.cloneDomainModel(this.domain.model);
            this.domainState = this.domain.model.nodes.length || this.domain.model.relations.length || this.domain.model.invariants.length ? 'ready' : 'empty';
            this.domainDirty = false;
            const selectedExists = this.selectedDomainId && [
                ...this.domainDraft.nodes, ...this.domainDraft.relations, ...this.domainDraft.invariants
            ].some(item => item.id === this.selectedDomainId);
            if (!selectedExists) {
                const first = this.domainDraft.nodes[0] ?? this.domainDraft.relations[0] ?? this.domainDraft.invariants[0];
                this.selectedDomainId = first?.id ?? null;
                this.selectedDomainType = this.domainDraft.nodes.length ? 'node' : this.domainDraft.relations.length ? 'relation' : 'invariant';
            }
            this.compareFrom = this.domainVersions[this.domainVersions.length - 1]?.revision ?? 0;
            this.compareTo = this.domainVersions[0]?.revision ?? 0;
        } catch {
            this.domainState = 'error';
        }
        this.update();
    }

    protected async refreshDomainVersions(): Promise<void> {
        const response = await fetch(`${this.backendUrl}/api/projects/${encodeURIComponent(this.selectedProject)}/workbench/domain-model/versions`);
        if (response.ok) this.domainVersions = await response.json() as DomainVersion[];
    }

    protected selectDomainItem(type: DomainItemType, id: string): void {
        this.selectedDomainType = type;
        this.selectedDomainId = id;
        this.update();
    }

    protected markDomainChanged(model: DomainModel): void {
        this.domainDraft = model;
        this.domainState = model.nodes.length || model.relations.length || model.invariants.length ? 'ready' : 'empty';
        this.domainDirty = true;
        this.domainNotice = 'Unsaved domain changes.';
        this.update();
    }

    protected updateDomainNode(id: string, patch: Partial<DomainNode>): void {
        if (!this.domainDraft) return;
        this.markDomainChanged({ ...this.domainDraft, nodes: this.domainDraft.nodes.map(node => node.id === id ? { ...node, ...patch } : node) });
    }

    protected updateDomainRelation(id: string, patch: Partial<DomainRelation>): void {
        if (!this.domainDraft) return;
        this.markDomainChanged({ ...this.domainDraft, relations: this.domainDraft.relations.map(relation => relation.id === id ? { ...relation, ...patch } : relation) });
    }

    protected updateDomainInvariant(id: string, patch: Partial<DomainInvariant>): void {
        if (!this.domainDraft) return;
        this.markDomainChanged({ ...this.domainDraft, invariants: this.domainDraft.invariants.map(invariant => invariant.id === id ? { ...invariant, ...patch } : invariant) });
    }

    protected addDomainItem(type: DomainItemType): void {
        if (!this.domainDraft) return;
        const allIds = new Set([...this.domainDraft.nodes, ...this.domainDraft.relations, ...this.domainDraft.invariants].map(item => item.id));
        let suffix = 1;
        while (allIds.has(`${type}-${suffix}`)) suffix++;
        const id = `${type}-${suffix}`;
        if (type === 'node') {
            this.markDomainChanged({ ...this.domainDraft, nodes: [...this.domainDraft.nodes, { id, kind: 'ENTITY', name: 'New domain element', properties: [], evidence: [], origin: 'HUMAN', confidence: 1 }] });
        } else if (type === 'relation') {
            if (this.domainDraft.nodes.length < 1) { this.domainNotice = 'Create a domain element before adding a relation.'; this.update(); return; }
            const nodeId = this.domainDraft.nodes[0].id;
            this.markDomainChanged({ ...this.domainDraft, relations: [...this.domainDraft.relations, { id, fromId: nodeId, toId: nodeId, kind: 'ASSOCIATES_WITH', sourceCardinality: 'ONE', targetCardinality: 'ONE' }] });
        } else {
            if (this.domainDraft.nodes.length < 1) { this.domainNotice = 'Create a domain element before adding an invariant.'; this.update(); return; }
            this.markDomainChanged({ ...this.domainDraft, invariants: [...this.domainDraft.invariants, { id, subjectId: this.domainDraft.nodes[0].id, expression: 'Describe the business rule', evidence: [], origin: 'HUMAN', confidence: 1 }] });
        }
        this.selectedDomainType = type;
        this.selectedDomainId = id;
    }

    protected removeSelectedDomainItem(): void {
        if (!this.domainDraft || !this.selectedDomainId) return;
        const id = this.selectedDomainId;
        if (this.selectedDomainType === 'node') {
            const referenced = this.domainDraft.relations.some(relation => relation.fromId === id || relation.toId === id)
                || this.domainDraft.invariants.some(invariant => invariant.subjectId === id);
            if (referenced) { this.domainNotice = 'Remove relations and invariants that reference this element first.'; this.update(); return; }
            this.markDomainChanged({ ...this.domainDraft, nodes: this.domainDraft.nodes.filter(node => node.id !== id) });
        } else if (this.selectedDomainType === 'relation') {
            this.markDomainChanged({ ...this.domainDraft, relations: this.domainDraft.relations.filter(relation => relation.id !== id) });
        } else {
            this.markDomainChanged({ ...this.domainDraft, invariants: this.domainDraft.invariants.filter(invariant => invariant.id !== id) });
        }
        this.selectedDomainId = null;
    }

    protected addDomainProperty(node: DomainNode): void {
        let suffix = node.properties.length + 1;
        const names = new Set(node.properties.map(property => property.name));
        while (names.has(`property${suffix}`)) suffix++;
        this.updateDomainNode(node.id, { properties: [...node.properties, { name: `property${suffix}`, type: 'string', required: false, evidence: [] }] });
    }

    protected updateDomainProperty(node: DomainNode, index: number, patch: Partial<DomainProperty>): void {
        this.updateDomainNode(node.id, { properties: node.properties.map((property, candidate) => candidate === index ? { ...property, ...patch } : property) });
    }

    protected async saveDomainModel(): Promise<void> {
        if (!this.domain || !this.domainDraft || !this.domainDirty || this.domainState === 'saving') return;
        this.domainState = 'saving';
        this.domainNotice = 'Saving domain revision…';
        this.update();
        try {
            const response = await fetch(`${this.backendUrl}/api/projects/${encodeURIComponent(this.selectedProject)}/workbench/domain-model`, {
                method: 'PUT', headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ expectedRevision: this.domain.revision, model: this.domainDraft })
            });
            if (response.status === 401 || response.status === 403) { this.domainState = 'permission-denied'; this.domainNotice = 'You do not have permission to save this DomainModel.'; this.update(); return; }
            if (response.status === 409) { this.domainState = 'conflict'; this.domainNotice = 'A newer revision exists. Reload before applying these changes.'; this.update(); return; }
            if (!response.ok) {
                const error = await response.json() as { message?: string; diagnostics?: DomainDiagnostic[] };
                this.domainNotice = error.diagnostics?.map(item => item.message).join(' · ') || error.message || 'DomainModel validation failed.';
                this.domainState = 'error'; this.update(); return;
            }
            this.domain = await response.json() as DomainModelView;
            this.domainDraft = this.cloneDomainModel(this.domain.model);
            this.domainDirty = false;
            this.domainState = this.domain.model.nodes.length || this.domain.model.relations.length || this.domain.model.invariants.length ? 'ready' : 'empty';
            this.domainNotice = `Saved immutable revision ${this.domain.revision}.`;
            await this.refreshDomainVersions();
        } catch { this.domainState = 'error'; this.domainNotice = 'The DomainModel could not be saved.'; }
        this.update();
    }

    protected async compareDomainVersions(): Promise<void> {
        if (!this.compareFrom || !this.compareTo) { this.domainNotice = 'Choose two persisted revisions to compare.'; this.update(); return; }
        try {
            const base = `${this.backendUrl}/api/projects/${encodeURIComponent(this.selectedProject)}/workbench/domain-model/compare`;
            const response = await fetch(`${base}?from=${this.compareFrom}&to=${this.compareTo}`);
            if (!response.ok) throw new Error('Comparison failed');
            this.domainComparison = await response.json() as DomainComparison;
            this.domainNotice = `Compared revisions ${this.compareFrom} and ${this.compareTo}.`;
        } catch { this.domainState = 'error'; this.domainNotice = 'The revision comparison could not be loaded.'; }
        this.update();
    }

    protected async restoreDomainVersion(revision: number): Promise<void> {
        if (!this.domain || this.domainDirty) { this.domainNotice = 'Save or reload the current draft before restoring history.'; this.update(); return; }
        try {
            const response = await fetch(`${this.backendUrl}/api/projects/${encodeURIComponent(this.selectedProject)}/workbench/domain-model/versions/${revision}:restore`, {
                method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ expectedRevision: this.domain.revision })
            });
            if (response.status === 409) { this.domainState = 'conflict'; this.domainNotice = 'A newer revision exists. Reload before restoring.'; this.update(); return; }
            if (response.status === 401 || response.status === 403) { this.domainState = 'permission-denied'; this.domainNotice = 'You do not have permission to restore revisions.'; this.update(); return; }
            if (!response.ok) throw new Error('Restore failed');
            this.domain = await response.json() as DomainModelView;
            this.domainDraft = this.cloneDomainModel(this.domain.model);
            this.domainDirty = false;
            this.domainState = this.domain.model.nodes.length || this.domain.model.relations.length || this.domain.model.invariants.length ? 'ready' : 'empty';
            this.domainNotice = `Restored revision ${revision} as revision ${this.domain.revision}.`;
            await this.refreshDomainVersions();
        } catch { this.domainState = 'error'; this.domainNotice = 'The historical revision could not be restored.'; }
        this.update();
    }

    protected async decideDomainSuggestion(suggestion: DomainSuggestion, action: 'accepted' | 'edited' | 'rejected'): Promise<void> {
        if (!this.domain || !this.domainDraft) return;
        if (this.domainDirty && action !== 'edited') { this.domainNotice = 'Save or reload the draft before accepting or rejecting a suggestion.'; this.update(); return; }
        const editedNode = action === 'edited' && suggestion.targetType === 'node' ? this.domainDraft.nodes.find(node => node.id === suggestion.targetId) : undefined;
        const editedInvariant = action === 'edited' && suggestion.targetType === 'invariant' ? this.domainDraft.invariants.find(invariant => invariant.id === suggestion.targetId) : undefined;
        try {
            const response = await fetch(`${this.backendUrl}/api/projects/${encodeURIComponent(this.selectedProject)}/workbench/domain-model/suggestions/${encodeURIComponent(suggestion.id)}`, {
                method: 'POST', headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ action, expectedRevision: this.domain.revision, editedNode, editedInvariant })
            });
            if (response.status === 409) { this.domainState = 'conflict'; this.domainNotice = 'The suggestion was decided against another revision. Reload required.'; this.update(); return; }
            if (response.status === 401 || response.status === 403) { this.domainState = 'permission-denied'; this.domainNotice = 'You do not have permission to decide suggestions.'; this.update(); return; }
            if (!response.ok) {
                const error = await response.json() as { message?: string; diagnostics?: DomainDiagnostic[] };
                this.domainNotice = error.diagnostics?.map(item => item.message).join(' · ') || error.message || 'Suggestion decision failed.';
                this.update(); return;
            }
            this.domain = await response.json() as DomainModelView;
            this.domainDraft = this.cloneDomainModel(this.domain.model);
            this.domainDirty = false;
            this.domainState = this.domain.model.nodes.length || this.domain.model.relations.length || this.domain.model.invariants.length ? 'ready' : 'empty';
            this.domainNotice = `Suggestion ${suggestion.id} recorded as ${action}.`;
            await this.refreshDomainVersions();
        } catch { this.domainState = 'error'; this.domainNotice = 'The suggestion decision could not be recorded.'; }
        this.update();
    }

    protected navigateToSource(sourceRef: string): void {
        const fileRef = sourceRef.split('#')[0].replace(/:\d+$/, '');
        const file = this.sourceExplorer?.files.find(candidate => candidate.id === fileRef || candidate.path === fileRef || candidate.name === fileRef);
        this.activeArea = 'project';
        if (file) this.selectedSourceFileId = file.id;
        this.symbolSearch = sourceRef.includes('#') ? sourceRef.slice(sourceRef.indexOf('#') + 1) : '';
        this.persistShellState();
        void this.persistContext();
        this.update();
    }

    protected relatedDomainNodes(file: SourceFile): DomainNode[] {
        return this.domain?.model.nodes.filter(node => node.evidence.some(evidence => {
            const ref = evidence.sourceRef.split('#')[0].replace(/:\d+$/, '');
            return ref === file.id || ref === file.path || ref === file.name;
        })) ?? [];
    }

    protected validateDomainDraft(model: DomainModel): DomainDiagnostic[] {
        const diagnostics: DomainDiagnostic[] = [];
        const add = (severity: string, code: string, targetId: string, message: string): void => {
            diagnostics.push({ severity, code, targetId, message });
        };
        if (model.schemaVersion !== '1') add('error', 'INVALID_SCHEMA', model.projectId, 'Only neutral DomainModel schema 1 is supported.');
        const duplicateIds = (items: Array<{ id: string }>, label: string): void => {
            const seen = new Set<string>();
            items.forEach(item => {
                if (!item.id.trim()) add('error', 'BLANK_ID', label, `${label} id is required.`);
                else if (seen.has(item.id)) add('error', 'DUPLICATE_ID', item.id, `Duplicate ${label} id.`);
                seen.add(item.id);
            });
        };
        duplicateIds(model.nodes, 'node');
        duplicateIds(model.relations, 'relation');
        duplicateIds(model.invariants, 'invariant');
        const nodeIds = new Set(model.nodes.map(node => node.id));
        model.nodes.forEach(node => {
            if (!node.name.trim()) add('error', 'BLANK_NAME', node.id, 'Domain element name is required.');
            if (!DOMAIN_KINDS.includes(node.kind as typeof DOMAIN_KINDS[number])) add('error', 'INVALID_KIND', node.id, 'Unsupported domain element kind.');
            if (!node.evidence.length) add('warning', 'MISSING_EVIDENCE', node.id, 'Domain element has no COBOL source evidence.');
            if (!Number.isFinite(node.confidence) || node.confidence < 0 || node.confidence > 1) add('error', 'INVALID_CONFIDENCE', node.id, 'Confidence must be between 0 and 1.');
            else if (node.confidence < 0.5) add('warning', 'LOW_CONFIDENCE', node.id, 'Confidence is below 50%.');
            const propertyNames = new Set<string>();
            node.properties.forEach(property => {
                if (!property.name.trim() || !property.type.trim()) add('error', 'INVALID_PROPERTY', node.id, 'Property name and type are required.');
                if (propertyNames.has(property.name)) add('error', 'DUPLICATE_PROPERTY', node.id, `Duplicate property ${property.name}.`);
                propertyNames.add(property.name);
            });
        });
        model.relations.forEach(relation => {
            if (!nodeIds.has(relation.fromId) || !nodeIds.has(relation.toId)) add('error', 'BROKEN_REFERENCE', relation.id, 'Relation references an unknown domain element.');
            if (!RELATION_KINDS.includes(relation.kind as typeof RELATION_KINDS[number])) add('error', 'INVALID_RELATION_KIND', relation.id, 'Unsupported relation kind.');
            if (!CARDINALITIES.includes(relation.sourceCardinality as typeof CARDINALITIES[number]) || !CARDINALITIES.includes(relation.targetCardinality as typeof CARDINALITIES[number])) add('error', 'INVALID_CARDINALITY', relation.id, 'Unsupported relation cardinality.');
        });
        model.invariants.forEach(invariant => {
            if (!nodeIds.has(invariant.subjectId)) add('error', 'BROKEN_REFERENCE', invariant.id, 'Invariant references an unknown domain element.');
            if (!invariant.expression.trim()) add('error', 'BLANK_INVARIANT', invariant.id, 'Business rule is required.');
            if (!invariant.evidence.length) add('warning', 'MISSING_EVIDENCE', invariant.id, 'Business invariant has no COBOL source evidence.');
        });
        return diagnostics;
    }

    protected navigateToDomainFromSource(file: SourceFile, symbol?: SourceSymbol): void {
        const references = [file.id, file.path, file.name];
        const selected = this.domain?.model.nodes.find(node => node.evidence.some(evidence => {
            const [fileRef, symbolRef] = evidence.sourceRef.split('#');
            return references.includes(fileRef.replace(/:\d+$/, '')) && (!symbol || !symbolRef || symbolRef === symbol.id || symbolRef === symbol.name);
        }));
        this.activeArea = 'domain';
        this.domainFilter = symbol?.name ?? file.name;
        if (selected) {
            this.selectedDomainType = 'node';
            this.selectedDomainId = selected.id;
            this.domainFilter = '';
        }
        this.domainNotice = selected ? `Opened ${selected.name} from ${symbol?.name ?? file.name}.` : `No DomainModel element references ${symbol?.name ?? file.name}.`;
        this.persistShellState();
        void this.persistContext();
        this.update();
    }

    protected openDomainNode(node: DomainNode, context: string): void {
        this.activeArea = 'domain';
        this.selectedDomainType = 'node';
        this.selectedDomainId = node.id;
        this.domainFilter = '';
        this.domainNotice = `Opened ${node.name} from ${context}.`;
        this.persistShellState();
        void this.persistContext();
        this.update();
    }

    protected async readEnv(name: string): Promise<string | undefined> {
        const variable: EnvVariable | undefined = await this.envVariables.getValue(name);
        return variable?.value?.trim() || undefined;
    }

    protected restoreShellState(): void {
        try {
            const storedArea = window.localStorage.getItem(ACTIVE_AREA_KEY) as RenovatioAreaId | null;
            const storedProject = window.localStorage.getItem(SELECTED_PROJECT_KEY);
            if (AREAS.some(area => area.id === storedArea)) {
                this.activeArea = storedArea as RenovatioAreaId;
            }
            if (storedProject) {
                this.selectedProject = storedProject;
            }
        } catch {
            this.shellState = 'error';
        }
    }

    protected persistShellState(): void {
        try {
            window.localStorage.setItem(ACTIVE_AREA_KEY, this.activeArea);
            window.localStorage.setItem(SELECTED_PROJECT_KEY, this.selectedProject);
        } catch {
            this.shellState = 'permission-denied';
        }
    }

    protected async selectAsset(asset: ProjectAssetItem, persist = true): Promise<void> {
        this.selectedAsset = asset.name;
        this.selectedAssetId = asset.id;
        this.selectedAssetWritable = asset.writable;
        this.assetContentState = 'loading';
        if (persist) void this.persistContext();
        this.update();
        if (!this.projects.length) { this.assetContent = 'Live project adapter is unavailable; this fixture is read-only.'; this.assetContentState = 'ready'; this.update(); return; }
        try {
            const response = await fetch(`${this.backendUrl}/api/projects/${encodeURIComponent(this.selectedProject)}/workbench/assets/${this.selectedAssetId.split('/').map(encodeURIComponent).join('/')}`);
            if (!response.ok) throw new Error(`Asset adapter returned ${response.status}`);
            this.assetContent = await response.text();
            this.assetContentState = 'ready';
        } catch { this.assetContentState = 'error'; }
        this.update();
    }

    protected updateAssetContent(event: React.ChangeEvent<HTMLTextAreaElement>): void { this.assetContent = event.target.value; this.update(); }

    protected async saveAsset(): Promise<void> {
        if (!this.selectedAssetWritable || this.assetContentState === 'saving') return;
        this.assetContentState = 'saving'; this.update();
        try {
            const response = await fetch(`${this.backendUrl}/api/projects/${encodeURIComponent(this.selectedProject)}/workbench/assets/${this.selectedAssetId.split('/').map(encodeURIComponent).join('/')}`, { method: 'PUT', headers: { 'Content-Type': 'text/plain' }, body: this.assetContent });
            if (!response.ok) throw new Error(`Asset adapter returned ${response.status}`);
            this.assetContentState = 'ready';
        } catch { this.assetContentState = 'error'; }
        this.update();
    }

    protected selectProject(project: string): void {
        this.selectedProject = project;
        this.selectedSourceFileId = null;
        this.domain = undefined;
        this.domainDraft = undefined;
        this.domainDirty = false;
        this.architecture = undefined;
        this.architectureDraft = undefined;
        this.architectureDirty = false;
        this.selectedDomainId = null;
        this.persistShellState();
        void this.loadAssets().then(() => this.loadContext()).then(() => this.update()).catch(() => { this.shellState = 'error'; this.update(); });
        void this.loadSourceExplorer();
        void this.loadDomainModel();
        void this.loadArchitecture();
        this.update();
    }

    protected override onActivateRequest(message: Message): void {
        super.onActivateRequest(message);
        this.node.focus();
    }

    protected renderProjectExplorer(): React.ReactNode {
        return <aside className='renovatio-project-explorer' aria-labelledby='renovatio-project-heading'>
            <div className='renovatio-panel-heading'>
                <span className='renovatio-coordinate'>PROJECT.01</span>
                <h2 id='renovatio-project-heading'>Project explorer</h2>
            </div>
            <div className='renovatio-project-switcher' role='group' aria-label='Selected project'>
                {(this.projects.length ? this.projects : [{ id: 'payroll-modernization', name: 'PAYROLL' }, { id: 'ledger-modernization', name: 'LEDGER' }]).map(project => <button type='button' key={project.id} aria-pressed={this.selectedProject === project.id} onClick={() => this.selectProject(project.id)}>{project.name}</button>)}
            </div>
            <p className='renovatio-selection'>ACTIVE / {this.selectedProject}</p>
            <nav aria-label='Project assets'>
                {this.projectAssets.map(assetGroup => <section className='renovatio-asset-group' key={assetGroup.group} aria-label={assetGroup.group}>
                    <h3>{assetGroup.group}</h3>
                    <ul>{assetGroup.items.map(asset => <li key={asset.id}><button type='button'
                        aria-current={this.selectedAssetId === asset.id ? 'page' : undefined}
                        onClick={() => void this.selectAsset(asset)}>{asset.name}</button></li>)}</ul>
                </section>)}
            </nav>
        </aside>;
    }

    protected renderSourceExplorer(): React.ReactNode {
        const file = this.selectedSourceFile;
        const matches = this.symbolMatches();
        return <section className='renovatio-source-explorer' aria-label='Source explorer'>
            <div className='renovatio-panel-heading'>
                <span className='renovatio-coordinate'>PROJECT.02</span>
                <h3>Source explorer · {this.sourceExplorerState.toUpperCase()}</h3>
            </div>
            {this.sourceExplorerState === 'permission-denied' && <p>Source explorer is not authorized for this role; project navigation remains available.</p>}
            {this.sourceExplorerState === 'error' && <p>Source explorer data is unavailable; project navigation remains available.</p>}
            {this.sourceExplorerState === 'empty' && <p>No COBOL, copybook or JCL assets were found in this project workspace.</p>}
            {this.sourceExplorerState === 'ready' && this.sourceExplorer && <div className='renovatio-source-grid'>
                <nav aria-label='Source files'>
                    <ul>{this.sourceExplorer.files.map(entry => <li key={entry.id}>
                        <button type='button' aria-current={this.selectedSourceFileId === entry.id ? 'true' : undefined}
                            onClick={() => this.selectSourceFile(entry.id)}>{entry.name}</button>
                        <span className='renovatio-file-meta'>{entry.kind} · {entry.encoding} · {entry.analysisStatus} · {entry.hash.slice(0, 14)}…</span>
                    </li>)}</ul>
                </nav>
                <div className='renovatio-source-detail'>
                    <label>Symbol search
                        <input type='search' value={this.symbolSearch} onChange={event => this.updateSymbolSearch(event)} aria-label='Search symbols and references' />
                    </label>
                    {this.symbolSearch.trim()
                        ? <ol className='renovatio-symbol-results' aria-label='Search results'>
                            {matches.length
                                ? matches.map(match => <li key={`${match.file.id}:${match.symbol.id}`}>{match.symbol.kind} · {match.symbol.name} — {match.file.name} {match.symbol.line}:{match.symbol.column}</li>)
                                : <li>No symbol or reference matches “{this.symbolSearch}”.</li>}
                        </ol>
                        : file && <>
                            <ol className='renovatio-outline' aria-label={`Outline of ${file.name}`}>
                                {file.symbols.length
                                ? file.symbols.map(symbol => <li key={symbol.id} aria-current={undefined}>
                                        <button type='button' onClick={() => this.navigateToDomainFromSource(file, symbol)} aria-label={`Open DomainModel references for ${symbol.name}`}>
                                            <span className='renovatio-symbol-kind'>{symbol.kind}</span> {symbol.name}
                                            <span className='renovatio-symbol-pos'>{symbol.line}:{symbol.column}</span>
                                            <span className='renovatio-symbol-ir'>{symbol.irCoordinate}</span>
                                        </button>
                                    </li>)
                                    : <li>This file is shown as “{file.analysisStatus}”; no navigable symbols were extracted.</li>}
                            </ol>
                            <section className='renovatio-problems' aria-label='Problems'>
                                <h4>Problems</h4>
                                {file.diagnostics.length
                                    ? <ul>{file.diagnostics.map((diagnostic, index) => <li key={index}>{diagnostic.severity.toUpperCase()} · line {diagnostic.line} · {diagnostic.message}</li>)}</ul>
                                    : <p>No parse diagnostics for {file.name}.</p>}
                            </section>
                            <section className='renovatio-domain-links' aria-label='Related DomainModel elements'>
                                <h4>DomainModel references</h4>
                                {this.relatedDomainNodes(file).length
                                    ? <ul>{this.relatedDomainNodes(file).map(node => <li key={node.id}><button type='button' onClick={() => this.openDomainNode(node, file.name)}>{node.kind} · {node.name}</button></li>)}</ul>
                                    : <p>No domain element references {file.name}.</p>}
                            </section>
                        </>}
                    {this.sourceExplorer.datasets.length > 0 && <p className='renovatio-datasets'>Datasets: {this.sourceExplorer.datasets.map(dataset => dataset.name).join(', ')}</p>}
                </div>
            </div>}
        </section>;
    }

    protected renderDomainEditor(): React.ReactNode {
        const draft = this.domainDraft;
        const draftDiagnostics = draft ? this.validateDomainDraft(draft) : [];
        const selectedNode = this.selectedDomainType === 'node' ? draft?.nodes.find(node => node.id === this.selectedDomainId) : undefined;
        const selectedRelation = this.selectedDomainType === 'relation' ? draft?.relations.find(relation => relation.id === this.selectedDomainId) : undefined;
        const selectedInvariant = this.selectedDomainType === 'invariant' ? draft?.invariants.find(invariant => invariant.id === this.selectedDomainId) : undefined;
        const selectedEvidence = selectedNode?.evidence ?? selectedInvariant?.evidence ?? [];
        const term = this.domainFilter.trim().toLowerCase();
        const matches = (value: string): boolean => !term || value.toLowerCase().includes(term);
        const nodes = draft?.nodes.filter(node => matches(`${node.id} ${node.kind} ${node.name}`)) ?? [];
        const relations = draft?.relations.filter(relation => matches(`${relation.id} ${relation.kind} ${relation.fromId} ${relation.toId}`)) ?? [];
        const invariants = draft?.invariants.filter(invariant => matches(`${invariant.id} ${invariant.subjectId} ${invariant.expression}`)) ?? [];
        const controlDisabled = this.domainState === 'loading' || this.domainState === 'saving' || !draft || draftDiagnostics.some(diagnostic => diagnostic.severity === 'error');
        return <section className='renovatio-domain-editor' aria-label='Business DomainModel editor'>
            <header className='renovatio-domain-toolbar'>
                <div>
                    <span className='renovatio-coordinate'>DOMAIN.MODEL / SCHEMA {draft?.schemaVersion ?? '1'}</span>
                    <strong>REV {this.domain?.revision ?? 0} · {this.domain?.canonicalHash?.slice(0, 22) ?? 'UNSAVED'}</strong>
                </div>
                <div className='renovatio-domain-actions'>
                    <button type='button' onClick={() => void this.loadDomainModel()} disabled={this.domainState === 'loading' || this.domainState === 'saving'}>Reload</button>
                    <button type='button' className='is-primary' onClick={() => void this.saveDomainModel()} disabled={controlDisabled || !this.domainDirty}>Save revision</button>
                </div>
            </header>
            <p className={`renovatio-domain-state state-${this.domainState}`} role='status' aria-live='polite'>
                {this.domainNotice || `DomainModel is ${this.domainState}. ${draftDiagnostics.length} diagnostics.`}
            </p>
            {this.domainState === 'permission-denied' && <p>You may inspect project navigation, but this role cannot load or modify the DomainModel.</p>}
            {this.domainState === 'error' && !draft && <p>The DomainModel adapter is unavailable. Reload to retry.</p>}
            {this.domainState === 'conflict' && <p>Reload the latest revision, then reapply the intended correction. No newer data was overwritten.</p>}
            {draft && <div className='renovatio-domain-grid'>
                <aside className='renovatio-domain-catalog' aria-label='Domain elements'>
                    <label>Filter model
                        <input type='search' value={this.domainFilter} onChange={event => { this.domainFilter = event.target.value; this.update(); }} />
                    </label>
                    <div className='renovatio-domain-add' role='group' aria-label='Add model element'>
                        <button type='button' onClick={() => this.addDomainItem('node')}>+ Element</button>
                        <button type='button' onClick={() => this.addDomainItem('relation')}>+ Relation</button>
                        <button type='button' onClick={() => this.addDomainItem('invariant')}>+ Invariant</button>
                    </div>
                    <section><h3>Elements <span>{nodes.length}</span></h3><ul>
                        {nodes.map(node => <li key={node.id}><button type='button' aria-current={this.selectedDomainType === 'node' && this.selectedDomainId === node.id ? 'true' : undefined} onClick={() => this.selectDomainItem('node', node.id)}><span>{node.kind}</span>{node.name}<small>{node.id}</small></button></li>)}
                    </ul></section>
                    <section><h3>Relations <span>{relations.length}</span></h3><ul>
                        {relations.map(relation => <li key={relation.id}><button type='button' aria-current={this.selectedDomainType === 'relation' && this.selectedDomainId === relation.id ? 'true' : undefined} onClick={() => this.selectDomainItem('relation', relation.id)}><span>{relation.kind}</span>{relation.fromId} → {relation.toId}<small>{relation.id}</small></button></li>)}
                    </ul></section>
                    <section><h3>Invariants <span>{invariants.length}</span></h3><ul>
                        {invariants.map(invariant => <li key={invariant.id}><button type='button' aria-current={this.selectedDomainType === 'invariant' && this.selectedDomainId === invariant.id ? 'true' : undefined} onClick={() => this.selectDomainItem('invariant', invariant.id)}><span>RULE</span>{invariant.expression}<small>{invariant.id}</small></button></li>)}
                    </ul></section>
                    {!nodes.length && !relations.length && !invariants.length && <p className='renovatio-domain-empty'>No matching domain elements.</p>}
                </aside>
                <section className='renovatio-domain-form' aria-label='Selected domain element editor'>
                    <div className='renovatio-domain-section-heading'><div><span className='renovatio-coordinate'>STRUCTURED EDITOR</span><h3>{this.selectedDomainId ?? 'No selection'}</h3></div>
                        {this.selectedDomainId && <button type='button' className='is-danger' onClick={() => this.removeSelectedDomainItem()}>Remove</button>}
                    </div>
                    {selectedNode && <>
                        <div className='renovatio-domain-fields'>
                            <label>Stable id<input value={selectedNode.id} readOnly aria-readonly='true' /></label>
                            <label>Kind<select value={selectedNode.kind} onChange={event => this.updateDomainNode(selectedNode.id, { kind: event.target.value })}>{DOMAIN_KINDS.map(kind => <option key={kind}>{kind}</option>)}</select></label>
                            <label className='is-wide'>Name<input value={selectedNode.name} onChange={event => this.updateDomainNode(selectedNode.id, { name: event.target.value })} /></label>
                            <label>Origin<input value={selectedNode.origin} readOnly aria-readonly='true' /></label>
                            <label>Confidence<input type='number' min='0' max='1' step='0.01' value={selectedNode.confidence} onChange={event => this.updateDomainNode(selectedNode.id, { confidence: Number(event.target.value) })} /></label>
                        </div>
                        <div className='renovatio-domain-properties'><div><h4>Properties</h4><button type='button' onClick={() => this.addDomainProperty(selectedNode)}>+ Property</button></div>
                            {selectedNode.properties.length ? <ol>{selectedNode.properties.map((property, index) => <li key={`${property.name}:${index}`}>
                                <label>Name<input value={property.name} onChange={event => this.updateDomainProperty(selectedNode, index, { name: event.target.value })} /></label>
                                <label>Type<input value={property.type} onChange={event => this.updateDomainProperty(selectedNode, index, { type: event.target.value })} /></label>
                                <label className='renovatio-checkbox'><input type='checkbox' checked={property.required} onChange={event => this.updateDomainProperty(selectedNode, index, { required: event.target.checked })} />Required</label>
                                <button type='button' aria-label={`Remove property ${property.name}`} onClick={() => this.updateDomainNode(selectedNode.id, { properties: selectedNode.properties.filter((_, candidate) => candidate !== index) })}>×</button>
                                <div className='renovatio-property-evidence'>Evidence · {property.evidence.length || 'none'}{property.evidence.map(evidence => <button type='button' key={evidence.sourceRef} onClick={() => this.navigateToSource(evidence.sourceRef)}>{evidence.sourceRef}</button>)}</div>
                            </li>)}</ol> : <p>No structured properties.</p>}
                        </div>
                    </>}
                    {selectedRelation && <div className='renovatio-domain-fields'>
                        <label>Stable id<input value={selectedRelation.id} readOnly aria-readonly='true' /></label>
                        <label>Kind<select value={selectedRelation.kind} onChange={event => this.updateDomainRelation(selectedRelation.id, { kind: event.target.value })}>{RELATION_KINDS.map(kind => <option key={kind}>{kind}</option>)}</select></label>
                        <label>From<select value={selectedRelation.fromId} onChange={event => this.updateDomainRelation(selectedRelation.id, { fromId: event.target.value })}>{draft.nodes.map(node => <option key={node.id} value={node.id}>{node.name} · {node.id}</option>)}</select></label>
                        <label>To<select value={selectedRelation.toId} onChange={event => this.updateDomainRelation(selectedRelation.id, { toId: event.target.value })}>{draft.nodes.map(node => <option key={node.id} value={node.id}>{node.name} · {node.id}</option>)}</select></label>
                        <label>Source cardinality<select value={selectedRelation.sourceCardinality} onChange={event => this.updateDomainRelation(selectedRelation.id, { sourceCardinality: event.target.value })}>{CARDINALITIES.map(value => <option key={value}>{value}</option>)}</select></label>
                        <label>Target cardinality<select value={selectedRelation.targetCardinality} onChange={event => this.updateDomainRelation(selectedRelation.id, { targetCardinality: event.target.value })}>{CARDINALITIES.map(value => <option key={value}>{value}</option>)}</select></label>
                    </div>}
                    {selectedInvariant && <div className='renovatio-domain-fields'>
                        <label>Stable id<input value={selectedInvariant.id} readOnly aria-readonly='true' /></label>
                        <label>Subject<select value={selectedInvariant.subjectId} onChange={event => this.updateDomainInvariant(selectedInvariant.id, { subjectId: event.target.value })}>{draft.nodes.map(node => <option key={node.id} value={node.id}>{node.name} · {node.id}</option>)}</select></label>
                        <label className='is-wide'>Business rule<textarea value={selectedInvariant.expression} onChange={event => this.updateDomainInvariant(selectedInvariant.id, { expression: event.target.value })} /></label>
                        <label>Origin<input value={selectedInvariant.origin} readOnly aria-readonly='true' /></label>
                        <label>Confidence<input type='number' min='0' max='1' step='0.01' value={selectedInvariant.confidence} onChange={event => this.updateDomainInvariant(selectedInvariant.id, { confidence: Number(event.target.value) })} /></label>
                    </div>}
                    {!selectedNode && !selectedRelation && !selectedInvariant && <div className='renovatio-domain-placeholder'><strong>Select or create an element.</strong><p>Stable ids and evidence references remain immutable; names, kinds, properties and cardinalities are editable.</p></div>}
                </section>
                <aside className='renovatio-domain-inspector' aria-label='Domain provenance and history'>
                    <section><span className='renovatio-coordinate'>PROVENANCE</span><h3>Source evidence</h3>
                        {selectedEvidence.length ? <ul>{selectedEvidence.map((evidence, index) => {
                            const ref = evidence.sourceRef.split('#')[0].replace(/:\d+$/, '');
                            const source = this.sourceExplorer?.files.find(file => file.id === ref || file.path === ref || file.name === ref);
                            return <li key={`${evidence.sourceRef}:${index}`}><button type='button' onClick={() => this.navigateToSource(evidence.sourceRef)}>{evidence.sourceRef}</button><span>{evidence.provenance}</span><p>{evidence.rationale || 'No rationale recorded.'}</p>{source && <><span>{source.kind} · {source.encoding}</span><small>sha256 · {source.hash}</small></>}</li>;
                        })}</ul> : <p>No source evidence recorded for this selection.</p>}
                    </section>
                    <section><span className='renovatio-coordinate'>VALIDATION</span><h3>Diagnostics</h3>
                        {draftDiagnostics.length ? <ul>{draftDiagnostics.map((diagnostic, index) => <li key={`${diagnostic.code}:${diagnostic.targetId}:${index}`} className={`severity-${diagnostic.severity}`}><strong>{diagnostic.code}</strong><span>{diagnostic.targetId}</span><p>{diagnostic.message}</p></li>)}</ul> : <p>No draft diagnostics.</p>}
                    </section>
                    <section><span className='renovatio-coordinate'>LLM REVIEW</span><h3>Suggestion queue</h3>
                        {this.domain?.suggestions.length ? <ul>{this.domain.suggestions.map(suggestion => <li key={suggestion.id}><button type='button' onClick={() => this.selectDomainItem(suggestion.targetType as DomainItemType, suggestion.targetId)}>{suggestion.name}</button><span>{suggestion.targetType} · {suggestion.status}</span>{suggestion.status === 'pending' && <div>
                            <button type='button' disabled={this.domainDirty} onClick={() => void this.decideDomainSuggestion(suggestion, 'accepted')}>Accept</button>
                            <button type='button' disabled={!this.domainDirty || this.selectedDomainId !== suggestion.targetId} onClick={() => void this.decideDomainSuggestion(suggestion, 'edited')}>Accept edits</button>
                            <button type='button' disabled={this.domainDirty} onClick={() => void this.decideDomainSuggestion(suggestion, 'rejected')}>Reject</button>
                        </div>}</li>)}</ul> : <p>No LLM-origin suggestions await review.</p>}
                    </section>
                    <section><span className='renovatio-coordinate'>VERSIONS</span><h3>Immutable history</h3>
                        {this.domainVersions.length ? <><ul>{this.domainVersions.map(version => <li key={version.revision}><strong>REV {version.revision}</strong><span>{version.savedAt}</span><small>{version.canonicalHash.slice(0, 22)}</small><button type='button' onClick={() => void this.restoreDomainVersion(version.revision)} disabled={version.revision === this.domain?.revision}>Restore</button></li>)}</ul>
                            <div className='renovatio-domain-compare'><label>From<select value={this.compareFrom} onChange={event => { this.compareFrom = Number(event.target.value); this.update(); }}>{this.domainVersions.map(version => <option key={version.revision} value={version.revision}>REV {version.revision}</option>)}</select></label><label>To<select value={this.compareTo} onChange={event => { this.compareTo = Number(event.target.value); this.update(); }}>{this.domainVersions.map(version => <option key={version.revision} value={version.revision}>REV {version.revision}</option>)}</select></label><button type='button' onClick={() => void this.compareDomainVersions()}>Compare</button></div>
                            {this.domainComparison && <p className='renovatio-domain-diff'>Added {this.domainComparison.added.length} · Removed {this.domainComparison.removed.length} · Changed {this.domainComparison.changed.length}</p>}</> : <p>No persisted revisions yet.</p>}
                    </section>
                </aside>
            </div>}
        </section>;
    }

    protected renderArchitectureCanvas(): React.ReactNode {
        const profile = this.architectureDraft;
        const view = this.architecture;
        const layers = profile ? Array.from(new Set([...Object.keys(profile.packageRoots), ...Object.keys(profile.suffixes),
            ...profile.dependencyRules.flatMap(rule => [rule.fromLayer, rule.toLayer]), ...MVC_LAYERS])) : MVC_LAYERS;
        const layerNodes = view?.canvas.filter(node => node.layer === this.selectedArchitectureLayer) ?? [];
        const rules = profile?.dependencyRules ?? [];
        return <section className='renovatio-architecture-canvas' aria-label='Architecture Canvas editor' aria-live='polite'>
            <div className='renovatio-architecture-toolbar'>
                <div><span className='renovatio-coordinate'>ARCH.CANVAS</span><strong>REV {view?.revision ?? 0} · {view?.canonicalHash?.slice(0, 22) ?? 'sha256:pending'}</strong></div>
                <div className='renovatio-architecture-actions'>
                    <button type='button' onClick={() => void this.loadArchitecture()}>Reload</button>
                    <button type='button' className='is-primary' disabled={!this.architectureDirty || this.architectureState === 'saving'} onClick={() => void this.saveArchitectureProfile()}>Save profile</button>
                </div>
            </div>
            <p className={`renovatio-domain-state state-${this.architectureState}`}>{this.architectureNotice || `Architecture canvas ${this.architectureState}.`}</p>
            {profile && view && <div className='renovatio-architecture-grid'>
                <aside className='renovatio-architecture-palette' aria-label='Architecture style and layers'>
                    <section><h3>Style</h3><div className='renovatio-segmented' role='group' aria-label='Architecture style'>
                        {(['LAYERED_MVC', 'HEXAGONAL', 'CLEAN', 'LAYERED', 'TRANSACTION_SCRIPT'] as ArchitectureStyle[]).map(style =>
                            <button type='button' key={style} aria-pressed={profile.style === style} onClick={() => this.updateArchitectureProfile({ style })}>{style.replace('_', ' ')}</button>)}
                    </div></section>
                    <section><h3>Target</h3><label>Module grouping<select value={profile.moduleGrouping} onChange={event => this.updateArchitectureProfile({ moduleGrouping: event.target.value as ModuleGrouping })}>
                        {(['BY_PROGRAM', 'BY_DOMAIN', 'SINGLE_MODULE'] as ModuleGrouping[]).map(value => <option key={value}>{value}</option>)}
                    </select></label><label>Framework<select value={profile.framework} onChange={event => this.updateArchitectureProfile({ framework: event.target.value })}>
                        {['SPRING_BOOT', 'NONE'].map(value => <option key={value}>{value}</option>)}
                    </select></label><label>Persistence<select value={profile.persistence} onChange={event => this.updateArchitectureProfile({ persistence: event.target.value })}>
                        {['IN_MEMORY', 'JPA', 'SPRING_DATA_JDBC', 'PRISMA'].map(value => <option key={value}>{value}</option>)}
                    </select></label></section>
                    <section><h3>Layers <span>{layers.length}</span></h3><ul>
                        {layers.map(layer => <li key={layer}><button type='button' aria-current={this.selectedArchitectureLayer === layer ? 'true' : undefined} onClick={() => { this.selectedArchitectureLayer = layer; this.update(); }}><span>{layer.toUpperCase()}</span>{profile.packageRoots[layer] ?? profile.packageRoots.base}<small>{layerNodes.length && layer === this.selectedArchitectureLayer ? `${layerNodes.length} nodes` : profile.suffixes[layer] ?? 'no suffix'}</small></button></li>)}
                    </ul></section>
                </aside>
                <section className='renovatio-architecture-stage' aria-label='Editable architecture canvas'>
                    <div className='renovatio-architecture-rail'>
                        {layers.map(layer => <article key={layer} className={this.selectedArchitectureLayer === layer ? 'is-selected' : undefined}>
                            <button type='button' onClick={() => { this.selectedArchitectureLayer = layer; this.update(); }} aria-selected={this.selectedArchitectureLayer === layer}><span>{layer}</span><strong>{profile.suffixes[layer] ?? 'Component'}</strong></button>
                            {(view.canvas.filter(node => node.layer === layer).slice(0, 5)).map(node => <div key={node.id} className='renovatio-architecture-node'><span>{node.kind}</span>{node.className}<small>{node.packageName}</small></div>)}
                        </article>)}
                    </div>
                    <div className='renovatio-architecture-manifest' aria-label='Artifact and package manifest preview'><h3>Manifest preview</h3>
                        {view.manifest.length ? <ol>{view.manifest.slice(0, 12).map(entry => <li key={entry.path}><code>{entry.path}</code><span>{entry.role} · {entry.layer}</span></li>)}</ol> : <p>No artifact manifest is available for this profile.</p>}
                    </div>
                </section>
                <aside className='renovatio-architecture-inspector' aria-label='Architecture profile inspector'>
                    <section><span className='renovatio-coordinate'>NAMING</span><h3>{this.selectedArchitectureLayer}</h3>
                        <label>Package<input value={profile.packageRoots[this.selectedArchitectureLayer] ?? ''} onChange={event => this.updateArchitectureMap('packageRoots', this.selectedArchitectureLayer, event.target.value)} /></label>
                        <label>Suffix<input value={profile.suffixes[this.selectedArchitectureLayer] ?? ''} onChange={event => this.updateArchitectureMap('suffixes', this.selectedArchitectureLayer, event.target.value)} /></label>
                        <label>Class override key<input value={profile.classNames[this.selectedArchitectureLayer] ?? ''} onChange={event => this.updateArchitectureMap('classNames', this.selectedArchitectureLayer, event.target.value)} /></label>
                    </section>
                    <section><span className='renovatio-coordinate'>DEPENDENCIES</span><h3>Rules</h3>
                        <ul>{rules.map((rule, index) => <li key={`${rule.fromLayer}:${rule.toLayer}:${index}`} className={rule.allowed ? 'severity-warning' : 'severity-error'}>
                            <label>From<input value={rule.fromLayer} onChange={event => this.updateArchitectureRule(index, { fromLayer: event.target.value })} /></label>
                            <label>To<input value={rule.toLayer} onChange={event => this.updateArchitectureRule(index, { toLayer: event.target.value })} /></label>
                            <label className='renovatio-checkbox'><input type='checkbox' checked={rule.allowed} onChange={event => this.updateArchitectureRule(index, { allowed: event.target.checked })} />Allowed</label>
                            <label>Reason<input value={rule.reason} onChange={event => this.updateArchitectureRule(index, { reason: event.target.value })} /></label>
                        </li>)}</ul>
                        {view.dependencyDiagnostics.length ? <p className='renovatio-domain-diff'>{view.dependencyDiagnostics.length} illegal dependencies visible before generate.</p> : <p>No illegal dependency diagnostics.</p>}
                    </section>
                    <section><span className='renovatio-coordinate'>VERSIONS</span><h3>Profile history</h3>
                        {this.architectureVersions.length ? <><ul>{this.architectureVersions.map(version => <li key={version.revision}><strong>REV {version.revision}</strong><span>{version.style}</span><small>{version.canonicalHash.slice(0, 22)}</small><button type='button' onClick={() => void this.restoreArchitectureVersion(version.revision)} disabled={version.revision === view.revision}>Restore</button></li>)}</ul>
                            <div className='renovatio-domain-compare'><label>From<select value={this.architectureCompareFrom} onChange={event => { this.architectureCompareFrom = Number(event.target.value); this.update(); }}>{this.architectureVersions.map(version => <option key={version.revision} value={version.revision}>REV {version.revision}</option>)}</select></label><label>To<select value={this.architectureCompareTo} onChange={event => { this.architectureCompareTo = Number(event.target.value); this.update(); }}>{this.architectureVersions.map(version => <option key={version.revision} value={version.revision}>REV {version.revision}</option>)}</select></label><button type='button' onClick={() => void this.compareArchitectureVersions()}>Compare</button></div>
                            {this.architectureComparison && <p className='renovatio-domain-diff'>Added {this.architectureComparison.added.length} · Removed {this.architectureComparison.removed.length} · Changed {this.architectureComparison.changed.length}</p>}</> : <p>No persisted architecture revisions yet.</p>}
                    </section>
                </aside>
            </div>}
            {this.architectureState === 'empty' && <p>No architecture canvas is available for this project.</p>}
            {this.architectureState === 'permission-denied' && <p>You do not have permission to inspect this architecture profile.</p>}
            {this.architectureState === 'error' && <p>Architecture canvas is unavailable; project navigation remains available.</p>}
        </section>;
    }

    protected renderArea(): React.ReactNode {
        const area = AREAS.find(candidate => candidate.id === this.activeArea) ?? AREAS[0];
        return <section className='renovatio-area-content' aria-labelledby='renovatio-area-heading'>
            <span className='renovatio-coordinate'>{area.coordinate}</span>
            <h2 id='renovatio-area-heading'>{this.activeArea === 'project' ? 'Project navigation' : area.label}</h2>
            <p>{area.summary}</p>
            <article className='renovatio-asset-preview' aria-label='Selected shell context'>
                <span>{this.activeArea === 'project' ? 'SELECTED ASSET' : 'WORKBENCH AREA'}</span>
                <strong>{this.activeArea === 'project' ? this.selectedAsset : this.activeArea === 'domain' ? `DOMAINMODEL / REV ${this.domain?.revision ?? 0}` : this.activeArea === 'architecture' && this.architecture ? `ARCHITECTURE / REV ${this.architecture.revision}` : this.activeArea === 'equivalence' && this.equivalenceState === 'ready' ? 'EQUIVALENCE / INVENTORY READY' : `${area.label.toUpperCase()} / READY FOR ADAPTER`}</strong>
                <p>{this.activeArea === 'project'
                    ? 'Source content remains inside the configured workspace boundary.'
                    : this.activeArea === 'architecture' && this.architecture ? 'Editable target architecture profile, shadow manifest and dependency validation are loaded from the governed backend contract.'
                        : this.activeArea === 'equivalence' && this.equivalenceState === 'ready' ? 'Read-only inventory is loaded from the governed backend contract; it does not imply an equivalence verdict.'
                            : 'UI boundary is available; live data remains governed by the existing backend contract.'}</p>
            </article>
            {this.activeArea === 'project' && <section className='renovatio-asset-editor' aria-label='Selected asset content'>
                <div><span>ADAPTER CONTENT · {this.assetContentState.toUpperCase()}</span>{this.selectedAssetWritable && <button type='button' onClick={() => void this.saveAsset()} disabled={this.assetContentState === 'saving'}>Save development target</button>}</div>
                <textarea value={this.assetContent} readOnly={!this.selectedAssetWritable} onChange={event => this.updateAssetContent(event)} aria-label={`${this.selectedAsset} content`} spellCheck={false} />
                <p>{this.selectedAssetWritable ? 'Temporary development mode: writing a generated target is enabled. It is not production authorization.' : 'Legacy source and evidence are read-only.'}</p>
            </section>}
            {this.activeArea === 'project' && this.renderSourceExplorer()}
            {this.activeArea === 'domain' && this.renderDomainEditor()}
            {this.activeArea === 'analysis' && <section className='renovatio-asset-editor' aria-label='Analysis inventory'>
                <div><span>ANALYSIS ADAPTER · {this.analysisState.toUpperCase()}</span></div>
                {this.analysisState === 'ready' && <><p>{Object.entries(this.analysis?.inventory ?? {}).map(([category, count]) => `${category}: ${count}`).join(' · ')}</p>
                    <p>{this.analysis?.runs.length ? `Runs: ${this.analysis.runs.map(run => run.runId).join(', ')}` : 'No persisted runs for this project.'}</p></>}
                {this.analysisState === 'empty' && <p>No inventory or persisted runs are available.</p>}
                {this.analysisState === 'error' && <p>Analysis data is unavailable; project navigation remains available.</p>}
            </section>}
            {this.activeArea === 'architecture' && this.renderArchitectureCanvas()}
            {this.activeArea === 'ai' && <section className='renovatio-asset-editor' aria-label='Governed AI suggestions'>
                <div><span>GOVERNED SUGGESTIONS · {this.aiState.toUpperCase()}</span></div>
                {this.aiState === 'ready' && <p>{this.ai?.items.map(item => `${item.category} · ${item.source} · ${item.status} · ${(item.confidence * 100).toFixed(0)}%`).join(' | ')}</p>}
                {this.aiState === 'empty' && <p>No governed suggestions are available for this project.</p>}
                {this.aiState === 'error' && <p>Suggestion data is unavailable; no recommendation can be applied here.</p>}
            </section>}
            {this.activeArea === 'equivalence' && <section className='renovatio-asset-editor' aria-label='Equivalence evidence'>
                <div><span>EQUIVALENCE EVIDENCE · {this.equivalenceState.toUpperCase()}</span></div>
                {this.equivalenceState === 'ready' && <><p>Evidence: {this.equivalence?.evidence.map(item => item.name).join(', ') || 'none'}</p>
                    <p>Generated targets: {this.equivalence?.generatedTargets.map(item => item.name).join(', ') || 'none'}</p>
                    <p>Verdicts: {this.equivalence?.verdicts.map(verdict => `${verdict.fixtureId} · ${verdict.classification} · ${verdict.reason}${verdict.blocksRelease ? ' · RELEASE BLOCKED' : ''}`).join(' | ') || 'none'}</p>
                    <p>Only persisted reports are shown; an inventory alone implies no equivalence verdict.</p></>}
                {this.equivalenceState === 'empty' && <p>No persisted evidence or generated targets are available. This is not an equivalence verdict.</p>}
                {this.equivalenceState === 'error' && <p>Equivalence evidence is unavailable; no comparison or acceptance can occur here.</p>}
            </section>}
        </section>;
    }

    protected render(): React.ReactNode {
        return <main className='renovatio-surface renovatio-shell' aria-labelledby='renovatio-workbench-heading'>
            <header className='renovatio-header renovatio-shell-header'>
                <span className='renovatio-kicker'>IDE SHELL · ISSUE 180</span>
                <h1 id='renovatio-workbench-heading'>RENOVATIO / CONTROL DECK</h1>
                <p>Navigate governed modernization evidence without leaving the Theia workbench.</p>
                <a className='renovatio-dashboard-link' href={this.dashboardUrl} target='_blank' rel='noreferrer'>Open administrative dashboard</a>
            </header>
            <div className='renovatio-shell-grid'>
                <nav className='renovatio-activity-rail' aria-label='Renovatio activity areas'>
                    {AREAS.map(area => <button type='button' key={area.id} className={this.activeArea === area.id ? 'is-active' : undefined}
                        aria-pressed={this.activeArea === area.id} onClick={() => this.activateArea(area.id)}><span>{area.coordinate}</span>{area.label}</button>)}
                </nav>
                {this.renderProjectExplorer()}
                {this.renderArea()}
            </div>
            <section className='renovatio-bottom-panel' aria-label='Workbench status panel'>
                <div><span>STATE</span><strong>{this.loading ? 'LOADING' : this.shellState.toUpperCase()}</strong></div>
                <div><span>PROJECT</span><strong>{this.selectedProject}</strong></div>
                <div><span>LAYOUT</span><strong>THEIA TABS + PANELS</strong></div>
            </section>
        </main>;
    }
}
