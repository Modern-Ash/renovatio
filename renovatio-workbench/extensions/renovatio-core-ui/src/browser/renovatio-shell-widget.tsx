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
    protected loading = true;
    protected selectedAsset = 'PAYROLL.CBL';
    protected selectedProject = 'payroll-modernization';
    protected shellState: ShellState = 'loading';

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
        this.update();
    }

    protected async loadRuntimeConfig(): Promise<void> {
        const dashboard = await this.readEnv('RENOVATIO_DASHBOARD_URL');
        this.dashboardUrl = dashboard ?? this.dashboardUrl;
        this.loading = false;
        this.shellState = 'ready';
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

    protected selectAsset(asset: string): void {
        this.selectedAsset = asset;
        this.update();
    }

    protected selectProject(project: string): void {
        this.selectedProject = project;
        this.persistShellState();
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
                <button type='button' aria-pressed={this.selectedProject === 'payroll-modernization'} onClick={() => this.selectProject('payroll-modernization')}>PAYROLL</button>
                <button type='button' aria-pressed={this.selectedProject === 'ledger-modernization'} onClick={() => this.selectProject('ledger-modernization')}>LEDGER</button>
            </div>
            <p className='renovatio-selection'>ACTIVE / {this.selectedProject}</p>
            <nav aria-label='Project assets'>
                {PROJECT_ASSETS.map(assetGroup => <section className='renovatio-asset-group' key={assetGroup.group} aria-label={assetGroup.group}>
                    <h3>{assetGroup.group}</h3>
                    <ul>{assetGroup.items.map(asset => <li key={asset}><button type='button'
                        aria-current={this.selectedAsset === asset ? 'page' : undefined}
                        onClick={() => this.selectAsset(asset)}>{asset}</button></li>)}</ul>
                </section>)}
            </nav>
        </aside>;
    }

    protected renderArea(): React.ReactNode {
        const area = AREAS.find(candidate => candidate.id === this.activeArea) ?? AREAS[0];
        return <section className='renovatio-area-content' aria-labelledby='renovatio-area-heading'>
            <span className='renovatio-coordinate'>{area.coordinate}</span>
            <h2 id='renovatio-area-heading'>{this.activeArea === 'project' ? 'Project navigation' : area.label}</h2>
            <p>{area.summary}</p>
            <article className='renovatio-asset-preview' aria-label='Selected shell context'>
                <span>{this.activeArea === 'project' ? 'SELECTED ASSET' : 'WORKBENCH AREA'}</span>
                <strong>{this.activeArea === 'project' ? this.selectedAsset : `${area.label.toUpperCase()} / READY FOR ADAPTER`}</strong>
                <p>{this.activeArea === 'project'
                    ? 'Open in the Theia editor area; source content remains inside the configured workspace boundary.'
                    : 'UI boundary is available; live data remains governed by the existing backend contract.'}</p>
            </article>
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
