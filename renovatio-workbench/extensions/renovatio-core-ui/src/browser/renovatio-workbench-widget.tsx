import { Message, ReactWidget } from '@theia/core/lib/browser';
import { EnvVariable, EnvVariablesServer } from '@theia/core/lib/common/env-variables';
import { inject, injectable, postConstruct } from '@theia/core/shared/inversify';
import React from '@theia/core/shared/react';

export interface RenovatioRuntimeConfig {
    backendUrl: string;
    authMode: string;
    telemetryEnabled: boolean;
    workspaceRoot: string;
}

const DEFAULT_CONFIG: RenovatioRuntimeConfig = {
    backendUrl: 'http://127.0.0.1:8080',
    authMode: 'existing-backend',
    telemetryEnabled: false,
    workspaceRoot: '/workspace'
};

@injectable()
export class RenovatioWorkbenchWidget extends ReactWidget {
    static readonly ID = 'renovatio.workbench.widget';
    static readonly LABEL = 'Renovatio Workbench';

    @inject(EnvVariablesServer)
    protected readonly envVariables!: EnvVariablesServer;

    protected config: RenovatioRuntimeConfig = DEFAULT_CONFIG;
    protected loading = true;

    @postConstruct()
    protected init(): void {
        this.id = RenovatioWorkbenchWidget.ID;
        this.title.label = RenovatioWorkbenchWidget.LABEL;
        this.title.caption = 'Renovatio platform validation';
        this.title.closable = true;
        this.title.iconClass = 'codicon codicon-symbol-structure';
        this.addClass('renovatio-workbench-widget');
        this.node.tabIndex = 0;
        void this.loadRuntimeConfig();
    }

    protected async loadRuntimeConfig(): Promise<void> {
        const [backendUrl, authMode, telemetry, workspaceRoot] = await Promise.all([
            this.readEnv('RENOVATIO_BACKEND_URL'),
            this.readEnv('RENOVATIO_AUTH_MODE'),
            this.readEnv('RENOVATIO_TELEMETRY_ENABLED'),
            this.readEnv('RENOVATIO_WORKSPACE_ROOT')
        ]);
        this.config = {
            backendUrl: backendUrl ?? DEFAULT_CONFIG.backendUrl,
            authMode: authMode ?? DEFAULT_CONFIG.authMode,
            telemetryEnabled: telemetry?.toLowerCase() === 'true',
            workspaceRoot: workspaceRoot ?? DEFAULT_CONFIG.workspaceRoot
        };
        this.loading = false;
        this.update();
    }

    protected async readEnv(name: string): Promise<string | undefined> {
        const variable: EnvVariable | undefined = await this.envVariables.getValue(name);
        return variable?.value?.trim() || undefined;
    }

    protected override onActivateRequest(message: Message): void {
        super.onActivateRequest(message);
        this.node.focus();
    }

    protected render(): React.ReactNode {
        const telemetryLabel = this.config.telemetryEnabled ? 'enabled' : 'disabled';
        return (
            <main className='renovatio-surface' aria-labelledby='renovatio-workbench-heading'>
                <header className='renovatio-header'>
                    <span className='renovatio-kicker'>PLATFORM SPIKE · ISSUE 176</span>
                    <h1 id='renovatio-workbench-heading'>RENOVATIO / WORKBENCH</h1>
                    <p>Traceable COBOL modernization inside an extensible IDE shell.</p>
                </header>

                <section className='renovatio-status-grid' aria-label='Runtime configuration'>
                    <article className='renovatio-panel renovatio-panel-primary'>
                        <span className='renovatio-coordinate'>SYS.01</span>
                        <h2>Platform signal</h2>
                        <div className='renovatio-signal-row' role='status' aria-live='polite'>
                            <span className='renovatio-signal' aria-hidden='true' />
                            {this.loading ? 'Reading environment…' : 'Theia extension active'}
                        </div>
                    </article>

                    <article className='renovatio-panel'>
                        <span className='renovatio-coordinate'>API.02</span>
                        <h2>Backend boundary</h2>
                        <dl>
                            <div><dt>Base URL</dt><dd>{this.config.backendUrl}</dd></div>
                            <div><dt>Auth mode</dt><dd>{this.config.authMode}</dd></div>
                        </dl>
                    </article>

                    <article className='renovatio-panel'>
                        <span className='renovatio-coordinate'>ENV.03</span>
                        <h2>Workspace policy</h2>
                        <dl>
                            <div><dt>Root</dt><dd>{this.config.workspaceRoot}</dd></div>
                            <div><dt>Telemetry</dt><dd>{telemetryLabel}</dd></div>
                        </dl>
                    </article>
                </section>

                <section className='renovatio-flow' aria-labelledby='renovatio-flow-heading'>
                    <div>
                        <span className='renovatio-coordinate'>FLOW.04</span>
                        <h2 id='renovatio-flow-heading'>Evidence path</h2>
                    </div>
                    <ol>
                        <li><strong>COBOL</strong><span>source + hash</span></li>
                        <li><strong>DOMAIN</strong><span>model + provenance</span></li>
                        <li><strong>TARGET</strong><span>reviewed change set</span></li>
                    </ol>
                </section>
            </main>
        );
    }
}
