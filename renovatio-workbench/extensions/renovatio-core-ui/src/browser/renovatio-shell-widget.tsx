import { Message, ReactWidget } from '@theia/core/lib/browser';
import { EnvVariable, EnvVariablesServer } from '@theia/core/lib/common/env-variables';
import { inject, injectable, postConstruct } from '@theia/core/shared/inversify';
import React from '@theia/core/shared/react';

export type RenovatioAreaId = 'project' | 'analysis' | 'architecture' | 'ai' | 'equivalence';
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
    { id: 'architecture', coordinate: 'ACT.03', label: 'Architecture', summary: 'Domain model and target architecture views.' },
    { id: 'ai', coordinate: 'ACT.04', label: 'AI', summary: 'Governed suggestions, provenance and review boundaries.' },
    { id: 'equivalence', coordinate: 'ACT.05', label: 'Equivalence', summary: 'Evidence, test deltas and acceptance history.' }
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
type WorkbenchArchitecture = { modules: unknown[]; components: unknown[]; relations: unknown[]; diagnostics: unknown[]; hasFallback: boolean };
type WorkbenchAi = { items: Array<{ id: string; category: string; source: string; status: string; confidence: number; evidenceCount: number; llmFailed: boolean }> };
type WorkbenchEquivalence = { evidence: Array<{ id: string; name: string }>; generatedTargets: Array<{ id: string; name: string }>; verdicts: Array<{ fixtureId: string; classification: string; reason: string; blocksRelease: boolean }> };
type SourceSymbol = { id: string; kind: string; name: string; line: number; column: number; parentId: string | null; irCoordinate: string };
type SourceDiagnostic = { severity: string; message: string; line: number };
type SourceFile = { id: string; name: string; kind: string; path: string; hash: string; encoding: string; analysisStatus: string; symbols: SourceSymbol[]; diagnostics: SourceDiagnostic[] };
type SourceExplorer = { files: SourceFile[]; datasets: Array<{ id: string; name: string; referencedBy: string[] }> };
type AreaState = 'idle' | 'loading' | 'ready' | 'empty' | 'permission-denied' | 'error';

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
    protected architectureState: 'idle' | 'loading' | 'ready' | 'empty' | 'error' = 'idle';
    protected ai?: WorkbenchAi;
    protected aiState: 'idle' | 'loading' | 'ready' | 'empty' | 'error' = 'idle';
    protected equivalence?: WorkbenchEquivalence;
    protected equivalenceState: 'idle' | 'loading' | 'ready' | 'empty' | 'error' = 'idle';
    protected sourceExplorer?: SourceExplorer;
    protected sourceExplorerState: AreaState = 'idle';
    protected selectedSourceFileId: string | null = null;
    protected symbolSearch = '';

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
        this.architectureState = 'loading'; this.update();
        try {
            const response = await fetch(`${this.backendUrl}/api/projects/${encodeURIComponent(this.selectedProject)}/workbench/architecture`);
            if (!response.ok) throw new Error(`Architecture adapter returned ${response.status}`);
            this.architecture = await response.json() as WorkbenchArchitecture;
            this.architectureState = this.architecture.modules.length || this.architecture.components.length ? 'ready' : 'empty';
        } catch { this.architectureState = 'error'; }
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
        this.persistShellState();
        void this.loadAssets().then(() => this.loadContext()).then(() => this.update()).catch(() => { this.shellState = 'error'; this.update(); });
        void this.loadSourceExplorer();
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
                                        <span className='renovatio-symbol-kind'>{symbol.kind}</span> {symbol.name}
                                        <span className='renovatio-symbol-pos'>{symbol.line}:{symbol.column}</span>
                                        <span className='renovatio-symbol-ir'>{symbol.irCoordinate}</span>
                                    </li>)
                                    : <li>This file is shown as “{file.analysisStatus}”; no navigable symbols were extracted.</li>}
                            </ol>
                            <section className='renovatio-problems' aria-label='Problems'>
                                <h4>Problems</h4>
                                {file.diagnostics.length
                                    ? <ul>{file.diagnostics.map((diagnostic, index) => <li key={index}>{diagnostic.severity.toUpperCase()} · line {diagnostic.line} · {diagnostic.message}</li>)}</ul>
                                    : <p>No parse diagnostics for {file.name}.</p>}
                            </section>
                        </>}
                    {this.sourceExplorer.datasets.length > 0 && <p className='renovatio-datasets'>Datasets: {this.sourceExplorer.datasets.map(dataset => dataset.name).join(', ')}</p>}
                </div>
            </div>}
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
                <strong>{this.activeArea === 'project' ? this.selectedAsset : this.activeArea === 'architecture' && this.architectureState === 'ready' ? 'ARCHITECTURE / PREVIEW READY' : this.activeArea === 'equivalence' && this.equivalenceState === 'ready' ? 'EQUIVALENCE / INVENTORY READY' : `${area.label.toUpperCase()} / READY FOR ADAPTER`}</strong>
                <p>{this.activeArea === 'project'
                    ? 'Source content remains inside the configured workspace boundary.'
                    : this.activeArea === 'architecture' && this.architectureState === 'ready' ? 'Read-only target architecture preview is loaded from the governed backend contract.'
                        : this.activeArea === 'equivalence' && this.equivalenceState === 'ready' ? 'Read-only inventory is loaded from the governed backend contract; it does not imply an equivalence verdict.'
                            : 'UI boundary is available; live data remains governed by the existing backend contract.'}</p>
            </article>
            {this.activeArea === 'project' && <section className='renovatio-asset-editor' aria-label='Selected asset content'>
                <div><span>ADAPTER CONTENT · {this.assetContentState.toUpperCase()}</span>{this.selectedAssetWritable && <button type='button' onClick={() => void this.saveAsset()} disabled={this.assetContentState === 'saving'}>Save development target</button>}</div>
                <textarea value={this.assetContent} readOnly={!this.selectedAssetWritable} onChange={event => this.updateAssetContent(event)} aria-label={`${this.selectedAsset} content`} spellCheck={false} />
                <p>{this.selectedAssetWritable ? 'Temporary development mode: writing a generated target is enabled. It is not production authorization.' : 'Legacy source and evidence are read-only.'}</p>
            </section>}
            {this.activeArea === 'project' && this.renderSourceExplorer()}
            {this.activeArea === 'analysis' && <section className='renovatio-asset-editor' aria-label='Analysis inventory'>
                <div><span>ANALYSIS ADAPTER · {this.analysisState.toUpperCase()}</span></div>
                {this.analysisState === 'ready' && <><p>{Object.entries(this.analysis?.inventory ?? {}).map(([category, count]) => `${category}: ${count}`).join(' · ')}</p>
                    <p>{this.analysis?.runs.length ? `Runs: ${this.analysis.runs.map(run => run.runId).join(', ')}` : 'No persisted runs for this project.'}</p></>}
                {this.analysisState === 'empty' && <p>No inventory or persisted runs are available.</p>}
                {this.analysisState === 'error' && <p>Analysis data is unavailable; project navigation remains available.</p>}
            </section>}
            {this.activeArea === 'architecture' && <section className='renovatio-asset-editor' aria-label='Architecture preview'>
                <div><span>ARCHITECTURE PREVIEW · {this.architectureState.toUpperCase()}</span></div>
                {this.architectureState === 'ready' && <p>Modules: {this.architecture?.modules.length} · Components: {this.architecture?.components.length} · Relations: {this.architecture?.relations.length} · Diagnostics: {this.architecture?.diagnostics.length}</p>}
                {this.architectureState === 'empty' && <p>No architecture preview is available for this project.</p>}
                {this.architectureState === 'error' && <p>Architecture preview is unavailable; project navigation remains available.</p>}
            </section>}
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
                <span className='renovatio-kicker'>IDE SHELL · ISSUE 177</span>
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
